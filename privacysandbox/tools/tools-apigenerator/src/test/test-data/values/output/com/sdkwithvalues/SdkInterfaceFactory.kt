package com.sdkwithvalues

import android.app.sdksandbox.LoadSdkException
import android.app.sdksandbox.SandboxedSdk
import android.app.sdksandbox.SdkSandboxManager
import android.content.Context
import android.os.Bundle
import android.os.OutcomeReceiver
import com.sdkwithvalues.SdkRequestConverter.toParcelable
import com.sdkwithvalues.SdkResponseConverter.fromParcelable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public suspend fun createSdkInterface(context: Context): SdkInterface =
        suspendCancellableCoroutine {
    val sdkSandboxManager = context.getSystemService(SdkSandboxManager::class.java)
    val outcomeReceiver = object: OutcomeReceiver<SandboxedSdk, LoadSdkException> {
        override fun onResult(result: SandboxedSdk) {
            it.resume(SdkInterfaceClientProxy(ISdkInterface.Stub.asInterface(result.getInterface())))
        }
        override fun onError(error: LoadSdkException) {
            it.resumeWithException(error)
        }
    }
    sdkSandboxManager.loadSdk("com.sdkwithvalues", Bundle.EMPTY, Runnable::run, outcomeReceiver)
}

private class SdkInterfaceClientProxy(
    private val remote: ISdkInterface,
) : SdkInterface {
    public override suspend fun exampleMethod(request: SdkRequest): SdkResponse =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: ISdkResponseTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: ParcelableSdkResponse) {
                it.resumeWith(Result.success(fromParcelable(result)))
            }
            override fun onFailure(errorCode: Int, errorMessage: String) {
                it.resumeWithException(RuntimeException(errorMessage))
            }
        }
        remote.exampleMethod(toParcelable(request), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }
}
