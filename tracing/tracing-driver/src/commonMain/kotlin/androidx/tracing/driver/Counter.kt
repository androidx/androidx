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

package androidx.tracing.driver

/** Useful to emit counters into a Trace. */
public abstract class Counter {
    public abstract fun name(): String

    /** Emits a [Long] value into the trace for the provided [name]. */
    public abstract fun setValue(value: Long)

    /** Emits a [Double] value into the trace for the provided [name]. */
    public abstract fun setValue(value: Double)
}
