package com.dm.carwebsocket.websocket;

import android.util.Log;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * author : Zeyo
 * e-mail : zengyongsun@163.com
 * date   : 2020/10/23 13:22
 * desc   :
 * version: 1.0
 */
public class ServiceSocket extends WebSocketServer {

  private static final String TAG = "WebSocket#ServiceSocket";

  private SocketManager socketManager;

  public ServiceSocket(SocketManager serServiceManager, int port) {
    super(new InetSocketAddress(port));
    this.socketManager = serServiceManager;
  }

  @Override
  public void onOpen(WebSocket conn, ClientHandshake handshake) {
    Log.d(TAG, "onOpen: " + conn);
    socketManager.userLogin(conn);
  }

  @Override
  public void onClose(WebSocket conn, int code, String reason, boolean remote) {
    Log.d(TAG, "onClose: " + conn);
    socketManager.userLeave(conn);
  }

  @Override
  public void onMessage(WebSocket conn, String message) {
    Log.d(TAG, "onMessage: " + message);
    socketManager.onMessage(conn, message);
  }

  @Override
  public void onError(WebSocket conn, Exception ex) {
    Log.d(TAG, "onError: " + ex.toString());
    socketManager.onError(ex.toString());
  }

  @Override
  public void onStart() {
    //启动成功时调用
    Log.d(TAG, "onStart: ");
    socketManager.onStart();
  }
}
