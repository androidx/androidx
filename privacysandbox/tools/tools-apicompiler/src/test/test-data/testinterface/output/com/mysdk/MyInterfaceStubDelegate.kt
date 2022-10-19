package com.mysdk

import com.mysdk.RequestConverter.fromParcelable
import com.mysdk.ResponseConverter.toParcelable
import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

public class MyInterfaceStubDelegate internal constructor(
  public val `delegate`: MyInterface,
) : IMyInterface.Stub() {
  public override fun doSomething(request: ParcelableRequest,
      transactionCallback: IResponseTransactionCallback): Unit {
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.doSomething(fromParcelable(request))
        transactionCallback.onSuccess(toParcelable(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(404, t.message)
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun getMyInterface(input: IMyInterface,
      transactionCallback: IMyInterfaceTransactionCallback): Unit {
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.getMyInterface((input as MyInterfaceStubDelegate).delegate)
        transactionCallback.onSuccess(MyInterfaceStubDelegate(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(404, t.message)
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun getMySecondInterface(input: IMySecondInterface,
      transactionCallback: IMySecondInterfaceTransactionCallback): Unit {
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.getMySecondInterface((input as
            MySecondInterfaceStubDelegate).delegate)
        transactionCallback.onSuccess(MySecondInterfaceStubDelegate(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(404, t.message)
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doMoreStuff(x: Int): Unit {
    delegate.doMoreStuff(x)
  }
}
