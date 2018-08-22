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

package androidx.ui.scheduler.binding

/**
 * The various phases that a [SchedulerBinding] goes through during
 * [SchedulerBinding.handleBeginFrame].
 *
 * This is exposed by [SchedulerBinding.schedulerPhase].
 *
 * The values of this enum are ordered in the same order as the phases occur,
 * so their relative index values can be compared to each other.
 *
 * See also the discussion at [WidgetsBinding.drawFrame].
 */
internal enum class SchedulerPhase {
    /**
     * No frame is being processed. Tasks (scheduled by
     * [WidgetsBinding.scheduleTask]), microtasks (scheduled by
     * [scheduleMicrotask]), [Timer] callbacks, event handlers (e.g. from user
     * input), and other callbacks (e.g. from [Future]s, [Stream]s, and the like)
     * may be executing.
     */
    idle,

    /**
     * The transient callbacks (scheduled by
     * [WidgetsBinding.scheduleFrameCallback]) are currently executing.
     *
     * Typically, these callbacks handle updating objects to new animation
     * states.
     *
     * See [SchedulerBinding.handleBeginFrame].
     */
    transientCallbacks,

    /**
     * Microtasks scheduled during the processing of transient callbacks are
     * current executing.
     *
     * This may include, for instance, callbacks from futures resulted during the
     * [transientCallbacks] phase.
     */
    midFrameMicrotasks,

    /**
     * The persistent callbacks (scheduled by
     * [WidgetsBinding.addPersistentFrameCallback]) are currently executing.
     *
     * Typically, this is the build/layout/paint pipeline. See
     * [WidgetsBinding.drawFrame] and [SchedulerBinding.handleDrawFrame].
     */
    persistentCallbacks,

    /*
     ** The post-frame callbacks (scheduled by
     * [WidgetsBinding.addPostFrameCallback]) are currently executing.
     *
     * Typically, these callbacks handle cleanup and scheduling of work for the
     * next frame.
     *
     * See [SchedulerBinding.handleDrawFrame].
     */
    postFrameCallbacks
}