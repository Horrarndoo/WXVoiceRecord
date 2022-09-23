package com.example.wxvoicerecord.voice.record;

import android.text.TextUtils;

import com.example.wxvoicerecord.MyApp;

import java.io.File;

/**
 * Created by Horrarndoo on 2022/9/23.
 * <p>
 * 默认录音配置
 */
public class DefaultRecordConfig implements IRecordConfig {
    /**
     * 最短录音时间（单位：ms）
     **/
    public final static int SHORTREST_RECORD_TIME_THRESHOLD = 1000;
    /**
     * 最长录音时间（单位：ms）
     **/
    public final static int LONGEST_RECORD_TIME_THRESHOLD = 1000 * 30;
    /**
     * 录音剩余时间提醒阈值（录音剩余时间少于这个值就提醒，单位：ms）
     */
    public final static int WHAT_LEFT_TIME_TO_NOTICE = 1000 * 10;

    private String fileDir;

    public DefaultRecordConfig() {
        fileDir = MyApp.getContext().getFilesDir() + "/record";
        makeDirs(fileDir);
    }

    @Override
    public long getShortestRecordTime() {
        return SHORTREST_RECORD_TIME_THRESHOLD;
    }

    @Override
    public long getLongestRecordTime() {
        return LONGEST_RECORD_TIME_THRESHOLD;
    }

    @Override
    public long getWhatLeftTimeToNotice() {
        return WHAT_LEFT_TIME_TO_NOTICE;
    }

    @Override
    public String getRecordFileName() {
        return fileDir + "/voice_" + System.currentTimeMillis() + ".wav";
    }

    /**
     * 创建目录（可以是多个）
     *
     * @param filePath 目录路径
     * @return 如果路径为空时，返回false；如果目录创建成功，则返回true，否则返回false
     */
    public boolean makeDirs(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        File folder = new File(filePath);
        return folder.exists() && folder.isDirectory() || folder.mkdirs();
    }
}
