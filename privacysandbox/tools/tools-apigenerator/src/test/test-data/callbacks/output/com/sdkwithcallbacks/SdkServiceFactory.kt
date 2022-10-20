package com.sdkwithcallbacks

import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Bundle
import android.os.OutcomeReceiver
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public suspend fun createSdkService(context: Context): SdkService = suspendCancellableCoroutine {
    val sdkSandboxManager = context.getSystemService(SdkSandboxManager::class.java)
    val outcomeReceiver = object: OutcomeReceiver<SandboxedSdk, LoadSdkException> {
        override fun onResult(result: SandboxedSdk) {
            it.resume(SdkServiceClientProxy(ISdkService.Stub.asInterface(result.getInterface())))
        }
        override fun onError(error: LoadSdkException) {
            it.resumeWithException(error)
        }
    }
    sdkSandboxManager.loadSdk("com.sdkwithcallbacks", Bundle.EMPTY, Runnable::run, outcomeReceiver)
}
