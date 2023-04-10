package com.mysdk

import android.content.Context
import com.mysdk.PrivacySandboxThrowableParcelConverter
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

public class MyInterfaceStubDelegate internal constructor(
  public val `delegate`: MyInterface,
  public val context: Context,
) : IMyInterface.Stub() {
  public override fun doSomething(request: ParcelableRequest,
      transactionCallback: IResponseTransactionCallback): Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
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
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
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
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
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
    delegate.doMoreStuff(x)
  }
}
