package com.dm.carwebsocket.websocket;

public class SipDataBean {

  public String action;

  public String data;

  public String number;

  public String reason;

  public boolean accept;

  public boolean result;

  public String desc;

  public String state;

  public String respState() {
    return "{" +
            "action:" + action +
            ", state:" + state +
            '}';
  }

  public String respCall() {
    return "{" +
            "action:" + action +
            ", result:" + result +
            ", desc:" + desc +
            '}';
  }

  public String connect() {
    return "{" +
            "action:" + action +
            '}';
  }

  public String calling() {
    return "{" +
            "action:" + action +
            ", number:" + number +
            '}';
  }

  public String respError() {
    return "{" +
            "action:" + action +
            ", reason:" + reason +
            '}';
  }

}
