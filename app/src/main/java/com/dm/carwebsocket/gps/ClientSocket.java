package com.dm.carwebsocket.gps;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientSocket {


  private Socket mSocket = null;
  private InputStream mInputStream;
  private OutputStream mOutputStream;
  private DealWidth mDealWidth;

  public ClientSocket() {

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
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    }

  }
}

