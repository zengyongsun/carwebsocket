package com.dm.carwebsocket;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

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
import com.iflytek.cloud.util.ResourceUtil;

/**
 * author : Zeyo
 * e-mail : zengyongsun@163.com
 * date   : 2020/11/11 17:03
 * desc   :
 * version: 1.0
 */
public class WebSocketService extends Service
        implements ServiceManager.WebSocketReceiveData, SocketDataParser {

    public static final String TAG = WebSocketService.class.getSimpleName();
    private ServiceManager serviceManager;

    public static final String webSocketStateAction = "com.dm.carwebsocket.websocket_state";
    public static final String tcpStateAction = "com.dm.carwebsocket.tcp_state";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: 服务启动");
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        if (Build.VERSION.SDK_INT > 25) {
            String channelId = "car_service";
            NotificationChannel channel = new NotificationChannel(channelId, "channel_name", NotificationManager.IMPORTANCE_LOW);

            manager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(this, channelId)
                    .setContentTitle("车载服务器")
                    .setContentText("车载服务器启动")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);
        } else {
            Notification notification = new Notification.Builder(this)
                    .setContentTitle("车载服务器")
                    .setContentText("车载服务器启动")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(pendingIntent)
                    .build();
            startForeground(1, notification);
        }

        serviceManager = new ServiceManager();
        serviceManager.start(7890);
        serviceManager.setReceiveData(this);

        final ClientSocket clientSocket = new ClientSocket();
        clientSocket.setSocketDataParser(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ok = true;
                String host = (String) SPUtils.get(WebSocketService.this,
                        SPUtils.gps_tcp_ip, SPUtils.tcp_ip_default_value);
                while (ok) {
                    if (clientSocket.createConnect(host, 4444)) {
                        ok = false;
                        tcpState("车载服务器：RTC的tcp连接成功");
                    } else {
                        host = (String) SPUtils.get(WebSocketService.this,
                                SPUtils.gps_tcp_ip, SPUtils.tcp_ip_default_value);
                        tcpState("车载服务器：RTC的tcp连接失败，请检查tcpIP");
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        /*语音合成*/
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);
    }

    /**
     * 参数设置
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: 服务销毁");
        serviceManager.stop();
        if (null != mTts) {
            mTts.stopSpeaking();
            // 退出时释放连接
            mTts.destroy();
        }
    }

    @Override
    public void playVoice(String message) {
        voice(message);
    }

    @Override
    public void onStartWebSocket(String result) {
        Intent intent = new Intent(webSocketStateAction);
        intent.putExtra("result", result);
        //加了下面一行收不到动态注册的广播
//        intent.setComponent(new ComponentName("com.dm.carwebsocket", "com.dm.carwebsocket.MainActivity.WebSocketReceiver"));
        sendBroadcast(intent, null);
    }

    public void tcpState(String result) {
        Intent intent = new Intent(tcpStateAction);
        intent.putExtra("result", result);
        sendBroadcast(intent, null);
    }

    @Override
    public void dataParser(final String data) {
        Log.d(TAG, "dataParser: " + data);
        if (data.startsWith("$GPRMC")) {
            serviceManager.sendMessageToAll(parseToJson(data));
        }
    }

    public String parseToJson(String data) {
        // $GPRMC,  020250.00,          A,                   2813.9891299,N,            11252.6278784,E ,  0.033,            315.7, 161117,0.0,E,A*30
        // 数据id,  utc时间 Hhmmss.ss, 定位状态：A有效，V无效， 维度， 维度方向 N北纬 S南纬，  经度，E东经 W西经 ， 地面速度单位节， 地面航向，日期 日月年，磁偏角 度数，
        GpRmcBean gpRmcBean = new GpRmcBean(data);
        Gson gson = new Gson();
        return gson.toJson(gpRmcBean);
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

    private void voice(String msg) {
        Log.d(TAG, "准备点击： " + System.currentTimeMillis());
        setParam();
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
        Log.d(TAG, "showTip: " + str);
    }
}
