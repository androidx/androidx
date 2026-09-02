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
import androidx.compose.runtime.InternalComposeTracingApi
import androidx.startup.Initializer
import androidx.tracing.Tracer

// This is the initializer responsible in bootstrapping Tracing 2.0.
// We cannot refer to this class directly, because apps in g3 are not using initializers at all.
// They are expected to use something like Dagger to bootstrap tracing.
// This also makes it possible for apps using TikTok tracing to do the right thing.
internal const val CONNECTED_PROFILER_TRACING_INITIALIZER =
    "androidx.tracing.profiler.ConnectedProfilerTracingInitializer"

/**
 * Configures Perfetto SDK tracing in the app allowing for capturing Compose specific information
 * (e.g. Composable function names) in a Perfetto SDK trace
 */
@OptIn(InternalComposeTracingApi::class)
public class ComposeTracingInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        composeTraceSink = ComposeTracer(Tracer.global)
        Composer.setTracer(composeTraceSink)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        @Suppress("UNCHECKED_CAST")
        val klass = Class.forName(CONNECTED_PROFILER_TRACING_INITIALIZER) as Class<Initializer<*>>?
        // Be graceful when we cannot find the class on the class path.
        val dependencies = if (klass != null) listOf(klass) else emptyList()
        return dependencies
    }

    internal companion object {
        internal var composeTraceSink: ComposeTracer? = null
    }
}
