package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.ParcelableOuterValue;

oneway interface IOuterValueTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal);
    void onFailure(int errorCode, String errorMessage);
    void onSuccess(in ParcelableOuterValue result);
}