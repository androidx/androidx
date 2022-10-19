package com.mysdk

import com.mysdk.RequestConverter
import com.mysdk.RequestConverter.fromParcelable
import com.mysdk.ResponseConverter.toParcelable
import kotlin.Int
import kotlin.Unit
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
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.doStuff(x, y)
        transactionCallback.onSuccess(result)
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(404, t.message)
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun handleRequest(request: ParcelableRequest,
      transactionCallback: IResponseTransactionCallback): Unit {
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.handleRequest(fromParcelable(request))
        transactionCallback.onSuccess(toParcelable(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(404, t.message)
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun logRequest(request: ParcelableRequest,
      transactionCallback: IUnitTransactionCallback): Unit {
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.logRequest(fromParcelable(request))
        transactionCallback.onSuccess()
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(404, t.message)
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun setListener(listener: IMyCallback): Unit {
    delegate.setListener(MyCallbackClientProxy(listener))
  }

  public override fun doMoreStuff(): Unit {
    delegate.doMoreStuff()
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

  public override fun mutateMySecondInterface(input: IMySecondInterface): Unit {
    delegate.mutateMySecondInterface((input as MySecondInterfaceStubDelegate).delegate)
  }
}
