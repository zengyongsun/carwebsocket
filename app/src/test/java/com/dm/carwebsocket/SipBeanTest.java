package com.dm.carwebsocket;

import com.dm.carwebsocket.websocket.SipDataBean;
import com.google.gson.Gson;

import org.junit.Test;

public class SipBeanTest {

  @Test
  public void toJson() {
    Gson gson = new Gson();
    SipDataBean bean = new SipDataBean();
    bean.action = "call";
    bean.data = "hello";
    System.out.println(gson.toJson(bean));
    System.out.println(gson.toJson(bean.reason));
  }

  @Test
  public void fromJson() {
    Gson gson = new Gson();
    String data = "{action:call,error:no}";
    SipDataBean bean = gson.fromJson(data, SipDataBean.class);
    System.out.println(bean);

  }

}
