package com.outsystems.plugins.sslpinning.types;

import okhttp3.Call;

public class RequestEvents {

    private Call mCall;

    public RequestEvents(Call call) {
        this.mCall = call;
    }

    public Call getCall()
    {
        return mCall;
    }
}
