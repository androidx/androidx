package androidx.ui.foundation.assertions

import androidx.ui.foundation.debugPrint

/**
 * Signature for [FlutterErrorDetails.informationCollector] callback
 * and other callbacks that collect information into a string buffer.
 */
typealias InformationCollector = (StringBuffer) -> Unit

/** Signature for [FlutterError.onError] handler. */
typealias FlutterExceptionHandler = (FlutterErrorDetails) -> Unit

/**
 * Dump the current stack to the console using [debugPrint] and
 * [FlutterError.defaultStackFilter].
 *
 * The current stack is obtained using [StackTrace.current].
 *
 * The `maxFrames` argument can be given to limit the stack to the given number
 * of lines. By default, all non-filtered stack lines are shown.
 *
 * The `label` argument, if present, will be printed before the stack.
 */
fun debugPrintStack(label: String, maxFrames: Int? = null) {
    if (label != null)
        debugPrint(label)
    var lines = Thread.currentThread().getStackTrace().toString().trimEnd().split("\n")
    if (maxFrames != null)
        lines = lines.take(maxFrames)
    // TODO(Migration/Filip): This is enough at this stage
    debugPrint(lines.joinToString(separator = "\n"))
    // debugPrint(FlutterError.defaultStackFilter(lines).join('\n'));
}