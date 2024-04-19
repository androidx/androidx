package com.sdkwithvalues

import com.sdkwithvalues.PrivacySandboxThrowableParcelConverter.fromThrowableParcel
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import com.sdkwithvalues.RequestFlagConverter.fromParcelable as requestFlagConverterFromParcelable
import com.sdkwithvalues.RequestFlagConverter.toParcelable as requestFlagConverterToParcelable
import com.sdkwithvalues.SdkRequestConverter.toParcelable as sdkRequestConverterToParcelable
import com.sdkwithvalues.SdkResponseConverter.fromParcelable as sdkResponseConverterFromParcelable

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
                it.resumeWith(Result.success(sdkResponseConverterFromParcelable(result)))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.exampleMethod(sdkRequestConverterToParcelable(request), transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun processEnum(requestFlag: RequestFlag): RequestFlag =
            suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IRequestFlagTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: ParcelableRequestFlag) {
                it.resumeWith(Result.success(requestFlagConverterFromParcelable(result)))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processEnum(requestFlagConverterToParcelable(requestFlag), transactionCallback)
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
                        sdkResponseConverterFromParcelable(notNullValue) }))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processNullableValues(request?.let { notNullValue ->
                sdkRequestConverterToParcelable(notNullValue) }, transactionCallback)
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
                it.resumeWith(Result.success(result.map { sdkResponseConverterFromParcelable(it)
                        }.toList()))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.processValueList(x.map { sdkRequestConverterToParcelable(it) }.toTypedArray(),
                transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }
}
