package com.mysdk;

import com.mysdk.ICancellationSignal;
import com.mysdk.ParcelableOuterValue;
import com.mysdk.PrivacySandboxThrowableParcel;

oneway interface IListOuterValueTransactionCallback {
    void onCancellable(ICancellationSignal cancellationSignal);
    void onFailure(in PrivacySandboxThrowableParcel throwableParcel);
    void onSuccess(in ParcelableOuterValue[] result);
}