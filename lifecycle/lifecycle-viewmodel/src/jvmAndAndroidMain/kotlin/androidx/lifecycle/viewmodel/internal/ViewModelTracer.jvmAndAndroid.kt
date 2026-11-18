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

package androidx.lifecycle.viewmodel.internal

import androidx.lifecycle.ViewModelProvider
import androidx.tracing.Tracer
import kotlin.reflect.KClass

internal actual object ViewModelTracer {

    /**
     * Tracks whether [Tracer.global] initialized successfully in this process.
     *
     * Do not cache the [Tracer] instance itself because [Tracer.global] can be replaced after
     * startup via `Tracer.setGlobalTracer` or between tests. Cache only permanent initialization
     * failure so later calls skip repeated `NoClassDefFoundError` throws.
     */
    private var isTracerAvailable = true

    /**
     * Returns [Tracer.global], or `null` if [Tracer] static initialization fails.
     *
     * On Android, first access to [Tracer.global] initializes `PerfettoTracer`, which calls
     * `android.os.Process.myPid()`. Plain-JVM unit tests without Robolectric run against stub
     * `android.jar` classes that throw `RuntimeException("Stub!")`. That fails static
     * initialization with [ExceptionInInitializerError] on the first call and
     * [NoClassDefFoundError] on later calls (both [LinkageError]). Once class initialization fails,
     * the JVM marks [Tracer] unusable for this classloader, so tracing is disabled permanently.
     */
    internal fun getTracerOrNull(): Tracer? {
        if (!isTracerAvailable) return null
        return try {
            Tracer.global
        } catch (_: LinkageError) {
            isTracerAvailable = false
            null
        }
    }

    actual inline fun <T> trace(
        name: String,
        modelClass: KClass<*>,
        key: String,
        factory: ViewModelProvider.Factory,
        crossinline block: () -> T,
    ): T {
        // Resolve the tracer outside block() so a LinkageError from ViewModel creation is not
        // caught or retried.
        val tracer = getTracerOrNull()
        return if (tracer != null) {
            tracer.trace(
                category = "androidx.lifecycle.viewmodel",
                name = name,
                metadataBlock = {
                    addMetadataEntry("key", key)
                    addMetadataEntry("class", modelClass.java.simpleName)
                    addMetadataEntry("factory", factory.javaClass.simpleName)
                },
                block = block,
            )
        } else {
            block()
        }
    }
}
