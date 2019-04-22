package com.michaeltroger.shapedetection.devices;

public interface BTCallback {
    void onNext(String data);
    void onError(Exception e);
}
