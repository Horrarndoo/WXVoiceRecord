package com.example.wxvoicerecord.voice;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Looper;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wxvoicerecord.BuildConfig;
import com.example.wxvoicerecord.R;
import com.example.wxvoicerecord.utils.ViewUtils;

import java.io.File;

import androidx.appcompat.widget.AppCompatButton;


public class RecordButton extends AppCompatButton {
    private final static String TAG = "RecordButton";
    /**
     * 正常状态
     */
    private final static int NORMAL = 1;
    /**
     * 取消状态
     */
    private final static int CANCEL = 2;
    /**
     * 最短录音时间（单位：ms）
     **/
    private final static int MIN_RECORD_TIME_THRESHOLD = 1000;
    /**
     * 最长录音时间（单位：ms）
     **/
    private final static int MAX_RECORD_TIME_THRESHOLD = 1000 * 30;
    /**
     * 录音剩余时间提醒阈值（录音剩余时间少于这个值就提醒，单位：ms）
     */
    private final static int RECORD_LEFT_TIME_TO_NOTICE = 1000 * 10;
    /**
     * 录音文件名
     */
    private String mRecordFileName;
    /**
     * 录音结束监听
     */
    private OnFinishedRecordListener mOnFinishedRecordListener;
    /**
     * 开始录音时间
     */
    private long mStartRecordTime;
    /**
     * 首次进入录音倒计时震动提示
     */
    private volatile boolean vibrateNotice = true;
    /**
     * 录音状态，避免重复刷新界面，仅当状态变化时才刷新显示
     */
    private volatile int mRecordingState = NORMAL;
    /**
     * 图标初始宽度（用于动画）
     */
    private int mOriginIconWidth;
    /**
     * 图标初始高度（用于动画）
     */
    private int mOriginIconHeight;

    /**
     * 录音状态dialog
     */
    private Dialog mRecordStatusDialog;
    private LinearLayout llBottom;
    private TextView tvCancel;
    private TextView tvSend;
    private ImageView ivRecord;
    private ImageView ivCancel;
    private volatile MediaRecorder mRecorder;
    private boolean runningObtainDecibelThread = true;
    private ObtainDecibelThread mThread;
    private VoiceStatusPanel mVoiceStatusPanel;

    public RecordButton(Context context) {
        this(context, null);
    }

    public RecordButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, android.R.attr.borderlessButtonStyle);
    }

    public void setmOnFinishedRecordListener(OnFinishedRecordListener listener) {
        mOnFinishedRecordListener = listener;
    }

    @SuppressLint({"ClickableViewAccessibility", "UseCompatLoadingForDrawables"})
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //按下的时候，重新生成一个语音保存的地址，避免一直读写一个文件，可以引起错误
                setText("松开发送");
                MediaManager.reset();//停止其他音频播放
                initDialogAndStartRecord();
                break;
            case MotionEvent.ACTION_MOVE:
                //move事件时很有可能dialog还没有初始化完成
                if (mRecordStatusDialog == null || !mRecordStatusDialog.isShowing()) {
                    break;
                }
                if (!isLocationInRecordRect(event) && mRecordingState != CANCEL) {
                    mRecordingState = CANCEL;
                    showRecordingCancel();
                } else if (isLocationInRecordRect(event) && mRecordingState != NORMAL) {
                    mRecordingState = NORMAL;
                    showRecordingNormal();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                this.setText("按住录音");
                //当手指不在录音区域，会cancel
                if (!isLocationInRecordRect(event)) {
                    cancelRecord();
                } else {
                    finishRecord();
                }
                break;
        }
        return true;
    }

    /**
     * 触点是否在录音区域中
     *
     * @param event 时间
     * @return 是否在录音区域中
     */
    private boolean isLocationInRecordRect(MotionEvent event) {
        boolean result = ViewUtils.isEventAbsoluteLocationInView(getContext(), llBottom, event);
        return result;
    }

    /**
     * 显示正常录音状态
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void showRecordingNormal() {
        tvCancel.setVisibility(INVISIBLE);
        tvSend.setVisibility(VISIBLE);
        mVoiceStatusPanel.setCancel(false);
        llBottom.setBackground(getContext().getDrawable(R.drawable.layer_list_record_bottom_bg_selected));
        ivRecord.setColorFilter(getResources().getColor(R.color.record_selected_bottom_icon_color));
        ivCancel.setBackground(getContext().getDrawable(R.drawable.shape_record_cancel_button_unselected_bg));
        ivCancel.setColorFilter(getResources().getColor(R.color.record_cancel_button_unselected_tint_color));
        setCancelImageZoom(false);
    }

    /**
     * 显示松手取消录音状态
     */
    @SuppressLint("UseCompatLoadingForDrawables")
    private void showRecordingCancel() {
        tvCancel.setVisibility(VISIBLE);
        tvSend.setVisibility(INVISIBLE);
        mVoiceStatusPanel.setCancel(true);
        llBottom.setBackground(getContext().getDrawable(R.drawable.layer_list_record_bottom_bg_unselected));
        ivRecord.setColorFilter(getResources().getColor(R.color.record_unselected_bottom_icon_color));
        ivCancel.setBackground(getContext().getDrawable(R.drawable.shape_record_cancel_button_selected_bg));
        ivCancel.setColorFilter(getResources().getColor(R.color.record_cancel_button_selected_tint_color));
        setCancelImageZoom(true);
    }

    /**
     * 设置取消按钮缩放
     *
     * @param zoomIn 是否放大
     */
    private void setCancelImageZoom(boolean zoomIn) {
        ValueAnimator animator;
        if (zoomIn) {
            animator = ValueAnimator.ofFloat(1, 1.1f);
        } else {
            animator = ValueAnimator.ofFloat(1.1f, 1);
        }
        animator.setDuration(200);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(0);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                ivCancel.getLayoutParams().width = (int) (mOriginIconWidth * value);
                ivCancel.getLayoutParams().height = (int) (mOriginIconHeight * value);
                ivCancel.requestLayout();
            }
        });
        animator.start();
    }

    /**
     * 初始化录音对话框 并开始录音
     */
    private void initDialogAndStartRecord() {
        mRecordStatusDialog = new Dialog(getContext(), R.style.record_dialog_style);
        View view = View.inflate(getContext(), R.layout.dialog_record, null);
        mVoiceStatusPanel = view.findViewById(R.id.btn_wx_voice);
        tvCancel = view.findViewById(R.id.tv_cancel);
        tvSend = view.findViewById(R.id.tv_send);
        llBottom = view.findViewById(R.id.ll_bottom);
        ivRecord = view.findViewById(R.id.iv_record);
        ivCancel = view.findViewById(R.id.iv_cancel);
        tvCancel.setVisibility(INVISIBLE);

        mRecordStatusDialog.setContentView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        Window window = mRecordStatusDialog.getWindow();
        if (window != null) {
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            window.setBackgroundDrawableResource(android.R.color.transparent);
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            window.setAttributes(layoutParams);
        }

        if (startRecording()) {
            mRecordStatusDialog.show();
        }

        mOriginIconWidth = ivCancel.getLayoutParams().width;
        mOriginIconHeight = ivCancel.getLayoutParams().height;
    }

    /**
     * 放开手指，结束录音处理
     */
    private void finishRecord() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            post(new Runnable() {
                @Override
                public void run() {
                    finishRecord();
                }
            });
            return;
        }

        long reocrdTime = System.currentTimeMillis() - mStartRecordTime;

        vibrateNotice = true;
        final String wavFileName = mRecordFileName;
        File file = new File(wavFileName);
        stopRecording();
        if (!file.exists()) {
            //如果文件不存在，则返回
            //当我们到底最长时间，会在ObtainDecibelThread中，和onTouchEvent方法中，重复调用该方法
            //因此做一个检测
            return;
        }

        if (reocrdTime < MIN_RECORD_TIME_THRESHOLD) {
            Toast.makeText(getContext(), "录音时间太短", Toast.LENGTH_SHORT).show();
            file.delete();
            return;
        }

        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(wavFileName);
            mediaPlayer.prepare();
            mediaPlayer.getDuration();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mOnFinishedRecordListener == null)
            return;

        mOnFinishedRecordListener.onFinishedRecord(wavFileName, mediaPlayer.getDuration() / 1000);
    }

    /**
     * 取消录音对话框和停止录音
     */
    public void cancelRecord() {
        stopRecording();
        File file = new File(mRecordFileName);
        file.delete();
    }

    /**
     * 执行录音操作
     */
    private boolean startRecording() {
        if (mRecorder != null) {
            mRecorder.reset();
        } else {
            mRecorder = new MediaRecorder();
        }
        mStartRecordTime = System.currentTimeMillis();
        mRecordFileName = getContext().getFilesDir() + "/" + "voice_" + mStartRecordTime + ".wav";
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(mRecordFileName);
        try {
            mRecorder.prepare();
            mRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
            mRecorder.release();
            mRecorder = null;
            return false;
        }
        runningObtainDecibelThread = true;
        mThread = new ObtainDecibelThread();
        mThread.start();
        return true;
    }

    private void stopRecording() {
        runningObtainDecibelThread = false;
        if (mThread != null) {
            mThread = null;
        }

        if (mRecorder != null) {
            try {
                //停止时没有prepare，就会报stop failed
                mRecorder.stop();
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (mRecordStatusDialog != null) {
            mRecordStatusDialog.dismiss();
            mRecordStatusDialog = null;
        }
        if (mVoiceStatusPanel != null) {
            mVoiceStatusPanel.destroy();
            mVoiceStatusPanel = null;
        }
    }

    /**
     * 用来定时获取录音的声音大小，更新分贝值，以驱动动画
     * 获取录音时间，提醒用户
     * 到达最大时间以后自动停止
     */
    private class ObtainDecibelThread extends Thread {
        @Override
        public void run() {
            while (runningObtainDecibelThread) {
                if (mRecorder == null) {
                    break;
                }
                //声音振幅
                int maxAmplitude = mRecorder.getMaxAmplitude();
                //获取声音的db值
                double db = 20 * Math.log10(maxAmplitude);

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "分贝值：" + db);
                }

                long now = System.currentTimeMillis();
                long recordingTime = now - mStartRecordTime;
                //录音超出最大时间
                if (recordingTime > MAX_RECORD_TIME_THRESHOLD) {
                    finishRecord();
                    return;
                }

                //少于十秒则提醒
                long lessTime = MAX_RECORD_TIME_THRESHOLD - recordingTime;
                if (lessTime < RECORD_LEFT_TIME_TO_NOTICE) {
                    mVoiceStatusPanel.setmTextContent(lessTime / 1000 + "秒后将结束录音");
                    if (vibrateNotice) {
                        vibrateNotice = false;
                        Vibrator vibrator =
                                (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(500);
                    }
                } else {
                    mVoiceStatusPanel.updateVoiceDb((int) db);
                }

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 录音结束监听
     */
    public interface OnFinishedRecordListener {
        /**
         * 录音结束
         *
         * @param audioPath 音频文件路径
         * @param time      录音时长
         */
        void onFinishedRecord(String audioPath, int time);
    }
}
