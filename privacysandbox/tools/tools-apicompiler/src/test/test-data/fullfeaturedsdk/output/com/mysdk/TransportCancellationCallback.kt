package com.mysdk

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.Unit

internal class TransportCancellationCallback internal constructor(
  private val onCancel: () -> Unit,
) : ICancellationSignal.Stub() {
  private val hasCancelled: AtomicBoolean = AtomicBoolean(false)

  public override fun cancel() {
    if (hasCancelled.compareAndSet(false, true)) {
      onCancel()
    }
  }
}
