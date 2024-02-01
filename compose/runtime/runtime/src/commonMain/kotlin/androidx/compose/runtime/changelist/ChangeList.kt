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
import androidx.compose.runtime.Applier
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MovableContentState
import androidx.compose.runtime.MovableContentStateReference
import androidx.compose.runtime.RememberManager
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SlotTable
import androidx.compose.runtime.SlotWriter
import androidx.compose.runtime.changelist.Operation.AdvanceSlotsBy
import androidx.compose.runtime.changelist.Operation.AppendValue
import androidx.compose.runtime.changelist.Operation.ApplyChangeList
import androidx.compose.runtime.changelist.Operation.CopyNodesToNewAnchorLocation
import androidx.compose.runtime.changelist.Operation.CopySlotTableToAnchorLocation
import androidx.compose.runtime.changelist.Operation.DeactivateCurrentGroup
import androidx.compose.runtime.changelist.Operation.DetermineMovableContentNodeIndex
import androidx.compose.runtime.changelist.Operation.Downs
import androidx.compose.runtime.changelist.Operation.EndCompositionScope
import androidx.compose.runtime.changelist.Operation.EndCurrentGroup
import androidx.compose.runtime.changelist.Operation.EndMovableContentPlacement
import androidx.compose.runtime.changelist.Operation.EnsureGroupStarted
import androidx.compose.runtime.changelist.Operation.EnsureRootGroupStarted
import androidx.compose.runtime.changelist.Operation.InsertSlots
import androidx.compose.runtime.changelist.Operation.InsertSlotsWithFixups
import androidx.compose.runtime.changelist.Operation.MoveCurrentGroup
import androidx.compose.runtime.changelist.Operation.MoveNode
import androidx.compose.runtime.changelist.Operation.ReleaseMovableGroupAtCurrent
import androidx.compose.runtime.changelist.Operation.Remember
import androidx.compose.runtime.changelist.Operation.RemoveCurrentGroup
import androidx.compose.runtime.changelist.Operation.RemoveNode
import androidx.compose.runtime.changelist.Operation.ResetSlots
import androidx.compose.runtime.changelist.Operation.SideEffect
import androidx.compose.runtime.changelist.Operation.SkipToEndOfCurrentGroup
import androidx.compose.runtime.changelist.Operation.TrimParentValues
import androidx.compose.runtime.changelist.Operation.UpdateAnchoredValue
import androidx.compose.runtime.changelist.Operation.UpdateAuxData
import androidx.compose.runtime.changelist.Operation.UpdateNode
import androidx.compose.runtime.changelist.Operation.UpdateValue
import androidx.compose.runtime.changelist.Operation.Ups
import androidx.compose.runtime.changelist.Operation.UseCurrentNode
import androidx.compose.runtime.internal.IntRef

internal class ChangeList : OperationsDebugStringFormattable() {

    private val operations = Operations()

    val size: Int get() = operations.size
    fun isEmpty() = operations.isEmpty()
    fun isNotEmpty() = operations.isNotEmpty()

    fun clear() {
        operations.clear()
    }

    fun executeAndFlushAllPendingChanges(
        applier: Applier<*>,
        slots: SlotWriter,
        rememberManager: RememberManager
    ) = operations.executeAndFlushAllPendingOperations(applier, slots, rememberManager)

    fun pushRemember(value: RememberObserver) {
        operations.push(Remember) {
            setObject(Remember.Value, value)
        }
    }

    fun pushUpdateValue(value: Any?, groupSlotIndex: Int) {
        operations.push(UpdateValue) {
            setObject(UpdateValue.Value, value)
            setInt(UpdateValue.GroupSlotIndex, groupSlotIndex)
        }
    }

    fun pushUpdateAnchoredValue(value: Any?, anchor: Anchor, groupSlotIndex: Int) {
        operations.push(UpdateAnchoredValue) {
            setObject(UpdateAnchoredValue.Value, value)
            setObject(UpdateAnchoredValue.Anchor, anchor)
            setInt(UpdateAnchoredValue.GroupSlotIndex, groupSlotIndex)
        }
    }

    fun pushAppendValue(anchor: Anchor, value: Any?) {
        operations.push(AppendValue) {
            setObject(AppendValue.Anchor, anchor)
            setObject(AppendValue.Value, value)
        }
    }

    fun pushTrimValues(count: Int) {
        operations.push(TrimParentValues) {
            setInt(TrimParentValues.Count, count)
        }
    }

    fun pushResetSlots() {
        operations.push(ResetSlots)
    }

    fun pushDeactivateCurrentGroup() {
        operations.push(DeactivateCurrentGroup)
    }

    fun pushUpdateAuxData(data: Any?) {
        operations.push(UpdateAuxData) {
            setObject(UpdateAuxData.Data, data)
        }
    }

    fun pushEnsureRootStarted() {
        operations.push(EnsureRootGroupStarted)
    }

    fun pushEnsureGroupStarted(anchor: Anchor) {
        operations.push(EnsureGroupStarted) {
            setObject(EnsureGroupStarted.Anchor, anchor)
        }
    }

    fun pushEndCurrentGroup() {
        operations.push(EndCurrentGroup)
    }

    fun pushSkipToEndOfCurrentGroup() {
        operations.push(SkipToEndOfCurrentGroup)
    }

    fun pushRemoveCurrentGroup() {
        operations.push(RemoveCurrentGroup)
    }

    fun pushInsertSlots(
        anchor: Anchor,
        from: SlotTable
    ) {
        operations.push(InsertSlots) {
            setObject(InsertSlots.Anchor, anchor)
            setObject(InsertSlots.FromSlotTable, from)
        }
    }

    fun pushInsertSlots(
        anchor: Anchor,
        from: SlotTable,
        fixups: FixupList
    ) {
        operations.push(InsertSlotsWithFixups) {
            setObject(InsertSlotsWithFixups.Anchor, anchor)
            setObject(InsertSlotsWithFixups.FromSlotTable, from)
            setObject(InsertSlotsWithFixups.Fixups, fixups)
        }
    }

    fun pushMoveCurrentGroup(
        offset: Int
    ) {
        operations.push(MoveCurrentGroup) {
            setInt(MoveCurrentGroup.Offset, offset)
        }
    }

    fun pushEndCompositionScope(
        action: (Composition) -> Unit,
        composition: Composition
    ) {
        operations.push(EndCompositionScope) {
            setObject(EndCompositionScope.Action, action)
            setObject(EndCompositionScope.Composition, composition)
        }
    }

    fun pushUseNode(node: Any?) {
        if (node is ComposeNodeLifecycleCallback) {
            operations.push(UseCurrentNode)
        }
    }

    fun <T, V> pushUpdateNode(value: V, block: T.(V) -> Unit) {
        operations.push(UpdateNode) {
            setObject(UpdateNode.Value, value)
            @Suppress("UNCHECKED_CAST")
            setObject(UpdateNode.Block, block as (Any?.(Any?) -> Unit))
        }
    }

    fun pushRemoveNode(removeFrom: Int, moveCount: Int) {
        operations.push(RemoveNode) {
            setInt(RemoveNode.RemoveIndex, removeFrom)
            setInt(RemoveNode.Count, moveCount)
        }
    }

    fun pushMoveNode(to: Int, from: Int, count: Int) {
        operations.push(MoveNode) {
            setInt(MoveNode.To, to)
            setInt(MoveNode.From, from)
            setInt(MoveNode.Count, count)
        }
    }

    fun pushAdvanceSlotsBy(distance: Int) {
        operations.push(AdvanceSlotsBy) {
            setInt(AdvanceSlotsBy.Distance, distance)
        }
    }

    fun pushUps(count: Int) {
        operations.push(Ups) {
            setInt(Ups.Count, count)
        }
    }

    fun pushDowns(nodes: Array<Any?>) {
        if (nodes.isNotEmpty()) {
            operations.push(Downs) {
                setObject(Downs.Nodes, nodes)
            }
        }
    }

    fun pushSideEffect(effect: () -> Unit) {
        operations.push(SideEffect) {
            setObject(SideEffect.Effect, effect)
        }
    }

    fun pushDetermineMovableContentNodeIndex(
        effectiveNodeIndexOut: IntRef,
        anchor: Anchor
    ) {
        operations.push(DetermineMovableContentNodeIndex) {
            setObject(DetermineMovableContentNodeIndex.EffectiveNodeIndexOut, effectiveNodeIndexOut)
            setObject(DetermineMovableContentNodeIndex.Anchor, anchor)
        }
    }

    fun pushCopyNodesToNewAnchorLocation(
        nodes: List<Any?>,
        effectiveNodeIndex: IntRef
    ) {
        if (nodes.isNotEmpty()) {
            operations.push(CopyNodesToNewAnchorLocation) {
                setObject(CopyNodesToNewAnchorLocation.Nodes, nodes)
                setObject(CopyNodesToNewAnchorLocation.EffectiveNodeIndex, effectiveNodeIndex)
            }
        }
    }

    @OptIn(InternalComposeApi::class)
    fun pushCopySlotTableToAnchorLocation(
        resolvedState: MovableContentState?,
        parentContext: CompositionContext,
        from: MovableContentStateReference,
        to: MovableContentStateReference,
    ) {
        operations.push(CopySlotTableToAnchorLocation) {
            setObject(CopySlotTableToAnchorLocation.ResolvedState, resolvedState)
            setObject(CopySlotTableToAnchorLocation.ParentCompositionContext, parentContext)
            setObject(CopySlotTableToAnchorLocation.To, to)
            setObject(CopySlotTableToAnchorLocation.From, from)
        }
    }

    @OptIn(InternalComposeApi::class)
    fun pushReleaseMovableGroupAtCurrent(
        composition: ControlledComposition,
        parentContext: CompositionContext,
        reference: MovableContentStateReference
    ) {
        operations.push(ReleaseMovableGroupAtCurrent) {
            setObject(ReleaseMovableGroupAtCurrent.Composition, composition)
            setObject(ReleaseMovableGroupAtCurrent.ParentCompositionContext, parentContext)
            setObject(ReleaseMovableGroupAtCurrent.Reference, reference)
        }
    }

    fun pushEndMovableContentPlacement() {
        operations.push(EndMovableContentPlacement)
    }

    fun pushExecuteOperationsIn(
        changeList: ChangeList,
        effectiveNodeIndex: IntRef? = null
    ) {
        if (changeList.isNotEmpty()) {
            operations.push(ApplyChangeList) {
                setObject(ApplyChangeList.Changes, changeList)
                setObject(ApplyChangeList.EffectiveNodeIndex, effectiveNodeIndex)
            }
        }
    }

    override fun toDebugString(linePrefix: String): String {
        return buildString {
            append("ChangeList instance containing ")
            append(size)
            append(" operations")
            if (isNotEmpty()) {
                append(":\n")
                append(operations.toDebugString(linePrefix))
            }
        }
    }
}
