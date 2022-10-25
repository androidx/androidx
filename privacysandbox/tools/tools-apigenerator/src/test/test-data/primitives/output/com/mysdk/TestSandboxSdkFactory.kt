package com.mysdk

import android.os.IBinder

public fun wrapToTestSandboxSdk(binder: IBinder): TestSandboxSdk =
        TestSandboxSdkClientProxy(ITestSandboxSdk.Stub.asInterface(binder))
