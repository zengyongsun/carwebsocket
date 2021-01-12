package com.dm.carwebsocket;


import com.dm.carwebsocket.gps.GpRmcBean;
import com.dm.carwebsocket.gps.KSXTBean;
import com.google.gson.Gson;

import org.junit.Test;

public class KSXTBeanTest {

  @Test
  public void parse() {
    String data = "$KSXT,20201222075720.00,119.17084152,32.07387900,83.5770,269.94,1.19,207.28,0.001,,3,3,25,25,-3.617,-4.468,-7.860,-0.000,-0.001,-0.022,1.0,56,*33736701";
    KSXTBean ksxt = new  KSXTBean(data);
    Gson gson = new Gson();

    System.out.println(gson.toJson(ksxt));
  }

}
