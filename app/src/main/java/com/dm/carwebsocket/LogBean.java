package com.dm.carwebsocket;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * gps日志的实体类
 */
public class LogBean {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd HH:mm:ss", Locale.CHINA);

    public long timeMillis;
    public String log;

    public LogBean(long timeMillis, String log) {
        this.timeMillis = timeMillis;
        this.log = log;
    }

    public String flattenedLog() {
        return getFlattened() + log;
    }

    public String getFlattened() {
        return format(timeMillis) + "|:";
    }

    private String format(long timeMillis) {
        return sdf.format(timeMillis);
    }
}
