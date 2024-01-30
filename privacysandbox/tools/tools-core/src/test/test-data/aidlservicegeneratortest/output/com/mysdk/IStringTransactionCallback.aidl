package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.PrivacySandboxThrowableParcel;

oneway interface IStringTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal) = 6802168;
    void onFailure(in PrivacySandboxThrowableParcel throwableParcel) = 12699996;
    void onSuccess(String result) = 3868755;
}