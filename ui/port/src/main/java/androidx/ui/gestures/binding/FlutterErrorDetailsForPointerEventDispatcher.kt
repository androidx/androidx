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

package androidx.ui.gestures.binding

import androidx.ui.foundation.assertions.FlutterErrorDetails
import androidx.ui.foundation.assertions.InformationCollector
import androidx.ui.gestures.events.PointerEvent
import androidx.ui.gestures.hit_test.HitTestEntry

// / Variant of [FlutterErrorDetails] with extra fields for the gesture
// / library's binding's pointer event dispatcher ([GestureBinding.dispatchEvent]).
// /
// / See also [FlutterErrorDetailsForPointerRouter], which is also used by the
// / gesture library.
class FlutterErrorDetailsForPointerEventDispatcher(
    exception: Exception,
    stack: Array<StackTraceElement>? = null,
    library: String? = null,
    context: String? = null,
    // / The pointer event that was being routed when the exception was raised.
    val event: PointerEvent? = null,
    // / The hit test result entry for the object whose handleEvent method threw
    // / the exception.
    // /
    // / The target object itself is given by the [HitTestEntry.target] property of
    // / the hitTestEntry object.
    val hitTestEntry: HitTestEntry? = null,
    informationCollector: InformationCollector? = null,
    silent: Boolean = false
) : FlutterErrorDetails(
    exception = exception,
    stack = stack,
    library = library,
    context = context,
    informationCollector = informationCollector,
    silent = silent
)