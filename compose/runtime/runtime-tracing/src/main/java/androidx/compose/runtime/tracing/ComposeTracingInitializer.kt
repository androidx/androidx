/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.runtime.tracing

import android.content.Context
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionTracer
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.startup.Initializer
import androidx.tracing.Tracer
import androidx.tracing.profiler.ConnectedProfilerTracingInitializer

// The category being used for Recomposition tracing.
internal const val COMPOSE_TRACING_CATEGORY = "androidx.compose"

/**
 * Configures Perfetto SDK tracing in the app allowing for capturing Compose specific information
 * (e.g. Composable function names) in a Perfetto SDK trace
 */
@OptIn(InternalComposeTracingApi::class)
public class ComposeTracingInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Composer.setTracer(
            object : CompositionTracer {
                @JvmField val closeables = Stack<AutoCloseable>()

                override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) {
                    closeables +=
                        Tracer.global.beginSection(
                            category = COMPOSE_TRACING_CATEGORY,
                            name = info,
                            isRoot = false,
                            token = null,
                            metadataBlock = {},
                        )
                }

                override fun traceEventEnd() {
                    closeables.removeLastOrNull()?.close()
                }

                override fun isTraceInProgress(): Boolean =
                    Tracer.global.isCategoryEnabled(COMPOSE_TRACING_CATEGORY)
            }
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(ConnectedProfilerTracingInitializer::class.java)
    }
}
