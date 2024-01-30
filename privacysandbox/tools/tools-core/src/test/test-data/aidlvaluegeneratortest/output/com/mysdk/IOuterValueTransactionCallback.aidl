package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.ParcelableOuterValue;
import com.mysdk.PrivacySandboxThrowableParcel;

oneway interface IOuterValueTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal) = 6802168;
    void onFailure(in PrivacySandboxThrowableParcel throwableParcel) = 12699996;
    void onSuccess(in ParcelableOuterValue result) = 6937269;
}