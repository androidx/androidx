/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.arch.core.internal.FastSafeIterableMap
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An implementation of [Lifecycle] that can handle multiple observers.
 *
 * It is used by Fragments and Support Library Activities. You can also directly use it if you have
 * a custom LifecycleOwner.
 */
public actual open class LifecycleRegistry
private constructor(provider: LifecycleOwner, private val enforceMainThread: Boolean) :
    Lifecycle() {
    /**
     * Custom list that keeps observers and can handle removals / additions during traversal.
     *
     * Invariant: at any moment of time for observer1 & observer2: if addition_order(observer1) <
     * addition_order(observer2), then state(observer1) >= state(observer2),
     */
    private var observerMap = FastSafeIterableMap<LifecycleObserver, ObserverWithState>()

    /** Current state */
    private var state: State = State.INITIALIZED

    /**
     * The provider that owns this Lifecycle. Only WeakReference on LifecycleOwner is kept, so if
     * somebody leaks Lifecycle, they won't leak the whole Fragment / Activity. However, to leak
     * Lifecycle object isn't great idea neither, because it keeps strong references on all other
     * listeners, so you'll leak all of them as well.
     */
    private val lifecycleOwner: WeakReference<LifecycleOwner>
    private var addingObserverCounter = 0
    private var handlingEvent = false
    private var newEventOccurred = false

    // we have to keep it for cases:
    // void onStart() {
    //     mRegistry.removeObserver(this);
    //     mRegistry.add(newObserver);
    // }
    // newObserver should be brought only to CREATED state during the execution of
    // this onStart method. our invariant with observerMap doesn't help, because parent observer
    // is no longer in the map.
    private var parentStates = ArrayList<State>()

    /**
     * Creates a new LifecycleRegistry for the given provider.
     *
     * You should usually create this inside your LifecycleOwner class's constructor and hold onto
     * the same instance.
     *
     * @param provider The owner LifecycleOwner
     */
    public actual constructor(provider: LifecycleOwner) : this(provider, true)

    init {
        lifecycleOwner = WeakReference(provider)
    }

    /**
     * Moves the Lifecycle to the given state and dispatches necessary events to the observers.
     *
     * @param state new state
     */
    @MainThread
    @Deprecated("Override [currentState].")
    public open fun markState(state: State) {
        enforceMainThreadIfNeeded("markState")
        currentState = state
    }

    actual override var currentState: State
        get() = state
        /**
         * Moves the Lifecycle to the given state and dispatches necessary events to the observers.
         *
         * @param state new state
         */
        set(state) {
            enforceMainThreadIfNeeded("setCurrentState")
            moveToState(state)
        }

    private val _currentStateFlow: MutableStateFlow<State> = MutableStateFlow(State.INITIALIZED)
    override val currentStateFlow: StateFlow<State>
        get() = _currentStateFlow.asStateFlow()

    /**
     * Sets the current state and notifies the observers.
     *
     * Note that if the `currentState` is the same state as the last call to this method, calling
     * this method has no effect.
     *
     * @param event The event that was received
     */
    public actual open fun handleLifecycleEvent(event: Event) {
        enforceMainThreadIfNeeded("handleLifecycleEvent")
        moveToState(event.targetState)
    }

    private fun moveToState(next: State) {
        if (state == next) {
            return
        }
        @Suppress("NewApi") // b/437073246
        checkLifecycleStateTransition(lifecycleOwner.get(), state, next)

        state = next
        if (handlingEvent || addingObserverCounter != 0) {
            newEventOccurred = true
            // we will figure out what to do on upper level.
            return
        }
        handlingEvent = true
        sync()
        handlingEvent = false
        if (state == State.DESTROYED) {
            observerMap = FastSafeIterableMap()
        }
    }

    private val isSynced: Boolean
        get() {
            if (observerMap.size() == 0) {
                return true
            }
            val eldestObserverState = observerMap.eldest()!!.value.state
            val newestObserverState = observerMap.newest()!!.value.state
            return eldestObserverState == newestObserverState && state == newestObserverState
        }

    private fun calculateTargetState(observer: LifecycleObserver): State {
        val map = observerMap.ceil(observer)
        val siblingState = map?.value?.state
        val parentState =
            if (parentStates.isNotEmpty()) parentStates[parentStates.size - 1] else null
        return min(min(state, siblingState), parentState)
    }

    /**
     * Adds a LifecycleObserver that will be notified when the LifecycleOwner changes state.
     *
     * The given observer will be brought to the current state of the LifecycleOwner. For example,
     * if the LifecycleOwner is in [Lifecycle.State.STARTED] state, the given observer will receive
     * [Lifecycle.Event.ON_CREATE], [Lifecycle.Event.ON_START] events.
     *
     * @param observer The observer to notify.
     * @throws IllegalStateException if no event up from observer's initial state
     */
    @MainThread
    actual override fun addObserver(observer: LifecycleObserver) {
        enforceMainThreadIfNeeded("addObserver")
        val initialState = if (state == State.DESTROYED) State.DESTROYED else State.INITIALIZED
        val statefulObserver = ObserverWithState(observer, initialState)
        val previous = observerMap.putIfAbsent(observer, statefulObserver)
        if (previous != null) {
            return
        }
        @Suppress("NewApi") // b/437073246
        val lifecycleOwner =
            lifecycleOwner.get()
                ?: // it is null we should be destroyed. Fallback quickly
                return
        val isReentrance = addingObserverCounter != 0 || handlingEvent
        var targetState = calculateTargetState(observer)
        addingObserverCounter++
        while (statefulObserver.state < targetState && observerMap.contains(observer)) {
            pushParentState(statefulObserver.state)
            val event =
                Event.upFrom(statefulObserver.state)
                    ?: throw IllegalStateException("no event up from ${statefulObserver.state}")
            statefulObserver.dispatchEvent(lifecycleOwner, event)
            popParentState()
            // mState / subling may have been changed recalculate
            targetState = calculateTargetState(observer)
        }
        if (!isReentrance) {
            // we do sync only on the top level.
            sync()
        }
        addingObserverCounter--
    }

    private fun popParentState() {
        parentStates.removeAt(parentStates.size - 1)
    }

    private fun pushParentState(state: State) {
        parentStates.add(state)
    }

    @MainThread
    actual override fun removeObserver(observer: LifecycleObserver) {
        enforceMainThreadIfNeeded("removeObserver")
        // we consciously decided not to send destruction events here in opposition to addObserver.
        // Our reasons for that:
        // 1. These events haven't yet happened at all. In contrast to events in addObservers, that
        // actually occurred but earlier.
        // 2. There are cases when removeObserver happens as a consequence of some kind of fatal
        // event. If removeObserver method sends destruction events, then a clean up routine becomes
        // more cumbersome. More specific example of that is: your LifecycleObserver listens for
        // a web connection, in the usual routine in OnStop method you report to a server that a
        // session has just ended and you close the connection. Now let's assume now that you
        // lost an internet and as a result you removed this observer. If you get destruction
        // events in removeObserver, you should have a special case in your onStop method that
        // checks if your web connection died and you shouldn't try to report anything to a server.
        observerMap.remove(observer)
    }

    /**
     * The number of observers.
     *
     * @return The number of observers.
     */
    public actual open val observerCount: Int
        get() {
            enforceMainThreadIfNeeded("getObserverCount")
            return observerMap.size()
        }

    private fun forwardPass(lifecycleOwner: LifecycleOwner) {
        @Suppress()
        val ascendingIterator: Iterator<Map.Entry<LifecycleObserver, ObserverWithState>> =
            observerMap.iteratorWithAdditions()
        while (ascendingIterator.hasNext() && !newEventOccurred) {
            val (key, observer) = ascendingIterator.next()
            while (observer.state < state && !newEventOccurred && observerMap.contains(key)) {
                pushParentState(observer.state)
                val event =
                    Event.upFrom(observer.state)
                        ?: throw IllegalStateException("no event up from ${observer.state}")
                observer.dispatchEvent(lifecycleOwner, event)
                popParentState()
            }
        }
    }

    private fun backwardPass(lifecycleOwner: LifecycleOwner) {
        val descendingIterator = observerMap.descendingIterator()
        while (descendingIterator.hasNext() && !newEventOccurred) {
            val (key, observer) = descendingIterator.next()
            while (observer.state > state && !newEventOccurred && observerMap.contains(key)) {
                val event =
                    Event.downFrom(observer.state)
                        ?: throw IllegalStateException("no event down from ${observer.state}")
                pushParentState(event.targetState)
                observer.dispatchEvent(lifecycleOwner, event)
                popParentState()
            }
        }
    }

    // happens only on the top of stack (never in reentrance),
    // so it doesn't have to take in account parents
    private fun sync() {
        @Suppress("NewApi") // b/437073246
        val lifecycleOwner =
            lifecycleOwner.get()
                ?: throw IllegalStateException(
                    "LifecycleOwner of this LifecycleRegistry is already " +
                        "garbage collected. It is too late to change lifecycle state."
                )
        while (!isSynced) {
            newEventOccurred = false
            if (state < observerMap.eldest()!!.value.state) {
                backwardPass(lifecycleOwner)
            }
            val newest = observerMap.newest()
            if (!newEventOccurred && newest != null && state > newest.value.state) {
                forwardPass(lifecycleOwner)
            }
        }
        newEventOccurred = false
        _currentStateFlow.value = currentState
    }

    private fun enforceMainThreadIfNeeded(methodName: String) {
        if (enforceMainThread) {
            check(isMainThread()) { ("Method $methodName must be called on the main thread") }
        }
    }

    internal class ObserverWithState(observer: LifecycleObserver?, initialState: State) {
        var state: State
        var lifecycleObserver: LifecycleEventObserver

        init {
            lifecycleObserver = Lifecycling.lifecycleEventObserver(observer!!)
            state = initialState
        }

        fun dispatchEvent(owner: LifecycleOwner?, event: Event) {
            val newState = event.targetState
            state = min(state, newState)
            lifecycleObserver.onStateChanged(owner!!, event)
            state = newState
        }
    }

    public actual companion object {
        /**
         * Creates a new LifecycleRegistry for the given provider, that doesn't check that its
         * methods are called on the threads other than main.
         *
         * LifecycleRegistry is not synchronized: if multiple threads access this
         * `LifecycleRegistry`, it must be synchronized externally.
         *
         * Another possible use-case for this method is JVM testing, when main thread is not
         * present.
         */
        @JvmStatic
        @VisibleForTesting
        public actual fun createUnsafe(owner: LifecycleOwner): LifecycleRegistry {
            return LifecycleRegistry(owner, false)
        }

        @JvmStatic
        internal fun min(state1: State, state2: State?): State {
            return if ((state2 != null) && (state2 < state1)) state2 else state1
        }
    }
}

internal expect fun isMainThread(): Boolean
