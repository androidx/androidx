package com.mysdk;

import com.mysdk.IListStringTransactionCallback;
import com.mysdk.IStringTransactionCallback;
import com.mysdk.IUnitTransactionCallback;

oneway interface IMySdk {
    void methodWithoutReturnValue();
    void suspendMethodWithLists(in int[] l, IListStringTransactionCallback transactionCallback);
    void suspendMethodWithNullables(in int[] maybeInt, IListStringTransactionCallback transactionCallback);
    void suspendMethodWithReturnValue(boolean a, int b, long c, float d, double e, char f, int g, IStringTransactionCallback transactionCallback);
    void suspendMethodWithoutReturnValue(IUnitTransactionCallback transactionCallback);
}