package com.dm.carwebsocket.gps;

import android.text.TextUtils;

import java.text.SimpleDateFormat;

/**
 * // $GPRMC,  020250.00,          A,                   2813.9891299,N,            11252.6278784,E ,  0.033,            315.7, 161117,0.0,E,A*30
 * // 数据id,  utc时间 Hhmmss.ss, 定位状态：A有效，V无效， 维度， 维度方向 N北纬 S南纬，  经度，E东经 W西经 ， 地面速度单位节， 地面航向，日期 日月年，磁偏角 度数，
 */
public class GpRmcBean {

    /**
     * 定位状态 A有效，V无效
     */
    public String state;

    /**
     * utc时间 Hhmmss.ss
     */
    public String utcTime;
    //ddmmyy 日月年
    public String dateTime;

    /**
     * 经度 28°13.99891299 度分格式
     */
    public String longitude;
    public String longitudeDirection;

    /**
     * 纬度
     */
    public String latitude;
    public String latitudeDirection;

    /**
     * 地面速度，单位节
     */
    public String speed;

    /**
     * 地面航向，以真北为参考基准，顺时针方向的角度 单位度
     */
    public String direction;

    /**
     * 模式指示，N=数据无效，A=自主定位，F=估算，R=差分
     */
    public String mode;

    public GpRmcBean(String data) {
        String[] temp = data.split(",");
        utcTime = temp[1];
        dateTime = temp[9];
        state = temp[2];
        latitude = temp[3];
        latitudeDirection = temp[4];
        longitude = temp[5];
        longitudeDirection = temp[6];
        speed = temp[7];
        direction = temp[8];
        mode = temp[12].split("/*")[0];
    }

    /**
     * date='090919'
     *
     * @return yyyy-MM-dd
     */
    public String getDate() {
        if (dateTime.length() >= 6) {
            String dd = dateTime.substring(0, 2);
            String mm = dateTime.substring(2, 4);
            String yy = dateTime.substring(4, 6);
            return "20" + yy + "-" + mm + "-" + dd;
        }
        return "";
    }

    public long timestamp() {
        String dataStr = getDate() + " " + getTime();
        if (TextUtils.isEmpty(getDate()) || TextUtils.isEmpty(getTime())) {
            return -1;
        }
        if (dataStr.trim().length() > 0) {
            //差8时区，所以增加8小时的秒数，让系统计算
            return Long.parseLong(date2TimeStamp(dataStr, "yyyy-MM-dd HH:mm:ss")) + 8 * 60 * 60;
        }
        return -1;
    }

    /**
     * 日期格式字符串转换成时间戳
     *
     * @param date_str 字符串日期
     * @param format   如：yyyy-MM-dd HH:mm:ss
     * @return
     */
    public static String date2TimeStamp(String date_str, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);
            return String.valueOf(sdf.parse(date_str).getTime() / 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * time='091121.80'
     *
     * @return hh:mm:ss
     */
    public String getTime() {
        if (utcTime.length() >= 6) {
            //小时差时区
            String hh = utcTime.substring(0, 2);
            String mu = utcTime.substring(2, 4);
            String ss = utcTime.substring(4, 6);
            return hh + ":" + mu + ":" + ss;
        }
        return "";
    }
}
