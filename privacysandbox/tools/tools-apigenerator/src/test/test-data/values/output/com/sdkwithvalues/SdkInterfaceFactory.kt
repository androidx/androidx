package com.sdkwithvalues

import android.os.IBinder

public fun wrapToSdkInterface(binder: IBinder): SdkInterface =
        SdkInterfaceClientProxy(ISdkInterface.Stub.asInterface(binder))
