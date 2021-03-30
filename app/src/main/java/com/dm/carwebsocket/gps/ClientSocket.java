package com.dm.carwebsocket.gps;

import android.util.Log;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class ClientSocket {

    public interface ConnectState {
        void socketDisconnect();

        void message(String str);
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

    private SocketDataParser mSocketDataParser;

    public void setSocketDataParser(SocketDataParser mSocketDataParser) {
        this.mSocketDataParser = mSocketDataParser;
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
            this.mDealWidth = new DealWidth(this.mInputStream, this.mOutputStream, this.mSocketDataParser);
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
        private SocketDataParser parser;

        public DealWidth(InputStream in, OutputStream out, SocketDataParser parser) throws Exception {
            this.in = in;
            this.out = out;
            if (parser != null) {
                this.parser = parser;
                this.startThread();
            } else {
                throw new Exception("no SocketDataParser");
            }
        }

        public void startThread() {
            this.processThread = new ProcessThread();
            this.processThread.start();
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
                    InputStreamReader inputStreamReader = new InputStreamReader(DealWidth.this.in);
                    BufferedReader br = new BufferedReader(inputStreamReader);
                    String data = null;
                    try {
                        while ((data = br.readLine()) != null) {
                            DealWidth.this.parser.dataParser(data);
                            for (ConnectState item : mState) {
                                item.message(data);
                            }
                        }
                    } catch (SocketException e) {
                        Log.d(TAG, "run:SocketException:" + e.getMessage());
                        if (mState != null) {

                            for (ConnectState item : mState) {
                                item.socketDisconnect();
                            }
                            disConnect();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, "run:Exception" + e.getMessage());
                        if (mState != null) {
                            for (ConnectState item : mState) {
                                item.socketDisconnect();
                            }
                            disConnect();
                        }
                    }
                }
            }
        }


    }

    private class ProcessThread2 extends Thread {

        private boolean isRunning;

        public ProcessThread2() {
            this.isRunning = true;
        }

        public void setRunning(boolean running) {
            isRunning = running;
        }

        @Override
        public void run() {
            while (isRunning) {
                String str = "$KSXT,20201222075720.00,119.17084152,32.07387900,83.5770,269.94,1.19,207.28,0.001,,3,3,25,25,-3.617,-4.468,-7.860,-0.000,-0.001,-0.022,1.0,56,*33736701";
                for (ConnectState item : mState) {
                    item.message(str);
                }
                Log.d(TAG, "run: 运行了");
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

