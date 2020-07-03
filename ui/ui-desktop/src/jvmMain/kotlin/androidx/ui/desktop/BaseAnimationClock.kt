/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.desktop

import androidx.animation.AnimationClockObservable
import androidx.animation.AnimationClockObserver

/**
 * Copy-pasted androidx.animation.BaseAnimationClock to make possible to use internal
 * dispatchTime.
 * TODO: Move DesktopAnimationClock to ui-animation-core when MPP is ready. Remove this base
 * class after that
 */
abstract class BaseAnimationClock : AnimationClockObservable {
    // Using LinkedHashSet to increase removal performance
    private val observers: MutableSet<AnimationClockObserver> = LinkedHashSet()

    private val pendingActions: MutableList<Int> = mutableListOf()
    private val pendingObservers: MutableList<AnimationClockObserver> = mutableListOf()

    private fun addToPendingActions(action: Int, observer: AnimationClockObserver) =
        synchronized(pendingActions) {
            pendingActions.add(action) && pendingObservers.add(observer)
        }

    private fun pendingActionsIsNotEmpty(): Boolean =
        synchronized(pendingActions) {
            pendingActions.isNotEmpty()
        }

    private inline fun forEachObserver(crossinline action: (AnimationClockObserver) -> Unit) =
        synchronized(observers) {
            observers.forEach(action)
        }

    /**
     * Subscribes [observer] to this clock. Duplicate subscriptions will be ignored.
     */
    override fun subscribe(observer: AnimationClockObserver) {
        addToPendingActions(AddAction, observer)
    }

    override fun unsubscribe(observer: AnimationClockObserver) {
        addToPendingActions(RemoveAction, observer)
    }

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
        synchronized(observers) {
            // Start with processing pending actions: it might remove the last observers
            processPendingActions()
            return observers.isNotEmpty()
        }
    }

    private fun processPendingActions(): Set<AnimationClockObserver> {
        synchronized(observers) {
            synchronized(pendingActions) {
                if (pendingActions.isEmpty()) {
                    return emptySet()
                }
                val additions = LinkedHashSet<AnimationClockObserver>()
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
                return additions
            }
        }
    }

    private companion object {
        private const val AddAction = 1
        private const val RemoveAction = 2
    }
}