package com.dm.carwebsocket.gps;

/**
 * author : Zeyo
 * e-mail : zengyongsun@163.com
 * date   : 2020/12/23 002310:56
 * desc   :$KSXT,20201222075720.00,119.17084152,32.07387900,83.5770,269.94,1.19,207.28,0.001,,3,3,25,25,-3.617,-4.468,-7.860,-0.000,-0.001,-0.022,1.0,56,*33736701
 * version: 1.0
 */
public class KSXTBean {

    //20201222075720.00
    public String time;
    //纬度
    public String latitude;
    //经度
    public String longitude;
    //高度
    public String altitude;
    //方位角
    public String azimuth;
    //俯仰角
    public String pitchAngle;
    //速度角
    public String velocityAngle;
    //速度 km/h
    public String speed;
    //卫星定位状态
    public String satellitePositioningStatus;
    //卫星定向状态
    public String SatelliteOrientationStatus;
    //前天线卫星数
    public String beforeAntennaSatellites;
    //后天线卫星数
    public String afterAntennaSatellites;
    //差分年龄
    public String differentialAge;

    public KSXTBean(String data) {
        String[] temp = data.split(",");
        time = temp[1];
        longitude = temp[2];
        latitude = temp[3];
        altitude = temp[4];
        azimuth = temp[5];
        pitchAngle = temp[6];
        velocityAngle = temp[7];
        speed = temp[8];
        satellitePositioningStatus = temp[10];
        SatelliteOrientationStatus = temp[11];
        beforeAntennaSatellites = temp[12];
        afterAntennaSatellites = temp[13];
        differentialAge = temp[20];
    }

    public KSXTBean() {
    }
}
