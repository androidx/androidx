package com.mysdk;

import com.mysdk.ICancellationSignal;

oneway interface IUnitTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal);
    void onFailure(int errorCode, String errorMessage);
    void onSuccess();
}