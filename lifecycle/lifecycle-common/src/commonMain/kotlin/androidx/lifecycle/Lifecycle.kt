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

@file:Suppress("FacadeClassJvmName") // Cannot be updated, the Kt name has been released

package androidx.lifecycle

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * Defines an object with a lifecycle state control flow.
 *
 * Commonly implemented by UI container classes (such as Activities and Fragments on Android) or
 * custom components to expose their lifecycle to other components.
 *
 * [Event.ON_CREATE], [Event.ON_START], [Event.ON_RESUME] events are dispatched **after** the
 * [LifecycleOwner]'s related method returns. [Event.ON_PAUSE], [Event.ON_STOP], [Event.ON_DESTROY]
 * events are dispatched **before** the [LifecycleOwner]'s related method is called. This gives you
 * certain guarantees on which state the owner is in.
 *
 * To observe lifecycle events, call [addObserver] passing an object that implements either
 * [DefaultLifecycleObserver] or [LifecycleEventObserver].
 *
 * @see State for the valid lifecycle states.
 * @see Event for the transition events between states.
 */
public abstract class Lifecycle {
    /**
     * Caches the [CoroutineScope] associated with this [Lifecycle].
     *
     * This field is used by [coroutineScope] to retrieve or initialize the coroutine scope in a
     * thread-safe manner. It is for **internal use** by the lifecycle library group.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @set:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public var internalScopeRef: AtomicReference<Any?> = AtomicReference(null)

    /**
     * Adds a [LifecycleObserver] to receive [LifecycleOwner] state changes.
     *
     * Brings the given [observer] up to the current [State] of the [LifecycleOwner]. For example,
     * if the [LifecycleOwner] is in [State.STARTED], the [observer] receives [Event.ON_CREATE] and
     * [Event.ON_START] [Event]s.
     *
     * @param observer The observer to notify.
     */
    @MainThread public abstract fun addObserver(observer: LifecycleObserver)

    /**
     * Removes the given [observer] from the list of registered observers.
     *
     * If called while a state change is being dispatched:
     * - If the given [observer] has not yet received that event, it will not receive it.
     * - If the given [observer] has more than one method that observes the currently dispatched
     *   event, and at least one of them received the event, all of them receive it, and removal
     *   occurs afterward.
     *
     * @param observer The observer to remove.
     */
    @MainThread public abstract fun removeObserver(observer: LifecycleObserver)

    /** The current [State] of the [Lifecycle]. */
    @get:MainThread public abstract val currentState: State

    /** Lazily initialized backing field for [currentStateFlow]. */
    private var _currentStateFlow: MutableStateFlow<State>? = null

    /**
     * Returns a [StateFlow] where the [StateFlow.value] represents the current [State] of this
     * [Lifecycle].
     */
    public open val currentStateFlow: StateFlow<State>
        get() {
            // If currentStateFlow is never accessed, it is not created. Once created, a single
            // observer is kept registered for the lifetime of this Lifecycle.
            if (_currentStateFlow == null) {
                val flow = MutableStateFlow(currentState)
                addObserver { _, event -> flow.value = event.targetState }
                _currentStateFlow = flow
            }
            return _currentStateFlow!!.asStateFlow()
        }

    /**
     * Represents a transition event triggered by a change in the [LifecycleOwner]'s state.
     *
     * Together with [State]s, these events form a directed graph defining the valid lifecycle flow
     * of a component:
     * ```
     *               +-------------+
     *               | INITIALIZED |
     *               +-------------+
     *                      |
     *                      | ON_CREATE
     *                      v
     *                 +---------+
     *                 | CREATED | ----------+
     *                 +---------+           |
     *                  |       ^            |
     *         ON_START |       | ON_STOP    |
     *                  v       |            | ON_DESTROY
     *                 +---------+           |
     *                 | STARTED |           |
     *                 +---------+           |
     *                  |       ^            |
     *        ON_RESUME |       | ON_PAUSE   |
     *                  v       |            v
     *                 +---------+     +-----------+
     *                 | RESUMED |     | DESTROYED |
     *                 +---------+     +-----------+
     * ```
     *
     * @see State for the states resulting from these transition events.
     */
    public enum class Event {
        /**
         * Dispatched when the [LifecycleOwner] enters the created state.
         *
         * Called after the [LifecycleOwner]'s `onCreate` returns. Transitions the lifecycle to
         * [State.CREATED].
         */
        ON_CREATE,

        /**
         * Dispatched when the [LifecycleOwner] enters the started state.
         *
         * Called after the [LifecycleOwner]'s `onStart` returns. Transitions the lifecycle to
         * [State.STARTED].
         */
        ON_START,

        /**
         * Dispatched when the [LifecycleOwner] enters the resumed state.
         *
         * Called after the [LifecycleOwner]'s `onResume` returns. Transitions the lifecycle to
         * [State.RESUMED].
         */
        ON_RESUME,

        /**
         * Dispatched when the [LifecycleOwner] is about to leave the resumed state.
         *
         * Called before the [LifecycleOwner]'s `onPause` is called. Transitions the lifecycle to
         * [State.STARTED].
         */
        ON_PAUSE,

        /**
         * Dispatched when the [LifecycleOwner] is about to leave the started state.
         *
         * Called before the [LifecycleOwner]'s `onStop` is called. Transitions the lifecycle to
         * [State.CREATED].
         */
        ON_STOP,

        /**
         * Dispatched when the [LifecycleOwner] is about to be destroyed.
         *
         * Called before the [LifecycleOwner]'s `onDestroy` is called. Transitions the lifecycle to
         * [State.DESTROYED].
         */
        ON_DESTROY,

        /** A wildcard event constant that matches all [Event]s. */
        ON_ANY;

        /**
         * Returns the new [State] of a [Lifecycle] that just reported this [Event].
         *
         * | Reported [Event] | Resulting [State]                 |
         * |------------------|-----------------------------------|
         * | [ON_CREATE]      | [State.CREATED]                   |
         * | [ON_STOP]        | [State.CREATED]                   |
         * | [ON_START]       | [State.STARTED]                   |
         * | [ON_PAUSE]       | [State.STARTED]                   |
         * | [ON_RESUME]      | [State.RESUMED]                   |
         * | [ON_DESTROY]     | [State.DESTROYED]                 |
         * | [ON_ANY]         | throws [IllegalArgumentException] |
         */
        public val targetState: State
            get() =
                when (this) {
                    ON_CREATE,
                    ON_STOP -> State.CREATED
                    ON_START,
                    ON_PAUSE -> State.STARTED
                    ON_RESUME -> State.RESUMED
                    ON_DESTROY -> State.DESTROYED
                    ON_ANY -> throw IllegalArgumentException("$this has no target state")
                }

        public companion object {
            /**
             * Returns the [Event] that transitions down from the specified [state] to a lower
             * state, or `null` if no transition exists.
             *
             * | Given [state]       | Returned [Event]   |
             * |---------------------|--------------------|
             * | [State.DESTROYED]   | `null`             |
             * | [State.INITIALIZED] | `null`             |
             * | [State.CREATED]     | [Event.ON_DESTROY] |
             * | [State.STARTED]     | [Event.ON_STOP]    |
             * | [State.RESUMED]     | [Event.ON_PAUSE]   |
             */
            @JvmStatic
            public fun downFrom(state: State): Event? =
                when (state) {
                    State.DESTROYED -> null
                    State.INITIALIZED -> null
                    State.CREATED -> ON_DESTROY
                    State.STARTED -> ON_STOP
                    State.RESUMED -> ON_PAUSE
                }

            /**
             * Returns the [Event] that transitions down into the specified target [state], or
             * `null` if no transition exists.
             *
             * | Target [state]      | Returned [Event]   |
             * |---------------------|--------------------|
             * | [State.DESTROYED]   | [Event.ON_DESTROY] |
             * | [State.INITIALIZED] | `null`             |
             * | [State.CREATED]     | [Event.ON_STOP]    |
             * | [State.STARTED]     | [Event.ON_PAUSE]   |
             * | [State.RESUMED]     | `null`             |
             */
            @JvmStatic
            public fun downTo(state: State): Event? =
                when (state) {
                    State.DESTROYED -> ON_DESTROY
                    State.INITIALIZED -> null
                    State.CREATED -> ON_STOP
                    State.STARTED -> ON_PAUSE
                    State.RESUMED -> null
                }

            /**
             * Returns the [Event] that transitions up from the specified [state] to a higher state,
             * or `null` if no transition exists.
             *
             * | Given [state]       | Returned [Event]  |
             * |---------------------|-------------------|
             * | [State.DESTROYED]   | `null`            |
             * | [State.INITIALIZED] | [Event.ON_CREATE] |
             * | [State.CREATED]     | [Event.ON_START]  |
             * | [State.STARTED]     | [Event.ON_RESUME] |
             * | [State.RESUMED]     | `null`            |
             */
            @JvmStatic
            public fun upFrom(state: State): Event? =
                when (state) {
                    State.DESTROYED -> null
                    State.INITIALIZED -> ON_CREATE
                    State.CREATED -> ON_START
                    State.STARTED -> ON_RESUME
                    State.RESUMED -> null
                }

            /**
             * Returns the [Event] that transitions up into the specified target [state], or `null`
             * if no transition exists.
             *
             * | Target [state]      | Returned [Event]  |
             * |---------------------|-------------------|
             * | [State.DESTROYED]   | `null`            |
             * | [State.INITIALIZED] | `null`            |
             * | [State.CREATED]     | [Event.ON_CREATE] |
             * | [State.STARTED]     | [Event.ON_START]  |
             * | [State.RESUMED]     | [Event.ON_RESUME] |
             */
            @JvmStatic
            public fun upTo(state: State): Event? =
                when (state) {
                    State.DESTROYED -> null
                    State.INITIALIZED -> null
                    State.CREATED -> ON_CREATE
                    State.STARTED -> ON_START
                    State.RESUMED -> ON_RESUME
                }
        }
    }

    /**
     * Represents the current lifecycle state of a [LifecycleOwner].
     *
     * You can visualize these states as nodes in a graph where [Event]s represent the directed
     * edges guiding transitions between them:
     * ```
     *               +-------------+
     *               | INITIALIZED |
     *               +-------------+
     *                      |
     *                      | ON_CREATE
     *                      v
     *                 +---------+
     *                 | CREATED | ----------+
     *                 +---------+           |
     *                  |       ^            |
     *         ON_START |       | ON_STOP    |
     *                  v       |            | ON_DESTROY
     *                 +---------+           |
     *                 | STARTED |           |
     *                 +---------+           |
     *                  |       ^            |
     *        ON_RESUME |       | ON_PAUSE   |
     *                  v       |            v
     *                 +---------+     +-----------+
     *                 | RESUMED |     | DESTROYED |
     *                 +---------+     +-----------+
     * ```
     *
     * Transitions between these states occur strictly sequentially. When moving between
     * non-adjacent states, the registry dispatches all intermediate [Event]s. For example,
     * transitioning from [State.RESUMED] to [State.DESTROYED] will sequentially dispatch
     * [Event.ON_PAUSE], [Event.ON_STOP], and [Event.ON_DESTROY].
     *
     * @see Event for the transition events between these states.
     */
    public enum class State {
        /**
         * Destroyed state for a [LifecycleOwner].
         *
         * **This is the terminal state.** Once reached, this [Lifecycle] will not dispatch any more
         * events, and it cannot transition to any other state.
         *
         * For instance, for an Android Activity, this state is reached right before the activity's
         * `onDestroy` is called.
         */
        DESTROYED,

        /**
         * Initialized state for a [LifecycleOwner].
         *
         * **This is the initial state.** Once the lifecycle transitions out of this state, it
         * cannot transition back to it.
         *
         * For instance, for an Android Activity, this is the state when it is constructed but has
         * not received `onCreate` yet.
         */
        INITIALIZED,

        /**
         * Created state for a [LifecycleOwner].
         *
         * **This is a cyclic state.** The lifecycle can transition back and forth between
         * [CREATED], [STARTED], and [RESUMED] states.
         *
         * For instance, for an Android Activity, this state is reached after the activity's
         * `onCreate` returns, or right before `onStop` is called.
         */
        CREATED,

        /**
         * Started state for a [LifecycleOwner].
         *
         * **This is a cyclic state.** The lifecycle can transition back and forth between
         * [CREATED], [STARTED], and [RESUMED] states.
         *
         * For instance, for an Android Activity, this state is reached after the activity's
         * `onStart` returns, or right before `onPause` is called.
         */
        STARTED,

        /**
         * Resumed state for a [LifecycleOwner].
         *
         * **This is a cyclic state.** The lifecycle can transition back and forth between
         * [CREATED], [STARTED], and [RESUMED] states.
         *
         * For instance, for an Android Activity, this state is reached after the activity's
         * `onResume` returns.
         */
        RESUMED;

        /**
         * Returns true if this state is greater than or equal to the given [state].
         *
         * States are ordered as [DESTROYED] < [INITIALIZED] < [CREATED] < [STARTED] < [RESUMED].
         */
        public fun isAtLeast(state: State): Boolean {
            return compareTo(state) >= 0
        }
    }
}

/** An object reference that may be updated atomically. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public expect class AtomicReference<V>(initialValue: V) {
    /** Returns the current value. */
    public fun get(): V

    /** Atomically sets the value to [newValue] if the current value is equal to [expectedValue]. */
    public fun compareAndSet(expectedValue: V, newValue: V): Boolean
}

/**
 * [CoroutineScope] tied to this [Lifecycle].
 *
 * Canceled when the [Lifecycle] is destroyed. Bound to `Dispatchers.Main.immediate`.
 */
public val Lifecycle.coroutineScope: LifecycleCoroutineScope
    get() {
        while (true) {
            val existing = internalScopeRef.get() as LifecycleCoroutineScopeImpl?
            if (existing != null) {
                return existing
            }
            val newScope =
                LifecycleCoroutineScopeImpl(this, SupervisorJob() + Dispatchers.Main.immediate)
            if (internalScopeRef.compareAndSet(null, newScope)) {
                newScope.register()
                return newScope
            }
        }
    }

/**
 * [CoroutineScope] tied to a [Lifecycle] and `Dispatchers.Main.immediate`.
 *
 * Canceled when the [Lifecycle] is destroyed.
 */
public expect abstract class LifecycleCoroutineScope internal constructor() : CoroutineScope {
    internal abstract val lifecycle: Lifecycle
}

internal class LifecycleCoroutineScopeImpl(
    override val lifecycle: Lifecycle,
    override val coroutineContext: CoroutineContext,
) : LifecycleCoroutineScope(), LifecycleEventObserver {
    init {
        // in case we are initialized on a non-main thread, make the best effort check before
        // we return the scope. This is not sync but if developer is launching on a non-main
        // dispatcher, they cannot be 100% sure anyway.
        if (lifecycle.currentState == Lifecycle.State.DESTROYED) {
            coroutineContext.cancel()
        }
    }

    fun register() {
        launch(Dispatchers.Main.immediate) {
            if (lifecycle.currentState >= Lifecycle.State.INITIALIZED) {
                lifecycle.addObserver(this@LifecycleCoroutineScopeImpl)
            } else {
                coroutineContext.cancel()
            }
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (lifecycle.currentState <= Lifecycle.State.DESTROYED) {
            lifecycle.removeObserver(this)
            coroutineContext.cancel()
        }
    }
}

/** Creates a [Flow] of [Lifecycle.Event]s dispatched by this [Lifecycle]. */
public val Lifecycle.eventFlow: Flow<Lifecycle.Event>
    get() =
        callbackFlow {
                val observer = addObserver { _, event ->
                    trySend(event)

                    // Completes the producer if lifecycle is `DESTROYED`.
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        close()
                    }
                }

                awaitClose { removeObserver(observer) }
            }
            .flowOn(Dispatchers.Main.immediate)

/**
 * Adds a [LifecycleObserver] to this [Lifecycle] using the provided [action].
 *
 * Invokes [action] whenever a [Lifecycle.Event] occurs.
 *
 * @param action The action invoked on each [Lifecycle.Event], providing the [LifecycleOwner] and
 *   the specific [Lifecycle.Event].
 * @return the added [LifecycleObserver] instance (can be used to later remove it).
 */
public inline fun Lifecycle.addObserver(
    crossinline action: LifecycleObserver.(source: LifecycleOwner, event: Lifecycle.Event) -> Unit
): LifecycleObserver {
    val observer =
        object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                action(this, source, event)
            }
        }
    addObserver(observer)
    return observer
}
