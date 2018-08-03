package androidx.ui.widgets.framework

import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.assertions.FlutterErrorDetails
import androidx.ui.foundation.assertions.InformationCollector

fun _debugReportException(
        context: String,
        exception : Any,
        stack: Array<StackTraceElement>,
        informationCollector: InformationCollector? = null
): FlutterErrorDetails {
    val details = FlutterErrorDetails(
            exception = exception,
            stack = stack,
            library = "widgets library",
            context = context,
            informationCollector = informationCollector
    );
    FlutterError.reportError(details);
    return details;
}