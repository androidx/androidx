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

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Changes
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MovableContentState
import androidx.compose.runtime.MovableContentStateReference
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RememberObserverHolder
import androidx.compose.runtime.SlotStorage
import androidx.compose.runtime.composeRuntimeError
import androidx.compose.runtime.composer.RememberManager
import androidx.compose.runtime.composer.gapbuffer.changelist.OperationErrorContext
import androidx.compose.runtime.composer.linkbuffer.GroupAddress
import androidx.compose.runtime.composer.linkbuffer.GroupHandle
import androidx.compose.runtime.composer.linkbuffer.SlotAddress
import androidx.compose.runtime.composer.linkbuffer.SlotTable
import androidx.compose.runtime.composer.linkbuffer.SlotTableAddressSpace
import androidx.compose.runtime.composer.linkbuffer.SlotTableEditor
import androidx.compose.runtime.composer.linkbuffer.anchorHandle
import androidx.compose.runtime.composer.linkbuffer.asLinkBufferSlotTable
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.AppendValue
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.ApplyChangeList
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.ClearAllRecompositionRequired
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.CopyNodesToNewAnchorLocation
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.CopySlotTableToHandleLocation
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.DeactivateGroup
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.DetermineMovableContentNodeIndex
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.Downs
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.EndCompositionScope
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.EndMovableContentPlacement
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.EndResumingScope
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.InsertSlots
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.InsertSlotsWithFixups
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.MoveGroup
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.MoveNode
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.ReleaseMovableGroup
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.Remember
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.RememberPausingScope
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.RemoveGroup
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.RemoveNode
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.RemoveTailGroupsAndValues
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.ResetSlots
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.SeekToGroupHandle
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.SideEffect
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.StartResumingScope
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.UpdateAuxData
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.UpdateNode
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.UpdateRememberObserverHolderOrdering
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.UpdateValue
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.UpdateValueRelative
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.Ups
import androidx.compose.runtime.composer.linkbuffer.changelist.Operation.UseCurrentNode
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.tooling.CompositionErrorContextImpl

internal class ChangeList : Changes() {

    private val operations = Operations()

    val size: Int
        get() = operations.size

    override fun isEmpty() = operations.isEmpty()

    fun hasChangesRequiringApplication() = operations.requiresApplication

    override fun clear() {
        operations.clear()
    }

    override fun execute(
        slotStorage: SlotStorage,
        applier: Applier<*>,
        rememberManager: RememberManager,
        errorContext: CompositionErrorContextImpl?,
    ) {
        val slotTable = slotStorage.asLinkBufferSlotTable()
        slotTable.edit {
            executeAndFlushAllPendingChanges(applier, this, rememberManager, errorContext)
        }
    }

    fun executeAndFlushAllPendingChanges(
        applier: Applier<*>,
        slots: SlotTableEditor,
        rememberManager: RememberManager,
        errorContext: OperationErrorContext?,
    ) =
        operations.executeAndFlushAllPendingOperations(
            applier,
            slots,
            rememberManager,
            errorContext,
        )

    fun pushRemember(value: RememberObserverHolder) {
        operations.push(Remember) { setObject(Remember.Value, value) }
    }

    fun pushRememberPausingScope(scope: RecomposeScopeImpl) {
        operations.push(RememberPausingScope) { setObject(RememberPausingScope.Scope, scope) }
    }

    fun pushStartResumingScope(scope: RecomposeScopeImpl) {
        operations.push(StartResumingScope) { setObject(StartResumingScope.Scope, scope) }
    }

    fun pushEndResumingScope(scope: RecomposeScopeImpl) {
        operations.push(EndResumingScope) { setObject(EndResumingScope.Scope, scope) }
    }

    fun pushUpdateRememberObserverHolderOrdering(
        holder: RememberObserverHolder,
        after: GroupAddress,
    ) {
        operations.push(UpdateRememberObserverHolderOrdering) {
            setObject(UpdateRememberObserverHolderOrdering.Holder, holder)
            setInt(UpdateRememberObserverHolderOrdering.After, after)
        }
    }

    fun pushUpdateRelativeValue(slotIndex: Int, value: Any?) {
        operations.push(UpdateValueRelative) {
            setInt(UpdateValueRelative.SlotIndex, slotIndex)
            setObject(UpdateValueRelative.Value, value)
        }
    }

    fun pushUpdateValue(groupSlotAddress: SlotAddress, value: Any?) {
        operations.push(UpdateValue) {
            setObject(UpdateValue.Value, value)
            setInt(UpdateValue.SlotAddress, groupSlotAddress)
        }
    }

    fun pushAppendValue(value: Any?) {
        operations.push(AppendValue) { setObject(AppendValue.Value, value) }
    }

    fun pushRemoveTailGroupsAndValues(firstTailGroupToRemove: GroupAddress, count: Int) {
        operations.push(RemoveTailGroupsAndValues) {
            setInts(
                RemoveTailGroupsAndValues.FirstTailGroupToRemove,
                firstTailGroupToRemove,
                RemoveTailGroupsAndValues.TailSlotCount,
                count,
            )
        }
    }

    fun pushResetSlots() {
        operations.push(ResetSlots)
    }

    fun pushDeactivateGroup() {
        operations.push(DeactivateGroup)
    }

    fun pushUpdateAuxData(data: Any?) {
        operations.push(UpdateAuxData) { setObject(UpdateAuxData.Data, data) }
    }

    fun pushRemoveGroup() {
        operations.push(RemoveGroup)
    }

    fun pushInsertSlots(sourceTable: SlotTable, source: GroupHandle) {
        operations.push(InsertSlots) {
            setLong(
                highParameter = InsertSlots.SourceHighBits,
                lowParameter = InsertSlots.SourceLowBits,
                value = source,
            )
            setObject(InsertSlots.FromSlotTable, sourceTable)
        }
    }

    fun pushInsertSlots(sourceTable: SlotTable, source: GroupHandle, fixups: FixupList) {
        operations.push(InsertSlotsWithFixups) {
            setLong(
                highParameter = InsertSlotsWithFixups.SourceHighBits,
                lowParameter = InsertSlotsWithFixups.SourceLowBits,
                value = source,
            )
            setObjects(
                InsertSlotsWithFixups.FromSlotTable,
                sourceTable,
                InsertSlotsWithFixups.Fixups,
                fixups,
            )
        }
    }

    fun pushMoveGroup(offset: Int) {
        operations.push(MoveGroup) { setInt(MoveGroup.Offset, offset) }
    }

    fun pushClearAllRecompositionRequiredGroups() {
        operations.push(ClearAllRecompositionRequired)
    }

    fun pushEndCompositionScope(action: (Composition) -> Unit, composition: Composition) {
        operations.push(EndCompositionScope) {
            setObjects(
                EndCompositionScope.Action,
                action,
                EndCompositionScope.Composition,
                composition,
            )
        }
    }

    fun pushUseNode(node: Any?) {
        if (node is ComposeNodeLifecycleCallback) {
            operations.push(UseCurrentNode)
        }
    }

    fun <T, V> pushUpdateNode(value: V, block: T.(V) -> Unit) {
        operations.push(UpdateNode) {
            @Suppress("UNCHECKED_CAST")
            setObjects(UpdateNode.Value, value, UpdateNode.Block, block as (Any?.(Any?) -> Unit))
        }
    }

    fun pushRemoveNode(nodeIndex: Int, removeCount: Int) {
        operations.push(RemoveNode) {
            setInts(RemoveNode.RemoveIndex, nodeIndex, RemoveNode.Count, removeCount)
        }
    }

    fun pushMoveNode(to: Int, from: Int, count: Int) {
        operations.push(MoveNode) {
            setInts(MoveNode.To, to, MoveNode.From, from, MoveNode.Count, count)
        }
    }

    fun pushSeekToGroupHandle(handle: GroupHandle) {
        operations.push(SeekToGroupHandle) {
            setLong(
                highParameter = SeekToGroupHandle.GroupHandleHighBits,
                lowParameter = SeekToGroupHandle.GroupHandleLowBits,
                value = handle,
            )
        }
    }

    fun pushSeekToAnchor(addressSpace: SlotTableAddressSpace, handle: GroupHandle) {
        operations.push(Operation.SeekToAnchor) {
            setObject(Operation.SeekToAnchor.AnchorHandle, addressSpace.anchorHandle(handle))
        }
    }

    fun pushStartGroup() {
        operations.push(Operation.StartGroup)
    }

    fun pushSkipGroup() {
        operations.push(Operation.SkipGroup)
    }

    fun pushUps(count: Int) {
        operations.push(Ups) { setInt(Ups.Count, count) }
    }

    fun pushDowns(nodes: Array<Any?>) {
        if (nodes.isNotEmpty()) {
            operations.push(Downs) { setObject(Downs.Nodes, nodes) }
        }
    }

    fun pushSideEffect(effect: () -> Unit) {
        operations.push(SideEffect) { setObject(SideEffect.Effect, effect) }
    }

    fun pushDetermineMovableContentNodeIndex(
        effectiveNodeIndexOut: IntRef,
        groupHandle: GroupHandle,
    ) {
        operations.push(DetermineMovableContentNodeIndex) {
            setObject(DetermineMovableContentNodeIndex.EffectiveNodeIndexOut, effectiveNodeIndexOut)
            setLong(
                DetermineMovableContentNodeIndex.GroupHandleHighBits,
                DetermineMovableContentNodeIndex.GroupHandleLowBits,
                groupHandle,
            )
        }
    }

    fun pushCopyNodesToNewAnchorLocation(nodes: List<Any?>, effectiveNodeIndex: IntRef) {
        if (nodes.isNotEmpty()) {
            operations.push(CopyNodesToNewAnchorLocation) {
                setObjects(
                    CopyNodesToNewAnchorLocation.Nodes,
                    nodes,
                    CopyNodesToNewAnchorLocation.EffectiveNodeIndex,
                    effectiveNodeIndex,
                )
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
        operations.push(CopySlotTableToHandleLocation) {
            setObjects(
                CopySlotTableToHandleLocation.ResolvedState,
                resolvedState,
                CopySlotTableToHandleLocation.ParentCompositionContext,
                parentContext,
                CopySlotTableToHandleLocation.To,
                to,
                CopySlotTableToHandleLocation.From,
                from,
            )
        }
    }

    @OptIn(InternalComposeApi::class)
    fun pushReleaseMovableGroup(
        composition: ControlledComposition,
        parentContext: CompositionContext,
        reference: MovableContentStateReference,
    ) {
        operations.push(ReleaseMovableGroup) {
            setObjects(
                ReleaseMovableGroup.Composition,
                composition,
                ReleaseMovableGroup.ParentCompositionContext,
                parentContext,
                ReleaseMovableGroup.Reference,
                reference,
            )
        }
    }

    fun pushEndMovableContentPlacement() {
        operations.push(EndMovableContentPlacement)
    }

    @OptIn(InternalComposeApi::class)
    fun pushDisposeDisposeMovableContentState(resolvedState: MovableContentState) {
        operations.push(Operation.DisposeMovableContentState) {
            setObject(Operation.DisposeMovableContentState.ResolvedState, resolvedState)
        }
    }

    fun pushExecuteOperationsIn(changeList: ChangeList, effectiveNodeIndex: IntRef? = null) {
        if (changeList.isNotEmpty()) {
            operations.push(ApplyChangeList) {
                setObjects(
                    ApplyChangeList.Changes,
                    changeList,
                    ApplyChangeList.EffectiveNodeIndex,
                    effectiveNodeIndex,
                )
                // Ensure that if the ChangeList we're pushing requires applications that we
                // propagate this to the outer ChangeList. This ensures that if the outer change
                // list only includes non-externallyVisible changes that the operations in the inner
                // list can still require the applier.
                if (changeList.operations.requiresApplication) requireApplication()
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

internal fun Changes.asLinkBufferChangeList() =
    this as? ChangeList ?: composeRuntimeError("Inconsistent composition")
