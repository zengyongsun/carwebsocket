package com.dm.carwebsocket.websocket;

import org.java_websocket.WebSocket;

public interface SocketManager {

  void userLogin(WebSocket conn);

  void userLeave(WebSocket conn);

  void onMessage(WebSocket conn, String message);

  void onError(String ex);

  void onStart();
}
