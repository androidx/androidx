package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.PrivacySandboxThrowableParcel;

oneway interface IStringTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal);
    void onFailure(in PrivacySandboxThrowableParcel throwableParcel);
    void onSuccess(String result);
}