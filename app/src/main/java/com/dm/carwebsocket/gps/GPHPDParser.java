package com.dm.carwebsocket.gps;

import android.util.Log;

public class GPHPDParser implements SocketDataParser {

  public static final String TAG = GPHPDParser.class.getSimpleName();

  @Override
  public void dataParser(String data) {
    Log.d(TAG, "dataParser: " + data);
  }

}
