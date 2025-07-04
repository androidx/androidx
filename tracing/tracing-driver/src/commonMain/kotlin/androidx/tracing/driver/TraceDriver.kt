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

/** The entry point for the tracing API. */
public class TraceDriver(private val sink: TraceSink, private val isEnabled: Boolean = true) {
    public val context: TraceContext =
        if (isEnabled) {
            TraceContext(sink = sink, isEnabled = true)
        } else {
            EmptyTraceContext
        }

    /**
     * @param id is the Process id.
     * @param name is the name of the Process.
     * @return a [ProcessTrack] instance that we can associate trace packets to.
     */
    public fun ProcessTrack(id: Int, name: String): ProcessTrack =
        context.getOrCreateProcessTrack(id, name)
}
