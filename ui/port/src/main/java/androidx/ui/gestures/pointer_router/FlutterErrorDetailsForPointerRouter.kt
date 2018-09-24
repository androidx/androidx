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

package androidx.ui.gestures.pointer_router

import androidx.ui.foundation.assertions.FlutterErrorDetails
import androidx.ui.foundation.assertions.InformationCollector
import androidx.ui.gestures.events.PointerEvent

/**
 * Variant of [FlutterErrorDetails] with extra fields for the gestures
 * library's pointer router ([PointerRouter]).
 *
 * See also [FlutterErrorDetailsForPointerEventDispatcher], which is also used
 * by the gestures library.
 */
class FlutterErrorDetailsForPointerRouter(
    exception: Exception,
    stack: Array<StackTraceElement>,
    library: String,
    context: String,
    /**
     * The pointer router that caught the exception.
     *
     * In a typical application, this is the value of [GestureBinding.pointerRouter] on
     * the binding ([GestureBinding.instance]).
     */
    val router: PointerRouter,
    /** The callback that threw the exception. */
    val route: PointerRoute,
    /** The pointer event that was being routed when the exception was raised. */
    val event: PointerEvent,
    informationCollector: InformationCollector,
    silent: Boolean = false
) : FlutterErrorDetails(
    exception = exception,
    stack = stack,
    library = library,
    context = context,
    informationCollector = informationCollector,
    silent = silent
)