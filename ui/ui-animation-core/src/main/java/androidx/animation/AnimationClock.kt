/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.animation

import android.view.Choreographer

/**
 * Default Choreographer based clock that pushes a new frame to all subscribers on each
 * Choreographer tick, until all subscribers have unsubscribed.
 */
class DefaultAnimationClock : BaseAnimationClock() {
    private val frameCallback = Choreographer.FrameCallback {
        dispatchTime(it / 1000000)
    }

    override fun subscribe(observer: AnimationClockObserver) {
        // TODO: Support subscription from non-UI thread
        if (observers.isEmpty()) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
        super.subscribe(observer)
    }

    override fun dispatchTime(frameTimeMillis: Long) {
        super.dispatchTime(frameTimeMillis)
        if (observers.isNotEmpty()) {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }
    }
}

/**
 * A custom clock whose frame time can be manually updated via mutating [clockTimeMillis].
 */
class ManualAnimationClock(initTimeMillis: Long) : BaseAnimationClock() {
    /**
     * Clock time in milliseconds. When [clockTimeMillis] is updated, the [ManualAnimationClock]
     * notifies all its observers (i.e. animations) the new clock time. The animations will
     * consequently snap to the new play time.
     */
    var clockTimeMillis: Long = initTimeMillis
        set(value) {
            field = value
            // Notify subscribers when the value is set
            dispatchTime(value)
        }

    override fun subscribe(observer: AnimationClockObserver) {
        super.subscribe(observer)
        // Immediately push the current frame time to the new subscriber
        observer.onAnimationFrame(clockTimeMillis)
    }
}

/**
 * Base implementation for the AnimationClockObservable that handles the subscribing and
 * unsubscribing logic that would be common for all custom animation clocks.
 */
sealed class BaseAnimationClock : AnimationClockObservable {
    // By allowing this observers list to be mutated during iteration, it means sequential animation
    // will see an animation end and the subsequent animation start in the same frame. This is a
    // desirable outcome. Does it have any side effects?
    internal val observers: MutableList<AnimationClockObserver> = mutableListOf()
    private val toBeRemoved: MutableList<AnimationClockObserver> = mutableListOf()

    override fun subscribe(observer: AnimationClockObserver) {
        // TODO: Support subscription from non-UI thread
        observers.add(observer)
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        // TODO: Support removing subscription from non-UI thread
        if (observers.contains(observer)) {
            // FIXME(147736746): checking if observers contains the observer can still trigger a
            //  bug, if the observer is subscribed and unsubscribed multiple times per frame. For
            //  example: subscribe(x), unsubscribe(x), unsubscribe(x), subscribe(x)
            toBeRemoved.add(observer)
        }
    }

    internal open fun dispatchTime(frameTimeMillis: Long) {
        reset()
        // Start dispatching to observers the new frame time
        for (observer in observers) {
            observer.onAnimationFrame(frameTimeMillis)
        }
        reset()
    }

    private fun reset() {
        // Remove the first occurrence of the removed items in observers list
        toBeRemoved.forEach {
            observers.remove(it)
        }
        toBeRemoved.clear()
    }
}
