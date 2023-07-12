package com.mysdk;

import com.mysdk.IMyInterfaceTransactionCallback;

oneway interface IMyInterface {
    void methodWithInterfaceParam(IMyInterface myInterface) = 5537946;
    void suspendMethodWithInterfaceReturn(IMyInterfaceTransactionCallback transactionCallback) = 13841773;
}