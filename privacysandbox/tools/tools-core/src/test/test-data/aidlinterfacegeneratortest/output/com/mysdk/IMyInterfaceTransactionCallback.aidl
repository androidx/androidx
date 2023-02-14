package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.IMyInterface;
import com.mysdk.PrivacySandboxThrowableParcel;

oneway interface IMyInterfaceTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal);
    void onFailure(in PrivacySandboxThrowableParcel throwableParcel);
    void onSuccess(IMyInterface result);
}