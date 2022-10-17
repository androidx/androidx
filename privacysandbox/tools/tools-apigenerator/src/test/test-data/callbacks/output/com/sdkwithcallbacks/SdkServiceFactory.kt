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

private class SdkServiceClientProxy(
    private val remote: ISdkService,
) : SdkService {
    public override suspend fun registerCallback(callback: SdkCallback): Unit =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IUnitTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess() {
                it.resumeWith(Result.success(Unit))
            }
            override fun onFailure(errorCode: Int, errorMessage: String) {
                it.resumeWithException(RuntimeException(errorMessage))
            }
        }
        remote.registerCallback(SdkCallbackStubDelegate(callback), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }
}
