package com.mysdk

import android.content.Context
import com.myotherpackage.MyOtherPackageInterfaceStubDelegate
import com.mysdk.PrivacySandboxThrowableParcelConverter
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class MySdkStubDelegate internal constructor(
  public val `delegate`: MySdk,
  public val context: Context,
) : IMySdk.Stub() {
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

  public override fun doStuff(
    x: Int,
    y: Int,
    transactionCallback: IStringTransactionCallback,
  ): Unit {
    val job = coroutineScope.launch {
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
    val job = coroutineScope.launch {
      try {
        val result = delegate.getMyInterface()
        transactionCallback.onSuccess(MyMainPackageInterfaceStubDelegate(result, context))
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
    val job = coroutineScope.launch {
      try {
        val result = delegate.getMyOtherPackageInterface()
        transactionCallback.onSuccess(MyOtherPackageInterfaceStubDelegate(result, context))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }
}
