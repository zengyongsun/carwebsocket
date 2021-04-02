package com.dm.carwebsocket.gps;

import java.util.Observable;

public abstract class DataPackageProcess extends Observable {
    public DataPackageProcess() {
    }

    public abstract void analyzeData(byte[] data);
}

