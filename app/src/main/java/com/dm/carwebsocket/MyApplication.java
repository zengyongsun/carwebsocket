package com.dm.carwebsocket;

import android.app.Application;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 应用程序入口处调用,避免手机内存过小,杀死后台进程后通过历史intent进入Activity造成SpeechUtility对象为null
        // 注意：此接口在非主进程调用会返回null对象，如需在非主进程使用语音功能，请增加参数：SpeechConstant.FORCE_LOGIN+"=true"
        // 参数间使用“,”分隔。
        // 设置你申请的应用appid

        String param = "appid=" + getString(R.string.app_id) +
                "," +
                // 设置使用v5+
                SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC +
                SpeechConstant.FORCE_LOGIN + "=true";
        SpeechUtility.createUtility(this, param);
    }
}
