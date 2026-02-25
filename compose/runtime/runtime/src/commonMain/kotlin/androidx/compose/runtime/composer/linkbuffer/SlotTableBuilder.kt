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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.runtime.composer.linkbuffer

import androidx.compose.runtime.Anchor
import androidx.compose.runtime.Composer
import androidx.compose.runtime.IntStack
import androidx.compose.runtime.composer.GroupSourceInformation
import androidx.compose.runtime.debugRuntimeCheck
import androidx.compose.runtime.tooling.ComposeStackTraceBuilder
import androidx.compose.runtime.tooling.ComposeStackTraceFrame

internal class SlotTableBuilder(
    val table: SlotTable,
    var recordSourceInformation: Boolean,
    var recordCallByInformation: Boolean,
) {
    // The address space the table is in.
    private val addressSpace = table.addressSpace

    // The current parent of new child groups
    private var parent = NULL_ADDRESS

    // The stack of previous parents that will become [parent] once the current group ends.
    // TODO: Use the parent stored in the group instead of a stack
    private val parentStack = IntStack()

    // The previous child that will point to any new groups create. NULL_ADDRESS indicates that
    // the next child is the first child to be created in a group.
    private var previousSibling = lastSiblingOf(table.root, addressSpace)

    // The stack of previous siblings that will become [previousSibling] when the current group
    // ends.
    private val previousSiblingStack = IntStack()

    // The count of the nodes in the children of this group
    private var nodeCount = 0

    // A cached reference to [addressSpace.slots] to save an indirection in most cases. Care must
    // be taken with this value as [addressSpace.slots] may change if the address space for the
    // slots change.
    private var slots = addressSpace.slots

    // The slot address in the slots array the parent groups slots start.
    private var slotStart = 0

    // The slot address in the slots array that is the next slot to be written
    private var slotCurrent = 0

    // The address of the end of the current slot region allocated to the group.
    private var slotEnd = 0

    // Whether the current slot is using reserved range space
    private var inReservedRange = false

    // The slot address of the start of the reserved range of the slot address space.
    private var slotReserveStart = 0

    // The slot address of the end of the reserved range of the slot address space.
    private var slotReserveEnd = 0

    // The slot address of the last used part of the reserved range of the slot address space.
    private var slotReserveUsedUpTo = 0

    constructor(
        addressSpace: SlotTableAddressSpace,
        recordSourceInformation: Boolean,
        recordCallByInformation: Boolean,
    ) : this(
        SlotTable(
            addressSpace = addressSpace,
            recordSourceInformation = recordSourceInformation,
            recordCallByInformation = recordCallByInformation,
        ),
        recordSourceInformation,
        recordCallByInformation,
    )

    /** True when [close] has been called */
    var isClosed = false
        private set

    val isEmpty
        get() = parent == NULL_ADDRESS

    val parentGroup
        get() = parent

    val parentAnchor
        get() = addressSpace.anchorOfAddress(parentGroup)

    val slotIndex
        get() = slotCurrent - slotStart

    val parentHandle: GroupHandle
        get() =
            makeGroupHandle(
                if (previousSiblingStack.isEmpty()) NULL_ADDRESS else previousSiblingStack.peek(),
                parent,
            )

    fun groupKey(address: GroupAddress) = addressSpace.groups.groupKey(address)

    fun groupObjectKey(address: GroupAddress) =
        addressSpace.groups.let {
            val flags = it.groupFlags(address)
            if (HasObjectKeyFlag in flags)
                slots[slotAddressOf(it.groupSlotRange(address)) + objectKeySlotIndex(flags)]
            else null
        }

    fun groupAux(address: GroupAddress) =
        addressSpace.groups.let {
            val flags = it.groupFlags(address)
            if (HasAuxSlotFlag in flags)
                slots[slotAddressOf(it.groupSlotRange(address)) + auxSlotIndex(flags)]
            else Composer.Empty
        }

    fun flagsOf(address: SlotAddress) = addressSpace.groups.groupFlags(address)

    fun isNode() =
        parent.let { it != NULL_ADDRESS && IsNodeFlag in addressSpace.groups.groupFlags(it) }

    fun lastRoot(): GroupHandle {
        var last = table.root
        var previous = NULL_ADDRESS
        if (last != NULL_ADDRESS) {
            addressSpace.traverseSiblingsAfter(table.root) { group ->
                previous = last
                last = group
            }
        }

        return makeGroupHandle(previous, last)
    }

    fun parent(address: GroupAddress) = addressSpace.groups.groupParent(address)

    fun buildStart() {
        // Reserve the end of the slot array for adding
        reserveSlotSlotRegion()
    }

    fun collectSourceInformation() {
        recordSourceInformation = true
        table.recordSourceInformation = true
    }

    fun collectCallByInformation() {
        recordCallByInformation = true
        table.recordCallByInformation = true
    }

    inline fun startGroup(key: Int, objectKey: Any? = Composer.Empty) {
        startNewGroup(
            key = key,
            flags =
                when {
                    objectKey === Composer.Empty -> 0
                    else -> HasObjectKeyFlag
                },
            objectKey = objectKey,
            aux = null,
            node = null,
        )
    }

    inline fun startNodeGroup(key: Int, objectKey: Any?, node: Any?) =
        startNewGroup(
            key = key,
            flags =
                when {
                    objectKey === Composer.Empty -> IsNodeFlag
                    else -> IsNodeFlag or HasObjectKeyFlag
                },
            objectKey = objectKey,
            aux = null,
            node = node,
        )

    inline fun startDataGroup(key: Int, objectKey: Any?, aux: Any?) =
        startNewGroup(
            key = key,
            flags =
                when {
                    objectKey === Composer.Empty -> HasAuxSlotFlag
                    else -> HasAuxSlotFlag or HasObjectKeyFlag
                },
            objectKey = objectKey,
            aux = aux,
            node = null,
        )

    private fun startNewGroup(key: Int, flags: GroupFlags, objectKey: Any?, aux: Any?, node: Any?) {
        val parent = parent

        // Allocate a group and connect it to the tree of groups as a child of the parent
        // and next sibling of the previous sibling group.
        val group = addressSpace.allocateGroup(key, parent, flags)
        val groups = addressSpace.groups
        val previousSibling = previousSibling
        if (previousSibling == NULL_ADDRESS) {
            if (parent == NULL_ADDRESS) {
                table.root = group
            } else {
                groups.groupChild(parent, group)
            }
        } else {
            groups.groupNext(previousSibling, group)
        }

        // Preserve the state of the group that should be restored by endGroup
        parentStack.push(parent)
        previousSiblingStack.push(previousSibling)
        this.parent = group
        this.previousSibling = NULL_ADDRESS
        if (parent != NULL_ADDRESS) {
            groups.groupChildNodeCount(parent, nodeCount)
        }
        nodeCount = 0

        // Preserve the state of the slots that should be restored by endGroup.
        saveSlotRange(parent)
        val newStart = slotReserveUsedUpTo
        slotStart = newStart
        slotCurrent = newStart
        slotEnd = slotReserveEnd
        inReservedRange = true

        if (IsNodeFlag in flags) append(node)
        if (HasObjectKeyFlag in flags) append(objectKey)
        if (HasAuxSlotFlag in flags) append(aux)

        // Record provisional slot range
        val modifiedCurrent = slotCurrent
        val modifiedStart = slotStart
        if (modifiedCurrent > modifiedStart) {
            groups.groupSlotRange(
                group,
                slotRangeFromAddressAndSize(modifiedStart, modifiedCurrent - modifiedStart),
            )
        }

        if (recordSourceInformation && parent >= 0) {
            addressSpace
                .recordSourceInformation(parent, null, group)
                .reportGroup(addressSpace.anchorOfAddress(group))
        }
    }

    fun endGroup(): Int {
        val previousParent = parent
        val groups = addressSpace.groups
        groups.groupChildNodeCount(previousParent, nodeCount)
        saveSlotRange(group = previousParent)
        val newParent = parentStack.pop()
        parent = newParent
        val previousSibling = previousSiblingStack.pop()
        this.previousSibling =
            if (previousSibling == NULL_ADDRESS) {
                if (newParent == NULL_ADDRESS) table.root else groups.groupChild(newParent)
            } else groups.groupNext(previousSibling)
        restoreFromSlotRange(parent)
        val nodeGroupCount = groups.groupNodeCount(previousParent)
        nodeCount = groups.groupChildNodeCount(parent) + nodeGroupCount
        return nodeGroupCount
    }

    fun append(value: Any?) {
        if (slotCurrent < slotEnd) {
            slots[slotCurrent++] = value
            return
        }

        // We are adding to a slot region that is not at the end of the slot address space so
        // we may need to either grow it in place or move it.
        slowAppend(value)
    }

    /**
     * Inserts an aux slot into the parent group and initializes its value to [value]. The group
     * must not already have an aux slot, and must not have more than one slot currently in use.
     */
    fun insertAux(value: Any?) {
        val group = parentGroup
        val groups = addressSpace.groups
        val oldFlags = groups.groupFlags(group)

        debugRuntimeCheck(HasAuxSlotFlag !in oldFlags) {
            "Cannot insert aux $value for group $group because it already has an aux slot."
        }

        val updatedFlags = oldFlags or HasAuxSlotFlag
        groups.groupFlags(group, updatedFlags)

        // Allocate space for the new slot, the value will not be used if it is in the wrong place
        // but this ensures the slot space has enough room to insert the slot.
        append(value)

        // Shift the values if necessary
        val auxAddress = slotStart + auxSlotIndex(updatedFlags)
        if (auxAddress + 1 != slotCurrent) {
            // There are values to move, move them first
            val slots = addressSpace.slots
            slots.copyInto(
                destination = slots,
                destinationOffset = auxAddress + 1,
                startIndex = auxAddress,
                endIndex = slotCurrent - 1,
            )

            // Update the aux slot value.
            slots[auxAddress] = value
        }
    }

    fun addFlags(flags: GroupFlags) {
        val groups = addressSpace.groups
        val newFlags = flags or groups.groupFlags(parent)
        groups.groupFlags(parent, newFlags)
        val propagatingFlags = propagatingFlagsOf(newFlags)
        if (propagatingFlags != 0) {
            addressSpace.traverseParents(parent) {
                val flags = groups.groupFlags(it)
                if (propagatingFlags in flags) return
                groups.groupFlags(it, flags or propagatingFlags)
            }
        }
    }

    fun moveFrom(sourceEditor: SlotTableEditor, sourceHandle: GroupHandle) {
        debugRuntimeCheck(sourceEditor.addressSpace == addressSpace) {
            "Cannot move groups to a build from a separate address space"
        }
        val previous = sourceEditor.handle()
        sourceEditor.seek(sourceHandle)
        sourceEditor.removeGroup(false)
        sourceEditor.seek(previous)
        val group = sourceHandle.group
        val groups = addressSpace.groups

        // Insert the node
        val parent = parent
        val previousSibling = previousSibling
        if (previousSibling == NULL_ADDRESS) {
            if (parent == NULL_ADDRESS) {
                table.root = group
            } else {
                groups.groupChild(parent, group)
            }
        } else {
            groups.groupNext(previousSibling, group)
        }
        groups.groupParent(group, parent)
        groups.groupNext(previousSibling, group)
        groups.groupNext(group, NULL_ADDRESS)
        this.previousSibling = group
        nodeCount += groups.groupNodeCount(group)
        // Propagate the new flags and additional nodes
        val propagatingFlags = propagatingFlagsOf(groups.groupFlags(group))
        if (propagatingFlags != 0) {
            run {
                addressSpace.traverseGroupAndParents(parent) { ancestor ->
                    val ancestorFlags = groups.groupFlags(ancestor)
                    val alreadyHave = ancestorFlags and propagatingFlags
                    if (alreadyHave != propagatingFlags) {
                        groups.groupFlags(ancestor, ancestorFlags or propagatingFlags)
                    } else return@run
                }
            }
        }
    }

    @Suppress("unused")
    fun recordGroupSourceInformation(sourceInformation: String) {
        if (recordSourceInformation) {
            addressSpace.recordSourceInformation(parent, sourceInformation, NULL_ADDRESS)
        }
    }

    @Suppress("unused")
    fun recordGrouplessCallSourceInformationStart(key: Int, sourceInformation: String) {
        if (recordCallByInformation) {
            addressSpace.recordCalledBy(key, groupKey(parent))
        }
        if (recordSourceInformation) {
            addressSpace
                .recordSourceInformation(parent, null, NULL_ADDRESS)
                .startGrouplessCall(key, sourceInformation, slotCurrent - slotStart)
        }
    }

    fun recordGrouplessCallSourceInformationEnd() {
        if (recordSourceInformation) {
            addressSpace
                .recordSourceInformation(parent, null, NULL_ADDRESS)
                .endGrouplessCall(slotCurrent - slotStart)
        }
    }

    fun close() {
        debugRuntimeCheck(!isClosed) { "Closing an already closed builder" }
        isClosed = true
    }

    fun build(): SlotTable {
        buildEnd()
        close()
        return table
    }

    private fun buildEnd() {
        if (parent != NULL_ADDRESS) {
            // If we are being ended before the slot table is complete (because of an exception,
            // for example) then we need to ensure the groups reference the correct slots before we
            // leave or the slots can become lost
            saveSlotRange(parent)
        }
        returnReservedSlotRegion()
    }

    private fun saveSlotRange(group: Int): Int {
        if (group < 0) return 0
        val groups = addressSpace.groups
        val slotCurrent = slotCurrent
        val slotAddress = slotStart
        if (slotCurrent > slotAddress) {
            // Slots were written to the parent, convert it to a range
            val slotSize =
                if (inReservedRange) {
                    // Slots are in the reserved range, update the reserved range and
                    // save the new slot address into the slot range of the group
                    val slotSize = slotCurrent - slotAddress
                    val slotRange = slotRangeFromAddressAndSize(slotAddress, slotSize)
                    if (isLargeSlotRangeSize(slotSize)) {
                        addressSpace.recordLargeBlock(slotAddress, slotSize)
                    }
                    slotReserveUsedUpTo = slotCurrent
                    groups.groupSlotRange(group, slotRange)
                    slotSize
                } else {
                    val slotSize = slotCurrent - slotAddress
                    val slotAllocated = slotEnd - slotAddress
                    if (slotAllocated != slotSize) {
                        // This will be may be if slots are added to a group after a child was
                        // added.
                        addressSpace.resizeSlotRangeAtGroup(group, slotAllocated, slotSize)
                    }
                    slotSize
                }
            return slotSize
        } else {
            groups.groupSlotRange(group, NULL_ADDRESS)
            return 0
        }
    }

    private fun restoreFromSlotRange(group: GroupAddress) {
        val groups = addressSpace.groups
        val slotRange = groups.groupSlotRange(group)
        if (slotRange != NULL_ADDRESS) {
            addressSpace.slotAddressAndSize(slotRange) { address, size ->
                slotStart = address
                val end = address + size
                slotEnd = end
                slotCurrent = end
            }
            inReservedRange = false
        } else {
            // If if doesn't have a slot range then ensure it will allocate from the reserved
            // range, if slots are added.
            val reserve = slotReserveUsedUpTo
            slotStart = reserve
            slotCurrent = reserve
            slotEnd = slotReserveEnd
            inReservedRange = true
        }
    }

    private fun reserveSlotSlotRegion() {
        val reservation = addressSpace.reserveSlots()
        val start = reservation.toInt()
        val end = (reservation ushr Int.SIZE_BITS).toInt()
        slotReserveStart = start
        slotReserveUsedUpTo = start
        slotReserveEnd = end
    }

    private fun returnReservedSlotRegion() {
        if (slotReserveStart != slotReserveEnd) {
            addressSpace.restoreSlots(slotReserveUsedUpTo, slotReserveEnd)
            slotReserveStart = 0
            slotReserveUsedUpTo = 0
            slotReserveEnd = 0
        }
    }

    private fun slowAppend(value: Any?) {
        val parent = parent
        val size = saveSlotRange(group = parent)
        returnReservedSlotRegion()

        // if the group has children already and it is a small slot space we need to grow it to
        // a large space first. This avoids growing slot space for each write
        addressSpace.writeSlot(parent, size, value)

        // The slots array may have changed in writeSlot()
        slots = addressSpace.slots
        reserveSlotSlotRegion()
        restoreFromSlotRange(group = parent)
    }
}

internal inline operator fun Int.contains(other: Int): Boolean = (other and this) == other

internal class BuilderTraceBuilder(private val builder: SlotTableBuilder) :
    ComposeStackTraceBuilder() {
    override fun sourceInformationOf(anchor: Anchor): GroupSourceInformation? =
        builder.table.addressSpace.sourceInformationOf(anchor.asLinkAnchor().address)

    override fun groupKeyOf(anchor: Anchor): Int = builder.groupKey(anchor.asLinkAnchor().address)
}

internal fun SlotTableBuilder.buildTrace(): List<ComposeStackTraceFrame> {
    if (!isClosed && !isEmpty) {
        return table.addressSpace.buildTrace(parentGroup, slotIndex, BuilderTraceBuilder(this))
    }
    return emptyList()
}

private inline fun lastSiblingOf(
    address: GroupAddress,
    addressSpace: SlotTableAddressSpace,
): GroupAddress =
    if (address == NULL_ADDRESS) NULL_ADDRESS
    else {
        var last = NULL_ADDRESS
        addressSpace.traverseSiblings(address) { last = it }
        last
    }
