package com.mysdk;

import com.mysdk.IListOuterValueTransactionCallback;
import com.mysdk.IOuterValueTransactionCallback;
import com.mysdk.IUnitTransactionCallback;
import com.mysdk.ParcelableOuterValue;

oneway interface IMySdk {
    void methodReceivingValue(in ParcelableOuterValue value);
    void suspendMethodReceivingValue(in ParcelableOuterValue inputValue, IUnitTransactionCallback transactionCallback);
    void suspendMethodThatReturnsValue(IOuterValueTransactionCallback transactionCallback);
    void suspendMethodWithListsOfValues(in ParcelableOuterValue[] inputValues, IListOuterValueTransactionCallback transactionCallback);
    void suspendMethodWithNullableValues(in ParcelableOuterValue maybeValue, IOuterValueTransactionCallback transactionCallback);
}