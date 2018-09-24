/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package androidx.ui.gestures.recognizer

import androidx.annotation.CallSuper
import androidx.ui.assert
import androidx.ui.foundation.assertions.FlutterError
import androidx.ui.foundation.assertions.FlutterErrorDetails
import androidx.ui.foundation.debugPrint
import androidx.ui.foundation.diagnostics.DiagnosticPropertiesBuilder
import androidx.ui.foundation.diagnostics.DiagnosticableTree
import androidx.ui.foundation.diagnostics.DiagnosticsProperty
import androidx.ui.gestures.arena.GestureArenaMember
import androidx.ui.gestures.debugPrintGestureArenaDiagnostics
import androidx.ui.gestures.debugPrintRecognizerCallbacksTrace
import androidx.ui.gestures.events.PointerDownEvent

/**
 * The base class that all gesture recognizers inherit from.
 *
 * Provides a basic API that can be used by classes that work with
 * gesture recognizers but don't care about the specific details of
 * the gestures recognizers themselves.
 *
 * See also:
 *
 *  * [GestureDetector], the widget that is used to detect gestures.
 *  * [debugPrintRecognizerCallbacksTrace], a flag that can be set to help
 *    debug issues with gesture recognizers.
 */
abstract class GestureRecognizer(
    /**
     * The recognizer's owner.
     *
     * This is used in the [toString] serialization to report the object for which
     * this gesture recognizer was created, to aid in debugging.
     */
    val debugOwner: Any? = null
) : GestureArenaMember, DiagnosticableTree {
    /**
     * Registers a new pointer that might be relevant to this gesture
     * detector.
     *
     * The owner of this gesture recognizer calls addPointer() with the
     * PointerDownEvent of each pointer that should be considered for
     * this gesture.
     *
     * It's the GestureRecognizer's responsibility to then add itself
     * to the global pointer router (see [PointerRouter]) to receive
     * subsequent events for this pointer, and to add the pointer to
     * the global gesture arena manager (see [GestureArenaManager]) to track
     * that pointer.
     */
    abstract fun addPointer(event: PointerDownEvent)

    /**
     * Releases any resources used by the object.
     *
     * This method is called by the owner of this gesture recognizer
     * when the object is no longer needed (e.g. when a gesture
     * recognizer is being unregistered from a [GestureDetector], the
     * GestureDetector widget calls this method).
     */
    @CallSuper
    open fun dispose() {
    }

    /**
     * Returns a very short pretty description of the gesture that the
     * recognizer looks for, like 'tap' or 'horizontal drag'.
     */
    abstract val debugDescription: String

    /**
     * Invoke a callback provided by the application, catching and logging any
     * exceptions.
     *
     * The `name` argument is ignored except when reporting exceptions.
     *
     * The `debugReport` argument is optional and is used when
     * [debugPrintRecognizerCallbacksTrace] is true. If specified, it must be a
     * callback that returns a string describing useful debugging information,
     * e.g. the arguments passed to the callback.
     */
    protected fun <T> invokeCallback(
        name: String,
        callback: RecognizerCallback<T>,
        debugReport: (() -> String)? = null
    ): T? {
        var result: T? = null
        try {
            assert {
                if (debugPrintRecognizerCallbacksTrace) {
                    val report: String? = if (debugReport != null) debugReport() else null
                    // The 19 in the line below is the width of the prefix used by
                    // _debugLogDiagnostic in arena.dart.
                    val prefix: String =
                        if (debugPrintGestureArenaDiagnostics) " ".repeat(19) + "❙ " else ""
                    debugPrint("$prefix$this calling $name callback." +
                            if (report?.isNotEmpty() == true) " $report" else ""
                    )
                }
                true
            }
            result = callback()
        } catch (exception: Exception) {
            FlutterError.reportError(
                FlutterErrorDetails(
                    exception = exception,
                    stack = exception.stackTrace,
                    library = "gesture",
                    context = "while handling a gesture",
                    informationCollector = { information: StringBuffer ->
                        information.appendln("Handler: $name")
                        information.appendln("Recognizer:")
                        information.appendln("  $this")
                    }
                )
            )
        }
        return result
    }

    override fun debugFillProperties(properties: DiagnosticPropertiesBuilder) {
        super.debugFillProperties(properties)
        properties.add(DiagnosticsProperty.create("debugOwner", debugOwner, defaultValue = null))
    }
}
