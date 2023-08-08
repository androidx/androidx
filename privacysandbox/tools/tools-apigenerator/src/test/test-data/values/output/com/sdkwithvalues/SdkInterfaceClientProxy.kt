package com.sdkwithvalues

import com.sdkwithvalues.PrivacySandboxThrowableParcelConverter.fromThrowableParcel
import com.sdkwithvalues.SdkRequestConverter.toParcelable
import com.sdkwithvalues.SdkResponseConverter.fromParcelable
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public class SdkInterfaceClientProxy(
    public val remote: ISdkInterface,
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
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.exampleMethod(toParcelable(request), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processNullableValues(request: SdkRequest?): SdkResponse? =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: ISdkResponseTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: ParcelableSdkResponse?) {
                it.resumeWith(Result.success(result?.let { notNullValue ->
                        fromParcelable(notNullValue) }))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processNullableValues(request?.let { notNullValue -> toParcelable(notNullValue) },
                transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processValueList(x: List<SdkRequest>): List<SdkResponse> =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IListSdkResponseTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: Array<ParcelableSdkResponse>) {
                it.resumeWith(Result.success(result.map { fromParcelable(it) }.toList()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processValueList(x.map { toParcelable(it) }.toTypedArray(), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }
}
