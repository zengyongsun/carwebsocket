package com.dm.carwebsocket.sip;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.dm.carwebsocket.R;

import org.linphone.core.AuthInfo;
import org.linphone.core.ProxyConfig;


public class InfoSipActivity extends AppCompatActivity implements View.OnClickListener {

  TextView content;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_info_sip);
    content = findViewById(R.id.content);
    findViewById(R.id.back).setOnClickListener(this);
    findViewById(R.id.delete).setOnClickListener(this);
    initView();
  }

  private void initView() {
    ProxyConfig[] list = LinphoneService.getCore().getProxyConfigList();
    for (ProxyConfig bean : list) {
      AuthInfo authInfo = bean.findAuthInfo();
      if (authInfo != null) {
        content.setText(content.getText().toString() + bean.findAuthInfo().getUsername() + "\n");
      }
    }
  }


  public void onDeleteClicked() {
    ProxyConfig[] list = LinphoneService.getCore().getProxyConfigList();
    for (ProxyConfig bean : list) {
      LinphoneService.getCore().removeProxyConfig(bean);
    }
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    switch (id) {
      case R.id.back:
        finish();
        break;
      case R.id.delete:
        onDeleteClicked();
        break;
    }
  }
}
