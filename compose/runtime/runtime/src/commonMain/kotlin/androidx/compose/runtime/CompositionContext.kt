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

import androidx.collection.ScatterSet
import androidx.compose.runtime.internal.persistentCompositionLocalHashMapOf
import androidx.compose.runtime.tooling.CompositionData
import kotlin.coroutines.CoroutineContext

private val EmptyPersistentCompositionLocalMap: PersistentCompositionLocalMap =
    persistentCompositionLocalHashMapOf()

/**
 * A [CompositionContext] is an opaque type that is used to logically "link" two compositions
 * together. The [CompositionContext] instance represents a reference to the "parent" composition in
 * a specific position of that composition's tree, and the instance can then be given to a new
 * "child" composition. This reference ensures that invalidations and [CompositionLocal]s flow
 * logically through the two compositions as if they were not separate.
 *
 * The "parent" of a root composition is a [Recomposer].
 *
 * @see rememberCompositionContext
 */
@OptIn(InternalComposeApi::class, ExperimentalComposeRuntimeApi::class)
public abstract class CompositionContext internal constructor() {
    internal abstract val compositeKeyHashCode: CompositeKeyHashCode
    internal abstract val collectingParameterInformation: Boolean
    internal abstract val collectingSourceInformation: Boolean
    internal abstract val collectingCallByInformation: Boolean
    internal open val observerHolder: CompositionObserverHolder?
        get() = null

    /** The [CoroutineContext] with which effects for the composition will be executed in. */
    public abstract val effectCoroutineContext: CoroutineContext
    internal abstract val recomposeCoroutineContext: CoroutineContext

    /** Associated composition if one exists. */
    internal abstract val composition: Composition?

    internal abstract fun composeInitial(
        composition: ControlledComposition,
        content: @Composable () -> Unit,
    )

    internal abstract fun composeInitialPaused(
        composition: ControlledComposition,
        shouldPause: ShouldPauseCallback,
        content: @Composable () -> Unit,
    ): ScatterSet<RecomposeScopeImpl>

    internal abstract fun recomposePaused(
        composition: ControlledComposition,
        shouldPause: ShouldPauseCallback,
        invalidScopes: ScatterSet<RecomposeScopeImpl>,
    ): ScatterSet<RecomposeScopeImpl>

    internal abstract fun reportPausedScope(scope: RecomposeScopeImpl)

    internal abstract fun invalidate(composition: ControlledComposition)

    internal abstract fun invalidateScope(scope: RecomposeScopeImpl)

    internal open fun recordInspectionTable(table: MutableSet<CompositionData>) {}

    internal open fun registerComposer(composer: Composer) {}

    internal open fun unregisterComposer(composer: Composer) {}

    internal abstract fun registerComposition(composition: ControlledComposition)

    internal abstract fun unregisterComposition(composition: ControlledComposition)

    internal open fun getCompositionLocalScope(): PersistentCompositionLocalMap =
        EmptyPersistentCompositionLocalMap

    internal open fun startComposing() {}

    internal open fun doneComposing() {}

    internal abstract fun insertMovableContent(reference: MovableContentStateReference)

    internal abstract fun deletedMovableContent(reference: MovableContentStateReference)

    internal abstract fun movableContentStateReleased(
        reference: MovableContentStateReference,
        data: MovableContentState,
        applier: Applier<*>,
    )

    internal open fun movableContentStateResolve(
        reference: MovableContentStateReference
    ): MovableContentState? = null

    internal abstract fun reportRemovedComposition(composition: ControlledComposition)

    public abstract fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle
}
