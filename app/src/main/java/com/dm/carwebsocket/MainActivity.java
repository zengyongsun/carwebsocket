package com.dm.carwebsocket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dm.carwebsocket.settings.SettingsActivity;
import com.dm.carwebsocket.util.IPUtils;
import com.dm.carwebsocket.util.SPUtils;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView socketState;
    private TextView tcpState;
    private TextView voiceState;
    private TextView ipValueTv;
    private TextView tpcIPTv;

    private WebSocketReceiver receiver;

    // 语音合成对象
    private SpeechSynthesizer mTts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        intiView();
        // 初始化语音合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
        startBroadReceiver();
        onStartService();
    }

    private void intiView() {
        ipValueTv = findViewById(R.id.ipValue);
        tpcIPTv = findViewById(R.id.tcpIp);
        voiceState = findViewById(R.id.voiceState);
        tcpState = findViewById(R.id.tcpState);
        socketState = findViewById(R.id.webSocketState);

        findViewById(R.id.btVoice).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voice();
            }
        });

        findViewById(R.id.goSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
    }

    /**
     * 服务通过这个广播和Activity通讯
     */
    private void startBroadReceiver() {
        receiver = new WebSocketReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WebSocketService.tcpStateAction);
        filter.addAction(WebSocketService.webSocketStateAction);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ipValueTv.setText("本机IP：" + IPUtils.getLocalIp());
        tpcIPTv.setText("RTK模块IP：" + SPUtils.get(this, SPUtils.gps_tcp_ip,
                SPUtils.tcp_ip_default_value));
    }

    private void onStartService() {
        Intent intent = new Intent(this, WebSocketService.class);
        startService(intent);
    }

    private void onStopService() {
        Intent intent = new Intent(this, WebSocketService.class);
        startService(intent);
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                voiceState.setText("初始化失败,错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
                voiceState.setText("语音播报模块成功");
            }
        }
    };

    private void voice() {
        setParam();
        int code = mTts.startSpeaking("语音播报正常", null);
        if (code != ErrorCode.SUCCESS) {
            voiceState.setText("语音合成失败,错误码: " + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    }

    /**
     * 语音合成参数设置，不设置默认使用的是云语音合成，而不是离线的语音合成
     */
    private void setParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置使用本地引擎
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        //设置发音人资源路径
        mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
        //设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
    }

    //获取发音人资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        String type = "tts";
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, type + "/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, type + "/" + "xiaoyan" + ".jet"));
        return tempBuffer.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: activity 销毁");
        //注销广播
        unregisterReceiver(receiver);
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }

    public class WebSocketReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String result = intent.getStringExtra("result");
            if (WebSocketService.webSocketStateAction.equals(action)) {
                socketState.setText(result);
            } else if (WebSocketService.tcpStateAction.equals(action)) {
                tcpState.setText(result);
            }
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }

}