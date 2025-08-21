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
import androidx.compose.runtime.MovableContentState
import androidx.compose.runtime.MovableContentStateReference
import androidx.compose.runtime.OffsetApplier
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RecomposeScopeOwner
import androidx.compose.runtime.RememberManager
import androidx.compose.runtime.RememberObserverHolder
import androidx.compose.runtime.SlotTable
import androidx.compose.runtime.SlotWriter
import androidx.compose.runtime.TestOnly
import androidx.compose.runtime.composeRuntimeError
import androidx.compose.runtime.deactivateCurrentGroup
import androidx.compose.runtime.extractMovableContentAtCurrent
import androidx.compose.runtime.internal.IntRef
import androidx.compose.runtime.internal.identityHashCode
import androidx.compose.runtime.removeCurrentGroup
import androidx.compose.runtime.runtimeCheck
import androidx.compose.runtime.snapshots.fastForEachIndexed
import androidx.compose.runtime.tooling.ComposeStackTraceFrame
import androidx.compose.runtime.tooling.ComposeToolingApi
import androidx.compose.runtime.tooling.attachComposeStackTrace
import androidx.compose.runtime.tooling.buildTrace
import kotlin.jvm.JvmInline

internal typealias IntParameter = Int

internal sealed class Operation(val ints: Int = 0, val objects: Int = 0) {
    val name: String
        get() = this::class.simpleName.orEmpty()

    fun OperationArgContainer.executeWithComposeStackTrace(
        applier: Applier<*>,
        slots: SlotWriter,
        rememberManager: RememberManager,
        errorContext: OperationErrorContext?,
    ) {
        withCurrentStackTrace(errorContext, slots, getGroupAnchor(slots)) {
            execute(applier, slots, rememberManager, errorContext)
        }
    }

    protected open fun OperationArgContainer.getGroupAnchor(slots: SlotWriter): Anchor? = null

    protected abstract fun OperationArgContainer.execute(
        applier: Applier<*>,
        slots: SlotWriter,
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
            slots: SlotWriter,
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
            slots: SlotWriter,
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

    object AdvanceSlotsBy : Operation(ints = 1) {
        inline val Distance
            get() = 0

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                Distance -> "distance"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            slots: SlotWriter,
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
            slots: SlotWriter,
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
            slots: SlotWriter,
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
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val anchor = getObject(Anchor)
            val value = getObject(Value)
            if (value is RememberObserverHolder) {
                rememberManager.remembering(value)
            }
            slots.appendSlot(anchor, value)
        }
    }

    object TrimParentValues : Operation(ints = 1) {
        inline val Count
            get() = 0

        override fun intParamName(parameter: IntParameter): String =
            when (parameter) {
                Count -> "count"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val count = getInt(Count)
            slots.forEachTailSlot(slots.parent, count) { slotIndex, value ->
                when (value) {
                    is RememberObserverHolder -> {
                        rememberManager.forgetting(instance = value)
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
            get() = 0

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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val value = getObject(Value)
            val groupSlotIndex = getInt(GroupSlotIndex)
            if (value is RememberObserverHolder) {
                rememberManager.remembering(value)
            }
            when (val previous = slots.set(groupSlotIndex, value)) {
                is RememberObserverHolder -> {
                    rememberManager.forgetting(previous)
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
            get() = 0

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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val value = getObject(Value)
            val anchor = getObject(Anchor)
            val groupSlotIndex = getInt(GroupSlotIndex)
            if (value is RememberObserverHolder) {
                rememberManager.remembering(value)
            }
            val groupIndex = slots.anchorIndex(anchor)
            when (val previous = slots.set(groupIndex, groupSlotIndex, value)) {
                is RememberObserverHolder -> {
                    rememberManager.forgetting(previous)
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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.updateAux(getObject(Data))
        }
    }

    object EnsureRootGroupStarted : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.ensureStarted(getObject(Anchor))
        }
    }

    object RemoveCurrentGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.removeCurrentGroup(rememberManager)
        }
    }

    object MoveCurrentGroup : Operation(ints = 1) {
        inline val Offset
            get() = 0

        override fun intParamName(parameter: IntParameter) =
            when (parameter) {
                Offset -> "offset"
                else -> super.intParamName(parameter)
            }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.moveGroup(getInt(Offset))
        }
    }

    object EndCurrentGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            slots.endGroup()
        }
    }

    object SkipToEndOfCurrentGroup : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            slots: SlotWriter,
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
            slots: SlotWriter,
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
            slots: SlotWriter,
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
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val insertTable = getObject(FromSlotTable)
            val anchor = getObject(Anchor)

            slots.beginInsert()
            slots.moveFrom(
                table = insertTable,
                index = anchor.toIndexFor(insertTable),
                removeSourceGroup = false,
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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val insertTable = getObject(FromSlotTable)
            val anchor = getObject(Anchor)
            val fixups = getObject(Fixups)

            insertTable.write { writer ->
                fixups.executeAndFlushAllPendingFixups(
                    applier,
                    writer,
                    rememberManager,
                    errorContext?.withCurrentStackTrace(slots),
                )
            }
            slots.beginInsert()
            slots.moveFrom(
                table = insertTable,
                index = anchor.toIndexFor(insertTable),
                removeSourceGroup = false,
            )
            slots.endInsert()
        }
    }

    object InsertNodeFixup : Operation(ints = 1, objects = 2) {
        inline val Factory
            get() = ObjectParameter<() -> Any?>(0)

        inline val InsertIndex
            get() = 0

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

        override fun OperationArgContainer.getGroupAnchor(slots: SlotWriter): Anchor? =
            getObject(GroupAnchor)

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            get() = 0

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

        override fun OperationArgContainer.getGroupAnchor(slots: SlotWriter): Anchor? =
            getObject(GroupAnchor)

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val effectiveNodeIndexOut = getObject(EffectiveNodeIndexOut)

            effectiveNodeIndexOut.element =
                positionToInsert(
                    slots = slots,
                    anchor = getObject(Anchor),
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
            slots: SlotWriter,
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
                newOwner = to.composition as RecomposeScopeOwner,
            )
        }
    }

    object EndMovableContentPlacement : Operation() {
        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            positionToParentOf(
                slots = slots,
                applier = @Suppress("UNCHECKED_CAST") (applier as Applier<Any?>),
                index = 0,
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
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
        ) {
            val composition = getObject(Composition)
            val reference = getObject(Reference)
            val parentContext = getObject(ParentCompositionContext)
            val state =
                extractMovableContentAtCurrent(
                    composition = composition,
                    reference = reference,
                    slots = slots,
                    applier = null,
                )
            parentContext.movableContentStateReleased(reference, state, applier)
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
        val block: (Applier<*>, SlotWriter, RememberManager) -> Unit = { _, _, _ -> },
    ) : Operation(ints, objects) {
        @Suppress("PrimitiveInCollection") val intParams = List(ints) { it }
        @Suppress("PrimitiveInCollection")
        val objParams = List(objects) { index -> ObjectParameter<Any?>(index) }

        override fun OperationArgContainer.execute(
            applier: Applier<*>,
            slots: SlotWriter,
            rememberManager: RememberManager,
            errorContext: OperationErrorContext?,
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

private inline fun withCurrentStackTrace(
    errorContext: OperationErrorContext?,
    writer: SlotWriter,
    location: Anchor?,
    block: () -> Unit,
) {
    try {
        block()
    } catch (e: Throwable) {
        throw e.attachComposeStackTrace(errorContext, writer, location)
    }
}

@OptIn(ComposeToolingApi::class)
@Suppress("ListIterator")
private fun Throwable.attachComposeStackTrace(
    errorContext: OperationErrorContext?,
    writer: SlotWriter,
    anchor: Anchor?,
): Throwable {
    if (errorContext == null) return this
    return attachComposeStackTrace {
        if (anchor != null) {
            writer.seek(anchor)
        }
        val trace = writer.buildTrace()
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
        trace + parentTrace
    }
}

private fun OperationErrorContext.withCurrentStackTrace(slots: SlotWriter): OperationErrorContext {
    val parent = this
    return object : OperationErrorContext {
        override fun buildStackTrace(currentOffset: Int?): List<ComposeStackTraceFrame> {
            val parentTrace = parent.buildStackTrace(null)
            // Slots are positioned at the start of the next group when insertion happens
            val currentGroup = slots.parent
            if (currentGroup < 0) return parentTrace
            return slots.buildTrace(currentOffset, currentGroup, slots.parent(currentGroup)) +
                parentTrace
        }
    }
}
