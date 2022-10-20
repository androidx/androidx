package com.sdkwithvalues

import com.sdkwithvalues.SdkRequestConverter.toParcelable
import com.sdkwithvalues.SdkResponseConverter.fromParcelable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public class SdkInterfaceClientProxy(
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
