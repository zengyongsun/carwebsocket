package com.dm.carwebsocket.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.dm.carwebsocket.R;
import com.dm.carwebsocket.util.SPUtils;

public class SettingsActivity extends AppCompatActivity {

    private EditText tcpIp;
    private EditText updateIp;
    private EditText updatePort;

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


        updateIp = findViewById(R.id.updateIp);
        updatePort = findViewById(R.id.updatePort);

        findViewById(R.id.btUpdate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SPUtils.put(SettingsActivity.this, SPUtils.UPDATE_IP,
                        updateIp.getText().toString());
                SPUtils.put(SettingsActivity.this, SPUtils.UPDATE_PORT,
                        updatePort.getText().toString());
            }
        });
        String update_Ip = (String) SPUtils.get(this, SPUtils.UPDATE_IP, SPUtils.update_ip_default_value);
        String update_Port = (String) SPUtils.get(this, SPUtils.UPDATE_PORT, SPUtils.update_port_default_value);
        updateIp.setText(update_Ip);
        updatePort.setText(update_Port);
    }
}