package com.ntoprevd.cogno.data.repository;

public interface OnResultCallback<T> {
    void onSuccess(T result);

    void onError(Exception e);
}
