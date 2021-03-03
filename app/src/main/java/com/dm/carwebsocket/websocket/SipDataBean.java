package com.dm.carwebsocket.websocket;

import org.json.JSONException;
import org.json.JSONObject;

public class SipDataBean {

    public String action;

    public String data;

    public String number;

    public String reason;

    public boolean accept;

    public boolean result;

    public String desc;

    public String state;

    public String respState() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("state", state);
        return object.toString();
    }

    public String respCall() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("result", result);
        object.put("desc", desc);
        return object.toString();
    }

    public String connect() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("action", action);
        return object.toString();
    }

    public String calling() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("number", number);
        return object.toString();
    }

    public String respError() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("action", action);
        object.put("reason", reason);
        return object.toString();
    }

}
