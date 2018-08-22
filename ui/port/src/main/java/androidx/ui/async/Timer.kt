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

package androidx.ui.async

import android.os.Handler
import androidx.ui.core.Duration

/**
 * A count-down timer that can be configured to fire once or repeatedly.
 *
 * The timer counts down from the specified duration to 0.
 * When the timer reaches 0, the timer invokes the specified callback function.
 * Use a periodic timer to repeatedly count down the same interval.
 *
 * A negative duration is treated the same as a duration of 0.
 * If the duration is statically known to be 0, consider using [run].
 *
 * Frequently the duration is either a constant or computed as in the
 * following example (taking advantage of the multiplication operator of
 * the [Duration] class):
 * ```dart
 * const timeout = const Duration(seconds: 3);
 * const ms = const Duration(milliseconds: 1);
 *
 * startTimeout([int milliseconds]) {
 *   var duration = milliseconds == null ? timeout : ms * milliseconds;
 *   return new Timer(duration, handleTimeout);
 * }
 * ...
 * void handleTimeout() {  // callback function
 *   ...
 * }
 * ```
 * Note: If Dart code using Timer is compiled to JavaScript, the finest
 * granularity available in the browser is 4 milliseconds.
 *
 * See [Stopwatch] for measuring elapsed time.
 */
abstract class Timer {

    companion object {

        /**
         * Creates a new timer.
         *
         * The [callback] function is invoked after the given [duration].
         *
         */
        fun create(duration: Duration, callback: () -> Unit): Timer {
            // TODO(Migration/Andrey): temporary porting the logic with the Android's Handler
            val handler = Handler()
            handler.postDelayed(callback, duration.inMilliseconds)
            return object : Timer() {
                override fun cancel() {
                    handler.removeCallbacks(callback)
                }

                override val tick: Int
                    get() = TODO()

                override val isActive: Boolean
                    get() = TODO()
            }

//            if (Zone.current == Zone.root) {
//                // No need to bind the callback. We know that the root's timer will
//                // be invoked in the root zone.
//                return Zone.current.createTimer(duration, callback);
//            }
//            return Zone.current
//                    .createTimer(duration, Zone.current.bindCallbackGuarded(callback));
        }

//        /**
//         * Creates a new repeating timer.
//         *
//         * The [callback] is invoked repeatedly with [duration] intervals until
//         * canceled with the [cancel] function.
//         *
//         * The exact timing depends on the underlying timer implementation.
//         * No more than `n` callbacks will be made in `duration * n` time,
//         * but the time between two consecutive callbacks
//         * can be shorter and longer than `duration`.
//         *
//         * In particular, an implementation may schedule the next callback, e.g.,
//         * a `duration` after either when the previous callback ended,
//         * when the previous callback started, or when the previous callback was
//         * scheduled for - even if the actual callback was delayed.
//         */
//        factory Timer.periodic(Duration duration, void callback(Timer timer)) {
//            if (Zone.current == Zone.root) {
//                // No need to bind the callback. We know that the root's timer will
//                // be invoked in the root zone.
//                return Zone.current.createPeriodicTimer(duration, callback);
//            }
//            var boundCallback = Zone.current.bindUnaryCallbackGuarded<Timer>(callback);
//            return Zone.current.createPeriodicTimer(duration, boundCallback);
//        }

        /**
         * Runs the given [callback] asynchronously as soon as possible.
         *
         * This function is equivalent to `new Timer(Duration.zero, callback)`.
         */
        fun run(callback: () -> Unit) {
            create(Duration.zero, callback)
        }

//        external static Timer _createTimer(Duration duration, void callback());
//        external static Timer _createPeriodicTimer(
//        Duration duration, void callback(Timer timer));
    }

    /**
     * Cancels the timer.
     */
    abstract fun cancel()

    /**
     * The number of durations preceding the most recent timer event.
     *
     * The value starts at zero and is incremented each time a timer event
     * occurs, so each callback will see a larger value than the previous one.
     *
     * If a periodic timer with a non-zero duration is delayed too much,
     * so more than one tick should have happened,
     * all but the last tick in the past are considered "missed",
     * and no callback is invoked for them.
     * The [tick] count reflects the number of durations that have passed and
     * not the number of callback invocations that have happened.
     */
    abstract val tick: Int

    /**
     * Returns whether the timer is still active.
     *
     * A non-periodic timer is active if the callback has not been executed,
     * and the timer has not been canceled.
     *
     * A periodic timer is active if it has not been canceled.
     */
    abstract val isActive: Boolean
}