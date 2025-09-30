/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.runtime.retain

import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.RetainScope.*
import androidx.compose.runtime.retain.RetainStateProvider.AlwaysKeepExitedValues
import androidx.compose.runtime.retain.RetainStateProvider.NeverKeepExitedValues
import androidx.compose.runtime.retain.RetainStateProvider.NeverKeepExitedValues.isKeepingExitedValues
import androidx.compose.runtime.retain.RetainStateProvider.RetainStateObserver
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Remember the value produced by [calculation] and retain it in the current [RetainScope]. A
 * retained value is one that is persisted in memory to survive transient destruction and recreation
 * of a portion or the entirety of the content in the composition hierarchy. Some examples of when
 * content is transient destroyed occur include:
 * - Navigation destinations that are on the back stack, not currently visible, and not composed
 * - UI components that are collapsed, not rendering, and not composed
 * - On Android, composition hierarchies hosted by an Activity that is being destroyed and recreated
 *   due to a configuration change
 *
 * When a value retained by [retain] leaves the composition hierarchy during one of these retention
 * scenarios, the [LocalRetainScope] will persist it until the content is recreated. If an instance
 * of this function then re-enters the composition hierarchy during the recreation, it will return
 * the retained value instead of invoking [calculation] again.
 *
 * If this function leaves the composition hierarchy when the [LocalRetainScope] is not keeping
 * values that exit the composition, the value will be discarded immediately.
 *
 * The lifecycle of the retained value can be observed by implementing [RetainObserver]. Callbacks
 * from [RememberObserver] are never invoked on objects retained this way. It is invalid to retain
 * an object that is a [RememberObserver] but not a [RetainObserver], and an exception will be
 * thrown.
 *
 * The lifecycle of a retained value is shown in the diagram below. This diagram tracks how a
 * retained value is held through its lifecycle and when it transitions between states.
 *
 * ```
 * ┌──────────────────────┐
 * │                      │
 * │ retain(keys) { ... } │
 * │        ┌────────────┐│
 * └────────┤  value: T  ├┘
 *          └──┬─────────┘
 *             │   ▲
 *         Exit│   │Enter
 *  composition│   │composition
 *    or change│   │
 *         keys│   │                         ┌──────────────────────────┐
 *             │   ├───No retained value─────┤   calculation: () -> T   │
 *             │   │   or different keys     └──────────────────────────┘
 *             │   │                         ┌──────────────────────────┐
 *             │   └───Re-enter composition──┤    Local RetainScope     │
 *             │       with the same keys    └─────────────────┬────────┘
 *             │                                           ▲   │
 *             │                      ┌─Yes────────────────┘   │ value not
 *             │                      │                        │ restored and
 *             │   .──────────────────┴──────────────────.     │ scope stops
 *             └─▶(   RetainScope.isKeepingExitedValues   )    │ keeping exited
 *                 `──────────────────┬──────────────────'     │ values
 *                                    │                        ▼
 *                                    │      ┌──────────────────────────┐
 *                                    └─No──▶│     value is retired     │
 *                                           └──────────────────────────┘
 * ```
 *
 * **Important:** Retained values are held longer than the lifespan of the composable they are
 * associated with. This can cause memory leaks if a retained object is kept beyond its expected
 * lifetime. Be cautious with the types of data that you retain. Never retain an Android Context or
 * an object that references a Context (including View), either directly or indirectly. To mark that
 * a custom class should not be retained (possibly because it will cause a memory leak), you can
 * annotate your class definition with [androidx.compose.runtime.annotation.DoNotRetain].
 *
 * @param calculation A computation to invoke to create a new value, which will be used when a
 *   previous one is not available to return because it was neither remembered nor retained.
 * @return The result of [calculation]
 * @throws IllegalArgumentException if the return result of [calculation] both implements
 *   [RememberObserver] and does not also implement [RetainObserver]
 * @see remember
 */
@Composable
public inline fun <reified T> retain(noinline calculation: () -> T): T {
    return retain(typeHash = classHash<T>(), calculation = calculation)
}

/**
 * Remember the value produced by [calculation] and retain it in the current [RetainScope]. A
 * retained value is one that is persisted in memory to survive transient destruction and recreation
 * of a portion of the entirety of the composition hierarchy. Some examples of when this transient
 * destruction occur include:
 * - Navigation destinations that are on the back stack, not currently visible, and not composed
 * - UI components that are collapsed, not rendering, and not composed
 * - On Android, composition hierarchies hosted by an Activity that is being destroyed and recreated
 *   due to a configuration change
 *
 * When a value retained by [retain] leaves the composition hierarchy during one of these retention
 * scenarios, the [LocalRetainScope] will persist it until the content is recreated. If an instance
 * of this function then re-enters the composition hierarchy during the recreation, it will return
 * the retained value instead of invoking [calculation] again.
 *
 * If this function leaves the composition hierarchy when the [LocalRetainScope] is not keeping
 * values that exit the composition or is invoked with list of [keys] that are not all equal (`==`)
 * to the values they had in the previous composition, the value will be discarded immediately and
 * [calculation] will execute again when a new value is needed.
 *
 * The lifecycle of the retained value can be observed by implementing [RetainObserver]. Callbacks
 * from [RememberObserver] are never invoked on objects retained this way. It is illegal to retain
 * an object that is a [RememberObserver] but not a [RetainObserver].
 *
 * Keys passed to this composable will be kept in-memory while the computed value is retained for
 * comparison against the old keys until the value is retired. Keys are allowed to implement
 * [RememberObserver] arbitrarily, unlike the values returned by [calculation]. If a key implements
 * [RetainObserver], it will **not** receive retention callbacks from this usage.
 *
 * The lifecycle of a retained value is shown in the diagram below. This diagram tracks how a
 * retained value is held through its lifecycle and when it transitions between states.
 *
 * ```text
 * ┌──────────────────────┐
 * │                      │
 * │ retain(keys) { ... } │
 * │        ┌────────────┐│
 * └────────┤  value: T  ├┘
 *          └──┬─────────┘
 *             │   ▲
 *         Exit│   │Enter
 *  composition│   │composition
 *    or change│   │
 *         keys│   │                         ┌──────────────────────────┐
 *             │   ├───No retained value─────┤   calculation: () -> T   │
 *             │   │   or different keys     └──────────────────────────┘
 *             │   │                         ┌──────────────────────────┐
 *             │   └───Re-enter composition──┤    Local RetainScope     │
 *             │       with the same keys    └─────────────────┬────────┘
 *             │                                           ▲   │
 *             │                      ┌─Yes────────────────┘   │ value not
 *             │                      │                        │ restored and
 *             │   .──────────────────┴──────────────────.     │ scope stops
 *             └─▶(   RetainScope.isKeepingExitedValues   )    │ keeping exited
 *                 `──────────────────┬──────────────────'     │ values
 *                                    │                        ▼
 *                                    │      ┌──────────────────────────┐
 *                                    └─No──▶│     value is retired     │
 *                                           └──────────────────────────┘
 * ```
 *
 * **Important:** Retained values are held longer than the lifespan of the composable they are
 * associated with. This can cause memory leaks if a retained object is kept beyond its expected
 * lifetime. Be cautious with the types of data that you retain. Never retain an Android Context or
 * an object that references a Context (including View), either directly or indirectly. To mark that
 * a custom class should not be retained (possibly because it will cause a memory leak), you can
 * annotate your class definition with [androidx.compose.runtime.annotation.DoNotRetain].
 *
 * @param keys An arbitrary list of keys that, if changed, will cause an old retained value to be
 *   discarded and for [calculation] to return a new value, regardless of whether the old value was
 *   being retained in the [RetainScope] or not.
 * @param calculation A producer that will be invoked to initialize the retained value if a value
 *   from the previous composition isn't available.
 * @return The result of [calculation]
 * @throws IllegalArgumentException if the return result of [calculation] both implements
 *   [RememberObserver] and does not also implement [RetainObserver]
 * @see remember
 */
@Composable
public inline fun <reified T> retain(vararg keys: Any?, noinline calculation: () -> T): T {
    return retain(typeHash = classHash<T>(), keys = keys, calculation = calculation)
}

@PublishedApi
@Composable
internal fun <T> retain(typeHash: Int, calculation: () -> T): T {
    return retainImpl(
        key =
            RetainKeys(
                keys = null,
                positionalKey = currentCompositeKeyHashCode,
                typeHash = typeHash,
            ),
        calculation = calculation,
    )
}

@PublishedApi
@Composable
internal fun <T> retain(typeHash: Int, vararg keys: Any?, calculation: () -> T): T {
    return retainImpl(
        key =
            RetainKeys(
                keys = keys,
                positionalKey = currentCompositeKeyHashCode,
                typeHash = typeHash,
            ),
        calculation = calculation,
    )
}

@Composable
private fun <T> retainImpl(key: RetainKeys, calculation: () -> T): T {
    val retainScope = LocalRetainScope.current
    val holder =
        remember(key) {
            val retainedValue = retainScope.getExitedValueOrDefault(key, RetainScopeMissingValue)
            if (retainedValue !== RetainScopeMissingValue) {
                RetainedValueHolder(
                    key = key,
                    value = @Suppress("UNCHECKED_CAST") (retainedValue as T),
                    owner = retainScope,
                    isNewlyRetained = false,
                )
            } else {
                RetainedValueHolder(
                    key = key,
                    value = calculation(),
                    owner = retainScope,
                    isNewlyRetained = true,
                )
            }
        }

    if (holder.owner !== retainScope) {
        SideEffect { holder.readoptUnder(retainScope) }
    }
    return holder.value
}

private val RetainScopeMissingValue = Any()

/**
 * The [RetainScope] in which [retain] values will be tracked in. Since a RetainScope controls
 * retention scenarios and signals when to start and end the retention of objects removed from
 * composition, a composition hierarchy may have several RetainScopes to introduce retention periods
 * to specific pieces of content.
 *
 * The default implementation is a [ForgetfulRetainScope] that causes [retain] to behave the same as
 * [remember]. On Android, a lifecycle-aware scope is installed at the root of the composition that
 * retains values across configuration changes.
 *
 * If this CompositionLocal is updated, all values previously returned by [retain] will be adopted
 * to the new scope and will follow the new scope's retention lifecycle.
 *
 * RetainScopes should be installed so that their tracked transiently removed content is always
 * removed from composition in the same frame (and by extension, all retained values leave
 * composition in the same frame). If the RetainScope starts keeping exited values and its tracked
 * content is removed in an arbitrary order across several recompositions, it may cause retained
 * values to be restored incorrectly if the retained values from different regions in the
 * composition have the same [currentCompositeKeyHashCode].
 */
public val LocalRetainScope: ProvidableCompositionLocal<RetainScope> = staticCompositionLocalOf {
    ForgetfulRetainScope
}

/**
 * A RetainScope acts as a storage area for objects being retained. An instance of a RetainScope
 * also defines a specific retention policy to describe when removed state should be retained and
 * when it should be forgotten.
 *
 * The general pattern for retention is as follows:
 * 1. The RetainScope receives a notification from that transient content removal is about to begin.
 *    The source of this notification varies depending on what retention scenario is being captured,
 *    but could, for example, be a signal that an Android Activity is being recreated, or that
 *    content is about to be navigated away from/collapsed with the potential of being returned to.
 *    At this time, the scope's owner should call [requestKeepExitedValues].
 * 2. Transient content removal begins. The content is recomposed, removed from the hierarchy, and
 *    remembered values are forgotten. Values remembered by [retain] leave the composition but are
 *    not yet released. Every value returned by [retain] will be passed as an argument to
 *    [saveExitingValue] so that it can later be returned by [getExitedValueOrDefault].
 * 3. An arbitrary amount of time passes, and the removed content is restored in the composition
 *    hierarchy at its previous location. When a [retain] call is invoked during the restoration, it
 *    calls [getExitedValueOrDefault]. If all the input keys match a retained value, the previous
 *    result is returned and the retained value is removed from the pool of restorable objects that
 *    exited the previous composition. This step may be skipped if it becomes impossible to return
 *    to the transiently removed content while this scope is keeping exited values.
 * 4. The content finishes composing after being restored, and the entire frame completes. The owner
 *    of this scope should call [unRequestKeepExitedValues]. When retention stops being requested,
 *    it immediately ends. Any values that are retained and not currently used in a composition (and
 *    therefore not restored by [getExitedValueOrDefault]) are then immediately discarded.
 *
 * A given `RetainScope` should only be used by a single
 * [Recomposer][androidx.compose.runtime.Recomposer] at a time. It can move between recomposers (for
 * example, when the Window is recreated), but should never be used by two Recomposers
 * simultaneously. It is valid for a RetainScope to be used in multiple compositions at the same
 * time, or in the same composition multiple times.
 *
 * @see retain
 * @see LocalRetainScope
 * @see ControlledRetainScope
 * @see ForgetfulRetainScope
 */
public abstract class RetainScope : RetainStateProvider {

    protected var keepExitedValuesRequests: Int = 0
        private set

    final override val isKeepingExitedValues: Boolean
        get() = keepExitedValuesRequests > 0

    private val observers = MutableScatterSet<RetainStateObserver>(0)

    /**
     * If this scope is currently keeping exited values and has a value previously created with the
     * given [key], its original record is returned and removed from the list of exited kept objects
     * that this scope is tracking.
     *
     * @param key The keys to resolve a retained value that has left composition
     * @param defaultIfAbsent A value to be returned if there are no retained values that have
     *   exited composition and are being held by this RetainScope for the given [key].
     * @return A retained value for [key] if there is one and it hasn't already re-entered
     *   composition, otherwise [defaultIfAbsent].
     */
    public abstract fun getExitedValueOrDefault(key: Any, defaultIfAbsent: Any?): Any?

    /**
     * Invoked when a retained value is exiting composition while this scope is keeping exited
     * values. It is up to the implementation of this method to decide whether and how to store
     * these values so that they can later be retrieved by [getExitedValueOrDefault].
     *
     * The given [key] are not guaranteed to be unique. To handle duplicate keys, implementors
     * should return retained values with the same keys from [getExitedValueOrDefault] in the
     * opposite order they are received by [saveExitingValue].
     *
     * If the implementation of this scope does not accept this value into its kept exited object
     * list, it MUST call [RetainObserver.onRetired] if [value] implements [RetainObserver].
     */
    protected abstract fun saveExitingValue(key: Any, value: Any?)

    /**
     * Called to increment the number of requests to keep exited values. When there are a positive
     * number of requests, this scope begins keeping exited values and continues until all requests
     * are cleared.
     *
     * This method is not thread safe and should only be called on the applier thread.
     */
    protected fun requestKeepExitedValues() {
        if (keepExitedValuesRequests++ == 0) {
            onStartKeepingExitedValues()
            observers.forEach { it.onStartKeepingExitedValues() }
        }
    }

    /**
     * Clears a previous call to [requestKeepExitedValues]. If all requests to keep exited values
     * have been cleared, this scope will stop keeping exited values.
     *
     * This method is not thread safe and should only be called on the applier thread.
     *
     * @throws IllegalStateException if [unRequestKeepExitedValues] is called more times than
     *   [requestKeepExitedValues] has been called.
     */
    protected fun unRequestKeepExitedValues() {
        checkPrecondition(isKeepingExitedValues) {
            "Unexpected call to unRequestKeepExitedValues() without a " +
                "corresponding requestKeepExitedValues()"
        }
        if (--keepExitedValuesRequests == 0) {
            onStopKeepingExitedValues()
            observers.forEach { it.onStopKeepingExitedValues() }
        }
    }

    /**
     * Called when this scope first starts to keep exited values (i.e. when [isKeepingExitedValues]
     * transitions from false to true). When this is called, implementors should prepare to begin to
     * store values they receive from [saveExitingValue].
     */
    protected abstract fun onStartKeepingExitedValues()

    /**
     * Called when this scope stops keeping exited values (i.e. when [isKeepingExitedValues]
     * transitions from true to false). After this is called, all exited values that have been kept
     * and not restored via [getExitedValueOrDefault] should be retired.
     *
     * Implementors MUST invoke [RetainObserver.onRetired] for all exited and unrestored
     * [RememberObservers][RememberObserver] when this method is invoked.
     */
    protected abstract fun onStopKeepingExitedValues()

    final override fun addRetainStateObserver(observer: RetainStateObserver) {
        observers += observer
    }

    final override fun removeRetainStateObserver(observer: RetainStateObserver) {
        observers -= observer
    }

    internal class RetainedValueHolder<out T>
    internal constructor(
        val key: Any,
        val value: T,
        owner: RetainScope,
        private var isNewlyRetained: Boolean,
    ) : RememberObserver {

        var owner: RetainScope = owner
            private set

        init {
            if (value is RememberObserver && value !is RetainObserver) {
                throw IllegalArgumentException(
                    "Retained a value that implements RememberObserver but not RetainObserver. " +
                        "To receive the correct callbacks, the retained value '$value' must also " +
                        "implement RetainObserver."
                )
            }
        }

        internal fun readoptUnder(newScope: RetainScope) {
            owner = newScope
        }

        override fun onRemembered() {
            if (value is RetainObserver) {
                if (isNewlyRetained) {
                    isNewlyRetained = false
                    value.onRetained()
                }
                value.onEnteredComposition()
            }
        }

        override fun onForgotten() {
            if (owner.isKeepingExitedValues) {
                owner.saveExitingValue(key, value)
            }

            if (value is RetainObserver) {
                value.onExitedComposition()
                if (!owner.isKeepingExitedValues) value.onRetired()
            }
        }

        override fun onAbandoned() {
            if (owner.isKeepingExitedValues) {
                if (value is RetainObserver) value.onRetained()
                owner.saveExitingValue(key, value)
            } else if (value is RetainObserver) {
                value.onUnused()
            }
        }
    }
}

/**
 * [RetainStateProvider] is an owner of the [isKeepingExitedValues] state used by [RetainScope].
 * This interface is extracted to allow retain state to be observed without the presence of the
 * value storage. This is particularly useful as most [RetainScope]s respect a hierarchy where they
 * begin keeping exited values when either their retain condition becomes true or their parent scope
 * begins keeping exited values.
 */
public interface RetainStateProvider {
    /**
     * Returns whether the associated retain scenario is active, and associated scopes should retain
     * objects as they are removed from the composition hierarchy.
     */
    public val isKeepingExitedValues: Boolean

    /**
     * Registers the given [observer] with this [RetainStateProvider] to be notified when the value
     * of [isKeepingExitedValues] changes. The added observer will receive its first notification
     * the next time [isKeepingExitedValues] is updated.
     *
     * This method is not thread safe and should only be invoked on the applier thread.
     *
     * @see removeRetainStateObserver
     */
    public fun addRetainStateObserver(observer: RetainStateObserver)

    /**
     * Removes a previously registered [observer]. It will receive no further updates from this
     * [RetainStateProvider] unless it is registered again in the future. If the observer is not
     * currently registered, this this method does nothing.
     *
     * This method is not thread safe and should only be invoked on the applier thread.
     *
     * @see addRetainStateObserver
     */
    public fun removeRetainStateObserver(observer: RetainStateObserver)

    /**
     * Listener interface to observe changes in the value of
     * [RetainStateProvider.isKeepingExitedValues].
     *
     * @see RetainStateProvider.addRetainStateObserver
     * @see RetainStateProvider.removeRetainStateObserver
     */
    @Suppress("CallbackName")
    public interface RetainStateObserver {
        /**
         * Called to indicate that [RetainStateProvider.isKeepingExitedValues] has become `true`.
         * This callback should only be invoked on the applier thread.
         */
        public fun onStartKeepingExitedValues()

        /**
         * Called to indicate that [RetainStateProvider.isKeepingExitedValues] has become `false`.
         * This callback should only be invoked on the applier thread.
         */
        public fun onStopKeepingExitedValues()
    }

    /**
     * An implementation of [RetainStateProvider] that is not backed by a [RetainScope] and is
     * always set to keep exited values. This object is stateless and can be used to orphan a nested
     * [RetainScope] while maintaining it in a state where the scope keeps all exited values.
     */
    @Stable
    public object AlwaysKeepExitedValues : RetainStateProvider {
        override val isKeepingExitedValues: Boolean
            get() = true

        override fun addRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }

        override fun removeRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }
    }

    /**
     * An implementation of [RetainStateProvider] that is not backed by a [RetainScope] and is never
     * set to keep exited values. This object is stateless and can be used to orphan a nested
     * [RetainScope] and clear any parent-driven state of [isKeepingExitedValues].
     */
    @Stable
    public object NeverKeepExitedValues : RetainStateProvider {
        override val isKeepingExitedValues: Boolean
            get() = false

        override fun addRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }

        override fun removeRetainStateObserver(observer: RetainStateObserver) {
            // Value never changes. Nothing to observe.
        }
    }
}

/**
 * A [ControlledRetainScope] is effectively a "Mutable" [RetainScope]. This scope can be used to
 * define a custom retain scenario and supports nesting within another [RetainScope] via
 * [setParentRetainStateProvider].
 *
 * This class can be used to create your own retention scenario. A retention scenario is a situation
 * in which content is transiently removed from the composition hierarchy and can be restored with
 * the retained values from the previous composition.
 *
 * When using this class to create your own retention scenario, call [startKeepingExitedValues] to
 * make this scope start keeping exited values state before any content is transiently removed. When
 * the transiently removed content is restored, call [stopKeepingExitedValues] **after all content
 * has been restored**. You can use [androidx.compose.runtime.Recomposer.scheduleFrameEndCallback]
 * or [androidx.compose.runtime.Composer.scheduleFrameEndCallback] to ensure that all content has
 * settled in subcompositions and movable content that may not be realized or applied in as part of
 * a composition that is currently ongoing.
 *
 * To create a [ControlledRetainScope] that is managed entirely within the composition hierarchy,
 * you can use [retainControlledRetainScope] to create a [ControlledRetainScope] that is
 * automatically parented to the current [LocalRetainScope].
 */
public class ControlledRetainScope : RetainScope() {
    private val keptExitedValues = SafeMultiValueMap<Any, Any?>()

    private var parentScope: RetainStateProvider = NeverKeepExitedValues
    private val parentObserver =
        object : RetainStateObserver {
            override fun onStartKeepingExitedValues() {
                requestKeepExitedValues()
            }

            override fun onStopKeepingExitedValues() {
                unRequestKeepExitedValues()
            }
        }

    /**
     * Returns the number of calls to [startKeepingExitedValues] - [stopKeepingExitedValues].
     * Effectively, the total number of active requests to this [ControlledRetainScope].
     *
     * Note that this value **ignores any parent state.** It only counts explicit requests from the
     * user to [startKeepingExitedValues]. This scope could still be retaining if this value is `0`
     * if the parent scope is retaining. This is useful if you want to track your contributions to
     * this scope's state, ignoring the parent.
     */
    public val keepExitedValuesRequestsFromSelf: Int
        get() = keepExitedValuesRequests - if (parentScope.isKeepingExitedValues) 1 else 0

    /**
     * Calling this function will automatically mirror the state of [isKeepingExitedValues] to match
     * [parent]'s state. This is an addition to requests made on the [ControlledRetainScope], so
     * keeping exited values is a function of whether the parent is keeping exited values OR this
     * scope has been requested to keep exited values.
     *
     * A [ControlledRetainScope] can only have one parent. If a new parent is provided, it will
     * replace the old one and will match the new parent's [isKeepingExitedValues] state. This may
     * cause this scope to start or stop keeping exited values if this scope has no other active
     * requests from [startKeepingExitedValues].
     */
    public fun setParentRetainStateProvider(parent: RetainStateProvider) {
        val oldParent = parentScope
        parentScope = parent

        parent.addRetainStateObserver(parentObserver)
        oldParent.removeRetainStateObserver(parentObserver)

        if (parent.isKeepingExitedValues) startKeepingExitedValues()
        if (oldParent.isKeepingExitedValues) stopKeepingExitedValues()
    }

    /**
     * Indicates that this scope should keep retained values that exit the composition. If this
     * scope is already in this mode, the scope will not change states. The number of times this
     * function is called is tracked and must be matched by the same number of calls to
     * [stopKeepingExitedValues] before the kept values will be retired.
     */
    public fun startKeepingExitedValues(): Unit = requestKeepExitedValues()

    /**
     * Stops keeping values that have exited the composition. This function cancels a request that
     * previously began by calling [startKeepingExitedValues]. If [startKeepingExitedValues] has
     * been called more than [stopKeepingExitedValues], the scope will continue to keep retained
     * values that have exited the composition until [stopKeepingExitedValues] has been called the
     * same number of times as [startKeepingExitedValues].
     *
     * @throws IllegalStateException if [stopKeepingExitedValues] is called more times than
     *   [startKeepingExitedValues]
     */
    public fun stopKeepingExitedValues(): Unit = unRequestKeepExitedValues()

    override fun onStartKeepingExitedValues() {
        checkPrecondition(keptExitedValues.isEmpty()) {
            "Attempted to start keeping exited values with pending exited values"
        }
    }

    override fun onStopKeepingExitedValues() {
        keptExitedValues.forEachValue { value -> if (value is RetainObserver) value.onRetired() }
        keptExitedValues.clear()
    }

    @Suppress("UNCHECKED_CAST")
    override fun getExitedValueOrDefault(key: Any, defaultIfAbsent: Any?): Any? {
        return keptExitedValues.removeLast(key, defaultIfAbsent)
    }

    override fun saveExitingValue(key: Any, value: Any?) {
        keptExitedValues.add(key, value)
    }
}

/**
 * The ForgetfulRetainScope is an implementation of [RetainScope] that is incapable of keeping any
 * exited values. When installed as the [LocalRetainScope], all invocations of [retain] will behave
 * like a standard [remember]. [RetainObserver] callbacks are still dispatched instead of
 * [RememberObserver] callbacks, meaning that this class will always immediately retire a value as
 * soon as it exits composition.
 */
public object ForgetfulRetainScope : RetainScope() {
    override fun onStartKeepingExitedValues() {
        throw UnsupportedOperationException("ForgetfulRetainScope can never keep exited values.")
    }

    override fun onStopKeepingExitedValues() {
        // Do nothing. This implementation never keeps exited values.
    }

    override fun getExitedValueOrDefault(key: Any, defaultIfAbsent: Any?): Any? {
        return defaultIfAbsent
    }

    override fun saveExitingValue(key: Any, value: Any?) {
        throw UnsupportedOperationException("ForgetfulRetainScope can never keep exited values.")
    }
}

/**
 * `RetainedContentHost` is used to install a [RetainScope] around a block of [content]. The
 * installed `RetainScope` is managed such that the scope will start to keep exited values when
 * [active] is false, and stop keeping exited values when [active] becomes true. See
 * [RetainScope.isKeepingExitedValues] for more information on this terminology.
 *
 * `RetainedContentHost` is designed as an out-of-the-box solution for managing content that's
 * controlled effectively by an if/else statement. The [content] provided to this lambda will render
 * when [active] is true, and be removed when [active] is false. If the content is hidden and then
 * shown again in this way, the installed RetainScope will restore all retained values from the last
 * time the content was shown.
 *
 * The managed RetainScope is _also_ retained. If this composable is removed while the parent scope
 * is keeping its exited values, this scope will be persisted so that it can be restored in the
 * future. If this composable is removed while its parent scope is not keeping its exited values,
 * the scope will be discarded and all its held values will be immediately retired.
 *
 * For this reason, when using this as a mechanism to retain values for content that is being shown
 * and hidden, this composable must be hoisted high enough so that it is not removed when the
 * content being retained is hidden.
 *
 * @param active Whether this host should compose its [content]. When this value is true, [content]
 *   will be rendered and the installed [RetainScope] will not keep exited values. When this value
 *   is false, [content] will stop being rendered and the installed [RetainScope] will collect and
 *   keep its exited values for future restoration.
 * @param content The content to render. Inside of this lambda, [LocalRetainScope] is set to the
 *   [RetainScope] managed by this composable.
 * @sample androidx.compose.runtime.retain.samples.retainedContentHostSample
 * @see retainControlledRetainScope
 */
@Composable
public fun RetainedContentHost(active: Boolean, content: @Composable () -> Unit) {
    val retainScope = retainControlledRetainScope()
    if (active) {
        CompositionLocalProvider(LocalRetainScope provides retainScope, content)

        // Match the isKeepingExitedValues state to the active parameter. This effect must come
        // AFTER the content to correctly capture values.
        val composer = currentComposer
        DisposableEffect(retainScope) {
            // Stop keeping exited values when we become active. Use the request count to only
            // look at our state and to ignore any parent-influenced requests.
            val cancellationHandle =
                if (retainScope.keepExitedValuesRequestsFromSelf > 0) {
                    composer.scheduleFrameEndCallback { retainScope.stopKeepingExitedValues() }
                } else {
                    null
                }

            onDispose {
                // Start keeping exited values when we deactivate
                cancellationHandle?.cancel()
                retainScope.startKeepingExitedValues()
            }
        }
    }
}

/**
 * Retains a [ControlledRetainScope] that is nested under the current [LocalRetainScope] and has no
 * other defined retention scenarios.
 *
 * A [ControlledRetainScope] created in this way will mirror the retention behavior of
 * [LocalRetainScope]. When the parent scope begins retaining its values, the returned scope will
 * receive a request to start retaining values as well. When the parent scope stops retaining
 * values, that request is cleared.
 *
 * This API is available as a building block for other retain scopes defined in composition. To
 * define your own retention scenario, call [ControlledRetainScope.startKeepingExitedValues] and
 * [ControlledRetainScope.stopKeepingExitedValues] on the returned scope as appropriate. You must
 * also install this scope in the composition hierarchy by providing it as the value of
 * [LocalRetainScope].
 *
 * When this value stops being retained, it will automatically stop keeping exited values,
 * regardless of how many times [ControlledRetainScope.startKeepingExitedValues] was called.
 *
 * @return A [ControlledRetainScope] nested under the [LocalRetainScope], ready to be installed in
 *   the composition hierarchy and be used to define a retention scenario.
 * @sample androidx.compose.runtime.retain.samples.retainControlledRetainScopeSample
 * @see RetainedContentHost
 */
@Composable
public fun retainControlledRetainScope(): ControlledRetainScope {
    val retainScope = retain { RetainControlledRetainScopeWrapper() }.retainScope

    val parentScope = LocalRetainScope.current
    DisposableEffect(parentScope) {
        retainScope.setParentRetainStateProvider(parentScope)
        onDispose {
            // Keep the parent's state until we get a new scope. This lets us continue
            // retaining when the composition hierarchy is destroyed and this parent is removed.
            retainScope.setParentRetainStateProvider(
                if (parentScope.isKeepingExitedValues) {
                    AlwaysKeepExitedValues
                } else {
                    NeverKeepExitedValues
                }
            )
        }
    }

    return retainScope
}

private class RetainControlledRetainScopeWrapper : RetainObserver {
    val retainScope = ControlledRetainScope()

    override fun onRetained() {}

    override fun onEnteredComposition() {}

    override fun onExitedComposition() {}

    override fun onRetired() {
        // The retainScope has stopped being retained. Dispose it.
        retainScope.setParentRetainStateProvider(NeverKeepExitedValues)
        while (retainScope.isKeepingExitedValues) retainScope.stopKeepingExitedValues()
    }

    override fun onUnused() {
        // Need to clean up if the scope is abandoned, in case our parent caused us
        // to initialize with kept values.
        onRetired()
    }
}

/**
 * Returns a [retain] instance of a new [RetainScopeHolder]. A RetainScopeHolder is a container of
 * [RetainScope]s that allows a parent composable to have children with different retention
 * lifecycles. See [RetainScopeHolder] for more information on how to use this class, including a
 * sample.
 *
 * The returned provider will be parented to the [LocalRetainScope] at this point in the composition
 * hierarchy. If the [LocalRetainScope] is changed, the returned provider will be re-parented to the
 * new [LocalRetainScope]. When this invocation leaves composition, it will continue retaining if
 * its parent scope was retaining. When this [RetainScopeHolder] is retired, its child scopes will
 * also be retired and the scope will be [disposed][RetainScopeHolder.dispose].
 *
 * This method is intended to be used for managing retain state in composables that swap in and out
 * children arbitrarily.
 */
@Composable
public fun retainRetainScopeHolder(): RetainScopeHolder {
    val provider = retain { RetainScopeHolderWrapper() }.retainScopeHolder
    val parentScope = LocalRetainScope.current
    DisposableEffect(parentScope) {
        provider.setParentRetainStateProvider(parentScope)
        onDispose {
            provider.setParentRetainStateProvider(
                parent =
                    if (parentScope.isKeepingExitedValues) {
                        AlwaysKeepExitedValues
                    } else {
                        NeverKeepExitedValues
                    }
            )
        }
    }
    return provider
}

private class RetainScopeHolderWrapper : RetainObserver {
    val retainScopeHolder = RetainScopeHolder()

    override fun onRetained() {}

    override fun onEnteredComposition() {}

    override fun onExitedComposition() {}

    override fun onRetired() {
        retainScopeHolder.dispose()
    }

    override fun onUnused() {
        retainScopeHolder.dispose()
    }
}

/**
 * A [RetainScopeHolder] creates and manages [RetainScope] instances for collections of items. This
 * is desirable for components that swap in and out children where each child should be able to
 * retain state when it becomes removed from the composition hierarchy.
 *
 * To use this class, call [getOrCreateRetainScopeForChild] to instantiate the [RetainScope] that
 * should be installed for a given child content block. For automatic installation and content
 * tracking, wrap your content in [RetainScopeProvider].
 *
 * You can also install the managed retain scopes manually by obtaining a RetainScope with
 * [getOrCreateRetainScopeForChild] and setting it as the [LocalRetainScope] for your children's
 * content. When a child is being removed, call [startKeepingExitedValues] to begin the transient
 * destruction phase of your retention scenario. After the child has been added back to the
 * composition, invoke [stopKeepingExitedValues] to finalize the restoration of retained values.
 *
 * When a [RetainScopeHolder] is no longer used, you must call [dispose] before the provider is
 * garbage collected. This ensures that all retained values are correctly retired. Failure to do so
 * may result in leaked memory from undispatched [RetainObserver.onRetired] callbacks. Instances
 * created by [retainRetainScopeHolder] are automatically disposed when the provider stops being
 * retained.
 *
 * @sample androidx.compose.runtime.retain.samples.retainScopeHolderSample
 */
public class RetainScopeHolder() {
    private var isDisposed = false
    private val childScopes = MutableScatterMap<Any?, ControlledRetainScope>()

    private var parentScope: RetainStateProvider = NeverKeepExitedValues
    private var isParentKeepingExitedValues = false
    private val parentObserver =
        object : RetainStateObserver {
            override fun onStartKeepingExitedValues() {
                setKeepExitedValues(true)
            }

            override fun onStopKeepingExitedValues() {
                setKeepExitedValues(false)
            }
        }

    /**
     * Starts keeping exited values for a child with the given [key]. If a retain scope has not been
     * created for this key (because [getOrCreateRetainScopeForChild] was not called for the key or
     * it has been cleared with [clearChild] or [clearChildren]), then this function does nothing.
     * If the retain scope for the given key is already keeping exited values, the scope will not
     * change states. The number of times this function is called is tracked and must be matched by
     * the same number of calls to [stopKeepingExitedValues] for the given key before its kept
     * values will be retired.
     *
     * This function must be called **before** any content for the associated child is removed from
     * the composition hierarchy.
     *
     * @param key The key of the child to begin retention for
     */
    public fun startKeepingExitedValues(key: Any?) {
        val scope = childScopes[key] ?: return
        scope.startKeepingExitedValues()
    }

    /**
     * Stops keeping exited values for a child with the given [key] as previously started by
     * [startKeepingExitedValues]. If the underlying scope is not retaining because
     * [startKeepingExitedValues] has not been called, this function will throw an exception. If no
     * such retain scope exists because it was cleared with [clearChild] or never created with
     * [getOrCreateRetainScopeForChild], this function will do nothing.
     *
     * If [startKeepingExitedValues] has been called more than [stopKeepingExitedValues], the scope
     * will continue to keep retained values that have exited the composition until
     * [stopKeepingExitedValues] has been called the same number of times as
     * [startKeepingExitedValues].
     *
     * This function must be called **after** the completion of the frame in which the child content
     * is being restored to allow the restored child to re-consume all of its retained values. You
     * can use [androidx.compose.runtime.Recomposer.scheduleFrameEndCallback] or
     * [androidx.compose.runtime.Composer.scheduleFrameEndCallback] to insert a sufficient delay.
     *
     * @param key The key of the child to end retention for
     * @throws IllegalStateException if [startKeepingExitedValues] is called more times than
     *   [stopKeepingExitedValues] has been called for the given key
     */
    public fun stopKeepingExitedValues(key: Any?) {
        val scope = childScopes[key] ?: return
        checkPrecondition(scope.keepExitedValuesRequestsFromRetainScopeHolder >= 1) {
            "Unexpected call to unRequestKeepExitedValues() without a " +
                "corresponding requestKeepExitedValues() for key $key"
        }
        scope.stopKeepingExitedValues()
    }

    /**
     * Gets the total number of active requests from [startKeepingExitedValues] for the given [key].
     * Effectively, this is the number of calls to [startKeepingExitedValues] minus the number of
     * calls to [stopKeepingExitedValues] for the given [key].
     *
     * This counter resets if [clearScope] is called for the given [key]. If the scope has not been
     * created for [key] by [getOrCreateRetainScopeForChild], this function will return `0`.
     *
     * @param key the key of the child to look up
     * @return The number of active requests against the given child to keep exited values
     * @see ControlledRetainScope.keepExitedValuesRequestsFromSelf
     */
    public fun keepExitedValuesRequestsFor(key: Any?): Int {
        val scope = childScopes[key] ?: return 0
        return scope.keepExitedValuesRequestsFromRetainScopeHolder
    }

    // We manage the parent ourselves without wiring it up, because it is more efficient than
    // attaching a listener. Because of this, we need to manually subtract out the parent count.
    private val ControlledRetainScope.keepExitedValuesRequestsFromRetainScopeHolder: Int
        get() = keepExitedValuesRequestsFromSelf - if (isParentKeepingExitedValues) 1 else 0

    /**
     * Installs child [content] that should be retained under the given [key].
     * [startKeepingExitedValues] and [stopKeepingExitedValues] and automatically called based on
     * the presence of this composable for the [key].
     *
     * When removed, this composable begins keeping exited values from the [content] lambda under
     * the given [key]. When added back to the composition hierarchy, the scope will stop keeping
     * retained values once the composition completes. The keys used with this method should only be
     * used once per [RetainScopeHolder]in a composition.
     *
     * This composable only attempts to manage the retention lifecycle for the [content] and [key]
     * pair. It will retain removed content indefinitely until [clearChild] or [clearChildren] is
     * invoked.
     *
     * @param key The child key associated with the given [content]. This key is used to identify
     *   the retention pool for objects [retained][retain] by the content composable.
     * @param content The composable content to compose with the [RetainScope] of the given [key]
     * @throws IllegalStateException if [dispose] has been called
     */
    @Composable
    public fun RetainScopeProvider(key: Any?, content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalRetainScope provides getOrCreateRetainScopeForChild(key)) {
            content()
            PresenceIndicator(key)
        }
    }

    /**
     * Indicates the presence of [key] in the composition hierarchy. When this composable is added,
     * [stopKeepingExitedValues] is called for the [key]. When this composable is removed,
     * [startKeepingExitedValues] will be called at the completion of the frame.
     *
     * **This composable must be placed such that it appears AFTER the composable content content
     * for [key] in a preorder traversal of the composition hierarchy.** Otherwise, the underlying
     * requests that start and stop keeping exited values may be scheduled in an incorrect order,
     * causing lost state. For example,
     * ```kotlin
     * // Correct ordering.
     * if (showA) {
     *     CompositionLocalProvider(
     *         LocalRetainScope provides getOrCreateRetainScopeForChild("A")
     *     ) {
     *         ContentA()
     *     }
     *     PresenceIndicator("A")
     * }
     *
     * // Incorrect ordering.
     * if (showB) {
     *     PresenceIndicator("B")
     *
     *     CompositionLocalProvider(
     *         LocalRetainScope provides getOrCreateRetainScopeForChild("B")
     *     ) {
     *         ContentB()
     *     }
     * }
     * ```
     */
    @Composable
    private fun PresenceIndicator(key: Any?) {
        val composer = currentComposer
        DisposableEffect(key) {
            // This key is entering the composition. End retention when the frame completes. Use
            // the request count to not attempt `stopKeepingExitedValues` when we initially enter
            // the composition.
            val endRetainHandle =
                if (keepExitedValuesRequestsFor(key) > 0) {
                    composer.scheduleFrameEndCallback { stopKeepingExitedValues(key) }
                } else {
                    null
                }
            onDispose {
                // This key is exiting the composition. Begin retaining now.
                endRetainHandle?.cancel()
                startKeepingExitedValues(key)
            }
        }
    }

    /**
     * Creates or returns a previously created [RetainScope] instance for the given [key]. The
     * returned [RetainScope] will be managed by this provider. It will begin retaining if the
     * parent retain scope starts retaining or if [startKeepingExitedValues] is called with the same
     * [key], and it will stop retaining with the parent retain scope ends retaining and there is no
     * [startKeepingExitedValues] call without a corresponding [stopKeepingExitedValues] call for
     * the specified [key].
     *
     * The first time this function is called for a given [key], a new [RetainScope] is created for
     * the [key]. When this function is called for the same [key], it will return the same
     * [RetainScope] it originally returned. If a given [key]'s scope is [cleared][clearChild], then
     * a new one will be created for it the next time it is requested via this function.
     *
     * This function must be called before [startKeepingExitedValues] or [stopKeepingExitedValues]
     * is called for those two methods to have any effect on the retention state for the given
     * [key].
     *
     * @param key The [key] to return an existing [RetainScope] instance for, if one exists, or to
     *   create a new instance for
     * @return A [RetainScope] instance suitable to be installed as the [LocalRetainScope] for the
     *   child content with the specified [key]
     * @throws IllegalStateException if [dispose] has been called
     */
    public fun getOrCreateRetainScopeForChild(key: Any?): RetainScope {
        checkPrecondition(!isDisposed) {
            "Cannot get a RetainScope after a FanOutRetainScopeProvider has been disposed."
        }

        return childScopes.getOrPut(key) {
            ControlledRetainScope().apply {
                if (isParentKeepingExitedValues) startKeepingExitedValues()
            }
        }
    }

    /**
     * When a [RetainStateProvider] is set as the parent of a [RetainScopeHolder], the
     * [RetainScopeHolder] will mirror the retention state of the parent. If the parent stops
     * retaining, all children that have started retaining via [startKeepingExitedValues] will
     * continue being retained after the parent stops retaining.
     *
     * If this function is called twice, the new parent will replace the old parent. The new
     * parent's state is immediately applied to the child scopes.
     *
     * To clear a parent, call this function and pass in either
     * [RetainStateProvider.AlwaysKeepExitedValues] or [RetainStateProvider.NeverKeepExitedValues]
     * depending on whether you want this scope to keep exited values in the absence of a parent.
     */
    public fun setParentRetainStateProvider(parent: RetainStateProvider) {
        val oldParent = parentScope
        parentScope = parent

        parent.addRetainStateObserver(parentObserver)
        oldParent.removeRetainStateObserver(parentObserver)

        setKeepExitedValues(parent.isKeepingExitedValues)
    }

    /**
     * Removes the [RetainScope] for the child with the given [key] from this [RetainScopeHolder].
     * If the key doesn't have an associated [RetainScope] yet (either because it hasn't been
     * created or has already been cleared), this function does nothing.
     *
     * If the scope being cleared is currently keeping exited values, it will stop as a result of
     * this call. If a child with the given [key] is currently in the composition hierarchy, its
     * retained values will not be persisted the next time the child content is destroyed. Orphaned
     * RetainScopes will never begin keeping exited values, and the content will need to be
     * recreated with a new RetainScope before exited values will be kept again.
     *
     * If [getOrCreateRetainScopeForChild] is called again for the given [key], a new [RetainScope]
     * will be created and returned.
     *
     * @param key The key of the child content whose [RetainScope] should be discarded
     */
    public fun clearChild(key: Any?) {
        childScopes.remove(key)?.let { clearScope(it) }
    }

    /**
     * Bulk removes all child scopes for which the [predicate] returns true. This function follows
     * the same clearing rules as [clearChild].
     *
     * @param predicate The predicate to evaluate on all child keys in the [RetainScopeHolder]. If
     *   the predicate returns `true` for a given key, it will be cleared. If the predicate returns
     *   `false` it will remain in the collection.
     * @see clearChild
     */
    public fun clearChildren(predicate: (key: Any?) -> Boolean) {
        childScopes.removeIf { key, scope -> predicate(key).also { if (it) clearScope(scope) } }
    }

    private fun clearScope(scope: ControlledRetainScope) {
        while (scope.keepExitedValuesRequestsFromSelf > 0) scope.stopKeepingExitedValues()
    }

    /**
     * Removes all child [RetainScope]s from this [RetainScopeHolder] and marks it as ineligible for
     * future use. This is required to invoke when the scope is no longer used to retire any
     * retained values. Failing to do so may result in memory leaks from undispatched
     * [RetainObserver.onRetired] and [RetainedEffect] callbacks. When this function is called, all
     * values retained in scopes managed by this provider will be immediately retired.
     *
     * If this scope has already been disposed, this function will do nothing.
     */
    public fun dispose() {
        isDisposed = true
        clearChildren { true }
    }

    private fun setKeepExitedValues(shouldRetain: Boolean) {
        if (shouldRetain == isParentKeepingExitedValues) return
        isParentKeepingExitedValues = shouldRetain

        if (shouldRetain) {
            childScopes.forEachValue { scope -> scope.startKeepingExitedValues() }
        } else {
            childScopes.forEachValue { scope -> scope.stopKeepingExitedValues() }
        }
    }
}

/**
 * Represents all identifying parameters passed into [retain]. Implementations of [RetainScope] are
 * given these keys to identify instances of a [retain] invocation.
 *
 * These keys should not be introspected.
 */
@Stable
private class RetainKeys(
    private val keys: Array<out Any?>?,
    val positionalKey: CompositeKeyHashCode,
    val typeHash: Int,
) {

    override fun equals(other: Any?): Boolean {
        return other is RetainKeys &&
            other.positionalKey == this.positionalKey &&
            other.typeHash == this.typeHash &&
            other.keys.contentEquals(this.keys)
    }

    override fun hashCode(): Int {
        var result = keys?.contentHashCode() ?: 0
        result = 31 * result + positionalKey.hashCode()
        result = 31 * result + typeHash.hashCode()
        return result
    }
}
