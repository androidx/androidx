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

package androidx.compose.ui.layout

import androidx.collection.IntList
import androidx.collection.MutableOrderedScatterSet
import androidx.collection.mutableIntListOf
import androidx.collection.mutableIntSetOf
import androidx.collection.mutableOrderedScatterSetOf
import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.PausableComposition
import androidx.compose.runtime.PausedComposition
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.ReusableComposition
import androidx.compose.runtime.ReusableContentHost
import androidx.compose.runtime.ShouldPauseCallback
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHashCode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.internal.checkPrecondition
import androidx.compose.ui.internal.requirePrecondition
import androidx.compose.ui.internal.throwIllegalStateExceptionForNullCheck
import androidx.compose.ui.internal.throwIndexOutOfBoundsException
import androidx.compose.ui.layout.SubcomposeLayoutState.PausedPrecomposition
import androidx.compose.ui.layout.SubcomposeLayoutState.PrecomposedSlotHandle
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ComposeUiNode.Companion.ApplyOnDeactivatedNodeAssertion
import androidx.compose.ui.node.ComposeUiNode.Companion.SetCompositeKeyHash
import androidx.compose.ui.node.ComposeUiNode.Companion.SetModifier
import androidx.compose.ui.node.ComposeUiNode.Companion.SetResolvedCompositionLocals
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.LayoutNode.LayoutState
import androidx.compose.ui.node.LayoutNode.UsageByParent
import androidx.compose.ui.node.OutOfFrameExecutor
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.TraversableNode.Companion.TraverseDescendantsAction
import androidx.compose.ui.node.checkMeasuredSize
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.node.traverseDescendants
import androidx.compose.ui.platform.createPausableSubcomposition
import androidx.compose.ui.platform.createSubcomposition
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import kotlin.jvm.JvmInline

/**
 * Analogue of [Layout] which allows to subcompose the actual content during the measuring stage for
 * example to use the values calculated during the measurement as params for the composition of the
 * children.
 *
 * Possible use cases:
 * * You need to know the constraints passed by the parent during the composition and can't solve
 *   your use case with just custom [Layout] or [LayoutModifier]. See
 *   [androidx.compose.foundation.layout.BoxWithConstraints].
 * * You want to use the size of one child during the composition of the second child.
 * * You want to compose your items lazily based on the available size. For example you have a list
 *   of 100 items and instead of composing all of them you only compose the ones which are currently
 *   visible(say 5 of them) and compose next items when the component is scrolled.
 *
 * @sample androidx.compose.ui.samples.SubcomposeLayoutSample
 * @param modifier [Modifier] to apply for the layout.
 * @param measurePolicy Measure policy which provides ability to subcompose during the measuring.
 */
@Composable
fun SubcomposeLayout(
    modifier: Modifier = Modifier,
    measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult,
) {
    SubcomposeLayout(
        state = remember { SubcomposeLayoutState() },
        modifier = modifier,
        measurePolicy = measurePolicy,
    )
}

/**
 * Analogue of [Layout] which allows to subcompose the actual content during the measuring stage for
 * example to use the values calculated during the measurement as params for the composition of the
 * children.
 *
 * Possible use cases:
 * * You need to know the constraints passed by the parent during the composition and can't solve
 *   your use case with just custom [Layout] or [LayoutModifier]. See
 *   [androidx.compose.foundation.layout.BoxWithConstraints].
 * * You want to use the size of one child during the composition of the second child.
 * * You want to compose your items lazily based on the available size. For example you have a list
 *   of 100 items and instead of composing all of them you only compose the ones which are currently
 *   visible(say 5 of them) and compose next items when the component is scrolled.
 *
 * @sample androidx.compose.ui.samples.SubcomposeLayoutSample
 * @param state the state object to be used by the layout.
 * @param modifier [Modifier] to apply for the layout.
 * @param measurePolicy Measure policy which provides ability to subcompose during the measuring.
 */
@Composable
@UiComposable
fun SubcomposeLayout(
    state: SubcomposeLayoutState,
    modifier: Modifier = Modifier,
    measurePolicy: SubcomposeMeasureScope.(Constraints) -> MeasureResult,
) {
    val compositeKeyHash = currentCompositeKeyHashCode.hashCode()
    val compositionContext = rememberCompositionContext()
    val materialized = currentComposer.materialize(modifier)
    val localMap = currentComposer.currentCompositionLocalMap
    ReusableComposeNode<LayoutNode, Applier<Any>>(
        factory = LayoutNode.Constructor,
        update = {
            set(state, state.setRoot)
            set(compositionContext, state.setCompositionContext)
            set(measurePolicy, state.setMeasurePolicy)
            set(localMap, SetResolvedCompositionLocals)
            reconcile(ApplyOnDeactivatedNodeAssertion)
            set(materialized, SetModifier)
            set(compositeKeyHash, SetCompositeKeyHash)
        },
    )
    if (!currentComposer.skipping) {
        SideEffect { state.forceRecomposeChildren() }
    }
}

/**
 * The receiver scope of a [SubcomposeLayout]'s measure lambda which adds ability to dynamically
 * subcompose a content during the measuring on top of the features provided by [MeasureScope].
 */
interface SubcomposeMeasureScope : MeasureScope {
    /**
     * Performs subcomposition of the provided [content] with given [slotId].
     *
     * @param slotId unique id which represents the slot we are composing into. If you have fixed
     *   amount or slots you can use enums as slot ids, or if you have a list of items maybe an
     *   index in the list or some other unique key can work. To be able to correctly match the
     *   content between remeasures you should provide the object which is equals to the one you
     *   used during the previous measuring.
     * @param content the composable content which defines the slot. It could emit multiple layouts,
     *   in this case the returned list of [Measurable]s will have multiple elements. **Note:** When
     *   a [SubcomposeLayout] is in a [LookaheadScope], the subcomposition only happens during the
     *   lookahead pass. In the post-lookahead/main pass, [subcompose] will return the list of
     *   [Measurable]s that were subcomposed during the lookahead pass. If the structure of the
     *   subtree emitted from [content] is dependent on incoming constraints, consider using
     *   constraints received from the lookahead pass for both passes.
     */
    fun subcompose(slotId: Any?, content: @Composable () -> Unit): List<Measurable>
}

/**
 * State used by [SubcomposeLayout].
 *
 * [slotReusePolicy] the policy defining what slots should be retained to be reused later.
 */
class SubcomposeLayoutState(private val slotReusePolicy: SubcomposeSlotReusePolicy) {
    /** State used by [SubcomposeLayout]. */
    constructor() : this(NoOpSubcomposeSlotReusePolicy)

    /**
     * State used by [SubcomposeLayout].
     *
     * @param maxSlotsToRetainForReuse when non-zero the layout will keep active up to this count
     *   slots which we were used but not used anymore instead of disposing them. Later when you try
     *   to compose a new slot instead of creating a completely new slot the layout would reuse the
     *   previous slot which allows to do less work especially if the slot contents are similar.
     */
    @Deprecated(
        "This constructor is deprecated",
        ReplaceWith(
            "SubcomposeLayoutState(SubcomposeSlotReusePolicy(maxSlotsToRetainForReuse))",
            "androidx.compose.ui.layout.SubcomposeSlotReusePolicy",
        ),
    )
    constructor(
        maxSlotsToRetainForReuse: Int
    ) : this(SubcomposeSlotReusePolicy(maxSlotsToRetainForReuse))

    private var _state: LayoutNodeSubcompositionsState? = null
    private val state: LayoutNodeSubcompositionsState
        get() =
            requireNotNull(_state) { "SubcomposeLayoutState is not attached to SubcomposeLayout" }

    // Pre-allocated lambdas to update LayoutNode
    internal val setRoot: LayoutNode.(SubcomposeLayoutState) -> Unit = {
        _state =
            subcompositionsState
                ?: LayoutNodeSubcompositionsState(this, slotReusePolicy).also {
                    subcompositionsState = it
                }
        state.makeSureStateIsConsistent()
        state.slotReusePolicy = slotReusePolicy
    }
    internal val setCompositionContext: LayoutNode.(CompositionContext) -> Unit = {
        state.compositionContext = it
    }
    internal val setMeasurePolicy:
        LayoutNode.((SubcomposeMeasureScope.(Constraints) -> MeasureResult)) -> Unit =
        {
            measurePolicy = state.createMeasurePolicy(it)
        }

    /**
     * Composes the content for the given [slotId]. This makes the next scope.subcompose(slotId)
     * call during the measure pass faster as the content is already composed.
     *
     * If the [slotId] was precomposed already but after the future calculations ended up to not be
     * needed anymore (meaning this slotId is not going to be used during the measure pass anytime
     * soon) you can use [PrecomposedSlotHandle.dispose] on a returned object to dispose the
     * content.
     *
     * @param slotId unique id which represents the slot to compose into.
     * @param content the composable content which defines the slot.
     * @return [PrecomposedSlotHandle] instance which allows you to dispose the content.
     */
    fun precompose(slotId: Any?, content: @Composable () -> Unit): PrecomposedSlotHandle =
        state.precompose(slotId, content)

    /**
     * Creates [PausedPrecomposition], which allows to perform the composition in an incremental
     * manner.
     *
     * @param slotId unique id which represents the slot to compose into.
     * @param content the composable content which defines the slot.]
     * @return [PausedPrecomposition] for the given [slotId]. It allows to perform the composition
     *   in an incremental manner. Performing full or partial precomposition makes the next
     *   scope.subcompose(slotId) call during the measure pass faster as the content is already
     *   composed.
     */
    fun createPausedPrecomposition(
        slotId: Any?,
        content: @Composable () -> Unit,
    ): PausedPrecomposition = state.precomposePaused(slotId, content)

    internal fun forceRecomposeChildren() = state.forceRecomposeChildren()

    /**
     * A [PausedPrecomposition] is a subcomposition that can be composed incrementally as it
     * supports being paused and resumed.
     *
     * Pausable subcomposition can be used between frames to prepare a subcomposition before it is
     * required by the main composition. For example, this is used in lazy lists to prepare list
     * items in between frames to that are likely to be scrolled in. The composition is paused when
     * the start of the next frame is near, allowing composition to be spread across multiple frames
     * without delaying the production of the next frame.
     *
     * @see [PausedComposition]
     */
    sealed interface PausedPrecomposition {

        /**
         * Returns `true` when the [PausedPrecomposition] is complete. [isComplete] matches the last
         * value returned from [resume]. Once a [PausedPrecomposition] is [isComplete] the [apply]
         * method should be called. If the [apply] method is not called synchronously and
         * immediately after [resume] returns `true` then this [isComplete] can return `false` as
         * any state changes read by the paused composition while it is paused will cause the
         * composition to require the paused composition to need to be resumed before it is used.
         */
        val isComplete: Boolean

        /**
         * Resume the composition that has been paused. This method should be called until [resume]
         * returns `true` or [isComplete] is `true` which has the same result as the last result of
         * calling [resume]. The [shouldPause] parameter is a lambda that returns whether the
         * composition should be paused. For example, in lazy lists this returns `false` until just
         * prior to the next frame starting in which it returns `true`
         *
         * Calling [resume] after it returns `true` or when `isComplete` is true will throw an
         * exception.
         *
         * @param shouldPause A lambda that is used to determine if the composition should be
         *   paused. This lambda is called often so should be a very simple calculation. Returning
         *   `true` does not guarantee the composition will pause, it should only be considered a
         *   request to pause the composition. Not all composable functions are pausable and only
         *   pausable composition functions will pause.
         * @return `true` if the composition is complete and `false` if one or more calls to
         *   `resume` are required to complete composition.
         */
        @Suppress("ExecutorRegistration") fun resume(shouldPause: ShouldPauseCallback): Boolean

        /**
         * Apply the composition. This is the last step of a paused composition and is required to
         * be called prior to the composition is usable.
         *
         * Calling [apply] should always be proceeded with a check of [isComplete] before it is
         * called and potentially calling [resume] in a loop until [isComplete] returns `true`. This
         * can happen if [resume] returned `true` but [apply] was not synchronously called
         * immediately afterwords. Any state that was read that changed between when [resume] being
         * called and [apply] being called may require the paused composition to be resumed before
         * applied.
         *
         * @return [PrecomposedSlotHandle] you can use to premeasure the slot as well, or to dispose
         *   the composed content.
         */
        fun apply(): PrecomposedSlotHandle

        /**
         * Cancels the paused composition. This should only be used if the composition is going to
         * be disposed and the entire composition is not going to be used.
         */
        fun cancel()
    }

    /** Instance of this interface is returned by [precompose] function. */
    interface PrecomposedSlotHandle {

        /**
         * This function allows to dispose the content for the slot which was precomposed previously
         * via [precompose].
         *
         * If this slot was already used during the regular measure pass via
         * [SubcomposeMeasureScope.subcompose] this function will do nothing.
         *
         * This could be useful if after the future calculations this item is not anymore expected
         * to be used during the measure pass anytime soon.
         */
        fun dispose()

        /** The amount of placeables composed into this slot. */
        val placeablesCount: Int
            get() = 0

        /**
         * Performs synchronous measure of the placeable at the given [index].
         *
         * @param index the placeable index. Should be smaller than [placeablesCount].
         * @param constraints Constraints to measure this placeable with.
         */
        fun premeasure(index: Int, constraints: Constraints) {}

        /**
         * Conditionally executes [block] for each [Modifier.Node] of this Composition that is a
         * [TraversableNode] with a matching [key].
         *
         * See [androidx.compose.ui.node.traverseDescendants] for the complete semantics of this
         * function.
         */
        fun traverseDescendants(key: Any?, block: (TraversableNode) -> TraverseDescendantsAction) {}

        /**
         * Retrieves the latest measured size for a given placeable [index]. This will return
         * [IntSize.Zero] if this is called before [premeasure].
         */
        fun getSize(index: Int): IntSize = IntSize.Zero
    }
}

/**
 * This policy allows [SubcomposeLayout] to retain some of slots which we were used but not used
 * anymore instead of disposing them. Next time when you try to compose a new slot instead of
 * creating a completely new slot the layout would reuse the kept slot. This allows to do less work
 * especially if the slot contents are similar.
 */
interface SubcomposeSlotReusePolicy {
    /**
     * This function will be called with [slotIds] set populated with the slot ids available to
     * reuse. In the implementation you can remove slots you don't want to retain.
     */
    fun getSlotsToRetain(slotIds: SlotIdsSet)

    /**
     * Returns true if the content previously composed with [reusableSlotId] is compatible with the
     * content which is going to be composed for [slotId]. Slots could be considered incompatible if
     * they display completely different types of the UI.
     */
    fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean

    /**
     * Set containing slot ids currently available to reuse. Used by [getSlotsToRetain]. The set
     * retains the insertion order of its elements, guaranteeing stable iteration order.
     *
     * This class works exactly as [MutableSet], but doesn't allow to add new items in it.
     */
    class SlotIdsSet
    internal constructor(
        @PublishedApi
        internal val set: MutableOrderedScatterSet<Any?> = mutableOrderedScatterSetOf()
    ) : Collection<Any?> {

        override val size: Int
            get() = set.size

        override fun isEmpty(): Boolean = set.isEmpty()

        override fun containsAll(elements: Collection<Any?>): Boolean {
            elements.forEach { element ->
                if (element !in set) {
                    return false
                }
            }
            return true
        }

        override fun contains(element: Any?): Boolean = set.contains(element)

        internal fun add(slotId: Any?) = set.add(slotId)

        override fun iterator(): MutableIterator<Any?> = set.asMutableSet().iterator()

        /**
         * Removes a [slotId] from this set, if it is present.
         *
         * @return `true` if the slot id was removed, `false` if the set was not modified.
         */
        fun remove(slotId: Any?): Boolean = set.remove(slotId)

        /**
         * Removes all slot ids from [slotIds] that are also contained in this set.
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun removeAll(slotIds: Collection<Any?>): Boolean = set.remove(slotIds)

        /**
         * Removes all slot ids that match the given [predicate].
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun removeAll(predicate: (Any?) -> Boolean): Boolean {
            val size = set.size
            set.removeIf(predicate)
            return size != set.size
        }

        /**
         * Retains only the slot ids that are contained in [slotIds].
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun retainAll(slotIds: Collection<Any?>): Boolean = set.retainAll(slotIds)

        /**
         * Retains only slotIds that match the given [predicate].
         *
         * @return `true` if any slot id was removed, `false` if the set was not modified.
         */
        fun retainAll(predicate: (Any?) -> Boolean): Boolean = set.retainAll(predicate)

        /** Removes all slot ids from this set. */
        fun clear() = set.clear()

        /**
         * Remove entries until [size] equals [maxSlotsToRetainForReuse]. Entries inserted last are
         * removed first.
         */
        fun trimToSize(maxSlotsToRetainForReuse: Int) = set.trimToSize(maxSlotsToRetainForReuse)

        /**
         * Iterates over every element stored in this set by invoking the specified [block] lambda.
         * The iteration order is the same as the insertion order. It is safe to remove the element
         * passed to [block] during iteration.
         *
         * NOTE: This method is obscured by `Collection<T>.forEach` since it is marked with
         *
         * @HidesMember, which means in practice this will never get called. Please use
         *   [fastForEach] instead.
         */
        fun forEach(block: (Any?) -> Unit) = set.forEach(block)

        /**
         * Iterates over every element stored in this set by invoking the specified [block] lambda.
         * The iteration order is the same as the insertion order. It is safe to remove the element
         * passed to [block] during iteration.
         *
         * NOTE: this method was added in order to allow for a more performant forEach method. It is
         * necessary because [forEach] is obscured by `Collection<T>.forEach` since it is marked
         * with @HidesMember.
         */
        inline fun fastForEach(block: (Any?) -> Unit) = set.forEach(block)
    }
}

/**
 * Creates [SubcomposeSlotReusePolicy] which retains the fixed amount of slots.
 *
 * @param maxSlotsToRetainForReuse the [SubcomposeLayout] will retain up to this amount of slots.
 */
fun SubcomposeSlotReusePolicy(maxSlotsToRetainForReuse: Int): SubcomposeSlotReusePolicy =
    FixedCountSubcomposeSlotReusePolicy(maxSlotsToRetainForReuse)

/**
 * The inner state containing all the information about active slots and their compositions. It is
 * stored inside LayoutNode object as in fact we need to keep 1-1 mapping between this state and the
 * node: when we compose a slot we first create a virtual LayoutNode child to this node and then
 * save the extra information inside this state. Keeping this state inside LayoutNode also helps us
 * to retain the pool of reusable slots even when a new SubcomposeLayoutState is applied to
 * SubcomposeLayout and even when the SubcomposeLayout's LayoutNode is reused via the
 * ReusableComposeNode mechanism.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class LayoutNodeSubcompositionsState(
    private val root: LayoutNode,
    slotReusePolicy: SubcomposeSlotReusePolicy,
) : ComposeNodeLifecycleCallback {
    var compositionContext: CompositionContext? = null

    var slotReusePolicy: SubcomposeSlotReusePolicy = slotReusePolicy
        set(value) {
            if (field !== value) {
                field = value
                // the new policy will be applied after measure
                markActiveNodesAsReused(deactivate = false)
                root.requestRemeasure()
            }
        }

    private var currentIndex = 0
    private var currentApproachIndex = 0
    private val nodeToNodeState = mutableScatterMapOf<LayoutNode, NodeState>()

    // this map contains active slotIds (without precomposed or reusable nodes)
    private val slotIdToNode = mutableScatterMapOf<Any?, LayoutNode>()
    private val scope = Scope()
    private val approachMeasureScope = ApproachMeasureScopeImpl()

    private val precomposeMap = mutableScatterMapOf<Any?, LayoutNode>()
    private val reusableSlotIdsSet = SubcomposeSlotReusePolicy.SlotIdsSet()

    // SlotHandles precomposed in the approach pass. These slot handles are owned by the approach
    // pass, hence the approach pass is responsible for disposing them when they are no longer
    // needed. Note: if `precompose` is called on a slot owned by the approach pass, the
    // approach will yield ownership to the new caller. When the new caller disposes a slot
    // that is still needed by approach, the approach pass will be triggered to create
    // and own the slot.
    private val approachPrecomposeSlotHandleMap = mutableScatterMapOf<Any?, PrecomposedSlotHandle>()

    // Slot ids of compositions needed in the approach pass. These compositions are either owned
    // by the approach pass, or by the caller of [SubcomposeLayoutState#precompose]. For
    // compositions not created by the approach pass, if they are disposed while the approach pass
    // still needs it, the approach pass will be triggered to re-create the composition.
    // The valid slot ids are stored between 0 and currentApproachIndex - 1, beyond index
    // currentApproachIndex are [UnspecifiedSlotId]s.
    private val slotIdsOfCompositionsNeededInApproach = mutableVectorOf<Any?>()

    /**
     * `root.foldedChildren` list consist of:
     * 1) all the active children (used during the last measure pass)
     * 2) `reusableCount` nodes in the middle of the list which were active and stopped being used.
     *    now we keep them (up to `maxCountOfSlotsToReuse`) in order to reuse next time we will need
     *    to compose a new item
     * 4) `precomposedCount` nodes in the end of the list which were precomposed and are waiting to
     *    be used during the next measure passes.
     */
    private var reusableCount = 0
    private var precomposedCount = 0

    override fun onReuse() {
        markActiveNodesAsReused(deactivate = false)
    }

    override fun onDeactivate() {
        markActiveNodesAsReused(deactivate = true)
    }

    override fun onRelease() {
        disposeCurrentNodes()
    }

    fun subcompose(slotId: Any?, content: @Composable () -> Unit): List<Measurable> {
        makeSureStateIsConsistent()
        val layoutState = root.layoutState
        checkPrecondition(
            layoutState == LayoutState.Measuring ||
                layoutState == LayoutState.LayingOut ||
                layoutState == LayoutState.LookaheadMeasuring ||
                layoutState == LayoutState.LookaheadLayingOut
        ) {
            "subcompose can only be used inside the measure or layout blocks"
        }

        val node =
            slotIdToNode.getOrPut(slotId) {
                val precomposed = precomposeMap.remove(slotId)
                if (precomposed != null) {
                    val nodeState = nodeToNodeState[precomposed]
                    nodeState?.record(SLOperation.TookFromPrecomposeMap)
                    @Suppress("ExceptionMessage") checkPrecondition(precomposedCount > 0)
                    precomposedCount--
                    precomposed
                } else {
                    takeNodeFromReusables(slotId) ?: createNodeAt(currentIndex)
                }
            }

        if (root.foldedChildren.getOrNull(currentIndex) !== node) {
            // the node has a new index in the list
            val itemIndex = root.foldedChildren.indexOf(node)
            requirePrecondition(itemIndex >= currentIndex) {
                "Key \"$slotId\" was already used. If you are using LazyColumn/Row please make " +
                    "sure you provide a unique key for each item."
            }
            if (currentIndex != itemIndex) {
                move(itemIndex, currentIndex)
            }
        }
        currentIndex++

        subcompose(node, slotId, pausable = false, content)

        return if (layoutState == LayoutState.Measuring || layoutState == LayoutState.LayingOut) {
            node.childMeasurables
        } else {
            node.childLookaheadMeasurables
        }
    }

    // This may be called in approach pass, if a node is only emitted in the approach pass, but
    // not in the lookahead pass.
    private fun subcompose(
        node: LayoutNode,
        slotId: Any?,
        pausable: Boolean,
        content: @Composable () -> Unit,
    ) {
        val nodeState = nodeToNodeState.getOrPut(node) { NodeState(slotId, {}) }
        val contentChanged = nodeState.content !== content
        if (nodeState.pausedComposition != null) {
            if (contentChanged) {
                // content did change so it is not safe to apply the current paused composition.
                nodeState.cancelPausedPrecomposition()
            } else if (pausable) {
                // the paused composition is initialized and the content didn't change
                return
            } else {
                // we can apply as we are still composing the same content.
                nodeState.applyPausedPrecomposition(shouldComplete = true)
            }
        }
        val hasPendingChanges = nodeState.composition?.hasInvalidations ?: true
        if (contentChanged || hasPendingChanges || nodeState.forceRecompose) {
            nodeState.content = content
            subcompose(node, nodeState, pausable)
            nodeState.forceRecompose = false
        }
    }

    private val outOfFrameExecutor: OutOfFrameExecutor?
        get() = root.requireOwner().outOfFrameExecutor

    private fun subcompose(node: LayoutNode, nodeState: NodeState, pausable: Boolean) {
        requirePrecondition(nodeState.pausedComposition == null) {
            "new subcompose call while paused composition is still active"
        }
        Snapshot.withoutReadObservation {
            ignoreRemeasureRequests {
                val existing = nodeState.composition
                val parentComposition =
                    compositionContext
                        ?: throwIllegalStateExceptionForNullCheck(
                            "parent composition reference not set"
                        )
                nodeState.record(
                    if (existing == null) SLOperation.SubcomposeNew else SLOperation.Subcompose
                )
                if (pausable) {
                    nodeState.record(SLOperation.SubcomposePausable)
                }
                if (nodeState.forceReuse) {
                    nodeState.record(SLOperation.SubcomposeForceReuse)
                }
                val composition =
                    if (existing == null || existing.isDisposed) {
                        if (pausable) {
                            createPausableSubcomposition(node, parentComposition)
                        } else {
                            createSubcomposition(node, parentComposition)
                        }
                    } else {
                        existing
                    }
                nodeState.composition = composition
                val content = nodeState.content
                val composable: @Composable () -> Unit =
                    if (outOfFrameExecutor != null) {
                        nodeState.composedWithReusableContentHost = false
                        content
                    } else {
                        nodeState.composedWithReusableContentHost = true
                        { ReusableContentHost(nodeState.active, content) }
                    }
                if (pausable) {
                    composition as PausableComposition
                    if (nodeState.forceReuse) {
                        nodeState.pausedComposition =
                            composition.setPausableContentWithReuse(composable)
                    } else {
                        nodeState.pausedComposition = composition.setPausableContent(composable)
                    }
                } else {
                    if (nodeState.forceReuse) {
                        composition.setContentWithReuse(composable)
                    } else {
                        composition.setContent(composable)
                    }
                }
                nodeState.forceReuse = false
            }
        }
    }

    private fun getSlotIdAtIndex(foldedChildren: List<LayoutNode>, index: Int): Any? {
        val node = foldedChildren[index]
        return nodeToNodeState[node]!!.slotId
    }

    fun disposeOrReuseStartingFromIndex(startIndex: Int) {
        reusableCount = 0
        val foldedChildren = root.foldedChildren
        val lastReusableIndex = foldedChildren.size - precomposedCount - 1
        var needApplyNotification = false
        if (startIndex <= lastReusableIndex) {
            // construct the set of available slot ids
            reusableSlotIdsSet.clear()
            for (i in startIndex..lastReusableIndex) {
                val slotId = getSlotIdAtIndex(foldedChildren, i)
                reusableSlotIdsSet.add(slotId)
            }

            slotReusePolicy.getSlotsToRetain(reusableSlotIdsSet)
            // iterating backwards so it is easier to remove items
            var i = lastReusableIndex
            Snapshot.withoutReadObservation {
                while (i >= startIndex) {
                    val node = foldedChildren[i]
                    val nodeState = nodeToNodeState[node]!!
                    val slotId = nodeState.slotId
                    if (slotId in reusableSlotIdsSet) {
                        reusableCount++
                        if (nodeState.active) {
                            node.resetLayoutState()
                            nodeState.reuseComposition(forceDeactivate = false)

                            if (nodeState.composedWithReusableContentHost) {
                                needApplyNotification = true
                            }
                        }
                    } else {
                        ignoreRemeasureRequests {
                            nodeToNodeState.remove(node)
                            nodeState.composition?.dispose()
                            root.removeAt(i, 1)
                        }
                    }
                    // remove it from slotIdToNode so it is not considered active
                    slotIdToNode.remove(slotId)
                    i--
                }
            }
        }

        if (needApplyNotification) {
            Snapshot.sendApplyNotifications()
        }

        makeSureStateIsConsistent()
    }

    private fun NodeState.deactivateOutOfFrame(executor: OutOfFrameExecutor) {
        executor.schedule {
            if (!active) {
                record(SLOperation.DeactivateOutOfFrame)
                composition?.deactivate()
            } else {
                record(SLOperation.DeactivateOutOfFrameCancelled)
            }
        }
    }

    private fun markActiveNodesAsReused(deactivate: Boolean) {
        precomposedCount = 0
        precomposeMap.clear()

        val foldedChildren = root.foldedChildren
        val childCount = foldedChildren.size
        if (reusableCount != childCount) {
            reusableCount = childCount
            Snapshot.withoutReadObservation {
                for (i in 0 until childCount) {
                    val node = foldedChildren[i]
                    val nodeState = nodeToNodeState[node]
                    if (nodeState != null && nodeState.active) {
                        node.resetLayoutState()
                        nodeState.reuseComposition(forceDeactivate = deactivate)
                        nodeState.slotId = ReusedSlotId
                        if (deactivate) {
                            nodeState.record(SLOperation.SlotToReusedFromOnDeactivate)
                        } else {
                            nodeState.record(SLOperation.SlotToReusedFromOnReuse)
                        }
                    }
                }
            }
            slotIdToNode.clear()
        }

        makeSureStateIsConsistent()
    }

    private fun disposeCurrentNodes() {
        root.ignoreRemeasureRequests {
            nodeToNodeState.forEachValue { it.composition?.dispose() }
            root.removeAll()
        }

        nodeToNodeState.clear()
        slotIdToNode.clear()
        precomposedCount = 0
        reusableCount = 0
        precomposeMap.clear()

        makeSureStateIsConsistent()
    }

    fun makeSureStateIsConsistent() {
        val childrenCount = root.foldedChildren.size
        requirePrecondition(nodeToNodeState.size == childrenCount) {
            "Inconsistency between the count of nodes tracked by the state " +
                "(${nodeToNodeState.size}) and the children count on the SubcomposeLayout" +
                " ($childrenCount). Are you trying to use the state of the" +
                " disposed SubcomposeLayout?"
        }
        requirePrecondition(childrenCount - reusableCount - precomposedCount >= 0) {
            "Incorrect state. Total children $childrenCount. Reusable children " +
                "$reusableCount. Precomposed children $precomposedCount"
        }
        requirePrecondition(precomposeMap.size == precomposedCount) {
            "Incorrect state. Precomposed children $precomposedCount. Map size " +
                "${precomposeMap.size}"
        }
    }

    private fun LayoutNode.resetLayoutState() {
        measurePassDelegate.measuredByParent = UsageByParent.NotUsed
        lookaheadPassDelegate?.let { it.measuredByParent = UsageByParent.NotUsed }
    }

    private fun takeNodeFromReusables(slotId: Any?): LayoutNode? {
        if (reusableCount == 0) {
            return null
        }
        val foldedChildren = root.foldedChildren
        val reusableNodesSectionEnd = foldedChildren.size - precomposedCount
        val reusableNodesSectionStart = reusableNodesSectionEnd - reusableCount
        var index = reusableNodesSectionEnd - 1
        var chosenIndex = -1
        // first try to find a node with exactly the same slotId
        while (index >= reusableNodesSectionStart) {
            if (getSlotIdAtIndex(foldedChildren, index) == slotId) {
                // we have a node with the same slotId
                chosenIndex = index
                break
            } else {
                index--
            }
        }
        if (chosenIndex == -1) {
            // try to find a first compatible slotId from the end of the section
            index = reusableNodesSectionEnd - 1
            while (index >= reusableNodesSectionStart) {
                val node = foldedChildren[index]
                val nodeState = nodeToNodeState[node]!!
                if (
                    nodeState.slotId === ReusedSlotId ||
                        slotReusePolicy.areCompatible(slotId, nodeState.slotId)
                ) {
                    nodeState.slotId = slotId
                    chosenIndex = index
                    break
                }
                index--
            }
        }
        return if (chosenIndex == -1) {
            // no compatible nodes found
            null
        } else {
            if (index != reusableNodesSectionStart) {
                // we need to rearrange the items
                move(index, reusableNodesSectionStart, 1)
            }
            reusableCount--
            val node = foldedChildren[reusableNodesSectionStart]
            val nodeState = nodeToNodeState[node]!!
            // create a new instance to avoid change notifications
            nodeState.record(SLOperation.Reused)
            nodeState.activeState = mutableStateOf(true)
            nodeState.forceReuse = true
            nodeState.forceRecompose = true
            node
        }
    }

    fun createMeasurePolicy(
        block: SubcomposeMeasureScope.(Constraints) -> MeasureResult
    ): MeasurePolicy {
        return object : LayoutNode.NoIntrinsicsMeasurePolicy(error = NoIntrinsicsMessage) {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints,
            ): MeasureResult {
                scope.layoutDirection = layoutDirection
                scope.density = density
                scope.fontScale = fontScale
                if (!isLookingAhead && root.lookaheadRoot != null) {
                    // Approach pass
                    currentApproachIndex = 0
                    val result = approachMeasureScope.block(constraints)
                    val indexAfterMeasure = currentApproachIndex
                    return createMeasureResult(result) {
                        currentApproachIndex = indexAfterMeasure
                        result.placeChildren()
                        // dispose
                        disposeUnusedSlotsInApproach()
                        disposeOrReuseStartingFromIndex(currentIndex)
                    }
                } else {
                    // Lookahead pass, or the main pass if not in a lookahead scope.
                    currentIndex = 0
                    val result = scope.block(constraints)
                    val indexAfterMeasure = currentIndex
                    return createMeasureResult(result) {
                        currentIndex = indexAfterMeasure
                        result.placeChildren()
                        if (root.lookaheadRoot == null) {
                            // If this is in lookahead scope, we need to dispose *after*
                            // approach placement, to give approach pass the opportunity to
                            // transfer the ownership of subcompositions before disposing.
                            disposeOrReuseStartingFromIndex(currentIndex)
                        }
                    }
                }
            }
        }
    }

    private fun disposeUnusedSlotsInApproach() {
        // Iterate over the slots owned by approach, and dispose slots if neither lookahead
        // nor approach needs it.
        approachPrecomposeSlotHandleMap.removeIf { slotId, handle ->
            val id = slotIdsOfCompositionsNeededInApproach.indexOf(slotId)
            if (id < 0 || id >= currentApproachIndex) {
                if (id >= 0) {
                    // Remove the slotId from the list before disposing
                    slotIdsOfCompositionsNeededInApproach[id] = UnspecifiedSlotId
                }
                if (precomposeMap.contains(slotId)) {
                    // Node has not been needed by lookahead, or approach.
                    handle.dispose()
                }
                true
            } else {
                false
            }
        }
    }

    private inline fun createMeasureResult(
        result: MeasureResult,
        crossinline placeChildrenBlock: () -> Unit,
    ) =
        object : MeasureResult by result {
            override fun placeChildren() {
                placeChildrenBlock()
            }
        }

    private val NoIntrinsicsMessage =
        "Asking for intrinsic measurements of SubcomposeLayout " +
            "layouts is not supported. This includes components that are built on top of " +
            "SubcomposeLayout, such as lazy lists, BoxWithConstraints, TabRow, etc. To mitigate " +
            "this:\n" +
            "- if intrinsic measurements are used to achieve 'match parent' sizing, consider " +
            "replacing the parent of the component with a custom layout which controls the order in " +
            "which children are measured, making intrinsic measurement not needed\n" +
            "- adding a size modifier to the component, in order to fast return the queried " +
            "intrinsic measurement."

    fun precompose(slotId: Any?, content: @Composable () -> Unit): PrecomposedSlotHandle {
        precompose(slotId, content, pausable = false)
        return createPrecomposedSlotHandle(slotId)
    }

    private fun precompose(slotId: Any?, content: @Composable () -> Unit, pausable: Boolean) {
        if (!root.isAttached) {
            return
        }
        makeSureStateIsConsistent()
        if (!slotIdToNode.containsKey(slotId)) {
            // Yield ownership of PrecomposedHandle from approach to the caller of precompose
            approachPrecomposeSlotHandleMap.remove(slotId)
            val node =
                precomposeMap.getOrPut(slotId) {
                    val reusedNode = takeNodeFromReusables(slotId)
                    if (reusedNode != null) {
                        // now move this node to the end where we keep precomposed items
                        val nodeIndex = root.foldedChildren.indexOf(reusedNode)
                        move(nodeIndex, root.foldedChildren.size, 1)
                        precomposedCount++
                        reusedNode
                    } else {
                        createNodeAt(root.foldedChildren.size).also { precomposedCount++ }
                    }
                }
            subcompose(node, slotId, pausable = pausable, content)
        }
    }

    private fun NodeState.reuseComposition(forceDeactivate: Boolean) {
        if (!forceDeactivate && composedWithReusableContentHost) {
            // Deactivation through ReusableContentHost is controlled with the active flag
            active = false
        } else {
            // Otherwise, create a new instance to avoid state change notifications
            activeState = mutableStateOf(false)
        }

        if (pausedComposition != null) {
            // Cancelling disposes composition, so no additional work is needed.
            cancelPausedPrecomposition()
        } else if (forceDeactivate) {
            record(SLOperation.ReuseForceSyncDeactivation)
            composition?.deactivate()
        } else {
            val outOfFrameExecutor = outOfFrameExecutor
            if (outOfFrameExecutor != null) {
                record(SLOperation.ReuseScheduleOutOfFrameDeactivation)
                deactivateOutOfFrame(outOfFrameExecutor)
            } else {
                if (!composedWithReusableContentHost) {
                    record(SLOperation.ReuseSyncDeactivation)
                    composition?.deactivate()
                } else {
                    record(SLOperation.ReuseDeactivationViaHost)
                }
            }
        }
    }

    private fun NodeState.cancelPausedPrecomposition() {
        pausedComposition?.let {
            it.cancel()
            pausedComposition = null
            composition?.dispose()
            composition = null
            record(SLOperation.CancelPausedPrecomposition)
        }
    }

    private fun disposePrecomposedSlot(slotId: Any?) {
        makeSureStateIsConsistent()
        val node = precomposeMap.remove(slotId)
        if (node != null) {
            checkPrecondition(precomposedCount > 0) { "No pre-composed items to dispose" }
            val itemIndex = root.foldedChildren.indexOf(node)
            checkPrecondition(itemIndex >= root.foldedChildren.size - precomposedCount) {
                "Item is not in pre-composed item range"
            }
            // move this item into the reusable section
            reusableCount++
            precomposedCount--

            nodeToNodeState[node]?.cancelPausedPrecomposition()

            val reusableStart = root.foldedChildren.size - precomposedCount - reusableCount
            move(itemIndex, reusableStart, 1)
            disposeOrReuseStartingFromIndex(reusableStart)
        }
        // If the slot is not owned by approach (e.g. created for prefetch) and disposed before
        // approach finishes using it, the approach pass will be invoked to re-create the
        // composition if needed.
        if (slotIdsOfCompositionsNeededInApproach.contains(slotId)) {
            root.requestRemeasure(true)
        }
    }

    private fun createPrecomposedSlotHandle(slotId: Any?): PrecomposedSlotHandle {
        if (!root.isAttached) {
            return object : PrecomposedSlotHandle {
                override fun dispose() {}
            }
        }
        return object : PrecomposedSlotHandle {
            // Saves indices of placeables that have been premeasured in this handle
            val hasPremeasured = mutableIntSetOf()

            override fun dispose() {
                disposePrecomposedSlot(slotId)
            }

            override val placeablesCount: Int
                get() = precomposeMap[slotId]?.children?.size ?: 0

            override fun premeasure(index: Int, constraints: Constraints) {
                val node = precomposeMap[slotId]
                if (node != null && node.isAttached) {
                    val size = node.children.size
                    if (index < 0 || index >= size) {
                        throwIndexOutOfBoundsException(
                            "Index ($index) is out of bound of [0, $size)"
                        )
                    }
                    requirePrecondition(!node.isPlaced) {
                        "Pre-measure called on node that is not placed"
                    }
                    root.ignoreRemeasureRequests {
                        node.requireOwner().measureAndLayout(node.children[index], constraints)
                    }
                    hasPremeasured.add(index)
                }
            }

            override fun traverseDescendants(
                key: Any?,
                block: (TraversableNode) -> TraverseDescendantsAction,
            ) {
                precomposeMap[slotId]?.nodes?.head?.traverseDescendants(key, block)
            }

            override fun getSize(index: Int): IntSize {
                val node = precomposeMap[slotId]
                if (node != null && node.isAttached) {
                    val size = node.children.size
                    if (index < 0 || index >= size) {
                        throwIndexOutOfBoundsException(
                            "Index ($index) is out of bound of [0, $size)"
                        )
                    }

                    if (hasPremeasured.contains(index)) {
                        return IntSize(node.children[index].width, node.children[index].height)
                    }
                }
                return IntSize.Zero
            }
        }
    }

    fun precomposePaused(slotId: Any?, content: @Composable () -> Unit): PausedPrecomposition {
        if (!root.isAttached) {
            return object : PausedPrecompositionImpl {
                override val isComplete: Boolean = true

                override fun resume(shouldPause: ShouldPauseCallback) = true

                override fun apply() = createPrecomposedSlotHandle(slotId)

                override fun cancel() {}
            }
        }
        precompose(slotId, content, pausable = true)
        return object : PausedPrecompositionImpl {
            override fun cancel() {
                if (nodeState?.pausedComposition != null) {
                    // only dispose if the paused composition is still waiting to be applied
                    disposePrecomposedSlot(slotId)
                }
            }

            private val nodeState: NodeState?
                get() = precomposeMap[slotId]?.let { nodeToNodeState[it] }

            override val isComplete: Boolean
                get() = nodeState?.pausedComposition?.isComplete ?: true

            override fun resume(shouldPause: ShouldPauseCallback): Boolean {
                val nodeState = nodeState
                val pausedComposition = nodeState?.pausedComposition
                return if (pausedComposition != null && !pausedComposition.isComplete) {
                    nodeState.record(SLOperation.ResumePaused)
                    val isComplete =
                        Snapshot.withoutReadObservation {
                            ignoreRemeasureRequests {
                                try {
                                    pausedComposition.resume(shouldPause)
                                } catch (e: Throwable) {
                                    throw SubcomposeLayoutPausableCompositionException(
                                        nodeState.operations,
                                        slotId,
                                        e,
                                    )
                                }
                            }
                        }
                    if (!isComplete) {
                        nodeState.record(SLOperation.PausePaused)
                    }
                    isComplete
                } else {
                    true
                }
            }

            override fun apply(): PrecomposedSlotHandle {
                nodeState?.applyPausedPrecomposition(shouldComplete = false)
                return createPrecomposedSlotHandle(slotId)
            }
        }
    }

    fun forceRecomposeChildren() {
        val childCount = root.foldedChildren.size
        if (reusableCount != childCount) {
            // only invalidate children if there are any non-reused ones
            // in other cases, all of them are going to be invalidated later anyways
            nodeToNodeState.forEachValue { nodeState -> nodeState.forceRecompose = true }

            if (root.lookaheadRoot != null) {
                // If the SubcomposeLayout is in a LookaheadScope, request for a lookahead measure
                // so that lookahead gets triggered again to recompose children.
                if (!root.lookaheadMeasurePending) {
                    root.requestLookaheadRemeasure()
                }
            } else {
                if (!root.measurePending) {
                    root.requestRemeasure()
                }
            }
        }
    }

    private fun createNodeAt(index: Int) =
        LayoutNode(isVirtual = true).also { node ->
            ignoreRemeasureRequests { root.insertAt(index, node) }
        }

    private fun move(from: Int, to: Int, count: Int = 1) {
        ignoreRemeasureRequests { root.move(from, to, count) }
    }

    private inline fun <T> ignoreRemeasureRequests(block: () -> T): T =
        root.ignoreRemeasureRequests(block)

    private fun NodeState.applyPausedPrecomposition(shouldComplete: Boolean) {
        val pausedComposition = pausedComposition
        if (pausedComposition != null) {
            Snapshot.withoutReadObservation {
                ignoreRemeasureRequests {
                    try {
                        if (shouldComplete) {
                            while (!pausedComposition.isComplete) {
                                pausedComposition.resume { false }
                            }
                        }
                        pausedComposition.apply()
                    } catch (e: Throwable) {
                        throw SubcomposeLayoutPausableCompositionException(operations, slotId, e)
                    }
                    this.pausedComposition = null
                }
            }
        }
    }

    private class NodeState(
        var slotId: Any?,
        var content: @Composable () -> Unit,
        var composition: ReusableComposition? = null,
    ) {
        var forceRecompose = false
        var forceReuse = false
        var pausedComposition: PausedComposition? = null
        var activeState = mutableStateOf(true)
        var composedWithReusableContentHost = false
        var active: Boolean
            get() = activeState.value
            set(value) {
                activeState.value = value
            }

        val operations = mutableIntListOf()

        fun record(op: SLOperation) {
            operations.add(op.value)
            if (operations.size >= 50) {
                operations.removeRange(0, 10)
            }
        }
    }

    private inner class Scope : SubcomposeMeasureScope {
        // MeasureScope delegation
        override var layoutDirection: LayoutDirection = LayoutDirection.Rtl
        override var density: Float = 0f
        override var fontScale: Float = 0f
        override val isLookingAhead: Boolean
            get() =
                root.layoutState == LayoutState.LookaheadLayingOut ||
                    root.layoutState == LayoutState.LookaheadMeasuring

        override fun subcompose(slotId: Any?, content: @Composable () -> Unit) =
            this@LayoutNodeSubcompositionsState.subcompose(slotId, content)

        override fun layout(
            width: Int,
            height: Int,
            alignmentLines: Map<AlignmentLine, Int>,
            rulers: (RulerScope.() -> Unit)?,
            placementBlock: Placeable.PlacementScope.() -> Unit,
        ): MeasureResult {
            checkMeasuredSize(width, height)
            return object : MeasureResult {
                override val width: Int
                    get() = width

                override val height: Int
                    get() = height

                override val alignmentLines: Map<AlignmentLine, Int>
                    get() = alignmentLines

                override val rulers: (RulerScope.() -> Unit)?
                    get() = rulers

                override fun placeChildren() {
                    if (isLookingAhead) {
                        val delegate = root.innerCoordinator.lookaheadDelegate
                        if (delegate != null) {
                            delegate.placementScope.placementBlock()
                            return
                        }
                    }
                    root.innerCoordinator.placementScope.placementBlock()
                }
            }
        }
    }

    private inner class ApproachMeasureScopeImpl : SubcomposeMeasureScope, MeasureScope by scope {
        /**
         * This function retrieves [Measurable]s created for [slotId] based on the subcomposition
         * that happened in the lookahead pass. If [slotId] was not subcomposed in the lookahead
         * pass, [subcompose] will return an [emptyList].
         */
        override fun subcompose(slotId: Any?, content: @Composable () -> Unit): List<Measurable> {
            val nodeInSlot = slotIdToNode[slotId]
            if (nodeInSlot != null && root.foldedChildren.indexOf(nodeInSlot) < currentIndex) {
                // Check that the node has been composed in lookahead. Otherwise, we need to
                // compose the node in approach pass via approachSubcompose.
                return nodeInSlot.childMeasurables
            } else {
                return approachSubcompose(slotId, content)
            }
        }
    }

    private fun approachSubcompose(
        slotId: Any?,
        content: @Composable () -> Unit,
    ): List<Measurable> {
        requirePrecondition(slotIdsOfCompositionsNeededInApproach.size >= currentApproachIndex) {
            "Error: currentApproachIndex cannot be greater than the size of the" +
                "approachComposedSlotIds list."
        }
        val nodeForSlot = slotIdToNode[slotId]
        if (slotIdsOfCompositionsNeededInApproach.size == currentApproachIndex) {
            slotIdsOfCompositionsNeededInApproach.add(slotId)
        } else {
            slotIdsOfCompositionsNeededInApproach[currentApproachIndex] = slotId
        }
        currentApproachIndex++
        val precomposed = precomposeMap.contains(slotId)
        if (!precomposed && nodeForSlot == null) {
            // The slot was not composed in the lookahead pass. And it has not been pre-composed in
            // the approach pass. Hence, we will precompose it for the approach pass, and track it
            // in approachPrecomposeSlotHandleMap so that it can be disposed when no longer needed
            // in approach.
            precompose(slotId, content).also { approachPrecomposeSlotHandleMap[slotId] = it }
        } else {
            // A non-null `nodeForSlot` here means that the slot was composed in lookahead
            // initially, but no longer needed && has not been disposed yet.
            // Move from lookahead composed to pre-composed, so that it can be disposed when
            // no longer needed in approach.
            if (!precomposed && nodeForSlot != null) {
                // Transfer ownership of the subcomposition from lookahead pass to approach pass.
                // As a result, the composition can be disposed as soon as approach pass no
                // longer needs it.
                // First, move this node to the end where we keep precomposed items
                val nodeIndex = root.foldedChildren.indexOf(nodeForSlot)
                move(nodeIndex, root.foldedChildren.size, 1)
                precomposedCount++
                // Remove the slotId from slotIdToNode so that if lookahead were to subcompose
                // this item, it'll need to take the node out of precomposeMap.
                slotIdToNode.remove(slotId)
                precomposeMap[slotId] = nodeForSlot
                approachPrecomposeSlotHandleMap[slotId] = createPrecomposedSlotHandle(slotId)

                if (root.isAttached) {
                    makeSureStateIsConsistent()
                }
            }

            // Re-subcompose if needed based on forceRecompose
            val node = precomposeMap[slotId]
            val nodeState = node?.let { nodeToNodeState[it] }
            if (nodeState?.forceRecompose == true) {
                subcompose(node, slotId, pausable = false, content)
            }
        }

        return precomposeMap[slotId]?.run {
            measurePassDelegate.childDelegates.also {
                it.fastForEach { delegate -> delegate.markDetachedFromParentLookaheadPass() }
            }
        } ?: emptyList()
    }
}

private val ReusedSlotId =
    object {
        override fun toString(): String = "ReusedSlotId"
    }

private class FixedCountSubcomposeSlotReusePolicy(private val maxSlotsToRetainForReuse: Int) :
    SubcomposeSlotReusePolicy {

    override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
        if (slotIds.size > maxSlotsToRetainForReuse) {
            slotIds.trimToSize(maxSlotsToRetainForReuse)
        }
    }

    override fun areCompatible(slotId: Any?, reusableSlotId: Any?): Boolean = true
}

private object NoOpSubcomposeSlotReusePolicy : SubcomposeSlotReusePolicy {
    override fun getSlotsToRetain(slotIds: SubcomposeSlotReusePolicy.SlotIdsSet) {
        slotIds.clear()
    }

    override fun areCompatible(slotId: Any?, reusableSlotId: Any?) = false
}

private interface PausedPrecompositionImpl : PausedPrecomposition

private val UnspecifiedSlotId = Any()

@JvmInline
private value class SLOperation(val value: Int) {
    companion object {
        val CancelPausedPrecomposition = SLOperation(0)
        val ReuseForceSyncDeactivation = SLOperation(1)
        val ReuseScheduleOutOfFrameDeactivation = SLOperation(2)
        val ReuseSyncDeactivation = SLOperation(3)
        val ReuseDeactivationViaHost = SLOperation(4)
        val TookFromPrecomposeMap = SLOperation(5)
        val Subcompose = SLOperation(6)
        val SubcomposeNew = SLOperation(7)
        val SubcomposePausable = SLOperation(8)
        val SubcomposeForceReuse = SLOperation(9)
        val DeactivateOutOfFrame = SLOperation(10)
        val DeactivateOutOfFrameCancelled = SLOperation(11)
        val SlotToReusedFromOnDeactivate = SLOperation(12)
        val SlotToReusedFromOnReuse = SLOperation(13)
        val Reused = SLOperation(14)
        val ResumePaused = SLOperation(15)
        val PausePaused = SLOperation(16)
        val ApplyPaused = SLOperation(17)
    }
}

private class SubcomposeLayoutPausableCompositionException(
    private val operations: IntList,
    private val slotId: Any?,
    cause: Throwable?,
) : IllegalStateException(cause) {

    private fun operationsList(): List<String> = buildList {
        var currentOperation = operations.size - 1
        while (currentOperation >= 0) {
            val operation = operations[currentOperation]
            val stringValue =
                when (SLOperation(operation)) {
                    SLOperation.CancelPausedPrecomposition -> "CancelPausedPrecomposition"
                    SLOperation.ReuseForceSyncDeactivation -> "ReuseForceSyncDeactivation"
                    SLOperation.ReuseScheduleOutOfFrameDeactivation ->
                        "ReuseScheduleOutOfFrameDeactivation"
                    SLOperation.ReuseSyncDeactivation -> "ReuseSyncDeactivation"
                    SLOperation.ReuseDeactivationViaHost -> "ReuseDeactivationViaHost"
                    SLOperation.TookFromPrecomposeMap -> "TookFromPrecomposeMap"
                    SLOperation.Subcompose -> "Subcompose"
                    SLOperation.SubcomposeNew -> "SubcomposeNew"
                    SLOperation.SubcomposePausable -> "SubcomposePausable"
                    SLOperation.SubcomposeForceReuse -> "SubcomposeForceReuse"
                    SLOperation.DeactivateOutOfFrame -> "DeactivateOutOfFrame"
                    SLOperation.DeactivateOutOfFrameCancelled -> "DeactivateOutOfFrameCancelled"
                    SLOperation.SlotToReusedFromOnDeactivate -> "SlotToReusedFromOnDeactivate"
                    SLOperation.SlotToReusedFromOnReuse -> "SlotToReusedFromOnReuse"
                    SLOperation.Reused -> "Reused"
                    SLOperation.ResumePaused -> "ResumePaused"
                    SLOperation.PausePaused -> "PausePaused"
                    SLOperation.ApplyPaused -> "ApplyPaused"
                    else -> "Unexpected $operation"
                }
            add("$currentOperation: $stringValue")
            currentOperation--
        }
    }

    @Suppress("ListIterator")
    override val message: String?
        get() =
            """
            |slotid=$slotId. Last operations:
            |${operationsList().joinToString("\n")}
            """
                .trimMargin()
}
