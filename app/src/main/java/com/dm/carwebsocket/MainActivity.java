package com.dm.carwebsocket;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dm.carwebsocket.gps.ClientSocket;
import com.dm.carwebsocket.gps.SocketDataParser;
import com.dm.carwebsocket.settings.SettingsActivity;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity implements SocketDataParser, ServiceManager.WebSocketReceiveData {

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
        startActivity(new Intent(MainActivity.this, SettingsActivity.class));
      }
    });

    Log.d(TAG, "onCreate: " + IPUtils.getLocalIp());
  }

  private void voice(String msg) {
    ((MyApplication) getApplication()).voice(msg);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    serviceManager.stop();
  }

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
  public void playVoice(String message) {
    textView.setText(message);
    voice(message);
  }
}