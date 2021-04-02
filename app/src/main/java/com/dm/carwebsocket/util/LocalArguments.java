package com.dm.carwebsocket.util;

import android.content.Context;

/**
 * @author : Zeyo
 * e-mail : zengyongsun@163.com
 * date   : 2019/8/26 17:32
 * desc   :
 * version: 1.0
 */
public class LocalArguments {

    private String update_port = "8003";
    private String update_ip = "192.168.4.101";

    private String service_port = "5000";
    private String service_ip = "58.51.41.30";

    private String cpe_settings_ip = "192.168.1.1";

    private String rabbit_username = "test";
    private String rabbit_password = "123456";

    private String local_port = "5672";

    private String max_speed = "30";
    private String gps_upload_speed = "5";


    private String device_id = "-1";

    private String serial_port_device = "/dev/ttySAC2";
    private String serial_port_baudrate = "115200";

    private String sip_domain = "172.16.100.243";
    private String sip_call_number = "8888";
    private String sip_username = "8020";
    private String sip_password = "Abc123456";

    private boolean is_night = false;

    private String advance_settings_pwd = "666";
    private String factory_settings_pwd = "369";

    private String settings_remark = "无";

    private String device_ram = "0";
    private String device_rom = "0";
    private Object play_voice = "3";

    private boolean write_log = true;


    private Context mContext;

    private static final LocalArguments ourInstance = new LocalArguments();

    public static LocalArguments getInstance() {
        return ourInstance;
    }

    private LocalArguments() {
    }

    public void init(Context context) {
        mContext = context;
    }


    public String serviceIp() {
        return (String) SPUtils.get(mContext, SPUtils.SERVICE_IP, service_ip);
    }

    public void modifyServiceIp(String ip) {
        SPUtils.put(mContext, SPUtils.SERVICE_IP, ip);
    }

    public String servicePort() {
        return (String) SPUtils.get(mContext, SPUtils.SERVICE_PORT, service_port);
    }

    public void modifyServicePort(String port) {
        SPUtils.put(mContext, SPUtils.SERVICE_PORT, port);
    }
}
