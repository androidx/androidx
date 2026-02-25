/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.runtime.composer.linkbuffer

import androidx.collection.IntSet
import androidx.compose.runtime.Anchor
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.Composer
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RememberObserverHolder
import androidx.compose.runtime.ReusableRememberObserverHolder
import androidx.compose.runtime.asLinkRememberObserverHolder
import androidx.compose.runtime.composer.GroupSourceInformation
import androidx.compose.runtime.composer.RememberManager
import androidx.compose.runtime.debugRuntimeCheck
import androidx.compose.runtime.runtimeCheck
import androidx.compose.runtime.tooling.ComposeStackTraceBuilder
import androidx.compose.runtime.tooling.ComposeStackTraceFrame

internal class SlotTableEditor(val table: SlotTable) {
    private var parent = NULL_ADDRESS
    private var current = table.root
    internal val addressSpace = table.addressSpace

    val currentGroup
        get() = current

    val parentGroup
        get() = parent

    val isNode
        get() = IsNodeFlag in addressSpace.groups.groupFlags(current)

    val isGroupEnd
        get() = current == NULL_ADDRESS

    var isClosed = false
        private set

    val isEmpty
        get() = table.isEmpty

    val groupKey
        get() = addressSpace.groups.groupKey(current)

    val objectKey
        get() = objectKey(current)

    val node
        get() = node(current)

    val isParentGroupANode
        get() = IsNodeFlag in addressSpace.groups.groupFlags(parent)

    var previousSibling = NULL_ADDRESS
        private set

    fun groupKey(group: GroupAddress) = addressSpace.groups.groupKey(group)

    fun objectKey(group: GroupAddress): Any? {
        val groups = addressSpace.groups
        val flags = groups.groupFlags(group)
        return if (HasObjectKeyFlag in flags) {
            val slots = addressSpace.slots
            slots[slotAddressOf(groups.groupSlotRange(group)) + objectKeySlotIndex(flags)]
        } else Composer.Empty
    }

    fun node(group: GroupAddress): Any? {
        val groups = addressSpace.groups
        val flags = groups.groupFlags(group)
        return if (IsNodeFlag in flags) {
            val slots = addressSpace.slots
            slots[slotAddressOf(groups.groupSlotRange(group)) + nodeSlotIndex(flags)]
        } else null
    }

    /** Sets the given [group]'s node to [newValue]. */
    fun updateNode(group: GroupAddress = currentGroup, newValue: Any?) {
        val addressSpace = addressSpace
        val groups = addressSpace.groups
        val slots = addressSpace.slots
        val groupFlags = groups.groupFlags(group)
        val slotIndex = nodeSlotIndex(groupFlags)
        debugRuntimeCheck(IsNodeFlag in groupFlags) {
            "Cannot update node for group that does not have node slot"
        }
        val slotRange = groups.groupSlotRange(group)
        val slotAddress = slotAddressOf(slotRange + slotIndex)
        slots[slotAddress] = newValue
    }

    fun flagsOf(group: GroupAddress) = addressSpace.groups.groupFlags(group)

    fun isNode(group: GroupAddress) = IsNodeFlag in flagsOf(group)

    fun nodeCountOf(groups: GroupAddress) = addressSpace.groups.groupNodeCount(groups)

    fun parentOf(group: GroupAddress) = addressSpace.groups.groupParent(group)

    fun firstChildOf(group: GroupAddress) = addressSpace.groups.groupChild(group)

    fun handle() = makeGroupHandle(previousSibling, current)

    fun parentGroup(groupAddress: GroupAddress): GroupAddress =
        addressSpace.groups.groupParent(groupAddress)

    fun close() {
        if (!isClosed) {
            isClosed = true
            table.closeEditor(this)
        }
    }

    fun startGroup() {
        // The first child of current becomes the new current and the current becomes the parent
        // `previousSibling` is NULL_ADDRESS because it is the first child in the list which
        // does not have a sibling
        val current = current
        runtimeCheck(current > 0) {
            "Cannot start a group because current does not refer to a child of a group"
        }
        parent = current
        val groups = addressSpace.groups
        if (current + SLOT_TABLE_GROUP_SIZE > groups.size) return
        this.current = groups.groupChild(current)
        previousSibling = NULL_ADDRESS
    }

    fun endGroup() {
        // The moves to the next sibling group (if there is one).
        // To do this
        //   - parent's next sibling becomes current
        //   - parent becomes the parent's parent (which is the next siblings parent)
        //   - previousSibling becomes parent as it is the previous group to the new current.
        val parent = parent
        debugRuntimeCheck(parent >= 0) { "No group is started. Cannot end group." }
        val groups = addressSpace.groups
        if (parent + SLOT_TABLE_GROUP_SIZE > groups.size) return

        // Adjust the group pointers
        val newCurrent = groups.groupNext(parent)
        this.parent = groups.groupParent(parent)
        previousSibling = parent
        current = newCurrent
    }

    fun removeGroup(freeGroup: Boolean = true) {
        val groups = addressSpace.groups
        val current = current

        val currentFlags = groups.groupFlags(current)
        val nodeCountDelta = -groupFlagsNodeCount(currentFlags)
        val flagsToRemove = propagatingFlagsOf(currentFlags)
        propagateChanges(
            current,
            nodeCountDelta,
            flagsToRemove = flagsToRemove,
            flagsToAdd = 0,
            removingGroup = true,
        )
        val next = groups.groupNext(current)
        val previousSibling = previousSibling

        if (previousSibling == NULL_ADDRESS) {
            val parent = parent
            if (parent == NULL_ADDRESS) {
                table.root = next
            } else {
                groups.groupChild(parent, next)
            }
        } else {
            groups.groupNext(previousSibling, next)
        }
        if (freeGroup) {
            addressSpace.freeGroupTree(current)
        }
        this.current = next
    }

    fun insertGroupFrom(insertTable: SlotTable, handle: GroupHandle) {
        require(insertTable.addressSpace == table.addressSpace) {
            "Cannot insert a group from an unrelated table"
        }
        insertTable.edit {
            seek(handle)
            removeGroup(freeGroup = false)
        }
        insertGroup(handle.group)
    }

    private fun insertGroup(group: GroupAddress) {
        val previousSibling = previousSibling
        val parent = parent
        val groups = addressSpace.groups
        if (previousSibling == NULL_ADDRESS) {
            if (parent == NULL_ADDRESS) table.root = group else groups.groupChild(parent, group)
        } else {
            groups.groupNext(previousSibling, group)
        }
        groups.groupParent(group, parent)
        groups.groupNext(group, current)
        val flags = groups.groupFlags(group)
        val nodeCountDelta = groupFlagsNodeCount(flags)
        this.current = group
        val flagsToAdd = propagatingFlagsOf(flags)
        propagateChanges(
            group,
            nodeCountDelta,
            flagsToRemove = 0,
            flagsToAdd = flagsToAdd,
            removingGroup = false,
        )
    }

    fun moveGroup(offset: Int) {
        if (offset == 0) return
        val current = current
        val previousSibling = previousSibling
        var source = current
        var previousSource = previousSibling
        val groups = addressSpace.groups
        repeat(offset) {
            previousSource = source
            source = groups.groupNext(source)
            check(source != NULL_ADDRESS) { "Offset($offset) too large" }
        }
        val sourceNext = groups.groupNext(source)
        groups.groupNext(previousSource, sourceNext)
        groups.groupNext(source, current)
        if (previousSibling == NULL_ADDRESS) {
            val parent = parent
            groups.groupChild(parent, source)
        } else {
            groups.groupNext(previousSibling, source)
        }
        this.current = source
    }

    fun moveGroup(handle: GroupHandle) {
        val current = current
        val previousSibling = previousSibling
        val source = handle.group
        var previousSource = handle.context
        val groups = addressSpace.groups
        val parent = parent
        if (
            (previousSource == NULL_ADDRESS && groups.groupChild(parent) != source) ||
                (previousSource != NULL_ADDRESS && groups.groupNext(previousSource) != source)
        ) {
            // The group previous to the group being moved or was changed. We need to find the
            // previous sibling to the group to be moved by traversing the list. We assume that the
            // group being move is after current so we start there.
            previousSource = current
            while (previousSource != NULL_ADDRESS && groups.groupNext(previousSource) != source) {
                previousSource = groups.groupNext(previousSource)
            }
            check(previousSource != NULL_ADDRESS) {
                "Could not find the group previous to current($current)"
            }
        }
        val sourceNext = groups.groupNext(source)
        groups.groupNext(previousSource, sourceNext)
        groups.groupNext(source, current)
        if (previousSibling == NULL_ADDRESS) {
            groups.groupChild(parent, source)
        } else {
            groups.groupNext(previousSibling, source)
        }
        this.current = source
    }

    fun moveFrom(
        sourceTable: SlotTable,
        sourceHandle: GroupHandle,
        destination: GroupHandle = NULL_GROUP_HANDLE,
    ) {
        sourceTable.edit { this@SlotTableEditor.moveFrom(this@edit, sourceHandle, destination) }
    }

    fun moveFrom(
        sourceEditor: SlotTableEditor,
        sourceHandle: GroupHandle,
        destination: GroupHandle = NULL_GROUP_HANDLE,
    ): GroupHandle {
        debugRuntimeCheck(sourceHandle != NULL_GROUP_HANDLE) { "Invalid source handle" }
        sourceEditor.seek(sourceHandle)
        val newGroup =
            if (sourceEditor.addressSpace != addressSpace) {
                // This call must be done before the call to removeGroup as removeGroup will also
                // remove any anchors associated with the groups. This ensures that the anchors are
                // moved to the new space before they are removed with the groups.
                val newGroup =
                    addressSpace.copyTreeFrom(sourceEditor.addressSpace, sourceHandle.group)
                sourceEditor.removeGroup(freeGroup = true)
                newGroup
            } else {
                val newGroup = sourceHandle.group
                sourceEditor.removeGroup(freeGroup = false)
                newGroup
            }

        val previous =
            if (destination != NULL_GROUP_HANDLE) {
                handle().also { seek(destination) }
            } else NULL_GROUP_HANDLE
        val previousPreviousSibling = previousSibling
        insertGroup(newGroup)
        previousSibling = previousPreviousSibling
        current = newGroup
        val result = makeGroupHandle(previousPreviousSibling, newGroup)
        if (previous != NULL_GROUP_HANDLE) {
            seek(previous)
        }
        if (table.recordSourceInformation) {
            // The group's source information moved so it needs to be inserted into the new parent
            // in correct location (after the previous sibling)
            addressSpace.recordMovedSourceInformation(newGroup, previousPreviousSibling)
        }
        return result
    }

    fun skipGroup(): Int {
        val current = current
        check(current != NULL_ADDRESS) { "Skipping past the end of a group" }
        this.previousSibling = current
        this.current = addressSpace.groups.groupNext(current)
        return addressSpace.groups.groupNodeCount(current)
    }

    fun skipToGroupEnd() {
        // NOTE: This could be made much faster by storing the child list as a circular list
        // though that will make reading the list slightly slower.
        var current = current
        if (current != NULL_ADDRESS) {
            var previous = previousSibling
            val groups = addressSpace.groups
            while (current != NULL_ADDRESS) {
                previous = current
                current = groups.groupNext(current)
            }
            this.previousSibling = previous
            this.current = NULL_ADDRESS
        }
    }

    fun seek(anchor: LinkAnchor) {
        seek(makeGroupHandle(LAZY_ADDRESS, anchor.address))
    }

    fun seek(handle: GroupHandle) {
        debugRuntimeCheck(containsHandle(handle)) {
            "Handle ${handle.group}:${handle.context} is not in the table being read"
        }
        val handleContext = handle.context
        val groups = addressSpace.groups
        val destinationGroup = handle.group
        val destinationParent =
            when (destinationGroup) {
                NULL_ADDRESS -> handleContext
                else -> groups.groupParent(destinationGroup)
            }
        val destinationPreviousSibling =
            when (destinationGroup) {
                NULL_ADDRESS -> NULL_ADDRESS
                else -> handleContext
            }

        parent = destinationParent
        current = destinationGroup

        // Validate the new previous. This could be wrong because the previous in the handle was
        // moved or removed or a new node was inserted between the previous and the node. If
        // The previous is wrong then the we need to scan to find the correct one.
        var newPrevious = destinationPreviousSibling
        if (
            if (destinationPreviousSibling == NULL_ADDRESS)
                if (destinationParent == NULL_ADDRESS) table.root != destinationGroup
                else groups.groupChild(destinationParent) != destinationGroup
            else groups.groupNext(destinationPreviousSibling) != destinationGroup
        ) {
            // The handle previous no longer points to the this group scan the parent to determine
            // the actual previous
            val firstChild =
                if (destinationParent == NULL_ADDRESS) table.root
                else groups.groupChild(destinationParent)
            newPrevious = NULL_ADDRESS
            run {
                addressSpace.traverseSiblings(firstChild) {
                    if (it == destinationGroup) return@run
                    newPrevious = it
                }
            }
        }

        // Validate that the new previous is valid
        debugRuntimeCheck(
            // Repeat the valid sibling check here to ensure it passes now.
            if (newPrevious == NULL_ADDRESS)
                if (destinationParent == NULL_ADDRESS) table.root == destinationGroup
                else groups.groupChild(destinationParent) == destinationGroup
            else groups.groupNext(newPrevious) == destinationGroup
        ) {
            "Could not find group's previous in seek()"
        }
        previousSibling = newPrevious
    }

    fun updateParentNode(node: Any?) {
        val groups = addressSpace.groups
        val parent = parent
        val parentFlags = groups.groupFlags(parent)
        debugRuntimeCheck(IsNodeFlag in parentFlags) {
            "Cannot update node for group that does not have node slot"
        }
        val slotAddress = slotAddressOf(groups.groupSlotRange(parent)) + nodeSlotIndex(parentFlags)
        addressSpace.slots[slotAddress] = node
    }

    fun updateAux(value: Any?) {
        val groups = addressSpace.groups
        val current = current
        val flags = groups.groupFlags(current)
        debugRuntimeCheck(HasAuxSlotFlag in flags) {
            "Cannot update a group's aux that does not have an aux slot allocated"
        }
        val slotAddress = slotAddressOf(groups.groupSlotRange(current)) + auxSlotIndex(flags)
        addressSpace.slots[slotAddress] = value
    }

    fun setAbsolute(slotAddress: SlotAddress, value: Any?): Any? {
        val slots = addressSpace.slots
        debugRuntimeCheck(slotAddress in 0 until slots.size) {
            "Attempted to write to an invalid slot address."
        }
        val oldValue = slots[slotAddress]
        slots[slotAddress] = value
        return oldValue
    }

    fun setRelative(index: Int, value: Any?): Any? =
        setAbsolute(slotAddressOf(addressSpace.groups.groupSlotRange(parent)) + index, value)

    fun appendSlot(value: Any?) {
        val groups = addressSpace.groups
        val parent = parent
        val slotRange = groups.groupSlotRange(parent)
        if (slotRange == NULL_ADDRESS) {
            addressSpace.writeSlot(parent, 0, value)
        } else {
            addressSpace.slotAddressAndSize(slotRange) { address, size ->
                addressSpace.writeSlot(parent, size, value)
            }
        }
    }

    fun trimSlots(slots: Int) {
        val addressSpace = addressSpace
        val parent = parent
        val groups = addressSpace.groups
        val slotRange = groups.groupSlotRange(parent)
        val size = addressSpace.slotSize(slotRange)
        val newSize = size - slots
        val utilitySlots = utilitySlotsCountForFlags(groups.groupFlags(parent))
        runtimeCheck(newSize >= utilitySlots) { "Attempted to trim more slots than the group has" }
        addressSpace.resizeSlotRangeAtGroup(parent, newSize)
    }

    fun containsHandle(groupHandle: GroupHandle): Boolean {
        val group =
            groupHandle.group.let {
                if (it != NULL_ADDRESS) it
                else {
                    groupHandle.context
                }
            }
        if (group == NULL_ADDRESS) return false
        val root = table.root
        val groups = addressSpace.groups
        addressSpace.traverseGroupAndParents(group) {
            if (it == root) return true
            if (it <= 0) {
                return false
            }
            if (groups.groupParent(it) == -1) {
                // Check if the root of the groupHandle is a sibling of the table root
                addressSpace.traverseSiblings(root) { sibling -> if (sibling == it) return true }
            }
        }
        return false
    }

    inline fun buildInsertTable(block: SlotTableBuilder.() -> Unit): SlotTable =
        SlotTable.build(addressSpace, block)

    /**
     * Replace the key of the current group with one that will not match its current value which
     * will cause the composer to discard it and rebuild the content.
     *
     * This is used during live edit when the function that generated the content has been changed
     * and the slot table information does not match the expectations of the new code. This is done
     * conservatively in that any change in the code is assume to make the state stored in the table
     * incompatible.
     */
    internal fun bashGroup(newKey: Int) {
        addressSpace.groups.groupKey(currentGroup, newKey)
    }

    fun visitSlotsInRememberOrder(
        inGroup: GroupAddress,
        callback: VisitSlotsInRememberOrderCallback,
    ) {
        if (inGroup < 0) return
        val groups = addressSpace.groups
        val slots = addressSpace.slots
        var lastVisitedChild = NULL_ADDRESS
        val slotRange = groups.groupSlotRange(inGroup)

        slots.forEachSlotInRangeIndexed(slotRange) { slotIndex, slotValue ->
            if (slotValue is RememberObserverHolder) {
                val requiredLastChild = slotValue.asLinkRememberObserverHolder().after.address
                while (lastVisitedChild != requiredLastChild) {
                    val nextGroup =
                        when {
                            lastVisitedChild < 0 -> groups.groupChild(inGroup)
                            else -> groups.groupNext(lastVisitedChild)
                        }
                    runtimeCheck(nextGroup >= 0) {
                        "A RememberObserver cannot be forgotten correctly because its group " +
                            "ordering metadata is inconsistent with the rest of the SlotTable"
                    }
                    visitSlotsInRememberOrder(nextGroup, callback)
                    lastVisitedChild = nextGroup
                }
            }
            val shouldClear = callback.visit(inGroup, slotIndex, slotValue)
            if (shouldClear) {
                val slotAddress = slotAddressOf(slotRange)
                slots[slotAddress + slotIndex] = Composer.Empty
            }
        }

        var nextGroup =
            when {
                lastVisitedChild < 0 -> groups.groupChild(inGroup)
                else -> groups.groupNext(lastVisitedChild)
            }
        while (nextGroup >= 0) {
            visitSlotsInRememberOrder(inGroup = nextGroup, callback = callback)
            nextGroup = groups.groupNext(nextGroup)
        }
    }

    internal fun visitTailSlotsInRememberOrder(
        inGroup: GroupAddress,
        firstTailGroupToVisit: GroupAddress,
        tailSlots: Int,
        callback: VisitSlotsInRememberOrderCallback,
    ) {
        if (inGroup < 0) return
        val groups = addressSpace.groups
        val slots = addressSpace.slots

        var inTailGroupRegion = false
        var lastVisitedChild = NULL_ADDRESS
        val slotRange = groups.groupSlotRange(inGroup)
        val slotAddress = slotAddressOf(slotRange)
        val slotSize = addressSpace.slotSize(slotRange)
        val start = slotAddress + slotSize - tailSlots
        val end = start + tailSlots
        slots.forEachSlotInRangeIndexed(start, end) { slotIndex, slotValue ->
            if (slotValue is RememberObserverHolder) {
                val requiredLastChild = slotValue.asLinkRememberObserverHolder().after.address
                while (lastVisitedChild != requiredLastChild) {
                    val nextGroup =
                        when {
                            lastVisitedChild < 0 -> groups.groupChild(inGroup)
                            else -> groups.groupNext(lastVisitedChild)
                        }
                    runtimeCheck(nextGroup >= 0) {
                        "A RememberObserver cannot be forgotten correctly because its group " +
                            "ordering metadata is inconsistent with the rest of the SlotTable"
                    }

                    inTailGroupRegion = inTailGroupRegion or (firstTailGroupToVisit == nextGroup)
                    if (inTailGroupRegion) visitSlotsInRememberOrder(nextGroup, callback)
                    lastVisitedChild = nextGroup
                }
            }

            val shouldClear = callback.visit(inGroup, slotIndex, slotValue)
            if (shouldClear) {
                val slotAddress = slotAddressOf(slotRange)
                slots[slotAddress + slotIndex] = Composer.Empty
            }
        }

        var nextGroup =
            when {
                lastVisitedChild < 0 -> groups.groupChild(inGroup)
                else -> groups.groupNext(lastVisitedChild)
            }
        while (nextGroup >= 0) {
            inTailGroupRegion = inTailGroupRegion or (firstTailGroupToVisit == nextGroup)
            if (inTailGroupRegion) {
                visitSlotsInRememberOrder(inGroup = nextGroup, callback = callback)
            }
            nextGroup = groups.groupNext(nextGroup)
        }
    }

    fun interface VisitSlotsInRememberOrderCallback {
        /**
         * @return true if the slot should be removed (reset to Composer.Empty) after visiting it
         */
        fun visit(group: GroupAddress, slotIndex: Int, slot: Any?): Boolean
    }

    fun removeAllInstancesOfFlags(flags: GroupFlags) {
        val flagsToClear = flags or propagatingFlagsOf(flags)
        val addressSpace = addressSpace
        val groups = addressSpace.groups
        addressSpace.traverseGroupPartially(
            start = table.root,
            includeSiblingsOfStartGroup = true,
        ) { group ->
            val groupFlags = groups.groupFlags(group)
            if (flagsToClear and groupFlags == 0) {
                // Group had no flags to clear. Don't traverse its children.
                false
            } else {
                groups.groupFlags(group, groupFlags and flagsToClear.inv())
                true
            }
        }
    }

    fun addFlagsToAllGroupsIn(groupSet: IntSet, flags: GroupFlags) {
        groupSet.forEach { group ->
            propagateChanges(
                group,
                nodeCountDelta = 0,
                flagsToRemove = 0,
                flagsToAdd = flags,
                removingGroup = false,
            )
        }
    }

    /**
     * Reset the writer to the beginning of the slot table and in the state as if it had just been
     * opened. This differs form closing a writer and opening a new one in that the instance doesn't
     * change.
     */
    fun reset() {
        parent = NULL_ADDRESS
        previousSibling = NULL_ADDRESS
        current = table.root
    }

    private fun propagateChanges(
        group: GroupAddress,
        nodeCountDelta: Int,
        flagsToRemove: Int,
        flagsToAdd: Int,
        removingGroup: Boolean,
    ) {
        var effectiveNodeCountDelta = nodeCountDelta
        var effectiveFlagsToRemove = flagsToRemove
        var effectiveFlagsToAdd = flagsToAdd
        val groups = addressSpace.groups
        addressSpace.traverseParents(group) { current ->
            var flags = groups.groupFlags(current)
            if (effectiveNodeCountDelta != 0) {
                val nodes = groups.groupChildNodeCount(current)
                flags = groups.groupChildNodeCount(current, nodes + effectiveNodeCountDelta)
                if (IsNodeFlag in flags) effectiveNodeCountDelta = 0
            }
            var computedFlagsToRemove = 0
            if (effectiveFlagsToRemove != 0) {
                val flagsToCheck = effectiveFlagsToRemove or (effectiveFlagsToRemove shr 1)
                run {
                    addressSpace.traverseChildren(current) { child ->
                        if (
                            (!removingGroup || child != group) &&
                                flagsToCheck and groups.groupFlags(child) != 0
                        ) {
                            computedFlagsToRemove = 0
                            return@run
                        }
                    }
                    computedFlagsToRemove = effectiveFlagsToRemove
                }
            }
            if (computedFlagsToRemove != 0 || effectiveFlagsToAdd != 0) {
                val newFlags = (flags and computedFlagsToRemove.inv()) or effectiveFlagsToAdd
                if (newFlags != flags) {
                    groups.groupFlags(current, newFlags)
                    effectiveFlagsToRemove = computedFlagsToRemove
                } else {
                    effectiveFlagsToAdd = 0
                }
            } else {
                effectiveFlagsToAdd = 0
            }
            if (
                effectiveNodeCountDelta == 0 &&
                    effectiveFlagsToRemove == 0 &&
                    effectiveFlagsToAdd == 0
            ) {
                // Nothing left to do
                return
            }
        }
    }

    private inline fun Array<Any?>.forEachSlotInRangeIndexed(
        start: Int,
        end: Int,
        block: (index: Int, value: Any?) -> Unit,
    ) {
        for (index in start until end) {
            block(index - start, this[index])
        }
    }

    private inline fun Array<Any?>.forEachSlotInRangeIndexed(
        slotRange: SlotRange,
        block: (index: Int, value: Any?) -> Unit,
    ) {
        if (slotRange != NULL_ADDRESS) {
            addressSpace.slotAddressAndSize(slotRange) { address, size ->
                forEachSlotInRangeIndexed(address, address + size, block)
            }
        }
    }
}

internal fun SlotTableEditor.removeGroupAndForgetSlots(rememberManager: RememberManager) {
    // Notify the lifecycle manager of any observers leaving the slot table
    // The notification order should ensure that listeners are notified of leaving
    // in opposite order that they are notified of entering.

    // To ensure this order, we call `enters` as a pre-order traversal
    // of the group tree, and then call `leaves` in the inverse order.
    visitSlotsInRememberOrder(currentGroup) { _, _, slot ->
        // even that in the documentation we claim ComposeNodeLifecycleCallback should be only
        // implemented on the nodes we do not really enforce it here as doing so will be expensive.
        if (slot is ComposeNodeLifecycleCallback) rememberManager.releasing(slot)
        if (slot is RememberObserverHolder) rememberManager.forgetting(slot)
        if (slot is RecomposeScopeImpl) slot.release()

        return@visitSlotsInRememberOrder false
    }

    removeGroup()
}

/**
 * Notify the lifecycle manager of any observers leaving the slot table The notification order
 * should ensure that listeners are notified of leaving in opposite order that they are notified of
 * entering.
 */
internal fun SlotTableEditor.deactivateGroup(rememberManager: RememberManager) {
    // Notify the lifecycle manager of any observers leaving the slot table
    // The notification order should ensure that listeners are notified of leaving
    // in opposite order that they are notified of entering.

    // To ensure this order, we call `enters` as a pre-order traversal
    // of the group tree, and then call `leaves` in the inverse order.
    visitSlotsInRememberOrder(currentGroup) { slotGroup, slotIndex, data ->
        when (data) {
            is ComposeNodeLifecycleCallback -> {
                if (slotIndex == nodeSlotIndex(flagsOf(slotGroup))) {
                    rememberManager.deactivating(data)
                }
            }
            is ReusableRememberObserverHolder -> {
                // do nothing, the value should be preserved on reuse
            }
            is RememberObserverHolder -> {
                rememberManager.forgetting(data)
                return@visitSlotsInRememberOrder true
            }
            is RecomposeScopeImpl -> {
                data.release()
                return@visitSlotsInRememberOrder true
            }
        }
        return@visitSlotsInRememberOrder false
    }
}

internal fun SlotTableEditor.buildTrace(
    child: Any? = null,
    group: Int = currentGroup,
): List<ComposeStackTraceFrame> {
    if (!isClosed && !isEmpty) {
        return table.addressSpace.buildTrace(group, child, EditorTraceBuilder(this))
    }
    return emptyList()
}

internal class EditorTraceBuilder(private val editor: SlotTableEditor) :
    ComposeStackTraceBuilder() {
    override fun sourceInformationOf(anchor: Anchor): GroupSourceInformation? =
        editor.table.addressSpace.sourceInformationOf(anchor.asLinkAnchor().address)

    override fun groupKeyOf(anchor: Anchor): Int = editor.groupKey(anchor.asLinkAnchor().address)
}
