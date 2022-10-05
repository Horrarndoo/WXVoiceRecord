package com.example.wxvoicerecord.voice.record;

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
import com.example.wxvoicerecord.dialog.RecordStatusDialog;
import com.example.wxvoicerecord.utils.ViewUtils;
import com.example.wxvoicerecord.voice.MediaManager;

import java.io.File;

import androidx.appcompat.widget.AppCompatButton;

/**
 * Created by Horrarndoo on 2022/9/21.
 * <p>
 * 录音按钮（实现类似微信点击录音的效果，并且对使用者提供回调）
 */
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
     * 录音文件名
     */
    private String mRecordFileName;
    /**
     * 录音配置
     */
    private IRecordConfig mRecordConfig;
    /**
     * 录音监听
     */
    private OnRecordListener mOnRecordListener;
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
    private static volatile int mRecordingState = CANCEL;
    /**
     * 录音状态dialog
     */
    private RecordStatusDialog mRecordStatusDialog;
    private volatile MediaRecorder mRecorder;
    private boolean runningObtainDecibelThread = true;
    private ObtainDecibelThread mThread;

    public RecordButton(Context context) {
        this(context, null);
    }

    public RecordButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecordButton(Context context, AttributeSet attrs, int defStyle) {
        //取消按键阴影
        super(context, attrs, android.R.attr.borderlessButtonStyle);
        mRecordConfig = new DefaultRecordConfig();
    }

    public void setOnRecordListener(OnRecordListener listener) {
        mOnRecordListener = listener;
    }

    public void setRecordConfig(IRecordConfig config) {
        this.mRecordConfig = config;
    }

    @SuppressLint({"ClickableViewAccessibility", "UseCompatLoadingForDrawables"})
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                //按下的时候，重新生成一个语音保存的地址，避免一直读写一个文件，可以引起错误
                MediaManager.getInstance().reset();//停止其他音频播放
                initDialogAndStartRecord();
                break;
            case MotionEvent.ACTION_MOVE:
                //move事件时很有可能dialog还没有初始化完成
                if (mRecordStatusDialog == null || !mRecordStatusDialog.isShowing()) {
                    break;
                }
                if (!isLocationInRecordRect(event) && mRecordingState != CANCEL) {
                    mRecordingState = CANCEL;
                    mRecordStatusDialog.showRecordingCancel();
                } else if (isLocationInRecordRect(event) && mRecordingState != NORMAL) {
                    mRecordingState = NORMAL;
                    mRecordStatusDialog.showRecordingNormal();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                //录音达到最大时长时，会自动结束录音并且消息dialog，此时需要判断，否则会判断为cancel将录音文件删除
                if (mRecordStatusDialog == null) {
                    break;
                }

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
     * @return 触点是否在录音区域中
     */
    private boolean isLocationInRecordRect(MotionEvent event) {
        return ViewUtils.isEventAbsoluteLocationInView(getContext(),
                mRecordStatusDialog.getBottomLayout(), event);
    }

    /**
     * 初始化录音对话框 并开始录音
     */
    private void initDialogAndStartRecord() {
        mRecordStatusDialog = new RecordStatusDialog(getContext());
        if (startRecording()) {
            mRecordStatusDialog.show();
        }
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
            return;
        }

        if (reocrdTime < mRecordConfig.getShortestRecordTime()) {
            Toast.makeText(getContext(), R.string.talk_time_is_too_short, Toast.LENGTH_SHORT).show();
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

        if (mOnRecordListener != null) {
            mOnRecordListener.onFinish(wavFileName, mediaPlayer.getDuration() / 1000);
        }
    }

    /**
     * 放开手指，取消录音处理
     */
    public void cancelRecord() {
        stopRecording();
        File file = new File(mRecordFileName);
        file.delete();
        if (mOnRecordListener != null) {
            mOnRecordListener.onCancel();
        }
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
        mRecordFileName = mRecordConfig.getRecordFileName();
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
        mRecordingState = CANCEL;
    }

    /**
     * 用来定时获取录音的声音大小，更新分贝值，以驱动动画
     * 获取录音时间，提醒用户
     * 到达最大时间以后自动停止
     */
    private class ObtainDecibelThread extends Thread {
        @SuppressLint("MissingPermission")
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
                if (recordingTime > mRecordConfig.getLongestRecordTime()) {
                    finishRecord();
                    return;
                }

                //少于十秒则提醒
                long lessTime = mRecordConfig.getLongestRecordTime() - recordingTime;
                if (lessTime < mRecordConfig.getWhatLeftTimeToNotice()) {
                    mRecordStatusDialog.updatePanelText(lessTime / 1000 + getResources().getString(R.string.will_be_finish_record_after_x_second));
                    if (vibrateNotice) {
                        vibrateNotice = false;
                        Vibrator vibrator =
                                (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(500);
                    }
                } else {
                    mRecordStatusDialog.updatePanelVoiceDb((int) db);
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
     * 录音监听
     */
    public interface OnRecordListener {
        /**
         * 录音结束
         *
         * @param audioPath 音频文件路径
         * @param time      录音时长
         */
        void onFinish(String audioPath, int time);

        /**
         * 录音取消
         */
        void onCancel();
    }
}
