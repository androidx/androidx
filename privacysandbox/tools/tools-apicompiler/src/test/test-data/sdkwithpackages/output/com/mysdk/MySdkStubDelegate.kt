package com.mysdk

import com.myotherpackage.MyOtherPackageInterfaceStubDelegate
import com.mysdk.PrivacySandboxThrowableParcelConverter
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

public class MySdkStubDelegate internal constructor(
  public val `delegate`: MySdk,
) : IMySdk.Stub() {
  public override fun doStuff(
    x: Int,
    y: Int,
    transactionCallback: IStringTransactionCallback,
  ): Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.doStuff(x, y)
        transactionCallback.onSuccess(result)
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override
      fun getMyInterface(transactionCallback: IMyMainPackageInterfaceTransactionCallback): Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.getMyInterface()
        transactionCallback.onSuccess(MyMainPackageInterfaceStubDelegate(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override
      fun getMyOtherPackageInterface(transactionCallback: IMyOtherPackageInterfaceTransactionCallback):
      Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.getMyOtherPackageInterface()
        transactionCallback.onSuccess(MyOtherPackageInterfaceStubDelegate(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }
}
