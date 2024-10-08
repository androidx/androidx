/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.runtime.internal.PlatformOptimizedCancellationException
import kotlin.concurrent.Volatile
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.jvm.JvmField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Schedule [effect] to run when the current composition completes successfully and applies changes.
 * [SideEffect] can be used to apply side effects to objects managed by the composition that are not
 * backed by [snapshots][androidx.compose.runtime.snapshots.Snapshot] so as not to leave those
 * objects in an inconsistent state if the current composition operation fails.
 *
 * [effect] will always be run on the composition's apply dispatcher and appliers are never run
 * concurrent with themselves, one another, applying changes to the composition tree, or running
 * [RememberObserver] event callbacks. [SideEffect]s are always run after [RememberObserver] event
 * callbacks.
 *
 * A [SideEffect] runs after **every** recomposition. To launch an ongoing task spanning potentially
 * many recompositions, see [LaunchedEffect]. To manage an event subscription or other object
 * lifecycle, see [DisposableEffect].
 */
@Composable
@NonRestartableComposable
@ExplicitGroupsComposable
@OptIn(InternalComposeApi::class)
fun SideEffect(effect: () -> Unit) {
    currentComposer.recordSideEffect(effect)
}

/**
 * Receiver scope for [DisposableEffect] that offers the [onDispose] clause that should be the last
 * statement in any call to [DisposableEffect].
 */
class DisposableEffectScope {
    /**
     * Provide [onDisposeEffect] to the [DisposableEffect] to run when it leaves the composition or
     * its key changes.
     */
    inline fun onDispose(crossinline onDisposeEffect: () -> Unit): DisposableEffectResult =
        object : DisposableEffectResult {
            override fun dispose() {
                onDisposeEffect()
            }
        }
}

interface DisposableEffectResult {
    fun dispose()
}

private val InternalDisposableEffectScope = DisposableEffectScope()

private class DisposableEffectImpl(
    private val effect: DisposableEffectScope.() -> DisposableEffectResult
) : RememberObserver {
    private var onDispose: DisposableEffectResult? = null

    override fun onRemembered() {
        onDispose = InternalDisposableEffectScope.effect()
    }

    override fun onForgotten() {
        onDispose?.dispose()
        onDispose = null
    }

    override fun onAbandoned() {
        // Nothing to do as [onRemembered] was not called.
    }
}

private const val DisposableEffectNoParamError =
    "DisposableEffect must provide one or more 'key' parameters that define the identity of " +
        "the DisposableEffect and determine when its previous effect should be disposed and " +
        "a new effect started for the new key."

private const val LaunchedEffectNoParamError =
    "LaunchedEffect must provide one or more 'key' parameters that define the identity of " +
        "the LaunchedEffect and determine when its previous effect coroutine should be cancelled " +
        "and a new effect launched for the new key."

/**
 * A side effect of composition that must be reversed or cleaned up if the [DisposableEffect] leaves
 * the composition.
 *
 * It is an error to call [DisposableEffect] without at least one `key` parameter.
 */
// This deprecated-error function shadows the varargs overload so that the varargs version
// is not used without key parameters.
@Composable
@NonRestartableComposable
@Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
@Deprecated(DisposableEffectNoParamError, level = DeprecationLevel.ERROR)
fun DisposableEffect(effect: DisposableEffectScope.() -> DisposableEffectResult): Unit =
    error(DisposableEffectNoParamError)

/**
 * A side effect of composition that must run for any new unique value of [key1] and must be
 * reversed or cleaned up if [key1] changes or if the [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the [DisposableEffect]. If a
 * key changes, the [DisposableEffect] must [dispose][DisposableEffectScope.onDispose] its current
 * [effect] and reset by calling [effect] again. Examples of keys include:
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided, performing cleanup for the old operation before initializing the new.
 * For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause as
 * the final statement in its [effect] block. If your operation does not require disposal it might
 * be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should be
 * managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run on
 * the composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running [RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
fun DisposableEffect(key1: Any?, effect: DisposableEffectScope.() -> DisposableEffectResult) {
    remember(key1) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [key1] or [key2] and must
 * be reversed or cleaned up if [key1] or [key2] changes, or if the [DisposableEffect] leaves the
 * composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the [DisposableEffect]. If a
 * key changes, the [DisposableEffect] must [dispose][DisposableEffectScope.onDispose] its current
 * [effect] and reset by calling [effect] again. Examples of keys include:
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided, performing cleanup for the old operation before initializing the new.
 * For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause as
 * the final statement in its [effect] block. If your operation does not require disposal it might
 * be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should be
 * managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run on
 * the composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running [RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
fun DisposableEffect(
    key1: Any?,
    key2: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectResult
) {
    remember(key1, key2) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [key1], [key2] or [key3]
 * and must be reversed or cleaned up if [key1], [key2] or [key3] changes, or if the
 * [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the [DisposableEffect]. If a
 * key changes, the [DisposableEffect] must [dispose][DisposableEffectScope.onDispose] its current
 * [effect] and reset by calling [effect] again. Examples of keys include:
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided, performing cleanup for the old operation before initializing the new.
 * For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause as
 * the final statement in its [effect] block. If your operation does not require disposal it might
 * be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should be
 * managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run on
 * the composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running [RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
fun DisposableEffect(
    key1: Any?,
    key2: Any?,
    key3: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectResult
) {
    remember(key1, key2, key3) { DisposableEffectImpl(effect) }
}

/**
 * A side effect of composition that must run for any new unique value of [keys] and must be
 * reversed or cleaned up if any [keys] change or if the [DisposableEffect] leaves the composition.
 *
 * A [DisposableEffect]'s _key_ is a value that defines the identity of the [DisposableEffect]. If a
 * key changes, the [DisposableEffect] must [dispose][DisposableEffectScope.onDispose] its current
 * [effect] and reset by calling [effect] again. Examples of keys include:
 * * Observable objects that the effect subscribes to
 * * Unique request parameters to an operation that must cancel and retry if those parameters change
 *
 * [DisposableEffect] may be used to initialize or subscribe to a key and reinitialize when a
 * different key is provided, performing cleanup for the old operation before initializing the new.
 * For example:
 *
 * @sample androidx.compose.runtime.samples.disposableEffectSample
 *
 * A [DisposableEffect] **must** include an [onDispose][DisposableEffectScope.onDispose] clause as
 * the final statement in its [effect] block. If your operation does not require disposal it might
 * be a [SideEffect] instead, or a [LaunchedEffect] if it launches a coroutine that should be
 * managed by the composition.
 *
 * There is guaranteed to be one call to [dispose][DisposableEffectScope.onDispose] for every call
 * to [effect]. Both [effect] and [dispose][DisposableEffectScope.onDispose] will always be run on
 * the composition's apply dispatcher and appliers are never run concurrent with themselves, one
 * another, applying changes to the composition tree, or running [RememberObserver] event callbacks.
 */
@Composable
@NonRestartableComposable
@Suppress("ArrayReturn")
fun DisposableEffect(
    vararg keys: Any?,
    effect: DisposableEffectScope.() -> DisposableEffectResult
) {
    remember(*keys) { DisposableEffectImpl(effect) }
}

internal class LaunchedEffectImpl(
    parentCoroutineContext: CoroutineContext,
    private val task: suspend CoroutineScope.() -> Unit
) : RememberObserver {
    private val scope = CoroutineScope(parentCoroutineContext)
    private var job: Job? = null

    override fun onRemembered() {
        // This should never happen but is left here for safety
        job?.cancel("Old job was still running!")
        job = scope.launch(block = task)
    }

    override fun onForgotten() {
        job?.cancel(LeftCompositionCancellationException())
        job = null
    }

    override fun onAbandoned() {
        job?.cancel(LeftCompositionCancellationException())
        job = null
    }
}

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] when the [LaunchedEffect]
 * leaves the composition.
 *
 * It is an error to call [LaunchedEffect] without at least one `key` parameter.
 */
// This deprecated-error function shadows the varargs overload so that the varargs version
// is not used without key parameters.
@Deprecated(LaunchedEffectNoParamError, level = DeprecationLevel.ERROR)
@Suppress("DeprecatedCallableAddReplaceWith", "UNUSED_PARAMETER")
@Composable
fun LaunchedEffect(block: suspend CoroutineScope.() -> Unit): Unit =
    error(LaunchedEffectNoParamError)

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with a different [key1]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback events
 * by way of storing callback data in [MutableState] passed to [key1]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@NonRestartableComposable
@OptIn(InternalComposeApi::class)
fun LaunchedEffect(key1: Any?, block: suspend CoroutineScope.() -> Unit) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(key1) { LaunchedEffectImpl(applyContext, block) }
}

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with a different [key1] or [key2]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback events
 * by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@NonRestartableComposable
@OptIn(InternalComposeApi::class)
fun LaunchedEffect(key1: Any?, key2: Any?, block: suspend CoroutineScope.() -> Unit) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(key1, key2) { LaunchedEffectImpl(applyContext, block) }
}

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with a different [key1], [key2] or [key3]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback events
 * by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@NonRestartableComposable
@OptIn(InternalComposeApi::class)
fun LaunchedEffect(key1: Any?, key2: Any?, key3: Any?, block: suspend CoroutineScope.() -> Unit) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(key1, key2, key3) { LaunchedEffectImpl(applyContext, block) }
}

private class LeftCompositionCancellationException :
    PlatformOptimizedCancellationException("The coroutine scope left the composition")

/**
 * When [LaunchedEffect] enters the composition it will launch [block] into the composition's
 * [CoroutineContext]. The coroutine will be [cancelled][Job.cancel] and **re-launched** when
 * [LaunchedEffect] is recomposed with any different [keys]. The coroutine will be
 * [cancelled][Job.cancel] when the [LaunchedEffect] leaves the composition.
 *
 * This function should **not** be used to (re-)launch ongoing tasks in response to callback events
 * by way of storing callback data in [MutableState] passed to [key]. Instead, see
 * [rememberCoroutineScope] to obtain a [CoroutineScope] that may be used to launch ongoing jobs
 * scoped to the composition in response to event callbacks.
 */
@Composable
@NonRestartableComposable
@Suppress("ArrayReturn")
@OptIn(InternalComposeApi::class)
fun LaunchedEffect(vararg keys: Any?, block: suspend CoroutineScope.() -> Unit) {
    val applyContext = currentComposer.applyCoroutineContext
    remember(*keys) { LaunchedEffectImpl(applyContext, block) }
}

// Maintenance note: this class once was used by the inlined implementation of
// rememberCoroutineScope and must be maintained for binary compatibility. The new implementation
// of RememberedCoroutineScope implements RememberObserver directly, since as of this writing the
// compose runtime no longer implicitly treats objects incidentally stored in the slot table (e.g.
// previous parameter values from a skippable invocation, remember keys, etc.) as eligible
// RememberObservers. This dramatically reduces the risk of receiving unexpected RememberObserver
// lifecycle callbacks when a reference to a RememberObserver is leaked into user code and we can
// omit wrapper RememberObservers such as this one.
@PublishedApi
internal class CompositionScopedCoroutineScopeCanceller(val coroutineScope: CoroutineScope) :
    RememberObserver {
    override fun onRemembered() {
        // Nothing to do
    }

    override fun onForgotten() {
        val coroutineScope = coroutineScope
        if (coroutineScope is RememberedCoroutineScope) {
            coroutineScope.cancelIfCreated()
        } else {
            coroutineScope.cancel(LeftCompositionCancellationException())
        }
    }

    override fun onAbandoned() {
        val coroutineScope = coroutineScope
        if (coroutineScope is RememberedCoroutineScope) {
            coroutineScope.cancelIfCreated()
        } else {
            coroutineScope.cancel(LeftCompositionCancellationException())
        }
    }
}

private class CancelledCoroutineContext : CoroutineContext.Element {
    override val key: CoroutineContext.Key<*>
        get() = Key

    companion object Key : CoroutineContext.Key<CancelledCoroutineContext>
}

private class ForgottenCoroutineScopeException :
    PlatformOptimizedCancellationException("rememberCoroutineScope left the composition")

internal class RememberedCoroutineScope(
    private val parentContext: CoroutineContext,
    private val overlayContext: CoroutineContext,
) : CoroutineScope, RememberObserver {
    private val lock = makeSynchronizedObject(this)

    // The goal of this implementation is to make cancellation as cheap as possible if the
    // coroutineContext property was never accessed, consisting only of taking a monitor lock and
    // setting a volatile field.

    @Volatile private var _coroutineContext: CoroutineContext? = null

    override val coroutineContext: CoroutineContext
        get() {
            var localCoroutineContext = _coroutineContext
            if (
                localCoroutineContext == null || localCoroutineContext === CancelledCoroutineContext
            ) {
                // Yes, we're leaking our lock here by using the instance of the object
                // that also gets handled by user code as a CoroutineScope as an intentional
                // tradeoff for avoiding the allocation of a dedicated lock object.
                // Since we only use it here for this lazy initialization and control flow
                // does not escape the creation of the CoroutineContext while holding the lock,
                // the splash damage should be acceptable.
                synchronized(lock) {
                    localCoroutineContext = _coroutineContext
                    if (localCoroutineContext == null) {
                        val parentContext = parentContext
                        val childJob = Job(parentContext[Job])
                        localCoroutineContext = parentContext + childJob + overlayContext
                    } else if (localCoroutineContext === CancelledCoroutineContext) {
                        // Lazily initialize the child job here, already cancelled.
                        // Assemble the CoroutineContext exactly as otherwise expected.
                        val parentContext = parentContext
                        val cancelledChildJob =
                            Job(parentContext[Job]).apply {
                                cancel(ForgottenCoroutineScopeException())
                            }
                        localCoroutineContext = parentContext + cancelledChildJob + overlayContext
                    }
                    _coroutineContext = localCoroutineContext
                }
            }
            return localCoroutineContext!!
        }

    fun cancelIfCreated() {
        // Take the lock unconditionally; this is internal API only used by internal
        // RememberObserver implementations that are not leaked to user code; we can assume
        // this won't be called repeatedly. If this assumption is violated we'll simply create a
        // redundant exception.
        synchronized(lock) {
            val context = _coroutineContext
            if (context == null) {
                _coroutineContext = CancelledCoroutineContext
            } else {
                // Ignore optimizing the case where we might be cancelling an already cancelled job;
                // only internal callers such as RememberObservers will invoke this method.
                context.cancel(ForgottenCoroutineScopeException())
            }
        }
    }

    override fun onRemembered() {
        // Do nothing
    }

    override fun onForgotten() {
        cancelIfCreated()
    }

    override fun onAbandoned() {
        cancelIfCreated()
    }

    companion object {
        @JvmField val CancelledCoroutineContext: CoroutineContext = CancelledCoroutineContext()
    }
}

@PublishedApi
@OptIn(InternalComposeApi::class)
internal fun createCompositionCoroutineScope(
    coroutineContext: CoroutineContext,
    composer: Composer
) =
    if (coroutineContext[Job] != null) {
        CoroutineScope(
            Job().apply {
                completeExceptionally(
                    IllegalArgumentException(
                        "CoroutineContext supplied to " +
                            "rememberCoroutineScope may not include a parent job"
                    )
                )
            }
        )
    } else {
        val applyContext = composer.applyCoroutineContext
        RememberedCoroutineScope(applyContext, coroutineContext)
    }

/**
 * Return a [CoroutineScope] bound to this point in the composition using the optional
 * [CoroutineContext] provided by [getContext]. [getContext] will only be called once and the same
 * [CoroutineScope] instance will be returned across recompositions.
 *
 * This scope will be [cancelled][CoroutineScope.cancel] when this call leaves the composition. The
 * [CoroutineContext] returned by [getContext] may not contain a [Job] as this scope is considered
 * to be a child of the composition.
 *
 * The default dispatcher of this scope if one is not provided by the context returned by
 * [getContext] will be the applying dispatcher of the composition's [Recomposer].
 *
 * Use this scope to launch jobs in response to callback events such as clicks or other user
 * interaction where the response to that event needs to unfold over time and be cancelled if the
 * composable managing that process leaves the composition. Jobs should never be launched into
 * **any** coroutine scope as a side effect of composition itself. For scoped ongoing jobs initiated
 * by composition, see [LaunchedEffect].
 *
 * This function will not throw if preconditions are not met, as composable functions do not yet
 * fully support exceptions. Instead the returned scope's [CoroutineScope.coroutineContext] will
 * contain a failed [Job] with the associated exception and will not be capable of launching child
 * jobs.
 */
@Composable
inline fun rememberCoroutineScope(
    crossinline getContext: @DisallowComposableCalls () -> CoroutineContext = {
        EmptyCoroutineContext
    }
): CoroutineScope {
    val composer = currentComposer
    return remember { createCompositionCoroutineScope(getContext(), composer) }
}
