package com.sdk

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public class MyInterfaceClientProxy(
    public val remote: IMyInterface,
) : MyInterface {
    public override suspend fun add(x: Int, y: Int): Int = suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IIntTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: Int) {
                it.resumeWith(Result.success(result))
            }
            override fun onFailure(errorCode: Int, errorMessage: String) {
                it.resumeWithException(RuntimeException(errorMessage))
            }
        }
        remote.add(x, y, transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override fun doSomething(firstInterface: MyInterface,
            secondInterface: MySecondInterface): Unit {
        remote.doSomething((firstInterface as MyInterfaceClientProxy).remote, (secondInterface as
                MySecondInterfaceClientProxy).remote)
    }
}
