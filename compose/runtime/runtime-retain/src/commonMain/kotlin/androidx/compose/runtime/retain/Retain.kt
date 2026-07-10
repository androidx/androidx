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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositeKeyHashCode
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.remember

/**
 * Remember the value produced by [calculation] and retain it in the [LocalRetainedValuesStore]. A
 * retained value is one that is persisted in memory to survive transient destruction and recreation
 * of a portion or the entirety of the content in the composition hierarchy. Some examples of when
 * content is transiently destroyed include:
 * - Navigation destinations that are on the back stack, not currently visible, and not composed
 * - UI components that are collapsed, not rendering, and not composed
 * - On Android, composition hierarchies hosted by an Activity that is being destroyed and recreated
 *   due to a configuration change
 *
 * When the content tracked by a [RetainedValuesStore] is removed with the expectation that it will
 * be recreated in the future, all of its retained values will be persisted until the content is
 * recreated. If an instance of this function then re-enters the composition hierarchy during this
 * recreation, the retained value will be returned instead of invoking [calculation] again.
 *
 * If this function leaves the composition hierarchy when the [LocalRetainedValuesStore] is not
 * retaining values that exit the composition, the value will be discarded immediately.
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
 *         keys│   │                         ┌───────────────────────────┐
 *             │   ├───No retained value─────┤   calculation: () -> T    │
 *             │   │   or different keys     └───────────────────────────┘
 *             │   │                         ┌───────────────────────────┐
 *             │   └───Re-enter composition──┤ Local RetainedValuesStore │
 *             │       with the same keys    └─────────────────┬─────────┘
 *             │                                           ▲   │
 *             │                      ┌─Yes────────────────┘   │ value not
 *             │                      │                        │ restored and
 *             │   .──────────────────┴──────────────────.     │ store stops
 *             └─▶(        isRetainingExitedValues        )    │ retaining exited
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
 * @sample androidx.compose.runtime.retain.samples.retainSample
 * @sample androidx.compose.runtime.retain.samples.rememberAndRetainSample
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
 * Remember the value produced by [calculation] and retain it in the [LocalRetainedValuesStore]. A
 * retained value is one that is persisted in memory to survive transient destruction and recreation
 * of a portion or the entirety of the content in the composition hierarchy. Some examples of when
 * content is transiently destroyed include:
 * - Navigation destinations that are on the back stack, not currently visible, and not composed
 * - UI components that are collapsed, not rendering, and not composed
 * - On Android, composition hierarchies hosted by an Activity that is being destroyed and recreated
 *   due to a configuration change
 *
 * When the content tracked by a [RetainedValuesStore] is removed with the expectation that it will
 * be recreated in the future, all of its retained values will be persisted until the content is
 * recreated. If an instance of this function then re-enters the composition hierarchy during this
 * recreation, the retained value will be returned instead of invoking [calculation] again.
 *
 * If this function leaves the composition hierarchy when the [LocalRetainedValuesStore] is not
 * retaining values that exit the composition or is invoked with list of [keys] that are not all
 * equal (`==`) to the values they had in the previous composition, the value will be discarded
 * immediately and [calculation] will execute again when a new value is needed.
 *
 * The lifecycle of the retained value can be observed by implementing [RetainObserver]. Callbacks
 * from [RememberObserver] are never invoked on objects retained this way. It is invalid to retain
 * an object that is a [RememberObserver] but not a [RetainObserver].
 *
 * Keys passed to this composable will be kept in-memory while the computed value is retained for
 * comparison against the old keys until the value is [retired][RetainObserver.onRetired]. Keys are
 * allowed to implement [RememberObserver] arbitrarily, unlike the values returned by [calculation].
 * If a key implements [RetainObserver], it will **not** receive retention callbacks from this
 * usage.
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
 *         keys│   │                         ┌───────────────────────────┐
 *             │   ├───No retained value─────┤   calculation: () -> T    │
 *             │   │   or different keys     └───────────────────────────┘
 *             │   │                         ┌───────────────────────────┐
 *             │   └───Re-enter composition──┤ Local RetainedValuesStore │
 *             │       with the same keys    └─────────────────┬─────────┘
 *             │                                           ▲   │
 *             │                      ┌─Yes────────────────┘   │ value not
 *             │                      │                        │ restored and
 *             │   .──────────────────┴──────────────────.     │ store stops
 *             └─▶(        isRetainingExitedValues        )    │ retaining exited
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
 * Because keys are held for the same duration as retained values, all input keys must follow the
 * same lifespan requirements to prevent a memory leak. Do not use a key that references objects
 * like Context or View. Types annotated with [androidx.compose.runtime.annotation.DoNotRetain] are
 * similarly flagged as an error when used as a key to retain.
 *
 * @sample androidx.compose.runtime.retain.samples.retainSample
 * @sample androidx.compose.runtime.retain.samples.rememberAndRetainSample
 * @param keys An arbitrary list of keys that, if changed, will cause an old retained value to be
 *   discarded and for [calculation] to return a new value, regardless of whether the old value was
 *   being retained in the [RetainedValuesStore] or not.
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
    val retainedValuesStore = LocalRetainedValuesStore.current
    val holder =
        remember(key) {
            val retainedValue =
                retainedValuesStore.consumeExitedValueOrDefault(
                    key = key,
                    defaultValue = RetainedValuesStoreMissingValue,
                )

            if (retainedValue !== RetainedValuesStoreMissingValue) {
                RetainedValueHolder(
                    key = key,
                    value = @Suppress("UNCHECKED_CAST") (retainedValue as T),
                    owner = retainedValuesStore,
                    isNewlyRetained = false,
                )
            } else {
                RetainedValueHolder(
                    key = key,
                    value = calculation(),
                    owner = retainedValuesStore,
                    isNewlyRetained = true,
                )
            }
        }

    if (holder.owner !== retainedValuesStore) {
        SideEffect { holder.readoptUnder(retainedValuesStore) }
    }
    return holder.value
}

private val RetainedValuesStoreMissingValue = Any()

/**
 * Represents all identifying parameters passed into [retain]. Implementations of
 * [RetainedValuesStore] are given these keys to identify instances of a [retain] invocation.
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
