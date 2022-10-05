package com.example.wxvoicerecord.voice;


import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

public class MediaManager {
    public volatile static MediaManager instance;
    private MediaPlayer mPlayer;
    private static boolean isVoicePause;
    /**
     * 当前播放录音文件path
     */
    private String mPlayingVoicePath;
    /**
     * 播放完成监听
     */
    private OnCompletionListener mOnCompletionListener;

    private MediaManager() {
        mPlayer = new MediaPlayer();
    }

    public static MediaManager getInstance() {
        if (instance == null) {
            synchronized (MediaManager.class) {
                if (instance == null) {
                    instance = new MediaManager();
                }
            }
        }
        return instance;
    }

    /**
     * 播放音频
     */
    public void playVoice(String voicePath, OnCompletionListener onCompletionListener) {
        if (mPlayer == null) {
            return;
        }

        mPlayer.reset();
        mOnCompletionListener = onCompletionListener;
        try {
            mPlayingVoicePath = voicePath;
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setOnCompletionListener(onCompletionListener);
            mPlayer.setDataSource(voicePath);
            mPlayer.setVolume(90, 90);
            mPlayer.setLooping(false);
            mPlayer.prepare();
            mPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        if (isPlaying()) {
            mPlayer.pause();
            isVoicePause = true;
        }
    }

    public void reset() {
        if (isPlaying()) {
            mPlayer.reset();
            if (mOnCompletionListener != null) {
                mOnCompletionListener.onCompletion(null);
            }
        }
        mPlayingVoicePath = "";
    }

    public boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    public String getPlayingVoicePath() {
        if (!isPlaying()) {
            mPlayingVoicePath = "";
        }
        return mPlayingVoicePath;
    }

    public void resume() {
        if (mPlayer != null && isVoicePause) {
            mPlayer.start();
            isVoicePause = false;
        }
    }

    public void release() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        mPlayingVoicePath = "";
        instance = null;
    }
}
