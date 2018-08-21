package androidx.ui.widgets.framework

import androidx.ui.foundation.assertions.FlutterErrorDetails
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.StringProperty
import androidx.ui.rendering.error.RenderErrorBox
import androidx.ui.rendering.obj.RenderObject
import androidx.ui.widgets.framework.key.UniqueKey

// / Signature for the constructor that is called when an error occurs while
// / building a widget.
// /
// / The argument provides information regarding the cause of the error.
// /
// / See also:
// /
// /  * [ErrorWidget.builder], which can be set to override the default
// /    [ErrorWidget] builder.
// /  * [FlutterError.reportError], which is typically called with the same
// /    [FlutterErrorDetails] object immediately prior to [ErrorWidget.builder]
// /    being called.
typealias ErrorWidgetBuilder = (FlutterErrorDetails) -> Widget

// / A widget that renders an exception's message.
// /
// / This widget is used when a build method fails, to help with determining
// / where the problem lies. Exceptions are also logged to the console, which you
// / can read using `flutter logs`. The console will also include additional
// / information such as the stack trace for the exception.
class ErrorWidget(exception: Any) : LeafRenderObjectWidget(key = UniqueKey()) {

    // / The message to display.
    val message = _stringify(exception)

    companion object {
        // / The configurable factory for [ErrorWidget].
        // /
        // / When an error occurs while building a widget, the broken widget is
        // / replaced by the widget returned by this function. By default, an
        // / [ErrorWidget] is returned.
        // /
        // / The system is typically in an unstable state when this function is called.
        // / An exception has just been thrown in the middle of build (and possibly
        // / layout), so surrounding widgets and render objects may be in a rather
        // / fragile state. The framework itself (especially the [BuildOwner]) may also
        // / be confused, and additional exceptions are quite likely to be thrown.
        // /
        // / Because of this, it is highly recommended that the widget returned from
        // / this function perform the least amount of work possible. A
        // / [LeafRenderObjectWidget] is the best choice, especially one that
        // / corresponds to a [RenderBox] that can handle the most absurd of incoming
        // / constraints. The default constructor maps to a [RenderErrorBox].
        // /
        // / See also:
        // /
        // /  * [FlutterError.onError], which is typically called with the same
        // /    [FlutterErrorDetails] object immediately prior to this callback being
        // /    invoked, and which can also be configured to control how errors are
        // /    reported.
        val builder: ErrorWidgetBuilder = { details -> ErrorWidget(details.exception) }

        internal fun _stringify(exception: Any): String {
            try {
                return exception.toString()
            } catch (e: Throwable) { } // ignore: empty_catches
            return "Error"
        }
    }

    override fun createRenderObject(context: BuildContext): RenderObject = RenderErrorBox(message)

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(StringProperty("message", message, quoted = false))
    }
}
