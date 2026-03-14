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

@file:Suppress("NOTHING_TO_INLINE")
@file:OptIn(ExperimentalComposeRuntimeApi::class)

package androidx.compose.runtime

import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet
import androidx.collection.MutableScatterMap
import androidx.collection.MutableScatterSet
import androidx.collection.ScatterSet
import androidx.collection.mutableScatterSetOf
import androidx.compose.runtime.collection.MultiValueMap
import androidx.compose.runtime.collection.ScopeMap
import androidx.compose.runtime.composer.GroupInfo
import androidx.compose.runtime.composer.GroupKind
import androidx.compose.runtime.composer.ThrowingRememberManagerStub
import androidx.compose.runtime.composer.gapbuffer.BitVector
import androidx.compose.runtime.composer.linkbuffer.GroupAddress
import androidx.compose.runtime.composer.linkbuffer.GroupHandle
import androidx.compose.runtime.composer.linkbuffer.HasMovableContentFlag
import androidx.compose.runtime.composer.linkbuffer.HasSubcompositionContextFlag
import androidx.compose.runtime.composer.linkbuffer.IsMovableContentFlag
import androidx.compose.runtime.composer.linkbuffer.IsNodeFlag
import androidx.compose.runtime.composer.linkbuffer.IsRecompositionRequiredFlag
import androidx.compose.runtime.composer.linkbuffer.IsSubcompositionContextFlag
import androidx.compose.runtime.composer.linkbuffer.KeyInfo
import androidx.compose.runtime.composer.linkbuffer.LAZY_ADDRESS
import androidx.compose.runtime.composer.linkbuffer.LinkAnchor
import androidx.compose.runtime.composer.linkbuffer.NULL_ADDRESS
import androidx.compose.runtime.composer.linkbuffer.NULL_GROUP_HANDLE
import androidx.compose.runtime.composer.linkbuffer.NullAnchor
import androidx.compose.runtime.composer.linkbuffer.SlotTable
import androidx.compose.runtime.composer.linkbuffer.SlotTableAddressSpace
import androidx.compose.runtime.composer.linkbuffer.SlotTableBuilder
import androidx.compose.runtime.composer.linkbuffer.SlotTableReader
import androidx.compose.runtime.composer.linkbuffer.asLinkAnchor
import androidx.compose.runtime.composer.linkbuffer.asLinkBufferSlotTable
import androidx.compose.runtime.composer.linkbuffer.buildTrace
import androidx.compose.runtime.composer.linkbuffer.changelist.ChangeList
import androidx.compose.runtime.composer.linkbuffer.changelist.ComposerChangeListWriter
import androidx.compose.runtime.composer.linkbuffer.changelist.FixupList
import androidx.compose.runtime.composer.linkbuffer.changelist.asLinkBufferChangeList
import androidx.compose.runtime.composer.linkbuffer.compositionGroupOf
import androidx.compose.runtime.composer.linkbuffer.contains
import androidx.compose.runtime.composer.linkbuffer.context
import androidx.compose.runtime.composer.linkbuffer.findLocation
import androidx.compose.runtime.composer.linkbuffer.group
import androidx.compose.runtime.composer.linkbuffer.groupFlags
import androidx.compose.runtime.composer.linkbuffer.groupFlagsChildNodeCount
import androidx.compose.runtime.composer.linkbuffer.groupFlagsNodeCount
import androidx.compose.runtime.composer.linkbuffer.groupParent
import androidx.compose.runtime.composer.linkbuffer.makeGroupHandle
import androidx.compose.runtime.composer.linkbuffer.traceForGroup
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.internal.invokeComposable
import androidx.compose.runtime.internal.persistentCompositionLocalHashMapOf
import androidx.compose.runtime.internal.trace
import androidx.compose.runtime.snapshots.currentSnapshot
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.snapshots.fastToSet
import androidx.compose.runtime.tooling.ComposeStackTrace
import androidx.compose.runtime.tooling.ComposeStackTraceFrame
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionErrorContextImpl
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.tooling.CompositionInstance
import androidx.compose.runtime.tooling.LocalCompositionErrorContext
import androidx.compose.runtime.tooling.LocalInspectionTables
import androidx.compose.runtime.tooling.attachComposeStackTrace
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * A virtual group handle is a handle that can be a group handle for group in the reader or a group
 * the builder. If the extension property `isInsertHandle` is true then the handle in the group is
 * in the builder else it is in the reader. This used to track the updated node counts. If a node is
 * inserted the node count of all nodes (up to a node group) needs to be updated. This tracks the
 * changes to the node groups that will be recorded in the slot table when changes are applied. The
 * updated node count can be obtained by calling `updatedNodeCount()`
 */
private typealias VirtualGroupHandle = GroupHandle

/**
 * Return true if this the virtual handle is for a group that has been inserted (e.g. in the
 * builder) instead of for a group that is in the current slot table (e.g. in the reader)
 *
 * The virtual group uses negative values to indicate that it is a virtual group handle instead of
 * an actual group handle in the range Int.MIN_VALUE until -8.
 */
private val VirtualGroupHandle.isInsertHandle
    get() = this.group < -8

/**
 * Convert a group handle into a insert group handle. Note that -10 is used here as -2 and -1 are
 * valid group addresses and so -10 moves the insert groups out of this range by a safe margin.
 *
 * The virtual group uses negative values to indicate that it is a virtual group handle instead of
 * an actual group handle in the range Int.MIN_VALUE until -8. The -10 here is used as -1 is in
 * bound of the group handle and -10 - -1 is -9 which is the largest value in range.
 */
private fun GroupHandle.toInsertAddress() = makeGroupHandle(this.context, -10 - this.group)

/**
 * Convert a virtual group handle to a group handle. This will throw a debug check if the value is
 * an insert handle.
 */
private inline fun VirtualGroupHandle.toGroupHandle(): GroupHandle {
    debugRuntimeCheck(!isInsertHandle) {
        "A virtual handle to a group in the builder cannot immediately converted ot a group handle"
    }
    return this
}

private inline fun GroupAddress.toGroupHandle(): GroupHandle = makeGroupHandle(LAZY_ADDRESS, this)

/**
 * Pending starts when the key is different than expected indicating that the structure of the tree
 * changed. It is used to determine how to update the nodes and the slot table when changes to the
 * structure of the tree is detected.
 */
private class LinkPending(val keyInfos: MutableList<KeyInfo>, val startIndex: Int) {
    var groupIndex: Int = 0

    /**
     * A bitvector that tracks which child groups of the group associated with this Pending object
     * have been reconciled into their final locations. This is indexed relative to the original
     * order of the child groups. A value of true for a given group index means that the group has
     * been started. It may or may not have needed to be moved into its location recorded during the
     * apply phase.
     */
    private val placedGroups = BitVector()

    init {
        requirePrecondition(startIndex >= 0) { "Invalid start index" }
    }

    private val usedKeys = mutableListOf<KeyInfo>()
    private val groupInfos = run {
        var runningNodeIndex = 0
        val result = MutableIntObjectMap<GroupInfo>()
        for (index in 0 until keyInfos.size) {
            val keyInfo = keyInfos[index]
            result[keyInfo.address] = GroupInfo(index, runningNodeIndex, keyInfo.nodes)
            runningNodeIndex += keyInfo.nodes
        }
        result
    }

    /**
     * A multi-map of keys from the previous composition. The keys can be retrieved in the order
     * they were generated by the previous composition.
     */
    val keyMap by lazy {
        multiMap<Any, KeyInfo>(keyInfos.size).also {
            for (index in 0 until keyInfos.size) {
                val keyInfo = keyInfos[index]
                it.add(keyInfo.joinedKey, keyInfo)
            }
        }
    }

    /** Get the next key information for the given key. */
    fun getNext(key: Int, dataKey: Any?): KeyInfo? {
        val joinedKey: Any = if (dataKey != null) JoinedKey(key, dataKey) else key
        return keyMap.removeFirst(joinedKey)
    }

    /** Record that this key info was generated. */
    fun recordUsed(keyInfo: KeyInfo) = usedKeys.add(keyInfo)

    val used: List<KeyInfo>
        get() = usedKeys

    /**
     * Returns the GroupHandle of the first group (relative to the original ordering of the children
     * before recomposition) that has NOT been accepted via [markGroupLocationReconciled]. This is
     * necessary to track, as the handle returned by this will be an "anchor" which moved groups
     * must be positioned before.
     */
    fun groupHandleOfNextUnmovedGroup() = keyInfos[placedGroups.nextClear(0)].handle

    /**
     * Mark the [index]-th child (based on the original ordering of the children before
     * recomposition) as having all the relevant instructions in the change list to place it in its
     * final location.
     */
    fun markGroupLocationReconciled(index: Int) {
        placedGroups[index] = true
    }

    // TODO(chuckj): This is a correct but expensive implementation (worst cases of O(N^2)). Rework
    // to O(N)
    fun registerMoveSlot(from: Int, to: Int) {
        if (from > to) {
            groupInfos.forEachValue { group ->
                val position = group.slotIndex
                if (position == from) group.slotIndex = to
                else if (position in to until from) group.slotIndex = position + 1
            }
        } else if (to > from) {
            groupInfos.forEachValue { group ->
                val position = group.slotIndex
                if (position == from) group.slotIndex = to
                else if (position in (from + 1) until to) group.slotIndex = position - 1
            }
        }
    }

    fun registerMoveNode(from: Int, to: Int, count: Int) {
        if (from > to) {
            groupInfos.forEachValue { group ->
                val position = group.nodeIndex
                if (position in from until from + count) group.nodeIndex = to + (position - from)
                else if (position in to until from) group.nodeIndex = position + count
            }
        } else if (to > from) {
            groupInfos.forEachValue { group ->
                val position = group.nodeIndex
                if (position in from until from + count) group.nodeIndex = to + (position - from)
                else if (position in (from + 1) until to) group.nodeIndex = position - count
            }
        }
    }

    @OptIn(InternalComposeApi::class)
    fun registerInsert(keyInfo: KeyInfo, insertIndex: Int) {
        groupInfos[keyInfo.address] = GroupInfo(-1, insertIndex, 0)
    }

    fun updateNodeCount(group: GroupAddress, newCount: Int): Boolean {
        val groupInfo = groupInfos[group]
        if (groupInfo != null) {
            val index = groupInfo.nodeIndex
            val difference = newCount - groupInfo.nodeCount
            groupInfo.nodeCount = newCount
            if (difference != 0) {
                groupInfos.forEachValue { childGroupInfo ->
                    if (childGroupInfo.nodeIndex >= index && childGroupInfo != groupInfo) {
                        val newIndex = childGroupInfo.nodeIndex + difference
                        if (newIndex >= 0) childGroupInfo.nodeIndex = newIndex
                    }
                }
            }
            return true
        }
        return false
    }

    @OptIn(InternalComposeApi::class)
    fun slotPositionOf(keyInfo: KeyInfo) = groupInfos[keyInfo.address]?.slotIndex ?: -1

    @OptIn(InternalComposeApi::class)
    fun nodePositionOf(keyInfo: KeyInfo) = groupInfos[keyInfo.address]?.nodeIndex ?: -1

    @OptIn(InternalComposeApi::class)
    fun updatedNodeCountOf(keyInfo: KeyInfo) =
        groupInfos[keyInfo.address]?.nodeCount ?: keyInfo.nodes
}

@OptIn(ExperimentalComposeRuntimeApi::class, InternalComposeApi::class)
@ComposeCompilerApi
internal class LinkComposer(
    override val applier: Applier<*>,
    private val parentContext: CompositionContext,
    private val abandonSet: MutableSet<RememberObserver>,
    private val slotTable: SlotTable,
    private var changes: Changes,
    private var lateChanges: Changes,
    private val observerHolder: CompositionObserverHolder,
    override val composition: CompositionImpl,
) : InternalComposer() {
    private val invalidations = ScopeMap<RecomposeScopeImpl, Any>()
    private val pendingStack = Stack<LinkPending?>()
    private var pending: LinkPending? = null
    private var nodeIndex: Int = 0
    private var groupNodeCount: Int = 0
    private var rGroupIndex: Int = 0
    private val parentStateStack = IntStack()
    private var nodeCountOverrides: MutableIntIntMap? = null
    private var nodeCountVirtualOverrides: MutableIntIntMap? = null
    private var forceRecomposeScopes = false
    private var forciblyRecompose = false
    private var nodeExpected = false
    private val entersStack = IntStack()
    private var rootProvider: PersistentCompositionLocalMap = persistentCompositionLocalHashMapOf()
    private var providerUpdates: MutableIntObjectMap<PersistentCompositionLocalMap>? = null
    private var providersInvalid = false
    private val providersInvalidStack = IntStack()
    private var reusing = false
    private var reusingGroup = NULL_ADDRESS
    private var providerCache: PersistentCompositionLocalMap? = null

    internal var reader: SlotTableReader = slotTable.openReader().also { it.close() }
    private var builder: SlotTableBuilder =
        SlotTableBuilder(
                slotTable.addressSpace,
                recordSourceInformation = false,
                recordCallByInformation = false,
            )
            .also { it.close() }
    private var builderHasAProvider = false
    private val changeListWriter = ComposerChangeListWriter(this, changes.asLinkBufferChangeList())
    private var _compositionData: CompositionData? = null
    private var lastPlacedChildGroup = NULL_ADDRESS
    private var insertFixups = FixupList()
    internal val insertTable: SlotTable
        get() = builder.table

    internal val readerTable: SlotTable
        get() = reader.table

    private var childrenComposing: Int = 0
    private var compositionToken: Int = 0

    override var sourceMarkersEnabled =
        parentContext.collectingSourceInformation || parentContext.collectingCallByInformation

    private val derivedStateObserver =
        object : DerivedStateObserver {
            override fun start(derivedState: DerivedState<*>) {
                childrenComposing++
            }

            override fun done(derivedState: DerivedState<*>) {
                childrenComposing--
            }
        }

    private val invalidateStack = Stack<RecomposeScopeImpl>()

    override var isComposing = false
        private set

    internal var isDisposed = false
        private set

    override val areChildrenComposing
        get() = childrenComposing > 0

    override val currentRecomposeScope: RecomposeScopeImpl?
        get() =
            invalidateStack.let {
                if (childrenComposing == 0 && it.isNotEmpty()) it.peek() else null
            }

    /**
     * Returns the hash of the composite key calculated as a combination of the keys of all the
     * currently started groups via [startGroup].
     */
    @InternalComposeApi
    override var compositeKeyHashCode: CompositeKeyHashCode = EmptyCompositeKeyHashCode
        private set

    override val defaultsInvalid: Boolean
        get() {
            return !skipping || providersInvalid || currentRecomposeScope?.defaultsInvalid == true
        }

    override fun disableReusing() {
        reusing = false
    }

    override fun disableSourceInformation() {
        sourceMarkersEnabled = false
    }

    override fun enableReusing() {
        reusing = reusingGroup >= 0
    }

    override var deferredChanges: Changes? = null

    private var shouldPauseCallback: ShouldPauseCallback? = null

    override val errorContext: CompositionErrorContextImpl? = CompositionErrorContextImpl(this)
        get() = if (sourceMarkersEnabled) field else null

    override fun forceRecomposeScopes(): Boolean {
        return if (!forceRecomposeScopes) {
            forceRecomposeScopes = true
            forciblyRecompose = true
            true
        } else {
            false
        }
    }

    override var inserting: Boolean = false
        private set

    override fun prepareCompose(block: () -> Unit) {
        runtimeCheck(!isComposing) { "Preparing a composition while composing is not supported" }
        isComposing = true
        try {
            block()
        } finally {
            isComposing = false
        }
    }

    override val recomposeScope: RecomposeScope?
        get() = currentRecomposeScope

    override val recomposeScopeIdentity: Any?
        get() = currentRecomposeScope?.anchor

    override fun recordUsed(scope: RecomposeScope) {
        (scope as? RecomposeScopeImpl)?.used = true
    }

    override val skipping: Boolean
        get() {
            return !inserting &&
                !reusing &&
                !providersInvalid &&
                currentRecomposeScope?.requiresRecompose == false &&
                !forciblyRecompose
        }

    /** See [Composer.apply] */
    override fun <V, T> apply(value: V, block: T.(V) -> Unit) {
        if (inserting) {
            insertFixups.updateNode(value, block)
        } else {
            changeListWriter.updateNode(value, block)
        }
    }

    /** See [Composer.applyCoroutineContext] */
    @InternalComposeApi
    override val applyCoroutineContext: CoroutineContext =
        parentContext.effectCoroutineContext + (errorContext ?: EmptyCoroutineContext)

    /** See [Composer.buildContext] */
    @InternalComposeApi
    override fun buildContext(): CompositionContext {
        startGroup(referenceKey, reference)
        if (inserting) builder.addFlags(IsSubcompositionContextFlag)

        var observerHolder = nextSlot() as? ReusableRememberObserverHolder
        if (observerHolder == null) {
            observerHolder =
                ReusableLinkRememberObserverHolder(
                    CompositionContextHolder(
                        CompositionContextImpl(
                            compositeKeyHashCode,
                            forceRecomposeScopes,
                            sourceMarkersEnabled,
                            composition.observerHolder,
                        )
                    ),
                    NullAnchor,
                )
            updateValue(observerHolder)
        }
        val holder = observerHolder.wrapped as CompositionContextHolder
        holder.ref.updateCompositionLocalScope(currentCompositionLocalScope())
        endGroup()

        return holder.ref
    }

    /** See [Composer.changed] */
    override fun changed(value: Any?): Boolean {
        return if (nextSlot() != value) {
            updateValue(value)
            true
        } else {
            false
        }
    }

    /** See [Composer.changed] */
    override fun changedInstance(value: Any?): Boolean {
        return if (nextSlot() !== value) {
            updateValue(value)
            true
        } else {
            false
        }
    }

    /** See [Composer.changed] */
    override fun changed(value: Char): Boolean {
        val next = nextSlot()
        if (next is Char) {
            val nextPrimitive: Char = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    /** See [Composer.changed] */
    override fun changed(value: Byte): Boolean {
        val next = nextSlot()
        if (next is Byte) {
            val nextPrimitive: Byte = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    /** See [Composer.changed] */
    override fun changed(value: Short): Boolean {
        val next = nextSlot()
        if (next is Short) {
            val nextPrimitive: Short = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    /** See [Composer.changed] */
    override fun changed(value: Boolean): Boolean {
        val next = nextSlot()
        if (next is Boolean) {
            val nextPrimitive: Boolean = next
            if (value == nextPrimitive) return false
        }
        updateValue(value)
        return true
    }

    /** See [Composer.changed] */
    override fun changed(value: Float): Boolean {
        val next = nextSlot()
        if (next is Float && value == next) {
            return false
        } else {
            updateValue(value)
            return true
        }
    }

    /** See [Composer.changed] */
    override fun changed(value: Long): Boolean {
        val next = nextSlot()
        if (next is Long && value == next) {
            return false
        } else {
            updateValue(value)
            return true
        }
    }

    /** See [Composer.changed] */
    override fun changed(value: Double): Boolean {
        val next = nextSlot()
        if (next is Double && value == next) {
            return false
        } else {
            updateValue(value)
            return true
        }
    }

    /** See [Composer.changed] */
    override fun changed(value: Int): Boolean {
        val next = nextSlot()
        if (next is Int && value == next) {
            return false
        } else {
            updateValue(value)
            return true
        }
    }

    /** See [Composer.collectParameterInformation] */
    override fun collectParameterInformation() {
        forceRecomposeScopes = true
        sourceMarkersEnabled = true
        slotTable.collectSourceInformation()
        builder.collectSourceInformation()
    }

    /** See [InternalComposer.composeContent] */
    @InternalComposeApi
    override fun composeContent(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>,
        content: @Composable () -> Unit,
        shouldPause: ShouldPauseCallback?,
    ) {
        runtimeCheck(changes.isEmpty()) { "Expected applyChanges() to have been called" }
        this.shouldPauseCallback = shouldPause
        try {
            doCompose(invalidationsRequested, content)
        } finally {
            this.shouldPauseCallback = null
        }
    }

    override val compositionData: CompositionData
        get() {
            val data = _compositionData
            if (data == null) {
                val newData = LinkCompositionDataImpl(composition)
                _compositionData = newData
                return newData
            }
            return data
        }

    /** See [Composer.consume] */
    @InternalComposeApi
    override fun <T> consume(key: CompositionLocal<T>): T = currentCompositionLocalScope().read(key)

    /** See [Composer.createNode] */
    override fun <T> createNode(factory: () -> T) {
        validateNodeExpected()
        runtimeCheck(inserting) { "createNode() can only be called when inserting" }
        val insertIndex = parentStateStack.peek()
        groupNodeCount++
        val handle = builder.parentHandle
        if (changeListWriter.isInAnchorMode) {
            val anchor = builder.table.addressSpace.anchorOfAddress(handle.group)
            insertFixups.createAndInsertNodeByAnchor(factory, insertIndex, anchor)
        } else {
            insertFixups.createAndInsertNode(factory, insertIndex, builder.parentHandle)
        }
    }

    /** See [Composer.currentCompositionLocalMap] */
    override val currentCompositionLocalMap: CompositionLocalMap
        get() = currentCompositionLocalScope()

    /** See [Composer.currentMarker] */
    override val currentMarker: GroupAddress
        get() = if (inserting) -builder.parentGroup else reader.parentGroup

    /** See [InternalComposer.deactivate] */
    override fun deactivate() {
        invalidateStack.clear()
        invalidations.clear()
        changes.clear()
        providerUpdates = null
    }

    /** See [Composer.deactivateToEndGroup] */
    override fun deactivateToEndGroup(changed: Boolean) {
        runtimeCheck(groupNodeCount == 0) {
            "No nodes can be emitted before calling deactivateToEndGroup"
        }
        if (!inserting) {
            if (!changed) {
                skipReaderToGroupEnd()
                return
            }
            changeListWriter.deactivateCurrentGroup()
            reader.skipToGroupEnd()
        }
    }

    /** See [InternalComposer.dispose] */
    override fun dispose() {
        slotTable.dispose()
    }

    /** See [Composer.endDefaults] */
    override fun endDefaults() {
        endGroup()
        val scope = currentRecomposeScope
        if (scope != null && scope.used) {
            scope.defaultsInScope = true
        }
    }

    /** See [Composer.endNode] */
    override fun endNode() = end(isNode = true)

    /** See [Composer.endProvider] */
    @InternalComposeApi
    override fun endProvider() {
        endGroup()
        endGroup()
        providersInvalid = providersInvalidStack.pop().asBool()
        providerCache = null
    }

    /** See [Composer.endProviders] */
    @InternalComposeApi
    override fun endProviders() {
        endGroup()
        endGroup()
        providersInvalid = providersInvalidStack.pop().asBool()
        providerCache = null
    }

    /** See [Composer.endReplaceableGroup] */
    override fun endReplaceableGroup() = endGroup()

    /** See [Composer.endRestartGroup] */
    override fun endRestartGroup(): ScopeUpdateScope? {
        // This allows for the invalidate stack to be out of sync since this might be called during
        // exception stack unwinding that might have not called the doneJoin/endRestartGroup in the
        // the correct order.
        val scope = if (invalidateStack.isNotEmpty()) invalidateStack.pop() else null
        if (scope != null) {
            scope.requiresRecompose = false
            exitRecomposeScope(scope)?.let { changeListWriter.endCompositionScope(it, composition) }
            if (scope.resuming) {
                scope.resuming = false
                changeListWriter.endResumingScope(scope)
                scope.reusing = false
                if (scope.resetReusing && reusingGroup == reader.parentGroup) {
                    scope.resetReusing = false
                    reusingGroup = NULL_ADDRESS
                    reusing = false
                }
            }
        }
        val result =
            if (scope != null && !scope.skipped && (scope.used || forceRecomposeScopes)) {
                if (scope.anchor == null) {
                    scope.anchor =
                        if (inserting) {
                            builder.parentAnchor
                        } else {
                            reader.parentAnchor
                        }
                }
                scope.defaultsInvalid = false
                scope
            } else {
                null
            }
        end(isNode = false)
        return result
    }

    /** See [Composer.endReplaceGroup] */
    @ComposeCompilerApi override fun endReplaceGroup() = endGroup()

    /** See [InternalComposer.endReusableGroup] */
    override fun endReusableGroup() {
        if (reusing && reader.parentGroup == reusingGroup) {
            reusingGroup = NULL_ADDRESS
            reusing = false
        }
        end(isNode = false)
    }

    /** See [InternalComposer.endReuseFromRoot] */
    override fun endReuseFromRoot() {
        val reusingGroupKey = reusingGroup.let { if (it < 0) rootKey else reader.groupKey(it) }
        requirePrecondition(!isComposing && reusingGroupKey == rootKey) {
            "Cannot disable reuse from root if it was caused by other groups"
        }
        reusingGroup = NULL_ADDRESS
        reusing = false
    }

    /** See [Composer.endMovableGroup] */
    override fun endMovableGroup() = endGroup()

    /** See [Composer.endToMarker] */
    override fun endToMarker(marker: GroupAddress) {
        if (marker < 0) {
            // If the marker is negative then the marker is for the writer
            val writerLocation = -marker
            val builder = builder
            val targetParents =
                MutableIntSet().apply {
                    readerTable.traverseGroupAndParents(writerLocation) { parent -> add(parent) }
                }

            while (builder.parentGroup !in targetParents) {
                end(builder.isNode())
            }
        } else {
            // If the marker is positive then the marker is for the reader. However, if we are
            // inserting then we need to close the inserting groups first.
            if (inserting) {
                // We might be inserting, we need to close all the groups until we are no longer
                // inserting.
                val builder = builder
                while (inserting) {
                    end(builder.isNode())
                }
            }

            val markerParents =
                MutableIntSet().apply {
                    readerTable.traverseGroupAndParents(marker) { parent -> add(parent) }
                }

            val reader = reader
            var parent = reader.parentGroup
            while (parent !in markerParents) {
                end(IsNodeFlag in reader.flagsOf(parent))
                parent = reader.parentGroup
            }
        }
    }

    /** See [InternalComposer.hasPendingChanges] */
    override val hasPendingChanges: Boolean
        get() = changes.isNotEmpty()

    /** See [Composer.insertMovableContent] */
    @InternalComposeApi
    override fun insertMovableContent(value: MovableContent<*>, parameter: Any?) {
        @Suppress("UNCHECKED_CAST")
        invokeMovableContentLambda(
            value as MovableContent<Any?>,
            currentCompositionLocalScope(),
            parameter,
            force = false,
        )
    }

    /** See [Composer.insertMovableContentReferences] */
    @InternalComposeApi
    override fun insertMovableContentReferences(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    ) {
        var completed = false
        try {
            insertMovableContentGuarded(references)
            completed = true
        } finally {
            if (completed) {
                cleanUpCompose()
            } else {
                // if we finished with error, cleanup more aggressively
                abortRoot()
            }
        }
    }

    /** See [Composer.joinKey] */
    override fun joinKey(left: Any?, right: Any?): Any =
        getKey(if (inserting) null else reader.groupObjectKey, left, right)
            ?: JoinedKey(left, right)

    /** See [InternalComposer.parentKey] */
    @TestOnly
    override fun parentKey(): Int {
        return if (inserting) {
            builder.groupKey(builder.parentGroup)
        } else {
            reader.groupKey(reader.parentGroup)
        }
    }

    /** See [InternalComposer.parentStackTrace] */
    override fun parentStackTrace(): List<ComposeStackTraceFrame> {
        val parentComposition = parentContext.composition as? CompositionImpl ?: return emptyList()
        val position =
            parentComposition.slotStorage
                .asLinkBufferSlotTable()
                .findSubcompositionContextGroup(parentContext)

        return if (position != null) {
            parentComposition.slotStorage.asLinkBufferSlotTable().read {
                traceForGroup(position, 0)
            } + parentComposition.composer.parentStackTrace()
        } else {
            emptyList()
        }
    }

    /** See [InternalComposer.recompose] */
    @InternalComposeApi
    override fun recompose(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>,
        shouldPause: ShouldPauseCallback?,
    ): Boolean {
        runtimeCheck(changes.isEmpty()) { "Expected applyChanges() to have been called" }
        // even if invalidationsRequested is empty we still need to recompose if the Composer has
        // some invalidations scheduled already. it can happen when during some parent composition
        // there were a change for a state which was used by the child composition. such changes
        // will be tracked and added into `invalidations` list.
        if (
            invalidationsRequested.size > 0 ||
                invalidations.isNotEmpty() ||
                (slotTable.root >= 0 && requiresRecomposition(slotTable.root)) ||
                forciblyRecompose
        ) {
            shouldPauseCallback = shouldPause
            try {
                changeListWriter.startComposition()
                doCompose(invalidationsRequested, null)
            } finally {
                shouldPauseCallback = null
            }
            if (changes.asLinkBufferChangeList().hasChangesRequiringApplication()) {
                return true
            } else if (changes.isNotEmpty()) {
                executeChangesImmediatelyWithoutApplier()
            }
        }
        return false
    }

    /** See [Composer.recordSideEffect] */
    @InternalComposeApi
    override fun recordSideEffect(effect: () -> Unit) {
        changeListWriter.sideEffect(effect)
    }

    /** See [Composer.rememberedValue] */
    override fun rememberedValue(): Any? = nextSlotForCache().unwrapRememberObserverHolder()

    /** See [Composer.shouldExecute] */
    @InternalComposeApi
    override fun shouldExecute(parametersChanged: Boolean, flags: Int): Boolean {
        // We only want to pause when we are not resuming and only when inserting new content or
        // when reusing content. This 0 bit of `flags` is only 1 if this function was restarted by
        // the restart lambda. The other bits of this flags are currently all 0's and are reserved
        // for future use.
        if (((flags and 1) == 0) && (inserting || reusing)) {
            val callback = shouldPauseCallback ?: return true
            val scope = currentRecomposeScope ?: return true
            val pausing = callback.shouldPause()
            if (pausing && !scope.resuming) {
                scope.used = true
                // Force the composer back into the reusing state when this scope restarts.
                scope.reusing = reusing
                scope.paused = true
                // Remember a place-holder object to ensure all remembers are sent in the correct
                // order. The remember manager will record the remember callback for the resumed
                // content into a place-holder to ensure that, when the remember callbacks are
                // dispatched, the callbacks for the resumed content are dispatched in the same
                // order they would have been had the content not paused.
                changeListWriter.rememberPausingScope(scope)
                parentContext.reportPausedScope(scope)
                return false
            }
            return true
        }

        // Otherwise we should execute the function if the parameters have changed or when
        // skipping is disabled.
        return parametersChanged || !skipping
    }

    /** See [Composer.skipCurrentGroup] */
    @InternalComposeApi
    @ComposeCompilerApi
    override fun skipCurrentGroup() {
        if (!requiresRecomposition(reader.currentGroup)) {
            skipGroup()
        } else {
            val reader = reader
            val key = reader.groupKey
            val dataKey = reader.groupObjectKey
            val aux = reader.groupAux
            val rGroupIndex = rGroupIndex
            updateCompositeKeyWhenWeEnterGroup(key, rGroupIndex, dataKey, aux)
            startReaderGroup(reader.isNode, null)
            recomposeToGroupEnd()
            reader.endGroup()
            updateCompositeKeyWhenWeExitGroup(key, rGroupIndex, dataKey, aux)
        }
    }

    /** See [Composer.skipToGroupEnd] */
    @InternalComposeApi
    override fun skipToGroupEnd() {
        runtimeCheck(groupNodeCount == 0) {
            "No nodes can be emitted before calling skipAndEndGroup"
        }

        // This can be called when inserting is true and `shouldExecute` returns false.
        // When `inserting` the writer is already at the end of the group so we don't need to
        // move the writer.
        if (!inserting) {
            currentRecomposeScope?.scopeSkipped()
            if (reader.currentGroup < 0 || !requiresRecomposition(reader.parentGroup)) {
                skipReaderToGroupEnd()
            } else {
                recomposeToGroupEnd()
            }
        }
    }

    /** See [Composer.scheduleFrameEndCallback] */
    override fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle {
        return parentContext.scheduleFrameEndCallback(action)
    }

    /** See [Composer.sourceInformation] */
    override fun sourceInformation(sourceInformation: String) {
        if (inserting && sourceMarkersEnabled) {
            builder.recordGroupSourceInformation(sourceInformation)
        }
    }

    /** See [Composer.sourceInformationMarkerEnd] */
    override fun sourceInformationMarkerEnd() {
        if (inserting && sourceMarkersEnabled) {
            builder.recordGrouplessCallSourceInformationEnd()
        }
    }

    /** See [Composer.sourceInformationMarkerStart] */
    override fun sourceInformationMarkerStart(key: Int, sourceInformation: String) {
        if (inserting && sourceMarkersEnabled) {
            builder.recordGrouplessCallSourceInformationStart(key, sourceInformation)
        }
    }

    // This is only used in tests to ensure the stacks do not silently leak.
    /** See [InternalComposer.stacksSize] */
    override fun stacksSize(): Int {
        return entersStack.size +
            invalidateStack.size +
            providersInvalidStack.size +
            pendingStack.size +
            parentStateStack.size
    }

    /** See [InternalComposer.stackTraceForValue] */
    override fun stackTraceForValue(value: Any?): ComposeStackTrace {
        if (!sourceMarkersEnabled) return ComposeStackTrace(emptyList(), false)

        return ComposeStackTrace(
            slotTable
                .findLocation { it === value || (it as? RememberObserverHolder)?.wrapped === value }
                ?.let { (groupIndex, dataIndex) ->
                    stackTraceForGroup(groupIndex, dataIndex) + parentStackTrace()
                } ?: emptyList(),
            sourceMarkersEnabled,
        )
    }

    /** See [Composer.startDefaults] */
    override fun startDefaults() = start(defaultsKey, null, GroupKind.Group, null)

    /** See [Composer.startNode] */
    override fun startNode() {
        start(nodeKey, null, GroupKind.Node, null)
        nodeExpected = true
    }

    /** See [Composer.startProvider] */
    @Suppress("UNCHECKED_CAST")
    @InternalComposeApi
    override fun startProvider(value: ProvidedValue<*>) {
        val parentScope = currentCompositionLocalScope()
        startGroup(providerKey, provider)
        val oldState =
            rememberedValue().let { if (it == Composer.Empty) null else it as ValueHolder<Any?> }
        val local = value.compositionLocal as CompositionLocal<Any?>
        val state = local.updatedStateOf(value as ProvidedValue<Any?>, oldState)
        val change = state != oldState
        if (change) {
            updateRememberedValue(state)
        }
        val providers: PersistentCompositionLocalMap
        val invalid: Boolean
        if (inserting) {
            providers =
                if (value.canOverride || !parentScope.contains(local)) {
                    parentScope.putValue(local, state)
                } else {
                    parentScope
                }
            invalid = false
            builderHasAProvider = true
        } else {
            val oldScope = reader.groupAux(reader.currentGroup) as PersistentCompositionLocalMap
            providers =
                when {
                    (!skipping || change) && (value.canOverride || !parentScope.contains(local)) ->
                        parentScope.putValue(local, state)
                    !change && !providersInvalid -> oldScope
                    providersInvalid -> parentScope
                    else -> oldScope
                }
            invalid = reusing || oldScope !== providers
        }
        if (invalid && !inserting) {
            recordProviderUpdate(providers)
        }
        providersInvalidStack.push(providersInvalid.asInt())
        providersInvalid = invalid
        providerCache = providers
        start(compositionLocalMapKey, compositionLocalMap, GroupKind.Group, providers)
    }

    @InternalComposeApi
    override fun startProviders(values: Array<out ProvidedValue<*>>) {
        val parentScope = currentCompositionLocalScope()
        startGroup(providerKey, provider)
        val providers: PersistentCompositionLocalMap
        val invalid: Boolean
        if (inserting) {
            val currentProviders = updateCompositionMap(values, parentScope)
            providers = updateProviderMapGroup(parentScope, currentProviders)
            invalid = false
            builderHasAProvider = true
        } else {
            val oldScope = reader.get(0) as PersistentCompositionLocalMap
            val oldValues = reader.get(1) as PersistentCompositionLocalMap
            val currentProviders = updateCompositionMap(values, parentScope, oldValues)
            // skipping is true iff parentScope has not changed.
            if (!skipping || reusing || oldValues != currentProviders) {
                providers = updateProviderMapGroup(parentScope, currentProviders)

                // Compare against the old scope as currentProviders might have modified the scope
                // back to the previous value. This could happen, for example, if currentProviders
                // and parentScope have a key in common and the oldScope had the same value as
                // currentProviders for that key. If the scope has not changed, because these
                // providers obscure a change in the parent as described above, re-enable skipping
                // for the child region.
                invalid = reusing || providers != oldScope
            } else {
                // Nothing has changed
                skipGroup()
                providers = oldScope
                invalid = false
            }
        }

        if (invalid && !inserting) {
            recordProviderUpdate(providers)
        }
        providersInvalidStack.push(providersInvalid.asInt())
        providersInvalid = invalid
        providerCache = providers
        start(compositionLocalMapKey, compositionLocalMap, GroupKind.Group, providers)
    }

    /** See [Composer.startReplaceableGroup] */
    override fun startReplaceableGroup(key: Int) = start(key, null, GroupKind.Group, null)

    /** See [Composer.startReplaceGroup] */
    override fun startReplaceGroup(key: Int) {
        val pending = pending
        if (pending != null) {
            start(key, null, GroupKind.Group, null)
            return
        }
        validateNodeNotExpected()

        updateCompositeKeyWhenWeEnterGroup(key, rGroupIndex, null, null)

        rGroupIndex++

        val reader = reader
        if (inserting) {
            reader.beginEmpty()
            builder.startGroup(key, Composer.Empty)
            enterGroup(false, null)
            return
        }
        val slotKey = reader.groupKey
        if (slotKey == key && !reader.hasObjectKey) {
            reader.startGroup()
            enterGroup(false, null)
            return
        }

        if (!reader.isGroupEnd) {
            // Delete the group that was not expected
            val removeIndex = nodeIndex
            recordDelete()
            val nodesToRemove = reader.skipGroup()
            changeListWriter.removeNode(removeIndex, nodesToRemove)
        }

        // Insert the new group
        reader.beginEmpty()
        inserting = true
        providerCache = null
        ensureBuilder()
        builder.startGroup(key, Composer.Empty)
        enterGroup(false, null)
    }

    /** See [Composer.startRestartGroup] */
    override fun startRestartGroup(key: Int): Composer {
        startReplaceGroup(key)
        addRecomposeScope()
        return this
    }

    /** See [Composer.startReusableGroup] */
    override fun startReusableGroup(key: Int, dataKey: Any?) {
        if (
            !inserting && reader.groupKey == key && reader.groupAux != dataKey && reusingGroup < 0
        ) {
            // Starting to reuse nodes
            reusingGroup = reader.currentGroup
            reusing = true
        }
        start(key, null, GroupKind.Group, dataKey)
    }

    /** See [Composer.startReusableNode] */
    override fun startReusableNode() {
        start(nodeKey, null, GroupKind.ReusableNode, null)
        nodeExpected = true
    }

    /** See [InternalComposer.startReuseFromRoot] */
    override fun startReuseFromRoot() {
        reusingGroup = slotTable.root
        reusing = true
    }

    /** See [Composer.startMovableGroup] */
    override fun startMovableGroup(key: Int, dataKey: Any?) =
        start(key, dataKey, GroupKind.Group, null)

    /** See [InternalComposer.tryImminentInvalidation] */
    override fun tryImminentInvalidation(scope: RecomposeScopeImpl, instance: Any?): Boolean {
        val anchor = scope.anchor ?: return false
        val address = anchor.asLinkAnchor().address
        if (address < 0 || !isComposing) return false

        if (isGroupAfterCurrentReaderPosition(address.toGroupHandle())) {
            // if we are invalidating a scope that is going to be traversed during this
            // composition.
            reader.addFlag(address, IsRecompositionRequiredFlag)
            when (instance) {
                null,
                ScopeInvalidated -> {
                    invalidations.set(scope, ScopeInvalidated)
                }
                is ScatterSet<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    invalidations.addAll(scope, instance as ScatterSet<Any>)
                }
                else -> {
                    if (invalidations[scope] != ScopeInvalidated) {
                        invalidations.add(scope, instance)
                    }
                }
            }
            return true
        }
        return false
    }

    /** See [InternalComposer.updateComposerInvalidations] */
    override fun updateComposerInvalidations(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>
    ) {
        // Add the requested invalidations
        invalidationsRequested.map.forEach { scope, instances ->
            scope as RecomposeScopeImpl
            val anchor = scope.anchor?.asLinkAnchor()
            if (anchor == null || !anchor.valid) return@forEach
            val address = anchor.address
            reader.addFlag(address, IsRecompositionRequiredFlag)
            when (instances) {
                ScopeInvalidated -> invalidations.set(scope, ScopeInvalidated)
                is MutableScatterSet<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    instances as MutableScatterSet<Any>
                    invalidations.addAll(scope, instances)
                }
                else -> invalidations.add(scope, instances)
            }
            reader.addFlag(address, IsRecompositionRequiredFlag)
        }
    }

    /** See [Composer.updateRememberedValue] */
    override fun updateRememberedValue(value: Any?) = updateCachedValue(value)

    /** See [Composer.useNode] */
    override fun useNode() {
        validateNodeExpected()
        runtimeCheck(!inserting) { "useNode() called while inserting" }
        val node = reader.parentNode
        changeListWriter.moveDown(node)

        if (reusing && node is ComposeNodeLifecycleCallback) {
            changeListWriter.useNode(node)
        }
    }

    /** See [InternalComposer.verifyConsistent] */
    override fun verifyConsistent() {
        if (!isComposing) {
            insertTable.verifyWellFormed()
        }
    }

    internal fun nextSlot(): Any? =
        if (inserting) {
            validateNodeNotExpected()
            Composer.Empty
        } else
            reader.next().let {
                if (reusing && it !is ReusableRememberObserverHolder) Composer.Empty else it
            }

    internal fun nextSlotForCache(): Any? {
        return if (inserting) {
            validateNodeNotExpected()
            Composer.Empty
        } else
            reader.next().let {
                if (reusing && it !is ReusableRememberObserverHolder) Composer.Empty
                else if (it is RememberObserverHolder) {
                    it.apply {
                        changeListWriter.updateRememberOrdering(
                            holder = it.asLinkRememberObserverHolder(),
                            after = readerTable.addressSpace.anchorOfAddress(lastPlacedChildGroup),
                        )
                    }
                } else it
            }
    }

    @PublishedApi
    @OptIn(InternalComposeApi::class)
    internal fun updateValue(value: Any?) {
        if (inserting) {
            builder.append(value)
        } else {
            if (reader.hadNext) {
                // We need to update the slot we just read so which is is one previous to the
                // current group slot index.
                changeListWriter.updateValue(reader.parentCurrentSlotOffset - 1, value)
            } else {
                changeListWriter.appendValue(value)
            }
        }
    }

    /** Discard a pending composition because an error was encountered during composition */
    @OptIn(InternalComposeApi::class)
    private fun abortRoot() {
        cleanUpCompose()
        pendingStack.clear()
        parentStateStack.clear()
        entersStack.clear()
        providersInvalidStack.clear()
        providerUpdates = null
        insertFixups.clear()
        compositeKeyHashCode = CompositeKeyHashCode(0)
        childrenComposing = 0
        nodeExpected = false
        inserting = false
        reusing = false
        isComposing = false
        forciblyRecompose = false
        reusingGroup = NULL_ADDRESS
        if (!reader.isClosed) {
            reader.close()
        }
        resetInsertBuilder(dispose = false)
    }

    private fun addRecomposeScope() {
        if (inserting) {
            val scope = RecomposeScopeImpl(owner = composition)
            invalidateStack.push(scope)
            updateValue(scope)
            enterRecomposeScope(scope)
        } else {
            val parentGroup = reader.parentGroup
            val parentRecomposeScope = reader.getRecomposeScopeOrNull(parentGroup)
            val invalidation = parentRecomposeScope?.let { invalidations.remove(it) }
            val wasInvalidated = reader.recomposeRequired(parentGroup)
            if (wasInvalidated) {
                reader.removeFlag(IsRecompositionRequiredFlag)
            }
            val slot = reader.next()
            val scope =
                if (slot == Composer.Empty) {
                    // This code is executed when a previously deactivate region is becomes active
                    // again. See Composer.deactivateToEndGroup()
                    val newScope = RecomposeScopeImpl(owner = composition)
                    updateValue(newScope)
                    newScope
                } else slot as RecomposeScopeImpl
            scope.requiresRecompose =
                wasInvalidated ||
                    invalidation != null ||
                    scope.forcedRecompose.also { forced ->
                        if (forced) scope.forcedRecompose = false
                    }
            invalidateStack.push(scope)
            enterRecomposeScope(scope)

            if (scope.paused) {
                scope.paused = false
                scope.resuming = true
                changeListWriter.startResumingScope(scope)
                if (!reusing && scope.reusing) {
                    reusing = true
                    reusingGroup = reader.parentGroup
                    scope.resetReusing = true
                }
            }
        }
    }

    private fun cleanUpCompose() {
        pending = null
        nodeIndex = 0
        groupNodeCount = 0
        compositeKeyHashCode = EmptyCompositeKeyHashCode
        nodeExpected = false
        invalidateStack.clear()
        clearUpdatedNodeCounts()
    }

    private fun clearUpdatedNodeCounts() {
        nodeCountOverrides = null
        nodeCountVirtualOverrides = null
    }

    private fun currentStackTrace(): ComposeStackTrace? =
        if (sourceMarkersEnabled) {
            ComposeStackTrace(
                buildList {
                    addAll(builder.buildTrace())
                    addAll(reader.buildTrace())
                    addAll(parentStackTrace())
                },
                sourceMarkersEnabled,
            )
        } else {
            null
        }

    @InternalComposeApi
    private fun doCompose(
        invalidationsRequested: ScopeMap<RecomposeScopeImpl, Any>,
        content: (@Composable () -> Unit)?,
    ) {
        runtimeCheck(!isComposing) { "Reentrant composition is not supported" }
        val observer = observerHolder.current()
        trace("Compose:recompose") {
            compositionToken = currentSnapshot().snapshotId.hashCode()
            providerUpdates = null
            updateComposerInvalidations(invalidationsRequested)
            nodeIndex = 0
            var complete = false
            isComposing = true
            observer?.onBeginComposition(composition)
            try {
                startRoot()

                // vv Experimental for forced
                val savedContent = nextSlot()
                if (savedContent !== content && content != null) {
                    updateValue(content as Any?)
                }
                // ^^ Experimental for forced

                // Ignore reads of derivedStateOf recalculations
                observeDerivedStateRecalculations(derivedStateObserver) {
                    if (content != null) {
                        startGroup(invocationKey, invocation)
                        invokeComposable(this, content)
                        endGroup()
                    } else if (
                        (forciblyRecompose || providersInvalid) &&
                            savedContent != null &&
                            savedContent != Composer.Empty
                    ) {
                        startGroup(invocationKey, invocation)
                        @Suppress("UNCHECKED_CAST")
                        invokeComposable(this, savedContent as @Composable () -> Unit)
                        endGroup()
                    } else {
                        skipCurrentGroup()
                    }
                }
                endRoot()
                complete = true
            } catch (e: Throwable) {
                throw e.attachComposeStackTrace { currentStackTrace() }
            } finally {
                observer?.onEndComposition(composition)
                isComposing = false
                if (!complete) abortRoot()
                resetInsertBuilder(dispose = !complete)
            }
        }
    }

    private fun end(isNode: Boolean) {
        // All the changes to the group (or node) have been recorded. All new nodes have been
        // inserted but it has yet to determine which need to be removed or moved. Note that the
        // changes are relative to the first change in the list of nodes that are changing.

        // The rGroupIndex for parent is two pack from the current stack top which has already been
        // incremented past this group needs to be offset by one.
        val rGroupIndex = parentStateStack.peek2() - 1
        if (inserting) {
            val parent = builder.parentGroup
            updateCompositeKeyWhenWeExitGroup(
                builder.groupKey(parent),
                rGroupIndex,
                builder.groupObjectKey(parent),
                builder.groupAux(parent),
            )
        } else {
            val parent = reader.parentGroup
            updateCompositeKeyWhenWeExitGroup(
                reader.groupKey(parent),
                rGroupIndex,
                reader.groupObjectKey(parent),
                reader.groupAux(parent),
            )
        }
        var expectedNodeCount = groupNodeCount
        val pending = pending
        if (pending != null && pending.keyInfos.isNotEmpty()) {
            // previous contains the list of keys as they were generated in the previous composition
            val previous = pending.keyInfos

            // current contains the list of keys in the order they need to be in the new composition
            val current = pending.used

            // usedKeys contains the keys that were used in the new composition, therefore if a key
            // doesn't exist in this set, it needs to be removed.
            val usedKeys = current.fastToSet()

            val placedKeys = mutableSetOf<KeyInfo>()
            var currentIndex = 0
            val currentEnd = current.size
            var previousIndex = 0
            val previousEnd = previous.size

            // Traverse the list of changes to determine startNode movement
            var nodeOffset = 0
            while (previousIndex < previousEnd) {
                val previousInfo = previous[previousIndex]
                if (!usedKeys.contains(previousInfo)) {
                    // If the key info was not used the group was deleted, remove the nodes in the
                    // group
                    val deleteOffset = pending.nodePositionOf(previousInfo)
                    changeListWriter.removeNode(
                        nodeIndex = deleteOffset + pending.startIndex,
                        count = previousInfo.nodes,
                    )
                    pending.updateNodeCount(previousInfo.address, 0)
                    reader.reposition(previousInfo.handle)
                    recordDelete()
                    reader.skipGroup()
                    previousIndex++
                    continue
                }

                if (previousInfo in placedKeys) {
                    // If the group was already placed in the correct location, skip it.
                    previousIndex++
                    continue
                }

                if (currentIndex < currentEnd) {
                    // At this point current should match previous unless the group is new or was
                    // moved.
                    val currentInfo = current[currentIndex]
                    if (currentInfo !== previousInfo) {
                        val nodePosition = pending.nodePositionOf(currentInfo)
                        placedKeys.add(currentInfo)
                        if (nodePosition != nodeOffset) {
                            val updatedCount = pending.updatedNodeCountOf(currentInfo)
                            changeListWriter.moveNode(
                                fromNodeIndex = nodePosition + pending.startIndex,
                                toNodeIndex = nodeOffset + pending.startIndex,
                                count = updatedCount,
                            )
                            pending.registerMoveNode(nodePosition, nodeOffset, updatedCount)
                        } // else the nodes are already in the correct position
                    } else {
                        // The correct nodes are in the right location
                        previousIndex++
                    }
                    currentIndex++
                    nodeOffset += pending.updatedNodeCountOf(currentInfo)
                }
            }

            // If there are any current nodes left they where inserted into the right location
            // when the group began so the rest are ignored.
            changeListWriter.endNodeMovement()

            // We have now processed the entire list so move the slot table to the end of the group
            if (previous.isNotEmpty()) {
                reader.skipToGroupEnd()
            }
        }

        val inserting = inserting
        // Detect removing nodes at the end. No pending is created in this case we just have more
        // nodes in the previous composition than we expect (i.e. we are not yet at an end)
        if (!inserting) {
            val removeIndex = nodeIndex
            var predecessor = reader.previousSibling
            // Remove nodes and release movableContent first. We'll delete the group data next.
            readerTable.traverseSiblings(reader.currentGroup) { group ->
                reportFreeMovableContent(makeGroupHandle(reader.parentGroup, predecessor, group))
                val nodesToRemove = reader.nodeCount(group)
                changeListWriter.removeNode(removeIndex, nodesToRemove)
                changeListWriter.endNodeMovement()
                predecessor = group
            }

            // Remove the remaining unused slots and groups at the same time. This needs to happen
            // simultaneously in case both a `remember` and a child group with a `remember` are
            // being removed at the same time. This allows us to traverse the forgotten values in
            // the same pass, which is required to realize the correct forgotten order of the
            // removed slots. Due to code generation issues (b/346821372) this may also see
            // remembers that were removed prior to the children being called so this must be done
            // before the children are deleted to ensure that the `RememberEventDispatcher` receives
            // the `leaving()` call in the correct order so the `onForgotten` is dispatched in the
            // correct order for the values being removed.
            changeListWriter.removeTailGroupsAndValues(
                firstTailGroupToRemove = reader.currentGroup,
                count = reader.remainingSlots,
            )
        }

        if (inserting) {
            if (isNode) {
                insertFixups.endNodeInsert()
                expectedNodeCount = 1
            }
            lastPlacedChildGroup = builder.parentGroup
            reader.endEmpty()
            builder.endGroup()
            if (!reader.inEmpty) {
                val insertSrcAddress = builder.lastRoot()
                recordInsert(insertSrcAddress)
                this.inserting = false
                if (!readerTable.isEmpty) {
                    val insertedGroup = insertSrcAddress.toInsertAddress()
                    updateChildNodeCount(insertedGroup, 0)
                    updateNodeCountOverrides(insertedGroup, expectedNodeCount)
                }
            }
        } else {
            if (isNode) changeListWriter.moveUp()
            val parentGroup = reader.parentHandle
            val parentNodeCount = updatedNodeCount(parentGroup)
            if (expectedNodeCount != parentNodeCount) {
                updateNodeCountOverrides(parentGroup, expectedNodeCount)
            }
            if (isNode) {
                expectedNodeCount = 1
            }

            lastPlacedChildGroup = parentGroup.group
            reader.endGroup()
            changeListWriter.endNodeMovement()
        }

        exitGroup(expectedNodeCount, inserting)
    }

    /** End the current group. */
    private fun endGroup() = end(isNode = false)

    /**
     * End the composition. This should be called, and only be called, to end the first group in the
     * composition.
     */
    @OptIn(InternalComposeApi::class)
    private fun endRoot() {
        endGroup()
        parentContext.doneComposing()
        endGroup()
        finalizeCompose()
        reader.close()
        forciblyRecompose = false
        providersInvalid = providersInvalidStack.pop().asBool()
    }

    override fun changesApplied() {
        providerUpdates = null
    }

    private fun enterGroup(isNode: Boolean, newPending: LinkPending?) {
        // When entering a group all the information about the parent should be saved, to be
        // restored when end() is called, and all the tracking counters set to initial state for the
        // group.
        pendingStack.push(pending)
        this.pending = newPending
        this.parentStateStack.push(groupNodeCount)
        this.parentStateStack.push(rGroupIndex)
        this.parentStateStack.push(nodeIndex)
        if (isNode) nodeIndex = 0
        groupNodeCount = 0
        rGroupIndex = 0
        lastPlacedChildGroup = NULL_ADDRESS
    }

    /**
     * Executes the changes in [changes] synchronously and immediately, outside of the normal flow
     * in `Composition.applyChangesInLocked`. This can only be called when [changes] only contains
     * operations that have no effect on the composition or applier — effectively, this means the
     * changes exclusively operate on the SlotTable in a way that's not externally visible.
     *
     * Currently, this function is only used to clear
     * [androidx.compose.runtime.composer.linkbuffer.IsRecompositionRequiredFlag] from groups after
     * performing a recomposition that did not result in a change to the composition.
     *
     * If any change attempts to act on the Applier or RememberManager, an exception will be thrown.
     */
    private fun executeChangesImmediatelyWithoutApplier() {
        slotTable.edit {
            changes
                .asLinkBufferChangeList()
                .executeAndFlushAllPendingChanges(
                    slots = this,
                    applier = ThrowingApplierStub,
                    rememberManager = ThrowingRememberManagerStub,
                    errorContext = errorContext,
                )
        }
    }

    private fun exitGroup(expectedNodeCount: Int, inserting: Boolean) {
        // Restore the parent's state updating them if they have changed based on changes in the
        // children. For example, if a group generates nodes then the number of generated nodes will
        // increment the node index and the group's node count. If the parent is tracking structural
        // changes in pending then restore that too.
        val previousPending = pendingStack.pop()
        if (previousPending != null && !inserting) {
            previousPending.groupIndex++
        }
        this.pending = previousPending
        this.nodeIndex = parentStateStack.pop() + expectedNodeCount
        this.rGroupIndex = parentStateStack.pop()
        this.groupNodeCount = parentStateStack.pop() + expectedNodeCount
    }

    private fun ensureBuilder() {
        if (builder.isClosed) {
            builder =
                SlotTableBuilder(
                    slotTable.addressSpace,
                    slotTable.recordSourceInformation,
                    slotTable.recordCallByInformation,
                )
            builder.buildStart()
            builderHasAProvider = false
            providerCache = null
        }
    }

    private fun finalizeCompose() {
        changeListWriter.finalizeComposition()
        runtimeCheck(pendingStack.isEmpty()) { "Start/end imbalance" }
        cleanUpCompose()
    }

    private fun currentCompositionLocalScope(): PersistentCompositionLocalMap {
        providerCache?.let {
            return it
        }
        return currentCompositionLocalScope(reader.parentGroup)
    }

    /** Return the current [CompositionLocal] scope which was provided by a parent group. */
    private fun currentCompositionLocalScope(group: GroupAddress): PersistentCompositionLocalMap {
        if (inserting && builderHasAProvider) {
            var current = builder.parentGroup
            while (current >= 0) {
                if (
                    builder.groupKey(current) == compositionLocalMapKey &&
                        builder.groupObjectKey(current) == compositionLocalMap
                ) {
                    val providers = builder.groupAux(current) as PersistentCompositionLocalMap
                    providerCache = providers
                    return providers
                }
                current = builder.parent(current)
            }
        }
        if (!reader.isEmpty) {
            var current = group
            while (current >= 0) {
                if (
                    reader.groupKey(current) == compositionLocalMapKey &&
                        reader.groupObjectKey(current) == compositionLocalMap
                ) {
                    val providers =
                        providerUpdates?.get(current)
                            ?: reader.groupAux(current) as PersistentCompositionLocalMap
                    providerCache = providers
                    return providers
                }
                current = reader.parentOf(current)
            }
        }
        providerCache = rootProvider
        return rootProvider
    }

    @InternalComposeApi
    private fun insertMovableContentGuarded(
        references: List<Pair<MovableContentStateReference, MovableContentStateReference?>>
    ) {
        changeListWriter.withChangeList(lateChanges.asLinkBufferChangeList()) {
            changeListWriter.resetSlots()
            references.fastForEach { (to, from) ->
                val handle = to.anchor.asLinkAnchor().address.toGroupHandle()
                val effectiveNodeIndex = IntRef()
                // Insert content at the handle
                changeListWriter.determineMovableContentNodeIndex(effectiveNodeIndex, handle)
                if (from == null) {
                    val toSlotTable = to.slotStorage.asLinkBufferSlotTable()
                    if (toSlotTable == builder.table) {
                        // We are going to compose reading the insert table which will also
                        // perform an insert. This would then cause both a reader and a writer to
                        // be created simultaneously which will throw an exception. To prevent
                        // that we release the old insert table and replace it with a fresh one.
                        // This allows us to read from the old table and write to the new table.

                        // This occurs when the placeholder version of movable content was inserted
                        // but no content was available to move so we now need to create the
                        // content.

                        resetInsertBuilder(dispose = false)
                    }
                    toSlotTable.read {
                        reposition(handle)
                        val offsetChanges = ChangeList()
                        recomposeMovableContent {
                            changeListWriter.withChangeList(offsetChanges) {
                                withReader(this@read) {
                                    changeListWriter.withoutImplicitRootStart {
                                        changeListWriter.inRelativeAddressMode(handle) {
                                            invokeMovableContentLambda(
                                                to.content,
                                                to.locals,
                                                to.parameter,
                                                force = true,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        changeListWriter.includeOperationsIn(
                            other = offsetChanges,
                            effectiveNodeIndex = effectiveNodeIndex,
                        )
                    }
                } else {
                    // If the state was already removed from the from table then it will have a
                    // state recorded in the recomposer, retrieve that now if we can. If not the
                    // state is still in its original location, recompose over it there.
                    val resolvedState = parentContext.movableContentStateResolve(from)
                    val fromTable =
                        resolvedState?.slotStorage?.asLinkBufferSlotTable()
                            ?: from.slotStorage.asLinkBufferSlotTable()
                    val fromAddress =
                        resolvedState?.slotStorage?.asLinkBufferSlotTable()?.root
                            ?: from.anchor.asLinkAnchor().address
                    val nodesToInsert = fromTable.collectNodesFrom(fromAddress)

                    // Insert nodes if necessary
                    if (nodesToInsert.isNotEmpty()) {
                        changeListWriter.copyNodesToNewAnchorLocation(
                            nodesToInsert,
                            effectiveNodeIndex,
                        )
                        if (to.slotStorage == slotTable) {
                            // Inserting the content into the current slot table then we need to
                            // update the virtual node counts. Otherwise, we are inserting into
                            // a new slot table which is being created, not updated, so the virtual
                            // node counts do not need to be updated.
                            updateChildNodeCount(
                                virtualGroup = to.anchor.asLinkAnchor().address.toGroupHandle(),
                                count =
                                    updatedNodeCount(
                                        to.anchor.asLinkAnchor().address.toGroupHandle()
                                    ) + nodesToInsert.size,
                            )
                        }
                    }

                    // Copy the slot table into the anchor location
                    changeListWriter.copySlotTableToAnchorLocation(
                        resolvedState = resolvedState,
                        parentContext = parentContext,
                        from = from,
                        to = to,
                    )

                    fromTable.read {
                        withReader(this) {
                            reader.reposition(fromAddress)
                            val offsetChanges = ChangeList()
                            changeListWriter.withChangeList(offsetChanges) {
                                changeListWriter.withoutImplicitRootStart {
                                    changeListWriter.inRelativeAddressMode(reader.handle()) {
                                        from.transferPendingInvalidations()
                                        recomposeMovableContent(
                                            from = from.composition,
                                            to = to.composition,
                                            address = reader.currentGroup,
                                            invalidations = from.invalidations,
                                        ) {
                                            invokeMovableContentLambda(
                                                content = to.content,
                                                locals = to.locals,
                                                parameter = to.parameter,
                                                force = true,
                                            )
                                        }
                                    }
                                }
                            }
                            changeListWriter.includeOperationsIn(
                                other = offsetChanges,
                                effectiveNodeIndex = effectiveNodeIndex,
                            )
                        }
                    }

                    changeListWriter.disposeResolvedMovableState(resolvedState)
                }
            }
            resetInsertBuilder(dispose = false)
            changeListWriter.endMovableContentPlacement()
        }
    }

    private fun <R> recomposeMovableContent(
        from: ControlledComposition? = null,
        to: ControlledComposition? = null,
        address: GroupAddress = NULL_ADDRESS,
        invalidations: List<Pair<RecomposeScopeImpl, Any?>> = emptyList(),
        block: () -> R,
    ): R {
        val savedIsComposing = isComposing
        val savedNodeIndex = nodeIndex
        try {
            isComposing = true
            nodeIndex = 0
            invalidations.fastForEach { (scope, instances) ->
                if (instances != null) {
                    tryImminentInvalidation(scope, instances)
                } else {
                    tryImminentInvalidation(scope, null)
                }
            }
            return from?.delegateInvalidations(to, address, block) ?: block()
        } finally {
            isComposing = savedIsComposing
            nodeIndex = savedNodeIndex
        }
    }

    @OptIn(ExperimentalComposeApi::class)
    @InternalComposeApi
    private fun invokeMovableContentLambda(
        content: MovableContent<Any?>,
        locals: PersistentCompositionLocalMap,
        parameter: Any?,
        force: Boolean,
    ) {
        // Start the movable content group
        startMovableGroup(movableContentKey, content)
        updateSlot(parameter)

        // All movable content has a composite hash value rooted at the content itself so the hash
        // value doesn't change as the content moves in the tree.
        val savedCompositeKeyHash = compositeKeyHashCode

        try {
            compositeKeyHashCode = CompositeKeyHashCode(movableContentKey)

            if (inserting) builder.addFlags(flags = IsMovableContentFlag)

            // Capture the local providers at the point of the invocation. This allows detecting
            // changes to the locals as the value moves well as enables finding the correct
            // providers
            // when applying late changes which might be very complicated otherwise.
            val providersChanged = if (inserting) false else reader.groupAux != locals
            if (providersChanged) recordProviderUpdate(locals)
            start(compositionLocalMapKey, compositionLocalMap, GroupKind.Group, locals)
            providerCache = null

            // Either insert a place-holder to be inserted later (either created new or moved from
            // another location) or (re)compose the movable content. This is forced if a new value
            // needs to be created as a late change.
            if (inserting && !force) {
                builderHasAProvider = true

                val address = builder.parent(builder.parentGroup)
                val anchor = builder.table.addressSpace.anchorOfAddress(address)
                val reference =
                    MovableContentStateReference(
                        content,
                        parameter,
                        composition,
                        builder.table,
                        anchor,
                        emptyList(),
                        currentCompositionLocalScope(),
                        null,
                    )
                parentContext.insertMovableContent(reference)
            } else {
                val savedProvidersInvalid = providersInvalid
                providersInvalid = providersChanged

                // All changes should use AnchorHandle's to navigate as the content that is
                // being recomposed here may be moved to a different address space.
                changeListWriter.seekTo(reader.handle(), resetRelativeAddressing = true)
                changeListWriter.inAnchorMode {
                    invokeComposable(this, { content.content(parameter) })
                }
                providersInvalid = savedProvidersInvalid
            }
        } catch (e: Throwable) {
            throw e.attachComposeStackTrace { currentStackTrace() }
        } finally {
            // Restore the state back to what is expected by the caller.
            endGroup()
            providerCache = null
            compositeKeyHashCode = savedCompositeKeyHash
            endMovableGroup()
        }
    }

    private fun isGroupAfterCurrentReaderPosition(group: GroupHandle): Boolean {
        val readerPosition = reader.handle()
        if (readerPosition == NULL_GROUP_HANDLE) return true
        return readerTable.firstGroupInTopologicalOrder(group, readerPosition) == readerPosition
    }

    /**
     * Recompose any invalidate child groups of the current parent group. This should be called
     * after the group is started but on or before the first child group. It is intended to be
     * called instead of [skipReaderToGroupEnd] if any child groups are invalid. If no children are
     * invalid it will call [skipReaderToGroupEnd].
     */
    @InternalComposeApi
    private fun recomposeToGroupEnd() {
        val wasComposing = isComposing
        isComposing = true
        var recomposed = false
        val reader = reader
        val parent = reader.parentGroup
        val recomposeIndex = nodeIndex
        val recomposeCompositeKey = compositeKeyHashCode
        val oldGroupNodeCount = groupNodeCount
        val oldRGroupIndex = rGroupIndex

        reader.traverseChildrenConditionally(
            group = parent,
            enter = { group ->
                // Enter the group if one or more of the children need to be recomposed.
                val recomposeRequired = reader.hasRecomposeRequired(group)
                if (recomposeRequired) {
                    val dataKey = reader.groupObjectKey(group)
                    if (dataKey is MovableContent<*>)
                        compositeKeyHashCode = CompositeKeyHashCode(movableContentKey)
                    else {
                        updateCompositeKeyWhenWeEnterGroup(
                            groupKey = reader.groupKey(group),
                            rGroupIndex = rGroupIndex,
                            dataKey = reader.groupObjectKey(group),
                            data = reader.groupAux(group),
                        )
                    }
                    parentStateStack.push(nodeIndex)
                    parentStateStack.push(rGroupIndex)
                    if (reader.isNode(group)) {
                        changeListWriter.moveDown(reader.node(group))
                        nodeIndex = 0
                    }
                    rGroupIndex = 0
                } else {
                    // We are not entering the group so need to record we are skipping the
                    // nodes in the group
                    nodeIndex +=
                        if (reader.isNode(group)) 1 else updatedNodeCount(group.toGroupHandle())
                    if (!reader.hasObjectKey(group)) rGroupIndex++
                }
                recomposeRequired
            },
            block = { group ->
                // Recompose this group if it requires recomposition
                if (reader.recomposeRequired(group)) {
                    reader.reposition(group)
                    val scope = requireRecomposeScope(group)
                    val invalidations = invalidations[scope]

                    if (scope.isInvalidFor(invalidations)) {
                        recomposed = true

                        // We have moved so the cached lookup of the provider is invalid
                        providerCache = null

                        debugRuntimeCheck(rGroupIndex == rGroupIndexOf(group)) {
                            "rGroupIndex was not tracked correctly"
                        }

                        // Invoke the function with the same parameters as the last composition
                        // (which were captured in the lambda set into the scope).
                        scope.compose(this)

                        // We could have moved out of a provider so the provider cache is
                        // invalid.
                        providerCache = null

                        // Let the iteration know we used the group
                        true
                    } else {
                        // If the invalidation is not used restore the reads that were removed
                        // when the invalidation was recorded. This happens, for example, when on of
                        // a derived state's dependencies changed but the derived state itself was
                        // not changed.
                        invalidateStack.push(scope)
                        val observer = observerHolder.current()
                        if (observer != null) {
                            try {
                                observer.onScopeEnter(scope)
                                scope.rereadTrackedInstances()
                            } finally {
                                observer.onScopeExit(scope)
                            }
                        } else {
                            scope.rereadTrackedInstances()
                        }
                        invalidateStack.pop()

                        // Let the iteration know we are skipping the group
                        false
                    }
                } else false
            },
            exit = { group ->
                if (reader.isNode(group)) changeListWriter.moveUp()
                rGroupIndex = parentStateStack.pop()
                nodeIndex = parentStateStack.pop() + updatedNodeCount(group.toGroupHandle())
                updateCompositeKeyWhenWeExitGroup(
                    groupKey = reader.groupKey(group),
                    rGroupIndex = rGroupIndex,
                    dataKey = reader.groupObjectKey(group),
                    data = reader.groupAux(group),
                )
                if (!reader.hasObjectKey(group)) rGroupIndex++
            },
            skip = { group ->
                // If a group has no children and is not used by block above then  skip is called
                // instead of enter. In this case we still need to update the nodeIndex
                nodeIndex +=
                    if (reader.isNode(group)) 1 else updatedNodeCount(group.toGroupHandle())

                // If we are skipping the group we need the rGroupIndex to account for the group
                if (!reader.hasObjectKey(group)) rGroupIndex++
            },
        )

        // Restore the parent of the reader to the previous parent
        reader.restoreParent(parent)

        if (recomposed) {
            reader.skipToGroupEnd()
            val parentGroupNodes = updatedNodeCount(parent.toGroupHandle())
            nodeIndex = recomposeIndex + parentGroupNodes
            groupNodeCount = oldGroupNodeCount + parentGroupNodes
            rGroupIndex = oldRGroupIndex
        } else {
            // No recompositions were requested in the range, skip it.
            skipReaderToGroupEnd()

            // No need to restore the parent state for nodeIndex, groupNodeCount and
            // rGroupIndex as they are going to be restored immediately by the endGroup
        }
        compositeKeyHashCode = recomposeCompositeKey

        isComposing = wasComposing
    }

    private fun recordDelete() {
        // It is import that the movable content is reported first so it can be removed before the
        // group itself is removed.
        reportFreeMovableContent(reader.handle())
        changeListWriter.removeGroup()
    }

    private fun recordInsert(source: GroupHandle) {
        if (insertFixups.isEmpty()) {
            changeListWriter.insertSlots(builder.table, source)
        } else {
            changeListWriter.insertSlots(builder.table, source, insertFixups)
            insertFixups = FixupList()
        }
    }

    private fun recordProviderUpdate(providers: PersistentCompositionLocalMap) {
        val providerUpdates =
            providerUpdates
                ?: run {
                    val newProviderUpdates = MutableIntObjectMap<PersistentCompositionLocalMap>()
                    this.providerUpdates = newProviderUpdates
                    newProviderUpdates
                }
        providerUpdates[reader.currentGroup] = providers
    }

    /**
     * Called during composition to report all the content of the composition will be released as
     * this composition is to be disposed.
     */
    private fun reportAllMovableContent() {
        if (slotTable.containsFlags(HasMovableContentFlag)) {
            composition.updateMovingInvalidations()
            val changes = ChangeList()
            deferredChanges = changes
            slotTable.read {
                this@LinkComposer.reader = this
                changeListWriter.withChangeList(changes) { reportFreeMovableContent(rootHandle()) }
            }
        }
    }

    /**
     * Report any movable content that the group contains as being removed and ready to be moved.
     * Returns true if the group itself was removed.
     *
     * Returns the number of nodes left in place which is used to calculate the node index of any
     * nested calls.
     *
     * @param groupBeingRemoved The group that is being removed from the table
     */
    @OptIn(InternalComposeApi::class)
    private fun reportFreeMovableContent(groupBeingRemoved: GroupHandle) {

        fun createMovableContentReferenceForGroup(
            group: GroupAddress,
            nestedStates: List<MovableContentStateReference>?,
        ): MovableContentStateReference {

            @Suppress("UNCHECKED_CAST")
            val movableContent = reader.groupObjectKey(group) as MovableContent<Any?>
            val parameter = reader.get(group, 0)
            val invalidations = reader.findInvalidations(group, invalidations)
            val anchor = readerTable.addressSpace.anchorOfAddress(group)
            val reference =
                MovableContentStateReference(
                    movableContent,
                    parameter,
                    composition,
                    readerTable,
                    anchor,
                    invalidations,
                    currentCompositionLocalScope(group),
                    nestedStates,
                )
            return reference
        }

        fun movableContentReferenceFor(group: GroupAddress): MovableContentStateReference? {
            val flags = reader.flagsOf(group)
            return if (IsMovableContentFlag in flags) {
                val nestedStates =
                    if (HasMovableContentFlag in flags)
                        buildList<MovableContentStateReference> {
                            reader.traverseChildrenConditionally(
                                group,
                                enter = { child -> HasMovableContentFlag in reader.flagsOf(child) },
                                block = { child ->
                                    if (IsMovableContentFlag in reader.flagsOf(child)) {
                                        movableContentReferenceFor(child)?.let { add(it) }
                                        true
                                    } else false
                                },
                                exit = {},
                                skip = {},
                            )
                        }
                    else null
                createMovableContentReferenceForGroup(group, nestedStates)
            } else null
        }

        fun reportGroup(handle: GroupHandle, needsNodeDelete: Boolean, nodeIndex: Int): Int {
            val group = handle.group
            if (group < 0) return 0
            val flags = reader.flagsOf(group)
            val childNodeCount =
                when {
                    IsMovableContentFlag in flags -> {
                        // If the group is a movable content block, schedule it to be removed and
                        // report that it is free to be moved to the parentContext. Nested movable
                        // content is recomposed if necessary once the group has been claimed by
                        // another insert. If the nested movable content ends up being removed this
                        // is reported during that recomposition so there is no need to look at
                        // child movable content here.
                        @Suppress("UNCHECKED_CAST")
                        val reference = movableContentReferenceFor(group)
                        if (reference != null) {
                            parentContext.deletedMovableContent(reference)
                            changeListWriter.releaseMovableGroup(
                                composition,
                                parentContext,
                                reference,
                            )
                        }
                        if (needsNodeDelete) {
                            changeListWriter.endNodeMovementAndDeleteNode(nodeIndex, group)
                            0 // These nodes were deleted
                        } else groupFlagsNodeCount(reader.flagsOf(group))
                    }

                    IsSubcompositionContextFlag in flags -> {
                        // Group is a composition context reference. As this is being removed assume
                        // all movable groups in the composition that have this context will also be
                        // released when the compositions are disposed.
                        val observerHolder = reader.get(group, 0) as? RememberObserverHolder
                        val contextHolder = observerHolder?.wrapped as? CompositionContextHolder
                        if (contextHolder != null) {
                            // The contextHolder can be EMPTY in cases where the content has been
                            // deactivated. Content is deactivated if the content is just being
                            // held onto for recycling and is not otherwise active. In this case
                            // the composers we are likely to find here have already been disposed.
                            val compositionContext = contextHolder.ref
                            compositionContext.composers.forEach { composer ->
                                composer.reportAllMovableContent()

                                // Mark the composition as being removed so it will not be
                                // recomposed this turn.
                                parentContext.reportRemovedComposition(composer.composition)
                            }
                        }
                        groupFlagsNodeCount(reader.flagsOf(group))
                    }
                    HasMovableContentFlag in flags || HasSubcompositionContextFlag in flags -> {
                        // Traverse the group freeing the child movable content. This group is known
                        // to have at least one child that contains movable content because the
                        // group is marked as containing a mark.
                        var runningNodeCount = 0
                        reader.traverseChildrenByHandle(group) { childHandle ->
                            // A tree is not disassembled when it is removed, the root nodes of the
                            // sub-trees are removed, therefore, if we enter a node that contains
                            // movable content, the nodes should be removed so some future
                            // composition can re-insert them at a new location. Otherwise the
                            // applier will attempt to insert a node that already has a parent. If
                            // there is no node between the group removed and this group then the
                            // nodes will be removed by normal recomposition.
                            val childGroup = childHandle.group
                            val isNode = IsNodeFlag in reader.flagsOf(childGroup)
                            if (isNode) {
                                changeListWriter.endNodeMovement()
                                changeListWriter.moveDown(reader.node(childGroup))
                            }
                            runningNodeCount +=
                                reportGroup(
                                    handle = childHandle,
                                    needsNodeDelete = isNode || needsNodeDelete,
                                    nodeIndex = if (isNode) 0 else nodeIndex + runningNodeCount,
                                )
                            if (isNode) {
                                changeListWriter.endNodeMovement()
                                changeListWriter.moveUp()
                            }
                        }
                        runningNodeCount
                    }
                    else -> groupFlagsNodeCount(reader.flagsOf(group))
                }

            return if (IsNodeFlag in flags) 1 else childNodeCount
        }
        // If the group that is being deleted is a node we need to remove any children that
        // are moved.
        val group = groupBeingRemoved.group
        val rootIsNode = IsNodeFlag in reader.flagsOf(group)
        if (rootIsNode) {
            changeListWriter.endNodeMovement()
            changeListWriter.moveDown(reader.node(group))
        }
        reportGroup(groupBeingRemoved, needsNodeDelete = rootIsNode, nodeIndex = 0)
        changeListWriter.endNodeMovement()
        if (rootIsNode) {
            changeListWriter.moveUp()
        }
    }

    private fun resetInsertBuilder(dispose: Boolean) {
        if (!builder.isClosed) {
            val table = builder.build()
            if (dispose) table.dispose()
        }
        builder =
            SlotTableBuilder(
                    slotTable.addressSpace,
                    recordSourceInformation = false,
                    recordCallByInformation = false,
                )
                .also { it.close() }
    }

    private fun requireRecomposeScope(group: GroupAddress): RecomposeScopeImpl {
        val slot = reader.get(group, 0)
        runtimeCheck(slot != Composer.Empty) {
            "Cannot obtain RecomposeScope. Group does not have a corresponding slot."
        }
        runtimeCheck(slot is RecomposeScopeImpl) {
            "Expected a RecomposeScope in the first non-utility slot, found $slot."
        }
        return slot
    }

    private fun requiresRecomposition(group: GroupAddress): Boolean =
        reader.hasRecomposeRequired(group)

    private fun rGroupIndexOf(group: GroupAddress): Int {
        var result = 0
        val groupParent = reader.parentOf(group)
        val eldestSibling =
            if (groupParent < 0) {
                readerTable.root
            } else {
                reader.firstChildOf(groupParent)
            }

        readerTable.traverseSiblings(eldestSibling) { predecessor ->
            if (predecessor == group) return result
            if (!reader.hasObjectKey(predecessor)) result++
        }
        return result
    }

    private fun skipGroup() {
        groupNodeCount += reader.skipGroup()
    }

    private fun skipReaderToGroupEnd() {
        groupNodeCount = reader.parentNodeCount
        reader.skipToGroupEnd()
    }

    private fun start(key: Int, objectKey: Any?, kind: GroupKind, data: Any?) {
        validateNodeNotExpected()

        updateCompositeKeyWhenWeEnterGroup(key, rGroupIndex, objectKey, data)

        if (objectKey == null) rGroupIndex++

        // Check for the insert fast path. If we are already inserting (creating nodes) then
        // there is no need to track insert, deletes and moves with a pending changes object.
        val isNode = kind.isNode
        if (inserting) {
            reader.beginEmpty()
            val builder = builder
            when {
                isNode -> builder.startNodeGroup(key, Composer.Empty, Composer.Empty)
                data != null -> builder.startDataGroup(key, objectKey ?: Composer.Empty, data)
                else -> builder.startGroup(key, objectKey ?: Composer.Empty)
            }
            pending?.let { pending ->
                val insertKeyInfo =
                    KeyInfo(
                        key = key,
                        objectKey = -1,
                        handle = builder.parentHandle.toInsertAddress(),
                        nodes = -1,
                        index = 0,
                    )
                pending.registerInsert(insertKeyInfo, nodeIndex - pending.startIndex)
                pending.recordUsed(insertKeyInfo)
            }
            enterGroup(isNode, null)
            return
        }

        val forceReplace = !kind.isReusable && reusing
        if (pending == null) {
            val slotKey = reader.groupKey
            if (!forceReplace && slotKey == key && objectKey == reader.groupObjectKey) {
                // The group is the same as what was generated last time.
                startReaderGroup(isNode, data)
            } else {
                pending = LinkPending(reader.extractKeys(), nodeIndex)
            }
        }

        val pending = pending
        var newPending: LinkPending? = null
        if (pending != null) {
            // Check to see if the key was generated last time from the keys collected above.
            val keyInfo = pending.getNext(key, objectKey)
            if (!forceReplace && keyInfo != null) {
                // This group was generated last time, use it.
                pending.recordUsed(keyInfo)

                // Move the slot table to the location where the information about this group is
                // stored. The slot information will move once the changes are applied so moving the
                // current of the slot table is sufficient.
                val location = keyInfo.handle

                // Determine what index this group is in. This is used for inserting nodes into the
                // group.
                nodeIndex = pending.nodePositionOf(keyInfo) + pending.startIndex

                // Determine how to move the slot group to the correct position.
                val relativePosition = pending.slotPositionOf(keyInfo)
                val currentRelativePosition = relativePosition - pending.groupIndex
                pending.registerMoveSlot(relativePosition, pending.groupIndex)
                if (currentRelativePosition > 0) {
                    reader.reposition(pending.groupHandleOfNextUnmovedGroup())
                    // The slot group must be moved, record the move to be performed during apply.
                    changeListWriter.moveGroup(currentRelativePosition)
                }
                pending.markGroupLocationReconciled(keyInfo.index)
                reader.reposition(location)
                startReaderGroup(isNode, data)
            } else {
                // The group is new, go into insert mode. All child groups will written to the
                // insertTable until the group is complete which will schedule the groups to be
                // inserted into in the table.
                reader.beginEmpty()
                inserting = true
                providerCache = null
                ensureBuilder()
                val builder = builder
                when {
                    isNode -> builder.startNodeGroup(key, Composer.Empty, Composer.Empty)
                    data != null -> builder.startDataGroup(key, objectKey ?: Composer.Empty, data)
                    else -> builder.startGroup(key, objectKey ?: Composer.Empty)
                }
                val insertKeyInfo =
                    KeyInfo(
                        key = key,
                        objectKey = -1,
                        handle = builder.parentHandle.toInsertAddress(),
                        nodes = -1,
                        index = 0,
                    )
                pending.registerInsert(insertKeyInfo, nodeIndex - pending.startIndex)
                pending.recordUsed(insertKeyInfo)
                newPending = LinkPending(mutableListOf(), if (isNode) 0 else nodeIndex)
            }
        }

        enterGroup(isNode, newPending)
    }

    /**
     * Start a group with the given key. During recomposition if the currently expected group does
     * not match the given key a group the groups emitted in the same parent group are inspected to
     * determine if one of them has this key and that group the first such group is moved (along
     * with any nodes emitted by the group) to the current position and composition continues. If no
     * group with this key is found, then the composition shifts into insert mode and new nodes are
     * added at the current position.
     *
     * @param key The key for the group
     */
    private inline fun startGroup(key: Int) = start(key, null, GroupKind.Group, null)

    private fun startGroup(key: Int, dataKey: Any?) = start(key, dataKey, GroupKind.Group, null)

    /** Start the reader group updating the data of the group if necessary */
    private fun startReaderGroup(isNode: Boolean, data: Any?) {
        if (isNode) {
            reader.startNode()
        } else {
            if (data != null && reader.groupAux !== data) {
                changeListWriter.updateAuxData(data)
            }
            reader.startGroup()
        }
    }

    private fun startRoot() {
        rGroupIndex = 0
        reader = slotTable.openReader()
        startGroup(rootKey)

        // parent reference management
        parentContext.startComposing()
        val parentProvider = parentContext.getCompositionLocalScope()
        providersInvalidStack.push(providersInvalid.asInt())
        providersInvalid = changed(parentProvider)
        providerCache = null

        // Inform observer if one is defined
        if (!forceRecomposeScopes) {
            forceRecomposeScopes = parentContext.collectingParameterInformation
        }

        // Propagate collecting source information
        if (!sourceMarkersEnabled) {
            sourceMarkersEnabled = parentContext.collectingSourceInformation
        }

        rootProvider =
            if (sourceMarkersEnabled) {
                @Suppress("UNCHECKED_CAST") // ProvidableCompositionLocal to CompositionLocal
                parentProvider.putValue(
                    LocalCompositionErrorContext as CompositionLocal<Any?>,
                    StaticValueHolder(errorContext),
                )
            } else {
                parentProvider
            }

        rootProvider.read(LocalInspectionTables)?.let {
            it.add(compositionData)
            parentContext.recordInspectionTable(it)
        }

        startGroup(parentContext.compositeKeyHashCode.hashCode())
    }

    private fun stackTraceForGroup(group: Int, dataOffset: Int?): List<ComposeStackTraceFrame> {
        if (!sourceMarkersEnabled) return emptyList()

        return slotTable.read { traceForGroup(group, dataOffset) }
    }

    /**
     * Schedule the current value in the slot table to be updated to [value].
     *
     * @param value the value to schedule to be written to the slot table.
     */
    internal fun updateCachedValue(value: Any?) {
        val toStore =
            if (value is RememberObserver) {
                val holder =
                    LinkRememberObserverHolder(
                        wrapped = value,
                        after = readerTable.addressSpace.anchorOfAddress(lastPlacedChildGroup),
                    )
                if (inserting) changeListWriter.remember(holder)
                abandonSet.add(value)

                holder
            } else value
        updateValue(toStore)
    }

    private fun updateChildNodeCount(virtualGroup: VirtualGroupHandle, count: Int) {
        if (updatedNodeCount(virtualGroup) != count) {
            if (virtualGroup.isInsertHandle) {
                val virtualCounts =
                    nodeCountVirtualOverrides
                        ?: run {
                            val newCounts = MutableIntIntMap()
                            nodeCountVirtualOverrides = newCounts
                            newCounts
                        }
                virtualCounts[virtualGroup.group] = count
            } else {
                val nodeCounts =
                    nodeCountOverrides
                        ?: run {
                            val newCounts = MutableIntIntMap()
                            nodeCountOverrides = newCounts
                            newCounts
                        }
                nodeCounts[virtualGroup.toGroupHandle().group] = count
            }
        }
    }

    /**
     * As operations to insert and remove nodes are recorded, the number of nodes that will be in
     * the group after changes are applied is maintained in a side overrides table. This method
     * updates that count and then updates any parent groups that include the nodes this group
     * emits.
     */
    private fun updateNodeCountOverrides(virtualHandle: VirtualGroupHandle, newCount: Int) {
        // The value of group can be negative which indicates it is tracking an inserted group
        // instead of an existing group. The index is a virtual index calculated by
        // insertedGroupVirtualIndex which corresponds to the location of the groups to insert in
        // the insertTable.
        val currentCount = updatedNodeCount(virtualHandle)
        if (currentCount != newCount) {
            // Update the overrides
            val delta = newCount - currentCount
            var current = virtualHandle

            var minPending = pendingStack.size - 1
            while (current.group != NULL_ADDRESS) {
                val newCurrentNodes = updatedNodeCount(current) + delta
                updateChildNodeCount(current, newCurrentNodes)
                for (pendingIndex in minPending downTo 0) {
                    val pending = pendingStack.peek(pendingIndex)
                    if (
                        pending != null && pending.updateNodeCount(current.group, newCurrentNodes)
                    ) {
                        minPending = pendingIndex - 1
                        break
                    }
                }
                if (current.isInsertHandle) {
                    current = reader.parentHandle
                } else {
                    val groups = readerTable.addressSpace.groups
                    val group = current.group
                    if (IsNodeFlag in groups.groupFlags(group)) break
                    current = groups.groupParent(group).toGroupHandle()
                }
            }
        }
    }

    /**
     * Update (or create) the slots to record the providers. The providers maps are first the scope
     * followed by the map used to augment the parent scope. Both are needed to detect inserts,
     * updates and deletes to the providers.
     */
    private fun updateProviderMapGroup(
        parentScope: PersistentCompositionLocalMap,
        currentProviders: PersistentCompositionLocalMap,
    ): PersistentCompositionLocalMap {
        val providerScope = parentScope.mutate { it.putAll(currentProviders) }
        startGroup(providerMapsKey, providerMaps)
        updateSlot(providerScope)
        updateSlot(currentProviders)
        endGroup()
        return providerScope
    }

    private fun updateSlot(value: Any?) {
        nextSlot()
        updateValue(value)
    }

    private fun updatedNodeCount(virtualHandle: VirtualGroupHandle): Int {
        if (virtualHandle.isInsertHandle)
            return nodeCountVirtualOverrides?.getOrDefault(virtualHandle.group, 0) ?: 0
        val handle = virtualHandle.toGroupHandle()
        val group = handle.group
        val nodeCounts = nodeCountOverrides
        if (nodeCounts != null) {
            val override = nodeCounts.getOrDefault(group, -1)
            if (override >= 0) return override
        }
        return groupFlagsChildNodeCount(readerTable.addressSpace.groups.groupFlags(group))
    }

    private fun Any?.unwrapRememberObserverHolder(): Any? =
        if (this is RememberObserverHolder) this.wrapped else this

    private inline fun <R> withReader(reader: SlotTableReader, block: () -> R): R {
        val savedReader = this.reader
        val savedCountOverrides = nodeCountOverrides
        val savedProviderUpdates = providerUpdates
        nodeCountOverrides = null
        providerUpdates = null
        try {
            this.reader = reader
            return block()
        } finally {
            this.reader = savedReader
            nodeCountOverrides = savedCountOverrides
            providerUpdates = savedProviderUpdates
        }
    }

    private fun enterRecomposeScope(scope: RecomposeScopeImpl) {
        scope.start(compositionToken)
        observerHolder.current()?.onScopeEnter(scope)
    }

    private fun exitRecomposeScope(scope: RecomposeScopeImpl): ((Composition) -> Unit)? {
        observerHolder.current()?.onScopeExit(scope)
        return scope.end(compositionToken)
    }

    private inline fun updateCompositeKeyWhenWeEnterGroup(
        groupKey: Int,
        rGroupIndex: Int,
        dataKey: Any?,
        data: Any?,
    ) {
        if (dataKey == null)
            if (data != null && groupKey == reuseKey && data != Composer.Companion.Empty)
                updateCompositeKeyWhenWeEnterGroupKeyHash(data.hashCode(), rGroupIndex)
            else updateCompositeKeyWhenWeEnterGroupKeyHash(groupKey, rGroupIndex)
        else if (dataKey is Enum<*>) updateCompositeKeyWhenWeEnterGroupKeyHash(dataKey.ordinal, 0)
        else updateCompositeKeyWhenWeEnterGroupKeyHash(dataKey.hashCode(), 0)
    }

    private inline fun updateCompositeKeyWhenWeEnterGroupKeyHash(groupKey: Int, rGroupIndex: Int) {
        compositeKeyHashCode =
            compositeKeyHashCode.compoundWith(groupKey, 3).compoundWith(rGroupIndex, 3)
    }

    private inline fun updateCompositeKeyWhenWeExitGroup(
        groupKey: Int,
        rGroupIndex: Int,
        dataKey: Any?,
        data: Any?,
    ) {
        if (dataKey == null)
            if (data != null && groupKey == reuseKey && data != Composer.Companion.Empty)
                updateCompositeKeyWhenWeExitGroupKeyHash(data.hashCode(), rGroupIndex)
            else updateCompositeKeyWhenWeExitGroupKeyHash(groupKey, rGroupIndex)
        else if (dataKey is Enum<*>) updateCompositeKeyWhenWeExitGroupKeyHash(dataKey.ordinal, 0)
        else updateCompositeKeyWhenWeExitGroupKeyHash(dataKey.hashCode(), 0)
    }

    private inline fun updateCompositeKeyWhenWeExitGroupKeyHash(groupKey: Int, rGroupIndex: Int) {
        compositeKeyHashCode =
            compositeKeyHashCode.unCompoundWith(rGroupIndex, 3).unCompoundWith(groupKey, 3)
    }

    private fun validateNodeExpected() {
        runtimeCheck(nodeExpected) {
            "A call to createNode(), emitNode() or useNode() expected was not expected"
        }
        nodeExpected = false
    }

    private fun validateNodeNotExpected() {
        runtimeCheck(!nodeExpected) { "A call to createNode(), emitNode() or useNode() expected" }
    }

    @OptIn(ExperimentalComposeRuntimeApi::class, InternalComposeApi::class)
    internal inner class CompositionContextImpl(
        override val compositeKeyHashCode: CompositeKeyHashCode,
        override val collectingParameterInformation: Boolean,
        override val collectingSourceInformation: Boolean,
        override val observerHolder: CompositionObserverHolder?,
    ) : CompositionContext() {
        var inspectionTables: MutableSet<MutableSet<CompositionData>>? = null
        val composers = mutableSetOf<LinkComposer>()

        override val collectingCallByInformation: Boolean
            get() = parentContext.collectingCallByInformation

        override val stackTraceEnabled: Boolean
            get() = parentContext.stackTraceEnabled

        fun dispose() {
            if (composers.isNotEmpty()) {
                inspectionTables?.let {
                    for (composer in composers) {
                        for (table in it) table.remove(composer.compositionData)
                    }
                }
                composers.clear()
            }
        }

        override fun registerComposer(composer: Composer) {
            super.registerComposer(composer)
            composers.add(composer.asLinkComposer())
        }

        override fun unregisterComposer(composer: Composer) {
            inspectionTables?.forEach { it.remove(composer.asLinkComposer().compositionData) }
            composers.remove(composer)
        }

        override fun registerComposition(composition: ControlledComposition) {
            parentContext.registerComposition(composition)
        }

        override fun unregisterComposition(composition: ControlledComposition) {
            parentContext.unregisterComposition(composition)
        }

        override fun reportPausedScope(scope: RecomposeScopeImpl) {
            parentContext.reportPausedScope(scope)
        }

        override val effectCoroutineContext: CoroutineContext
            get() = parentContext.effectCoroutineContext

        override fun composeInitial(
            composition: ControlledComposition,
            content: @Composable () -> Unit,
        ) {
            parentContext.composeInitial(composition, content)
        }

        override fun composeInitialPaused(
            composition: ControlledComposition,
            shouldPause: ShouldPauseCallback,
            content: @Composable () -> Unit,
        ): ScatterSet<RecomposeScopeImpl> =
            parentContext.composeInitialPaused(composition, shouldPause, content)

        override fun recomposePaused(
            composition: ControlledComposition,
            shouldPause: ShouldPauseCallback,
            invalidScopes: ScatterSet<RecomposeScopeImpl>,
        ): ScatterSet<RecomposeScopeImpl> =
            parentContext.recomposePaused(composition, shouldPause, invalidScopes)

        override fun invalidate(composition: ControlledComposition) {
            // Invalidate ourselves with our parent before we invalidate a child composer.
            // This ensures that when we are scheduling recompositions, parents always
            // recompose before their children just in case a recomposition in the parent
            // would also cause other recomposition in the child.
            // If the parent ends up having no real invalidations to process we will skip work
            // for that composer along a fast path later.
            // This invalidation process could be made more efficient as it's currently N^2 with
            // subcomposition meta-tree depth thanks to the double recursive parent walk
            // performed here, but we currently assume a low N.
            parentContext.invalidate(this@LinkComposer.composition)
            parentContext.invalidate(composition)
        }

        override fun invalidateScope(scope: RecomposeScopeImpl) {
            parentContext.invalidateScope(scope)
        }

        // This is snapshot state not because we need it to be observable, but because
        // we need changes made to it in composition to be visible for the rest of the current
        // composition and not become visible outside of the composition process until composition
        // succeeds.
        private var compositionLocalScope by
            mutableStateOf<PersistentCompositionLocalMap>(
                persistentCompositionLocalHashMapOf(),
                referentialEqualityPolicy(),
            )

        override fun getCompositionLocalScope(): PersistentCompositionLocalMap =
            compositionLocalScope

        fun updateCompositionLocalScope(scope: PersistentCompositionLocalMap) {
            compositionLocalScope = scope
        }

        override fun recordInspectionTable(table: MutableSet<CompositionData>) {
            (inspectionTables
                    ?: HashSet<MutableSet<CompositionData>>().also { inspectionTables = it })
                .add(table)
        }

        override fun startComposing() {
            childrenComposing++
        }

        override fun doneComposing() {
            childrenComposing--
        }

        override fun insertMovableContent(reference: MovableContentStateReference) {
            parentContext.insertMovableContent(reference)
        }

        override fun deletedMovableContent(reference: MovableContentStateReference) {
            parentContext.deletedMovableContent(reference)
        }

        @OptIn(InternalComposeApi::class)
        override fun movableContentStateResolve(
            reference: MovableContentStateReference
        ): MovableContentState? = parentContext.movableContentStateResolve(reference)

        override fun movableContentStateReleased(
            reference: MovableContentStateReference,
            data: MovableContentState,
            applier: Applier<*>,
        ) {
            parentContext.movableContentStateReleased(reference, data, applier)
        }

        override fun reportRemovedComposition(composition: ControlledComposition) {
            parentContext.reportRemovedComposition(composition)
        }

        override val composition: Composition
            get() = this@LinkComposer.composition

        override fun scheduleFrameEndCallback(action: () -> Unit): CancellationHandle {
            return parentContext.scheduleFrameEndCallback(action)
        }
    }

    /**
     * A holder that will dispose of its [CompositionContext] when it leaves the composition that
     * will not have its reference made visible to user code.
     */
    internal class CompositionContextHolder(val ref: CompositionContextImpl) : RememberObserver {

        override fun onRemembered() {}

        override fun onAbandoned() {
            ref.dispose()
        }

        override fun onForgotten() {
            ref.dispose()
        }
    }
}

internal fun Composer.asLinkComposer(): LinkComposer =
    this as? LinkComposer ?: composeRuntimeError("Inconsistent composition")

internal open class LinkRememberObserverHolder(
    override var wrapped: RememberObserver,
    var after: LinkAnchor,
) : RememberObserverHolder

internal class ReusableLinkRememberObserverHolder(wrapped: RememberObserver, after: LinkAnchor) :
    LinkRememberObserverHolder(wrapped, after), ReusableRememberObserverHolder

internal fun RememberObserverHolder.asLinkRememberObserverHolder() =
    this as? LinkRememberObserverHolder ?: composeRuntimeError("Inconsistent composition")

internal fun ReusableRememberObserverHolder.asLinkRememberObserverHolder() =
    this as? ReusableLinkRememberObserverHolder ?: composeRuntimeError("Inconsistent composition")

internal fun SlotTable.findSubcompositionContextGroup(context: CompositionContext): Int? {
    read {
        traverseChildrenConditionally(
            group = root,
            enter = { group -> HasSubcompositionContextFlag in groupFlags(group) },
            block = { group ->
                if (IsSubcompositionContextFlag in groupFlags(group)) {
                    forEachGroupSlot(group) { slot, _ ->
                        val observerHolder = slot as? RememberObserverHolder
                        val wrapped = observerHolder?.wrapped
                        val contextHolder = wrapped as? LinkComposer.CompositionContextHolder
                        if (contextHolder != null && contextHolder.ref == context) {
                            return group
                        }
                    }
                }
                false
            },
            exit = {},
            skip = {},
        )
    }
    return null
}

internal fun SlotTableReader.findInvalidations(
    group: GroupAddress,
    invalidations: ScopeMap<RecomposeScopeImpl, Any>,
): List<Pair<RecomposeScopeImpl, Any>> =
    if (invalidations.isEmpty()) emptyList()
    else
        buildList {
            val movableRecomposeScopes = buildScatterSet {
                table.traverseGroup(group) { group ->
                    getRecomposeScopeOrNull(group)?.let { add(it) }
                }
            }

            invalidations.forEach { recomposeScope, invalidation ->
                if (recomposeScope in movableRecomposeScopes) {
                    add(recomposeScope to invalidation)
                }
            }
        }

internal fun SlotTableReader.getRecomposeScopeOrNull(group: GroupAddress): RecomposeScopeImpl? =
    getOrNull(group, 0) as? RecomposeScopeImpl

internal class LinkCompositionDataImpl(val composition: Composition) :
    CompositionData, CompositionInstance {
    private val slotTable
        get() = (composition as CompositionImpl).slotStorage.asLinkBufferSlotTable()

    override val compositionGroups: Iterable<CompositionGroup>
        get() = slotTable.compositionGroups

    override val isEmpty: Boolean
        get() = slotTable.isEmpty

    override fun find(identityToFind: Any): CompositionGroup? = slotTable.find(identityToFind)

    override fun hashCode(): Int = composition.hashCode() * 31

    override fun equals(other: Any?): Boolean =
        other is LinkCompositionDataImpl && composition == other.composition

    override val parent: CompositionInstance?
        get() = composition.parent?.let { LinkCompositionDataImpl(it) }

    override val data: CompositionData
        get() = this

    override fun findContextGroup(): CompositionGroup? {
        val parentSlotTable = composition.parent?.slotTable?.asLinkBufferSlotTable() ?: return null
        val context = composition.context ?: return null

        return parentSlotTable.findSubcompositionContextGroup(context)?.let {
            parentSlotTable.compositionGroupOf(it)
        }
    }

    private val Composition.slotTable
        get() = (this as? CompositionImpl)?.slotStorage

    private val Composition.context
        get() = (this as? CompositionImpl)?.parent

    private val Composition.parent
        get() = context?.composition
}

private inline fun <T> buildScatterSet(
    builderAction: MutableScatterSet<T>.() -> Unit
): ScatterSet<T> {
    val mutableSet = mutableScatterSetOf<T>()
    mutableSet.builderAction()
    return mutableSet
}

private fun <K : Any, V : Any> multiMap(initialCapacity: Int) =
    MultiValueMap<K, V>(MutableScatterMap(initialCapacity))

private fun getKey(value: Any?, left: Any?, right: Any?): Any? =
    (value as? JoinedKey)?.let {
        if (it.left == left && it.right == right) value
        else getKey(it.left, left, right) ?: getKey(it.right, left, right)
    }

private fun Boolean.asInt() = if (this) 1 else 0

private fun Int.asBool() = this != 0

private fun SlotTable.firstGroupInTopologicalOrder(a: GroupHandle, b: GroupHandle): GroupHandle {
    // Easy cases: a and b are the same, are direct child/parent, or have the same parent.
    // Allows skipping the calls to distanceFrom()
    if (a == b) return a

    val addressSpace = addressSpace
    val groups = addressSpace.groups
    var currentA =
        if (a.group == NULL_ADDRESS) {
            if (b.group == NULL_ADDRESS) a.context
            else if (addressSpace.childOf(a.context, b.group)) return b else a.context
        } else a.group
    var currentB =
        if (b.group == NULL_ADDRESS) {
            if (a.group == NULL_ADDRESS) b.context
            else if (addressSpace.childOf(b.context, a.group)) return a else b.context
        } else b.group

    if (currentA == currentB) return a

    var aParent = if (currentA == NULL_ADDRESS) NULL_ADDRESS else groups.groupParent(currentA)
    var bParent = if (currentB == NULL_ADDRESS) NULL_ADDRESS else groups.groupParent(currentB)

    // If a or b is a direct parent or child return the parent
    if (aParent == currentB) return b
    if (bParent == currentA) return a

    if (aParent != bParent) {
        // General case:
        //   1. Find the depth of a and b, then follow their parents to reach the same level.
        //   2. Continue traversing up the tree until finding a parent of a and b which share
        // parents.
        //   3. For the children that have the same parent, figure out which group comes first and
        //      return a or b accordingly.
        val aDepth = addressSpace.distanceFrom(currentA, root)
        val bDepth = addressSpace.distanceFrom(currentB, root)

        if (aDepth > bDepth) {
            repeat(aDepth - bDepth) {
                currentA = aParent
                aParent = groups.groupParent(aParent)
            }

            // b is one of a's parents
            if (currentA == currentB) return b
        } else {
            repeat(bDepth - aDepth) {
                currentB = bParent
                bParent = groups.groupParent(bParent)
            }
            // a is one of b's parents
            if (currentB == currentA) return a
        }

        while (aParent != bParent) {
            // Traverse upwards for A
            currentA = aParent
            aParent = groups.groupParent(aParent)

            // Traverse upwards for B
            currentB = bParent
            bParent = groups.groupParent(bParent)
        }
    }

    runtimeCheck(currentA != currentB) { "Unexpected slot table structure" }
    return when (addressSpace.findFirstSibling(aParent, currentA, currentB)) {
        currentA -> a
        currentB -> b
        else -> composeRuntimeError("Unexpected slot table structure")
    }
}

private fun SlotTableAddressSpace.findFirstSibling(
    parent: GroupAddress,
    a: GroupAddress,
    b: GroupAddress,
): GroupAddress {
    // Fast path: a or b points past the end of the group. Return the other group. It's either
    //  first or also pointing to the same space.
    if (a == NULL_ADDRESS) return b
    if (b == NULL_ADDRESS) return a

    traverseChildren(parent) { group ->
        if (group == a) return a
        if (group == b) return b
    }
    composeRuntimeError("Unexpected slot table structure")
}

private inline fun SlotTableAddressSpace.childOf(
    parent: GroupAddress,
    child: GroupAddress,
): Boolean {
    traverseGroupAndParents(child) { group -> if (group == parent) return true }
    return false
}

private fun SlotTable.collectNodesFrom(group: GroupAddress): List<Any?> {
    val result = mutableListOf<Any?>()
    read {
        traverseGroupPartially(group) { current ->
            if (isNode(current)) {
                result.add(node(current))
                false
            } else true
        }
    }
    return result
}
