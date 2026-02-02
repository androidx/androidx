/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.benchmark.traceprocessor

@ExperimentalTraceProcessorApi
@Suppress("NotCloseable")
public interface ServerLifecycleManager {
    /**
     * Called to Start an instance of Trace Processor
     *
     * @return the port to use to communicate to Trace Processor
     */
    public fun start(): Int

    /**
     * Called to construct a more detailed failure message when [TraceProcessor] cannot be connected
     * to.
     */
    public fun timeoutMessage(): String = "Unable to start perfetto process"

    /** Called to stop the running instance of Trace Processor. */
    public fun stop()
}
