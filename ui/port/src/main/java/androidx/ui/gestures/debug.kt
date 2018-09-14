/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.ui.gestures

import androidx.ui.assert
import androidx.ui.foundation.assertions.FlutterError

// Any changes to this file should be reflected in the debugAssertAllGesturesVarsUnset()
// function below.

// / Whether to print the results of each hit test to the console.
// /
// / When this is set, in debug mode, any time a hit test is triggered by the
// / [GestureBinding] the results are dumped to the console.
// /
// / This has no effect in release builds.
var debugPrintHitTestResults = false

// / Prints information about gesture recognizers and gesture arenas.
// /
// / This flag only has an effect in debug mode.
// /
// / See also:
// /
// /  * [GestureArenaManager], the class that manages gesture arenas.
// /  * [debugPrintRecognizerCallbacksTrace], for debugging issues with
// /    gesture recognizers.
var debugPrintGestureArenaDiagnostics = false

// / Logs a message every time a gesture recognizer callback is invoked.
// /
// / This flag only has an effect in debug mode.
// /
// / This is specifically used by [GestureRecognizer.invokeCallback]. Gesture
// / recognizers that do not use this method to invoke callbacks may not honor
// / the [debugPrintRecognizerCallbacksTrace] flag.
// /
// / See also:
// /
// /  * [debugPrintGestureArenaDiagnostics], for debugging issues with gesture
// /    arenas.
var debugPrintRecognizerCallbacksTrace = false

// / Returns true if none of the gestures library debug variables have been changed.
// /
// / This function is used by the test framework to ensure that debug variables
// / haven't been inadvertently changed.
// /
// / See [https://docs.flutter.io/flutter/gestures/gestures-library.html] for
// / a complete list.
fun debugAssertAllGesturesVarsUnset(reason: String): Boolean {
    assert {
        if (debugPrintHitTestResults ||
            debugPrintGestureArenaDiagnostics ||
            debugPrintRecognizerCallbacksTrace
        )
            throw FlutterError(reason)
        true
    }
    return true
}