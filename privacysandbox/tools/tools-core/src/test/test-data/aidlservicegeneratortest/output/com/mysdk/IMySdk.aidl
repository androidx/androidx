package com.mysdk;

import com.mysdk.IListStringTransactionCallback;
import com.mysdk.IStringTransactionCallback;
import com.mysdk.IUnitTransactionCallback;

oneway interface IMySdk {
    void methodWithoutReturnValue() = 12054020;
    void suspendMethodWithLists(in int[] l, IListStringTransactionCallback transactionCallback) = 669054;
    void suspendMethodWithNullables(in int[] maybeInt, IListStringTransactionCallback transactionCallback) = 13840793;
    void suspendMethodWithReturnValue(boolean a, int b, long c, float d, double e, char f, int g, IStringTransactionCallback transactionCallback) = 737811;
    void suspendMethodWithoutReturnValue(IUnitTransactionCallback transactionCallback) = 2878300;
}