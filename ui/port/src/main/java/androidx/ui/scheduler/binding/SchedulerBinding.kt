package androidx.ui.scheduler.binding

import androidx.ui.engine.window.AppLifecycleState
import androidx.ui.foundation.binding.BindingBase
import androidx.ui.foundation.binding.BindingBaseImpl
import androidx.ui.services.ServicesBinding
import androidx.ui.services.ServicesBindingImpl

// Stub. TODO(migration/popam)
interface SchedulerBinding : ServicesBinding {
    fun handleAppLifecycleStateChanged(state: AppLifecycleState)
    fun ensureVisualUpdate()
}

open class SchedulerMixinsWrapper(
    base: BindingBase,
    services: ServicesBinding
) : BindingBase by base, ServicesBinding by services

object SchedulerBindingImpl : SchedulerMixinsWrapper(
        BindingBaseImpl,
        ServicesBindingImpl
), SchedulerBinding {

    override fun handleAppLifecycleStateChanged(state: AppLifecycleState) {
        TODO("not implemented")
    }

    override fun ensureVisualUpdate() {
        TODO("not implemented")
    }
}