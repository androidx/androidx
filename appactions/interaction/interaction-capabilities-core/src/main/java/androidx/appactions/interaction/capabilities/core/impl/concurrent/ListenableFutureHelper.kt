package androidx.appactions.interaction.capabilities.core.impl.concurrent

import androidx.annotation.RestrictTo
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO(b/269525385): merge this into Futures utility class once it's migrated to Kotlin.
/** @suppress */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun <T> convertToListenableFuture(
    tag: String,
    block: suspend CoroutineScope.() -> T,
): ListenableFuture<T> {
    val scope = CoroutineScope(Dispatchers.Default)
    return CallbackToFutureAdapter.getFuture { completer ->
        val job =
            scope.launch {
                try {
                    completer.set(scope.block())
                } catch (t: Throwable) {
                    completer.setException(t)
                }
            }
        completer.addCancellationListener(
            { job.cancel() },
            Runnable::run,
        )
        "ListenableFutureHelper#convertToListenableFuture for '$tag'"
    }
}
