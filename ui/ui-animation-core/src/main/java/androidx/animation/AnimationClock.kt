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

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import androidx.annotation.CallSuper

/**
 * Default Choreographer based clock that pushes a new frame to all subscribers on each
 * Choreographer tick, until all subscribers have unsubscribed.
 */
class DefaultAnimationClock : BaseAnimationClock() {
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    @Volatile private var subscribedToChoreographer = false
    private val frameCallback = Choreographer.FrameCallback {
        dispatchTime(it / 1000000)
    }

    override fun subscribe(observer: AnimationClockObserver) {
        postFrameCallbackToChoreographer()
        super.subscribe(observer)
    }

    private fun postFrameCallbackToChoreographer() {
        if (!subscribedToChoreographer) {
            // Check if we are currently on the main thread
            if (Looper.myLooper() == Looper.getMainLooper()) {
                Choreographer.getInstance().postFrameCallback(frameCallback)
            } else {
                mainThreadHandler.post {
                    Choreographer.getInstance().postFrameCallback(frameCallback)
                }
            }
            subscribedToChoreographer = true
        }
    }

    override fun dispatchTime(frameTimeMillis: Long) {
        super.dispatchTime(frameTimeMillis)
        subscribedToChoreographer = if (hasObservers()) {
            Choreographer.getInstance().postFrameCallback(this@DefaultAnimationClock.frameCallback)
            true
        } else {
            false
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
    // Using LinkedHashSet to increase removal performance
    private val observers: MutableSet<AnimationClockObserver> = LinkedHashSet()

    private val pendingActions: MutableList<Int> = mutableListOf()
    private val pendingObservers: MutableList<AnimationClockObserver> = mutableListOf()

    private fun addToPendingActions(action: Int, observer: AnimationClockObserver) =
        synchronized(pendingActions) {
            pendingActions.add(action) && pendingObservers.add(observer)
        }
    private fun pendingActionsIsNotEmpty(): Boolean = synchronized(pendingActions) {
        pendingActions.isNotEmpty()
    }
    private fun pendingActionsHasAddAction(): Boolean = synchronized(pendingActions) {
        pendingActions.any { it == AddAction }
    }

    private inline fun forEachObserver(crossinline action: (AnimationClockObserver) -> Unit) =
        synchronized(observers) {
            observers.forEach(action)
        }
    private fun observersIsNotEmpty() = synchronized(observers) { observers.isNotEmpty() }

    /**
     * Subscribes [observer] to this clock. Duplicate subscriptions will be ignored.
     */
    override fun subscribe(observer: AnimationClockObserver) {
        addToPendingActions(AddAction, observer)
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        addToPendingActions(RemoveAction, observer)
    }

    @CallSuper
    internal open fun dispatchTime(frameTimeMillis: Long) {
        processPendingActions()

        forEachObserver {
            it.onAnimationFrame(frameTimeMillis)
        }

        while (pendingActionsIsNotEmpty()) {
            processPendingActions().forEach {
                it.onAnimationFrame(frameTimeMillis)
            }
        }
    }

    internal fun hasObservers(): Boolean {
        return observersIsNotEmpty() || pendingActionsHasAddAction()
    }

    // Declare as member for performance
    private val additions: MutableSet<AnimationClockObserver> = LinkedHashSet(50)

    private fun processPendingActions(): Set<AnimationClockObserver> {
        additions.clear()
        synchronized(observers) {
            synchronized(pendingActions) {
                pendingActions.forEachIndexed { i, action ->
                    when (action) {
                        AddAction -> {
                            // This check ensures that we only have one instance of the observer in
                            // the callbacks at any given time.
                            if (observers.add(pendingObservers[i])) {
                                additions.add(pendingObservers[i])
                            }
                        }
                        RemoveAction -> {
                            observers.remove(pendingObservers[i])
                            additions.remove(pendingObservers[i])
                        }
                    }
                }
                pendingActions.clear()
                pendingObservers.clear()
            }
        }
        return additions
    }

    internal companion object {
        private const val AddAction = 1
        private const val RemoveAction = 2
    }
}
