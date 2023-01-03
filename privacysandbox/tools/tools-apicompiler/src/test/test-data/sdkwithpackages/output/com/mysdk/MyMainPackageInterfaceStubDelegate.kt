package com.mysdk

import com.myotherpackage.MyOtherPackageDataClassConverter.fromParcelable
import com.myotherpackage.MyOtherPackageDataClassConverter.toParcelable
import com.myotherpackage.ParcelableMyOtherPackageDataClass
import com.mysdk.PrivacySandboxThrowableParcelConverter
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import kotlin.IntArray
import kotlin.Unit
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

public class MyMainPackageInterfaceStubDelegate internal constructor(
  public val `delegate`: MyMainPackageInterface,
) : IMyMainPackageInterface.Stub() {
  public override fun doIntStuff(x: IntArray, transactionCallback: IListIntTransactionCallback):
      Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
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
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.useDataClass(fromParcelable(x))
        transactionCallback.onSuccess(toParcelable(result))
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }
}
