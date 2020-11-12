package com.dm.carwebsocket.util;


import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * @author : Zeyo
 * e-mail : zengyongsun@163.com
 * date   : 2019/6/17 15:19
 * desc   :
 * version: 1.0
 */
public class IPUtils {

  /**
   * @param ip "192.168.4.250"
   */
  public static int ipStr2int(String ip) {
    InetAddress inetAddress;
    try {
      inetAddress = InetAddress.getByName(ip);
    } catch (UnknownHostException e) {
      return -1;
    }
    byte[] addrBytes;
    int addr;
    addrBytes = inetAddress.getAddress();
    addr = ((addrBytes[3] & 0xff) << 24) | ((addrBytes[2] & 0xff) << 16)
            | ((addrBytes[1] & 0xff) << 8)
            | (addrBytes[0] & 0xff);
    return addr;
  }

  @NonNull
  public static String ipInt2str(int ip) {
    StringBuffer sb = new StringBuffer();
    int b = (ip >> 0) & 0xff;
    sb.append(b + ".");
    b = (ip >> 8) & 0xff;
    sb.append(b + ".");
    b = (ip >> 16) & 0xff;
    sb.append(b + ".");
    b = (ip >> 24) & 0xff;
    sb.append(b);
    return sb.toString();
  }

  /**
   * 获取本机ip地址 192.168.4.250
   */
  public static String getLocalIpAddress() {
    try {
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress() && (inetAddress instanceof Inet4Address)) {
            return inetAddress.getHostAddress();
          }
        }
      }
    } catch (SocketException ex) {
      ex.printStackTrace();
    }
    return "";
  }

  public static int getLoacalIpAddress() {
    if (getLocalIpAddress().equals("")) {
      return -1;
    }
    return ipStr2int(getLocalIpAddress());
  }


  /**
   * getIpAddrForInterfaces("eth0")
   *
   * @param interfaceName
   * @return
   */
  public static String getIpAddrForInterfaces(String interfaceName) {
    try {
      //获取本机所有的网络接口
      Enumeration<NetworkInterface> enNetworkInterface = NetworkInterface.getNetworkInterfaces();
      //判断 Enumeration 对象中是否还有数据
      while (enNetworkInterface.hasMoreElements()) {
        //获取 Enumeration 对象中的下一个数据
        NetworkInterface networkInterface = enNetworkInterface.nextElement();
        // 判断网口是否在使用
        if (!networkInterface.isUp()) {
          continue;
        }
        // 网口名称是否和需要的相同
        if (!interfaceName.equals(networkInterface.getDisplayName())) {
          continue;
        }
        //getInetAddresses 方法返回绑定到该网卡的所有的 IP 地址。
        Enumeration<InetAddress> enInetAddress = networkInterface.getInetAddresses();
        while (enInetAddress.hasMoreElements()) {
          InetAddress inetAddress = enInetAddress.nextElement();
          //判断是否未ipv4
          if (inetAddress instanceof Inet4Address) {
            return inetAddress.getHostAddress();
          }
//                    判断未lo时
//                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
//                        return inetAddress.getHostAddress();
//                    }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "error";
  }

  /**
   * getIpAddrMaskForInterfaces("eth0")
   */
  public static String getIpAddrMaskForInterfaces(String interfaceName) {
    try {
      //获取本机所有的网络接口
      Enumeration<NetworkInterface> networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
      //判断 Enumeration 对象中是否还有数据
      while (networkInterfaceEnumeration.hasMoreElements()) {
        //获取 Enumeration 对象中的下一个数据
        NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
        //判断网口是否在使用，判断是否时我们获取的网口
        if (!networkInterface.isUp() && !interfaceName.equals(networkInterface.getDisplayName())) {
          continue;
        }

        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
          //仅仅处理ipv4
          if (interfaceAddress.getAddress() instanceof Inet4Address) {
            //获取掩码位数，通过 calcMaskByPrefixLength 转换为字符串
            return calcMaskByPrefixLength(interfaceAddress.getNetworkPrefixLength());
          }
        }
      }
    } catch (SocketException e) {
      e.printStackTrace();
    }
    return "error";
  }

  /**
   * 通过子网掩码的位数计算子网掩码
   *
   * @param length 位数
   */
  public static String calcMaskByPrefixLength(int length) {

    int mask = 0xffffffff << (32 - length);
    int partsNum = 4;
    int bitsOfPart = 8;
    int[] maskParts = new int[partsNum];
    int selector = 0x000000ff;

    for (int i = 0; i < maskParts.length; i++) {
      int pos = maskParts.length - 1 - i;
      maskParts[pos] = (mask >> (i * bitsOfPart)) & selector;
    }

    String result = "";
    result = result + maskParts[0];
    for (int i = 1; i < maskParts.length; i++) {
      result = result + "." + maskParts[i];
    }
    return result;
  }


  public static String getLocalIp() {
    String ip = IPUtils.getLocalIpAddress();
    if (ip.isEmpty()) {
//      ip = Helper.Preference().getString(SharedPreferencesValue.localIp, SharedPreferencesValue.localIpDefValue);
    }
    return ip;
  }

  public static String getGateWay() {
    String gateway = IPUtils.getLocalGateway();
    if (gateway.equals("dev")||gateway.equals("error")) {
//      gateway = Helper.Preference().getString(SharedPreferencesValue.gateway, "");
    }
    return gateway;
  }

  public static String getMask() {
    String mask = IPUtils.getIpAddrMaskForInterfaces("eth0");
    if (mask.equals("255.0.0.0")) {
//      mask = Helper.Preference().getString(SharedPreferencesValue.mask, "");
    }
    return mask;
  }


  /**
   * 获取网关地址
   *
   * @return
   */
  public static String getLocalGateway() {
    String[] arr;
    try {
      Process process = Runtime.getRuntime().exec("ip route list table 0");
      String data = null;
      BufferedReader ie = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
      String string = in.readLine();

      arr = string.split("\\s+");
      return arr[2];
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "error";
  }
}
