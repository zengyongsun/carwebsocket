package com.dm.carwebsocket.sip;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.dm.carwebsocket.R;
import com.dm.carwebsocket.util.SPUtils;
import com.dm.carwebsocket.websocket.SipDataBean;
import com.dm.carwebsocket.websocket.SipSocketManger;

import org.json.JSONException;
import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.CallParams;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Factory;
import org.linphone.core.LogCollectionState;
import org.linphone.core.ProxyConfig;
import org.linphone.core.RegistrationState;
import org.linphone.core.tools.Log;
import org.linphone.mediastream.Version;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

public class LinphoneService extends Service implements SipSocketManger.SipSocketReceiveData {

  private static final String START_LINPHONE_LOGS = " ==== Device information dump ====";

  public static final String sipStateAction = "com.dm.carwebsocket.sip_state";


  /**
   * 保持对服务的静态引用，以便我们可以从应用程序的任何位置访问它
   */
  private static LinphoneService sInstance;

  private Handler mHandler;
  private Timer mTimer;

  private Core mCore;
  private CoreListenerStub mCoreListener;
  private SipSocketManger sipSocketManger;
  private Call currentCall;
  private AudioManager mAudioManager;

  public static boolean isReady() {
    return sInstance != null;
  }

  public static LinphoneService getInstance() {
    return sInstance;
  }

  public static Core getCore() {
    return sInstance.mCore;
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  RegistrationState currentState;

  @Override
  public void onCreate() {
    super.onCreate();

    // 第一次调用liblinphone SDK必须是Factory方法.因此，让我们启用库调试日志和日志收集
    String basePath = getFilesDir().getAbsolutePath();
    Factory.instance().setLogCollectionPath(basePath);
    Factory.instance().enableLogCollection(LogCollectionState.Enabled);
    Factory.instance().setDebugMode(true, getString(R.string.app_name));

    // 转储有关我们正在运行的设备的一些有用信息
    Log.i(START_LINPHONE_LOGS);
    dumpDeviceInformation();
    dumpInstalledLinphoneInformation();
    mAudioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
    mHandler = new Handler();
    // 这将是我们的主要核心监听，它将根据事件改变活动
    mCoreListener = new CoreListenerStub() {
      Call.State last = null;

      @Override
      public void onRegistrationStateChanged(Core lc, ProxyConfig cfg, RegistrationState cstate,
                                             String message) {
        super.onRegistrationStateChanged(lc, cfg, cstate, message);
        currentState = cstate;

      }

      @Override
      public void onCallStateChanged(Core core, Call call, Call.State state, String message) {
        android.util.Log.d("LinphoneService", state + " " + message);
        currentCall = call;
        if (state == Call.State.IncomingReceived) {
          // 对于此示例，我们将自动应答来电
          //此统计信息表示已建立呼叫，让我们开始呼叫活动
          SipDataBean bean = new SipDataBean();
          bean.action = action_incoming;
          bean.number = call.getRemoteAddress().getUsername();
          try {
            sipSocketManger.sendMessageToAll(bean.calling());
          } catch (JSONException e) {
            e.printStackTrace();
          }
        } else if (state == Call.State.OutgoingRinging) {
          //呼叫，对方响铃
          SipDataBean bean = new SipDataBean();
          bean.action = action_call;
          bean.result = true;
          bean.desc = "响铃中";
          try {
            sipSocketManger.sendMessageToAll(bean.respCall());
          } catch (JSONException e) {
            e.printStackTrace();
          }
        } else if (state == Call.State.Connected) {
//          core.setPlaybackDevice("STREAM_MUSIC");
          routeAudioToSpeaker();
//          android.util.Log.d("LinphoneService", ""+mAudioManager.isSpeakerphoneOn());
//          android.util.Log.d("LinphoneService", ""+core.getPlaybackGainDb());
          core.setPlaybackGainDb(1.0f);
//          android.util.Log.d("LinphoneService", ""+core.getPlaybackGainDb());
          //接通状态
          SipDataBean bean = new SipDataBean();
          bean.action = action_connected;
          try {
            sipSocketManger.sendMessageToAll(bean.connect());
          } catch (JSONException e) {
            e.printStackTrace();
          }
        } else if (state == Call.State.End) {
          SipDataBean bean = new SipDataBean();
          bean.action = action_hang_up;
          try {
            sipSocketManger.sendMessageToAll(bean.connect());
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }

        if (state == Call.State.Error) {
          SipDataBean bean = new SipDataBean();
          if (last == Call.State.OutgoingRinging) {
            bean.action = action_call;
            bean.result = false;
            bean.desc = "对方拒绝";
            try {
              sipSocketManger.sendMessageToAll(bean.respCall());
            } catch (JSONException e) {
              e.printStackTrace();
            }
          } else {
            bean.action = action_error;
            bean.reason = message;
            try {
              sipSocketManger.sendMessageToAll(bean.respError());
            } catch (JSONException e) {
              e.printStackTrace();
            }
          }
        }
        last = state;
      }
    };

    try {
      // 让我们将一些RAW资源复制到设备
      // 默认配置文件只能安装一次（第一次）
      copyIfNotExist(R.raw.linphonerc_default, basePath + "/.linphonerc");
      // 出厂配置用于覆盖任何其他设置，让我们每次都复制它
      copyFromPackage(R.raw.linphonerc_factory, "linphonerc");
    } catch (IOException ioe) {
      Log.e(ioe);
    }

    // 创建Core并添加我们的监听器
    mCore = Factory.instance()
            .createCore(basePath + "/.linphonerc", basePath + "/linphonerc", this);
    mCore.addListener(mCoreListener);
    // Core已准备好配置
    configureCore();

    sipSocketManger = new SipSocketManger();
    sipSocketManger.start(6789);
    sipSocketManger.setReceiveData(this);
  }

  public void routeAudioToSpeaker() {
    routeAudioToSpeakerHelper(true);
  }

  private void routeAudioToSpeakerHelper(boolean speakerOn) {
    //需要权限 -- 普通权限
    mAudioManager.setSpeakerphoneOn(speakerOn);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);

    // 如果我们的服务已经运行，则无需继续
    if (sInstance != null) {
      return START_STICKY;
    }

    // 我们的服务已经启动，我们可以继续参考
    //从现在开始，Launcher将能够调用onServiceReady（）
    sInstance = this;

    // 必须在创建和配置Core后启动Core
    mCore.start();
    // 我们还必须定期调用Core的 iterate（）方法
    TimerTask lTask =
            new TimerTask() {
              @Override
              public void run() {
                mHandler.post(
                        new Runnable() {
                          @Override
                          public void run() {
                            if (mCore != null) {
                              mCore.iterate();
                            }
                          }
                        });
              }
            };
    mTimer = new Timer("Linphone scheduler");
    mTimer.schedule(lTask, 0, 20);

    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    mCore.removeListener(mCoreListener);
    mTimer.cancel();
    mCore.stop();
    // 停止的Core可以再次启动
    // 为了确保释放资源，我们必须确保将垃圾收集
    mCore = null;
    // 不要忘记释放单例
    sInstance = null;

    sipSocketManger.stop();

    super.onDestroy();
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    // 对于此示例，我们将在杀死应用程序的同时终止服务
    stopSelf();

    super.onTaskRemoved(rootIntent);
  }

  private void configureCore() {
    //如果需要，我们将为用户签名的证书创建一个目录
    String basePath = getFilesDir().getAbsolutePath();
    String userCerts = basePath + "/user-certs";
    File f = new File(userCerts);
    if (!f.exists()) {
      if (!f.mkdir()) {
        Log.e(userCerts + " can't be created.");
      }
    }
    mCore.setUserCertificatesPath(userCerts);
  }

  private void dumpDeviceInformation() {
    StringBuilder sb = new StringBuilder();
    sb.append("DEVICE=").append(Build.DEVICE).append("\n");
    sb.append("MODEL=").append(Build.MODEL).append("\n");
    sb.append("MANUFACTURER=").append(Build.MANUFACTURER).append("\n");
    sb.append("SDK=").append(Build.VERSION.SDK_INT).append("\n");
    sb.append("Supported ABIs=");
    for (String abi : Version.getCpuAbis()) {
      sb.append(abi).append(", ");
    }
    sb.append("\n");
    Log.i(sb.toString());
  }

  private void dumpInstalledLinphoneInformation() {
    PackageInfo info = null;
    try {
      info = getPackageManager().getPackageInfo(getPackageName(), 0);
    } catch (PackageManager.NameNotFoundException nnfe) {
      Log.e(nnfe);
    }

    if (info != null) {
      Log.i(
              "[Service] Linphone version is ",
              info.versionName + " (" + info.versionCode + ")");
    } else {
      Log.i("[Service] Linphone version is unknown");
    }
  }

  private void copyIfNotExist(int ressourceId, String target) throws IOException {
    File lFileToCopy = new File(target);
    if (!lFileToCopy.exists()) {
      copyFromPackage(ressourceId, lFileToCopy.getName());
    }
  }

  private void copyFromPackage(int ressourceId, String target) throws IOException {
    FileOutputStream lOutputStream = openFileOutput(target, 0);
    InputStream lInputStream = getResources().openRawResource(ressourceId);
    int readByte;
    byte[] buff = new byte[8048];
    while ((readByte = lInputStream.read(buff)) != -1) {
      lOutputStream.write(buff, 0, readByte);
    }
    lOutputStream.flush();
    lOutputStream.close();
    lInputStream.close();
  }

  @Override
  public void receiveMessage(SipDataBean message) {
    android.util.Log.d("LinphoneService", "receiveMessage: " + message);
    if (action_incoming.equals(message.action)) {
      //来电的操作
      if (message.accept) {//接听
        acceptPhone();
      } else {//拒接
        terminatePhone();
      }
    }
    if (action_call.equals(message.action)) {
      //拨打电话
      String domain = (String) SPUtils.get(this,
              SPUtils.sip_domain, "192.168.4.7");
      callPhone(message.number, domain);
    }

    if (action_hang_up.equals(message.action)) {
      //挂断
      terminatePhone();
    }
    if (action_state.equals(message.action)) {
      getState();
    }

  }

  public void terminatePhone() {
    if (currentCall != null) {
      currentCall.terminate();
    }
  }

  public void callPhone(String sipNumber, String sipDomain) {
    String userId = "sip:" + sipNumber + "@" + sipDomain;
    if (LinphoneService.isReady()) {
      Core core = LinphoneService.getCore();
      Address address = core.interpretUrl(userId);
      if (address == null) {
        Log.e("TAG", "newOutgoingCall: Couldn't convert to String to Address : " + userId);
        return;
      }

      if (core.isNetworkReachable()) {
        CallParams params = core.createCallParams(null);
        params.enableVideo(false);
        core.inviteAddressWithParams(address, params);
      } else {
        Toast.makeText(this, "无法获取网络", Toast.LENGTH_LONG).show();
      }
    }
  }

  public void getState() {
    SipDataBean bean = new SipDataBean();
    bean.action = action_state;
    switch (currentState) {
      case None:
        bean.state = "None";
        break;
      case Progress:
        bean.state = "Progress";
        break;
      case Ok:
        bean.state = "Ok";
        break;
      case Cleared:
        bean.state = "Cleared";
        break;
      case Failed:
        bean.state = "Failed";
        break;
      default:
        bean.state = "Error";
        break;
    }
    try {
      sipSocketManger.sendMessageToAll(bean.respState());
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  public void acceptPhone() {
    if (currentCall != null) {
      currentCall.accept();
    }
  }

  private String action_call = "call";
  private String action_hang_up = "hang-up";
  private String action_incoming = "incoming";
  private String action_connected = "connect";
  private String action_error = "error";
  private String action_state = "state";


  @Override
  public void onStartWebSocket(String result) {
    SPUtils.put(this, SPUtils.sip_desc, result);
    android.util.Log.d("LinphoneService", "onStartWebSocket: " + result);
    Intent intent = new Intent(sipStateAction);
    intent.putExtra("result", result);
    sendBroadcast(intent, null);
  }
}
