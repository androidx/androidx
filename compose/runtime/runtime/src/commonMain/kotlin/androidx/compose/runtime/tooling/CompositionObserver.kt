/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime.tooling

import androidx.compose.runtime.Composition
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.ObservableCompositionServiceKey
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getCompositionService

/**
 * Observe when new compositions are added to a recomposer. This, combined with,
 * [CompositionObserver], allows observing when any composition is being performed.
 *
 * This observer is registered with a [Recomposer] by calling [Recomposer.observe].
 */
@Suppress("CallbackName")
@ExperimentalComposeRuntimeApi
public interface CompositionRegistrationObserver {

    /**
     * Called whenever a [Composition] is registered with a [Recomposer] for which this is an
     * observer. A Composition is registered with its Recomposer when it begins its initial
     * composition, before any content is added. When a [CompositionRegistrationObserver] is
     * registered, this method will be called for all the [Recomposer]'s currently known
     * composition.
     *
     * This method is called on the same thread that the [Composition] being registered is being
     * composed on. During the initial dispatch, it is invoked on the same thread that the callback
     * is being registered on. Implementations of this method should be thread safe as they might be
     * called on an arbitrary thread.
     *
     * @param composition The [Composition] instance that is being registered with the recomposer.
     */
    public fun onCompositionRegistered(composition: ObservableComposition)

    /**
     * Called whenever a [Composition] is unregistered with a [Recomposer] for which this is an
     * observer. A Composition is unregistered from its Recomposer when the composition is
     * [disposed][Composition.dispose]. This method is called on the same thread that the
     * [Composition] being unregistered was composed on. Implementations of this method should be
     * thread safe as they might be called on an arbitrary thread.
     *
     * @param composition The [Composition] instance that is being unregistered with the recomposer.
     */
    public fun onCompositionUnregistered(composition: ObservableComposition)
}

/**
 * Observe [RecomposeScope] management inside composition.
 *
 * The expected lifecycle of a [RecomposeScope] in composition is as follows:
 * ```
 * // In the composition phase:
 * [onBeginComposition]
 *        ┃
 * [onScopeEnter] ━┓   // Composition enters a scope
 *                 ┃
 *          [onReadInScope]  // Record reads inside the scope
 *                 ┃
 *                 ┗━ [onScopeEnter]
 *                     ...             // Potentially enter nested function scopes
 *                 ┏━ [onScopeExit]
 *                 ┃
 * [onScopeExit]  ━┛   // Composition leaves the scope
 *        ┃
 * [onEndComposition]
 *
 * // In the apply changes phase:
 * [onScopeDisposed]   // Scope is discarded by composition and is no longer used.
 * ```
 *
 * The scopes can be invalidated at any point either by values previously reported in
 * [onReadInScope] or by calling [RecomposeScope.invalidate] directly. In these cases,
 * [onScopeInvalidated] will be called with the associated instance or `null` if the scope was
 * invalidated directly.
 *
 * Note that invalidation of the scope does not guarantee it will be composed. Some cases where it
 * is not composed are:
 * 1) The scope is no longer part of the composition (e.g the parent scope no longer executed the
 *    code branch the scope was a part of)
 * 2) The scope is part of movable content that was moved out of the composition.
 *
 * In the case of movable content, the scope will be recomposed as part of a different composition
 * when it is moved to that composition or it might be discarded (with a corresponding
 * [onScopeDisposed] call) if no other composition claims it.
 */
@Suppress("CallbackName")
@ExperimentalComposeRuntimeApi
public interface CompositionObserver {

    /** Called when the composition process begins for [composition] instance. */
    public fun onBeginComposition(composition: ObservableComposition)

    /** Called when [scope] enters the composition. */
    public fun onScopeEnter(scope: RecomposeScope)

    /**
     * Called when read of [value] is recorded in [scope] during composition.
     *
     * Reads can be recorded without re-execution of the function associated with the scope, for
     * example when derived state invalidates the scope with the same value as before.
     *
     * The instances passed to this method are only tracked between [onScopeEnter] and [onScopeExit]
     * calls. Previously recorded instances should also be cleared when [onScopeEnter] is called
     * again, to avoid keeping stale instances that are no longer tracked by composition. For
     * example, this happens with `remember { state.value }`, with `state` recorded only during
     * first composition.
     *
     * @param scope A [RecomposeScope] that the read occurred in.
     * @param value A value that was recorded in [scope] and can invalidate it in the future. In
     *   most cases, [value] is a snapshot state instance.
     */
    public fun onReadInScope(scope: RecomposeScope, value: Any)

    /** Called when [RecomposeScope] exits composition. */
    public fun onScopeExit(scope: RecomposeScope)

    /** Called after composition process has been completed for [composition]. */
    public fun onEndComposition(composition: ObservableComposition)

    /**
     * Called when [scope] is invalidated by composition or [RecomposeScope.invalidate] call.
     *
     * Note that for invalidations caused by a state change, this callback is not called immediately
     * on the state write. Usually, invalidations from state changes are recorded right before
     * recomposition starts or during composition (e.g. if `rememberUpdatedState` is used). This
     * method is always guaranteed to be called before [onScopeEnter] for the corresponding
     * invalidation is executed. If the scope was invalidated by [RecomposeScope.invalidate],
     * however, this callback is executed before [RecomposeScope.invalidate] returns.
     *
     * @param scope A [RecomposeScope] that is invalidated.
     * @param value A value that invalidated composition. Can be `null` if the scope was invalidated
     *   by calling [RecomposeScope.invalidate] directly.
     */
    public fun onScopeInvalidated(scope: RecomposeScope, value: Any?)

    /**
     * Called when [RecomposeScope] is no longer used in composition. Can be called from any thread
     * whenever composition applies changes or is disposed.
     */
    public fun onScopeDisposed(scope: RecomposeScope)
}

/**
 * The handle returned by [Composition.setObserver] and [Recomposer.observe]. Calling [dispose] will
 * prevent further composition observation events from being sent to the registered observer.
 */
@ExperimentalComposeRuntimeApi
public interface CompositionObserverHandle {
    /** Unregister the observer. */
    public fun dispose()
}

/** A composition instance that supports observing lifecycle of its [RecomposeScope]. */
@ExperimentalComposeRuntimeApi
public interface ObservableComposition {
    /**
     * Observe the composition. Calling this twice on the same composition will implicitly dispose
     * the previous observer. the [CompositionObserver] will be called for this composition and all
     * sub-composition, transitively, for which this composition is a context. If [setObserver] is
     * called on a sub-composition, it will override the parent composition observer for itself and
     * all its sub-compositions.
     *
     * @param observer the observer that will be informed of composition events for this composition
     *   and all sub-compositions for which this composition is the composition context.
     * @return a handle that allows the observer to be disposed and detached from the composition.
     *   Disposing an observer for a composition with a parent observer will begin sending the
     *   events to the parent composition's observer.
     */
    public fun setObserver(observer: CompositionObserver): CompositionObserverHandle
}

/**
 * Register an observer to be notified when a composition is added to or removed from the given
 * [Recomposer]. When this method is called, the observer will be notified of all currently
 * registered compositions per the documentation in
 * [CompositionRegistrationObserver.onCompositionRegistered].
 *
 * @param observer the observer that will be informed of new compositions registered with this
 *   [Recomposer].
 * @return a handle that allows the observer to be disposed and detached from the [Recomposer].
 */
@ExperimentalComposeRuntimeApi
public fun Recomposer.observe(
    observer: CompositionRegistrationObserver
): CompositionObserverHandle {
    return addCompositionRegistrationObserver(observer)
}

/**
 * Observe the composition. Calling this twice on the same composition will implicitly dispose the
 * previous observer. the [CompositionObserver] will be called for this composition and all
 * sub-composition, transitively, for which this composition is a context. If [setObserver] is
 * called on a sub-composition, it will override the parent composition observer for itself and all
 * its sub-compositions.
 *
 * @param observer the observer that will be informed of composition events for this composition and
 *   all sub-compositions for which this composition is the composition context. Observing a
 *   composition will prevent the parent composition's observer from receiving composition events
 *   about this composition.
 * @return a handle that allows the observer to be disposed and detached from the composition.
 *   Disposing an observer for a composition with a parent observer will begin sending the events to
 *   the parent composition's observer. A `null` indicates the composition does not support being
 *   observed.
 */
@ExperimentalComposeRuntimeApi
public fun Composition.setObserver(observer: CompositionObserver): CompositionObserverHandle? =
    getCompositionService(ObservableCompositionServiceKey)?.setObserver(observer)
