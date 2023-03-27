package com.sdkwithcallbacks

public class SdkServiceClientProxy(
    public val remote: ISdkService,
) : SdkService {
    public override fun registerCallback(callback: SdkCallback): Unit {
        remote.registerCallback(SdkCallbackStubDelegate(callback))
    }
}
