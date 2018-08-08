package androidx.ui.services

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async

// TODO(migration/popam)
class SystemNavigator {
    companion object {
        fun pop(): Deferred<Unit> {
            return async { }
        }
    }
}