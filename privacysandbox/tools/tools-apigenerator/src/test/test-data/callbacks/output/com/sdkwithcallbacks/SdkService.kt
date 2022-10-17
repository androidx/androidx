package com.sdkwithcallbacks

public interface SdkService {
    public suspend fun registerCallback(callback: SdkCallback): Unit
}
