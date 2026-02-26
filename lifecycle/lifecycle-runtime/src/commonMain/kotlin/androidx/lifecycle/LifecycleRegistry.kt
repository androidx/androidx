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
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleRegistry.Companion.createUnsafe
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * An implementation of [Lifecycle] that can handle multiple observers.
 *
 * It is used by Fragments and Support Library Activities. You can also use it directly if you have
 * a custom [LifecycleOwner].
 */
public open class LifecycleRegistry
private constructor(provider: LifecycleOwner, private val enforceMainThread: Boolean) :
    Lifecycle() {

    /**
     * A map that holds observers and handles removals/additions during traversal.
     *
     * **Invariant:** At any time, for `observer1` & `observer2`: if `addition_order(observer1) <
     * addition_order(observer2)`, then `state(observer1) >= state(observer2)`.
     */
    private var observerMap = FastSafeIterableMap<LifecycleObserver, ObserverWithState>()

    /**
     * The provider that owns this Lifecycle.
     *
     * We use a [WeakReference] to avoid leaking the [LifecycleOwner] (e.g., Fragment or Activity)
     * if this [Lifecycle] is held longer than necessary.
     *
     * **Note:** Leaking this object is still dangerous, as it holds strong references to all its
     * observers.
     */
    private val lifecycleOwner = WeakReference(provider)

    /**
     * Tracks the nesting depth of [addObserver] calls to detect re-entrance.
     *
     * If an observer is added *inside* the callback of another observer (e.g., inside `onCreate`),
     * we increment this counter. If the counter is greater than 0, we are already calculating a
     * target state or dispatching events, so we skip the top-level [sync] call to avoid unsafe
     * processing.
     */
    private var addingObserverCounter = 0

    /**
     * Indicates that the registry is currently traversing the observer list to dispatch events.
     *
     * This guards [moveToState]. If a state transition is requested while we are already handling
     * an event (e.g., an observer calls `markState` in its callback), we cannot simply start a new
     * [sync] loop immediately. Instead, we set [newEventOccurred] to true, allowing the *outer*
     * loop to handle the new state after it finishes its current step.
     */
    private var handlingEvent = false

    /**
     * Signals that a state change occurred *while* the registry was already processing an event or
     * adding an observer.
     *
     * The [sync] loop checks this flag during iteration. If it detects a new event, it aborts the
     * current pass and restarts the synchronization process to ensure all observers converge on the
     * absolute latest state.
     */
    private var newEventOccurred = false

    /**
     * A stack of states used to ensure safe state transitions during re-entrant observer additions.
     *
     * When an observer is added *inside* a lifecycle callback (e.g., within `onStart`), we must
     * prevent the new observer from advancing past the current execution context.
     *
     * **Example:**
     *
     * ```kotlin
     * fun onStart() {
     * registry.removeObserver(this)
     * registry.addObserver(newObserver)
     * }
     * ```
     *
     * In this case, `newObserver` must only be brought up to the [State.CREATED] state while
     * `onStart` is still executing. The standard invariant in `observerMap` fails here because the
     * original observer (the "parent") has already been removed from the map.
     */
    private var parentStates = mutableListOf<State>()

    /**
     * Creates a new LifecycleRegistry for the given provider.
     *
     * You should usually create this inside your [LifecycleOwner] class's constructor and hold onto
     * the same instance.
     *
     * @param provider The owner LifecycleOwner
     */
    public constructor(provider: LifecycleOwner) : this(provider, true)

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

    /** The current internal state of the Lifecycle. */
    private var state = State.INITIALIZED
    override var currentState: State
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

    private val _currentStateFlow = MutableStateFlow(state)
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
    public open fun handleLifecycleEvent(event: Event) {
        enforceMainThreadIfNeeded("handleLifecycleEvent")
        moveToState(event.targetState)
    }

    /**
     * Updates the internal state and triggers the synchronization process.
     *
     * If we are already handling an event or adding an observer, we set [newEventOccurred] to true
     * to signal the active loop to restart, rather than starting a new sync immediately.
     */
    private fun moveToState(next: State) {
        if (state == next) {
            return
        }
        checkLifecycleStateTransition(lifecycleOwner.get(), state, next)

        state = next
        if (handlingEvent || addingObserverCounter != 0) {
            newEventOccurred = true
            // We are already inside a re-entrant call. The active loop
            // will notice this flag and restart to handle the new state.
            return
        }
        handlingEvent = true
        sync()
        handlingEvent = false
        if (state == State.DESTROYED) {
            observerMap = FastSafeIterableMap()
        }
    }

    /**
     * Checks if all observers are caught up to the current state.
     *
     * Because `observerMap` maintains the invariant that older observers have lower/equal states,
     * we only need to check the first (oldest) and last (newest) observers. If both match the
     * registry's state, then all observers in between must also be in that state.
     */
    private val isSynced: Boolean
        get() {
            if (observerMap.size() == 0) {
                return true
            }
            val eldestObserverState = observerMap.first().value.state
            val newestObserverState = observerMap.last().value.state
            return eldestObserverState == newestObserverState && state == newestObserverState
        }

    /**
     * Calculates the target state for a specific observer.
     *
     * We take the minimum of:
     * 1. The registry's current state.
     * 2. The state of the observer immediately preceding this one (to maintain the invariant).
     * 3. The parent state (if we are in a re-entrant call).
     */
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
     * [Lifecycle.Event.ON_CREATE] and [Lifecycle.Event.ON_START] events.
     *
     * @param observer The observer to notify.
     * @throws IllegalStateException if no event exists to move up from the observer's initial
     *   state.
     */
    @MainThread
    override fun addObserver(observer: LifecycleObserver) {
        enforceMainThreadIfNeeded("addObserver")
        val initialState = if (state == State.DESTROYED) State.DESTROYED else State.INITIALIZED
        val statefulObserver = ObserverWithState(observer, initialState)
        val previous = observerMap.putIfAbsent(observer, statefulObserver)
        if (previous != null) {
            return
        }
        val lifecycleOwner = lifecycleOwner.get() ?: return // If null, the owner is destroyed.
        val isReentrance = addingObserverCounter != 0 || handlingEvent
        var targetState = calculateTargetState(observer)
        addingObserverCounter++
        while (statefulObserver.state < targetState && observerMap.contains(observer)) {
            parentStates.add(statefulObserver.state)
            val event =
                Event.upFrom(statefulObserver.state)
                    ?: throw IllegalStateException("no event up from ${statefulObserver.state}")
            statefulObserver.dispatchEvent(lifecycleOwner, event)
            parentStates.removeLastOrNull()
            // The global state or sibling state may have changed during dispatch; recalculate.
            targetState = calculateTargetState(observer)
        }
        if (!isReentrance) {
            // We only run the full sync loop at the top level to avoid recursion issues.
            sync()
        }
        addingObserverCounter--
    }

    @MainThread
    override fun removeObserver(observer: LifecycleObserver) {
        enforceMainThreadIfNeeded("removeObserver")
        // We consciously decided not to send destruction events here, in contrast to addObserver.
        // Reasons:
        // 1. These events haven't occurred naturally. Unlike addObserver (where events historically
        //    happened), destruction is a future event.
        // 2. removeObserver is often called during fatal cleanup. If we dispatched destruction
        //    events here, cleanup logic would become brittle.
        //    Example: An observer manages a web connection. In onStop(), it reports "session ended"
        //    and closes the connection. If you lose internet connection and remove the observer,
        //    dispatching ON_DESTROY here might trigger the "report session ended" logic, which
        //    would fail (no internet) and require complex error handling in the observer.
        observerMap.remove(observer)
    }

    /**
     * The number of observers.
     *
     * @return The number of observers.
     */
    public open val observerCount: Int
        get() {
            enforceMainThreadIfNeeded("getObserverCount")
            return observerMap.size()
        }

    /**
     * Moves observers "up" towards [State.RESUMED].
     *
     * This dispatches events that initialize or activate the component (e.g., [Event.ON_CREATE],
     * [Event.ON_START], [Event.ON_RESUME]).
     *
     * We iterate from **oldest to newest** observers. This ensures that parents (usually added
     * first) are initialized before their children.
     */
    private fun forwardPass(lifecycleOwner: LifecycleOwner) {
        observerMap.forEachWithAdditions { (key, observer) ->
            while (observer.state < state && !newEventOccurred && observerMap.contains(key)) {
                parentStates.add(observer.state)
                val event =
                    checkNotNull(Event.upFrom(observer.state)) {
                        "no event up from ${observer.state}"
                    }
                observer.dispatchEvent(lifecycleOwner, event)
                parentStates.removeLastOrNull()
            }
        }
    }

    /**
     * Moves observers "down" towards [State.DESTROYED].
     *
     * This dispatches events that stop or tear down the component (e.g., [Event.ON_PAUSE],
     * [Event.ON_STOP], [Event.ON_DESTROY]).
     *
     * We iterate from **newest to oldest** observers. This ensures that children (usually added
     * last) are torn down before their parents, mirroring the stack-like behavior of typical
     * cleanup routines.
     */
    private fun backwardPass(lifecycleOwner: LifecycleOwner) {
        observerMap.forEachReversed { (key, observer) ->
            while (observer.state > state && !newEventOccurred && observerMap.contains(key)) {
                val event =
                    checkNotNull(Event.downFrom(observer.state)) {
                        "no event down from ${observer.state}"
                    }
                parentStates.add(event.targetState)
                observer.dispatchEvent(lifecycleOwner, event)
                parentStates.removeLastOrNull()
            }
        }
    }

    /**
     * Synchronizes the state of all observers with the registry's current [state].
     *
     * This method runs a loop that attempts to converge the observers' states toward the registry's
     * state. It handles two directions:
     * 1. **Backward Pass:** If the registry's state is *lower* than the oldest observer (e.g.,
     *    moving towards [State.DESTROYED]), we iterate strictly in reverse order to bring observers
     *    down.
     * 2. **Forward Pass:** If the registry's state is *higher* than the newest observer (e.g.,
     *    moving towards [State.RESUMED]), we iterate in addition order to bring observers up.
     *
     * If a new lifecycle event occurs *during* this traversal (re-entrance), the [newEventOccurred]
     * flag is set. The loop detects this, stops the current pass, and restarts to ensure all
     * observers settle on the most recent state.
     *
     * **Note:** This method must only be called from the top of the stack (never inside a
     * re-entrant call) to avoid stack overflow.
     */
    private fun sync() {
        val lifecycleOwner =
            checkNotNull(lifecycleOwner.get()) {
                "LifecycleOwner of this LifecycleRegistry is already " +
                    "garbage collected. It is too late to change lifecycle state."
            }
        while (!isSynced) {
            newEventOccurred = false
            // If the current state is "lower" than the oldest observer, we bring observers down.
            if (state < observerMap.first().value.state) {
                backwardPass(lifecycleOwner)
            }
            val newest = observerMap.lastOrNull()
            // If the current state is "higher" than the newest observer, we bring observers up.
            if (!newEventOccurred && newest != null && state > newest.value.state) {
                forwardPass(lifecycleOwner)
            }
        }
        newEventOccurred = false
        _currentStateFlow.value = currentState
    }

    /**
     * Ensures the method is called on the main thread if enforcement is enabled.
     *
     * @see createUnsafe
     */
    private fun enforceMainThreadIfNeeded(methodName: String) {
        if (enforceMainThread) {
            check(isMainThread()) { ("Method $methodName must be called on the main thread") }
        }
    }

    /** Wrapper that couples a [LifecycleObserver] with its current [State]. */
    internal class ObserverWithState(observer: LifecycleObserver?, initialState: State) {
        var state = initialState
        var lifecycleObserver = Lifecycling.lifecycleEventObserver(observer!!)

        fun dispatchEvent(owner: LifecycleOwner?, event: Event) {
            val newState = event.targetState
            state = min(state, newState)
            lifecycleObserver.onStateChanged(owner!!, event)
            state = newState
        }
    }

    public companion object {
        /**
         * Creates a new LifecycleRegistry for the given provider that doesn't check if its methods
         * are called on threads other than main.
         *
         * LifecycleRegistry is not synchronized: if multiple threads access this
         * `LifecycleRegistry`, it must be synchronized externally.
         *
         * Another possible use-case for this method is JVM testing, when the main thread is not
         * present.
         */
        @JvmStatic
        @VisibleForTesting
        public fun createUnsafe(owner: LifecycleOwner): LifecycleRegistry {
            return LifecycleRegistry(owner, false)
        }
    }
}

/** Returns the minimum of two states, handling nulls by treating them as "larger". */
private fun min(state1: State, state2: State?): State {
    return if ((state2 != null) && (state2 < state1)) state2 else state1
}

/**
 * Checks the [Lifecycle.State] of a component and throws an error if an invalid state transition is
 * detected.
 *
 * @param owner The [LifecycleOwner] holding the [Lifecycle] of the component.
 * @param current The current [Lifecycle.State] of the component.
 * @param next The desired next [Lifecycle.State] of the component.
 * @throws IllegalStateException if the component is in an invalid state for the desired transition.
 */
private fun checkLifecycleStateTransition(owner: LifecycleOwner?, current: State, next: State) {
    if (current == State.INITIALIZED && next == State.DESTROYED) {
        error(
            "State must be at least '${State.CREATED}' to be moved to '$next' in component $owner"
        )
    }
    if (current == State.DESTROYED && current != next) {
        error("State is '${State.DESTROYED}' and cannot be moved to `$next` in component $owner")
    }
}

/** Returns `true` if the current thread is the Platform Main Thread. */
internal expect fun isMainThread(): Boolean
