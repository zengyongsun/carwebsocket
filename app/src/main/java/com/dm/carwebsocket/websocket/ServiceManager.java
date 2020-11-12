package com.dm.carwebsocket.websocket;

import android.util.Log;

import org.java_websocket.WebSocket;

import java.util.HashSet;
import java.util.Set;

/**
 * author : Zeyo
 * e-mail : zengyongsun@163.com
 * date   : 2020/10/23 13:23
 * desc   :
 * version: 1.0
 */
public class ServiceManager {

    private static final String TAG = "WebSocket#SerManager";
    private ServiceSocket serviceSocket = null;

    private Set<WebSocket> userSet = new HashSet<>();
    private WebSocketReceiveData receiveData;

    public void setReceiveData(WebSocketReceiveData receiveData) {
        this.receiveData = receiveData;
    }

    public ServiceManager() {
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
            receiveData.playVoice(message);
        }
    }

    public void onStart(String result){
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
        }finally {
            serviceSocket = null;
        }
    }

    public interface WebSocketReceiveData {
        void playVoice(String message);
        void onStartWebSocket(String result);
    }


}
