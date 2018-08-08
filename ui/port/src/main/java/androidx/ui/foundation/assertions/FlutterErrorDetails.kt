package androidx.ui.foundation.assertions

import androidx.ui.foundation.IterableFilter

// / Signature for [FlutterErrorDetails.informationCollector] callback
// / and other callbacks that collect information into a string buffer.
typealias InformationCollector = (StringBuffer) -> Unit

// / Class for information provided to [FlutterExceptionHandler] callbacks.
// /
// / See [FlutterError.onError].
// /
// / Ctor comment:
// / Creates a [FlutterErrorDetails] object with the given arguments setting
// / the object's properties.
// /
// / The framework calls this constructor when catching an exception that will
// / subsequently be reported using [FlutterError.onError].
// /
// / The [exception] must not be null; other arguments can be left to
// / their default values. (`throw null` results in a
// / [NullThrownError] exception.)
class FlutterErrorDetails(
        // / The exception. Often this will be an [AssertionError], maybe specifically
        // / a [FlutterError]. However, this could be any value at all.
    val exception: Any,
        // / The stack trace from where the [exception] was thrown (as opposed to where
        // / it was caught).
        // /
        // / StackTrace objects are opaque except for their [toString] function.
        // /
        // / If this field is not null, then the [stackFilter] callback, if any, will
        // / be called with the result of calling [toString] on this object and
        // / splitting that result on line breaks. If there's no [stackFilter]
        // / callback, then [FlutterError.defaultStackFilter] is used instead. That
        // / function expects the stack to be in the format used by
        // / [StackTrace.toString].
    val stack: Array<StackTraceElement>,
        // / A human-readable brief name describing the library that caught the error
        // / message. This is used by the default error handler in the header dumped to
        // / the console.
    val library: String = "Flutter framework",
        // / A human-readable description of where the error was caught (as opposed to
        // / where it was thrown).
    val context: String,
        // / A callback which filters the [stack] trace. Receives an iterable of
        // / strings representing the frames encoded in the way that
        // / [StackTrace.toString()] provides. Should return an iterable of lines to
        // / output for the stack.
        // /
        // / If this is not provided, then [FlutterError.dumpErrorToConsole] will use
        // / [FlutterError.defaultStackFilter] instead.
        // /
        // / If the [FlutterError.defaultStackFilter] behavior is desired, then the
        // / callback should manually call that function. That function expects the
        // / incoming list to be in the [StackTrace.toString()] format. The output of
        // / that function, however, does not always follow this format.
        // /
        // / This won't be called if [stack] is null.
    val stackFilter: IterableFilter<String>? = null,
        // / A callback which, when called with a [StringBuffer] will write to that buffer
        // / information that could help with debugging the problem.
        // /
        // / Information collector callbacks can be expensive, so the generated information
        // / should be cached, rather than the callback being called multiple times.
        // /
        // / The text written to the information argument may contain newlines but should
        // / not end with a newline.
    val informationCollector: InformationCollector? = null,
        // / Whether this error should be ignored by the default error reporting
        // / behavior in release mode.
        // /
        // / If this is false, the default, then the default error handler will always
        // / dump this error to the console.
        // /
        // / If this is true, then the default error handler would only dump this error
        // / to the console in checked mode. In release mode, the error is ignored.
        // /
        // / This is used by certain exception handlers that catch errors that could be
        // / triggered by environmental conditions (as opposed to logic errors). For
        // / example, the HTTP library sets this flag so as to not report every 404
        // / error to the console on end-user devices, while still allowing a custom
        // / error handler to see the errors even in release builds.
    val silent: Boolean = false
) {

    // / Converts the [exception] to a string.
    // /
    // / This applies some additional logic to make [AssertionError] exceptions
    // / prettier, to handle exceptions that stringify to empty strings, to handle
    // / objects that don't inherit from [Exception] or [Error], and so forth.
    fun exceptionAsString(): String {
        var longMessage: String = ""
        if (exception is AssertionError) {
            // Regular _AssertionErrors thrown by assert() put the message last, after
            // some code snippets. This leads to ugly messages. To avoid this, we move
            // the assertion message up to before the code snippets, separated by a
            // newline, if we recognise that format is being used.
            val message = exception.message
            val fullMessage = exception.toString()
            if (message is String && message != fullMessage) {
                if (fullMessage.length > message.length) {
                    val position = fullMessage.lastIndexOf(message)
                    if (position == fullMessage.length - message.length &&
                            position > 2 &&
                            fullMessage.substring(position - 2, position) == ": ") {
                        longMessage = "${message.trimEnd()}\n" +
                                fullMessage.substring(0, position - 2)
                    }
                }
            }
            longMessage = longMessage ?: fullMessage
        } else if (exception is String) {
            longMessage = exception
        } else if (exception is Error || exception is Exception) {
            longMessage = exception.toString()
        } else {
            longMessage = "  $exception"
        }
        longMessage = longMessage.trimEnd()
        if (longMessage.isEmpty())
            longMessage = "  <no message available>"
        return longMessage
    }

    override fun toString(): String {
        val buffer = StringBuffer()
        if ((library != null && library != "") || (context != null && context != "")) {
            if (library != null && library != "") {
                buffer.append("Error caught by $library")
                if (context != null && context != "")
                    buffer.append(", ")
            } else {
                buffer.appendln("Exception ")
            }
            if (context != null && context != "")
                buffer.append("thrown $context")
            buffer.appendln(".")
        } else {
            buffer.append("An error was caught.")
        }
        buffer.appendln(exceptionAsString())
        if (informationCollector != null)
            informationCollector!!(buffer)
        if (stack != null) {

            var stackLines = stack.map { it.toString() }.asIterable()

            if (stackFilter != null) {
                stackLines = stackFilter!!(stackLines)
            } else {
                // TODO(Migration/Filip): This will not be that easy as it expects Dart stack lines
                // stackLines = FlutterError.defaultStackFilter(stackLines);
            }
            buffer.append(stackLines)
            buffer.append("\n")
        }
        return buffer.toString().trimEnd()
    }
}