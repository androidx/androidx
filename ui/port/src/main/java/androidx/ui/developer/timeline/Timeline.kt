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
 * Add to the timeline.
 *
 * [Timeline]'s methods add synchronous events to the timeline. When
 * generating a timeline in Chrome's tracing format, using [Timeline] generates
 * "Complete" events. [Timeline]'s [startSync] and [finishSync] can be used
 * explicitly, or implicitly by wrapping a closure in [timeSync]. For exmaple:
 *
 * ```dart
 * Timeline.startSync("Doing Something");
 * doSomething();
 * Timeline.finishSync();
 * ```
 *
 * Or:
 *
 * ```dart
 * Timeline.timeSync("Doing Something", () {
 *   doSomething();
 * });
 * ```
 */
class Timeline {

    companion object {

        /**
         * Start a synchronous operation labeled [name]. Optionally takes
         * a [Map] of [arguments]. This slice may also optionally be associated with
         * a [Flow] event. This operation must be finished before
         * returning to the event queue.
         */
        fun startSync(name: String, arguments: Map<*, *>? = null, flow: Flow? = null) {
            if (_isProduct) {
                return
            }
            if (!_isDartStreamEnabled()) {
                // Push a null onto the stack and return.
                _stack.add(null)
                return
            }
            var block = SyncBlock(name, _getTraceClock(), _getThreadCpuClock())
            if (arguments is Map) {
                block._arguments = arguments
            }
            if (flow is Flow) {
                block.flow = flow
            }
            _stack.add(block)
        }

        /**
         * Finish the last synchronous operation that was started.
         */
        fun finishSync() {
            if (_isProduct) {
                return
            }
            if (_stack.isEmpty()) {
                throw IllegalStateException("Uneven calls to startSync and finishSync")
            }
            // Pop top item off of stack.
            val block = _stack.removeAt(_stack.lastIndex)
            if (block == null) {
                // Dart stream was disabled when startSync was called.
                return
            }
            // Finish it.
            block.finish()
        }

        /**
         * Emit an instant event.
         */
        fun instantSync(name: String, arguments: Map<*, *>? = null) {
            if (_isProduct) {
                return
            }
            if (!_isDartStreamEnabled()) {
                // Stream is disabled.
                return
            }
            val instantArguments = arguments?.toMap()
            _reportInstantEvent(
                    _getTraceClock(),
                    "Dart", // TODO(Migration/Andrey): Kotlin? :)
                    name,
                    _argumentsAsJson(instantArguments))
        }

        /**
         * A utility method to time a synchronous [function]. Internally calls
         * [function] bracketed by calls to [startSync] and [finishSync].
         */
        fun timeSync(
            name: String,
            function: TimelineSyncFunction,
            arguments: Map<*, *>? = null,
            flow: Flow? = null
        ): Any {
            startSync(name, arguments, flow)
            try {
                return function()
            } finally {
                finishSync()
            }
        }

        /**
         * The current time stamp from the clock used by the timeline. Units are
         * microseconds.
         */
        internal val now get() = _getTraceClock()
        internal val _stack = mutableListOf<SyncBlock?>()
    }
}
    