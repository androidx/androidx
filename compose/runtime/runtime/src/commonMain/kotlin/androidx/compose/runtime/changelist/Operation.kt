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
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.InvalidationResult
import androidx.compose.runtime.MovableContentState
import androidx.compose.runtime.MovableContentStateReference
import androidx.compose.runtime.OffsetApplier
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RecomposeScopeOwner
import androidx.compose.runtime.RememberManager
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.RememberObserverHolder
import androidx.compose.runtime.SlotTable
import androidx.compose.runtime.SlotWriter
import androidx.compose.runtime.TestOnly
import androidx.compose.runtime.composeRuntimeError
import androidx.compose.runtime.deactivateCurrentGroup
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.internal.identityHashCode
import androidx.compose.runtime.movableContentKey
import androidx.compose.runtime.removeCurrentGroup
import androidx.compose.runtime.runtimeCheck
import androidx.compose.runtime.snapshots.fastForEachIndexed
import androidx.compose.runtime.withAfterAnchorInfo
import kotlin.jvm.JvmInline

internal sealed class Operation(val ints: Int = 0, val objects: Int = 0) {

    val name: String
        get() = this::class.simpleName.orEmpty()

    abstract fun OperationArgContainer.execute(
        applier: Applier<*>,
        slots: SlotWriter,
        rememberManager: RememberManager
    )

    open fun intParamName(parameter: IntParameter): String = "IntParameter(${parameter.offset})"

    open fun objectParamName(parameter: ObjectParameter<*>): String =
        "ObjectParameter(${parameter.offset})"

    override fun toString() = name

    @JvmInline value class IntParameter(val offset: Int)

    @JvmInline value class ObjectParameter<T>(val offset: Int)

    // region traversal operations
    object Ups : Operation(ints = 1) {
        inline val Count
            get() = IntParameter(0)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                Count -> "count"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            repeat(getInt(Count)) { applier.up() }
        }
    }

    object Downs : Operation(objects = 1) {
        inline val Nodes
            get() = ObjectParameter<Array<Any?>>(0)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Nodes -> "nodes"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            @Suppress("UNCHECKED_CAST") val nodeApplier = applier as Applier<Any?>
            val nodes = getObject(Nodes)
            for (index in nodes.indices) {
                nodeApplier.down(nodes[index])
            }
        }
    }

    object AdvanceSlotsBy : Operation(ints = 1) {
        inline val Distance
            get() = IntParameter(0)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                Distance -> "distance"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            slots.advanceBy(getInt(Distance))
        }
    }

    // endregion traversal operations

    // region operations for Remember and SideEffects
    object SideEffect : Operation(objects = 1) {
        inline val Effect
            get() = ObjectParameter<() -> Unit>(0)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Effect -> "effect"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            rememberManager.sideEffect(getObject(Effect))
        }
    }

    object Remember : Operation(objects = 1) {
        inline val Value
            get() = ObjectParameter<RememberObserver>(0)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Value -> "value"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            rememberManager.remembering(getObject(Value))
        }
    }

    object RememberPausingScope : Operation(objects = 1) {
        inline val Scope
            get() = ObjectParameter<RecomposeScopeImpl>(0)

        override fun objectParamName(parameter: ObjectParameter<*>): String =
            when (parameter) {
                Scope -> "scope"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val scope = getObject(Scope)
            rememberManager.rememberPausingScope(scope)
        }
    }

    object StartResumingScope : Operation(objects = 1) {
        inline val Scope
            get() = ObjectParameter<RecomposeScopeImpl>(0)

        override fun objectParamName(parameter: ObjectParameter<*>): String =
            when (parameter) {
                Scope -> "scope"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val scope = getObject(Scope)
            rememberManager.startResumingScope(scope)
        }
    }

    object EndResumingScope : Operation(objects = 1) {
        inline val Scope
            get() = ObjectParameter<RecomposeScopeImpl>(0)

        override fun objectParamName(parameter: ObjectParameter<*>): String =
            when (parameter) {
                Scope -> "scope"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val scope = getObject(Scope)
            rememberManager.endResumingScope(scope)
        }
    }

    object AppendValue : Operation(objects = 2) {
        inline val Anchor
            get() = ObjectParameter<Anchor>(0)

        inline val Value
            get() = ObjectParameter<Any?>(1)

        override fun objectParamName(parameter: ObjectParameter<*>): String =
            when (parameter) {
                Anchor -> "anchor"
                Value -> "value"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val anchor = getObject(Anchor)
            val value = getObject(Value)
            if (value is RememberObserverHolder) {
                rememberManager.remembering(value.wrapped)
            }
            slots.appendSlot(anchor, value)
        }
    }

    object TrimParentValues : Operation(ints = 1) {
        inline val Count
            get() = IntParameter(0)

        override fun intParamName(parameter: IntParameter): String =
            when (parameter) {
                Count -> "count"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val count = getInt(Count)
            val slotsSize = slots.slotsSize
            slots.forEachTailSlot(slots.parent, count) { slotIndex, value ->
                when (value) {
                    is RememberObserverHolder -> {
                        // Values are always updated in the composition order (not slot table order)
                        // so there is no need to reorder these.
                        val endRelativeOrder = slotsSize - slotIndex
                        slots.withAfterAnchorInfo(value.after) { priority, endRelativeAfter ->
                            rememberManager.forgetting(
                                instance = value.wrapped,
                                endRelativeOrder = endRelativeOrder,
                                priority = priority,
                                endRelativeAfter = endRelativeAfter
                            )
                        }
                    }
                    is RecomposeScopeImpl -> value.release()
                }
            }
            slots.trimTailSlots(count)
        }
    }

    object UpdateValue : Operation(ints = 1, objects = 1) {
        inline val Value
            get() = ObjectParameter<Any?>(0)

        inline val GroupSlotIndex
            get() = IntParameter(0)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                GroupSlotIndex -> "groupSlotIndex"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Value -> "value"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val value = getObject(Value)
            val groupSlotIndex = getInt(GroupSlotIndex)
            if (value is RememberObserverHolder) {
                rememberManager.remembering(value.wrapped)
            }
            when (val previous = slots.set(groupSlotIndex, value)) {
                is RememberObserverHolder -> {
                    val endRelativeOrder =
                        slots.slotsSize -
                            slots.slotIndexOfGroupSlotIndex(slots.currentGroup, groupSlotIndex)
                    // Values are always updated in the composition order (not slot table order)
                    // so there is no need to reorder these.
                    rememberManager.forgetting(previous.wrapped, endRelativeOrder, -1, -1)
                }
                is RecomposeScopeImpl -> previous.release()
            }
        }
    }

    object UpdateAnchoredValue : Operation(objects = 2, ints = 1) {
        inline val Value
            get() = ObjectParameter<Any?>(0)

        inline val Anchor
            get() = ObjectParameter<Anchor>(1)

        inline val GroupSlotIndex
            get() = IntParameter(0)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                GroupSlotIndex -> "groupSlotIndex"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Value -> "value"
                Anchor -> "anchor"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val value = getObject(Value)
            val anchor = getObject(Anchor)
            val groupSlotIndex = getInt(GroupSlotIndex)
            if (value is RememberObserverHolder) {
                rememberManager.remembering(value.wrapped)
            }
            val groupIndex = slots.anchorIndex(anchor)
            when (val previous = slots.set(groupIndex, groupSlotIndex, value)) {
                is RememberObserverHolder -> {
                    val endRelativeSlotOrder =
                        slots.slotsSize -
                            slots.slotIndexOfGroupSlotIndex(groupIndex, groupSlotIndex)
                    slots.withAfterAnchorInfo(previous.after) { priority, endRelativeAfter ->
                        rememberManager.forgetting(
                            previous.wrapped,
                            endRelativeSlotOrder,
                            priority,
                            endRelativeAfter
                        )
                    }
                }
                is RecomposeScopeImpl -> previous.release()
            }
        }
    }

    // endregion operations for Remember and SideEffects

    // region operations for Nodes and Groups
    object UpdateAuxData : Operation(objects = 1) {
        inline val Data
            get() = ObjectParameter<Any?>(0)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Data -> "data"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            slots.updateAux(getObject(Data))
        }
    }

    object EnsureRootGroupStarted : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            slots.ensureStarted(0)
        }
    }

    object EnsureGroupStarted : Operation(objects = 1) {
        inline val Anchor
            get() = ObjectParameter<Anchor>(0)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Anchor -> "anchor"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            slots.ensureStarted(getObject(Anchor))
        }
    }

    object RemoveCurrentGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            slots.removeCurrentGroup(rememberManager)
        }
    }

    object MoveCurrentGroup : Operation(ints = 1) {
        inline val Offset
            get() = IntParameter(0)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                Offset -> "offset"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            slots.moveGroup(getInt(Offset))
        }
    }

    object EndCurrentGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            slots.endGroup()
        }
    }

    object SkipToEndOfCurrentGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            slots.skipToGroupEnd()
        }
    }

    object EndCompositionScope : Operation(objects = 2) {
        inline val Action
            get() = ObjectParameter<(Composition) -> Unit>(0)

        inline val Composition
            get() = ObjectParameter<Composition>(1)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Action -> "anchor"
                Composition -> "composition"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val action = getObject(Action)
            val composition = getObject(Composition)

            action.invoke(composition)
        }
    }

    object UseCurrentNode : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            applier.reuse()
        }
    }

    object UpdateNode : Operation(objects = 2) {
        inline val Value
            get() = ObjectParameter<Any?>(0)

        inline val Block
            get() = ObjectParameter<Any?.(Any?) -> Unit /* Node?.(Value) -> Unit */>(1)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Value -> "value"
                Block -> "block"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val value = getObject(Value)
            val block = getObject(Block)
            applier.apply(block, value)
        }
    }

    object RemoveNode : Operation(ints = 2) {
        inline val RemoveIndex
            get() = IntParameter(0)

        inline val Count
            get() = IntParameter(1)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                RemoveIndex -> "removeIndex"
                Count -> "count"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            applier.remove(index = getInt(RemoveIndex), count = getInt(Count))
        }
    }

    object MoveNode : Operation(ints = 3) {
        inline val From
            get() = IntParameter(0)

        inline val To
            get() = IntParameter(1)

        inline val Count
            get() = IntParameter(2)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                From -> "from"
                To -> "to"
                Count -> "count"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            applier.move(from = getInt(From), to = getInt(To), count = getInt(Count))
        }
    }

    object InsertSlots : Operation(objects = 2) {
        inline val Anchor
            get() = ObjectParameter<Anchor>(0)

        inline val FromSlotTable
            get() = ObjectParameter<SlotTable>(1)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Anchor -> "anchor"
                FromSlotTable -> "from"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val insertTable = getObject(FromSlotTable)
            val anchor = getObject(Anchor)

            slots.beginInsert()
            slots.moveFrom(
                table = insertTable,
                index = anchor.toIndexFor(insertTable),
                removeSourceGroup = false
            )
            slots.endInsert()
        }
    }

    object InsertSlotsWithFixups : Operation(objects = 3) {
        inline val Anchor
            get() = ObjectParameter<Anchor>(0)

        inline val FromSlotTable
            get() = ObjectParameter<SlotTable>(1)

        inline val Fixups
            get() = ObjectParameter<FixupList>(2)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Anchor -> "anchor"
                FromSlotTable -> "from"
                Fixups -> "fixups"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val insertTable = getObject(FromSlotTable)
            val anchor = getObject(Anchor)
            val fixups = getObject(Fixups)

            insertTable.write { writer ->
                fixups.executeAndFlushAllPendingFixups(applier, writer, rememberManager)
            }
            slots.beginInsert()
            slots.moveFrom(
                table = insertTable,
                index = anchor.toIndexFor(insertTable),
                removeSourceGroup = false
            )
            slots.endInsert()
        }
    }

    object InsertNodeFixup : Operation(ints = 1, objects = 2) {
        inline val Factory
            get() = ObjectParameter<() -> Any?>(0)

        inline val InsertIndex
            get() = IntParameter(0)

        inline val GroupAnchor
            get() = ObjectParameter<Anchor>(1)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                InsertIndex -> "insertIndex"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Factory -> "factory"
                GroupAnchor -> "groupAnchor"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val node = getObject(Factory).invoke()
            val groupAnchor = getObject(GroupAnchor)
            val insertIndex = getInt(InsertIndex)

            val nodeApplier = @Suppress("UNCHECKED_CAST") (applier as Applier<Any?>)
            slots.updateNode(groupAnchor, node)
            nodeApplier.insertTopDown(insertIndex, node)
            nodeApplier.down(node)
        }
    }

    object PostInsertNodeFixup : Operation(ints = 1, objects = 1) {
        inline val InsertIndex
            get() = IntParameter(0)

        inline val GroupAnchor
            get() = ObjectParameter<Anchor>(0)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                InsertIndex -> "insertIndex"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                GroupAnchor -> "groupAnchor"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val groupAnchor = getObject(GroupAnchor)
            val insertIndex = getInt(InsertIndex)

            applier.up()
            val nodeApplier = @Suppress("UNCHECKED_CAST") (applier as Applier<Any?>)
            val nodeToInsert = slots.node(groupAnchor)
            nodeApplier.insertBottomUp(insertIndex, nodeToInsert)
        }
    }

    object DeactivateCurrentGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            slots.deactivateCurrentGroup(rememberManager)
        }
    }

    // endregion operations for Nodes and Groups

    // region operations for MovableContent
    object ResetSlots : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            slots.reset()
        }
    }

    object DetermineMovableContentNodeIndex : Operation(objects = 2) {
        inline val EffectiveNodeIndexOut
            get() = ObjectParameter<IntRef>(0)

        inline val Anchor
            get() = ObjectParameter<Anchor>(1)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                EffectiveNodeIndexOut -> "effectiveNodeIndexOut"
                Anchor -> "anchor"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val effectiveNodeIndexOut = getObject(EffectiveNodeIndexOut)

            effectiveNodeIndexOut.element =
                positionToInsert(
                    slots = slots,
                    anchor = getObject(Anchor),
                    applier = @Suppress("UNCHECKED_CAST") (applier as Applier<Any?>)
                )
        }
    }

    object CopyNodesToNewAnchorLocation : Operation(objects = 2) {
        // IntRef because the index calculated after the operation is queued as part of
        // `DetermineMovableContentNodeIndex`
        inline val EffectiveNodeIndex
            get() = ObjectParameter<IntRef>(0)

        inline val Nodes
            get() = ObjectParameter<List<Any?>>(1)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                EffectiveNodeIndex -> "effectiveNodeIndex"
                Nodes -> "nodes"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val effectiveNodeIndex = getObject(EffectiveNodeIndex).element
            val nodesToInsert = getObject(Nodes)
            @Suppress("UNCHECKED_CAST")
            nodesToInsert.fastForEachIndexed { i, node ->
                applier as Applier<Any?>
                applier.insertBottomUp(effectiveNodeIndex + i, node)
                applier.insertTopDown(effectiveNodeIndex + i, node)
            }
        }
    }

    @OptIn(InternalComposeApi::class)
    object CopySlotTableToAnchorLocation : Operation(objects = 4) {
        inline val ResolvedState
            get() = ObjectParameter<MovableContentState?>(0)

        inline val ParentCompositionContext
            get() = ObjectParameter<CompositionContext>(1)

        inline val From
            get() = ObjectParameter<MovableContentStateReference>(2)

        inline val To
            get() = ObjectParameter<MovableContentStateReference>(3)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                ResolvedState -> "resolvedState"
                ParentCompositionContext -> "resolvedCompositionContext"
                From -> "from"
                To -> "to"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val from = getObject(From)
            val to = getObject(To)
            val parentCompositionContext = getObject(ParentCompositionContext)

            val resolvedState =
                getObject(ResolvedState)
                    ?: parentCompositionContext.movableContentStateResolve(from)
                    ?: composeRuntimeError("Could not resolve state for movable content")

            // The slot table contains the movable content group plus the group
            // containing the movable content's table which then contains the actual
            // state to be inserted. The state is at index 2 in the table (for the
            // two groups) and is inserted into the provider group at offset 1 from the
            // current location.
            val anchors = slots.moveIntoGroupFrom(1, resolvedState.slotTable, 2)

            // For all the anchors that moved, if the anchor is tracking a recompose
            // scope, update it to reference its new composer.
            RecomposeScopeImpl.adoptAnchoredScopes(
                slots = slots,
                anchors = anchors,
                newOwner = to.composition as RecomposeScopeOwner
            )
        }
    }

    object EndMovableContentPlacement : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            positionToParentOf(
                slots = slots,
                applier = @Suppress("UNCHECKED_CAST") (applier as Applier<Any?>),
                index = 0
            )
            slots.endGroup()
        }
    }

    @OptIn(InternalComposeApi::class)
    object ReleaseMovableGroupAtCurrent : Operation(objects = 3) {
        inline val Composition
            get() = ObjectParameter<ControlledComposition>(0)

        inline val ParentCompositionContext
            get() = ObjectParameter<CompositionContext>(1)

        inline val Reference
            get() = ObjectParameter<MovableContentStateReference>(2)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Composition -> "composition"
                ParentCompositionContext -> "parentCompositionContext"
                Reference -> "reference"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            releaseMovableGroupAtCurrent(
                composition = getObject(Composition),
                parentContext = getObject(ParentCompositionContext),
                reference = getObject(Reference),
                slots = slots
            )
        }
    }

    object ApplyChangeList : Operation(objects = 2) {
        inline val Changes
            get() = ObjectParameter<ChangeList>(0)

        inline val EffectiveNodeIndex
            get() = ObjectParameter<IntRef?>(1)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Changes -> "changes"
                EffectiveNodeIndex -> "effectiveNodeIndex"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ) {
            val effectiveNodeIndex = getObject(EffectiveNodeIndex)?.element ?: 0

            getObject(Changes)
                .executeAndFlushAllPendingChanges(
                    applier =
                        if (effectiveNodeIndex > 0) {
                            OffsetApplier(applier, effectiveNodeIndex)
                        } else {
                            applier
                        },
                    slots = slots,
                    rememberManager = rememberManager
                )
        }
    }

    // endregion operations for MovableContent

    /**
     * Operation type used for tests. Operations can be created with arbitrary int and object
     * params, which lets us test [Operations] without relying on the implementation details of any
     * particular operation we use in production.
     */
    class TestOperation
    @TestOnly
    constructor(
        ints: Int = 0,
        objects: Int = 0,
        val block: (Applier<*>, SlotWriter, RememberManager) -> Unit = { _, _, _ -> }
    ) : Operation(ints, objects) {
        val intParams = List(ints) { index -> IntParameter(index) }
        val objParams = List(objects) { index -> ObjectParameter<Any?>(index) }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager
        ): Unit = block(applier, slots, rememberManager)

        override fun toString() =
            "TestOperation(ints = $ints, objects = $objects)@${identityHashCode(this)}"
    }
}

private fun positionToParentOf(slots: SlotWriter, applier: Applier<Any?>, index: Int) {
    while (!slots.indexInParent(index)) {
        slots.skipToGroupEnd()
        if (slots.isNode(slots.parent)) applier.up()
        slots.endGroup()
    }
}

private fun currentNodeIndex(slots: SlotWriter): Int {
    val original = slots.currentGroup

    // Find parent node
    var current = slots.parent
    while (current >= 0 && !slots.isNode(current)) {
        current = slots.parent(current)
    }

    var index = 0
    current++
    while (current < original) {
        if (slots.indexInGroup(original, current)) {
            if (slots.isNode(current)) index = 0
            current++
        } else {
            index += if (slots.isNode(current)) 1 else slots.nodeCount(current)
            current += slots.groupSize(current)
        }
    }
    return index
}

private fun positionToInsert(slots: SlotWriter, anchor: Anchor, applier: Applier<Any?>): Int {
    val destination = slots.anchorIndex(anchor)
    runtimeCheck(slots.currentGroup < destination)
    positionToParentOf(slots, applier, destination)
    var nodeIndex = currentNodeIndex(slots)
    while (slots.currentGroup < destination) {
        when {
            slots.indexInCurrentGroup(destination) -> {
                if (slots.isNode) {
                    applier.down(slots.node(slots.currentGroup))
                    nodeIndex = 0
                }
                slots.startGroup()
            }
            else -> nodeIndex += slots.skipGroup()
        }
    }

    runtimeCheck(slots.currentGroup == destination)
    return nodeIndex
}

/**
 * Release the movable group stored in [slots] to the recomposer to be used to insert in another
 * location if needed.
 */
@OptIn(InternalComposeApi::class)
private fun releaseMovableGroupAtCurrent(
    composition: ControlledComposition,
    parentContext: CompositionContext,
    reference: MovableContentStateReference,
    slots: SlotWriter
) {
    val slotTable = SlotTable()
    if (slots.collectingSourceInformation) {
        slotTable.collectSourceInformation()
    }
    if (slots.collectingCalledInformation) {
        slotTable.collectCalledByInformation()
    }

    // Write a table that as if it was written by a calling
    // invokeMovableContentLambda because this might be removed from the
    // composition before the new composition can be composed to receive it. When
    // the new composition receives the state it must recompose over the state by
    // calling invokeMovableContentLambda.
    val anchors =
        slotTable.write { writer ->
            writer.beginInsert()

            // This is the prefix created by invokeMovableContentLambda
            writer.startGroup(movableContentKey, reference.content)
            writer.markGroup()
            writer.update(reference.parameter)

            // Move the content into current location
            val anchors = slots.moveTo(reference.anchor, 1, writer)

            // skip the group that was just inserted.
            writer.skipGroup()

            // End the group that represents the call to invokeMovableContentLambda
            writer.endGroup()

            writer.endInsert()

            anchors
        }

    val state = MovableContentState(slotTable)
    if (RecomposeScopeImpl.hasAnchoredRecomposeScopes(slotTable, anchors)) {
        // If any recompose scopes are invalidated while the movable content is outside
        // a composition, ensure the reference is updated to contain the invalidation.
        val movableContentRecomposeScopeOwner =
            object : RecomposeScopeOwner {
                override fun invalidate(
                    scope: RecomposeScopeImpl,
                    instance: Any?
                ): InvalidationResult {
                    // Try sending this to the original owner first.
                    val result =
                        (composition as? RecomposeScopeOwner)?.invalidate(scope, instance)
                            ?: InvalidationResult.IGNORED

                    // If the original owner ignores this then we need to record it in the
                    // reference
                    if (result == InvalidationResult.IGNORED) {
                        reference.invalidations += scope to instance
                        return InvalidationResult.SCHEDULED
                    }
                    return result
                }

                // The only reason [recomposeScopeReleased] is called is when the recompose scope is
                // removed from the table. First, this never happens for content that is moving, and
                // 2) even if it did the only reason we tell the composer is to clear tracking
                // tables that contain this information which is not relevant here.
                override fun recomposeScopeReleased(scope: RecomposeScopeImpl) {
                    // Nothing to do
                }

                // [recordReadOf] this is also something that would happen only during active
                // recomposition which doesn't happened to a slot table that is moving.
                override fun recordReadOf(value: Any) {
                    // Nothing to do
                }
            }
        slotTable.write { writer ->
            RecomposeScopeImpl.adoptAnchoredScopes(
                slots = writer,
                anchors = anchors,
                newOwner = movableContentRecomposeScopeOwner
            )
        }
    }
    parentContext.movableContentStateReleased(reference, state)
}
