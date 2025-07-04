package com.sdkwithcallbacks

import android.os.IBinder

public object SdkServiceFactory {
    public fun wrapToSdkService(binder: IBinder): SdkService = SdkServiceClientProxy(ISdkService.Stub.asInterface(binder))
}
