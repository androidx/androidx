package com.mysdk;

import com.mysdk.IListOuterValueTransactionCallback;
import com.mysdk.IOuterValueTransactionCallback;
import com.mysdk.IUnitTransactionCallback;
import com.mysdk.ParcelableOuterValue;

oneway interface IMySdk {
    void methodReceivingValue(in ParcelableOuterValue value) = 860096;
    void suspendMethodReceivingValue(in ParcelableOuterValue inputValue, IUnitTransactionCallback transactionCallback) = 1452795;
    void suspendMethodThatReturnsValue(IOuterValueTransactionCallback transactionCallback) = 2938984;
    void suspendMethodWithListsOfValues(in ParcelableOuterValue[] inputValues, IListOuterValueTransactionCallback transactionCallback) = 14073433;
    void suspendMethodWithNullableValues(in ParcelableOuterValue maybeValue, IOuterValueTransactionCallback transactionCallback) = 12446561;
}