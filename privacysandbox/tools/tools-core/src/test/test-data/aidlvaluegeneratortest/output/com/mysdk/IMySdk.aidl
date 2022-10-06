package com.mysdk;

import com.mysdk.IOuterValueTransactionCallback;
import com.mysdk.IUnitTransactionCallback;
import com.mysdk.ParcelableInnerValue;
import com.mysdk.ParcelableOuterValue;

interface IMySdk {
    void methodReceivingValue(in ParcelableOuterValue value);
    void suspendMethodReceivingValue(in ParcelableOuterValue inputValue, IUnitTransactionCallback transactionCallback);
    void suspendMethodThatReturnsValue(IOuterValueTransactionCallback transactionCallback);
}