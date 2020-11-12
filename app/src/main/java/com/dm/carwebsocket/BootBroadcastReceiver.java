package com.dm.carwebsocket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * author : Zeyo
 * e-mail : zengyongsun@163.com
 * date   : 2020/10/28 14:32
 * desc   : 启动的广播
 * version: 1.0
 */
public class BootBroadcastReceiver extends BroadcastReceiver {

    static final String ACTION = "android.intent.action.BOOT_COMPLETED";
    private static final String TAG = BootBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION.equals(intent.getAction())) {
            Intent startIntent = new Intent(context, MainActivity.class);
            context.startActivity(startIntent);
        }
        Log.d(TAG, "onReceive: 调用了");
    }
}
