package com.mysdk

import android.content.Context
import com.mysdk.PrivacySandboxThrowableParcelConverter
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class MyInterfaceStubDelegate internal constructor(
  public val `delegate`: MyInterface,
  public val context: Context,
) : IMyInterface.Stub() {
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

  public override fun doSomething(request: ParcelableRequest,
      transactionCallback: IResponseTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.doSomething(RequestConverter(context).fromParcelable(request))
        transactionCallback.onSuccess(ResponseConverter(context).toParcelable(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun getMyInterface(input: IMyInterface,
      transactionCallback: IMyInterfaceTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.getMyInterface((input as MyInterfaceStubDelegate).delegate)
        transactionCallback.onSuccess(MyInterfaceStubDelegate(result, context))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun getMySecondInterface(input: IMySecondInterface,
      transactionCallback: IMySecondInterfaceTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.getMySecondInterface((input as
            MySecondInterfaceStubDelegate).delegate)
        transactionCallback.onSuccess(MySecondInterfaceStubDelegate(result, context))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doMoreStuff(x: Int): Unit {
    coroutineScope.launch {
      delegate.doMoreStuff(x)
    }
  }
}
