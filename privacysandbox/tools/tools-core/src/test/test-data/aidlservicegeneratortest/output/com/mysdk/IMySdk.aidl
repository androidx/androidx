package com.mysdk;

import com.mysdk.IStringTransactionCallback;
import com.mysdk.IUnitTransactionCallback;

interface IMySdk {
    void methodWithoutReturnValue();
    void suspendMethodWithReturnValue(boolean a, int b, long c, float d, double e, char f, int g, IStringTransactionCallback transactionCallback);
    void suspendMethodWithoutReturnValue(IUnitTransactionCallback transactionCallback);
}