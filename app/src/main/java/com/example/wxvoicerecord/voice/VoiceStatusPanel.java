package com.example.wxvoicerecord.voice;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.Nullable;

/**
 * Created by Horrarndoo on 2022/9/21.
 * <p>
 * 声音状态面板（配合RecordButton使用）
 * <p>
 * 在录音的时候可以通过 {@link #updateVoiceDb(int)} 来设置声音波纹的显示，根据传入的db值动态显示
 * <p>
 * 录音快结束时，更新面板内容，提示剩余时长
 * <p>
 * 根据RecordButton的选中状态，动态的更新背景
 * <p>
 * 如果不使用该控件的时候需要使用{@link #destroy()} 方法来销毁面板，避免内存泄露
 */
public class VoiceStatusPanel extends View {
    /**
     * 音量条的宽度
     */
    private static final int VOICE_LINE_WIDTH = 4;
    /**
     * 音量条之间的的间隔
     */
    private static final int VOICE_LINE_SPACE = 4;
    /**
     * 驱动Panel动画的事件
     */
    private static final int WHAT_ANIMATION = 1;
    /**
     * 驱动Panel宽度的事件
     */
    private static final int WHAT_CHANGE_PANEL_WIDTH = 2;
    /**
     * 驱动音量条高低变化的事件
     */
    private static final int WHAT_CHANGE_VOICE_DB = 3;
    /**
     * 驱动进入巡检模式的事件
     */
    private static final int WHAT_CHECK_VOICE = 4;
    /**
     * 监听模式下的声音线条高度
     */
    private static final int[] mCheckModeLineHeights = new int[]{15, 20, 25, 30, 25, 20, 15};
    /**
     * 监听模式线条尺寸下标
     */
    private int checkModeIndex = 0;
    /**
     * 是否监听模式（没有识别到人声）
     */
    private boolean isCheckMode = false;
    /**
     * 线条画笔
     */
    private Paint linePaint;
    /**
     * 背景画笔
     */
    private Paint bgPaint;
    /**
     * 文字画笔
     */
    private Paint txtPaint;
    /**
     * 是否首次显示（首次显示时开启放大动画）
     */
    private boolean firstShow = true;
    /**
     * 音量条集合
     */
    List<VoiceLine> mVoiceLines = new ArrayList<>();
    /**
     * handler，处理界面更新
     */
    private LineHandler mLineHandler;
    /**
     * 回弹插值器
     */
    private Interpolator mInterpolator = new BounceInterpolator();
    /**
     * 最开始显示的宽度占控件整个宽度的比例
     */
    private final float mDefaultWidthRotas = 0.42f;
    /**
     * 最大显示宽度
     */
    private final float mMaxWidthRotas = 0.8f;
    /**
     * 当前显示的宽度比例（占当前屏幕宽度的比例）
     */
    private float mCurrentWidthRotas = mDefaultWidthRotas;
    /**
     * 背景圆角的大小,单位dp
     */
    private int mBackgroundRound = 15;
    /**
     * 当前显示的宽度
     */
    private int mCurrentWidth = 0;
    /**
     * 文字区域
     */
    private Rect mTextRect = new Rect();
    /**
     * 是否显示文字
     */
    private boolean isShowingText = false;
    private String mTextContent = null;
    /**
     * 取消录音状态时背景色
     */
    private static final String CANCEL_BACKGROUND_COLOR = "#FA5251";
    /**
     * 正常状态时背景色
     */
    private static final String NORMAL_BACKGROUND_COLOR = "#95EC69";

    public VoiceStatusPanel(Context context) {
        super(context);
    }

    public VoiceStatusPanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#99000000"));
        linePaint.setStyle(Paint.Style.FILL);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor(NORMAL_BACKGROUND_COLOR));
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setStrokeCap(Paint.Cap.ROUND);
        bgPaint.setStrokeJoin(Paint.Join.ROUND);
        bgPaint.setAntiAlias(true);
        mBackgroundRound = dip2px(context, mBackgroundRound);

        txtPaint = new Paint();
        txtPaint.setColor(Color.BLACK);
        txtPaint.setStyle(Paint.Style.FILL);
        txtPaint.setStrokeCap(Paint.Cap.ROUND);
        txtPaint.setStrokeJoin(Paint.Join.ROUND);
        txtPaint.setTextSize(dip2px(context, 14));
        txtPaint.setAntiAlias(true);

        buildVoiceLines();
        new Thread(() -> {
            Looper.prepare();
            mLineHandler = new LineHandler(Looper.myLooper());
            Looper.loop();
        }).start();
    }

    /**
     * 声音线条，组合起来呈现声音波纹效果
     */
    private static class VoiceLine {
        /**
         * 绘制区域
         */
        RectF rectF;
        /**
         * 线条最大高度（单位像素）
         */
        int maxLineHeight;
        /**
         * 线条高度（单位像素）
         */
        int lineHeight;
        /**
         * 是否缩小模式
         */
        boolean small = true;
        /**
         * db转换比例（根据比例值可以显示一个高低不同的效果）
         */
        float rotas = 1.0f;
        /**
         * 时间完成度，返回在0-1（配合时间插值器使用）
         */
        float timeCompletion = 0;
        /**
         * 用于计算时间增长步长
         */
        int duration = 0;
    }

    /**
     * 是否为松手取消录音状态
     */
    private boolean isStatusCancel = false;

    /**
     * 设置取消状态
     *
     * @param cancel 是否取消
     */
    public void setCancel(boolean cancel) {
        if (cancel) {
            bgPaint.setColor(Color.parseColor(CANCEL_BACKGROUND_COLOR));
        } else {
            bgPaint.setColor(Color.parseColor(NORMAL_BACKGROUND_COLOR));
        }
        isStatusCancel = cancel;
    }

    /**
     * 每条音频线能显示的最大值与showVoiceSize的比值
     */
    float[] mRatios = new float[]{0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.5f, 0.3f, 0.5f, 0.8f, 1.0f
            , 0.8f, 0.5f, 0.3f, 0.5f, 0.7f, 0.6f, 0.5f, 0.4f, 0.3f, 0.2f};

    /**
     * 构建声音线条集合
     */
    private void buildVoiceLines() {
        mVoiceLines.clear();
        for (float ratio : mRatios) {
            int maxLineHeight = (int) (20 * ratio);
            RectF rect = new RectF(-VOICE_LINE_WIDTH / 2.f,
                    -maxLineHeight / 2.f,
                    VOICE_LINE_WIDTH / 2.f,
                    maxLineHeight / 2.f);
            VoiceLine drawLine = new VoiceLine();
            drawLine.maxLineHeight = maxLineHeight;
            drawLine.rectF = rect;
            drawLine.lineHeight = new Random().nextInt(maxLineHeight);
            drawLine.rotas = ratio;
            //通过设置不同的时间完成度，让每个音量条有不同的初始值。可以实现参差不齐的效果
            drawLine.timeCompletion = ratio;
            drawLine.duration = (int) (400 * (1.0f / ratio));
            mVoiceLines.add(drawLine);
        }
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void setTextContent(String textContent) {
        isShowingText = true;
        this.mTextContent = textContent;
        txtPaint.getTextBounds(mTextContent, 0, mTextContent.length(), mTextRect);
    }

    /**
     * 更新音量db值，其实就是音量条显示的高度（单位像素)
     *
     * @param db 音量db值
     */
    public void updateVoiceDb(int db) {
        Message message = Message.obtain();
        message.obj = db;
        message.what = WHAT_CHANGE_VOICE_DB;
        mLineHandler.sendMessage(message);
        if (firstShow) {
            //Panel宽度变化
            mLineHandler.removeMessages(WHAT_CHANGE_PANEL_WIDTH);
            mLineHandler.sendEmptyMessage(WHAT_CHANGE_PANEL_WIDTH);
            firstShow = false;
        }
    }

    /**
     * 设置插值器
     *
     * @param interpolator 插值器
     */
    public void setInterpolator(Interpolator interpolator) {
        this.mInterpolator = interpolator;
    }

    private class LineHandler extends Handler {
        public LineHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void dispatchMessage(@androidx.annotation.NonNull Message msg) {
            switch (msg.what) {
                case WHAT_ANIMATION:
                    for (VoiceLine voiceLine : mVoiceLines) {
                        //时间增长步长
                        float timeStep = 16.0f / voiceLine.duration;
                        //时间完成度
                        float timeCompletion = voiceLine.timeCompletion;
                        //更新时间完成度
                        timeCompletion += timeStep;
                        //获取动画完成度。
                        float animationCompletion = mInterpolator.getInterpolation(timeCompletion);
                        int lineHeight;
                        //更新音量条的高度
                        if (voiceLine.small) {
                            //变小
                            lineHeight =
                                    (int) ((1 - animationCompletion) * voiceLine.maxLineHeight);
                        } else {
                            lineHeight = (int) (animationCompletion * voiceLine.maxLineHeight);
                        }
                        if (timeCompletion >= 1) {
                            //完成了单边的缩小，或增长，则切换模式
                            voiceLine.small = !voiceLine.small;
                            voiceLine.timeCompletion = 0;
                        } else {
                            //更新时间完成度。
                            voiceLine.timeCompletion = timeCompletion;
                        }
                        //对最小值进行过滤
                        lineHeight = Math.max(lineHeight, 10);
                        //上下等高
                        RectF rectF = voiceLine.rectF;
                        rectF.top = -lineHeight * 1.0f / 2;
                        rectF.bottom = lineHeight * 1.0f / 2;
                        voiceLine.lineHeight = lineHeight;
                    }
                    //更新UI
                    invalidate();
                    removeMessages(WHAT_ANIMATION);
                    sendEmptyMessageDelayed(WHAT_ANIMATION, 16);
                    break;
                case WHAT_CHANGE_PANEL_WIDTH:
                    //松手取消录音状态下的面板宽度
                    if (isStatusCancel) {
                        if (mCurrentWidth >= (mDefaultWidthRotas - 0.05) * getWidth()) {
                            mCurrentWidth *= 0.97;
                            invalidate();
                        }
                        mLineHandler.sendEmptyMessageDelayed(WHAT_CHANGE_PANEL_WIDTH, 16);
                        break;
                    }
                    //恢复到原来的大小，-因为宽度是整型，强转比对可能不准，因此减去一个误差值
                    if (mCurrentWidth < mCurrentWidthRotas * getWidth() - 5) {
                        mCurrentWidth *= 1.03;
                        invalidate();
                        mLineHandler.sendEmptyMessageDelayed(WHAT_CHANGE_PANEL_WIDTH, 16);
                    } else if (mCurrentWidthRotas < mMaxWidthRotas) {
                        mCurrentWidthRotas += 0.0005f;
                        mCurrentWidth = (int) (getWidth() * mCurrentWidthRotas);
                        invalidate();
                        mLineHandler.sendEmptyMessageDelayed(WHAT_CHANGE_PANEL_WIDTH, 32);
                    }
                    break;
                case WHAT_CHANGE_VOICE_DB:
                    int voiceDecibel = (int) msg.obj;
                    //一般正常说话分贝值在40-60，这里以30为没有声音的判断依据
                    if (voiceDecibel < 30) {
                        if (!isCheckMode) {
                            //声音太小进入监听模式
                            isCheckMode = true;
                            checkModeIndex = mVoiceLines.size() - 1;
                            sendEmptyMessage(WHAT_CHECK_VOICE);
                        }
                        return;
                    } else {
                        //退出监听模式
                        isCheckMode = false;
                        removeMessages(WHAT_CHECK_VOICE);
                    }

                    for (VoiceLine drawLine : mVoiceLines) {
                        drawLine.timeCompletion = 0;
                        drawLine.small = false;
                        drawLine.maxLineHeight = (int) (drawLine.rotas * voiceDecibel);
                    }
                    sendEmptyMessage(WHAT_ANIMATION);
                    break;
                case WHAT_CHECK_VOICE:
                    //判断是否是监听模式
                    if (!isCheckMode) {
                        return;
                    }
                    //移除之前的上下的动画模式
                    removeMessages(WHAT_ANIMATION);
                    //由于声音太小，显示波浪动画表示正在检查声音
                    for (int i = 0; i < mVoiceLines.size(); i++) {
                        VoiceLine voiceLine = mVoiceLines.get(i);
                        int index = i - checkModeIndex;
                        if (index >= 0 && index < mCheckModeLineHeights.length) {
                            voiceLine.lineHeight = mCheckModeLineHeights[index];
                        } else {
                            voiceLine.lineHeight = 10;
                        }
                        voiceLine.rectF.top = -voiceLine.lineHeight / 2.f;
                        voiceLine.rectF.bottom = voiceLine.lineHeight / 2.f;

                    }
                    checkModeIndex--;
                    if (checkModeIndex == -mCheckModeLineHeights.length) {
                        checkModeIndex = mVoiceLines.size() - 1;
                    }
                    //更新UI
                    invalidate();
                    removeMessages(WHAT_CHECK_VOICE);
                    sendEmptyMessageDelayed(WHAT_CHECK_VOICE, 100);
                    break;
            }
        }
    }

    /**
     * 销毁
     */
    public void destroy() {
        mLineHandler.getLooper().quit();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCurrentWidth = (int) (mCurrentWidthRotas * w);
        mLineHandler.sendEmptyMessage(WHAT_ANIMATION);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        //三角宽
        int triangleWidth = dip2px(getContext(), 8);
        //三角高
        int triangleHeight = dip2px(getContext(), 10);
        //三角顶部弧形高度（顶点用一个弧边过度）
        int triangleTopArcHeight = dip2px(getContext(), 3);
        //圆角背景区域高度
        int roundRectHeight = getHeight() - triangleHeight;
        //平移画布到圆角背景区域中心点
        canvas.translate(getWidth() / 2.f, roundRectHeight / 2.f);
        //画圆角背景
        canvas.drawRoundRect(-mCurrentWidth / 2.f, -roundRectHeight / 2.f,
                mCurrentWidth / 2.f, roundRectHeight / 2.f,
                mBackgroundRound, mBackgroundRound, bgPaint);
        //绘制三角（从画布中心点开始画）
        @SuppressLint("DrawAllocation")
        Path pathTriangle = new Path();
        pathTriangle.moveTo(-triangleWidth, roundRectHeight / 2.f);
        pathTriangle.lineTo(triangleWidth, roundRectHeight / 2.f);
        pathTriangle.lineTo(triangleWidth / 2.f * (triangleTopArcHeight / (triangleHeight * 1.f))
                , roundRectHeight / 2.f + triangleHeight - triangleTopArcHeight);
        pathTriangle.quadTo(triangleWidth / 2.f * (triangleTopArcHeight / (triangleHeight * 1.f)),
                roundRectHeight / 2.f + triangleHeight - triangleTopArcHeight,
                -triangleWidth / 2.f * (triangleTopArcHeight / (triangleHeight * 1.f)),
                roundRectHeight / 2.f + triangleHeight - triangleTopArcHeight);
        pathTriangle.moveTo(-triangleWidth / 2.f * (triangleTopArcHeight / (triangleHeight * 1.f)),
                roundRectHeight / 2.f + triangleHeight - triangleTopArcHeight);

        pathTriangle.close();//关闭路径的绘制
        canvas.drawPath(pathTriangle, bgPaint);

        if (isShowingText) {
            int txtHeight = mTextRect.height() / 2;
            int txtWidth = mTextRect.width() / 2;
            canvas.drawText(mTextContent, -txtWidth, txtHeight, txtPaint);
        } else {
            float offsetX =
                    (mVoiceLines.size() - 1) * 1.0f / 2 * (VOICE_LINE_WIDTH + VOICE_LINE_SPACE);
            canvas.translate(-offsetX, 0);
            for (VoiceLine voiceLine : mVoiceLines) {
                canvas.drawRoundRect(voiceLine.rectF, VOICE_LINE_WIDTH / 2.f,
                        VOICE_LINE_WIDTH / 2.f, linePaint);
                canvas.translate(VOICE_LINE_WIDTH + VOICE_LINE_SPACE, 0);
            }
        }
        canvas.restore();
    }
}