package com.dm.carwebsocket.gps;


import android.util.Log;

import java.util.Arrays;

public class ReaderDataPackageParser {
    private byte[] m_btAryBuffer = new byte[4096];
    private int m_nLength = 0;

    public ReaderDataPackageParser() {
    }

    public void runReceiveDataCallback(byte[] btAryReceiveData, DataPackageProcess dataPackageProcess) {
        try {
            Log.d("ReaderDataPackageParser", "runReceiveDataCallback: " + m_nLength);
            //获取收到的数据长度
            int nCount = btAryReceiveData.length;
            //创建一个字节数组，长度为 当前数据长度+之前不完整数据的长度
            byte[] btAryBuffer = new byte[nCount + this.m_nLength];
            //复制数据之前不完整数据到创建的字节数组
            System.arraycopy(this.m_btAryBuffer, 0, btAryBuffer, 0, this.m_nLength);
            //复制收到的数据到创建的字节数组，
            System.arraycopy(btAryReceiveData, 0, btAryBuffer, this.m_nLength, btAryReceiveData.length);
            //新数组的下标位置
            int nIndex = -1;
            int nMarkIndex = 0;
            int nLen = 5;

            //36 75 83 88 84  $KSXT
            for (int nLoop = 0; nLoop < btAryBuffer.length; ++nLoop) {
                //数据有两位，即有数据的len
                if (btAryBuffer.length > nLoop + nLen) {
                    //查找起始的标识
                    if (btAryBuffer[nLoop] == 36 && btAryBuffer[nLoop + 1] == 75
                            && btAryBuffer[nLoop + 2] == 83 && btAryBuffer[nLoop + 3] == 88
                            && btAryBuffer[nLoop + 4] == 84) {
                        //$KSXT数据的头
                        nIndex = nLoop;
                        nLoop += 4;
                    }
//                    else {
//                        nMarkIndex = nLoop;
//                    }

                    //结束位
                    if (btAryBuffer[nLoop] == 42) {
                        if (nIndex != -1) {
                            byte[] btAryAnaly = new byte[nLoop - nIndex];
                            System.arraycopy(btAryBuffer, nIndex, btAryAnaly, 0, nLoop - nIndex);
                            //校验
                            dataPackageProcess.analyzeData(btAryAnaly);
                            Log.d("ReaderDataPackageParser", "runReceiveDataCallback--: " + new String(btAryAnaly));
                            nIndex = -1;
                        }
                    }
                }
                nMarkIndex = nLoop;
            }

            if (nIndex < nMarkIndex) {
                nIndex = nMarkIndex + 1;
            }

            if (nIndex < btAryBuffer.length) {
                this.m_nLength = btAryBuffer.length - nIndex;
                Arrays.fill(this.m_btAryBuffer, 0, 4096, (byte) 0);
                System.arraycopy(btAryBuffer, nIndex, this.m_btAryBuffer, 0, btAryBuffer.length - nIndex);
            } else {
                this.m_nLength = 0;
            }
        } catch (Exception var10) {
            var10.printStackTrace();
        }
    }
}

