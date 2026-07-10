/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.lifecycle

import androidx.lifecycle.Lifecycle.Event
import androidx.tracing.Tracer

internal actual object LifecycleTracer {

    actual inline fun <T> trace(
        name: String,
        event: Event,
        owner: LifecycleOwner?,
        observer: LifecycleObserver?,
        crossinline block: () -> T,
    ): T =
        Tracer.global.trace(
            category = "androidx.lifecycle",
            name = name,
            metadataBlock = {
                addMetadataEntry(
                    "event",
                    // Decodes Lifecycle.Event to String manually to prevent R8/ProGuard obfuscation
                    // from renaming the enum constants in trace outputs.
                    when (event) {
                        Event.ON_CREATE -> "ON_CREATE"
                        Event.ON_START -> "ON_START"
                        Event.ON_RESUME -> "ON_RESUME"
                        Event.ON_PAUSE -> "ON_PAUSE"
                        Event.ON_STOP -> "ON_STOP"
                        Event.ON_DESTROY -> "ON_DESTROY"
                        Event.ON_ANY -> "ON_ANY"
                    },
                )
                owner?.let { addMetadataEntry("owner", it.javaClass.simpleName) }
                observer?.let { addMetadataEntry("observer", it.javaClass.simpleName) }
            },
            block = block,
        )
}
