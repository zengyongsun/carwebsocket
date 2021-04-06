package com.dm.carwebsocket.gps;

import android.util.Log;

import java.io.*;
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

    private List<ConnectState> mState = new ArrayList<>();

    public void setState(ConnectState mState) {
        this.mState.add(mState);
    }

    public void removeState(ConnectState mState) {
        this.mState.remove(mState);
    }

    private static ClientSocket instance = new ClientSocket();

    private ClientSocket() {
//        new ProcessThread2().start();
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
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    private void disConnect() {
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
//        private ConnectThread connectThread = null;
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
            this.processThread = new ProcessThread();
            this.processThread.start();
//            this.connectThread = new ConnectThread();
//            this.connectThread.start();
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
//                if (connectThread != null) {
//                    connectThread.setRunning(false);
//                }
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
                        Log.d(TAG, "run:SocketException:" + e.getMessage());
                        if (mState != null) {
                            for (ConnectState item : mState) {
                                item.reconnect();
                            }
                            disConnect();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, "run:Exception" + e.getMessage());
                        if (mState != null) {
                            for (ConnectState item : mState) {
                                item.reconnect();
                            }
                            disConnect();
                        }
                    }
                }
            }
        }

        private class ConnectThread extends Thread {

            private boolean isRunning;

            public ConnectThread() {
                this.isRunning = true;
            }

            public void setRunning(boolean running) {
                isRunning = running;
            }

            @Override
            public void run() {
                while (isRunning) {
                    try {
                        DealWidth.this.out.write(1); // 发送心跳包
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (mState != null) {
                            for (ConnectState item : mState) {
                                item.reconnect();
                            }
                            disConnect();
                        }
                        System.out.println("IOException！");
                    }
                    System.out.println("目前是正常的！");
                    try {
                        Thread.sleep(3 * 1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}

