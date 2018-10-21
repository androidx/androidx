package androidx.ui.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

// TODO(migration/popam)
class SystemNavigator {
    companion object {
        fun CoroutineScope.pop(): Deferred<Unit> {
            return async { }
        }
    }
}