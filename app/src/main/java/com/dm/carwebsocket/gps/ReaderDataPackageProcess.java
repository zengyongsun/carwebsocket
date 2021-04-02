package com.dm.carwebsocket.gps;


public class ReaderDataPackageProcess extends DataPackageProcess {
    public ReaderDataPackageProcess() {
    }

    @Override
    public void analyzeData(byte[] data) {
        String msgTran = new String(data);
        if (msgTran != null) {
            this.setChanged();
            this.notifyObservers(msgTran);
        }
    }

}
