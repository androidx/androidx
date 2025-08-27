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

package androidx.compose.runtime

import androidx.collection.MutableScatterSet
import androidx.compose.runtime.RetainScope.*
import androidx.compose.runtime.RetainStateProvider.NeverKeepExitedValues
import androidx.compose.runtime.RetainStateProvider.RetainStateObserver
import androidx.compose.runtime.collection.SafeMultiValueMap
import androidx.compose.runtime.internal.classHash

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
 * A given `RetainScope` should only be used by a single [Recomposer] at a time. It can move between
 * recomposers (for example, when the Window is recreated), but should never be used by two
 * Recomposers simultaneously. It is valid for a RetainScope to be used in multiple compositions at
 * the same time, or in the same composition multiple times.
 *
 * @see retain
 * @see LocalRetainScope
 * @see ControlledRetainScope
 * @see ForgetfulRetainScope
 */
public abstract class RetainScope : RetainStateProvider {

    private var keepExitedValuesRequests = 0

    final override val isKeepingExitedValues: Boolean
        get() = keepExitedValuesRequests > 0

    private val observers = MutableScatterSet<RetainStateObserver>(0)

    /**
     * If this scope is currently keeping exited values and has a value previously created with the
     * given [keys], its original record is returned and removed from the list of exited kept
     * objects that this scope is tracking.
     *
     * @param key The keys to resolve a retained value that has left composition
     * @param defaultIfAbsent A value to be returned if there are no retained values that have
     *   exited composition and are being held by this RetainScope for the given [keys].
     * @return A retained value for [keys] if there is one and it hasn't already re-entered
     *   composition, otherwise [defaultIfAbsent].
     */
    public abstract fun getExitedValueOrDefault(key: Any, defaultIfAbsent: Any?): Any?

    /**
     * Invoked when a retained value is exiting composition while this scope is keeping exited
     * values. It is up to the implementation of this method to decide whether and how to store
     * these values so that they can later be retrieved by [getExitedValueOrDefault].
     *
     * The given [keys] are not guaranteed to be unique. To handle duplicate keys, implementors
     * should return retained values with the same keys from [getExitedValueOrDefault] in the
     * opposite order they are received by [saveExitingValue].
     *
     * If the implementation of this scope does not accept this value into its kept exited object
     * list, it MUST call [RetainObserver.onRetired] if [value] implements [RetainObserver].
     */
    protected abstract fun saveExitingValue(key: Any, value: Any?)

    /**
     * Called to increment the number of retain events requested. When there are a positive number
     * of retain requests, this scope begins keeping exited values and continues until all requests
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
     * Clears a previous call to [requestKeepExitedValues]. If all retain requests have been
     * cleared, this scope will stop keeping exited values.
     *
     * This method is not thread safe and should only be called on the applier thread.
     *
     * @throws IllegalStateException if [unRequestKeepExitedValues] is called more times than
     *   [requestKeepExitedValues] has been called.
     */
    protected fun unRequestKeepExitedValues() {
        requirePrecondition(isKeepingExitedValues) {
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
            if (value is RetainObserver) value.onRetired()
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
 * has been restored**. You can use [Recomposer.scheduleFrameEndCallback] or
 * [Composer.scheduleFrameEndCallback] to ensure that all content has settled in subcompositions and
 * movable content that may not be realized or applied in as part of a composition that is currently
 * ongoing.
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
        runtimeCheck(keptExitedValues.isEmpty()) {
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
