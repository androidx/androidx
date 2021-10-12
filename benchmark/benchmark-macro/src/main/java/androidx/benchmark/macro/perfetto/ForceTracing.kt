/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.benchmark.macro.perfetto

import android.os.Build
import android.os.Trace

/**
 * Wrapper that uses standard app tracing above API 24, and forces trace sections via reflection
 * on 23 and below.
 *
 * Prior to API 24, all apps with tracing enabled had to be listed in a single setprop, fully
 * enumerated by package name. As a setprop can only be 91 chars long, it's easy to hit cases where
 * a macrobenchmark and its target can't fit into this single setprop. For this reason, prior to 24,
 * the macrobenchmark process doesn't rely on the app tracing tag, and instead forces trace sections
 * via reflection, and the TRACE_TAG_ALWAYS tag.
 */
internal object ForceTracing {
    private val traceBeginMethod = if (Build.VERSION.SDK_INT < 24) {
        android.os.Trace::class.java.getMethod(
            "traceBegin",
            Long::class.javaPrimitiveType,
            String::class.javaPrimitiveType
        )
    } else null

    private val traceEndMethod = if (Build.VERSION.SDK_INT < 24) {
        android.os.Trace::class.java.getMethod(
            "traceEnd",
            Long::class.javaPrimitiveType,
            String::class.javaPrimitiveType
        )
    } else null

    private const val TRACE_TAG_ALWAYS = 1L shl 0 // copied from android.os.Trace

    fun begin(label: String) {
        if (Build.VERSION.SDK_INT < 24) {
            traceBeginMethod!!.invoke(null, TRACE_TAG_ALWAYS, label)
        } else {
            Trace.beginSection(label)
        }
    }

    fun end() {
        if (Build.VERSION.SDK_INT < 24) {
            traceEndMethod!!.invoke(null, TRACE_TAG_ALWAYS)
        } else {
            Trace.endSection()
        }
    }
}

internal inline fun <T> forceTrace(label: String, block: () -> T): T {
    ForceTracing.begin(label)
    return try {
        block()
    } finally {
        ForceTracing.end()
    }
}
