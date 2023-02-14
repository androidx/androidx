package com.myotherpackage

import com.myotherpackage.MyOtherPackageDataClassConverter.fromParcelable
import com.mysdk.IUnitTransactionCallback
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import com.mysdk.TransportCancellationCallback
import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

public class MyOtherPackageInterfaceStubDelegate internal constructor(
  public val `delegate`: MyOtherPackageInterface,
) : IMyOtherPackageInterface.Stub() {
  public override fun doStuff(x: Int): Unit {
    delegate.doStuff(x)
  }

  public override fun useDataClass(x: ParcelableMyOtherPackageDataClass,
      transactionCallback: IUnitTransactionCallback): Unit {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        delegate.useDataClass(fromParcelable(x))
        transactionCallback.onSuccess()
      }
      catch (t: Throwable) {
        transactionCallback.onFailure(toThrowableParcel(t))
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }
}
