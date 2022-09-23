package com.example.wxvoicerecord.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Horrarndoo on 2022/9/19.
 * <p>
 * 界面工具类
 */
public class ViewUtils {
    // copy from View.generateViewId for API <= 16
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);


    private static final int[] APPCOMPAT_CHECK_ATTRS = {
            androidx.appcompat.R.attr.colorPrimary
    };

    public static void checkAppCompatTheme(Context context) {
        @SuppressLint("ResourceType")
        TypedArray a = context.obtainStyledAttributes(APPCOMPAT_CHECK_ATTRS);
        final boolean failed = !a.hasValue(0);
        a.recycle();
        if (failed) {
            throw new IllegalArgumentException("You need to use a Theme.AppCompat theme "
                    + "(or descendant) with the design library.");
        }
    }

    /**
     * 判断事件触摸点是否在View范围内
     *
     * @param view 要判断的view
     * @param ev   输入事件
     * @return 事件触摸点是否在View范围内
     */
    public static boolean isEventLocationInView(View view, MotionEvent ev) {
        int[] location = {0, 0};
        view.getLocationInWindow(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        return (ev.getX() >= left) && (ev.getX() <= right)
                && (ev.getY() >= top) && (ev.getY() <= bottom);
    }

    /**
     * 判断事件绝对触摸点是否在View范围内
     *
     * @param view 要判断的view
     * @param ev   输入事件
     * @return 事件触摸点是否在View范围内
     */
    public static boolean isEventAbsoluteLocationInView(Context context, View view,
                                                        MotionEvent ev) {
        int statusBarHeight = 0;
        Window window = ((Activity) context).getWindow();
        //如果是非全屏应用，要把statusBar的高度减掉，因为rawY是从0 0 开始，view是从statusBar下方开始
        if ((window.getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN)
                != WindowManager.LayoutParams.FLAG_FULLSCREEN) {
            statusBarHeight = getStatusBarHeight(context);
        }

        int[] location = {0, 0};
        view.getLocationInWindow(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getWidth();
        int bottom = top + view.getHeight();
        return (ev.getRawX() >= left)
                && (ev.getRawX() <= right)
                && (ev.getRawY() - statusBarHeight >= top)
                && (ev.getRawY() - statusBarHeight <= bottom);
    }

    /**
     * 获取系统状态栏高度
     *
     * @param context context
     * @return 系统状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0, statusBarHeight = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return statusBarHeight;
    }
}
