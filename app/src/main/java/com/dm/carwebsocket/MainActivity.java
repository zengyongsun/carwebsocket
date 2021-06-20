package com.dm.carwebsocket;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.dm.carwebsocket.settings.SettingsActivity;
import com.dm.carwebsocket.sip.ConfigureAccountActivity;
import com.dm.carwebsocket.sip.InfoSipActivity;
import com.dm.carwebsocket.sip.LinphoneService;
import com.dm.carwebsocket.util.AppUtils;
import com.dm.carwebsocket.util.IPUtils;
import com.dm.carwebsocket.util.OkGoUpdateHttpUtil;
import com.dm.carwebsocket.util.SPUtils;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.util.ResourceUtil;
import com.vector.update_app.UpdateAppBean;
import com.vector.update_app.UpdateAppManager;
import com.vector.update_app.UpdateCallback;
import com.vector.update_app.service.DownloadService;
import com.vector.update_app.utils.AppUpdateUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import static com.vector.update_app.utils.Md5Util.getFileMD5;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView socketState;
    private TextView tvVersion;
    private TextView tcpState;
    private TextView voiceState;
    private TextView ipValueTv;
    private TextView tpcIPTv;
    private TextView sipSocketTv;

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
        tvVersion = findViewById(R.id.tvVersion);
        ipValueTv = findViewById(R.id.ipValue);
        tpcIPTv = findViewById(R.id.tcpIp);
        voiceState = findViewById(R.id.voiceState);
        tcpState = findViewById(R.id.tcpState);
        socketState = findViewById(R.id.webSocketState);
        sipSocketTv = findViewById(R.id.sipSocketState);

        findViewById(R.id.btVoice).setOnClickListener(this);
        findViewById(R.id.goSettings).setOnClickListener(this);
        findViewById(R.id.SipSettings).setOnClickListener(this);
        findViewById(R.id.SipClear).setOnClickListener(this);
        findViewById(R.id.showLog).setOnClickListener(this);
        findViewById(R.id.update).setOnClickListener(this);
        findViewById(R.id.pingIp).setOnClickListener(this);

        String tcpDesc = (String) SPUtils.get(this, SPUtils.tcp_desc, "");
        String sipDesc = (String) SPUtils.get(this, SPUtils.sip_desc, "");
        String rtcDesc = (String) SPUtils.get(this, SPUtils.rtc_desc, "");
        socketState.setText(rtcDesc);
        tcpState.setText(tcpDesc);
        sipSocketTv.setText(sipDesc);
    }

    /**
     * 服务通过这个广播和Activity通讯
     */
    private void startBroadReceiver() {
        receiver = new WebSocketReceiver();
        IntentFilter filter = new IntentFilter();
        //gsp tcp的状态
        filter.addAction(WebSocketService.tcpStateAction);
        //webSocket的状态
        filter.addAction(WebSocketService.webSocketStateAction);
        //sip服务状态
        filter.addAction(LinphoneService.sipStateAction);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        ipValueTv.setText("本机IP：" + IPUtils.getLocalIp());
        tpcIPTv.setText("RTK模块IP：" + SPUtils.get(this, SPUtils.gps_tcp_ip,
                SPUtils.tcp_ip_default_value));
        tvVersion.setText(AppUtils.getVersionName(this));
        //检测是否需要更新
        isNeedUpdate();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndRequestCallPermissions();
    }

    private void onStartService() {
        Intent intent = new Intent(this, WebSocketService.class);
        startService(intent);
        // 检查服务是否已在运行
        if (!LinphoneService.isReady()) {
            //如果没有启动，那就开启服务
            startService(new Intent().setClass(MainActivity.this,
                    LinphoneService.class));
        }
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

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btVoice:
                voice();
                break;
            case R.id.goSettings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.SipSettings:
                startActivity(new Intent(MainActivity.this, ConfigureAccountActivity.class));
                break;
            case R.id.SipClear:
                startActivity(new Intent(MainActivity.this, InfoSipActivity.class));
                break;
            case R.id.showLog:
                startActivity(new Intent(MainActivity.this, ShowLogActivity.class));
                break;
            case R.id.update:
                isNeedUpdate();
                break;
            case R.id.pingIp:
                startActivity(new Intent(MainActivity.this, PingIpActivity.class));
                break;
            default:
                Toast.makeText(MainActivity.this, "未知的按钮", Toast.LENGTH_SHORT).show();
                break;
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
            } else if (LinphoneService.sipStateAction.equals(action)) {
                sipSocketTv.setText(result);
            }
            ipValueTv.setText("本机IP：" + IPUtils.getLocalIp());
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();
        }
    }


    // 权限请求
    private void checkAndRequestCallPermissions() {
        ArrayList<String> permissionsList = new ArrayList<>();
        int recordAudio = getPackageManager().checkPermission(Manifest.permission.RECORD_AUDIO, getPackageName());
        int camera = getPackageManager().checkPermission(Manifest.permission.CAMERA, getPackageName());
        int sip = getPackageManager().checkPermission(Manifest.permission.USE_SIP, getPackageName());
        int storage = getPackageManager().checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, getPackageName());
        if (recordAudio != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "[Permission] Asking for record audio");
            permissionsList.add(Manifest.permission.RECORD_AUDIO);
        }

        if (storage != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "[Permission] Asking for record audio");
            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (sip != PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "[Permission] Asking for camera");
            permissionsList.add(Manifest.permission.USE_SIP);
        }

        if (permissionsList.size() > 0) {
            String[] permissions = new String[permissionsList.size()];
            permissions = permissionsList.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        // Callback for when permissions are asked to the user
        for (int i = 0; i < permissions.length; i++) {
            Log.i(TAG,
                    "[Permission] "
                            + permissions[i]
                            + " is "
                            + (grantResults[i] == PackageManager.PERMISSION_GRANTED
                            ? "granted"
                            : "denied"));
        }
    }

    private void isNeedUpdate() {
        //获取服务器的版本，看是否需要更新,后台下载apk
        checkUpdate();

    }

    private void checkUpdate() {
        String host_ip = "http://" + SPUtils.get(this, SPUtils.UPDATE_IP, SPUtils.update_ip_default_value) + ":"
                + SPUtils.get(this, SPUtils.UPDATE_PORT, SPUtils.update_port_default_value);
        String url = "/update_message.json";
        String path = "";
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ||
                !Environment.isExternalStorageRemovable()) {
            try {
                path = Objects.requireNonNull(getExternalCacheDir()).getAbsolutePath();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (TextUtils.isEmpty(path)) {
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            }
        } else {
            path = getCacheDir().getAbsolutePath();
        }
        new UpdateAppManager
                .Builder()
                //当前Activity
                .setActivity(this)
                //更新地址
                .setUpdateUrl(host_ip + url)
                //实现httpManager接口的对象
                .setHttpManager(new OkGoUpdateHttpUtil())
                //apk的保存路径
                .setTargetPath(path)
                .build()
                .checkNewApp(new UpdateCallback() {
                    @Override
                    protected void hasNewApp(UpdateAppBean updateApp, UpdateAppManager updateAppManager) {
                        Log.d(TAG, "hasNewApp: " + updateApp.toString());

                        if (!AppUtils.needUpdate(MainActivity.this, updateApp.getNewVersion())) {
                            return;
                        }
                        //添加信息
                        final UpdateAppBean updateAppBean = updateAppManager.fillUpdateAppData();
                        //设置不显示通知栏下载进度
                        updateAppBean.dismissNotificationProgress(true);

                        final File appFile = AppUpdateUtils.getAppFile(updateAppBean);
                        String md5 = getFileMD5(appFile);
                        Log.d("UpdateAppManager", "文件 md5 = " + md5 + "服务器 md5 = " + updateAppBean.getNewMd5());
                        if (updateAppBean.getNewMd5().equalsIgnoreCase(md5)) {
                            AppUpdateUtils.installApp(MainActivity.this, appFile);
                        } else {
                            updateAppManager.download(new DownloadService.DownloadCallback() {
                                @Override
                                public void onStart() {

                                }

                                @Override
                                public void onProgress(float progress, long totalSize) {

                                }

                                @Override
                                public void setMax(long totalSize) {

                                }

                                @Override
                                public boolean onFinish(File file) {
                                    String md5 = getFileMD5(appFile);
                                    if (updateAppBean.getNewMd5().equalsIgnoreCase(md5)) {
                                        Log.d("UpdateAppManager", file.getAbsolutePath());
                                        AppUpdateUtils.installApp(MainActivity.this, appFile);
                                    } else {
                                        Log.d("UpdateAppManager", "下载的文件md5匹配不上");
                                    }
                                    return false;
                                }


                                @Override
                                public void onError(String msg) {

                                }

                                @Override
                                public boolean onInstallAppAndAppOnForeground(File file) {
                                    return false;
                                }
                            });
                        }

                    }
                });

    }
}