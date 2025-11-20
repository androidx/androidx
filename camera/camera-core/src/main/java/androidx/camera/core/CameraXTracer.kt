/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.camera.core

import androidx.annotation.RestrictTo

/**
 * A utility object for wrapping code blocks in trace events.
 *
 * This object provides a simple way to add performance tracing to CameraX operations. All trace
 * events are prefixed with "CX:" to identify them as CameraX events.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object CameraXTracer {
    /**
     * Wraps the given [Runnable] in a trace event.
     *
     * @param label A descriptive name for the trace event.
     * @param block The code to be executed within the trace event.
     */
    @JvmStatic public fun trace(label: String, block: Runnable): Unit = trace(label, block::run)

    /**
     * Wraps the given code block in a trace event with "CX:" prefixed to identify as CameraX
     * events.
     *
     * @param label A descriptive name for the trace event.
     * @param block The code to be executed within the trace event.
     * @return The result of the [block] execution.
     */
    public inline fun <T> trace(label: String, crossinline block: () -> T): T =
        androidx.tracing.trace("CX:$label") { block() }
}
