package com.dm.carwebsocket.gps;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

public class ClientSocket {

    public interface ConnectState {
        void reconnect();

        void message(byte[] str);
    }

    private static final String TAG = ClientSocket.class.getSimpleName();


    private Socket mSocket = null;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private DealWidth mDealWidth;
    private String hostIp = "172.0.0.1";
    private PingIpThread pingIpThread = null;

    private List<ConnectState> mState = new ArrayList<>();

    public void setState(ConnectState mState) {
        this.mState.add(mState);
    }

    public void removeState(ConnectState mState) {
        this.mState.remove(mState);
    }

    private static ClientSocket instance = new ClientSocket();

    private ClientSocket() {
        pingIpThread = new PingIpThread();
        pingIpThread.start();
    }

    public static ClientSocket getInstance() {
        return instance;
    }


    public void registerObserver(Observer observer) {
        if (this.mDealWidth != null) {
            this.mDealWidth.registerObserver(observer);
        }
    }

    public void unRegisterObserver(Observer observer) {
        if (this.mDealWidth != null) {
            this.mDealWidth.unRegisterObserver(observer);
        }
    }

    public boolean createConnect(String host, int port) {
        this.disConnect();
        try {
            this.mSocket = new Socket();
            this.mSocket.connect(new InetSocketAddress(host, port), 5000);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            this.mInputStream = mSocket.getInputStream();
            this.mOutputStream = mSocket.getOutputStream();
            this.mDealWidth = new DealWidth(this.mSocket, this.mInputStream, this.mOutputStream,
                    new ReaderDataPackageParser(), new ReaderDataPackageProcess());
            this.pingIpThread.setPingIp(host);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private void disConnect() {
        isRead = false;
        try {
            if (mDealWidth != null) {
                mDealWidth.singOut();
                mDealWidth = null;
            }
            if (mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class DealWidth {
        private ProcessThread processThread = null;
        private InputStream in;
        private OutputStream out;
        private Socket mSocket;
        private ReaderDataPackageParser mPackageParser;
        private DataPackageProcess mPackageProcess;

        public DealWidth(Socket mSocket, InputStream in, OutputStream out, ReaderDataPackageParser parser,
                         DataPackageProcess process) throws Exception {
            this.mSocket = mSocket;
            this.in = in;
            this.out = out;
            if (parser != null) {
                this.mPackageParser = parser;
                this.mPackageProcess = process;
                this.startThread();
            } else {
                throw new Exception("no SocketDataParser");
            }
        }

        public void startThread() {
            Log.d(TAG, "startThread#analysisData: " + Thread.currentThread());
            this.processThread = new ProcessThread();
            this.processThread.start();

        }

        public void registerObserver(Observer observer) {
            this.mPackageProcess.addObserver(observer);
        }

        public void unRegisterObserver(Observer observer) {
            this.mPackageProcess.deleteObserver(observer);
        }

        public void singOut() {
            try {
                if (mOutputStream != null) {
                    mOutputStream.close();
                }
                if (mInputStream != null) {
                    mInputStream.close();
                }
                if (processThread != null) {
                    processThread.setRunning(false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private class ProcessThread extends Thread {

            private boolean isRunning;

            public ProcessThread() {
                this.isRunning = true;
            }

            public void setRunning(boolean running) {
                isRunning = running;
            }

            @Override
            public void run() {
                while (isRunning) {
                    byte[] btAryBuffer = new byte[1024];
                    try {
                        isRead = true;
                        isConnect =true;
                        int nLenRead = DealWidth.this.in.read(btAryBuffer);
                        if (nLenRead > 0) {
                            byte[] btAryReceiveData = new byte[nLenRead];
                            System.arraycopy(btAryBuffer, 0, btAryReceiveData, 0, nLenRead);
                            for (ConnectState item : mState) {
                                item.message(btAryReceiveData);
                            }
                            DealWidth.this.mPackageParser.runReceiveDataCallback(btAryReceiveData,
                                    DealWidth.this.mPackageProcess);
                        }
                    } catch (IOException e) {
                        isConnect = false;
                        judgment("IOException Process 断开连接");
                    } catch (Exception e) {
                        isConnect = false;
                        e.printStackTrace();
                        judgment("Exception Process 断开连接");
                    }
                }
            }
        }
    }


    private boolean isRead = false;
    private boolean isConnect = false;

    public class PingIpThread extends Thread {

        private boolean isRunning = true;
        private String pingIp = "";

        public PingIpThread() {
        }

        public void setRunning(boolean running) {
            isRunning = running;
        }

        public void setPingIp(String pingIp) {
            this.pingIp = pingIp;
        }

        @Override
        public void run() {
            Runtime runtime = Runtime.getRuntime();
            while (isRunning) {
                try {
                    ////ping -c 3 -w 100  中  ，-c 是指ping的次数 3是指ping 3次 ，-w 100  以秒为单位指定超时间隔，是指超时时间为100秒
                    Process p = runtime.exec("ping -c 1 " + pingIp);
                    int ret = p.waitFor();
                    String msg = Thread.currentThread().getName() + " " + pingIp + "    Process:    " + ret;
                    Log.i("Avalible", pingIp + "    Process:    " + ret);
                    if (ret != 0) {
                        judgment("connect断开连接");
                    }else{
                        isConnect =true;
                    }
                    if (mState != null) {
                        for (ConnectState item : mState) {
                            item.message(msg.getBytes());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.i("Avalible", "Process:" + e.getMessage());
                    judgment("Exception connect断开连接");
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private synchronized void judgment(String msg) {
        if (mState != null && isRead ) {
            disConnect();
            for (ConnectState item : mState) {
                item.reconnect();
                item.message(msg.getBytes());
            }
        }
    }
}

