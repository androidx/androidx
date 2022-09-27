package com.mysdk

import kotlin.Int
import kotlin.Unit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

public class MySdkStubDelegate internal constructor(
  private val `delegate`: MySdk,
) : IMySdk.Stub() {
  public override fun doStuff(
    x: Int,
    y: Int,
    transactionCallback: IStringTransactionCallback,
  ): Unit {
    val job = GlobalScope.launch(Dispatchers.Main) {
      try {
        val result = delegate.doStuff(x, y)
        transactionCallback.onSuccess(result)
      } catch (t: Throwable) {
        transactionCallback.onFailure(404, t.message)
      }
    }
    val cancellationSignal = TransportCancellationCallback() { job.cancel() }
    transactionCallback.onCancellable(cancellationSignal)
  }

  public override fun doMoreStuff(): Unit = delegate.doMoreStuff()
}
