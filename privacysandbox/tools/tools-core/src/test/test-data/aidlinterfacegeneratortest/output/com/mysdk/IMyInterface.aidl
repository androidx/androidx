package com.mysdk;

import android.os.Bundle;
import com.mysdk.IMyInterfaceTransactionCallback;

oneway interface IMyInterface {
    void methodWithActivityLauncherParam(in Bundle activityLauncher) = 12556385;
    void methodWithInterfaceParam(IMyInterface myInterface) = 5537946;
    void suspendMethodWithInterfaceReturn(IMyInterfaceTransactionCallback transactionCallback) = 13841773;
}