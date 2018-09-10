package androidx.ui.widgets.binding

import androidx.ui.engine.window.Window
import androidx.ui.foundation.binding.BindingBaseImpl
import androidx.ui.rendering.binding.RendererBindingImpl
import androidx.ui.scheduler.binding.SchedulerBindingImpl
import androidx.ui.services.ServicesBindingImpl

// / A concrete binding for applications based on the Widgets framework.
// / This is the glue that binds the framework to the Flutter engine.
class WidgetsFlutterBinding {
// TODO(Migration/Filip): extends BindingBase with GestureBinding, ServicesBinding, SchedulerBinding, PaintingBinding, RendererBinding, WidgetsBinding

    companion object {

        fun create(window: Window): WidgetsBinding {
            val base = BindingBaseImpl()
            return WidgetsBindingImpl(
                    window,
                    base,
                    RendererBindingImpl(window,
                            base,
                            SchedulerBindingImpl(window,
                                    base,
                                    ServicesBindingImpl(base)))
            )
        }
    }
}