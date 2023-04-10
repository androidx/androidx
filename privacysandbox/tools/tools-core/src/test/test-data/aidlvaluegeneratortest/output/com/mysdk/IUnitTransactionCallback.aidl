package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.PrivacySandboxThrowableParcel;

oneway interface IUnitTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal);
    void onFailure(in PrivacySandboxThrowableParcel throwableParcel);
    void onSuccess();
}