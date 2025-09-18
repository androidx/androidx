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

package androidx.compose.runtime.changelist

import androidx.compose.runtime.Anchor
import androidx.compose.runtime.ComposerImpl
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.IntStack
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MovableContentState
import androidx.compose.runtime.MovableContentStateReference
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RememberObserverHolder
import androidx.compose.runtime.SlotReader
import androidx.compose.runtime.SlotTable
import androidx.compose.runtime.Stack
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.runtimeCheck

internal class ComposerChangeListWriter(
    /**
     * The [Composer][ComposerImpl] that is building this ChangeList. The Composer's state may be
     * used to determine how the ChangeList should be written to.
     */
    private val composer: ComposerImpl,
    /** The ChangeList that will be written to */
    var changeList: ChangeList,
) {
    private val reader: SlotReader
        get() = composer.reader

    /**
     * Record whether any groups were stared. If no groups were started then the root group doesn't
     * need to be started or ended either.
     */
    private var startedGroup: Boolean = false

    /** A stack of the location of the groups that were started. */
    private val startedGroups = IntStack()

    /**
     * When inserting movable content, the group start and end is handled elsewhere. This flag lets
     * us disable automatic insertion of the root group for movable content. Set to `false` when
     * inserting movable content.
     */
    var implicitRootStart: Boolean = true

    // Navigating the writer slot is performed relatively as the location of a group in the writer
    // might be different than it is in the reader as groups can be inserted, deleted, or moved.
    //
    // writersReaderDelta tracks the difference between reader's current slot the current of
    // the writer must be before the recorded change is applied. Moving the writer to a location
    // is performed by advancing the writer the same the number of slots traversed by the reader
    // since the last write change. This works transparently for inserts. For deletes the number
    // of nodes deleted needs to be added to writersReaderDelta. When slots move the delta is
    // updated as if the move has already taken place. The delta is updated again once the group
    // begin edited is complete.
    //
    // The SlotTable requires that the group that contains any moves, inserts or removes must have
    // the group that contains the moved, inserted or removed groups be started with a startGroup
    // and terminated with a endGroup so the effects of the inserts, deletes, and moves can be
    // recorded correctly in its internal data structures. The startedGroups stack maintains the
    // groups that must be closed before we can move past the started group.

    /**
     * The skew or delta between where the writer will be and where the reader is now. This can be
     * thought of as the unrealized distance the writer must move to match the current slot in the
     * reader. When an operation affects the slot table the writer location must be realized by
     * moving the writer slot table the unrealized distance.
     */
    private var writersReaderDelta: Int = 0

    // Navigation of the node tree is performed by recording all the locations of the nodes as
    // they are traversed by the reader and recording them in the downNodes array. When the node
    // navigation is realized all the downs in the down nodes is played to the applier.
    //
    // If an up is recorded before the corresponding down is realized then it is simply removed
    // from the downNodes stack.
    private var pendingUps = 0
    private val pendingDownNodes = Stack<Any?>()

    private var removeFrom = -1
    private var moveFrom = -1
    private var moveTo = -1
    private var moveCount = 0

    private fun pushApplierOperationPreamble() {
        pushPendingUpsAndDowns()
    }

    private fun pushSlotEditingOperationPreamble() {
        realizeOperationLocation()
        recordSlotEditing()
    }

    private fun pushSlotTableOperationPreamble(useParentSlot: Boolean = false) {
        realizeOperationLocation(useParentSlot)
    }

    /** Called when reader current is moved directly, such as when a group moves, to [location]. */
    fun moveReaderRelativeTo(location: Int) {
        // Ensure the next skip will account for the distance we have already travelled.
        writersReaderDelta += location - reader.currentGroup
    }

    fun moveReaderToAbsolute(location: Int) {
        writersReaderDelta = location
    }

    fun recordSlotEditing() {
        // During initial composition (when the slot table is empty), no group needs
        // to be started.
        if (reader.size > 0) {
            val reader = reader
            val location = reader.parent

            if (startedGroups.peekOr(invalidGroupLocation) != location) {
                ensureRootStarted()

                if (location > 0) {
                    val anchor = reader.anchor(location)
                    startedGroups.push(location)
                    ensureGroupStarted(anchor)
                }
            }
        }
    }

    private fun ensureRootStarted() {
        if (!startedGroup && implicitRootStart) {
            pushSlotTableOperationPreamble()
            changeList.pushEnsureRootStarted()
            startedGroup = true
        }
    }

    private fun ensureGroupStarted(anchor: Anchor) {
        pushSlotTableOperationPreamble()
        changeList.pushEnsureGroupStarted(anchor)
        startedGroup = true
    }

    private fun realizeOperationLocation(forParent: Boolean = false) {
        val location = if (forParent) reader.parent else reader.currentGroup
        val distance = location - writersReaderDelta
        runtimeCheck(distance >= 0) { "Tried to seek backward" }
        if (distance > 0) {
            changeList.pushAdvanceSlotsBy(distance)
            writersReaderDelta = location
        }
    }

    val pastParent: Boolean
        get() = reader.parent - writersReaderDelta < 0

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

    fun updateValue(value: Any?, groupSlotIndex: Int) {
        pushSlotTableOperationPreamble(useParentSlot = true)
        changeList.pushUpdateValue(value, groupSlotIndex)
    }

    fun updateAnchoredValue(value: Any?, anchor: Anchor, groupSlotIndex: Int) {
        // Because this uses an anchor, it can be performed without positioning the writer.
        changeList.pushUpdateAnchoredValue(value, anchor, groupSlotIndex)
    }

    fun appendValue(anchor: Anchor, value: Any?) {
        // Because this uses an anchor, it can be performed without positioning the writer.
        changeList.pushAppendValue(anchor, value)
    }

    fun trimValues(count: Int) {
        if (count > 0) {
            pushSlotEditingOperationPreamble()
            changeList.pushTrimValues(count)
        }
    }

    fun resetSlots() {
        changeList.pushResetSlots()
    }

    fun updateAuxData(data: Any?) {
        pushSlotTableOperationPreamble()
        changeList.pushUpdateAuxData(data)
    }

    fun endRoot() {
        if (startedGroup) {
            pushSlotTableOperationPreamble()
            pushSlotTableOperationPreamble()
            changeList.pushEndCurrentGroup()
            startedGroup = false
        }
    }

    fun endCurrentGroup() {
        val location = reader.parent
        val currentStartedGroup = startedGroups.peekOr(-1)
        runtimeCheck(currentStartedGroup <= location) { "Missed recording an endGroup" }
        if (startedGroups.peekOr(-1) == location) {
            pushSlotTableOperationPreamble()
            startedGroups.pop()
            changeList.pushEndCurrentGroup()
        }
    }

    fun skipToEndOfCurrentGroup() {
        changeList.pushSkipToEndOfCurrentGroup()
    }

    fun removeCurrentGroup() {
        /*
          When a group is removed the reader will move but the writer will not so to ensure both
          the writer and reader are tracking the same slot we advance `writersReaderDelta` to
          account for the removal.
        */
        pushSlotEditingOperationPreamble()
        changeList.pushRemoveCurrentGroup()
        writersReaderDelta += reader.groupSize
    }

    fun insertSlots(anchor: Anchor, from: SlotTable) {
        pushPendingUpsAndDowns()
        pushSlotEditingOperationPreamble()
        realizeNodeMovementOperations()
        changeList.pushInsertSlots(anchor, from)
    }

    fun insertSlots(anchor: Anchor, from: SlotTable, fixups: FixupList) {
        pushPendingUpsAndDowns()
        pushSlotEditingOperationPreamble()
        realizeNodeMovementOperations()
        changeList.pushInsertSlots(anchor, from, fixups)
    }

    fun moveCurrentGroup(offset: Int) {
        pushSlotEditingOperationPreamble()
        changeList.pushMoveCurrentGroup(offset)
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
            runtimeCheck(nodeIndex >= 0) { "Invalid remove index $nodeIndex" }
            if (removeFrom == nodeIndex) {
                moveCount += count
            } else {
                realizeNodeMovementOperations()
                removeFrom = nodeIndex
                moveCount = count
            }
        }
    }

    fun moveNode(from: Int, to: Int, count: Int) {
        if (count > 0) {
            if (moveCount > 0 && moveFrom == from - moveCount && moveTo == to - moveCount) {
                moveCount += count
            } else {
                realizeNodeMovementOperations()
                moveFrom = from
                moveTo = to
                moveCount = count
            }
        }
    }

    fun releaseMovableContent() {
        pushPendingUpsAndDowns()
        if (startedGroup) {
            skipToEndOfCurrentGroup()
            endRoot()
        }
    }

    fun endNodeMovement() {
        realizeNodeMovementOperations()
    }

    fun endNodeMovementAndDeleteNode(nodeIndex: Int, group: Int) {
        endNodeMovement()
        pushPendingUpsAndDowns()
        val nodeCount = if (reader.isNode(group)) 1 else reader.nodeCount(group)
        if (nodeCount > 0) {
            removeNode(nodeIndex, nodeCount)
        }
    }

    private fun realizeNodeMovementOperations() {
        if (moveCount > 0) {
            if (removeFrom >= 0) {
                realizeRemoveNode(removeFrom, moveCount)
                removeFrom = -1
            } else {
                realizeMoveNode(moveTo, moveFrom, moveCount)

                moveFrom = -1
                moveTo = -1
            }
            moveCount = 0
        }
    }

    private fun realizeRemoveNode(removeFrom: Int, moveCount: Int) {
        pushApplierOperationPreamble()
        changeList.pushRemoveNode(removeFrom, moveCount)
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

    fun determineMovableContentNodeIndex(effectiveNodeIndexOut: IntRef, anchor: Anchor) {
        pushPendingUpsAndDowns()
        changeList.pushDetermineMovableContentNodeIndex(effectiveNodeIndexOut, anchor)
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
    fun releaseMovableGroupAtCurrent(
        composition: ControlledComposition,
        parentContext: CompositionContext,
        reference: MovableContentStateReference,
    ) {
        changeList.pushReleaseMovableGroupAtCurrent(composition, parentContext, reference)
    }

    fun endMovableContentPlacement() {
        pushPendingUpsAndDowns()
        changeList.pushEndMovableContentPlacement()
        writersReaderDelta = 0
    }

    fun includeOperationsIn(other: ChangeList, effectiveNodeIndex: IntRef? = null) {
        changeList.pushExecuteOperationsIn(other, effectiveNodeIndex)
    }

    fun finalizeComposition() {
        pushPendingUpsAndDowns()
        runtimeCheck(startedGroups.isEmpty()) { "Missed recording an endGroup()" }
    }

    fun resetTransientState() {
        startedGroup = false
        startedGroups.clear()
        writersReaderDelta = 0

        implicitRootStart = true

        pendingUps = 0
        pendingDownNodes.clear()

        removeFrom = -1
        moveFrom = -1
        moveTo = -1
        moveCount = 0
    }

    fun deactivateCurrentGroup() {
        pushSlotTableOperationPreamble()
        changeList.pushDeactivateCurrentGroup()
    }

    companion object {
        private const val invalidGroupLocation = -2
    }
}
