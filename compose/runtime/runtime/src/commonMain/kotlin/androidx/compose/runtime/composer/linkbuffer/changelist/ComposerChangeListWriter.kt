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

package androidx.compose.runtime.composer.linkbuffer.changelist

import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LinkComposer
import androidx.compose.runtime.MovableContentState
import androidx.compose.runtime.MovableContentStateReference
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RememberObserverHolder
import androidx.compose.runtime.Stack
import androidx.compose.runtime.composer.linkbuffer.GroupAddress
import androidx.compose.runtime.composer.linkbuffer.GroupHandle
import androidx.compose.runtime.composer.linkbuffer.NULL_ADDRESS
import androidx.compose.runtime.composer.linkbuffer.NULL_GROUP_HANDLE
import androidx.compose.runtime.composer.linkbuffer.SlotTable
import androidx.compose.runtime.composer.linkbuffer.SlotTableReader
import androidx.compose.runtime.composer.linkbuffer.context
import androidx.compose.runtime.composer.linkbuffer.group
import androidx.compose.runtime.composer.linkbuffer.groupFlagsNodeCount
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.runtimeCheck

internal enum class ComposerChangeListWriterAddressMode {
    /**
     * This mode is the normal mode of the change list writer where seek is performed by using the
     * group address.
     */
    AbsoluteAddressing,

    /**
     * This mode is used for movable content that is moved from one slot table to another where the
     * group address will change. This uses an anchor instead of the group address directly.
     */
    AnchorAddressing,

    /**
     * This mode is used for movable content because the slot table will move during composition and
     * absolute addressing will refer to the wrong group (the one where it currently is, not where
     * it is going) so this triggers relative addressing which will use `startGroup` instead of
     * `seek`.
     */
    RelativeAddressing,
}

internal class ComposerChangeListWriter(
    /**
     * The [androidx.compose.runtime.Composer] that is building this ChangeList. The Composer's
     * state may be used to determine how the ChangeList should be written to.
     */
    private val composer: LinkComposer,
    /** The ChangeList that will be written to */
    var changeList: ChangeList,
) {
    private val reader: SlotTableReader
        get() = composer.reader

    /**
     * When inserting movable content, the group start and end is handled elsewhere. This flag lets
     * us disable automatic insertion of the root group for movable content. Set to `false` when
     * inserting movable content.
     */
    var implicitRootStart: Boolean = true

    // Navigation of the node tree is performed by recording all the locations of the nodes as
    // they are traversed by the reader and recording them in the downNodes array. When the node
    // navigation is realized all the downs in the down nodes is played to the applier.
    //
    // If an up is recorded before the corresponding down is realized then it is simply removed
    // from the downNodes stack.
    private var pendingUps = 0
    private val pendingDownNodes = Stack<Any?>()

    private var removeFromNodeIndex = -1
    private var moveFromNodeIndex = -1
    private var moveToNodeIndex = -1
    private var moveCount = -1
    internal var addressMode = ComposerChangeListWriterAddressMode.AbsoluteAddressing

    /**
     * Tracks the expected location that the writer will be in after the last operation pushed to
     * the change list being built.
     */
    private var editorCurrentPosition = NULL_GROUP_HANDLE

    private fun pushApplierOperationPreamble() {
        pushPendingUpsAndDowns()
    }

    private fun pushSlotOperationPreamble() {
        val readerLocation = reader.handle()
        if (editorCurrentPosition != readerLocation) {
            seekTo(readerLocation)
        }
    }

    private fun pushSlotOperationPreambleUnconditionally() {
        seekTo(reader.handle())
    }

    fun startComposition() {
        pendingDownNodes.clear()
        pendingUps = 0
        removeFromNodeIndex = -1
        moveFromNodeIndex = -1
        moveToNodeIndex = -1
        addressMode = ComposerChangeListWriterAddressMode.AbsoluteAddressing
        editorCurrentPosition = NULL_GROUP_HANDLE
    }

    fun seekTo(handle: GroupHandle, resetRelativeAddressing: Boolean = false) {
        when (addressMode) {
            ComposerChangeListWriterAddressMode.AbsoluteAddressing ->
                changeList.pushSeekToGroupHandle(handle)

            ComposerChangeListWriterAddressMode.AnchorAddressing ->
                changeList.pushSeekToAnchor(reader.table.addressSpace, handle)

            ComposerChangeListWriterAddressMode.RelativeAddressing -> {
                val groupAddress = handle.group
                val parentOfHandle =
                    if (groupAddress == NULL_ADDRESS) handle.context
                    else reader.parentOf(groupAddress)
                runtimeCheck(parentOfHandle == editorCurrentPosition.group) {
                    "Relative addressing only supports navigating to a child of the current group"
                }
                changeList.pushStartGroup()
                var currentChild = reader.firstChildOf(parentOfHandle)
                while (currentChild != groupAddress) {
                    changeList.pushSkipGroup()
                    currentChild = reader.nextSiblingOf(currentChild)
                }
                // Once into the movable content that we need to relative address into,
                // we can switch to absolute addressing
                if (resetRelativeAddressing) {
                    addressMode = ComposerChangeListWriterAddressMode.AbsoluteAddressing
                }
            }
        }
        editorCurrentPosition = handle
    }

    val isInAnchorMode
        get() = addressMode == ComposerChangeListWriterAddressMode.AnchorAddressing

    inline fun inAnchorMode(block: () -> Unit) {
        editorCurrentPosition = NULL_GROUP_HANDLE
        inMode(ComposerChangeListWriterAddressMode.AnchorAddressing, block)
    }

    inline fun inRelativeAddressMode(relativeStart: GroupHandle, block: () -> Unit) {
        editorCurrentPosition = relativeStart
        inMode(ComposerChangeListWriterAddressMode.RelativeAddressing, block)
    }

    private inline fun inMode(newMode: ComposerChangeListWriterAddressMode, block: () -> Unit) {
        val previousMode = addressMode
        val previousCurrentPosition = editorCurrentPosition
        addressMode = newMode
        try {
            block()
        } finally {
            addressMode = previousMode

            // Once we leave an address mode we can no longer make assumptions about the current
            // state of the writer
            editorCurrentPosition =
                if (previousMode == ComposerChangeListWriterAddressMode.RelativeAddressing)
                    previousCurrentPosition
                else NULL_GROUP_HANDLE
        }
    }

    inline fun withChangeList(newChangeList: ChangeList, block: () -> Unit) {
        val previousChangeList = changeList
        try {
            changeList = newChangeList
            block()
        } finally {
            changeList = previousChangeList
        }
    }

    inline fun withoutImplicitRootStart(block: () -> Unit) {
        val previousImplicitRootStart = implicitRootStart
        try {
            implicitRootStart = false
            block()
        } finally {
            implicitRootStart = previousImplicitRootStart
        }
    }

    fun remember(value: RememberObserverHolder) {
        pushSlotOperationPreamble()
        changeList.pushRemember(value)
    }

    fun rememberPausingScope(scope: RecomposeScopeImpl) {
        changeList.pushRememberPausingScope(scope)
    }

    fun startResumingScope(scope: RecomposeScopeImpl) {
        changeList.pushStartResumingScope(scope)
    }

    fun endResumingScope(scope: RecomposeScopeImpl) {
        changeList.pushEndResumingScope(scope)
    }

    fun updateRememberOrdering(holder: RememberObserverHolder, after: GroupAddress) {
        if (holder.afterGroupIndex != after) {
            changeList.pushUpdateRememberObserverHolderOrdering(holder, after)
        }
    }

    fun updateValue(slotIndex: Int, value: Any?) {
        pushSlotOperationPreamble()
        changeList.pushUpdateRelativeValue(slotIndex = slotIndex, value = value)
    }

    fun appendValue(value: Any?) {
        pushSlotOperationPreamble()
        changeList.pushAppendValue(value)
    }

    fun removeTailGroupsAndValues(firstTailGroupToRemove: GroupAddress, count: Int) {
        if (firstTailGroupToRemove >= 0 || count > 0) {
            pushSlotOperationPreamble()
            changeList.pushRemoveTailGroupsAndValues(
                firstTailGroupToRemove = firstTailGroupToRemove,
                count = count,
            )
        }
    }

    fun resetSlots() {
        changeList.pushResetSlots()
        editorCurrentPosition = NULL_GROUP_HANDLE
    }

    fun updateAuxData(data: Any?) {
        pushSlotOperationPreamble()
        changeList.pushUpdateAuxData(data)
    }

    fun removeGroup() {
        pushSlotOperationPreamble()
        changeList.pushRemoveGroup()
    }

    fun insertSlots(sourceTable: SlotTable, source: GroupHandle) {
        runtimeCheck(source != NULL_GROUP_HANDLE) { "Tried moving from an unspecified position" }
        pushPendingUpsAndDowns()
        pushSlotOperationPreamble()
        realizeNodeMovementOperations()
        changeList.pushInsertSlots(sourceTable, source)
    }

    fun insertSlots(sourceTable: SlotTable, source: GroupHandle, fixups: FixupList) {
        runtimeCheck(source != NULL_GROUP_HANDLE) { "Tried moving from an unspecified position" }
        pushPendingUpsAndDowns()
        pushSlotOperationPreamble()
        realizeNodeMovementOperations()
        changeList.pushInsertSlots(sourceTable, source, fixups)
    }

    fun moveGroup(offset: Int) {
        runtimeCheck(offset >= 0) { "Offset must not be negative" }
        pushSlotOperationPreambleUnconditionally()
        changeList.pushMoveGroup(offset)
        editorCurrentPosition = NULL_GROUP_HANDLE
    }

    fun endCompositionScope(action: (Composition) -> Unit, composition: Composition) {
        changeList.pushEndCompositionScope(action, composition)
    }

    fun useNode(node: Any?) {
        pushApplierOperationPreamble()
        changeList.pushUseNode(node)
    }

    fun <T, V> updateNode(value: V, block: T.(V) -> Unit) {
        pushApplierOperationPreamble()
        changeList.pushUpdateNode(value, block)
    }

    fun removeNode(nodeIndex: Int, count: Int) {
        if (count > 0) {
            if (removeFromNodeIndex == nodeIndex) {
                moveCount += count
            } else {
                realizeNodeMovementOperations()
                removeFromNodeIndex = nodeIndex
                moveCount = count
            }
        }
    }

    fun moveNode(fromNodeIndex: Int, toNodeIndex: Int, count: Int) {
        if (count > 0) {
            if (
                moveCount > 0 &&
                    moveFromNodeIndex == fromNodeIndex &&
                    moveToNodeIndex == toNodeIndex
            ) {
                moveCount += count
            } else {
                realizeNodeMovementOperations()
                moveToNodeIndex = toNodeIndex
                moveFromNodeIndex = fromNodeIndex
                moveCount = count
            }
        }
    }

    fun endNodeMovement() {
        realizeNodeMovementOperations()
    }

    fun endNodeMovementAndDeleteNode(nodeIndex: Int, group: GroupAddress) {
        endNodeMovement()
        pushPendingUpsAndDowns()
        removeNode(nodeIndex, groupFlagsNodeCount(reader.flagsOf(group)))
    }

    private fun realizeNodeMovementOperations() {
        if (moveCount > 0) {
            if (removeFromNodeIndex >= 0) {
                realizeRemoveNode(removeFromNodeIndex, moveCount)
                removeFromNodeIndex = -1
            } else {
                realizeMoveNode(moveToNodeIndex, moveFromNodeIndex, moveCount)
                moveToNodeIndex = -1
                moveFromNodeIndex = -1
            }
            moveCount = 0
        }
    }

    private fun realizeRemoveNode(nodeIndex: Int, removeCount: Int) {
        pushApplierOperationPreamble()
        changeList.pushRemoveNode(nodeIndex, removeCount)
    }

    private fun realizeMoveNode(to: Int, from: Int, count: Int) {
        pushApplierOperationPreamble()
        changeList.pushMoveNode(to, from, count)
    }

    fun moveUp() {
        realizeNodeMovementOperations()
        if (pendingDownNodes.isNotEmpty()) {
            pendingDownNodes.pop()
        } else {
            pendingUps++
        }
    }

    fun moveDown(node: Any?) {
        realizeNodeMovementOperations()
        pendingDownNodes.push(node)
    }

    private fun pushPendingUpsAndDowns() {
        if (pendingUps > 0) {
            changeList.pushUps(pendingUps)
            pendingUps = 0
        }

        if (pendingDownNodes.isNotEmpty()) {
            changeList.pushDowns(pendingDownNodes.toArray())
            pendingDownNodes.clear()
        }
    }

    fun sideEffect(effect: () -> Unit) {
        changeList.pushSideEffect(effect)
    }

    fun determineMovableContentNodeIndex(effectiveNodeIndexOut: IntRef, handle: GroupHandle) {
        pushPendingUpsAndDowns()
        changeList.pushDetermineMovableContentNodeIndex(effectiveNodeIndexOut, handle)
        editorCurrentPosition = handle
    }

    fun copyNodesToNewAnchorLocation(nodes: List<Any?>, effectiveNodeIndex: IntRef) {
        changeList.pushCopyNodesToNewAnchorLocation(nodes, effectiveNodeIndex)
    }

    @OptIn(InternalComposeApi::class)
    fun copySlotTableToAnchorLocation(
        resolvedState: MovableContentState?,
        parentContext: CompositionContext,
        from: MovableContentStateReference,
        to: MovableContentStateReference,
    ) {
        changeList.pushCopySlotTableToAnchorLocation(resolvedState, parentContext, from, to)
    }

    @OptIn(InternalComposeApi::class)
    fun releaseMovableGroup(
        composition: ControlledComposition,
        parentContext: CompositionContext,
        reference: MovableContentStateReference,
    ) {
        changeList.pushReleaseMovableGroup(composition, parentContext, reference)

        // Invalidate our current position as it may have changed as a result of moving the
        // MovableContent's slots out of the writer.
        editorCurrentPosition = NULL_GROUP_HANDLE
    }

    fun endMovableContentPlacement() {
        changeList.pushEndMovableContentPlacement()
        pendingUps = 0
    }

    @OptIn(InternalComposeApi::class)
    fun disposeResolvedMovableState(resolvedState: MovableContentState?) {
        if (resolvedState != null) {
            changeList.pushDisposeDisposeMovableContentState(resolvedState)
        }
    }

    fun includeOperationsIn(other: ChangeList, effectiveNodeIndex: IntRef? = null) {
        changeList.pushExecuteOperationsIn(other, effectiveNodeIndex)
    }

    fun finalizeComposition() {
        pushPendingUpsAndDowns()
        changeList.pushClearAllRecompositionRequiredGroups()
        editorCurrentPosition = NULL_GROUP_HANDLE
    }

    fun resetTransientState() {
        implicitRootStart = true

        pendingUps = 0
        pendingDownNodes.clear()

        removeFromNodeIndex = -1
        moveFromNodeIndex = -1
        moveToNodeIndex = -1
        moveCount = -1
        editorCurrentPosition = NULL_GROUP_HANDLE
    }

    fun deactivateCurrentGroup() {
        pushSlotOperationPreamble()
        changeList.pushDeactivateGroup()
    }
}
