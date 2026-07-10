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

package androidx.tracing.profiler

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.startup.Initializer
import androidx.tracing.AbstractTraceDriver
import androidx.tracing.AbstractTraceDriver.Factory
import androidx.tracing.Tracer
import androidx.tracing.wire.TraceDriver
import androidx.tracing.wire.TraceSink

/**
 * Initializes [TraceDriver] process wide using `androidx.startup`.
 *
 * To customize the [AbstractTraceDriver] instance to use, your [android.app.Application] subtype
 * should implement [AbstractTraceDriver.Factory].
 */
@RestrictTo(Scope.LIBRARY_GROUP)
public class ConnectedProfilerTracingInitializer : Initializer<AbstractTraceDriver> {
    override fun create(context: Context): AbstractTraceDriver {
        // There is already a lock acquired by andoidx.startup here.
        // So this code does not have to be guarded by a lock.
        ConnectedProfilerTracing.initialize(context)

        // If the application subtype provides a custom implementation of an
        // TraceDriver, use it. Otherwise, fallback to the default initializer.
        val provider =
            // Support both ContextWrappers and applicationContext based lookups.
            context as? Factory ?: context.applicationContext as? Factory

        val driver =
            if (provider != null) {
                provider.create()
            } else {
                val sink = TraceSink(context = context)
                val driver =
                    TraceDriver(
                        context = context,
                        sink = sink,
                        isCategoryEnabled = { ConnectedProfilerTracing.readAfterInitialize() },
                    )
                driver
            }
        Tracer.setGlobalTracer(driver.tracer)
        return driver
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
