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

package androidx.ui.scheduler

import androidx.ui.assert
import androidx.ui.foundation.assertions.FlutterError

/**
 * Print a banner at the beginning of each frame.
 *
 * Frames triggered by the engine and handler by the scheduler binding will
 * have a banner giving the frame number and the time stamp of the frame.
 *
 * Frames triggered eagerly by the widget framework (e.g. when calling
 * [runApp]) will have a label saying "warm-up frame" instead of the time stamp
 * (the time stamp sent to frame callbacks in that case is the time of the last
 * frame, or 0:00 if it is the first frame).
 *
 * To include a banner at the end of each frame as well, to distinguish
 * intra-frame output from inter-frame output, set [debugPrintEndFrameBanner]
 * to true as well.
 *
 * See also:
 *
 *  * [debugProfilePaintsEnabled], which does something similar for
 *    painting but using the timeline view.
 *
 *  * [debugPrintLayouts], which does something similar for layout but using
 *    console output.
 *
 *  * The discussions at [WidgetsBinding.drawFrame] and at
 *    [SchedulerBinding.handleBeginFrame].
 */
internal val debugPrintBeginFrameBanner = false

/**
 * Print a banner at the end of each frame.
 *
 * Combined with [debugPrintBeginFrameBanner], this can be helpful for
 * determining if code is running during a frame or between frames.
 */
internal val debugPrintEndFrameBanner = false

/**
 * Log the call stacks that cause a frame to be scheduled.
 *
 * This is called whenever [SchedulerBinding.scheduleFrame] schedules a frame. This
 * can happen for various reasons, e.g. when a [Ticker] or
 * [AnimationController] is started, or when [RenderObject.markNeedsLayout] is
 * called, or when [State.setState] is called.
 *
 * To get a stack specifically when widgets are scheduled to be built, see
 * [debugPrintScheduleBuildForStacks].
 */
internal val debugPrintScheduleFrameStacks = false

/**
 * Returns true if none of the scheduler library debug variables have been changed.
 *
 * This function is used by the test framework to ensure that debug variables
 * haven't been inadvertently changed.
 *
 * See [https://docs.flutter.io/flutter/scheduler/scheduler-library.html] for
 * a complete list.
 */
internal fun debugAssertAllSchedulerVarsUnset(reason: String): Boolean {
    assert {
        if (debugPrintBeginFrameBanner ||
                debugPrintEndFrameBanner) {
            throw FlutterError(reason)
        }
        true
    }
    return true
}
