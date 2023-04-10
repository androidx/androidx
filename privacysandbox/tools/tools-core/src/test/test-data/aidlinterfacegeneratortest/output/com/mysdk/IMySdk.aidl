package com.mysdk;

import com.mysdk.IMyInterface;
import com.mysdk.IMyInterfaceTransactionCallback;

oneway interface IMySdk {
    void methodWithInterfaceParam(IMyInterface myInterface);
    void suspendMethodWithInterfaceReturn(IMyInterfaceTransactionCallback transactionCallback);
}