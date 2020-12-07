package com.dm.carwebsocket.sip;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.dm.carwebsocket.R;
import com.dm.carwebsocket.util.SPUtils;

import org.linphone.core.AccountCreator;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.ProxyConfig;
import org.linphone.core.RegistrationState;
import org.linphone.core.TransportType;


public class ConfigureAccountActivity extends AppCompatActivity {

  private static final String TAG = ConfigureAccountActivity.class.getSimpleName();
  private EditText mUsername, mPassword, mDomain;
  private RadioGroup mTransport;
  private Button mConnect;

  private AccountCreator mAccountCreator;
  private CoreListenerStub mCoreListener;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_configure_account);

    // 帐户创建者可以帮助您创建/配置帐户，甚至不是sip.linphone.org帐户
    // 由于我们只想配置现有帐户，因此无需服务器URL来请求知道帐户是否存在等等...
    mAccountCreator = LinphoneService.getCore().createAccountCreator(null);

    findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
    mUsername = findViewById(R.id.username);
    mPassword = findViewById(R.id.password);
    mDomain = findViewById(R.id.domain);
    mTransport = findViewById(R.id.assistant_transports);

    mConnect = findViewById(R.id.configure);
    mConnect.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                configureAccount();
              }
            });

    mCoreListener = new CoreListenerStub() {
      @Override
      public void onRegistrationStateChanged(Core core, ProxyConfig cfg, RegistrationState state, String message) {
        if (state == RegistrationState.Ok) {
          finish();
        } else if (state == RegistrationState.Failed) {
          Toast.makeText(ConfigureAccountActivity.this,
                  "注册失败: " + message, Toast.LENGTH_SHORT).show();
        }
      }
    };

    initValue();
  }

  @Override
  protected void onStart() {
    super.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    LinphoneService.getCore().addListener(mCoreListener);
  }

  private void initValue() {
    String username = (String) SPUtils.get(this, SPUtils.sip_name, "177");
    mUsername.setText(username);
    String password = (String) SPUtils.get(this, SPUtils.sip_pwd, "177");
    mPassword.setText(password);
    String domain = (String) SPUtils.get(this, SPUtils.sip_domain,
            "192.168.4.7");
    mDomain.setText(domain);
  }

  @Override
  protected void onPause() {
    LinphoneService.getCore().removeListener(mCoreListener);
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  private void configureAccount() {
    // 至少需要以下3个值
    mAccountCreator.setUsername(mUsername.getText().toString());
    mAccountCreator.setDomain(mDomain.getText().toString());
    mAccountCreator.setPassword(mPassword.getText().toString());

    // 默认情况下，如果未设置，则为UDP，但强烈建议使用TLS
    switch (mTransport.getCheckedRadioButtonId()) {
      case R.id.transport_udp:
        mAccountCreator.setTransport(TransportType.Udp);
        break;
      case R.id.transport_tcp:
        mAccountCreator.setTransport(TransportType.Tcp);
        break;
      case R.id.transport_tls:
        mAccountCreator.setTransport(TransportType.Tls);
        break;
      default:
        break;
    }
    removeOldProxyConfig();
    saveInfo(mUsername.getText().toString(), mPassword.getText().toString(), mDomain.getText().toString());
    //这将自动创建代理配置和身份验证信息并将其添加到Core
    ProxyConfig cfg = mAccountCreator.createProxyConfig();
    //确保新创建的是默认值
    LinphoneService.getCore().setDefaultProxyConfig(cfg);
  }

  private void removeOldProxyConfig() {
    ProxyConfig[] list = LinphoneService.getCore().getProxyConfigList();
    for (ProxyConfig bean : list) {
      LinphoneService.getCore().removeProxyConfig(bean);
    }

  }

  private void saveInfo(String username, String password, String domain) {
    SPUtils.put(this, SPUtils.sip_name, username);
    SPUtils.put(this, SPUtils.sip_pwd, password);
    SPUtils.put(this, SPUtils.sip_domain, domain);
  }
}
