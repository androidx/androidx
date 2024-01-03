package com.mysdk

import android.content.Context
import android.os.Bundle
import androidx.privacysandbox.ui.provider.toCoreLibInfo
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import kotlin.Int
import kotlin.IntArray
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
  ) {
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
      transactionCallback: IResponseTransactionCallback) {
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
      transactionCallback: IUnitTransactionCallback) {
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

  public override fun setListener(listener: IMyCallback) {
    coroutineScope.launch {
      delegate.setListener(MyCallbackClientProxy(listener, context))
    }
  }

  public override fun doMoreStuff() {
    coroutineScope.launch {
      delegate.doMoreStuff()
    }
  }

  public override fun getMyInterface(input: IMyInterface,
      transactionCallback: IMyInterfaceTransactionCallback) {
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

  public override fun mutateMySecondInterface(input: IMySecondInterface) {
    coroutineScope.launch {
      delegate.mutateMySecondInterface((input as MySecondInterfaceStubDelegate).delegate)
    }
  }

  public override fun handleNullablePrimitives(
    x: IntArray,
    y: IntArray,
    transactionCallback: IListStringTransactionCallback,
  ) {
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
      transactionCallback: IResponseTransactionCallback) {
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
      transactionCallback: IMyInterfaceTransactionCallback) {
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

  public override fun returnUiInterface(transactionCallback: IMyUiInterfaceTransactionCallback) {
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

  public override fun acceptUiInterfaceParam(input: IMyUiInterfaceCoreLibInfoAndBinderWrapper) {
    coroutineScope.launch {
      delegate.acceptUiInterfaceParam((input.binder as MyUiInterfaceStubDelegate).delegate)
    }
  }

  public override fun acceptSdkActivityLauncherParam(activityLauncher: Bundle) {
    coroutineScope.launch {
      delegate.acceptSdkActivityLauncherParam(SdkActivityLauncherAndBinderWrapper(activityLauncher))
    }
  }

  public override
      fun returnSdkActivityLauncher(transactionCallback: ISdkActivityLauncherTransactionCallback) {
    val job = coroutineScope.launch {
      try {
        val result = delegate.returnSdkActivityLauncher()
        transactionCallback.onSuccess(SdkActivityLauncherAndBinderWrapper.getLauncherInfo(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }
}
