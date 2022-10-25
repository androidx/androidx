package com.sdkwithcallbacks

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public class SdkServiceClientProxy(
    public val remote: ISdkService,
) : SdkService {
    public override fun registerCallback(callback: SdkCallback): Unit {
        remote.registerCallback(SdkCallbackStubDelegate(callback))
    }
}
