package com.example.wxvoicerecord.adapter;

import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.wxvoicerecord.R;
import com.example.wxvoicerecord.bean.VoiceMsg;
import com.example.wxvoicerecord.voice.MediaManager;

import java.text.SimpleDateFormat;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author zhuguohui
 * @description:
 * @date :2021/4/29 10:58
 */
public class VoiceMsgAdapter extends RecyclerView.Adapter<VoiceMsgAdapter.BaseViewHolder> {

    private List<VoiceMsg> msgList;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public VoiceMsgAdapter(List<VoiceMsg> msgList) {
        this.msgList = msgList;
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_msg, parent, false);
        return new BaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        VoiceMsg item = msgList.get(position);
        //根据语音时长设置气泡的宽度
        //chat_item_layout_content
        View layout = holder.itemView.findViewById(R.id.chat_item_layout_content);
        //最少长度dp_72,最大_242 ,变换空间0-170
        int min = (int) layout.getResources().getDimension(R.dimen.dp_100);
        int changeSpace = (int) layout.getResources().getDimension(R.dimen.dp_200);
        float scale = (float) (item.getDuration() * 1.0 / 60);

        //将scale的值控制在0-1.0之间
        scale = Math.min(1.0f, scale);
        scale = Math.max(0, scale);

        int width = (int) (min + scale * changeSpace);
        ViewGroup.LayoutParams params = layout.getLayoutParams();
        if (params != null) {
            params.width = width;
            layout.setLayoutParams(params);
        }
        TextView tvDuration=holder.itemView.findViewById(R.id.tvDuration);
        tvDuration.setText(item.getDuration()+"''");

        //设置时间
        String time = sdf.format(item.getTime());
        TextView tvTime = holder.itemView.findViewById(R.id.chat_item_time);
        tvTime.setText(time);

        holder.itemView.setOnClickListener(v -> playAudio(v, position));
    }

    //正在播放的控件
    ImageView ivAudio = null;

    private void playAudio(View view, int position) {
        VoiceMsg item = msgList.get(position);
        ImageView newAudioView = view.findViewById(R.id.ivAudio);
        boolean justStop = newAudioView == ivAudio;
        if (ivAudio != null) {
            ivAudio.setBackgroundResource(R.drawable.ic_vector_voice_left_level_3);
            MediaManager.getInstance().reset();
        }
        if (justStop) {
            //点击正在播放的View 只停止播放
            return;
        }

        ivAudio = newAudioView;
        MediaManager.getInstance().reset();

        ivAudio.setBackgroundResource(R.drawable.voice_animation_left_list);
        AnimationDrawable drawable = (AnimationDrawable) ivAudio.getBackground();
        drawable.start();
        MediaManager.getInstance().playVoice(item.getPath(), mp -> {
            ivAudio.setBackgroundResource(R.drawable.ic_vector_voice_left_level_3);
            MediaManager.getInstance().release();
            ivAudio = null;
        });

    }

    @Override
    public int getItemCount() {
        return msgList.size();
    }

    public static class BaseViewHolder extends RecyclerView.ViewHolder {

        public BaseViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
} 