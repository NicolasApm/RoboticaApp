package com.michaeltroger.shapedetection.views;


public interface ConectarBluetoothView {

    void showData(String data);

    void showMessage(String message);

    void RunOnMain(Runnable action);

    void finish();
}
