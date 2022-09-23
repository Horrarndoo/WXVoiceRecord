package com.example.wxvoicerecord.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

/**
 * Created by Horrarndoo on 2022/9/23.
 * <p>
 * 沉浸式dialog基类
 */
public abstract class BaseImmerseDialog extends Dialog {
    public BaseImmerseDialog(@NonNull Context context) {
        this(context, 0);
    }

    public BaseImmerseDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        Window window = getWindow();
        window.getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams layoutParams = window.getAttributes();//获取dialog布局的参数
        if (getBackgroundDrawable() != null) {
            window.setBackgroundDrawable(getBackgroundDrawable());
        }
        //全屏
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        //全屏
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        //设置导航栏透明
        window.setNavigationBarColor(Color.TRANSPARENT);
        //内容扩展到导航栏
        window.setType(WindowManager.LayoutParams.TYPE_APPLICATION_PANEL);
        if (Build.VERSION.SDK_INT >= 28) {
            layoutParams.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        window.setAttributes(layoutParams);
        init();
    }

    protected Drawable getBackgroundDrawable() {
        return null;
    }

    protected int getSystemUiVisibility() {
        return View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
    }

    protected abstract int getLayoutId();

    protected abstract void init();
}