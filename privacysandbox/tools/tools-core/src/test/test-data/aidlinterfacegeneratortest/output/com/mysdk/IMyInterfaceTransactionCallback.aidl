package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.IMyInterface;

oneway interface IMyInterfaceTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal);
    void onFailure(int errorCode, String errorMessage);
    void onSuccess(IMyInterface result);
}