package com.dm.carwebsocket;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dm.carwebsocket.gps.ClientSocket;
import com.dm.carwebsocket.gps.SocketDataParser;
import com.google.gson.Gson;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;

public class MainActivity extends AppCompatActivity implements
        SocketDataParser, ServiceManager.WebSocketReceiveData {

    private static final String TAG = MainActivity.class.getSimpleName();
    private ServiceManager serviceManager;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceManager = new ServiceManager();
        serviceManager.start(7890);
        serviceManager.setReceiveData(this);

        final ClientSocket clientSocket = new ClientSocket();
        clientSocket.setSocketDataParser(this);


        textView = findViewById(R.id.tv);

        TextView ipValue = findViewById(R.id.ipValue);
        ipValue.setText(IPUtils.getLocalIp());

        findViewById(R.id.openGPS).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (!clientSocket.createConnect("192.168.8.27", 4444)) {
                            Log.d(TAG, "run: clientSocket");
                        }
                    }
                }).start();
            }
        });
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voice("讯飞语音播报");
            }
        });

        Log.d(TAG, "onCreate: " + IPUtils.getLocalIp());


        /*语音合成*/
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };


    @Override
    public void dataParser(final String data) {
        if (data.startsWith("$GPRMC")) {
            serviceManager.sendMessageToAll(parseToJson(data));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(data);
                }
            });
        } else {
            Log.d(TAG, "dataParser: " + data);
        }
    }

    public String parseToJson(String data) {
        // $GPRMC,  020250.00,          A,                   2813.9891299,N,            11252.6278784,E ,  0.033,            315.7, 161117,0.0,E,A*30
        // 数据id,  utc时间 Hhmmss.ss, 定位状态：A有效，V无效， 维度， 维度方向 N北纬 S南纬，  经度，E东经 W西经 ， 地面速度单位节， 地面航向，日期 日月年，磁偏角 度数，
        GpRmcBean gpRmcBean = new GpRmcBean(data);
        Gson gson = new Gson();
        return gson.toJson(gpRmcBean);
    }

    @Override
    public void playVoice(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                voice(message);
            }
        });


    }

    private void voice(String msg) {
        Log.d(TAG, "准备点击： " + System.currentTimeMillis());
        int code = mTts.startSpeaking(msg, mTtsListener);
        if (code != ErrorCode.SUCCESS) {
            showTip("语音合成失败,错误码: " + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    }

    // 语音合成对象
    private SpeechSynthesizer mTts;
    //缓冲进度
    private int mPercentForBuffering = 0;
    //播放进度
    private int mPercentForPlaying = 0;
    private Toast mToast;

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            //showTip("开始播放");
            Log.d(TAG, "开始播放：" + System.currentTimeMillis());
        }

        @Override
        public void onSpeakPaused() {
            showTip("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            showTip("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            mPercentForBuffering = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
            showTip(String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                showTip("播放完成");
            } else if (error != null) {
                showTip(error.getPlainDescription(true));
            }
        }


        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_AUDIO_URL);
                Log.d(TAG, "session id =" + sid);
            }
        }
    };

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serviceManager.stop();
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }
}