/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.developer.timeline

/**
 * A synchronous block of time on the timeline. This block should not be
 * kept open across isolate messages.
 */
internal class SyncBlock(
        // The name of this block.
    private val name: String?,
        // The start time stamp.
    private val start: Long,
        // The start time stamp of the thread cpu clock.
    private val startCpu: Long
) {

    /**
     * The category this block belongs to.
     */
    val category: String = "Dart" // TODO(Migration/Andrey): Kotlin? :)

    /**
     * An (optional) set of arguments which will be serialized to JSON and
     * associated with this block.
     */
    internal var _arguments: Map<*, *>? = null

    /**
     * An (optional) flow event associated with this block.
     */
    internal var flow: Flow? = null

    /**
     * Finish this block of time. At this point, this block can no longer be
     * used.
     */
    fun finish() {
        // Report event to runtime.
        _reportCompleteEvent(
                start, startCpu, category, name, _argumentsAsJson(_arguments))
        flow?.let {
            _reportFlowEvent(start, startCpu, category, name, it.type, it.id,
                    _argumentsAsJson(null))
        }
    }
}