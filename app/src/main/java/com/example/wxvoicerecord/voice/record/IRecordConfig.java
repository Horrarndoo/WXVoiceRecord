package com.example.wxvoicerecord.voice.record;

/**
 * Created by Horrarndoo on 2022/9/23.
 * <p>
 * 录音配置
 */
public interface IRecordConfig {
    /**
     * 获取最短录音时间
     *
     * @return 获取最短录音时间
     */
    long getShortestRecordTime();

    /**
     * 获取最长录音时间
     *
     * @return 获取最长录音时间
     */
    long getLongestRecordTime();

    /**
     * 获取剩余时间提醒阈值
     *
     * @return 提醒剩余时间
     */
    long getWhatLeftTimeToNotice();

    /**
     * 获取录音文件名（绝对路径）
     * <p>
     * 如果本地没有路径文件夹，需要先创建文件夹
     *
     * @return 录音文件名
     */
    String getRecordFileName();
}
