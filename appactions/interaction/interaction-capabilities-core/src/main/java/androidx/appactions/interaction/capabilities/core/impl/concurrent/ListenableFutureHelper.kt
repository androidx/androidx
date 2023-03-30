package androidx.appactions.interaction.capabilities.core.impl.concurrent

import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

// TODO(b/269525385): merge this into Futures utility class once it's migrated to Kotlin.
internal class ListenableFutureHelper {
  companion object {
    fun <T> convertToListenableFuture(suspendFunction: suspend () -> T): ListenableFuture<T> {
      val scope = CoroutineScope(Dispatchers.Default)
      val future = SettableFuture.create<T>()

      scope.launch {
        try {
          val result = suspendFunction()
          future.set(result)
        } catch (t: Throwable) {
          future.setException(t)
        }
      }

      return object : ListenableFuture<T> {
        override fun addListener(listener: Runnable, executor: Executor) {
          future.addListener(listener, executor)
        }

        override fun isDone(): Boolean {
          return future.isDone
        }

        override fun get(): T {
          return future.get()
        }

        override fun get(timeout: Long, unit: TimeUnit): T {
          return future.get(timeout, unit)
        }

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
          return future.cancel(mayInterruptIfRunning)
        }

        override fun isCancelled(): Boolean {
          return future.isCancelled
        }

        // Add a method to explicitly close the scope
        fun close() {
          scope.cancel()
        }
      }
    }
  }
}