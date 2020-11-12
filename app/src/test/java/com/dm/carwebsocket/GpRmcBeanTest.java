package com.dm.carwebsocket;


import com.dm.carwebsocket.gps.GpRmcBean;
import com.google.gson.Gson;

import org.junit.Test;

public class GpRmcBeanTest {

  @Test
  public void parse() {
    String data = "$GPRMC,071317.00,A,2810.4195048,N,11254.9934421,E,0.009,98.2,101120,0.0,E,D*07";
    GpRmcBean gpRmcBean = new GpRmcBean(data);
    Gson gson = new Gson();

    System.out.println(gson.toJson(gpRmcBean));
  }

}
