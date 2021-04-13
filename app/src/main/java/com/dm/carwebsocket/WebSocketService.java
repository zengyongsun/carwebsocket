package com.dm.carwebsocket;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.dm.carwebsocket.gps.ClientSocket;
import com.dm.carwebsocket.gps.GpRmcBean;
import com.dm.carwebsocket.gps.KSXTBean;
import com.dm.carwebsocket.gps.RXObserver;
import com.dm.carwebsocket.util.SPUtils;
import com.dm.carwebsocket.websocket.ServiceManager;
import com.google.gson.Gson;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.util.ResourceUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * author : Zeyo
 * e-mail : zengyongsun@163.com
 * date   : 2020/11/11 17:03
 * desc   :
 * version: 1.0
 */
public class WebSocketService extends Service
        implements ServiceManager.WebSocketReceiveData, ClientSocket.ConnectState {

    public static final String TAG = WebSocketService.class.getSimpleName();
    private ServiceManager serviceManager;

    public static final String webSocketStateAction = "com.dm.carwebsocket.websocket_state";
    public static final String tcpStateAction = "com.dm.carwebsocket.tcp_state";
    private ClientSocket clientSocket;
    ExecutorService executorService =  Executors.newFixedThreadPool(3);
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: 服务启动");
        notification();
        serviceManager = new ServiceManager();
        serviceManager.start(7890);
        serviceManager.setReceiveData(this);

        clientSocket = ClientSocket.getInstance();
        clientSocket.setState(this);
        createConnect(clientSocket);

        // 初始化语音合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, null);
        Log.d(TAG, "onCreate#analysisData: " + Thread.currentThread());
    }

    private void notification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        if (Build.VERSION.SDK_INT > 25) {
            String channelId = "car_service";
            NotificationChannel channel = new NotificationChannel(channelId, "channel_name",
                    NotificationManager.IMPORTANCE_LOW);
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
    }

    RXObserver rxObserver = new RXObserver() {
        @Override
        public void analysisData(String msgTran) {
            Log.d(TAG, "analysisData: " + Thread.currentThread());
            serviceManager.sendMessageToAll(parseKSXTToJson(msgTran));
        }
    };

    private void createConnect(final ClientSocket clientSocket) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                boolean ok = true;
                String host = (String) SPUtils.get(WebSocketService.this,
                        SPUtils.gps_tcp_ip, SPUtils.tcp_ip_default_value);
//                String host = "192.168.4.7";
//                String host = "221.131.74.200";
                while (ok) {
                    if (clientSocket.createConnect(host, 4444)) {
                        ok = false;
                        tcpState(Thread.currentThread().getName()+"车载服务器：RTC的TCP连接成功");
                        clientSocket.registerObserver(rxObserver);
                    } else {
                        host = (String) SPUtils.get(WebSocketService.this,
                                SPUtils.gps_tcp_ip, SPUtils.tcp_ip_default_value);
                        tcpState(Thread.currentThread().getName()+"车载服务器：RTK的TCP连接失败，请检查应用的RTK IP配置");
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
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
        tempBuffer.append(ResourceUtil.generateResourcePath(this,
                ResourceUtil.RESOURCE_TYPE.assets, type + "/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this,
                ResourceUtil.RESOURCE_TYPE.assets, type + "/" + "xiaoyan" + ".jet"));
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
        SPUtils.put(this, SPUtils.rtc_desc, result);
        Intent intent = new Intent(webSocketStateAction);
        intent.putExtra("result", result);
        //加了下面一行收不到动态注册的广播
//        intent.setComponent(new ComponentName("com.dm.carwebsocket", "com.dm.carwebsocket.MainActivity.WebSocketReceiver"));
        sendBroadcast(intent, null);
    }

    public void tcpState(String result) {
        SPUtils.put(this, SPUtils.tcp_desc, result);
        Intent intent = new Intent(tcpStateAction);
        intent.putExtra("result", result);
        sendBroadcast(intent, null);
    }

    public String parseToJson(String data) {
        GpRmcBean gpRmcBean = new GpRmcBean(data);
        Gson gson = new Gson();
        return gson.toJson(gpRmcBean);
    }

    public String parseKSXTToJson(String data) {
        KSXTBean ksxt = null;
        try {
            ksxt = new KSXTBean(data);
        } catch (Exception e) {
            ksxt = new KSXTBean();
            Toast.makeText(this, "数据解析出错", Toast.LENGTH_SHORT).show();
        }
        Gson gson = new Gson();
        return gson.toJson(ksxt);
    }


    private void voice(String msg) {
        setParam();
        mTts.startSpeaking(msg, null);
    }

    // 语音合成对象
    private SpeechSynthesizer mTts;

    @Override
    public void reconnect() {
        //socket连接断开的回调
        createConnect(clientSocket);
    }

    @Override
    public void message(byte[] str) {

    }

}
