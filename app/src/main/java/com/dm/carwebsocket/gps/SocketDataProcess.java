package com.dm.carwebsocket.gps;

import java.util.Observable;

public abstract class SocketDataProcess extends Observable {

    public abstract void analyzeData(String data);

}
