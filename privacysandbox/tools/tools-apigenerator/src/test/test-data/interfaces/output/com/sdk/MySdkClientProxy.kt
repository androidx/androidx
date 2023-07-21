package com.sdk

import com.sdk.PrivacySandboxThrowableParcelConverter
import com.sdk.PrivacySandboxThrowableParcelConverter.fromThrowableParcel
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public class MySdkClientProxy(
    public val remote: IMySdk,
) : MySdk {
    public override suspend fun getInterface(): MyInterface = suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IMyInterfaceTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: IMyInterface) {
                it.resumeWith(Result.success(MyInterfaceClientProxy(result)))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.getInterface(transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun getUiInterface(): MySecondInterface = suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IMySecondInterfaceTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: IMySecondInterfaceCoreLibInfoAndBinderWrapper) {
                it.resumeWith(Result.success(MySecondInterfaceClientProxy(result.binder,
                        result.coreLibInfo)))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.getUiInterface(transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override suspend fun maybeGetInterface(): MyInterface? = suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IMyInterfaceTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: IMyInterface?) {
                it.resumeWith(Result.success(result?.let { notNullValue ->
                        MyInterfaceClientProxy(notNullValue) }))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.maybeGetInterface(transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }
}
