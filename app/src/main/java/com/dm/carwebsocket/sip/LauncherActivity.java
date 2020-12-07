package com.dm.carwebsocket.sip;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;


public class LauncherActivity extends AppCompatActivity {

    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_launcher);
        mHandler = new Handler();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 检查服务是否已在运行
        if (LinphoneService.isReady()) {
            onServiceReady();
        } else {
            //如果没有启动，那就开启服务
            startService(new Intent().setClass(LauncherActivity.this,
                    LinphoneService.class));
            //并等待它准备好，所以我们可以安全地使用它
            new ServiceWaitThread().start();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void onServiceReady() {
        //一旦服务准备就绪，我们就可以继续申请了
        //我们将转发意图动作，类型和附加内容，以便进行处理
        //如果需要，通过下一个活动，这不是启动工作
        Intent intent = new Intent();
//        intent.setClass(LauncherActivity.this, LoginActivity.class);
        if (getIntent() != null && getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
            intent.setAction(getIntent().getAction());
            intent.setType(getIntent().getType());
        }
        startActivity(intent);
        finish();
    }


    /**
     * 该线程将定期检查服务是否准备就绪，然后调用onServiceReady
     */
    private class ServiceWaitThread extends Thread {
        @Override
        public void run() {
            while (!LinphoneService.isReady()) {
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException("waiting thread sleep() has been interrupted");
                }
            }
            // 子线程中，不能操作UI，所以通过 getHandler
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onServiceReady();
                }
            });
        }
    }
}
