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
 * A class to represent Flow events.
 *
 * [Flow] objects are used to thread flow events between timeline slices,
 * for example, those created with the [Timeline] class below. Adding
 * [Flow] objects cause arrows to be drawn between slices in Chrome's trace
 * viewer. The arrows start at e.g [Timeline] events that are passed a
 * [Flow.begin] object, go through [Timeline] events that are passed a
 * [Flow.step] object, and end at [Timeline] events that are passed a
 * [Flow.end] object, all having the same [Flow.id]. For example:
 *
 * ```dart
 * var flow = Flow.begin();
 * Timeline.timeSync('flow_test', () {
 *   doSomething();
 * }, flow: flow);
 *
 * Timeline.timeSync('flow_test', () {
 *   doSomething();
 * }, flow: Flow.step(flow.id));
 *
 * Timeline.timeSync('flow_test', () {
 *   doSomething();
 * }, flow: Flow.end(flow.id));
 * ```
 */
class Flow internal constructor(
    internal val type: Int,
        // The flow id of the flow event.
    internal val id: Int
) {

    companion object {

        // These values must be kept in sync with the enum "EventType" in
        // runtime/vm/timeline.h.
        private const val begin = 9
        private const val step = 10
        private const val end = 11

        /**
         * A "begin" Flow event.
         *
         * When passed to a [Timeline] method, generates a "begin" Flow event.
         * If [id] is not provided, an id that conflicts with no other Dart-generated
         * flow id's will be generated.
         */
        fun begin(id: Int? = null): Flow {
            return Flow(begin, id ?: _getNextAsyncId())
        }

        /**
         * A "step" Flow event.
         *
         * When passed to a [Timeline] method, generates a "step" Flow event.
         * The [id] argument is required. It can come either from another [Flow]
         * event, or some id that comes from the environment.
         */
        fun step(id: Int) = Flow(step, id)

        /**
         * An "end" Flow event.
         *
         * When passed to a [Timeline] method, generates a "end" Flow event.
         * The [id] argument is required. It can come either from another [Flow]
         * event, or some id that comes from the environment.
         */
        fun end(id: Int) = Flow(end, id)
    }
}