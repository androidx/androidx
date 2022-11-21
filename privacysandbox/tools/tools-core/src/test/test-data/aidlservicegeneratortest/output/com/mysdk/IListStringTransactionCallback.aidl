package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.PrivacySandboxThrowableParcel;

oneway interface IListStringTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal);
    void onFailure(in PrivacySandboxThrowableParcel throwableParcel);
    void onSuccess(in String[] result);
}