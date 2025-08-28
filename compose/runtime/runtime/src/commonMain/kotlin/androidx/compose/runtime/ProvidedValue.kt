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

import kotlin.jvm.JvmName

/**
 * An instance to hold a value provided by [CompositionLocalProvider] and is created by the
 * [ProvidableCompositionLocal.provides] infix operator. If [canOverride] is `false`, the provided
 * value will not overwrite a potentially already existing value in the scope.
 *
 * This value cannot be created directly. It can only be created by using one of the `provides`
 * operators of [ProvidableCompositionLocal].
 *
 * @see ProvidableCompositionLocal.provides
 * @see ProvidableCompositionLocal.providesDefault
 * @see ProvidableCompositionLocal.providesComputed
 */
public class ProvidedValue<T>
internal constructor(
    /**
     * The composition local that is provided by this value. This is the left-hand side of the
     * [ProvidableCompositionLocal.provides] infix operator.
     */
    public val compositionLocal: CompositionLocal<T>,
    value: T?,
    private val explicitNull: Boolean,
    internal val mutationPolicy: SnapshotMutationPolicy<T>?,
    internal val state: MutableState<T>?,
    internal val compute: (CompositionLocalAccessorScope.() -> T)?,
    internal val isDynamic: Boolean,
) {
    private val providedValue: T? = value

    /**
     * The value provided by the [ProvidableCompositionLocal.provides] infix operator. This is the
     * right-hand side of the operator.
     */
    @Suppress("UNCHECKED_CAST")
    public val value: T
        get() = providedValue as T

    /**
     * This value is `true` if the provided value will override any value provided above it. This
     * value is `true` when using [ProvidableCompositionLocal.provides] but `false` when using
     * [ProvidableCompositionLocal.providesDefault].
     *
     * @see ProvidableCompositionLocal.provides
     * @see ProvidableCompositionLocal.providesDefault
     */
    @get:JvmName("getCanOverride")
    public var canOverride: Boolean = true
        private set

    @Suppress("UNCHECKED_CAST")
    internal val effectiveValue: T
        get() =
            when {
                explicitNull -> null as T
                state != null -> state.value
                providedValue != null -> providedValue
                else -> composeRuntimeError("Unexpected form of a provided value")
            }

    internal val isStatic
        get() = (explicitNull || value != null) && !isDynamic

    internal fun ifNotAlreadyProvided() = this.also { canOverride = false }
}
