package androidx.ui.foundation

import androidx.ui.VoidCallback

/** Whether we've been built in release mode. */
// val _kReleaseMode = Boolean.fromEnvironment('dart.vm.product');
val _kReleaseMode = false

/**
 * When running in profile mode (or debug mode), invoke the given function.
 *
 * In release mode, the function is not invoked.
 */
// TODO(devoncarew): Going forward, we'll want the call to profile() to be tree-shaken out.
fun profile(function: VoidCallback) {
    if (_kReleaseMode)
        return
    function()
}
