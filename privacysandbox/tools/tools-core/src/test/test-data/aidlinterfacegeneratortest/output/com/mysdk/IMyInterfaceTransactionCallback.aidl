package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.IMyInterface;
import com.mysdk.PrivacySandboxThrowableParcel;

oneway interface IMyInterfaceTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal) = 6802168;
    void onFailure(in PrivacySandboxThrowableParcel throwableParcel) = 12699996;
    void onSuccess(IMyInterface result) = 11329676;
}