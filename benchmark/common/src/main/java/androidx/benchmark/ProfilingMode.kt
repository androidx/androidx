/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.benchmark

import android.annotation.SuppressLint

internal enum class ProfilingMode {
    /**
     * Default, no profiling will be done
     */
    None,

    /**
     * Sampled profiling will be captured by the library
     */
    Sampled,

    /**
     * Method profiling will be captured by the library
     */
    Method,

    /**
     * Configure the library for external sampled profiling from Studio, with a pause before
     * and after the primary benchmark loop.
     *
     * Minimum looping time is increased to increase number of samples captured.
     *
     * Note: must set debuggable=true in benchmark AndroidManifest.xml!
     */
    ConnectedSampled,

    /**
     * Configure the library for external sampled profiling from Studio, with a pause before
     * and after the primary benchmark loop.
     *
     * Looping is disabled after warmup, so recorded allocations come from exactly one loop.
     *
     * Note: must set debuggable=true in benchmark AndroidManifest.xml!
     */
    ConnectedAllocation;

    // TODO: update callers to account for profileable, once Studio supports it
    fun requiresDebuggable(): Boolean =
        this == ConnectedSampled || this == ConnectedAllocation

    // `Connected` modes don't need dir, since library isn't doing the capture
    fun needsLibraryOutputDir(): Boolean =
        this == Sampled || this == Method

    companion object {
        @SuppressLint("DefaultLocale")
        fun getFromString(string: String): ProfilingMode? {
            val index = values()
                .map { it.toString().toLowerCase() }
                .indexOf(string.toLowerCase())
            return if (index == -1) {
                null
            } else {
                values()[index]
            }
        }
    }
}
