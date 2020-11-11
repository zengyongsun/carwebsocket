package com.dm.carwebsocket;

import android.app.Application;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

public class MyApplication extends Application implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;

    @Override
    public void onCreate() {
        super.onCreate();
        initSpeech();
    }

    public void initSpeech() {
        textToSpeech = new TextToSpeech(getApplicationContext(), this);
        // 设置音调，值越大声音越尖（女生），值越小则变成男声,1.0是常规
        textToSpeech.setPitch(0.5f);
        //设定语速 ，默认1.0正常语速
        textToSpeech.setSpeechRate(1.0f);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.CHINA);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "数据丢失或不支持", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void voice(String str) {
        //QUEUE_FLUSH方式表示清除当前队列中的内容而直接播放新的内容，
        // QUEUE_ADD方式表示将新的内容添加到队列尾部进行播放
        textToSpeech.speak(str, TextToSpeech.QUEUE_ADD, null);
    }
}
