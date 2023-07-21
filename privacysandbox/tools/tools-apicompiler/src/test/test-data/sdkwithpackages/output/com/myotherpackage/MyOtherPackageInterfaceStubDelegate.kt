package com.myotherpackage

import android.content.Context
import com.mysdk.IUnitTransactionCallback
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import com.mysdk.TransportCancellationCallback
import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class MyOtherPackageInterfaceStubDelegate internal constructor(
  public val `delegate`: MyOtherPackageInterface,
  public val context: Context,
) : IMyOtherPackageInterface.Stub() {
  private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)

  public override fun doStuff(x: Int): Unit {
    coroutineScope.launch {
      delegate.doStuff(x)
    }
  }

  public override fun useDataClass(x: ParcelableMyOtherPackageDataClass,
      transactionCallback: IUnitTransactionCallback): Unit {
    val job = coroutineScope.launch {
      try {
        delegate.useDataClass(MyOtherPackageDataClassConverter(context).fromParcelable(x))
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
