package com.mysdk

import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Bundle
import android.os.OutcomeReceiver
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public suspend fun createTestSandboxSdk(context: Context): TestSandboxSdk =
        suspendCancellableCoroutine {
    val sdkSandboxManager = context.getSystemService(SdkSandboxManager::class.java)
    val outcomeReceiver = object: OutcomeReceiver<SandboxedSdk, LoadSdkException> {
        override fun onResult(result: SandboxedSdk) {
            it.resume(TestSandboxSdkClientProxy(ITestSandboxSdk.Stub.asInterface(result.getInterface())))
        }
        override fun onError(error: LoadSdkException) {
            it.resumeWithException(error)
        }
    }
    sdkSandboxManager.loadSdk("com.mysdk", Bundle.EMPTY, Runnable::run, outcomeReceiver)
}
