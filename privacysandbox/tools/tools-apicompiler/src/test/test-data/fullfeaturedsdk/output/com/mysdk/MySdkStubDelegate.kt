package com.mysdk

import android.content.Context
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import com.mysdk.PrivacySandboxThrowableParcelConverter
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import kotlin.Int
import kotlin.IntArray
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

  public override fun handleRequest(request: ParcelableRequest,
      transactionCallback: IResponseTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.handleRequest(RequestConverter(context).fromParcelable(request))
        transactionCallback.onSuccess(ResponseConverter(context).toParcelable(result))
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
    val job = coroutineScope.launch {
      try {
        delegate.logRequest(RequestConverter(context).fromParcelable(request))
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
    coroutineScope.launch {
      delegate.setListener(MyCallbackClientProxy(listener, context))
    }
  }

  public override fun doMoreStuff(): Unit {
    coroutineScope.launch {
      delegate.doMoreStuff()
    }
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

  public override fun mutateMySecondInterface(input: IMySecondInterface): Unit {
    coroutineScope.launch {
      delegate.mutateMySecondInterface((input as MySecondInterfaceStubDelegate).delegate)
    }
  }

  public override fun handleNullablePrimitives(
    x: IntArray,
    y: IntArray,
    transactionCallback: IListStringTransactionCallback,
  ): Unit {
    val job = coroutineScope.launch {
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
    val job = coroutineScope.launch {
      try {
        val result = delegate.handleNullableValues(maybeRequest?.let { notNullValue ->
            RequestConverter(context).fromParcelable(notNullValue) })
        transactionCallback.onSuccess(result?.let { notNullValue ->
            ResponseConverter(context).toParcelable(notNullValue) })
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
    val job = coroutineScope.launch {
      try {
        val result = delegate.handleNullableInterfaces(maybeCallback?.let { notNullValue ->
            MyCallbackClientProxy(notNullValue, context) })
        transactionCallback.onSuccess(result?.let { notNullValue ->
            MyInterfaceStubDelegate(notNullValue, context) })
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun returnUiInterface(transactionCallback: IMyUiInterfaceTransactionCallback):
      Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.returnUiInterface()
        transactionCallback.onSuccess(IMyUiInterfaceCoreLibInfoAndBinderWrapperConverter.toParcelable(result.toCoreLibInfo(context),
            MyUiInterfaceStubDelegate(result, context)))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun acceptUiInterfaceParam(input: IMyUiInterface): Unit {
    coroutineScope.launch {
      delegate.acceptUiInterfaceParam((input as MyUiInterfaceStubDelegate).delegate)
    }
  }
}
