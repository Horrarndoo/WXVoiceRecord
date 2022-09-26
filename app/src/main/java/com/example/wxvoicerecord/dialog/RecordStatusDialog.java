package com.example.wxvoicerecord.dialog;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.wxvoicerecord.R;
import com.example.wxvoicerecord.utils.NavigationBarUtils;
import com.example.wxvoicerecord.voice.VoiceStatusPanel;

import androidx.annotation.NonNull;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

/**
 * Created by Horrarndoo on 2022/9/23.
 * <p>
 */
public class RecordStatusDialog extends BaseImmerseDialog {
    private volatile boolean isFirstShow = true;
    private LinearLayout llBottom;
    private TextView tvCancel;
    private TextView tvSend;
    private ImageView ivRecord;
    private ImageView ivCancel;
    private VoiceStatusPanel voiceStatusPanel;
    /**
     * 图标初始宽度（用于动画）
     */
    private int mOriginIconWidth;
    /**
     * 图标初始高度（用于动画）
     */
    private int mOriginIconHeight;

    public RecordStatusDialog(@NonNull Context context) {
        this(context, R.style.record_dialog_style);
    }

    public RecordStatusDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_record_status;
    }

    @Override
    protected void init() {
        voiceStatusPanel = findViewById(R.id.voice_status_panel);
        tvCancel = findViewById(R.id.tv_cancel);
        tvSend = findViewById(R.id.tv_send);
        llBottom = findViewById(R.id.ll_bottom);
        ivRecord = findViewById(R.id.iv_record);
        ivCancel = findViewById(R.id.iv_cancel);
        tvCancel.setVisibility(INVISIBLE);

        mOriginIconWidth = ivCancel.getLayoutParams().width;
        mOriginIconHeight = ivCancel.getLayoutParams().height;

        //因为使用了沉浸式，如果有navigaitionBar的时候，给一个margin
        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) llBottom.getLayoutParams();
        layoutParams.bottomMargin = NavigationBarUtils.getNavigationBarHeight(getContext());
        llBottom.requestLayout();
    }

    /**
     * 显示正常录音状态
     */
    @SuppressLint({"UseCompatLoadingForDrawables", "NewApi"})
    public void showRecordingNormal() {
        if (!isShowing()) {
            return;
        }

        tvCancel.setVisibility(INVISIBLE);
        tvSend.setVisibility(VISIBLE);
        voiceStatusPanel.setCancel(false);
        llBottom.setBackground(getContext().getDrawable(R.drawable.layer_list_record_bottom_bg_selected));
        ivRecord.setColorFilter(getContext().getColor(R.color.record_selected_bottom_icon_color));
        ivCancel.setBackground(getContext().getDrawable(R.drawable.shape_record_cancel_button_unselected_bg));
        ivCancel.setColorFilter(getContext().getColor(R.color.record_cancel_button_unselected_tint_color));
        if (isFirstShow) {
            isFirstShow = false;
            return;
        }
        setCancelImageZoom(false);
    }

    /**
     * 显示松手取消录音状态
     */
    @SuppressLint({"UseCompatLoadingForDrawables", "NewApi"})
    public void showRecordingCancel() {
        if (!isShowing()) {
            return;
        }

        tvCancel.setVisibility(VISIBLE);
        tvSend.setVisibility(INVISIBLE);
        voiceStatusPanel.setCancel(true);
        llBottom.setBackground(getContext().getDrawable(R.drawable.layer_list_record_bottom_bg_unselected));
        ivRecord.setColorFilter(getContext().getColor(R.color.record_unselected_bottom_icon_color));
        ivCancel.setBackground(getContext().getDrawable(R.drawable.shape_record_cancel_button_selected_bg));
        ivCancel.setColorFilter(getContext().getColor(R.color.record_cancel_button_selected_tint_color));
        if (isFirstShow) {
            isFirstShow = false;
            return;
        }
        setCancelImageZoom(true);
    }

    /**
     * 更新面板文字内容
     *
     * @param text 文字内容
     */
    public void updatePanelText(String text) {
        if (voiceStatusPanel == null) {
            return;
        }
        voiceStatusPanel.setTextContent(text);
    }

    /**
     * 更新面板声音分贝值
     *
     * @param db 分贝值
     */
    public void updatePanelVoiceDb(int db) {
        if (voiceStatusPanel == null) {
            return;
        }
        voiceStatusPanel.updateVoiceDb(db);
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

    @Override
    public void dismiss() {
        super.dismiss();
        if (voiceStatusPanel != null) {
            voiceStatusPanel.destroy();
            voiceStatusPanel = null;
        }
    }

    /**
     * 底部容器，用于外部判断触点是否超出松手范围
     *
     * @return 底部容器
     */
    public LinearLayout getBottomLayout() {
        return llBottom;
    }
}
