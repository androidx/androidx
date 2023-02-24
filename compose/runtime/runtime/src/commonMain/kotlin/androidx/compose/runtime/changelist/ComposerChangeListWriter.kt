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
import androidx.compose.runtime.Change
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.ComposerImpl
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MovableContentState
import androidx.compose.runtime.MovableContentStateReference
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SlotTable
import androidx.compose.runtime.Stack

internal class ComposerChangeListWriter(
    /**
     * The [Composer][ComposerImpl] that is building this ChangeList. The Composer's state
     * may be used to determine how the ChangeList should be written to.
     */
    private val composer: ComposerImpl,
    /**
     * The ChangeList that will be written to
     */
    var changeList: ChangeList
) {

    // Navigation of the node tree is performed by recording all the locations of the nodes as
    // they are traversed by the reader and recording them in the downNodes array. When the node
    // navigation is realized all the downs in the down nodes is played to the applier.
    //
    // If an up is recorded before the corresponding down is realized then it is simply removed
    // from the downNodes stack.
    private var pendingUps = 0
    private var pendingDownNodes = Stack<Any?>()

    inline fun withChangeList(
        newChangeList: ChangeList,
        block: () -> Unit
    ) {
        val previousChangeList = changeList
        try {
            changeList = newChangeList
            block()
        } finally {
            changeList = previousChangeList
        }
    }

    fun deactivate(node: ComposeNodeLifecycleCallback) {
        changeList.pushDeactivate(node)
    }

    fun remember(value: RememberObserver) {
        changeList.pushRemember(value)
    }

    fun updateValue(value: Any?, groupSlotIndex: Int) {
        changeList.pushUpdateValue(value, groupSlotIndex)
    }

    fun resetSlots() {
        changeList.pushResetSlots()
    }

    fun clearSlotValue(index: Int, data: Any) {
        changeList.pushClearSlotValue(index, data)
    }

    fun updateAuxData(data: Any?) {
        changeList.pushUpdateAuxData(data)
    }

    fun ensureRootStarted() {
        changeList.pushEnsureRootStarted()
    }

    fun ensureGroupStarted(anchor: Anchor) {
        changeList.pushEnsureGroupStarted(anchor)
    }

    fun endCurrentGroup() {
        changeList.pushEndCurrentGroup()
    }

    fun skipToEndOfCurrentGroup() {
        changeList.pushSkipToEndOfCurrentGroup()
    }

    fun removeCurrentGroup() {
        changeList.pushRemoveCurrentGroup()
    }

    fun insertSlots(
        anchor: Anchor,
        from: SlotTable
    ) {
        changeList.pushInsertSlots(anchor, from)
    }

    fun insertSlots(
        anchor: Anchor,
        from: SlotTable,
        fixups: List<Change>
    ) {
        changeList.pushInsertSlots(anchor, from, fixups)
    }

    fun moveCurrentGroup(
        offset: Int
    ) {
        changeList.pushMoveCurrentGroup(offset)
    }

    fun endCompositionScope(
        action: (Composition) -> Unit,
        composition: Composition
    ) {
        changeList.pushEndCompositionScope(action, composition)
    }

    fun useNode(node: Any?) {
        changeList.pushUseNode(node)
    }

    fun <T, V> updateNode(value: V, block: T.(V) -> Unit) {
        changeList.pushUpdateNode(value, block)
    }

    fun removeNode(removeFrom: Int, moveCount: Int) {
        changeList.pushRemoveNode(removeFrom, moveCount)
    }

    fun moveNode(to: Int, from: Int, count: Int) {
        changeList.pushMoveNode(to, from, count)
    }

    fun advanceSlotsBy(distance: Int) {
        changeList.pushAdvanceSlotsBy(distance)
    }

    fun moveUp() {
        if (pendingDownNodes.isNotEmpty()) {
            pendingDownNodes.pop()
        } else {
            pendingUps++
        }
    }

    fun moveDown(node: Any?) {
        pendingDownNodes.push(node)
    }

    // TODO: Make private and remove explicit calls from Composer.
    fun pushPendingUpsAndDowns() {
        pushUps(pendingUps)
        pendingUps = 0

        pushDowns(pendingDownNodes.toArray())
        pendingDownNodes.clear()
    }

    private fun pushUps(count: Int) {
        if (count > 0) {
            changeList.pushUps(count)
        }
    }

    private fun pushDowns(nodes: Array<Any?>) {
        if (nodes.isNotEmpty()) {
            changeList.pushDowns(nodes)
        }
    }

    fun sideEffect(effect: () -> Unit) {
        changeList.pushSideEffect(effect)
    }

    fun determineMovableContentNodeIndex(
        effectiveNodeIndexOut: IntRef,
        anchor: Anchor
    ) {
        pushPendingUpsAndDowns()
        changeList.pushDetermineMovableContentNodeIndex(effectiveNodeIndexOut, anchor)
    }

    fun copyNodesToNewAnchorLocation(
        nodes: List<Any?>,
        effectiveNodeIndex: IntRef
    ) {
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
        reference: MovableContentStateReference
    ) {
        changeList.pushReleaseMovableGroupAtCurrent(composition, parentContext, reference)
    }

    fun endMovableContentPlacement() {
        changeList.pushEndMovableContentPlacement()
    }

    fun includeOperationsIn(
        other: ChangeList,
        effectiveNodeIndex: IntRef? = null
    ) {
        changeList.pushExecuteOperationsIn(other, effectiveNodeIndex)
    }

    fun finalizeComposition() {
        pushPendingUpsAndDowns()
    }
}