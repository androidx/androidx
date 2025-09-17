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

@file:OptIn(InternalComposeApi::class)

package androidx.compose.runtime

import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.compose.runtime.changelist.ChangeList
import androidx.compose.runtime.collection.ScopeMap
import androidx.compose.runtime.collection.fastForEach
import androidx.compose.runtime.internal.AtomicReference
import androidx.compose.runtime.internal.RememberEventDispatcher
import androidx.compose.runtime.internal.trace
import androidx.compose.runtime.platform.makeSynchronizedObject
import androidx.compose.runtime.platform.synchronized
import androidx.compose.runtime.snapshots.ReaderKind
import androidx.compose.runtime.snapshots.StateObjectImpl
import androidx.compose.runtime.snapshots.fastAll
import androidx.compose.runtime.snapshots.fastAny
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.CompositionObserverHandle
import androidx.compose.runtime.tooling.ObservableComposition
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A composition object is usually constructed for you, and returned from an API that is used to
 * initially compose a UI. For instance, [setContent] returns a Composition.
 *
 * The [dispose] method should be used when you would like to dispose of the UI and the Composition.
 */
public interface Composition {
    /**
     * Returns true if any pending invalidations have been scheduled. An invalidation is schedule if
     * [RecomposeScope.invalidate] has been called on any composition scopes create for the
     * composition.
     *
     * Modifying [MutableState.value] of a value produced by [mutableStateOf] will automatically
     * call [RecomposeScope.invalidate] for any scope that read [State.value] of the mutable state
     * instance during composition.
     *
     * @see RecomposeScope
     * @see mutableStateOf
     */
    public val hasInvalidations: Boolean

    /** True if [dispose] has been called. */
    public val isDisposed: Boolean

    /**
     * Clear the hierarchy that was created from the composition and release resources allocated for
     * composition. After calling [dispose] the composition will no longer be recomposed and calling
     * [setContent] will throw an [IllegalStateException]. Calling [dispose] is idempotent, all
     * calls after the first are a no-op.
     */
    public fun dispose()

    /**
     * Update the composition with the content described by the [content] composable. After this has
     * been called the changes to produce the initial composition has been calculated and applied to
     * the composition.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content A composable function that describes the content of the composition.
     * @exception IllegalStateException thrown in the composition has been [dispose]d.
     */
    public fun setContent(content: @Composable () -> Unit)
}

/**
 * A [ReusableComposition] is a [Composition] that can be reused for different composable content.
 *
 * This interface is used by components that have to synchronize lifecycle of parent and child
 * compositions and efficiently reuse the nodes emitted by [ReusableComposeNode].
 */
public sealed interface ReusableComposition : Composition {
    /**
     * Update the composition with the content described by the [content] composable. After this has
     * been called the changes to produce the initial composition has been calculated and applied to
     * the composition.
     *
     * This method forces this composition into "reusing" state before setting content. In reusing
     * state, all remembered content is discarded, and nodes emitted by [ReusableComposeNode] are
     * re-used for the new content. The nodes are only reused if the group structure containing the
     * node matches new content.
     *
     * Will throw an [IllegalStateException] if the composition has been disposed.
     *
     * @param content A composable function that describes the content of the composition.
     * @exception IllegalStateException thrown in the composition has been [dispose]d.
     */
    public fun setContentWithReuse(content: @Composable () -> Unit)

    /**
     * Deactivate all observation scopes in composition and remove all remembered slots while
     * preserving nodes in place. The composition can be re-activated by calling [setContent] with a
     * new content.
     */
    public fun deactivate()
}

/**
 * A key to locate a service using the [CompositionServices] interface optionally implemented by
 * implementations of [Composition].
 */
public interface CompositionServiceKey<T>

/**
 * Allows finding composition services from the runtime. The services requested through this
 * interface are internal to the runtime and cannot be provided directly.
 *
 * The [CompositionServices] interface is used by the runtime to provide optional and/or
 * experimental services through public extension functions.
 *
 * Implementation of [Composition] that delegate to another [Composition] instance should implement
 * this interface and delegate calls to [getCompositionService] to the original [Composition].
 */
public interface CompositionServices {
    /** Find a service of class [T]. */
    public fun <T> getCompositionService(key: CompositionServiceKey<T>): T?
}

/**
 * Find a Composition service.
 *
 * Find services that implement optional and/or experimental services provided through public or
 * experimental extension functions.
 */
internal fun <T> Composition.getCompositionService(key: CompositionServiceKey<T>) =
    (this as? CompositionServices)?.getCompositionService(key)

/**
 * A controlled composition is a [Composition] that can be directly controlled by the caller.
 *
 * This is the interface used by the [Recomposer] to control how and when a composition is
 * invalidated and subsequently recomposed.
 *
 * Normally a composition is controlled by the [Recomposer] but it is often more efficient for tests
 * to take direct control over a composition by calling [ControlledComposition] instead of
 * [Composition].
 *
 * @see ControlledComposition
 */
public sealed interface ControlledComposition : Composition {
    /**
     * True if the composition is actively compositing such as when actively in a call to
     * [composeContent] or [recompose].
     */
    public val isComposing: Boolean

    /**
     * True after [composeContent] or [recompose] has been called and [applyChanges] is expected as
     * the next call. An exception will be throw in [composeContent] or [recompose] is called while
     * there are pending from the previous composition pending to be applied.
     */
    public val hasPendingChanges: Boolean

    /**
     * Called by the parent composition in response to calling [setContent]. After this method the
     * changes should be calculated but not yet applied. DO NOT call this method directly if this is
     * interface is controlled by a [Recomposer], either use [setContent] or
     * [Recomposer.composeInitial] instead.
     *
     * @param content A composable function that describes the tree.
     */
    public fun composeContent(content: @Composable () -> Unit)

    /**
     * Record the values that were modified after the last call to [recompose] or from the initial
     * call to [composeContent]. This should be called before [recompose] is called to record which
     * parts of the composition need to be recomposed.
     *
     * @param values the set of values that have changed since the last composition.
     */
    public fun recordModificationsOf(values: Set<Any>)

    /**
     * Returns true if any of the object instances in [values] is observed by this composition. This
     * allows detecting if values changed by a previous composition will potentially affect this
     * composition.
     */
    public fun observesAnyOf(values: Set<Any>): Boolean

    /**
     * Execute [block] with [isComposing] set temporarily to `true`. This allows treating
     * invalidations reported during [prepareCompose] as if they happened while composing to avoid
     * double invalidations when propagating changes from a parent composition while before
     * composing the child composition.
     */
    public fun prepareCompose(block: () -> Unit)

    /**
     * Record that [value] has been read. This is used primarily by the [Recomposer] to inform the
     * composer when the a [MutableState] instance has been read implying it should be observed for
     * changes.
     *
     * @param value the instance from which a property was read
     */
    public fun recordReadOf(value: Any)

    /**
     * Record that [value] has been modified. This is used primarily by the [Recomposer] to inform
     * the composer when the a [MutableState] instance been change by a composable function.
     */
    public fun recordWriteOf(value: Any)

    /**
     * Recompose the composition to calculate any changes necessary to the composition state and the
     * tree maintained by the applier. No changes have been made yet. Changes calculated will be
     * applied when [applyChanges] is called.
     *
     * @return returns `true` if any changes are pending and [applyChanges] should be called.
     */
    public fun recompose(): Boolean

    /**
     * Insert the given list of movable content with their paired state in potentially a different
     * composition. If the second part of the pair is null then the movable content should be
     * inserted as new. If second part of the pair has a value then the state should be moved into
     * the referenced location and then recomposed there.
     */
    @InternalComposeApi
    public fun insertMovableContent(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    )

    /** Dispose the value state that is no longer needed. */
    @InternalComposeApi public fun disposeUnusedMovableContent(state: MovableContentState)

    /**
     * Apply the changes calculated during [setContent] or [recompose]. If an exception is thrown by
     * [applyChanges] the composition is irreparably damaged and should be [dispose]d.
     */
    public fun applyChanges()

    /**
     * Apply change that must occur after the main bulk of changes have been applied. Late changes
     * are the result of inserting movable content and it must be performed after [applyChanges]
     * because, for content that have moved must be inserted only after it has been removed from the
     * previous location. All deletes must be executed before inserts. To ensure this, all deletes
     * are performed in [applyChanges] and all inserts are performed in [applyLateChanges].
     */
    public fun applyLateChanges()

    /**
     * Call when all changes, including late changes, have been applied. This signals to the
     * composition that any transitory composition state can now be discarded. This is advisory only
     * and a controlled composition will execute correctly when this is not called.
     */
    public fun changesApplied()

    /**
     * Abandon current changes and reset composition state. Called when recomposer cannot proceed
     * with current recomposition loop and needs to reset composition.
     */
    public fun abandonChanges()

    /**
     * Invalidate all invalidation scopes. This is called, for example, by [Recomposer] when the
     * Recomposer becomes active after a previous period of inactivity, potentially missing more
     * granular invalidations.
     */
    public fun invalidateAll()

    /**
     * Throws an exception if the internal state of the composer has been corrupted and is no longer
     * consistent. Used in testing the composer itself.
     */
    @InternalComposeApi public fun verifyConsistent()

    /**
     * Temporarily delegate all invalidations sent to this composition to the [to] composition. This
     * is used when movable content moves between compositions. The recompose scopes are not
     * redirected until after the move occurs during [applyChanges] and [applyLateChanges]. This is
     * used to compose as if the scopes have already been changed.
     */
    public fun <R> delegateInvalidations(
        to: ControlledComposition?,
        groupIndex: Int,
        block: () -> R,
    ): R

    /**
     * Sets the [shouldPause] callback allowing a composition to be pausable if it is not `null`.
     * Setting the callback to `null` disables pausing.
     *
     * @return the previous value of the callback which will be restored once the callback is no
     *   longer needed.
     * @see PausableComposition
     */
    @Suppress("ExecutorRegistration")
    public fun getAndSetShouldPauseCallback(shouldPause: ShouldPauseCallback?): ShouldPauseCallback?
}

/** Utility function to set and restore a should pause callback. */
internal inline fun <R> ControlledComposition.pausable(
    shouldPause: ShouldPauseCallback,
    block: () -> R,
): R {
    val previous = getAndSetShouldPauseCallback(shouldPause)
    return try {
        block()
    } finally {
        getAndSetShouldPauseCallback(previous)
    }
}

/**
 * The [CoroutineContext] that should be used to perform concurrent recompositions of this
 * [ControlledComposition] when used in an environment supporting concurrent composition.
 *
 * See [Recomposer.runRecomposeConcurrentlyAndApplyChanges] as an example of configuring such an
 * environment.
 */
// Implementation note: as/if this method graduates it should become a real method of
// ControlledComposition with a default implementation.
@ExperimentalComposeApi
public val ControlledComposition.recomposeCoroutineContext: CoroutineContext
    get() = (this as? CompositionImpl)?.recomposeContext ?: EmptyCoroutineContext

/**
 * This method is the way to initiate a composition. [parent] [CompositionContext] can be
 * * provided to make the composition behave as a sub-composition of the parent. If composition does
 * * not have a parent, [Recomposer] instance should be provided.
 *
 * It is important to call [Composition.dispose] when composition is no longer needed in order to
 * release resources.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent [CompositionContext].
 * @see Applier
 * @see Composition
 * @see Recomposer
 */
public fun Composition(applier: Applier<*>, parent: CompositionContext): Composition =
    CompositionImpl(parent, applier)

/**
 * This method is the way to initiate a reusable composition. [parent] [CompositionContext] can be
 * provided to make the composition behave as a sub-composition of the parent. If composition does
 * not have a parent, [Recomposer] instance should be provided.
 *
 * It is important to call [Composition.dispose] when composition is no longer needed in order to
 * release resources.
 *
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent [CompositionContext].
 * @see Applier
 * @see ReusableComposition
 * @see rememberCompositionContext
 */
public fun ReusableComposition(
    applier: Applier<*>,
    parent: CompositionContext,
): ReusableComposition = CompositionImpl(parent, applier)

/**
 * This method is a way to initiate a composition. Optionally, a [parent] [CompositionContext] can
 * be provided to make the composition behave as a sub-composition of the parent or a [Recomposer]
 * can be provided.
 *
 * A controlled composition allows direct control of the composition instead of it being controlled
 * by the [Recomposer] passed ot the root composition.
 *
 * It is important to call [Composition.dispose] this composer is no longer needed in order to
 * release resources.
 *
 * @sample androidx.compose.runtime.samples.CustomTreeComposition
 * @param applier The [Applier] instance to be used in the composition.
 * @param parent The parent [CompositionContext].
 * @see Applier
 * @see Composition
 * @see Recomposer
 */
@TestOnly
public fun ControlledComposition(
    applier: Applier<*>,
    parent: CompositionContext,
): ControlledComposition = CompositionImpl(parent, applier)

/**
 * Create a [Composition] using [applier] to manage the composition, as a child of [parent].
 *
 * When used in a configuration that supports concurrent recomposition, hint to the environment that
 * [recomposeCoroutineContext] should be used to perform recomposition. Recompositions will be
 * launched into the
 */
@ExperimentalComposeApi
public fun Composition(
    applier: Applier<*>,
    parent: CompositionContext,
    recomposeCoroutineContext: CoroutineContext,
): Composition = CompositionImpl(parent, applier, recomposeContext = recomposeCoroutineContext)

@TestOnly
@ExperimentalComposeApi
public fun ControlledComposition(
    applier: Applier<*>,
    parent: CompositionContext,
    recomposeCoroutineContext: CoroutineContext,
): ControlledComposition =
    CompositionImpl(parent, applier, recomposeContext = recomposeCoroutineContext)

private val PendingApplyNoModifications = Any()

@OptIn(ExperimentalComposeRuntimeApi::class)
internal val ObservableCompositionServiceKey =
    object : CompositionServiceKey<ObservableComposition> {}

private const val RUNNING = 0
private const val DEACTIVATED = 1
private const val INCONSISTENT = 2
private const val DISPOSED = 3

/**
 * The implementation of the [Composition] interface.
 *
 * @param parent An optional reference to the parent composition.
 * @param applier The applier to use to manage the tree built by the composer.
 * @param recomposeContext The coroutine context to use to recompose this composition. If left
 *   `null` the controlling recomposer's default context is used.
 */
@OptIn(ExperimentalComposeRuntimeApi::class)
internal class CompositionImpl(
    /**
     * The parent composition from [rememberCompositionContext], for sub-compositions, or the an
     * instance of [Recomposer] for root compositions.
     */
    @get:TestOnly val parent: CompositionContext,

    /** The applier to use to update the tree managed by the composition. */
    private val applier: Applier<*>,
    recomposeContext: CoroutineContext? = null,
) :
    ControlledComposition,
    ReusableComposition,
    RecomposeScopeOwner,
    CompositionServices,
    PausableComposition,
    ObservableComposition {

    /**
     * `null` if a composition isn't pending to apply. `Set<Any>` or `Array<Set<Any>>` if there are
     * modifications to record [PendingApplyNoModifications] if a composition is pending to apply,
     * no modifications. any set contents will be sent to [recordModificationsOf] after applying
     * changes before releasing [lock]
     */
    private val pendingModifications = AtomicReference<Any?>(null)

    // Held when making changes to self or composer
    private val lock = makeSynchronizedObject()

    /**
     * A set of remember observers that were potentially abandoned between [composeContent] or
     * [recompose] and [applyChanges]. When inserting new content any newly remembered
     * [RememberObserver]s are added to this set and then removed as [RememberObserver.onRemembered]
     * is dispatched. If any are left in this when exiting [applyChanges] they have been abandoned
     * and are sent an [RememberObserver.onAbandoned] notification.
     */
    @Suppress("AsCollectionCall") // Requires iterator API when dispatching abandons
    private val abandonSet = MutableScatterSet<RememberObserver>().asMutableSet()

    /** The slot table is used to store the composition information required for recomposition. */
    @Suppress("MemberVisibilityCanBePrivate") // published as internal
    internal val slotTable =
        SlotTable().also {
            if (parent.collectingCallByInformation) it.collectCalledByInformation()
            if (parent.collectingSourceInformation) it.collectSourceInformation()
        }

    /**
     * A map of observable objects to the [RecomposeScope]s that observe the object. If the key
     * object is modified the associated scopes should be invalidated.
     */
    private val observations = ScopeMap<Any, RecomposeScopeImpl>()

    /** Used for testing. Returns the objects that are observed */
    internal val observedObjects
        @TestOnly @Suppress("AsCollectionCall") get() = observations.map.asMap().keys

    /**
     * A set of scopes that were invalidated by a call from [recordModificationsOf]. This set is
     * only used in [addPendingInvalidationsLocked], and is reused between invocations.
     */
    private val invalidatedScopes = MutableScatterSet<RecomposeScopeImpl>()

    /**
     * A set of scopes that were invalidated conditionally (that is they were invalidated by a
     * [derivedStateOf] object) by a call from [recordModificationsOf]. They need to be held in the
     * [observations] map until invalidations are drained for composition as a later call to
     * [recordModificationsOf] might later cause them to be unconditionally invalidated.
     */
    private val conditionallyInvalidatedScopes = MutableScatterSet<RecomposeScopeImpl>()

    /** A map of object read during derived states to the corresponding derived state. */
    private val derivedStates = ScopeMap<Any, DerivedState<*>>()

    /** Used for testing. Returns dependencies of derived states that are currently observed. */
    internal val derivedStateDependencies
        @TestOnly @Suppress("AsCollectionCall") get() = derivedStates.map.asMap().keys

    /** Used for testing. Returns the conditional scopes being tracked by the composer */
    internal val conditionalScopes: List<RecomposeScopeImpl>
        @TestOnly
        @Suppress("AsCollectionCall")
        get() = conditionallyInvalidatedScopes.asSet().toList()

    /**
     * A list of changes calculated by [Composer] to be applied to the [Applier] and the [SlotTable]
     * to reflect the result of composition. This is a list of lambdas that need to be invoked in
     * order to produce the desired effects.
     */
    private val changes = ChangeList()

    /**
     * A list of changes calculated by [Composer] to be applied after all other compositions have
     * had [applyChanges] called. These changes move [MovableContent] state from one composition to
     * another and must be applied after [applyChanges] because [applyChanges] copies and removes
     * the state out of the previous composition so it can be inserted into the new location. As
     * inserts might be earlier in the composition than the position it is deleted, this move must
     * be done in two phases.
     */
    private val lateChanges = ChangeList()

    /**
     * When an observable object is modified during composition any recompose scopes that are
     * observing that object are invalidated immediately. Since they have already been processed
     * there is no need to process them again, so this set maintains a set of the recompose scopes
     * that were already dismissed by composition and should be ignored in the next call to
     * [recordModificationsOf].
     */
    private val observationsProcessed = ScopeMap<Any, RecomposeScopeImpl>()

    /**
     * A map of the invalid [RecomposeScope]s. If this map is non-empty the current state of the
     * composition does not reflect the current state of the objects it observes and should be
     * recomposed by calling [recompose]. Tbe value is a map of values that invalidated the scope.
     * The scope is checked with these instances to ensure the value has changed. This is used to
     * only invalidate the scope if a [derivedStateOf] object changes.
     */
    private var invalidations = ScopeMap<RecomposeScopeImpl, Any>()

    /**
     * As [RecomposeScope]s are removed the corresponding entries in the observations set must be
     * removed as well. This process is expensive so should only be done if it is certain the
     * [observations] set contains [RecomposeScope] that is no longer needed. [pendingInvalidScopes]
     * is set to true whenever a [RecomposeScope] is removed from the [slotTable].
     */
    @Suppress("MemberVisibilityCanBePrivate") // published as internal
    internal var pendingInvalidScopes = false

    /**
     * If the [shouldPause] callback is set the composition is pausable and should pause whenever
     * the [shouldPause] callback returns `true`.
     */
    private var shouldPause: ShouldPauseCallback? = null

    private var pendingPausedComposition: PausedCompositionImpl? = null

    private var invalidationDelegate: CompositionImpl? = null

    private var invalidationDelegateGroup: Int = 0

    internal val observerHolder = CompositionObserverHolder(parent = parent)

    private val rememberManager = RememberEventDispatcher()

    /** The [Composer] to use to create and update the tree managed by this composition. */
    internal val composer: ComposerImpl =
        ComposerImpl(
                applier = applier,
                parentContext = parent,
                slotTable = slotTable,
                abandonSet = abandonSet,
                changes = changes,
                lateChanges = lateChanges,
                composition = this,
                observerHolder = observerHolder,
            )
            .also { parent.registerComposer(it) }

    /** The [CoroutineContext] override, if there is one, for this composition. */
    private val _recomposeContext: CoroutineContext? = recomposeContext

    /** the [CoroutineContext] to use to [recompose] this composition. */
    val recomposeContext: CoroutineContext
        get() = _recomposeContext ?: parent.recomposeCoroutineContext

    /** Return true if this is a root (non-sub-) composition. */
    val isRoot: Boolean = parent is Recomposer

    /** True if [dispose] has been called. */
    private var state = RUNNING

    /** True if a sub-composition of this composition is current composing. */
    private val areChildrenComposing
        get() = composer.areChildrenComposing

    /**
     * The [Composable] function used to define the tree managed by this composition. This is set by
     * [setContent].
     */
    var composable: @Composable () -> Unit = {}

    override val isComposing: Boolean
        get() = composer.isComposing

    override val isDisposed: Boolean
        get() = state == DISPOSED

    override val hasPendingChanges: Boolean
        get() = synchronized(lock) { composer.hasPendingChanges }

    override fun setContent(content: @Composable () -> Unit) {
        val wasDeactivated = clearDeactivated()
        ensureRunning()

        if (wasDeactivated) {
            composeInitialWithReuse(content)
        } else {
            composeInitial(content)
        }
    }

    override fun setContentWithReuse(content: @Composable () -> Unit) {
        clearDeactivated()
        ensureRunning()

        composeInitialWithReuse(content)
    }

    override fun setPausableContent(content: @Composable () -> Unit): PausedComposition {
        val wasDeactivated = clearDeactivated()
        return composeInitialPaused(reusable = wasDeactivated, content)
    }

    override fun setPausableContentWithReuse(content: @Composable () -> Unit): PausedComposition {
        clearDeactivated()
        ensureRunning()

        return composeInitialPaused(reusable = true, content)
    }

    internal fun pausedCompositionFinished(ignoreSet: ScatterSet<RememberObserverHolder>?) {
        pendingPausedComposition = null
        if (ignoreSet != null) {
            rememberManager.ignoreForgotten(ignoreSet)
            state = INCONSISTENT
        }
    }

    private fun composeInitial(content: @Composable () -> Unit) {
        this.composable = content
        parent.composeInitial(this, composable)
    }

    private fun composeInitialPaused(
        reusable: Boolean,
        content: @Composable () -> Unit,
    ): PausedComposition {
        checkPrecondition(pendingPausedComposition == null) {
            "A pausable composition is in progress"
        }
        val pausedComposition =
            PausedCompositionImpl(
                composition = this,
                context = parent,
                composer = composer,
                content = content,
                reusable = reusable,
                abandonSet = abandonSet,
                applier = applier,
                lock = lock,
            )
        pendingPausedComposition = pausedComposition
        return pausedComposition
    }

    private fun composeInitialWithReuse(content: @Composable () -> Unit) {
        composer.startReuseFromRoot()
        composeInitial(content)
        composer.endReuseFromRoot()
    }

    private fun ensureRunning() {
        checkPrecondition(state == RUNNING) {
            when (state) {
                INCONSISTENT ->
                    "A previous pausable composition for this composition was cancelled. This " +
                        "composition must be disposed."
                DISPOSED -> "The composition is disposed"
                DEACTIVATED -> "The composition should be activated before setting content."
                else -> "" // Excluded by the precondition check
            }
        }
        checkPrecondition(pendingPausedComposition == null) {
            "A pausable composition is in progress"
        }
    }

    private fun clearDeactivated(): Boolean =
        synchronized(lock) {
            val isDeactivated = state == DEACTIVATED
            if (isDeactivated) {
                state = RUNNING
            }
            isDeactivated
        }

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun setObserver(observer: CompositionObserver): CompositionObserverHandle {
        synchronized(lock) {
            observerHolder.observer = observer
            observerHolder.root = true
        }
        return object : CompositionObserverHandle {
            override fun dispose() {
                synchronized(lock) {
                    if (observerHolder.observer == observer) {
                        observerHolder.observer = null
                        observerHolder.root = false
                    }
                }
            }
        }
    }

    fun invalidateGroupsWithKey(key: Int) {
        val scopesToInvalidate = synchronized(lock) { slotTable.invalidateGroupsWithKey(key) }
        // Calls to invalidate must be performed without the lock as the they may cause the
        // recomposer to take its lock to respond to the invalidation and that takes the locks
        // in the opposite order of composition so if composition begins in another thread taking
        // the recomposer lock with the composer lock held will deadlock.
        val forceComposition =
            scopesToInvalidate == null ||
                scopesToInvalidate.fastAny {
                    it.invalidateForResult(null) == InvalidationResult.IGNORED
                }
        if (forceComposition && composer.forceRecomposeScopes()) {
            parent.invalidate(this)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun drainPendingModificationsForCompositionLocked() {
        // Recording modifications may race for lock. If there are pending modifications
        // and we won the lock race, drain them before composing.
        when (val toRecord = pendingModifications.getAndSet(PendingApplyNoModifications)) {
            null -> {
                // Do nothing, just start composing.
            }
            PendingApplyNoModifications -> {
                composeRuntimeError("pending composition has not been applied")
            }
            is Set<*> -> {
                addPendingInvalidationsLocked(toRecord as Set<Any>, forgetConditionalScopes = true)
            }
            is Array<*> ->
                for (changed in toRecord as Array<Set<Any>>) {
                    addPendingInvalidationsLocked(changed, forgetConditionalScopes = true)
                }
            else -> composeRuntimeError("corrupt pendingModifications drain: $pendingModifications")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun drainPendingModificationsLocked() {
        when (val toRecord = pendingModifications.getAndSet(null)) {
            PendingApplyNoModifications -> {
                // No work to do
            }
            is Set<*> -> {
                addPendingInvalidationsLocked(toRecord as Set<Any>, forgetConditionalScopes = false)
            }
            is Array<*> ->
                for (changed in toRecord as Array<Set<Any>>) {
                    addPendingInvalidationsLocked(changed, forgetConditionalScopes = false)
                }
            null -> {
                if (pendingPausedComposition == null)
                    composeImmediateRuntimeError(
                        "calling recordModificationsOf and applyChanges concurrently is not supported"
                    )
                // otherwise, the paused composition may be being resumed concurrently.
            }
            else -> composeRuntimeError("corrupt pendingModifications drain: $pendingModifications")
        }
    }

    // Drain the modification out of the normal recordModificationsOf(), composition() cycle.
    // This avoids the checks to make sure the two calls are called in order.
    @Suppress("UNCHECKED_CAST")
    private fun drainPendingModificationsOutOfBandLocked() {
        when (val toRecord = pendingModifications.getAndSet(emptySet<Any>())) {
            PendingApplyNoModifications,
            null -> {
                // No work to do
            }
            is Set<*> -> {
                addPendingInvalidationsLocked(toRecord as Set<Any>, forgetConditionalScopes = false)
            }
            is Array<*> ->
                for (changed in toRecord as Array<Set<Any>>) {
                    addPendingInvalidationsLocked(changed, forgetConditionalScopes = false)
                }
            else -> composeRuntimeError("corrupt pendingModifications drain: $pendingModifications")
        }
    }

    override fun composeContent(content: @Composable () -> Unit) {
        // TODO: This should raise a signal to any currently running recompose calls
        //   to halt and return
        guardChanges {
            synchronized(lock) {
                drainPendingModificationsForCompositionLocked()
                guardInvalidationsLocked { invalidations ->
                    composer.composeContent(invalidations, content, shouldPause)
                }
            }
        }
    }

    internal fun updateMovingInvalidations() {
        synchronized(lock) {
            drainPendingModificationsOutOfBandLocked()
            guardInvalidationsLocked { invalidations ->
                composer.updateComposerInvalidations(invalidations)
            }
        }
    }

    override fun dispose() {
        synchronized(lock) {
            checkPrecondition(!composer.isComposing) {
                "Composition is disposed while composing. If dispose is triggered by a call in " +
                    "@Composable function, consider wrapping it with SideEffect block."
            }
            if (state != DISPOSED) {
                state = DISPOSED
                composable = {}

                // Changes are deferred if the composition contains movable content that needs
                // to be released. NOTE: Applying these changes leaves the slot table in
                // potentially invalid state. The routine use to produce this change list reuses
                // code that extracts movable content from groups that are being deleted. This code
                // does not bother to correctly maintain the node counts of a group nested groups
                // that are going to be removed anyway so the node counts of the groups affected
                // are might be incorrect after the changes have been applied.
                val deferredChanges = composer.deferredChanges
                if (deferredChanges != null) {
                    applyChangesInLocked(deferredChanges)
                }

                // Dispatch all the `onForgotten` events for object that are no longer part of a
                // composition because this composition is being discarded. It is important that
                // this is done after applying deferred changes above to avoid sending `
                // onForgotten` notification to objects that are still part of movable content that
                // will be moved to a new location.
                val nonEmptySlotTable = slotTable.groupsSize > 0
                if (nonEmptySlotTable || abandonSet.isNotEmpty()) {
                    rememberManager.use(abandonSet, composer.errorContext) {
                        if (nonEmptySlotTable) {
                            applier.onBeginChanges()
                            slotTable.write { writer -> writer.removeCurrentGroup(rememberManager) }
                            applier.clear()
                            applier.onEndChanges()
                            dispatchRememberObservers()
                        }
                        dispatchAbandons()
                    }
                }
                composer.dispose()
            }
        }
        parent.unregisterComposition(this)
    }

    override val hasInvalidations
        get() = synchronized(lock) { invalidations.size > 0 }

    /**
     * To bootstrap multithreading handling, recording modifications is now deferred between
     * recomposition with changes to apply and the application of those changes.
     * [pendingModifications] will contain a queue of changes to apply once all current changes have
     * been successfully processed. Draining this queue is the responsibility of [recompose] if it
     * would return `false` (changes do not need to be applied) or [applyChanges].
     */
    @Suppress("UNCHECKED_CAST")
    override fun recordModificationsOf(values: Set<Any>) {
        while (true) {
            val old = pendingModifications.get()
            val new: Any =
                when (old) {
                    null,
                    PendingApplyNoModifications -> values
                    is Set<*> -> arrayOf(old, values)
                    is Array<*> -> (old as Array<Set<Any>>) + values
                    else -> error("corrupt pendingModifications: $pendingModifications")
                }
            if (pendingModifications.compareAndSet(old, new)) {
                if (old == null) {
                    synchronized(lock) { drainPendingModificationsLocked() }
                }
                break
            }
        }
    }

    override fun observesAnyOf(values: Set<Any>): Boolean {
        values.fastForEach { value ->
            if (value in observations || value in derivedStates) return true
        }
        return false
    }

    override fun prepareCompose(block: () -> Unit) = composer.prepareCompose(block)

    /**
     * Extract the invalidations that are in the group with the given marker. This is used when
     * movable content is moved between tables and the content was invalidated. This is used to move
     * the invalidations with the content.
     */
    internal fun extractInvalidationsOf(anchor: Anchor): List<Pair<RecomposeScopeImpl, Any>> {
        return if (invalidations.size > 0) {
            val result = mutableListOf<Pair<RecomposeScopeImpl, Any>>()
            val slotTable = slotTable
            invalidations.removeIf { scope, value ->
                val scopeAnchor = scope.anchor
                if (scopeAnchor != null && slotTable.inGroup(anchor, scopeAnchor)) {
                    result.add(scope to value)

                    // Remove the invalidation
                    true
                } else {
                    // Keep the invalidation
                    false
                }
            }
            result
        } else emptyList()
    }

    private fun addPendingInvalidationsLocked(value: Any, forgetConditionalScopes: Boolean) {
        observations.forEachScopeOf(value) { scope ->
            if (
                !observationsProcessed.remove(value, scope) &&
                    scope.invalidateForResult(value) != InvalidationResult.IGNORED
            ) {
                if (scope.isConditional && !forgetConditionalScopes) {
                    conditionallyInvalidatedScopes.add(scope)
                } else {
                    invalidatedScopes.add(scope)
                }
            }
        }
    }

    private fun addPendingInvalidationsLocked(values: Set<Any>, forgetConditionalScopes: Boolean) {
        values.fastForEach { value ->
            if (value is RecomposeScopeImpl) {
                value.invalidateForResult(null)
            } else {
                addPendingInvalidationsLocked(value, forgetConditionalScopes)
                derivedStates.forEachScopeOf(value) {
                    addPendingInvalidationsLocked(it, forgetConditionalScopes)
                }
            }
        }

        val conditionallyInvalidatedScopes = conditionallyInvalidatedScopes
        val invalidatedScopes = invalidatedScopes
        if (forgetConditionalScopes && conditionallyInvalidatedScopes.isNotEmpty()) {
            observations.removeScopeIf { scope ->
                scope in conditionallyInvalidatedScopes || scope in invalidatedScopes
            }
            conditionallyInvalidatedScopes.clear()
            cleanUpDerivedStateObservations()
        } else if (invalidatedScopes.isNotEmpty()) {
            observations.removeScopeIf { scope -> scope in invalidatedScopes }
            cleanUpDerivedStateObservations()
            invalidatedScopes.clear()
        }
    }

    private fun cleanUpDerivedStateObservations() {
        derivedStates.removeScopeIf { derivedState -> derivedState !in observations }
        if (conditionallyInvalidatedScopes.isNotEmpty()) {
            conditionallyInvalidatedScopes.removeIf { scope -> !scope.isConditional }
        }
    }

    override fun recordReadOf(value: Any) {
        // Not acquiring lock since this happens during composition with it already held
        if (!areChildrenComposing) {
            composer.currentRecomposeScope?.let { scope ->
                scope.used = true

                val alreadyRead = scope.recordRead(value)

                observer()?.onReadInScope(scope, value)

                if (!alreadyRead) {
                    if (value is StateObjectImpl) {
                        value.recordReadIn(ReaderKind.Composition)
                    }

                    observations.add(value, scope)

                    // Record derived state dependency mapping
                    if (value is DerivedState<*>) {
                        val record = value.currentRecord
                        derivedStates.removeScope(value)
                        record.dependencies.forEachKey { dependency ->
                            if (dependency is StateObjectImpl) {
                                dependency.recordReadIn(ReaderKind.Composition)
                            }
                            derivedStates.add(dependency, value)
                        }
                        scope.recordDerivedStateValue(value, record.currentValue)
                    }
                }
            }
        }
    }

    private fun invalidateScopeOfLocked(value: Any) {
        // Invalidate any recompose scopes that read this value.
        observations.forEachScopeOf(value) { scope ->
            if (scope.invalidateForResult(value) == InvalidationResult.IMMINENT) {
                // If we process this during recordWriteOf, ignore it when recording modifications
                observationsProcessed.add(value, scope)
            }
        }
    }

    override fun recordWriteOf(value: Any) =
        synchronized(lock) {
            invalidateScopeOfLocked(value)

            // If writing to dependency of a derived value and the value is changed, invalidate the
            // scopes that read the derived value.
            derivedStates.forEachScopeOf(value) { invalidateScopeOfLocked(it) }
        }

    override fun recompose(): Boolean =
        synchronized(lock) {
            val pendingPausedComposition = pendingPausedComposition
            if (pendingPausedComposition != null && !pendingPausedComposition.isRecomposing) {
                // If the composition is pending do not recompose it now as the recomposition
                // is in the control of the pausable composition and is supposed to happen when
                // the resume is called. However, this may cause the pausable composition to go
                // revert to an incomplete state. If isRecomposing is true then this is being
                // called in resume()
                pendingPausedComposition.markIncomplete()
                pendingPausedComposition.pausableApplier.markRecomposePending()
                return false
            }
            drainPendingModificationsForCompositionLocked()
            guardChanges {
                guardInvalidationsLocked { invalidations ->
                    composer.recompose(invalidations, shouldPause).also { shouldDrain ->
                        // Apply would normally do this for us; do it now if apply shouldn't happen.
                        if (!shouldDrain) drainPendingModificationsLocked()
                    }
                }
            }
        }

    override fun insertMovableContent(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    ) {
        runtimeCheck(references.fastAll { it.first.composition == this })
        guardChanges { composer.insertMovableContentReferences(references) }
    }

    override fun disposeUnusedMovableContent(state: MovableContentState) {
        rememberManager.use(abandonSet, composer.errorContext) {
            val slotTable = state.slotTable
            slotTable.write { writer -> writer.removeCurrentGroup(rememberManager) }
            dispatchRememberObservers()
        }
    }

    private fun applyChangesInLocked(changes: ChangeList) {
        rememberManager.prepare(abandonSet, composer.errorContext)
        try {
            if (changes.isEmpty()) return
            val applier = pendingPausedComposition?.pausableApplier ?: applier
            val traceName =
                if (applier == pendingPausedComposition?.pausableApplier) {
                    "Compose:recordChanges"
                } else {
                    "Compose:applyChanges"
                }
            trace(traceName) {
                val rememberManager = pendingPausedComposition?.rememberManager ?: rememberManager
                applier.onBeginChanges()

                // Apply all changes
                slotTable.write { slots ->
                    changes.executeAndFlushAllPendingChanges(
                        applier,
                        slots,
                        rememberManager,
                        composer.errorContext,
                    )
                }
                applier.onEndChanges()
            }

            // Side effects run after lifecycle observers so that any remembered objects
            // that implement RememberObserver receive onRemembered before a side effect
            // that captured it and operates on it can run.
            rememberManager.dispatchRememberObservers()
            rememberManager.dispatchSideEffects()

            if (pendingInvalidScopes) {
                trace("Compose:unobserve") {
                    pendingInvalidScopes = false
                    observations.removeScopeIf { scope -> !scope.valid }
                    cleanUpDerivedStateObservations()
                }
            }
        } finally {
            // Only dispatch abandons if we do not have any late changes or pending paused
            // compositions. The instances in the abandon set can be remembered in the late changes
            // or when the paused composition is applied.
            try {
                if (this.lateChanges.isEmpty() && pendingPausedComposition == null) {
                    rememberManager.dispatchAbandons()
                }
            } finally {
                rememberManager.clear()
            }
        }
    }

    override fun applyChanges() {
        synchronized(lock) {
            guardChanges {
                applyChangesInLocked(changes)
                drainPendingModificationsLocked()
            }
        }
    }

    override fun applyLateChanges() {
        synchronized(lock) {
            guardChanges {
                if (lateChanges.isNotEmpty()) {
                    applyChangesInLocked(lateChanges)
                }
            }
        }
    }

    override fun changesApplied() {
        synchronized(lock) {
            guardChanges {
                composer.changesApplied()

                // By this time all abandon objects should be notified that they have been
                // abandoned.
                if (this.abandonSet.isNotEmpty()) {
                    rememberManager.use(abandonSet, traceContext = composer.errorContext) {
                        dispatchAbandons()
                    }
                }
            }
        }
    }

    private inline fun <T> guardInvalidationsLocked(
        block: (changes: ScopeMap<RecomposeScopeImpl, Any>) -> T
    ): T {
        val invalidations = takeInvalidations()
        return try {
            block(invalidations)
        } catch (e: Throwable) {
            this.invalidations = invalidations
            throw e
        }
    }

    private inline fun <T> guardChanges(block: () -> T): T =
        try {
            trackAbandonedValues(block)
        } catch (e: Throwable) {
            abandonChanges()
            throw e
        }

    override fun abandonChanges() {
        pendingModifications.set(null)
        changes.clear()
        lateChanges.clear()

        if (abandonSet.isNotEmpty()) {
            rememberManager.use(abandonSet, composer.errorContext) { dispatchAbandons() }
        }
    }

    override fun invalidateAll() {
        synchronized(lock) { slotTable.slots.forEach { (it as? RecomposeScopeImpl)?.invalidate() } }
    }

    override fun verifyConsistent() {
        synchronized(lock) {
            if (!isComposing) {
                composer.verifyConsistent()
                slotTable.verifyWellFormed()
                validateRecomposeScopeAnchors(slotTable)
            }
        }
    }

    override fun <R> delegateInvalidations(
        to: ControlledComposition?,
        groupIndex: Int,
        block: () -> R,
    ): R {
        return if (to != null && to != this && groupIndex >= 0) {
            invalidationDelegate = to as CompositionImpl
            invalidationDelegateGroup = groupIndex
            try {
                block()
            } finally {
                invalidationDelegate = null
                invalidationDelegateGroup = 0
            }
        } else block()
    }

    override fun getAndSetShouldPauseCallback(
        shouldPause: ShouldPauseCallback?
    ): ShouldPauseCallback? {
        val previous = this.shouldPause
        this.shouldPause = shouldPause
        return previous
    }

    override fun invalidate(scope: RecomposeScopeImpl, instance: Any?): InvalidationResult {
        if (scope.defaultsInScope) {
            scope.defaultsInvalid = true
        }
        val anchor = scope.anchor
        if (anchor == null || !anchor.valid)
            return InvalidationResult.IGNORED // The scope was removed from the composition
        if (!slotTable.ownsAnchor(anchor)) {
            // The scope might be owned by the delegate
            val delegate = synchronized(lock) { invalidationDelegate }
            if (delegate?.tryImminentInvalidation(scope, instance) == true)
                return InvalidationResult.IMMINENT // The scope was owned by the delegate

            return InvalidationResult.IGNORED // The scope has not yet entered the composition
        }
        if (!scope.canRecompose)
            return InvalidationResult.IGNORED // The scope isn't able to be recomposed/invalidated
        return invalidateChecked(scope, anchor, instance).also {
            if (it != InvalidationResult.IGNORED) {
                observer()?.onScopeInvalidated(scope, instance)
            }
        }
    }

    override fun recomposeScopeReleased(scope: RecomposeScopeImpl) {
        pendingInvalidScopes = true

        observer()?.onScopeDisposed(scope)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getCompositionService(key: CompositionServiceKey<T>): T? =
        if (key == ObservableCompositionServiceKey) this as T else null

    private fun tryImminentInvalidation(scope: RecomposeScopeImpl, instance: Any?): Boolean =
        isComposing && composer.tryImminentInvalidation(scope, instance)

    private fun invalidateChecked(
        scope: RecomposeScopeImpl,
        anchor: Anchor,
        instance: Any?,
    ): InvalidationResult {
        val delegate =
            synchronized(lock) {
                val delegate =
                    invalidationDelegate?.let { changeDelegate ->
                        // Invalidations are delegated when recomposing changes to movable content
                        // that is destined to be moved. The movable content is composed in the
                        // destination composer but all the recompose scopes point the current
                        // composer and will arrive here. this redirects the invalidations that
                        // will be moved to the destination composer instead of recording an
                        // invalid invalidation in the from composer.
                        if (slotTable.groupContainsAnchor(invalidationDelegateGroup, anchor)) {
                            changeDelegate
                        } else null
                    }
                if (delegate == null) {
                    if (tryImminentInvalidation(scope, instance)) {
                        // The invalidation was redirected to the composer.
                        return InvalidationResult.IMMINENT
                    }

                    // Observer requires a map of scope -> states, so we have to fill it if observer
                    // is set.
                    if (instance == null) {
                        // invalidations[scope] containing ScopeInvalidated means it was invalidated
                        // unconditionally.
                        invalidations.set(scope, ScopeInvalidated)
                    } else if (instance !is DerivedState<*>) {
                        // If observer is not set, we only need to add derived states to
                        // invalidation, as regular states are always going to invalidate.
                        invalidations.set(scope, ScopeInvalidated)
                    } else {
                        if (!invalidations.anyScopeOf(scope) { it === ScopeInvalidated }) {
                            invalidations.add(scope, instance)
                        }
                    }
                }
                delegate
            }

        // We call through the delegate here to ensure we don't nest synchronization scopes.
        if (delegate != null) {
            return delegate.invalidateChecked(scope, anchor, instance)
        }
        parent.invalidate(this)
        return if (isComposing) InvalidationResult.DEFERRED else InvalidationResult.SCHEDULED
    }

    internal fun removeObservation(instance: Any, scope: RecomposeScopeImpl) {
        observations.remove(instance, scope)
    }

    internal fun removeDerivedStateObservation(state: DerivedState<*>) {
        // remove derived state if it is not observed in other scopes
        if (state !in observations) {
            derivedStates.removeScope(state)
        }
    }

    /**
     * This takes ownership of the current invalidations and sets up a new array map to hold the new
     * invalidations.
     */
    private fun takeInvalidations(): ScopeMap<RecomposeScopeImpl, Any> {
        val invalidations = invalidations
        this.invalidations = ScopeMap()
        return invalidations
    }

    /**
     * Helper for [verifyConsistent] to ensure the anchor match there respective invalidation
     * scopes.
     */
    private fun validateRecomposeScopeAnchors(slotTable: SlotTable) {
        val scopes = slotTable.slots.mapNotNull { it as? RecomposeScopeImpl }
        scopes.fastForEach { scope ->
            scope.anchor?.let { anchor ->
                checkPrecondition(scope in slotTable.slotsOf(anchor.toIndexFor(slotTable))) {
                    val dataIndex = slotTable.slots.indexOf(scope)
                    "Misaligned anchor $anchor in scope $scope encountered, scope found at " +
                        "$dataIndex"
                }
            }
        }
    }

    private inline fun <T> trackAbandonedValues(block: () -> T): T {
        var success = false
        return try {
            block().also { success = true }
        } finally {
            if (!success && abandonSet.isNotEmpty()) {
                rememberManager.use(abandonSet, composer.errorContext) { dispatchAbandons() }
            }
        }
    }

    private fun observer(): CompositionObserver? = observerHolder.current()

    override fun deactivate() {
        synchronized(lock) {
            checkPrecondition(pendingPausedComposition == null) {
                "Deactivate is not supported while pausable composition is in progress"
            }
            val nonEmptySlotTable = slotTable.groupsSize > 0
            if (nonEmptySlotTable || abandonSet.isNotEmpty()) {
                trace("Compose:deactivate") {
                    rememberManager.use(abandonSet, composer.errorContext) {
                        if (nonEmptySlotTable) {
                            applier.onBeginChanges()
                            slotTable.write { writer ->
                                writer.deactivateCurrentGroup(rememberManager)
                            }
                            applier.onEndChanges()
                            dispatchRememberObservers()
                        }
                        dispatchAbandons()
                    }
                }
            }
            observations.clear()
            derivedStates.clear()
            invalidations.clear()
            changes.clear()
            lateChanges.clear()
            composer.deactivate()

            state = DEACTIVATED
        }
    }

    // This is only used in tests to ensure the stacks do not silently leak.
    internal fun composerStacksSizes(): Int = composer.stacksSize()
}

internal object ScopeInvalidated

@OptIn(ExperimentalComposeRuntimeApi::class)
internal class CompositionObserverHolder(
    var observer: CompositionObserver? = null,
    var root: Boolean = false,
    private val parent: CompositionContext,
) {
    fun current(): CompositionObserver? {
        return if (root) {
            observer
        } else {
            val parentHolder = parent.observerHolder
            val parentObserver = parentHolder?.observer
            if (parentObserver != observer) {
                observer = parentObserver
            }
            parentObserver
        }
    }
}
