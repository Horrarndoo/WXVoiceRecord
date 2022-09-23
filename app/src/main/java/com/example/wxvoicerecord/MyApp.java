package com.example.wxvoicerecord;

import android.app.Application;
import android.content.Context;

/**
 * Created by Horrarndoo on 2022/9/23.
 * <p>
 */
public class MyApp extends Application {
    protected static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    /**
     * 获取上下文对象
     *
     * @return context
     */
    public static Context getContext() {
        return context;
    }
}
