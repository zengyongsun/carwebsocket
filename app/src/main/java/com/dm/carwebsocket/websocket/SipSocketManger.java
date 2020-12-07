package com.dm.carwebsocket.websocket;

import android.util.Log;

import com.google.gson.Gson;

import org.java_websocket.WebSocket;

import java.util.HashSet;
import java.util.Set;

public class SipSocketManger implements SocketManager {

  private static final String TAG = "SipSocketManger";
  private ServiceSocket serviceSocket = null;

  private Set<WebSocket> userSet = new HashSet<>();

  private SipSocketReceiveData receiveData;

  public void setReceiveData(SipSocketReceiveData receiveData) {
    this.receiveData = receiveData;
  }

  public SipSocketManger() {
  }

  public void userLogin(WebSocket socket) {
    if (socket != null) {
      userSet.add(socket);
    }
  }

  public void userLeave(WebSocket socket) {
    if (userSet.equals(socket)) {
      userSet.remove(socket);
    }
  }

  public void sendMessageToAll(String message) {
    for (WebSocket socket : userSet) {
      if (socket != null) {
        socket.send(message);
      }
    }
  }

  public boolean start(int port) {
    if (port < 0) {
      Log.d(TAG, "start: port error");
      return false;
    }
    Log.d(TAG, "start: service socket");
    serviceSocket = new ServiceSocket(this, port);
    serviceSocket.start();
    return true;
  }

  public void onMessage(WebSocket socket, String message) {
    if (receiveData != null) {
      try {
        receiveData.receiveMessage(jsonToBean(message));
      }catch(Exception ex){
        ex.printStackTrace();

      }
    }
  }

  @Override
  public void onError(String ex) {
    if (receiveData != null) {
      receiveData.onStartWebSocket("车载服务器：SipWebSocket" + ex);
    }
  }

  @Override
  public void onStart() {
    if (receiveData != null) {
      receiveData.onStartWebSocket("车载服务器：SipWebSocket启动成功！");
    }
  }

  public void onStart(String result) {
    if (receiveData != null) {
      receiveData.onStartWebSocket(result);
    }
  }

  public boolean stop() {
    try {
      serviceSocket.stop();
      Log.d(TAG, "stop: stop service socket");
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      serviceSocket = null;
    }
  }

  private Gson gson = new Gson();

  public String parseToJson(SipDataBean data) {
    return gson.toJson(data);
  }

  public SipDataBean jsonToBean(String json) {
    return gson.fromJson(json, SipDataBean.class);
  }

  public interface SipSocketReceiveData {

    void receiveMessage(SipDataBean message);

    void onStartWebSocket(String result);
  }

}
