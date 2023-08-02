package com.mysdk;

import com.mysdk.IMyInterface;
import com.mysdk.IMyInterfaceTransactionCallback;

oneway interface IMySdk {
    void methodWithInterfaceParam(IMyInterface myInterface) = 5537946;
    void suspendMethodWithInterfaceReturn(IMyInterfaceTransactionCallback transactionCallback) = 13841773;
}