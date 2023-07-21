package com.mysdk

import android.content.Context
import com.mysdk.PrivacySandboxThrowableParcelConverter.toThrowableParcel
import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

public class WithoutRuntimeLibrarySdkStubDelegate internal constructor(
  public val `delegate`: WithoutRuntimeLibrarySdk,
  public val context: Context,
) : IWithoutRuntimeLibrarySdk.Stub() {
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
}
