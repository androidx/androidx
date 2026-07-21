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
 * A [Lifecycle] implementation that manages multiple [LifecycleObserver]s.
 *
 * Commonly used by UI containers (like Activities or Fragments on Android) to manage component
 * lifecycles. Can be used directly to implement custom [LifecycleOwner] components.
 */
public open class LifecycleRegistry
private constructor(provider: LifecycleOwner, private val enforceMainThread: Boolean) :
    Lifecycle() {

    /**
     * A map that holds [LifecycleObserver]s and handles removals/additions during traversal.
     *
     * **Invariant:** At any time, for `observer1` & `observer2`: if `addition_order(observer1) <
     * addition_order(observer2)`, then `state(observer1) >= state(observer2)`.
     */
    private var observerMap = FastSafeIterableMap<LifecycleObserver, ObserverWithState>()

    /**
     * The provider that owns this [Lifecycle].
     *
     * Uses a [WeakReference] to avoid leaking the [LifecycleOwner] if this [Lifecycle] is held
     * longer than necessary.
     *
     * Note: Leaking this object is still dangerous, as it holds strong references to all its
     * [LifecycleObserver]s.
     */
    private val lifecycleOwner = WeakReference(provider)

    /**
     * Tracks the nesting depth of [addObserver] calls to detect re-entrance.
     *
     * Skips top-level [sync] calls when the counter is greater than 0 to avoid unsafe processing if
     * a [LifecycleObserver] is added inside another [LifecycleObserver]'s callback.
     */
    private var addingObserverCounter = 0

    /**
     * Indicates whether the registry is traversing the [LifecycleObserver] list to dispatch
     * [Event]s.
     *
     * Guards [moveToState]. Setting [newEventOccurred] to true if a state transition is requested
     * during dispatch, allowing the outer loop to process the new state later.
     */
    private var handlingEvent = false

    /**
     * Signals that a state change occurred while processing an [Event] or adding a
     * [LifecycleObserver].
     *
     * Checked by [sync] during iteration to abort and restart synchronization if a new [Event] is
     * detected, ensuring [LifecycleObserver]s converge on the latest state.
     */
    private var newEventOccurred = false

    /**
     * A stack of states used to ensure safe [State] transitions during re-entrant
     * [LifecycleObserver] additions.
     *
     * When a [LifecycleObserver] is added inside a [Lifecycle] callback (e.g., within `onStart`),
     * we must prevent the new [LifecycleObserver] from advancing past the current execution
     * context.
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
     * original [LifecycleObserver] (the "parent") has already been removed from the map.
     */
    private val parentStates = mutableListOf<State>()

    /**
     * Creates a new [LifecycleRegistry] for the given [provider].
     *
     * You should usually create this inside your [LifecycleOwner] class's constructor and hold onto
     * the same instance.
     *
     * @param provider The owner [LifecycleOwner]
     */
    public constructor(provider: LifecycleOwner) : this(provider, enforceMainThread = true)

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

    /** The current internal [State] of the [Lifecycle]. */
    private var internalState = State.INITIALIZED

    /**
     * The current [State] of the [Lifecycle].
     *
     * Transitions the [Lifecycle] to the given target [State] and dispatches the corresponding
     * [Event]s to any registered [LifecycleObserver]s.
     *
     * @throws IllegalStateException if main-thread enforcement is enabled and called on a thread
     *   other than the main thread.
     */
    override var currentState: State
        get() = internalState
        set(state) {
            enforceMainThreadIfNeeded("setCurrentState")
            moveToState(state)
        }

    private val _currentStateFlow = MutableStateFlow(internalState)
    override val currentStateFlow: StateFlow<State>
        get() = _currentStateFlow.asStateFlow()

    /**
     * Sets the current [State] and notifies the [LifecycleObserver]s.
     *
     * No-ops if the target state of the [event] matches [currentState].
     *
     * @param event The [Event] to process.
     * @throws IllegalStateException if main-thread enforcement is enabled and called on a thread
     *   other than the main thread.
     */
    public open fun handleLifecycleEvent(event: Event) {
        enforceMainThreadIfNeeded("handleLifecycleEvent")
        LifecycleTracer.trace(
            name = "LifecycleRegistry#handleLifecycleEvent",
            owner = lifecycleOwner.get(),
            event = event,
        ) {
            moveToState(event.targetState)
        }
    }

    /**
     * Updates the [internalState] and triggers the synchronization process.
     *
     * If we are already handling an event or adding a [LifecycleObserver], we set
     * [newEventOccurred] to true to signal the active loop to restart, rather than starting a new
     * sync immediately.
     */
    private fun moveToState(next: State) {
        if (internalState == next) {
            return
        }
        checkLifecycleStateTransition(lifecycleOwner.get(), internalState, next)

        internalState = next
        if (handlingEvent || addingObserverCounter != 0) {
            newEventOccurred = true
            // We are already inside a re-entrant call. The active loop
            // will notice this flag and restart to handle the new state.
            return
        }
        handlingEvent = true
        sync()
        handlingEvent = false
        if (internalState == State.DESTROYED) {
            observerMap = FastSafeIterableMap()
        }
    }

    /**
     * Checks if all [LifecycleObserver]s are caught up to the [internalState].
     *
     * Because `observerMap` maintains the invariant that older [LifecycleObserver]s have
     * lower/equal states, we only need to check the first (oldest) and last (newest)
     * [LifecycleObserver]s. If both match the registry's state, then all [LifecycleObserver]s in
     * between must also be in that state.
     */
    private val isSynced: Boolean
        get() {
            if (observerMap.size() == 0) {
                return true
            }
            val eldestObserverState = observerMap.first().value.state
            val newestObserverState = observerMap.last().value.state
            return eldestObserverState == newestObserverState &&
                internalState == newestObserverState
        }

    /**
     * Calculates the target [State] for a specific [LifecycleObserver].
     *
     * We take the minimum of:
     * 1. The registry's current [internalState].
     * 2. The state of the [LifecycleObserver] immediately preceding this one (to maintain the
     *    invariant).
     * 3. The parent state (if we are in a re-entrant call).
     */
    private fun calculateTargetState(observer: LifecycleObserver): State {
        val siblingState = observerMap.ceil(observer)?.value?.state ?: State.RESUMED
        val parentState = parentStates.lastOrNull() ?: State.RESUMED
        return minOf(internalState, siblingState, parentState)
    }

    /**
     * Adds a [LifecycleObserver] that will be notified when the [LifecycleOwner] changes state.
     *
     * The given [observer] will be brought to the current [State] of the [LifecycleOwner]. For
     * example, if the [LifecycleOwner] is in [Lifecycle.State.STARTED], the given [observer]
     * receives [Lifecycle.Event.ON_CREATE] and [Lifecycle.Event.ON_START] [Event]s.
     *
     * @param observer The [LifecycleObserver] to notify.
     * @throws IllegalStateException if no event exists to move up from the observer's initial
     *   state.
     * @throws IllegalStateException if main-thread enforcement is enabled and called on a thread
     *   other than the main thread.
     */
    @MainThread
    override fun addObserver(observer: LifecycleObserver) {
        enforceMainThreadIfNeeded("addObserver")
        val initialState =
            if (internalState == State.DESTROYED) State.DESTROYED else State.INITIALIZED
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
                checkNotNull(Event.upFrom(statefulObserver.state)) {
                    "no event up from ${statefulObserver.state}"
                }
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

    /**
     * Removes the given [observer] from the list of registered [LifecycleObserver]s.
     *
     * @param observer The [LifecycleObserver] to remove.
     * @throws IllegalStateException if main-thread enforcement is enabled and called on a thread
     *   other than the main thread.
     */
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
     * The number of registered [LifecycleObserver]s.
     *
     * @throws IllegalStateException if main-thread enforcement is enabled and called on a thread
     *   other than the main thread.
     */
    public open val observerCount: Int
        get() {
            enforceMainThreadIfNeeded("getObserverCount")
            return observerMap.size()
        }

    /**
     * Moves [LifecycleObserver]s "up" towards [State.RESUMED].
     *
     * Dispatches [Event]s that activate components (e.g., [Event.ON_CREATE], [Event.ON_START],
     * [Event.ON_RESUME]). Iterates from oldest to newest [LifecycleObserver]s so parents initialize
     * before children.
     */
    private fun forwardPass(lifecycleOwner: LifecycleOwner) {
        observerMap.forEachWithAdditions { (key, observer) ->
            while (
                observer.state < internalState && !newEventOccurred && observerMap.contains(key)
            ) {
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
     * Moves [LifecycleObserver]s "down" towards [State.DESTROYED].
     *
     * Dispatches [Event]s that tear down components (e.g., [Event.ON_PAUSE], [Event.ON_STOP],
     * [Event.ON_DESTROY]). Iterates from newest to oldest [LifecycleObserver]s so children tear
     * down before parents.
     */
    private fun backwardPass(lifecycleOwner: LifecycleOwner) {
        observerMap.forEachReversed { (key, observer) ->
            while (
                observer.state > internalState && !newEventOccurred && observerMap.contains(key)
            ) {
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
     * Synchronizes the state of all [LifecycleObserver]s with the registry's current
     * [internalState].
     *
     * Iteratively converges [LifecycleObserver]s' states toward the registry's state. It executes:
     * - **Backward Pass**: brings [LifecycleObserver]s down in reverse addition order (e.g.,
     *   towards [State.DESTROYED]) when the registry's state is lower than the oldest
     *   [LifecycleObserver].
     * - **Forward Pass**: brings [LifecycleObserver]s up in addition order (e.g., towards
     *   [State.RESUMED]) when the registry's state is higher than the newest [LifecycleObserver].
     *
     * If a new [Event] occurs during synchronization (re-entrance), [newEventOccurred] is set,
     * aborting the current pass to restart the convergence with the latest state.
     *
     * Note: Call only from the top of the stack (never inside a re-entrant call) to avoid stack
     * overflow.
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
            if (internalState < observerMap.first().value.state) {
                backwardPass(lifecycleOwner)
            }
            val newestState = observerMap.lastOrNull()?.value?.state
            // If the current state is "higher" than the newest observer, we bring observers up.
            if (!newEventOccurred && newestState != null && internalState > newestState) {
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
            check(isMainThread()) { "Method $methodName must be called on the main thread" }
        }
    }

    /** Wrapper that couples a [LifecycleObserver] with its current [State]. */
    internal class ObserverWithState(private val observer: LifecycleObserver, initialState: State) {
        var state = initialState
        val lifecycleObserver = Lifecycling.lifecycleEventObserver(observer)

        fun dispatchEvent(owner: LifecycleOwner, event: Event) {
            val newState = event.targetState
            state = minOf(state, newState)
            LifecycleTracer.trace(
                name = "LifecycleRegistry#onStateChanged",
                owner = owner,
                observer = observer,
                event = event,
            ) {
                lifecycleObserver.onStateChanged(owner, event)
            }
            state = newState
        }
    }

    public companion object {
        /**
         * Creates a new [LifecycleRegistry] for the given [LifecycleOwner] without main-thread
         * enforcement.
         *
         * Note: [LifecycleRegistry] is not thread-safe. Multiple threads accessing it must
         * synchronize externally. Useful for JVM testing where a main thread dispatcher is absent.
         */
        @JvmStatic
        @VisibleForTesting
        public fun createUnsafe(owner: LifecycleOwner): LifecycleRegistry {
            return LifecycleRegistry(owner, false)
        }
    }
}

/**
 * Asserts that a transition from [current] to [next] state is valid.
 *
 * @throws IllegalStateException if the transition is invalid.
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
