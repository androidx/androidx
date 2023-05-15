package com.mysdk

import android.content.Context
import com.mysdk.PrivacySandboxThrowableParcelConverter
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import kotlin.Array
import kotlin.BooleanArray
import kotlin.CharArray
import kotlin.DoubleArray
import kotlin.FloatArray
import kotlin.IntArray
import kotlin.LongArray
import kotlin.String
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class MySecondInterfaceStubDelegate internal constructor(
  public val `delegate`: MySecondInterface,
  public val context: Context,
) : IMySecondInterface.Stub() {
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

  public override fun doIntStuff(x: IntArray, transactionCallback: IListIntTransactionCallback):
      Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.doIntStuff(x.toList())
        transactionCallback.onSuccess(result.toIntArray())
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doCharStuff(x: CharArray, transactionCallback: IListCharTransactionCallback):
      Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.doCharStuff(x.toList())
        transactionCallback.onSuccess(result.toCharArray())
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doFloatStuff(x: FloatArray,
      transactionCallback: IListFloatTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.doFloatStuff(x.toList())
        transactionCallback.onSuccess(result.toFloatArray())
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doLongStuff(x: LongArray, transactionCallback: IListLongTransactionCallback):
      Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.doLongStuff(x.toList())
        transactionCallback.onSuccess(result.toLongArray())
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doDoubleStuff(x: DoubleArray,
      transactionCallback: IListDoubleTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.doDoubleStuff(x.toList())
        transactionCallback.onSuccess(result.toDoubleArray())
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doBooleanStuff(x: BooleanArray,
      transactionCallback: IListBooleanTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.doBooleanStuff(x.toList())
        transactionCallback.onSuccess(result.toBooleanArray())
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doShortStuff(x: IntArray, transactionCallback: IListShortTransactionCallback):
      Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.doShortStuff(x.map { it.toShort() }.toList())
        transactionCallback.onSuccess(result.map { it.toInt() }.toIntArray())
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doStringStuff(x: Array<String>,
      transactionCallback: IListStringTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.doStringStuff(x.toList())
        transactionCallback.onSuccess(result.toTypedArray())
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doValueStuff(x: Array<ParcelableRequest>,
      transactionCallback: IListResponseTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        val result = delegate.doValueStuff(x.map { RequestConverter(context).fromParcelable(it)
            }.toList())
        transactionCallback.onSuccess(result.map { ResponseConverter(context).toParcelable(it)
            }.toTypedArray())
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }
}
