package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.PrivacySandboxThrowableParcel;

oneway interface IListStringTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal) = 6802168;
    void onFailure(in PrivacySandboxThrowableParcel throwableParcel) = 12699996;
    void onSuccess(in String[] result) = 13628579;
}