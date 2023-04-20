package com.mysdk

import android.content.Context
import com.myotherpackage.MyOtherPackageDataClassConverter
import com.myotherpackage.ParcelableMyOtherPackageDataClass
import com.mysdk.PrivacySandboxThrowableParcelConverter
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import kotlin.IntArray
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class MyMainPackageInterfaceStubDelegate internal constructor(
  public val `delegate`: MyMainPackageInterface,
  public val context: Context,
) : IMyMainPackageInterface.Stub() {
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

  public override fun useDataClass(x: ParcelableMyOtherPackageDataClass,
      transactionCallback: IMyOtherPackageDataClassTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        val result =
            delegate.useDataClass(MyOtherPackageDataClassConverter(context).fromParcelable(x))
        transactionCallback.onSuccess(MyOtherPackageDataClassConverter(context).toParcelable(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }
}
