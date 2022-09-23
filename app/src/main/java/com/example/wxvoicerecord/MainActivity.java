package com.example.wxvoicerecord;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wxvoicerecord.adapter.VoiceMsgAdapter;
import com.example.wxvoicerecord.bean.VoiceMsg;
import com.example.wxvoicerecord.voice.RecordButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    //申请录音权限
    private static final int GET_RECODE_AUDIO = 1;
    private static String[] needPermissions = {
            Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private List<VoiceMsg> voiceMsgList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyAudioPermissions(this);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        VoiceMsgAdapter adapter = new VoiceMsgAdapter(voiceMsgList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        RecordButton recordButton = findViewById(R.id.btn_record);
        recordButton.setmOnFinishedRecordListener(new RecordButton.OnFinishedRecordListener() {
            @Override
            public void onFinishedRecord(String audioPath, int time) {
                VoiceMsg msg = new VoiceMsg(audioPath, time, System.currentTimeMillis());
                voiceMsgList.add(msg);
                adapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(voiceMsgList.size() - 1);
            }
        });
    }

    /*
     * 申请录音权限*/
    public static void verifyAudioPermissions(Activity activity) {
        int permissionGet = 0;
        for (int i = 0; i < needPermissions.length; i++) {
            permissionGet += ActivityCompat.checkSelfPermission(activity,
                    needPermissions[i]);
        }
        if (permissionGet != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, needPermissions,
                    GET_RECODE_AUDIO);
        }
    }
}