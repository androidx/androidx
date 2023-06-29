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
import androidx.tracing.perfetto.Trace

/**
 * Configures Perfetto SDK tracing in the app allowing for capturing Compose specific
 * information (e.g. Composable function names) in a Perfetto SDK trace
 */
@OptIn(InternalComposeTracingApi::class)
class ComposeTracingInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Composer.setTracer(object : CompositionTracer {
            override fun traceEventStart(key: Int, dirty1: Int, dirty2: Int, info: String) =
                Trace.beginSection(info)

            override fun traceEventEnd() = Trace.endSection()

            override fun isTraceInProgress(): Boolean = Trace.isEnabled
        })
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
