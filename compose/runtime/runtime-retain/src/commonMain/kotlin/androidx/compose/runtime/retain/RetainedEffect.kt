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
import androidx.compose.runtime.NonRestartableComposable

/**
 * Receiver scope for [RetainedEffect] that offers the [onRetire] clause that should be the last
 * statement in any call to [RetainedEffect].
 */
public class RetainedEffectScope {
    /**
     * Provide [onRetiredEffect] to the [DisposableEffect] to run when it leaves the composition or
     * its key changes.
     */
    public inline fun onRetire(crossinline onRetiredEffect: () -> Unit): RetainedEffectResult =
        object : RetainedEffectResult {
            override fun retire() {
                onRetiredEffect()
            }
        }
}

/**
 * The return type of a built [RetainedEffect]. This is created in the
 * [RetainedEffectScope.onRetire] clause and tracks the `onRetiredEffect` callback for internal
 * usage.
 */
public interface RetainedEffectResult {
    /**
     * Called when the [RetainedEffect] is retired from composition. This should call the
     * `onRetiredEffect` provided to [RetainedEffectScope.onRetire].
     */
    public fun retire()
}

private val InternalRetainedEffectScope = RetainedEffectScope()

private class RetainedEffectImpl(
    private val effect: RetainedEffectScope.() -> RetainedEffectResult
) : RetainObserver {
    private var onRetire: RetainedEffectResult? = null

    override fun onRetained() {
        onRetire = InternalRetainedEffectScope.effect()
    }

    override fun onRetired() {
        onRetire?.retire()
        onRetire = null
    }

    override fun onEnteredComposition() {
        // Do nothing. The effect doesn't respond to this callback event.
    }

    override fun onExitedComposition() {
        // Do nothing. The effect doesn't respond to this callback event.
    }

    override fun onUnused() {
        // Do nothing. The effect doesn't respond to this callback event.
    }
}

// This deprecated-error function shadows the varargs overload so that the varargs version
// is not used without key parameters.
@Composable
@NonRestartableComposable
@Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
@Deprecated(RetainedEffectNoParamError, level = DeprecationLevel.ERROR)
public fun RetainedEffect(effect: RetainedEffectScope.() -> RetainedEffectResult): Unit =
    error(RetainedEffectNoParamError)

private const val RetainedEffectNoParamError =
    "RetainedEffect must provide one or more 'key' parameters that define the identity of " +
        "the RetainedEffect and determine when its previous effect should be disposed and " +
        "a new effect started for the new key."

/**
 * A side effect of composition that must run for any new unique value of [key1] and must be
 * reversed or cleaned up if [key1] changes or if the [RetainedEffect] permanently leaves
 * composition.
 *
 * A [RetainedEffect] tracks the lifecycle of retained content. If the current [RetainScope] is
 * keeping values because its managed content is being transiently destroyed, the [RetainedEffect]
 * is kept alive. From this state, the [RetainedEffect] can either:
 * - Be retired because the [RetainScope] is destroyed without its content being restored
 * - Be retired if the [RetainScope]'s content re-enters the composition but does not include this
 *   [RetainedEffect] or invokes it with different keys
 * - Be restored to the recreated composition hierarchy. In this case, the [RetainedEffect] does not
 *   execute any callbacks.
 *
 * If a [RetainedEffect] is removed from the composition hierarchy when the [RetainScope] is not
 * keeping exited values, then the scope will immediately be retired and behave like a
 * [androidx.compose.runtime.DisposableEffect]. Retirement has the same timing guarantees as
 * [RetainObserver.onRetired].
 *
 * A [RetainedEffect]'s _key_ is a value that defines the identity of the [RetainedEffect]. If a
 * [RetainedEffect] is recomposed with different keys, a new effect will be created and the previous
 * effect will be retired. If the current RetainScope is not keeping exited values, the retirement
 * happens before the new effect is started. Otherwise, the prior instance of the effect will
 * continue to be retained for possible restoration until the scope stops keeping exited values.
 *
 * [RetainedEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided. For example:
 *
 * @sample androidx.compose.runtime.retain.samples.retainedEffectSample
 *
 * A [RetainedEffect] **must** include a [retire][RetainedEffectScope.onRetire] clause as the final
 * statement in its [effect] block. If your operation does not require disposal it might be a
 * [androidx.compose.runtime.SideEffect] instead, or a [androidx.compose.runtime.LaunchedEffect] if
 * it launches a coroutine that should be managed by the composition.
 *
 * There is guaranteed to be one call to [retire][RetainedEffectScope.onRetire] for every call to
 * [effect]. Both [effect] and [retire][RetainedEffectScope.onRetire] will always be run on the
 * composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running
 * [androidx.compose.runtime.RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
public fun RetainedEffect(key1: Any?, effect: RetainedEffectScope.() -> RetainedEffectResult) {
    retain(key1) { RetainedEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [key1] or [key2] and must
 * be reversed or cleaned up if [key1] or [key2] changes or if the [RetainedEffect] permanently
 * leaves composition.
 *
 * A [RetainedEffect] tracks the lifecycle of retained content. If the current [RetainScope] is
 * keeping values because its managed content is being transiently destroyed, the [RetainedEffect]
 * is kept alive. From this state, the [RetainedEffect] can either:
 * - Be retired because the [RetainScope] is destroyed without its content being restored
 * - Be retired if the [RetainScope]'s content re-enters the composition but does not include this
 *   [RetainedEffect] or invokes it with different keys
 * - Be restored to the recreated composition hierarchy. In this case, the [RetainedEffect] does not
 *   execute any callbacks.
 *
 * If a [RetainedEffect] is removed from the composition hierarchy when the [RetainScope] is not
 * keeping exited values, then the scope will immediately be retired and behave like a
 * [androidx.compose.runtime.DisposableEffect]. Retirement has the same timing guarantees as
 * [RetainObserver.onRetired].
 *
 * A [RetainedEffect]'s _key_ is a value that defines the identity of the [RetainedEffect]. If a
 * [RetainedEffect] is recomposed with different keys, a new effect will be created and the previous
 * effect will be retired. If the current RetainScope is not keeping exited values, the retirement
 * happens before the new effect is started. Otherwise, the prior instance of the effect will
 * continue to be retained for possible restoration until the scope stops keeping exited values.
 *
 * [RetainedEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided. For example:
 *
 * @sample androidx.compose.runtime.retain.samples.retainedEffectSample
 *
 * A [RetainedEffect] **must** include a [retire][RetainedEffectScope.onRetire] clause as the final
 * statement in its [effect] block. If your operation does not require disposal it might be a
 * [androidx.compose.runtime.SideEffect] instead, or a [androidx.compose.runtime.LaunchedEffect] if
 * it launches a coroutine that should be managed by the composition.
 *
 * There is guaranteed to be one call to [retire][RetainedEffectScope.onRetire] for every call to
 * [effect]. Both [effect] and [retire][RetainedEffectScope.onRetire] will always be run on the
 * composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running
 * [androidx.compose.runtime.RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
public fun RetainedEffect(
    key1: Any?,
    key2: Any?,
    effect: RetainedEffectScope.() -> RetainedEffectResult,
) {
    retain(key1, key2) { RetainedEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [key1], [key2], or [key3]
 * and must be reversed or cleaned up if [key1], [key2], or [key3] changes or if the
 * [RetainedEffect] permanently leaves composition.
 *
 * A [RetainedEffect] tracks the lifecycle of retained content. If the current [RetainScope] is
 * keeping values because its managed content is being transiently destroyed, the [RetainedEffect]
 * is kept alive. From this state, the [RetainedEffect] can either:
 * - Be retired because the [RetainScope] is destroyed without its content being restored
 * - Be retired if the [RetainScope]'s content re-enters the composition but does not include this
 *   [RetainedEffect] or invokes it with different keys
 * - Be restored to the recreated composition hierarchy. In this case, the [RetainedEffect] does not
 *   execute any callbacks.
 *
 * If a [RetainedEffect] is removed from the composition hierarchy when the [RetainScope] is not
 * keeping exited values, then the scope will immediately be retired and behave like a
 * [androidx.compose.runtime.DisposableEffect]. Retirement has the same timing guarantees as
 * [RetainObserver.onRetired].
 *
 * A [RetainedEffect]'s _key_ is a value that defines the identity of the [RetainedEffect]. If a
 * [RetainedEffect] is recomposed with different keys, a new effect will be created and the previous
 * effect will be retired. If the current RetainScope is not keeping exited values, the retirement
 * happens before the new effect is started. Otherwise, the prior instance of the effect will
 * continue to be retained for possible restoration until the scope stops keeping exited values.
 *
 * [RetainedEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided. For example:
 *
 * @sample androidx.compose.runtime.retain.samples.retainedEffectSample
 *
 * A [RetainedEffect] **must** include a [retire][RetainedEffectScope.onRetire] clause as the final
 * statement in its [effect] block. If your operation does not require disposal it might be a
 * [androidx.compose.runtime.SideEffect] instead, or a [androidx.compose.runtime.LaunchedEffect] if
 * it launches a coroutine that should be managed by the composition.
 *
 * There is guaranteed to be one call to [retire][RetainedEffectScope.onRetire] for every call to
 * [effect]. Both [effect] and [retire][RetainedEffectScope.onRetire] will always be run on the
 * composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running
 * [androidx.compose.runtime.RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
public fun RetainedEffect(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    effect: RetainedEffectScope.() -> RetainedEffectResult,
) {
    retain(key1, key2, key3) { RetainedEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [keys] and must be
 * reversed or cleaned up if [keys] changes or if the [RetainedEffect] permanently leaves
 * composition.
 *
 * A [RetainedEffect] tracks the lifecycle of retained content. If the current [RetainScope] is
 * keeping values because its managed content is being transiently destroyed, the [RetainedEffect]
 * is kept alive. From this state, the [RetainedEffect] can either:
 * - Be retired because the [RetainScope] is destroyed without its content being restored
 * - Be retired if the [RetainScope]'s content re-enters the composition but does not include this
 *   [RetainedEffect] or invokes it with different keys
 * - Be restored to the recreated composition hierarchy. In this case, the [RetainedEffect] does not
 *   execute any callbacks.
 *
 * If a [RetainedEffect] is removed from the composition hierarchy when the [RetainScope] is not
 * keeping exited values, then the scope will immediately be retired and behave like a
 * [DisposableEffect]. Retirement has the same timing guarantees as [RetainObserver.onRetired].
 *
 * A [RetainedEffect]'s _key_ is a value that defines the identity of the [RetainedEffect]. If a
 * [RetainedEffect] is recomposed with different keys, a new effect will be created and the previous
 * effect will be retired. If the current RetainScope is not keeping exited values, the retirement
 * happens before the new effect is started. Otherwise, the prior instance of the effect will
 * continue to be retained for possible restoration until the scope stops keeping exited values.
 *
 * [RetainedEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided. For example:
 *
 * @sample androidx.compose.runtime.retain.samples.retainedEffectSample
 *
 * A [RetainedEffect] **must** include a [retire][RetainedEffectScope.onRetire] clause as the final
 * statement in its [effect] block. If your operation does not require disposal it might be a
 * [androidx.compose.runtime.SideEffect] instead, or a [androidx.compose.runtime.LaunchedEffect] if
 * it launches a coroutine that should be managed by the composition.
 *
 * There is guaranteed to be one call to [retire][RetainedEffectScope.onRetire] for every call to
 * [effect]. Both [effect] and [retire][RetainedEffectScope.onRetire] will always be run on the
 * composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running
 * [androidx.compose.runtime.RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
public fun RetainedEffect(
    vararg keys: Any?,
    effect: RetainedEffectScope.() -> RetainedEffectResult,
) {
    retain(*keys) { RetainedEffectImpl(effect) }
}
