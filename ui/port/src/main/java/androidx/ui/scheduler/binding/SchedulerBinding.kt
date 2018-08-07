package androidx.ui.scheduler.binding

import androidx.ui.engine.window.AppLifecycleState

// Stub. TODO(migration/popam)
interface SchedulerBinding {
    fun handleAppLifecycleStateChanged(state: AppLifecycleState) {
        TODO("migration/popam/Implement this")
    }
    fun ensureVisualUpdate() {
        TODO("migration/popam/Implement this")
    }
}

class SchedulerBindingImpl : SchedulerBinding