package com.sdkwithvalues

import android.os.IBinder

public object SdkInterfaceFactory {
    public fun wrapToSdkInterface(binder: IBinder): SdkInterface = SdkInterfaceClientProxy(ISdkInterface.Stub.asInterface(binder))
}
