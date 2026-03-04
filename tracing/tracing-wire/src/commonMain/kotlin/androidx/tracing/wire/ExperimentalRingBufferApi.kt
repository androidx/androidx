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

package androidx.tracing.wire

/**
 * Marks declarations in the Tracing API that are highly experimental for ring buffer tracing.
 *
 * Ring buffer tracing, such as using a [InMemoryRingBufferTraceSink], is particularly useful for
 * scenarios like anomaly or crash detection. In these cases, continuous tracing is needed but only
 * the most recent events are relevant when an anomaly occurs.
 *
 * Any use of an experimental ring buffer declaration has to be carefully reviewed.
 *
 * Carefully read documentation of any declaration marked as [ExperimentalRingBufferApi].
 */
@RequiresOptIn(message = "Marks APIs that are experimental for ring buffer tracing.")
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalRingBufferApi
