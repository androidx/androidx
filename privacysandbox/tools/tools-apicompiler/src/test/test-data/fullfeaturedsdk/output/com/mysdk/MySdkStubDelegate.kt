package com.mysdk

import com.mysdk.PrivacySandboxThrowableParcelConverter
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import com.mysdk.RequestConverter
import com.mysdk.RequestConverter.fromParcelable
import com.mysdk.ResponseConverter
import com.mysdk.ResponseConverter.toParcelable
import kotlin.Int
import kotlin.IntArray
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

  public override fun handleRequest(request: ParcelableRequest,
      transactionCallback: IResponseTransactionCallback): Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.handleRequest(fromParcelable(request))
        transactionCallback.onSuccess(toParcelable(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun logRequest(request: ParcelableRequest,
      transactionCallback: IUnitTransactionCallback): Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        delegate.logRequest(fromParcelable(request))
        transactionCallback.onSuccess()
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
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
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.getMyInterface((input as MyInterfaceStubDelegate).delegate)
        transactionCallback.onSuccess(MyInterfaceStubDelegate(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun mutateMySecondInterface(input: IMySecondInterface): Unit {
    delegate.mutateMySecondInterface((input as MySecondInterfaceStubDelegate).delegate)
  }

  public override fun handleNullablePrimitives(
    x: IntArray,
    y: IntArray,
    transactionCallback: IListStringTransactionCallback,
  ): Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.handleNullablePrimitives(x.firstOrNull(), y.firstOrNull())
        transactionCallback.onSuccess(if (result == null) arrayOf() else arrayOf(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun handleNullableValues(maybeRequest: ParcelableRequest?,
      transactionCallback: IResponseTransactionCallback): Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.handleNullableValues(maybeRequest?.let { notNullValue ->
            fromParcelable(notNullValue) })
        transactionCallback.onSuccess(result?.let { notNullValue -> toParcelable(notNullValue) })
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun handleNullableInterfaces(maybeCallback: IMyCallback?,
      transactionCallback: IMyInterfaceTransactionCallback): Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.handleNullableInterfaces(maybeCallback?.let { notNullValue ->
            MyCallbackClientProxy(notNullValue) })
        transactionCallback.onSuccess(result?.let { notNullValue ->
            MyInterfaceStubDelegate(notNullValue) })
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }
}
