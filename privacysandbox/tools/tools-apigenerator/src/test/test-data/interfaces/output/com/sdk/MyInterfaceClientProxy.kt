package com.sdk

import androidx.privacysandbox.activity.core.SdkActivityLauncher
import com.sdk.PrivacySandboxThrowableParcelConverter.fromThrowableParcel
import com.sdk.SdkActivityLauncherConverter.toBinder
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
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.add(x, y, transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }

    public override fun doSomething(firstInterface: MyInterface,
            secondInterface: MySecondInterface) {
        remote.doSomething((firstInterface as MyInterfaceClientProxy).remote,
                IMySecondInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable((secondInterface
                as MySecondInterfaceClientProxy).coreLibInfo, secondInterface.remote))
    }

    public override fun doSomethingWithNullableInterface(maybeInterface: MySecondInterface?) {
        remote.doSomethingWithNullableInterface(maybeInterface?.let { notNullValue ->
                IMySecondInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable((notNullValue as
                MySecondInterfaceClientProxy).coreLibInfo, notNullValue.remote) })
    }

    public override fun doSomethingWithSdkActivityLauncher(launcher: SdkActivityLauncher) {
        remote.doSomethingWithSdkActivityLauncher(toBinder(launcher))
    }
}
