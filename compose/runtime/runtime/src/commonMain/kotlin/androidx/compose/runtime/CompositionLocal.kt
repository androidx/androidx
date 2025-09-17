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

package androidx.compose.runtime

/**
 * Compose passes data through the composition tree explicitly through means of parameters to
 * composable functions. This is often times the simplest and best way to have data flow through the
 * tree.
 *
 * Sometimes this model can be cumbersome or break down for data that is needed by lots of
 * components, or when components need to pass data between one another but keep that implementation
 * detail private. For these cases, [CompositionLocal]s can be used as an implicit way to have data
 * flow through a composition.
 *
 * [CompositionLocal]s by their nature are hierarchical. They make sense when the value of the
 * [CompositionLocal] needs to be scoped to a particular sub-hierarchy of the composition.
 *
 * One must create a [CompositionLocal] instance, which can be referenced by the consumers
 * statically. [CompositionLocal] instances themselves hold no data, and can be thought of as a
 * type-safe identifier for the data being passed down a tree. [CompositionLocal] factory functions
 * take a single parameter: a factory to create a default value in cases where a [CompositionLocal]
 * is used without a Provider. If this is a situation you would rather not handle, you can throw an
 * error in this factory.
 *
 * @sample androidx.compose.runtime.samples.createCompositionLocal
 *
 * Somewhere up the tree, a [CompositionLocalProvider] component can be used, which provides a value
 * for the [CompositionLocal]. This would often be at the "root" of a tree, but could be anywhere,
 * and can also be used in multiple places to override the provided value for a sub-tree.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalProvider
 *
 * Intermediate components do not need to know about the [CompositionLocal] value, and can have zero
 * dependencies on it. For example, `SomeScreen` might look like this:
 *
 * @sample androidx.compose.runtime.samples.someScreenSample
 *
 * Finally, a component that wishes to consume the [CompositionLocal] value can use the [current]
 * property of the [CompositionLocal] key which returns the current value of the [CompositionLocal],
 * and subscribes the component to changes of it.
 *
 * @sample androidx.compose.runtime.samples.consumeCompositionLocal
 */
@Stable
public sealed class CompositionLocal<T>(defaultFactory: () -> T) {
    internal open val defaultValueHolder: ValueHolder<T> = LazyValueHolder(defaultFactory)

    internal abstract fun updatedStateOf(
        value: ProvidedValue<T>,
        previous: ValueHolder<T>?,
    ): ValueHolder<T>

    /**
     * Return the value provided by the nearest [CompositionLocalProvider] component that invokes,
     * directly or indirectly, the composable function that uses this property.
     *
     * @sample androidx.compose.runtime.samples.consumeCompositionLocal
     */
    @OptIn(InternalComposeApi::class)
    public inline val current: T
        @ReadOnlyComposable @Composable get() = currentComposer.consume(this)
}

/**
 * A [ProvidableCompositionLocal] can be used in [CompositionLocalProvider] to provide values.
 *
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 * @see CompositionLocal
 * @see CompositionLocalProvider
 */
@Stable
public abstract class ProvidableCompositionLocal<T> internal constructor(defaultFactory: () -> T) :
    CompositionLocal<T>(defaultFactory) {
    internal abstract fun defaultProvidedValue(value: T): ProvidedValue<T>

    /**
     * Associates a [CompositionLocal] key to a value in a call to [CompositionLocalProvider].
     *
     * @see CompositionLocal
     * @see ProvidableCompositionLocal
     */
    public infix fun provides(value: T): ProvidedValue<T> = defaultProvidedValue(value)

    /**
     * Associates a [CompositionLocal] key to a value in a call to [CompositionLocalProvider] if the
     * key does not already have an associated value.
     *
     * @see CompositionLocal
     * @see ProvidableCompositionLocal
     */
    public infix fun providesDefault(value: T): ProvidedValue<T> =
        defaultProvidedValue(value).ifNotAlreadyProvided()

    /**
     * Associates a [CompositionLocal] key to a lambda, [compute], in a call to [CompositionLocal].
     * The [compute] lambda is invoked whenever the key is retrieved. The lambda is executed in the
     * context of a [CompositionLocalContext] which allow retrieving the current values of other
     * composition locals by calling [CompositionLocalAccessorScope.currentValue], which is an
     * extension function provided by the context for a [CompositionLocal] key.
     *
     * The lambda passed to [providesComputed] will be invoked every time the
     * [CompositionLocal.current] is evaluated for the composition local and computes its value
     * based on the current value of the locals referenced in the lambda at the time
     * [CompositionLocal.current] is evaluated. This allows providing values that can be derived
     * from other locals. For example, if accent colors can be calculated from a single base color,
     * the accent colors can be provided as computed composition locals. Providing a new base color
     * would automatically update all the accent colors.
     *
     * @sample androidx.compose.runtime.samples.compositionLocalProvidedComputed
     * @sample androidx.compose.runtime.samples.compositionLocalComputedAfterProvidingLocal
     * @see CompositionLocal
     * @see CompositionLocalContext
     * @see ProvidableCompositionLocal
     */
    public infix fun providesComputed(
        compute: CompositionLocalAccessorScope.() -> T
    ): ProvidedValue<T> =
        ProvidedValue(
            compositionLocal = this,
            value = null,
            explicitNull = false,
            mutationPolicy = null,
            state = null,
            compute = compute,
            isDynamic = false,
        )

    override fun updatedStateOf(
        value: ProvidedValue<T>,
        previous: ValueHolder<T>?,
    ): ValueHolder<T> {
        return when (previous) {
            is DynamicValueHolder ->
                if (value.isDynamic) {
                    previous.state.value = value.effectiveValue
                    previous
                } else null
            is StaticValueHolder ->
                if (value.isStatic && value.effectiveValue == previous.value) previous else null
            is ComputedValueHolder -> if (value.compute === previous.compute) previous else null
            else -> null
        } ?: valueHolderOf(value)
    }

    private fun valueHolderOf(value: ProvidedValue<T>): ValueHolder<T> =
        when {
            value.isDynamic ->
                DynamicValueHolder(
                    value.state
                        ?: mutableStateOf(
                            value.value,
                            value.mutationPolicy ?: structuralEqualityPolicy(),
                        )
                )
            value.compute != null -> ComputedValueHolder(value.compute)
            value.state != null -> DynamicValueHolder(value.state)
            else -> StaticValueHolder(value.effectiveValue)
        }
}

/**
 * A [DynamicProvidableCompositionLocal] is a [CompositionLocal] backed by [mutableStateOf].
 * Providing new values using a [DynamicProvidableCompositionLocal] will provide the same [State]
 * with a different value. Reading the [CompositionLocal] value of a
 * [DynamicProvidableCompositionLocal] will record a read in the [RecomposeScope] of the
 * composition. Changing the provided value will invalidate the [RecomposeScope]s.
 *
 * @see compositionLocalOf
 */
internal class DynamicProvidableCompositionLocal<T>(
    private val policy: SnapshotMutationPolicy<T>,
    defaultFactory: () -> T,
) : ProvidableCompositionLocal<T>(defaultFactory) {

    override fun defaultProvidedValue(value: T) =
        ProvidedValue(
            compositionLocal = this,
            value = value,
            explicitNull = value === null,
            mutationPolicy = policy,
            state = null,
            compute = null,
            isDynamic = true,
        )
}

/**
 * A [StaticProvidableCompositionLocal] is a value that is expected to rarely change.
 *
 * @see staticCompositionLocalOf
 */
internal class StaticProvidableCompositionLocal<T>(defaultFactory: () -> T) :
    ProvidableCompositionLocal<T>(defaultFactory) {

    override fun defaultProvidedValue(value: T) =
        ProvidedValue(
            compositionLocal = this,
            value = value,
            explicitNull = value === null,
            mutationPolicy = null,
            state = null,
            compute = null,
            isDynamic = false,
        )
}

/**
 * Create a [CompositionLocal] key that can be provided using [CompositionLocalProvider]. Changing
 * the value provided during recomposition will invalidate the content of [CompositionLocalProvider]
 * that read the value using [CompositionLocal.current].
 *
 * [compositionLocalOf] creates a [ProvidableCompositionLocal] which can be used in a a call to
 * [CompositionLocalProvider]. Similar to [MutableList] vs. [List], if the key is made public as
 * [CompositionLocal] instead of [ProvidableCompositionLocal], it can be read using
 * [CompositionLocal.current] but not re-provided.
 *
 * @param policy a policy to determine when a [CompositionLocal] is considered changed. See
 *   [SnapshotMutationPolicy] for details.
 * @param defaultFactory a value factory to supply a value when a value is not provided. This
 *   factory is called when no value is provided through a [CompositionLocalProvider] of the caller
 *   of the component using [CompositionLocal.current]. If no reasonable default can be provided
 *   then consider throwing an exception.
 * @see CompositionLocal
 * @see staticCompositionLocalOf
 * @see mutableStateOf
 */
public fun <T> compositionLocalOf(
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy(),
    defaultFactory: () -> T,
): ProvidableCompositionLocal<T> = DynamicProvidableCompositionLocal(policy, defaultFactory)

/**
 * Create a [CompositionLocal] key that can be provided using [CompositionLocalProvider].
 *
 * Unlike [compositionLocalOf], reads of a [staticCompositionLocalOf] are not tracked by the
 * composer and changing the value provided in the [CompositionLocalProvider] call will cause the
 * entirety of the content to be recomposed instead of just the places where in the composition the
 * local value is used. This lack of tracking, however, makes a [staticCompositionLocalOf] more
 * efficient when the value provided is highly unlikely to or will never change. For example, the
 * android context, font loaders, or similar shared values, are unlikely to change for the
 * components in the content of a the [CompositionLocalProvider] and should consider using a
 * [staticCompositionLocalOf]. A color, or other theme like value, might change or even be animated
 * therefore a [compositionLocalOf] should be used.
 *
 * [staticCompositionLocalOf] creates a [ProvidableCompositionLocal] which can be used in a a call
 * to [CompositionLocalProvider]. Similar to [MutableList] vs. [List], if the key is made public as
 * [CompositionLocal] instead of [ProvidableCompositionLocal], it can be read using
 * [CompositionLocal.current] but not re-provided.
 *
 * @param defaultFactory a value factory to supply a value when a value is not provided. This
 *   factory is called when no value is provided through a [CompositionLocalProvider] of the caller
 *   of the component using [CompositionLocal.current]. If no reasonable default can be provided
 *   then consider throwing an exception.
 * @see CompositionLocal
 * @see compositionLocalOf
 */
public fun <T> staticCompositionLocalOf(defaultFactory: () -> T): ProvidableCompositionLocal<T> =
    StaticProvidableCompositionLocal(defaultFactory)

/**
 * Create a [CompositionLocal] that behaves like it was provided using
 * [ProvidableCompositionLocal.providesComputed] by default. If a value is provided using
 * [ProvidableCompositionLocal.provides] it behaves as if the [CompositionLocal] was produced by
 * calling [compositionLocalOf].
 *
 * In other words, a [CompositionLocal] produced by can be provided identically to
 * [CompositionLocal] created with [compositionLocalOf] with the only difference is how it behaves
 * when the value is not provided. For a [compositionLocalOf] the default value is returned. If no
 * default value has be computed for [CompositionLocal] the default computation is called.
 *
 * The lambda passed to [compositionLocalWithComputedDefaultOf] will be invoked every time the
 * [CompositionLocal.current] is evaluated for the composition local and computes its value based on
 * the current value of the locals referenced in the lambda at the time [CompositionLocal.current]
 * is evaluated. This allows providing values that can be derived from other locals. For example, if
 * accent colors can be calculated from a single base color, the accent colors can be provided as
 * computed composition locals. Providing a new base color would automatically update all the accent
 * colors.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalComputedByDefault
 * @sample androidx.compose.runtime.samples.compositionLocalComputedAfterProvidingLocal
 * @param defaultComputation the default computation to use when this [CompositionLocal] is not
 *   provided.
 * @see CompositionLocal
 * @see ProvidableCompositionLocal
 */
public fun <T> compositionLocalWithComputedDefaultOf(
    defaultComputation: CompositionLocalAccessorScope.() -> T
): ProvidableCompositionLocal<T> = ComputedProvidableCompositionLocal(defaultComputation)

internal class ComputedProvidableCompositionLocal<T>(
    defaultComputation: CompositionLocalAccessorScope.() -> T
) : ProvidableCompositionLocal<T>({ composeRuntimeError("Unexpected call to default provider") }) {
    override val defaultValueHolder = ComputedValueHolder(defaultComputation)

    override fun defaultProvidedValue(value: T): ProvidedValue<T> =
        ProvidedValue(
            compositionLocal = this,
            value = value,
            explicitNull = value === null,
            mutationPolicy = null,
            state = null,
            compute = null,
            isDynamic = true,
        )
}

public interface CompositionLocalAccessorScope {
    /**
     * An extension property that allows accessing the current value of a composition local in the
     * context of this scope. This scope is the type of the `this` parameter when in a computed
     * composition. Computed composition locals can be provided by either using
     * [compositionLocalWithComputedDefaultOf] or by using the
     * [ProvidableCompositionLocal.providesComputed] infix operator.
     *
     * @sample androidx.compose.runtime.samples.compositionLocalProvidedComputed
     * @see ProvidableCompositionLocal
     * @see ProvidableCompositionLocal.providesComputed
     * @see ProvidableCompositionLocal.provides
     * @see CompositionLocalProvider
     */
    public val <T> CompositionLocal<T>.currentValue: T
}

/**
 * Stores [CompositionLocal]'s and their values.
 *
 * Can be obtained via [currentCompositionLocalContext] and passed to another composition via
 * [CompositionLocalProvider].
 *
 * [CompositionLocalContext] is immutable and won't be changed after its obtaining.
 */
@Stable
public class CompositionLocalContext
internal constructor(internal val compositionLocals: PersistentCompositionLocalMap) {
    override fun equals(other: Any?): Boolean {
        return other is CompositionLocalContext && other.compositionLocals == this.compositionLocals
    }

    override fun hashCode(): Int {
        return compositionLocals.hashCode()
    }
}

/**
 * [CompositionLocalProvider] binds values to [ProvidableCompositionLocal] keys. Reading the
 * [CompositionLocal] using [CompositionLocal.current] will return the value provided in
 * [CompositionLocalProvider]'s [values] parameter for all composable functions called directly or
 * indirectly in the [content] lambda.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalProvider
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Composable
@OptIn(InternalComposeApi::class)
@NonSkippableComposable
public fun CompositionLocalProvider(
    vararg values: ProvidedValue<*>,
    content: @Composable () -> Unit,
) {
    currentComposer.startProviders(values)
    content()
    currentComposer.endProviders()
}

/**
 * [CompositionLocalProvider] binds value to [ProvidableCompositionLocal] key. Reading the
 * [CompositionLocal] using [CompositionLocal.current] will return the value provided in
 * [CompositionLocalProvider]'s [value] parameter for all composable functions called directly or
 * indirectly in the [content] lambda.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalProvider
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Composable
@OptIn(InternalComposeApi::class)
@NonSkippableComposable
public fun CompositionLocalProvider(value: ProvidedValue<*>, content: @Composable () -> Unit) {
    currentComposer.startProvider(value)
    content()
    currentComposer.endProvider()
}

/**
 * [CompositionLocalProvider] binds values to [CompositionLocal]'s, provided by [context]. Reading
 * the [CompositionLocal] using [CompositionLocal.current] will return the value provided in values
 * stored inside [context] for all composable functions called directly or indirectly in the
 * [content] lambda.
 *
 * @sample androidx.compose.runtime.samples.compositionLocalProvider
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Composable
public fun CompositionLocalProvider(
    context: CompositionLocalContext,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        *context.compositionLocals.map { it.value.toProvided(it.key) }.toTypedArray(),
        content = content,
    )
}

/**
 * [withCompositionLocal] binds value to [ProvidableCompositionLocal] key and returns the result
 * produced by the [content] lambda. Use with non-unit returning [content] lambdas or else use
 * [CompositionLocalProvider]. Reading the [CompositionLocal] using [CompositionLocal.current] will
 * return the value provided in [CompositionLocalProvider]'s [value] parameter for all composable
 * functions called directly or indirectly in the [content] lambda.
 *
 * @see CompositionLocalProvider
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Suppress("BanInlineOptIn") // b/430604046 - These APIs are stable so are ok to inline
@OptIn(InternalComposeApi::class)
@Composable
public inline fun <T> withCompositionLocal(
    value: ProvidedValue<*>,
    content: @Composable () -> T,
): T {
    currentComposer.startProvider(value)
    return content().also { currentComposer.endProvider() }
}

/**
 * [withCompositionLocals] binds values to [ProvidableCompositionLocal] key and returns the result
 * produced by the [content] lambda. Use with non-unit returning [content] lambdas or else use
 * [CompositionLocalProvider]. Reading the [CompositionLocal] using [CompositionLocal.current] will
 * return the values provided in [CompositionLocalProvider]'s [values] parameter for all composable
 * functions called directly or indirectly in the [content] lambda.
 *
 * @see CompositionLocalProvider
 * @see CompositionLocal
 * @see compositionLocalOf
 * @see staticCompositionLocalOf
 */
@Suppress("BanInlineOptIn") // b/430604046 - These APIs are stable so are ok to inline
@OptIn(InternalComposeApi::class)
@Composable
public inline fun <T> withCompositionLocals(
    vararg values: ProvidedValue<*>,
    content: @Composable () -> T,
): T {
    currentComposer.startProviders(values)
    return content().also { currentComposer.endProvider() }
}
