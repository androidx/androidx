package com.sdk

import android.os.IBinder

public fun wrapToMySdk(binder: IBinder): MySdk = MySdkClientProxy(IMySdk.Stub.asInterface(binder))
