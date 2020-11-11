package com.dm.carwebsocket;


import com.google.gson.Gson;

import org.junit.Test;

public class GpRmcBeanTest {

  @Test
  public void parse() {
    String data = "$GPRMC,020250.00,A,2813.9891299,N,11252.6278784,E,0.033,315.7,161117,0.0,E,A*30";
    GpRmcBean gpRmcBean = new GpRmcBean(data);
    Gson gson = new Gson();

    System.out.println(gson.toJson(gpRmcBean));
  }

}
