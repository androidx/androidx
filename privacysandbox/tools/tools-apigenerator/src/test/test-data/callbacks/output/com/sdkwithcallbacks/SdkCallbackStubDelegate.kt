package com.sdkwithcallbacks

import android.os.Bundle
import com.sdkwithcallbacks.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import com.sdkwithcallbacks.ResponseConverter.fromParcelable
import com.sdkwithcallbacks.ResponseConverter.toParcelable
import com.sdkwithcallbacks.SdkActivityLauncherConverter.getLocalOrProxyLauncher
import kotlin.Int
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class SdkCallbackStubDelegate internal constructor(
  public val `delegate`: SdkCallback,
) : ISdkCallback.Stub() {
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

  public override fun onCompleteInterface(myInterface: IMyInterface) {
    coroutineScope.launch {
      delegate.onCompleteInterface(MyInterfaceClientProxy(myInterface))
    }
  }

  public override fun onEmptyEvent() {
    coroutineScope.launch {
      delegate.onEmptyEvent()
    }
  }

  public override fun onPrimitivesReceived(x: Int, y: Int) {
    coroutineScope.launch {
      delegate.onPrimitivesReceived(x, y)
    }
  }

  public override fun onSdkActivityLauncherReceived(myLauncher: Bundle) {
    coroutineScope.launch {
      delegate.onSdkActivityLauncherReceived(getLocalOrProxyLauncher(myLauncher))
    }
  }

  public override fun onValueReceived(response: ParcelableResponse) {
    coroutineScope.launch {
      delegate.onValueReceived(fromParcelable(response))
    }
  }

  public override fun testing(transactionCallback: IResponseTransactionCallback) {
    val job = coroutineScope.launch {
      try {
        val result = delegate.testing()
        transactionCallback.onSuccess(toParcelable(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }
}
