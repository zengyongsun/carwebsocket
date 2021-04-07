package com.dm.carwebsocket.gps;

import java.util.Observable;
import java.util.Observer;

public class RXObserver implements Observer {

    public RXObserver() {
    }

    public final void update(Observable o, Object arg) {
        this.analysisData((String) arg);
    }

    public void analysisData(String msgTran) {

    }

}
