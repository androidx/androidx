package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.PrivacySandboxThrowableParcel;

oneway interface IUnitTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal) = 6802168;
    void onFailure(in PrivacySandboxThrowableParcel throwableParcel) = 12699996;
    void onSuccess() = 3578307;
}