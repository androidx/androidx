package androidx.ui.widgets

/// Log when widgets with global keys are deactivated and log when they are
/// reactivated (retaken).
///
/// This can help track down framework bugs relating to the [GlobalKey] logic.
var debugPrintGlobalKeyedWidgetLifecycle: Boolean = false