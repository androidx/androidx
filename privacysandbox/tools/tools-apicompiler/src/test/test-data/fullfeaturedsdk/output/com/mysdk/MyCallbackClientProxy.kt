package com.mysdk

import android.content.Context
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import com.mysdk.PrivacySandboxThrowableParcelConverter.fromThrowableParcel
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

public class MyCallbackClientProxy(
    public val remote: IMyCallback,
    public val context: Context,
) : MyCallback {
    public override fun onComplete(response: Response) {
        remote.onComplete(ResponseConverter(context).toParcelable(response))
    }

    public override fun onClick(x: Int, y: Int) {
        remote.onClick(x, y)
    }

    public override fun onCompleteInterface(myInterface: MyInterface) {
        remote.onCompleteInterface(MyInterfaceStubDelegate(myInterface, context))
    }

    public override fun onCompleteUiInterface(myUiInterface: MyUiInterface) {
        remote.onCompleteUiInterface(IMyUiInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable(myUiInterface.toCoreLibInfo(context),
                MyUiInterfaceStubDelegate(myUiInterface, context)))
    }

    public override suspend fun returnAValueFromCallback(): Response = suspendCancellableCoroutine {
        var mCancellationSignal: ICancellationSignal? = null
        val transactionCallback = object: IResponseTransactionCallback.Stub() {
            override fun onCancellable(cancellationSignal: ICancellationSignal) {
                if (it.isCancelled) {
                    cancellationSignal.cancel()
                }
                mCancellationSignal = cancellationSignal
            }
            override fun onSuccess(result: ParcelableResponse) {
                it.resumeWith(Result.success(ResponseConverter(context).fromParcelable(result)))
            }
            override fun onFailure(throwableParcel: PrivacySandboxThrowableParcel) {
                it.resumeWithException(fromThrowableParcel(throwableParcel))
            }
        }
        remote.returnAValueFromCallback(transactionCallback)
        it.invokeOnCancellation {
            mCancellationSignal?.cancel()
        }
    }
}
