package com.dm.carwebsocket.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.dm.carwebsocket.R;
import com.dm.carwebsocket.SPUtils;

public class SettingsActivity extends AppCompatActivity {

    private EditText tcpIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tcpIp = findViewById(R.id.gpsIp);
        findViewById(R.id.btGps).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SPUtils.put(SettingsActivity.this, SPUtils.gps_tcp_ip,
                        tcpIp.getText().toString());
            }
        });
        String ip = (String) SPUtils.get(this, SPUtils.gps_tcp_ip, SPUtils.tcp_ip_default_value);
        tcpIp.setText(ip);
    }
}