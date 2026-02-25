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

import androidx.collection.mutableIntSetOf
import androidx.compose.runtime.Applier
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.IntStack
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.InvalidationResult
import androidx.compose.runtime.LinkRememberObserverHolder
import androidx.compose.runtime.MovableContentState
import androidx.compose.runtime.MovableContentStateReference
import androidx.compose.runtime.OffsetApplier
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RecomposeScopeOwner
import androidx.compose.runtime.RememberObserverHolder
import androidx.compose.runtime.ScopeInvalidated
import androidx.compose.runtime.TestOnly
import androidx.compose.runtime.composeRuntimeError
import androidx.compose.runtime.composer.RememberManager
import androidx.compose.runtime.composer.gapbuffer.changelist.OperationErrorContext
import androidx.compose.runtime.composer.linkbuffer.AnchorHandle
import androidx.compose.runtime.composer.linkbuffer.GroupAddress
import androidx.compose.runtime.composer.linkbuffer.GroupHandle
import androidx.compose.runtime.composer.linkbuffer.IsMovableContentFlag
import androidx.compose.runtime.composer.linkbuffer.IsRecompositionRequiredFlag
import androidx.compose.runtime.composer.linkbuffer.LAZY_ADDRESS
import androidx.compose.runtime.composer.linkbuffer.LinkAnchor
import androidx.compose.runtime.composer.linkbuffer.NULL_ADDRESS
import androidx.compose.runtime.composer.linkbuffer.NULL_GROUP_HANDLE
import androidx.compose.runtime.composer.linkbuffer.SlotTable
import androidx.compose.runtime.composer.linkbuffer.SlotTableEditor
import androidx.compose.runtime.composer.linkbuffer.adoptScopesInGroupToNewParent
import androidx.compose.runtime.composer.linkbuffer.asLinkAnchor
import androidx.compose.runtime.composer.linkbuffer.asLinkBufferSlotTable
import androidx.compose.runtime.composer.linkbuffer.buildTrace
import androidx.compose.runtime.composer.linkbuffer.deactivateGroup
import androidx.compose.runtime.composer.linkbuffer.group
import androidx.compose.runtime.composer.linkbuffer.groupChild
import androidx.compose.runtime.composer.linkbuffer.makeGroupHandle
import androidx.compose.runtime.composer.linkbuffer.removeGroupAndForgetSlots
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.internal.identityHashCode
import androidx.compose.runtime.movableContentKey
import androidx.compose.runtime.runtimeCheck
import androidx.compose.runtime.snapshots.fastForEachIndexed
import androidx.compose.runtime.tooling.ComposeStackTrace
import androidx.compose.runtime.tooling.ComposeStackTraceFrame
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.attachComposeStackTrace
import kotlin.jvm.JvmInline

internal typealias IntParameter = Int

internal sealed class Operation(
    val ints: Int = 0,
    val objects: Int = 0,
    val isExternallyVisible: Boolean = true,
) {
    val name: String
        get() = this::class.simpleName.orEmpty()

    fun OperationArgContainer.executeWithComposeStackTrace(
        applier: Applier<*>,
        slots: SlotTableEditor,
        rememberManager: RememberManager,
        errorContext: OperationErrorContext?,
    ) {
        withCurrentStackTrace(errorContext, slots, getGroupHandle(slots)) {
            execute(applier, slots, rememberManager, errorContext)
        }
    }

    protected open fun OperationArgContainer.getGroupHandle(slots: SlotTableEditor): GroupHandle =
        NULL_GROUP_HANDLE

    protected abstract fun OperationArgContainer.execute(
        applier: Applier<*>,
        slots: SlotTableEditor,
        rememberManager: RememberManager,
        errorContext: OperationErrorContext?,
    )

    open fun intParamName(parameter: IntParameter): String = "IntParameter(${parameter})"

    open fun objectParamName(parameter: ObjectParameter<*>): String =
        "ObjectParameter(${parameter.offset})"

    override fun toString() = name

    @JvmInline value class ObjectParameter<T>(val offset: Int)

    // region traversal operations
    object Ups : Operation(ints = 1) {
        inline val Count
            get() = 0

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                Count -> "count"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            @Suppress("UNCHECKED_CAST") val nodeApplier = applier as Applier<Any?>
            val nodes = getObject(Nodes)
            for (index in nodes.indices) {
                nodeApplier.down(nodes[index])
            }
        }
    }

    object SeekToAnchor : Operation(objects = 1, isExternallyVisible = false) {
        inline val AnchorHandle
            get() = ObjectParameter<AnchorHandle>(0)

        override fun objectParamName(parameter: ObjectParameter<*>): String =
            when (parameter) {
                AnchorHandle -> "anchorHandle"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val anchorHandle = getObject(AnchorHandle)
            slots.seek(anchorHandle.toHandle())
        }
    }

    object SeekToGroupHandle : Operation(ints = 2, isExternallyVisible = false) {
        inline val GroupHandleHighBits
            get() = 0

        inline val GroupHandleLowBits
            get() = 1

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                GroupHandleHighBits -> "group[32..63]"
                GroupHandleLowBits -> "group[0..31]"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val handle = getLong(GroupHandleHighBits, GroupHandleLowBits)
            slots.seek(handle)
        }
    }

    object StartGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.startGroup()
        }
    }

    object SkipGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.skipGroup()
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            rememberManager.sideEffect(getObject(Effect))
        }
    }

    object Remember : Operation(objects = 1) {
        inline val Value
            get() = ObjectParameter<RememberObserverHolder>(0)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Value -> "value"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val scope = getObject(Scope)
            rememberManager.endResumingScope(scope)
        }
    }

    object AppendValue : Operation(objects = 1) {
        inline val Value
            get() = ObjectParameter<Any?>(0)

        override fun objectParamName(parameter: ObjectParameter<*>): String =
            when (parameter) {
                Value -> "value"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val value = getObject(Value)
            if (value is RememberObserverHolder) {
                rememberManager.remembering(value)
            }
            slots.appendSlot(value)
        }
    }

    object RemoveTailGroupsAndValues : Operation(ints = 2) {
        inline val FirstTailGroupToRemove
            get() = 0

        inline val TailSlotCount
            get() = 1

        override fun intParamName(parameter: IntParameter): String =
            when (parameter) {
                FirstTailGroupToRemove -> "firstTailGroupToRemove"
                TailSlotCount -> "tailSlotCount"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val tailSlotCount = getInt(TailSlotCount)
            val firstTailGroupToRemove = getInt(FirstTailGroupToRemove)

            slots.visitTailSlotsInRememberOrder(
                inGroup = slots.parentGroup,
                firstTailGroupToVisit = firstTailGroupToRemove,
                tailSlots = tailSlotCount,
            ) { _, _, value ->
                when (value) {
                    is ComposeNodeLifecycleCallback -> value.onRelease()
                    is RememberObserverHolder -> rememberManager.forgetting(value)
                    is RecomposeScopeImpl -> value.release()
                }
                false
            }

            slots.trimSlots(tailSlotCount)

            while (slots.currentGroup != firstTailGroupToRemove) {
                slots.skipGroup()
            }

            while (slots.currentGroup >= 0) {
                slots.removeGroup(true)
            }
        }
    }

    object UpdateValue : Operation(ints = 1, objects = 1) {
        inline val SlotAddress
            get() = 0

        inline val Value
            get() = ObjectParameter<Any?>(0)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                SlotAddress -> "slotAddress"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Value -> "value"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val value = getObject(Value)
            val slot = getInt(SlotAddress)
            if (value is RememberObserverHolder) {
                rememberManager.remembering(value)
            }
            when (val previous = slots.setAbsolute(slot, value)) {
                is RememberObserverHolder -> rememberManager.forgetting(previous)
                is RecomposeScopeImpl -> previous.release()
            }
        }
    }

    object UpdateRememberObserverHolderOrdering : Operation(ints = 0, objects = 2) {
        inline val After
            get() = ObjectParameter<LinkAnchor>(0)

        inline val Holder
            get() = ObjectParameter<LinkRememberObserverHolder>(1)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                After -> "after"
                Holder -> "rememberObserverHolder"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val holder = getObject(Holder)
            val after = getObject(After)

            holder.after = after
        }
    }

    object UpdateValueRelative : Operation(ints = 1, objects = 1) {
        inline val SlotIndex
            get() = 0

        inline val Value
            get() = ObjectParameter<Any?>(0)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                SlotIndex -> "slotIndex"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Value -> "value"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val value = getObject(Value)
            val slotIndex = getInt(SlotIndex)
            if (value is RememberObserverHolder) {
                rememberManager.remembering(value)
            }

            when (val previous = slots.setRelative(slotIndex, value)) {
                is RememberObserverHolder -> rememberManager.forgetting(previous)
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.updateAux(getObject(Data))
        }
    }

    object RemoveGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.removeGroupAndForgetSlots(rememberManager)
        }
    }

    object MoveGroup : Operation(ints = 1) {
        inline val Offset
            get() = 0

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                Offset -> "offset"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.moveGroup(getInt(Offset))
        }
    }

    object ClearAllRecompositionRequired : Operation(isExternallyVisible = false) {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.removeAllInstancesOfFlags(IsRecompositionRequiredFlag)
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val action = getObject(Action)
            val composition = getObject(Composition)

            action.invoke(composition)
        }
    }

    object UseCurrentNode : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val value = getObject(Value)
            val block = getObject(Block)
            applier.apply(block, value)
        }
    }

    object RemoveNode : Operation(ints = 2) {
        inline val RemoveIndex
            get() = 0

        inline val Count
            get() = 1

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                RemoveIndex -> "removeIndex"
                Count -> "count"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            applier.remove(index = getInt(RemoveIndex), count = getInt(Count))
        }
    }

    object MoveNode : Operation(ints = 3) {
        inline val From
            get() = 0

        inline val To
            get() = 1

        inline val Count
            get() = 2

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                From -> "from"
                To -> "to"
                Count -> "count"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            applier.move(from = getInt(From), to = getInt(To), count = getInt(Count))
        }
    }

    object InsertSlots : Operation(ints = 2, objects = 1) {
        inline val SourceHighBits
            get() = 0

        inline val SourceLowBits
            get() = 1

        inline val FromSlotTable
            get() = ObjectParameter<SlotTable>(0)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                SourceHighBits -> "source[32..63]"
                SourceLowBits -> "source[0..31]"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                FromSlotTable -> "from"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.moveFrom(
                sourceTable = getObject(FromSlotTable),
                sourceHandle = getLong(SourceHighBits, SourceLowBits),
            )
            slots.skipGroup()
        }
    }

    object InsertSlotsWithFixups : Operation(ints = 2, objects = 2) {
        inline val SourceHighBits
            get() = 0

        inline val SourceLowBits
            get() = 1

        inline val FromSlotTable
            get() = ObjectParameter<SlotTable>(0)

        inline val Fixups
            get() = ObjectParameter<FixupList>(1)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                SourceHighBits -> "sourceHandle[32..63]"
                SourceLowBits -> "sourceHandle[0..31]"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                FromSlotTable -> "from"
                Fixups -> "fixups"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val insertTable = getObject(FromSlotTable)
            val fixups = getObject(Fixups)

            insertTable.edit {
                fixups.executeAndFlushAllPendingFixups(
                    applier,
                    this,
                    rememberManager,
                    errorContext?.withCurrentStackTrace(slots),
                )
            }
            slots.moveFrom(
                sourceTable = insertTable,
                sourceHandle = getLong(SourceHighBits, SourceLowBits),
            )
            slots.skipGroup()
        }
    }

    object InsertNodeFixup : Operation(ints = 3, objects = 1) {
        inline val Factory
            get() = ObjectParameter<() -> Any?>(0)

        inline val InsertIndex
            get() = 0

        inline val GroupHandleHigh
            get() = 1

        inline val GroupHandleLow
            get() = 2

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                InsertIndex -> "insertIndex"
                GroupHandleHigh -> "groupHandleHigh"
                GroupHandleLow -> "groupHandleLow"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Factory -> "factory"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.getGroupHandle(slots: SlotTableEditor): GroupHandle =
            getLong(GroupHandleHigh, GroupHandleLow)

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val node = getObject(Factory).invoke()
            val insertIndex = getInt(InsertIndex)
            val groupAddress = getGroupHandle(slots).group

            val nodeApplier = @Suppress("UNCHECKED_CAST") (applier as Applier<Any?>)
            slots.updateNode(groupAddress, node)
            nodeApplier.insertTopDown(insertIndex, node)
            nodeApplier.down(node)
        }
    }

    object InsertNodeFixupByAnchor : Operation(ints = 1, objects = 2) {
        inline val Factory
            get() = ObjectParameter<() -> Any?>(0)

        inline val InsertIndex
            get() = 0

        inline val Anchor
            get() = ObjectParameter<LinkAnchor>(1)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                InsertIndex -> "insertIndex"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                Factory -> "factory"
                Anchor -> "anchor"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val node = getObject(Factory).invoke()
            val insertIndex = getInt(InsertIndex)
            val groupAddress = getObject(Anchor).address

            val nodeApplier = @Suppress("UNCHECKED_CAST") (applier as Applier<Any?>)
            slots.updateNode(groupAddress, node)
            nodeApplier.insertTopDown(insertIndex, node)
            nodeApplier.down(node)
        }
    }

    object PostInsertNodeFixup : Operation(ints = 3) {
        inline val InsertIndex
            get() = 0

        inline val GroupHandleHigh
            get() = 1

        inline val GroupHandleLow
            get() = 2

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                InsertIndex -> "insertIndex"
                GroupHandleHigh -> "groupHandleHigh"
                GroupHandleLow -> "groupHandleLow"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.getGroupHandle(slots: SlotTableEditor): GroupHandle =
            getLong(GroupHandleHigh, GroupHandleLow)

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val insertIndex = getInt(InsertIndex)
            val groupAddress = getLong(GroupHandleHigh, GroupHandleLow).group

            applier.up()
            val nodeApplier = @Suppress("UNCHECKED_CAST") (applier as Applier<Any?>)
            nodeApplier.insertBottomUp(insertIndex, slots.node(groupAddress))
        }
    }

    object PostInsertNodeFixupByAnchor : Operation(ints = 1, objects = 1) {
        inline val InsertIndex
            get() = 0

        inline val Anchor
            get() = ObjectParameter<LinkAnchor>(0)

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                InsertIndex -> "insertIndex"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>): String =
            when (parameter) {
                Anchor -> "anchor"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val insertIndex = getInt(InsertIndex)
            val groupAddress = getObject(Anchor).address

            applier.up()
            val nodeApplier = @Suppress("UNCHECKED_CAST") (applier as Applier<Any?>)
            nodeApplier.insertBottomUp(insertIndex, slots.node(groupAddress))
        }
    }

    object DeactivateGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.deactivateGroup(rememberManager)
        }
    }

    // endregion operations for Nodes and Groups

    // region operations for MovableContent
    object ResetSlots : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.reset()
        }
    }

    /**
     * Determine the insert node index and navigate the applier to the location of the parent node
     * children will be inserted into by performing all the up and downs necessary.
     *
     * NOTE: As a side-effect of this code, the slots parameter is navigated to the groupHandle
     * location. This is relied on by the relative update commands that usually following this task
     * in the change list.
     */
    object DetermineMovableContentNodeIndex : Operation(ints = 2, objects = 1) {
        inline val EffectiveNodeIndexOut
            get() = ObjectParameter<IntRef>(0)

        inline val GroupHandleLowBits
            get() = 0

        inline val GroupHandleHighBits
            get() = 1

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                GroupHandleLowBits -> "groupHandle[32..63]"
                GroupHandleHighBits -> "groupHandle[0..31]"
                else -> super.intParamName(parameter)
            }

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                EffectiveNodeIndexOut -> "effectiveNodeIndexOut"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val effectiveNodeIndexOut = getObject(EffectiveNodeIndexOut)

            effectiveNodeIndexOut.element =
                positionToInsert(
                    slots = slots,
                    destination = getLong(GroupHandleHighBits, GroupHandleLowBits),
                    applier = @Suppress("UNCHECKED_CAST") (applier as Applier<Any?>),
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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

    object EndMovableContentPlacement : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            while (slots.parentGroup >= 0) {
                if (slots.isParentGroupANode) applier.up()
                slots.endGroup()
            }
        }
    }

    @OptIn(InternalComposeApi::class)
    object CopySlotTableToHandleLocation : Operation(objects = 4) {
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val from = getObject(From)
            val to = getObject(To)
            val parentCompositionContext = getObject(ParentCompositionContext)

            val resolvedState =
                getObject(ResolvedState)
                    ?: parentCompositionContext.movableContentStateResolve(from)
                    ?: composeRuntimeError("Could not resolve state for movable content")

            val resolvedTable = resolvedState.slotStorage.asLinkBufferSlotTable()
            val newGroup =
                resolvedTable.edit {
                    // At this point, slots' currentGroup and the root of the resolvedTable both
                    // point to the wrapper group containing the MovableContent instance and the
                    // IsMovableContent flag. We want to move the content itself, which is the
                    // grandchild of this group (the child group is from invokeMovableContentLambda,
                    // which we also don't want to copy)
                    startGroup()
                    startGroup()
                    slots.moveFrom(
                        sourceEditor = this@edit,
                        sourceHandle = handle(),
                        destination =
                            makeGroupHandle(
                                groupContext = slots.firstChildOf(slots.currentGroup),
                                group = NULL_ADDRESS,
                            ),
                    )
                }

            // For all the anchors that moved, if the anchor is tracking a recompose
            // scope, update it to reference its new composer.
            slots.table.adoptScopesInGroupToNewParent(
                group = newGroup.group,
                newOwner = to.composition as RecomposeScopeOwner,
            )
        }
    }

    @OptIn(InternalComposeApi::class)
    object ReleaseMovableGroup : Operation(objects = 3) {
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            releaseMovableGroup(
                composition = getObject(Composition),
                parentContext = getObject(ParentCompositionContext),
                reference = getObject(Reference),
                slots = slots,
                applier = applier,
            )
        }
    }

    @OptIn(InternalComposeApi::class)
    object DisposeMovableContentState : Operation(objects = 1) {
        inline val ResolvedState
            get() = ObjectParameter<MovableContentState>(0)

        override fun objectParamName(parameter: ObjectParameter<*>) =
            when (parameter) {
                ResolvedState -> "resolvedState"
                else -> super.objectParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val resolvedState = getObject(ResolvedState)
            resolvedState.dispose()
        }
    }

    object ApplyChangeList : Operation(objects = 2, isExternallyVisible = false) {
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
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
                    rememberManager = rememberManager,
                    errorContext = errorContext?.withCurrentStackTrace(slots),
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
        val block: (Applier<*>, SlotTableEditor, RememberManager) -> Unit = { _, _, _ -> },
    ) : Operation(ints, objects) {
        @Suppress("PrimitiveInCollection") val intParams = List(ints) { it }
        @Suppress("PrimitiveInCollection")
        val objParams = List(objects) { index -> ObjectParameter<Any?>(index) }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotTableEditor,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ): Unit = block(applier, slots, rememberManager)

        override fun toString() =
            "TestOperation(ints = $ints, objects = $objects)@${identityHashCode(this)}"
    }
}

/**
 * Repositions both the SlotTableEditor and Applier so that they point to any parent of the given
 * [handle].
 */
private fun positionToParentOf(
    slots: SlotTableEditor,
    applier: Applier<Any?>,
    handle: GroupHandle,
) {
    if (slots.parentGroup >= 0) {
        val parents =
            mutableIntSetOf().apply {
                val slotTable = slots.table
                slotTable.traverseGroupAndParents(slots.parentOf(handle.group)) { parent ->
                    add(parent)
                }
            }

        while (slots.parentGroup >= 0 && slots.parentGroup !in parents) {
            if (slots.isParentGroupANode) applier.up()
            slots.endGroup()
        }
    }
}

/**
 * Repositions [slots] to the provided [destination] handle. When this function returns, the
 * SlotTableEditor's currentGroup will point to the group encoded in the destination handle, and the
 * applier will point to the corresponding node location of this position in the SlotTable.
 *
 * This operation is used to realize the positioning of movable content.
 *
 * @return The destination group's node index within the closest parent node
 */
private fun positionToInsert(
    slots: SlotTableEditor,
    destination: GroupHandle,
    applier: Applier<Any?>,
): Int {
    positionToParentOf(slots, applier, destination)

    // Enumerate the parents of the destination into a queue. These are the groups we will
    // need to start.
    val startParentGroup = slots.parentGroup
    val target = destination.group
    val parents =
        IntStack().apply {
            slots.table.traverseGroupAndParents(target) { parent ->
                if (parent == startParentGroup) return@apply
                push(parent)
            }
        }

    runtimeCheck(slots.parentGroup == startParentGroup) {
        "Unexpected slot table structure when inserting movable content"
    }

    val startingGroup = slots.currentGroup
    var nodeIndex = 0
    var foundParentNode = false

    // Traverse to the destination. Skip groups until we get to the next parent we're
    // expecting, then start it. After starting all parents, we still need to skip until
    // we reach the target group.
    while (slots.currentGroup != target) {
        when {
            parents.isNotEmpty() && slots.currentGroup == parents.peek() -> {
                if (slots.isNode) {
                    applier.down(slots.node)
                    nodeIndex = 0
                    foundParentNode = true
                }
                slots.startGroup()
                parents.pop()
            }
            else -> nodeIndex += slots.skipGroup()
        }
    }

    // If one of the parents was a node group, we're done. Otherwise, we need to go back to
    // the parents of the starting point to resolve the index, in case there were other node
    // groups before this section of the SlotTable we just explored.
    return nodeIndex + if (!foundParentNode) nodeIndex(slots, startingGroup) else 0
}

/**
 * @return The node index of the given [group] as it appears in its closest parent's node group. In
 *   other words, this returns the number of nodes before [group] in a preorder traversal of the
 *   SlotTable, starting from the first parent group that is a node group and not recursively
 *   counting child nodes.
 *
 * (As an implementation detail, the algorithm itself is implemented entirely bottom-up as this is
 * the more efficient route to take.)
 */
private fun nodeIndex(slots: SlotTableEditor, group: GroupAddress): Int {
    if (group < 0) return 0

    val slotTable = slots.table
    var index = 0
    var lastExploredGroup = group

    // Iterate through all parents until a parent node group is reached
    slotTable.traverseGroupAndParents(lastExploredGroup) { parent ->
        if (slots.isNode(parent)) return index

        val grandParent = slots.parentOf(parent)
        val firstSibling =
            if (grandParent < 0) {
                slotTable.root
            } else {
                slots.firstChildOf(grandParent)
            }

        // Count all node counts in siblings before this group
        slotTable.traverseSiblings(firstSibling) { predecessor ->
            if (predecessor == lastExploredGroup) {
                lastExploredGroup = grandParent
                return@traverseGroupAndParents
            }
            index += slots.nodeCountOf(predecessor)
        }
        lastExploredGroup = grandParent
    }

    return index
}

/**
 * Release the movable group stored in [slots] to the recomposer to be used to insert in another
 * location if needed.
 */
@OptIn(InternalComposeApi::class)
private fun releaseMovableGroup(
    composition: ControlledComposition,
    parentContext: CompositionContext,
    reference: MovableContentStateReference,
    slots: SlotTableEditor,
    applier: Applier<*>,
) {
    // Write a table that as if it was written by a calling
    // invokeMovableContentLambda because this might be removed from the
    // composition before the new composition can be composed to receive it. When
    // the new composition receives the state it must recompose over the state by
    // calling invokeMovableContentLambda.
    val slotTable =
        slots.table.buildSubTable {
            // Add the prefix created by invokeMovableContentLambda
            startGroup(movableContentKey, reference.content)
            addFlags(IsMovableContentFlag)
            append(reference.parameter)

            // Get the first child inside of the MovableContentStateReference to move the content
            // itself and not the reference's container
            val wrapperGroup = reference.anchor.asLinkAnchor().address
            val movableContentReference = slots.table.addressSpace.groups.groupChild(wrapperGroup)

            moveFrom(
                sourceEditor = slots,
                sourceHandle = makeGroupHandle(LAZY_ADDRESS, movableContentReference),
            )
            endGroup()
        }

    val state = MovableContentState(slotTable)
    if (slotTable.hasRecomposeScopes(slotTable.root)) {
        // If any recompose scopes are invalidated while the movable content is outside
        // a composition, ensure the reference is updated to contain the invalidation.
        val movableContentRecomposeScopeOwner =
            object : RecomposeScopeOwner {
                override fun invalidate(
                    scope: RecomposeScopeImpl,
                    instance: Any?,
                ): InvalidationResult {
                    // Try sending this to the original owner first.
                    val result =
                        (composition as? RecomposeScopeOwner)?.invalidate(scope, instance)
                            ?: InvalidationResult.IGNORED

                    // If the original owner ignores this then we need to record it in the
                    // reference
                    if (result == InvalidationResult.IGNORED) {
                        reference.invalidations += scope to (instance ?: ScopeInvalidated)
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
        slotTable.adoptScopesInGroupToNewParent(
            group = slotTable.root,
            newOwner = movableContentRecomposeScopeOwner,
        )
    }
    parentContext.movableContentStateReleased(reference, state, applier)
}

private inline fun withCurrentStackTrace(
    errorContext: OperationErrorContext?,
    editor: SlotTableEditor,
    location: GroupHandle,
    block: () -> Unit,
) {
    try {
        block()
    } catch (e: Throwable) {
        throw e.attachComposeStackTrace(errorContext, editor, location)
    }
}

@OptIn(ComposeToolingApi::class)
@Suppress("ListIterator")
private fun Throwable.attachComposeStackTrace(
    errorContext: OperationErrorContext?,
    editor: SlotTableEditor,
    handle: GroupHandle,
): Throwable {
    if (errorContext == null) return this
    return attachComposeStackTrace {
        if (handle != NULL_GROUP_HANDLE) {
            editor.seek(handle)
        }
        val trace = editor.buildTrace()
        val offset = trace.lastOrNull()?.groupOffset
        val parentTrace =
            errorContext.buildStackTrace(offset).let {
                if (offset == null || it.isEmpty()) {
                    it
                } else {
                    val head = it.first()
                    val tail = it.drop(1)
                    listOf(head.copy(groupOffset = offset)) + tail
                }
            }
        ComposeStackTrace(trace + parentTrace, errorContext.sourceInformationEnabled)
    }
}

private fun OperationErrorContext.withCurrentStackTrace(
    slots: SlotTableEditor
): OperationErrorContext {
    val parent = this
    return object : OperationErrorContext {
        override fun buildStackTrace(currentOffset: Int?): List<ComposeStackTraceFrame> {
            val parentTrace = parent.buildStackTrace(null)
            // Slots are positioned at the start of the next group when insertion happens
            val currentGroup = slots.parentGroup
            if (currentGroup < 0) return parentTrace
            return slots.buildTrace(currentOffset, currentGroup) + parentTrace
        }

        override val sourceInformationEnabled: Boolean
            get() = parent.sourceInformationEnabled
    }
}
