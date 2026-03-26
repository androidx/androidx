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

import androidx.annotation.IntRange as AndroidxIntRange
import androidx.annotation.VisibleForTesting
import androidx.collection.MutableIntIntMap
import androidx.collection.MutableIntObjectMap
import androidx.collection.MutableIntSet
import androidx.collection.MutableScatterMap
import androidx.collection.mutableIntIntMapOf
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableIntSetOf
import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.Composer
import androidx.compose.runtime.IntStack
import androidx.compose.runtime.composeRuntimeError
import androidx.compose.runtime.debugRuntimeCheck
import androidx.compose.runtime.runtimeCheck
import androidx.compose.runtime.tooling.ComposeStackTraceBuilder
import androidx.compose.runtime.tooling.ComposeStackTraceFrame

internal const val NULL_ADDRESS = -1

/**
 * This value should only be used in a [GroupHandle]. It is a group that is guaranteed to not point
 * to the group of a group handle. This is because the 0 group is used for free space tracking and
 * [groupNext] for 0 will always either be a free group or [NULL_ADDRESS]. When a [GroupHandle] is
 * used in [SlotTableEditor.seek] this will cause the parent list to be scanned to find the actual
 * predecessor. Using [LAZY_ADDRESS] allows a [GroupHandle] to be created when the predecessor is
 * not conveniently available such when [SlotTableReader.reposition].is called. This allows reader
 * repositioning to be `O(1)` and only take the cost of `O(N)` lookup of the predecessor when the
 * group handle is used.
 */
internal const val LAZY_ADDRESS = 0

internal const val SLOT_TABLE_GROUP_SIZE = 6
private const val SLOT_TABLE_GROUP_KEY_OFFSET = 0
private const val SLOT_TABLE_GROUP_NEXT_OFFSET = 1
private const val SLOT_TABLE_GROUP_PARENT_OFFSET = 2
private const val SLOT_TABLE_GROUP_CHILD_OFFSET = 3
private const val SLOT_TABLE_GROUP_FLAGS_OFFSET = 4
private const val SLOT_TABLE_GROUP_SLOTS_OFFSET = 5

internal const val SLOT_TABLE_SLOT_SHIFT = 4
private const val SLOT_TABLE_SLOT_SMALL_SIZE_MASK = 0xF
private const val SLOT_TABLE_SLOT_LARGE_SENTINEL = 0xF
internal const val SLOT_TABLE_SLOT_MAX_SMALL_SIZE = 15

/**
 * When a slot range moves this is the number of extra slots to reserve after it in order to allow
 * it to grow in-place instead of being repeatedly copied.
 */
private const val SLOT_TABLE_SLOT_MOVE_BUFFER_SIZE = 8

/** The initial size of the groups array in the slot table address space. */
private const val SLOT_TABLE_INITIAL_GROUPS_SIZE = SLOT_TABLE_GROUP_SIZE * 128

/** The initial size of the slots array in the slot table address space. */
private const val SLOT_TABLE_INITIAL_SLOTS_SIZE = 256

/**
 * An alias use when an integer value is an index into the groups array of the slot table address
 * space. These are always some multiple of [SLOT_TABLE_GROUP_SIZE] except 0 which is a special
 * group used to track the free list and used size of the groups in the address space. Reserving 0
 * is also a useful debugging aid as it is never a valid group address.
 */
internal typealias GroupAddress = Int

/**
 * An alias used when an integer value is an index into the slots array of the slot table address
 * space.
 */
internal typealias SlotAddress = Int

/**
 * An alias for when an integer value is used to store a range of slots. A slot range is organized:
 * ```
 * bits  | 0 - 3     | 4 - 31  |
 * -----------------------------
 * value | smallSize | address |
 * ```
 *
 * The first 4 bits are used to store the size - 1 of the range (0 sized ranges are special cased as
 * [NULL_ADDRESS], which is -1). A value of 15 (or 0xF) indicates that the value is larger than 15
 * slots and the actual size needs to be looked up in the largeSizes of the address space.
 */
internal typealias SlotRange = Int

/**
 * A [SlotTableAddressSpace] stores the values and groups of one or more slot tables.
 *
 * The groups track of ranges slots [SlotRange] that are inserted with the group and removed when
 * the group is removed. Slots can be added, removed, and modified by the slot table or delegate
 * classes (such as the [SlotTableEditor], for example). Each node can have any number of children
 * stored as a linked list. Each group also has a reference to its parent group (the group for whome
 * it is a direct child).
 *
 * The groups fields are stored linearly in the [groups] array as [SLOT_TABLE_GROUP_SIZE] contiguous
 * [Int] values. Logically the [groups] array can be though of as an array of,
 * ```kt
 * data class Group(
 *   val key: Int,
 *   val next: GroupAddress,
 *   val parent: GroupAddress,
 *   val child: GroupAddress,
 *   val flags: Int,
 *   val slotRange: SlotRange,
 * )
 * ```
 *
 * though they are stored in-place in the groups array. To read and modify the fields of a group,
 * use [groupKey], [groupNext], [groupParent], [groupChild], [groupFlags] and [groupSlotRange].
 *
 * The [slots] array stores the remembered values of a composition and are allocated to a group. The
 * slots array starts off as all elements being [Unallocated]. Slots are allocated by bump
 * allocation starting at the beginning of [slots]. If no room is left in the [slots], [slots] is
 * compacted by moving the range and rewriting the slot range references in groups. The means that
 * the [SlotRange] is the source of truth for the range of a group and must not be stored outside
 * the [groups] as the address in the range may become stale if any slots are allocated from the
 * slot table address space.
 */
internal class SlotTableAddressSpace(
    /** The storage location for slot table groups */
    var groups: IntArray,

    /** The storage location for slot table slots */
    var slots: Array<Any?>,
) {
    private var _largeSizes: MutableIntIntMap? = null
    private var unallocatedStart = 0
    private var unallocatedEnd = slots.size
    private var freeSlotCount = 0
    private var anchors = mutableIntObjectMapOf<LinkAnchor>()
    @VisibleForTesting
    var sourceInformationMap: MutableScatterMap<LinkAnchor, LinkGroupSourceInformation>? = null
    private val largeSizes
        get() =
            _largeSizes
                ?: run {
                    val largeSizes = mutableIntIntMapOf()
                    _largeSizes = largeSizes
                    largeSizes
                }

    constructor(
        @AndroidxIntRange(from = 1) groupsCapacity: Int,
        @AndroidxIntRange(from = 0) slotsCapacity: Int,
    ) : this(groups = newGroupsArray(groupsCapacity), slots = newSlotsArray(slotsCapacity))

    constructor() : this(EmptyGroupData, EmptySlotData) {
        debugRuntimeCheck(
            groups.groupKey(0) == 0 &&
                groups.groupNext(0) == NULL_ADDRESS &&
                groups.groupParent(0) == 0 &&
                groups.groupChild(0) == SLOT_TABLE_GROUP_SIZE &&
                groups.groupFlags(0) == 0 &&
                groups.groupSlotRange(0) == 0
        ) {
            "EmptyGroupData array was written to: ${toDebugString()}."
        }
    }

    /**
     * A map of source marker numbers to their, potentially indirect, parent key. This is recorded
     * for LiveEdit to allow a function that doesn't itself have a group to be invalidated.
     */
    internal var calledByMap: MutableIntObjectMap<MutableIntSet>? = null

    fun validate() {
        groups.validateFreeList()
        groups.validateSlotReferences()
    }

    inline fun allocateGroup(key: Int, parent: GroupAddress, flags: GroupFlags): GroupAddress =
        groups.groupAllocate(key, parent, flags).let {
            if (it < 0) {
                growGroups()
                groups.groupAllocate(key, parent, flags)
            } else it
        }

    fun freeGroupTree(address: GroupAddress) {
        removeSourceInformation(address)
        freeGroup(address)
    }

    private fun removeSourceInformation(address: GroupAddress) {
        val map = sourceInformationMap ?: return
        val anchor = anchors[address] ?: return
        val parent = anchors[groups.groupParent(address)] ?: return
        val parentSourceInformation = map[parent] ?: return
        parentSourceInformation.removeGroup(anchor)
    }

    private fun freeGroup(address: GroupAddress) {
        val groups = groups
        if (address + SLOT_TABLE_GROUP_SIZE > groups.size) {
            // This early return ensures the code generator that an ArrayIndexOutOfBoundsException
            // is impossible and prevents ART from inserting instructions to throw this exception.
            // The was determined to be the best code generation for this function using DEX2OAT.
            // Revalidate the code generation if this code is changed. This code is in the critical
            // path for deleting groups.
            return
        }
        runtimeCheck(!groupFlagsIsMarkedDeleted(groups.groupFlags(address))) {
            "Recursive loop in group structure detected at $address"
        }
        anchors[address]?.let {
            it.address = NULL_ADDRESS
            anchors.remove(address)
            sourceInformationMap?.remove(it)
        }
        freeSlots(groups.groupSlotRange(address))
        groups.groupSlotRange(address, NULL_ADDRESS)
        var child = groups.groupChild(address)
        while (child != NULL_ADDRESS) {
            if (child + SLOT_TABLE_GROUP_SIZE > groups.size) {
                // An assertion for the code generator as above
                return
            }
            val next = groups.groupNext(child)
            freeGroup(child)
            child = next
        }
        groups.groupNext(address, groups.groupNext(0))
        groups.groupParent(address, NULL_ADDRESS)
        groups.groupNext(0, address)
        groups.groupFlags(address, GroupFlagsSpec.CHILD_NODE_COUNT_MASK)
    }

    fun reserveSlots(): Long {
        val reserved = unallocatedStart
        val end = unallocatedEnd
        unallocatedStart = end
        return reserved.toUInt().toLong() or (end.toUInt().toLong() shl Int.SIZE_BITS)
    }

    fun restoreSlots(start: Int, end: Int) {
        debugRuntimeCheck(end >= start) { "Invalid call to restoreSlots" }
        if (end == unallocatedEnd) {
            // The slots array did not grow while the region was reserved, we can just give the
            // space back directly.
            unallocatedStart = start
        }
    }

    fun recordLargeBlock(address: Int, size: Int) {
        largeSizes[address] = size
    }

    fun readSlot(group: GroupAddress, offset: Int) =
        slots[slotAddressOf(groups.groupSlotRange(group)) + offset]

    fun writeSlot(group: GroupAddress, offset: Int, value: Any?): SlotRange {
        val groups = groups
        val range = groups.groupSlotRange(group)
        val newRange =
            if (range == NULL_ADDRESS) {
                val newRange = allocateSlots(offset + 1)
                groups.groupSlotRange(group, newRange)
                newRange
            } else
                slotAddressAndSize(range) { address, size ->
                    if (offset >= size) {
                        growSlotRangeAtGroup(group, size, offset + 1)
                    } else range
                }
        slots[slotAddressOf(newRange) + offset] = value
        return newRange
    }

    fun sourceInformationOf(group: GroupAddress) =
        sourceInformationMap?.let { map -> anchors[group]?.let { anchor -> map[anchor] } }

    fun recordSourceInformation(
        parent: GroupAddress,
        sourceInformation: String?,
        group: GroupAddress,
    ): LinkGroupSourceInformation {
        var sourceInformationMap = sourceInformationMap
        if (sourceInformationMap == null) {
            sourceInformationMap = mutableScatterMapOf()
            this.sourceInformationMap = sourceInformationMap
        }
        return sourceInformationMap.getOrPut(anchorOfAddress(parent)) {
            LinkGroupSourceInformation(0, sourceInformation, 0).also {
                if (sourceInformation == null) {
                    // If we called from a groupless call then the groups added before this call
                    // are not reflected in this group information so they need to be added now
                    // if they exist.
                    var child = groups.groupChild(parent)
                    while (child != group && child != NULL_ADDRESS) {
                        it.reportGroup(anchorOfAddress(child))
                        child = groups.groupNext(child)
                    }
                }
            }
        }
    }

    fun recordCalledBy(key: Int, parentKey: Int) {
        var calledByMap = calledByMap
        if (calledByMap == null) {
            calledByMap = mutableIntObjectMapOf()
            this.calledByMap = calledByMap
        }
        calledByMap.getOrPut(key) { mutableIntSetOf() }.add(parentKey)
    }

    private fun allocateSlots(size: Int): Int {
        // Prefer size at the end of the buffer
        val unallocatedStart = unallocatedStart
        val unallocatedEnd = unallocatedEnd
        if (unallocatedStart + size <= unallocatedEnd) {
            val newAddress = unallocatedStart
            this.unallocatedStart = newAddress + size
            if (isLargeSlotRangeSize(size)) {
                largeSizes[newAddress] = size
            }
            slots.fill(Composer.Empty, newAddress, newAddress + size)
            return slotRangeFromAddressAndSize(newAddress, size)
        } else {
            compactAndMaybeGrow(size)
            val newUnallocatedStart = this.unallocatedStart
            val newUnallocatedEnd = this.unallocatedEnd
            if (newUnallocatedStart + size <= newUnallocatedEnd) {
                val newAddress = newUnallocatedStart
                this.unallocatedStart = newAddress + size
                if (isLargeSlotRangeSize(size)) {
                    largeSizes[newAddress] = size
                }
                slots.fill(Composer.Empty, newAddress, newAddress + size)
                return slotRangeFromAddressAndSize(newAddress, size)
            }
            composeRuntimeError("compactAndMaybeGrow did not grow enough")
        }
    }

    fun resizeSlotRangeAtGroup(group: GroupAddress, size: Int, newSize: Int): SlotRange {
        return when {
            newSize == size -> groups.groupSlotRange(group)
            newSize > size -> growSlotRangeAtGroup(group, size, newSize)
            else -> shrinkSlotRangeAtGroup(group, size, newSize)
        }
    }

    fun resizeSlotRangeAtGroup(group: GroupAddress, newSize: Int): SlotRange {
        val slotRange = groups.groupSlotRange(group)
        return if (slotRange != NULL_ADDRESS || newSize != 0) {
            resizeSlotRangeAtGroup(group, slotSize(slotRange), newSize)
        } else slotRange
    }

    fun copyTreeFrom(
        sourceSpace: SlotTableAddressSpace,
        sourceAddress: GroupAddress,
    ): GroupAddress {
        fun copyGroup(parent: GroupAddress, address: GroupAddress): GroupAddress {
            val sourceGroups = sourceSpace.groups
            val sourceSlots = sourceSpace.slots
            val sourceFlags = sourceGroups.groupFlags(address)
            val newGroupAddress =
                allocateGroup(
                    key = sourceGroups.groupKey(address),
                    parent = parent,
                    flags = sourceFlags,
                )

            // NOTE: copyTreeFrom currently only used when groups are moving from one address space
            // to another. If copyTreeFrom is used to actually make a copy, moveAnchorFrom should
            // only then be called when the copy is in the process of moving a group. Same for
            // moveSourceInformation. In the case of moveSourceInformation a copy of the source
            // information should done at the same time.
            val anchor = moveAnchorFrom(sourceSpace, address, newGroupAddress)
            moveSourceInformation(sourceSpace, anchor)

            val slotRange = sourceGroups.groupSlotRange(address)
            if (slotRange != NULL_ADDRESS) {
                sourceSpace.slotAddressAndSize(slotRange) { address, size ->
                    val newSlotRange = allocateSlots(size)
                    // TODO: Consider coalescing these copies if slot ranges are adjacent in both
                    // the
                    // source and destination
                    sourceSlots.copyInto(
                        destination = slots,
                        destinationOffset = slotAddressOf(newSlotRange),
                        startIndex = address,
                        endIndex = address + size,
                    )
                    groups.groupSlotRange(newGroupAddress, newSlotRange)
                }
            }
            var previousSiblingAddress = NULL_ADDRESS
            var currentChildAddress = sourceGroups.groupChild(address)
            while (currentChildAddress != NULL_ADDRESS) {
                val newChildAddress = copyGroup(newGroupAddress, currentChildAddress)
                if (previousSiblingAddress == NULL_ADDRESS) {
                    groups.groupChild(newGroupAddress, newChildAddress)
                } else {
                    groups.groupNext(previousSiblingAddress, newChildAddress)
                }
                previousSiblingAddress = newChildAddress
                currentChildAddress = sourceGroups.groupNext(currentChildAddress)
            }
            return newGroupAddress
        }

        return copyGroup(NULL_ADDRESS, sourceAddress)
    }

    fun recordMovedSourceInformation(group: GroupAddress, previous: GroupAddress) {
        val sourceInformationMap = sourceInformationMap ?: return
        val parent = groups.groupParent(group)
        val anchor = anchors[parent] ?: return
        val parentInformation = sourceInformationMap[anchor] ?: return
        val previousAnchor = if (previous != NULL_ADDRESS) anchorOfAddress(previous) else null
        parentInformation.addGroupAfter(previousAnchor, anchorOfAddress(group))
    }

    fun anchorOfAddress(address: GroupAddress): LinkAnchor {
        when (address) {
            NULL_ADDRESS -> return NullAnchor
            LAZY_ADDRESS -> return LazyAnchor
        }
        runtimeCheck(address >= 0) { "Invalid anchor address $address" }
        return anchors.getOrPut(address) { LinkAnchor(address) }
    }

    fun ownsAnchor(anchor: LinkAnchor) = anchors[anchor.address] === anchor

    fun moveAnchorFrom(
        sourceSpace: SlotTableAddressSpace,
        oldAddress: GroupAddress,
        newAddress: GroupAddress,
    ): LinkAnchor? {
        debugRuntimeCheck(newAddress !in anchors) {
            "Anchor already exists for group address $newAddress"
        }
        return sourceSpace.anchors.remove(oldAddress)?.let { anchor ->
            anchor.address = newAddress
            anchors[newAddress] = anchor
            anchor
        }
    }

    fun moveSourceInformation(sourceSpace: SlotTableAddressSpace, anchor: LinkAnchor?) {
        if (anchor == null) return
        val sourceSourceInformationMap = sourceSpace.sourceInformationMap ?: return
        val sourceInformation = sourceSourceInformationMap[anchor] ?: return
        var thisSourceInformationMap = sourceInformationMap
        if (thisSourceInformationMap == null) {
            thisSourceInformationMap = mutableScatterMapOf()
            this.sourceInformationMap = thisSourceInformationMap
        } else {
            debugRuntimeCheck(anchor !in thisSourceInformationMap) {
                "Source information already exists for group ${anchor.address}"
            }
        }
        thisSourceInformationMap[anchor] = sourceInformation
        sourceSourceInformationMap.remove(anchor)
    }

    internal fun distanceFrom(groupAddress: GroupAddress, common: GroupAddress): Int {
        var current = groupAddress
        var depth = 0
        val groups = groups
        while (current != common && current >= 0) {
            depth++
            current = groups.groupParent(current)
        }
        return depth
    }

    internal inline fun traverseSiblings(
        group: GroupAddress,
        visit: (group: GroupAddress) -> Unit,
    ) {
        val groups = groups
        var current = group
        while (current >= 0) {
            visit(current)
            current = groups.groupNext(current)
        }
    }

    internal inline fun traverseSiblingsAfter(
        group: GroupAddress,
        visit: (group: GroupAddress) -> Unit,
    ) {
        val groups = groups
        var current = groups.groupNext(group)
        while (current >= 0) {
            visit(current)
            current = groups.groupNext(current)
        }
    }

    internal inline fun traverseChildren(
        parent: GroupAddress,
        visit: (group: GroupAddress) -> Unit,
    ) {
        val groups = groups
        var current = groups.groupChild(parent)
        while (current > 0) {
            visit(current)
            current = groups.groupNext(current)
        }
    }

    internal inline fun traverseParents(group: GroupAddress, visit: (group: GroupAddress) -> Unit) {
        traverseGroupAndParents(group, includeGroup = false, visit = visit)
    }

    internal inline fun traverseGroupAndParents(
        group: GroupAddress,
        includeGroup: Boolean = true,
        visit: (group: GroupAddress) -> Unit,
    ) {
        val groups = groups
        var current = if (!includeGroup) groups.groupParent(group) else group
        while (current > 0) {
            visit(current)
            current = groups.groupParent(current)
        }
        runtimeCheck(current != 0) { "Traversing parent of group not in the slot table: $group" }
    }

    internal inline fun traverseGroup(
        start: GroupAddress,
        includeSiblingsOfStartGroup: Boolean = false,
        visit: (group: GroupAddress) -> Unit,
    ) {
        if (start < 0) return
        val toVisit = IntStack()
        var group = start
        val groups = groups
        while (true) {
            visit(group)
            if (group != start || includeSiblingsOfStartGroup) {
                val nextSibling = groups.groupNext(group)
                if (nextSibling >= 0) toVisit.push(nextSibling)
            }
            val next = groups.groupChild(group)
            if (next >= 0) {
                group = next
            } else {
                if (toVisit.isEmpty()) break
                group = toVisit.pop()
            }
        }
    }

    internal inline fun traverseGroupPartially(
        start: GroupAddress,
        includeSiblingsOfStartGroup: Boolean = false,
        visit: (group: GroupAddress) -> Boolean,
    ) {
        if (start < 0) return
        val toVisit = IntStack()
        var group = start
        val groups = groups
        while (true) {
            val visitChildren = visit(group)
            if (group != start || includeSiblingsOfStartGroup) {
                val nextSibling = groups.groupNext(group)
                if (nextSibling >= 0) toVisit.push(nextSibling)
            }
            val next = groups.groupChild(group)
            if (visitChildren && next >= 0) {
                group = next
            } else {
                if (toVisit.isEmpty()) break
                group = toVisit.pop()
            }
        }
    }

    internal inline fun traverseAllChildren(
        parent: GroupAddress,
        visit: (group: GroupAddress) -> Unit,
    ) {
        if (parent >= 0) {
            traverseGroup(groups.groupChild(parent), true, visit)
        }
    }

    operator fun contains(group: GroupAddress) = group > 0 && group < groups.groupChild(0)

    private fun growSlotRangeAtGroup(group: GroupAddress, currentSize: Int, newSize: Int): Int {
        debugRuntimeCheck(newSize > currentSize) { "Should have called resizeSlotRange() instead" }

        val unallocatedStart = unallocatedStart
        val unallocatedEnd = unallocatedEnd
        debugRuntimeCheck(unallocatedEnd >= unallocatedStart) { "Unexpected unallocated range" }
        val range = groups.groupSlotRange(group)
        val address = slotAddressOf(range)
        if (address + currentSize == unallocatedStart) {
            // Special cases for a block that ends at the start of the unallocated slot range. These
            // special cases should be the common case for building a table.
            if (address + newSize <= unallocatedEnd) {
                // We have room at the end so we can just grow the space in place.
                this.unallocatedStart += newSize - currentSize
                if (newSize > SLOT_TABLE_SLOT_MAX_SMALL_SIZE) {
                    largeSizes[address] = newSize
                }
                val newRange = slotRangeFromAddressAndSize(address, newSize)
                slots.clearRange(address + currentSize, address + newSize)
                groups.groupSlotRange(group, newRange)
                return newRange
            }
        }

        // Try to grow in place. If the slots we need immediately after the current range are all
        // Composer.Empty then this range can grow in place.
        val needed = newSize - currentSize
        if (slots.allUnallocated(address + currentSize, needed)) {
            if (newSize > SLOT_TABLE_SLOT_MAX_SMALL_SIZE) {
                largeSizes[address] = newSize
            }
            val newRange = slotRangeFromAddressAndSize(address, newSize)
            slots.clearRange(address + currentSize, address + newSize)
            groups.groupSlotRange(group, newRange)
            freeSlotCount -= needed
            return newRange
        }

        // If we are growing the slot range, allocate extra space to avoid having to move the
        // slot range again too often.  This will trigger the code immediately above next time
        // the slot range grows allowing it to grow into the extra space.
        val bufferedSize = newSize + SLOT_TABLE_SLOT_MOVE_BUFFER_SIZE
        val bufferedRange = allocateSlots(bufferedSize)
        val newRange = shrinkSlotRange(bufferedRange, bufferedSize, newSize)
        val newAddress = slotAddressOf(newRange)

        // The groups range may have changed during allocate so we need to fetch it again.
        val currentRange = groups.groupSlotRange(group)
        val currentAddress = slotAddressOf(currentRange)
        if (newAddress != currentAddress) {
            slots.copyInto(
                destination = slots,
                destinationOffset = newAddress,
                startIndex = currentAddress,
                endIndex = currentAddress + currentSize,
            )
            freeSlotsAt(currentAddress, currentSize)
        }
        groups.groupSlotRange(group, newRange)
        return newRange
    }

    private fun shrinkSlotRange(range: SlotRange, currentSize: Int, newSize: Int): SlotRange {
        val address = slotAddressOf(range)
        if (newSize == 0) {
            if (range != NULL_ADDRESS) {
                freeSlotsAt(address, currentSize)
            }
            return NULL_ADDRESS
        }

        // Free the slots after the current slots
        val sizeToFree = currentSize - newSize
        val addressToFree = address + newSize
        if (sizeToFree > 0) {
            freeSlotsAt(addressToFree, sizeToFree)
        }
        if (isLargeSlotRangeSize(newSize)) {
            largeSizes[address] = newSize
        }

        // Return the new range
        return slotRangeFromAddressAndSize(address, newSize)
    }

    private fun shrinkSlotRangeAtGroup(
        group: GroupAddress,
        currentSize: Int,
        newSize: Int,
    ): SlotRange {
        val range = groups.groupSlotRange(group)
        val newRange = shrinkSlotRange(range, currentSize, newSize)
        groups.groupSlotRange(group, newRange)
        return newRange
    }

    inline fun slotSize(slotRange: SlotRange): Int {
        if (slotRange == NULL_ADDRESS) return 0
        val smallSize = slotSmallSizeOf(slotRange)
        return if (isLargeSlotRangeSize(smallSize)) largeSizes[slotAddressOf(slotRange)]
        else smallSize
    }

    inline fun <R> slotAddressAndSize(slotRange: Int, block: (address: Int, size: Int) -> R): R {
        val smallSize = slotSmallSizeOf(slotRange)
        val address = slotAddressOf(slotRange)
        val size = if (isLargeSlotRangeSize(smallSize)) largeSizes[address] else smallSize
        return block(address, size)
    }

    private fun freeSlots(slotRange: SlotRange) {
        if (slotRange != NULL_ADDRESS)
            slotAddressAndSize(slotRange) { address, size -> freeSlotsAt(address, size) }
    }

    private fun freeSlotsAt(address: Int, size: Int) {
        debugRuntimeCheck(size > 0) { "Invalid freeSlotAt call" }
        slots.clearRange(address, address + size)
        freeSlotCount += size
        if (isLargeSlotRangeSize(size)) {
            largeSizes.remove(address)
        }
    }

    private fun growGroups() {
        val oldSize = groups.size
        val newSize = (groups.size * 2).coerceAtLeast(SLOT_TABLE_INITIAL_GROUPS_SIZE)
        groups = groups.copyOf(newSize)
        groups.initGroups(oldSize)
    }

    fun toDebugString(): String = buildString {
        append("SlotTableAddressSpace:\n")
        val groups = groups
        append("  Group size: ")
        append(groups.size)
        appendLine()
        append("  Slots size: ")
        append(slots.size)
        appendLine()
        appendLine()

        // Unallocated groups
        append(" Groups:")
        appendLine()
        val unallocatedGroupsStart = groups.groupChild(0)
        val unallocatedGroupSize = (groups.size - unallocatedGroupsStart) / SLOT_TABLE_GROUP_SIZE
        append("  Unallocated groups: ")
        append(unallocatedGroupSize)
        appendLine()

        // Free groups
        var freeGroupCount = 0
        var currentFreeGroup = groups.groupNext(0)
        while (currentFreeGroup != NULL_ADDRESS) {
            freeGroupCount++
            currentFreeGroup = groups.groupNext(currentFreeGroup)
        }
        append("  Free groups:        ")
        append(freeGroupCount)
        appendLine()

        val totalFreeGroups = freeGroupCount + unallocatedGroupSize
        append("  Total free groups:  ")
        append(totalFreeGroups)
        appendLine()

        // Used group percent
        append("  Used group%:        ")
        val usedGroups = groups.size / SLOT_TABLE_GROUP_SIZE - totalFreeGroups
        val availableGroups = groups.size / SLOT_TABLE_GROUP_SIZE
        append(usedGroups.toDouble() / availableGroups.toDouble())
        appendLine()
        appendLine()

        // Slots
        append(" Slots:")
        appendLine()

        // Unallocated slots
        val unallocatedSlotsSize = unallocatedEnd - unallocatedStart
        append("  Unallocated slots: ")
        append(unallocatedSlotsSize)
        appendLine()

        append("  Slot used%:    ")
        val availableSlots = slots.size
        val usedSlots = availableSlots - freeSlotCount - unallocatedSlotsSize
        append(usedSlots.toDouble() / availableSlots.toDouble())
        appendLine()
    }

    private fun compactAndMaybeGrow(required: Int) {
        val slots = slots
        val currentSize = slots.size
        val unallocatedSize = unallocatedEnd - unallocatedStart
        val spaceUsed = slots.size - (unallocatedSize + freeSlotCount)
        val spaceNeeded = spaceUsed + required
        val adjustedSpace = spaceNeeded + (slots.size shr 5)
        val newSize =
            (1 shl (32 - adjustedSpace.countLeadingZeroBits())).let {
                if (it < currentSize) currentSize else it
            }
        debugRuntimeCheck(newSize - unallocatedSize > required)
        val newSlots =
            if (newSize != currentSize) {
                newSlotsArray(newSize.coerceAtLeast(SLOT_TABLE_INITIAL_SLOTS_SIZE))
            } else {
                slots
            }
        val newLargeSizes = mutableIntIntMapOf()
        var current = 0
        val groupsEnd = groups.groupChild(0)

        val mover = SlotMoveManager(source = slots, destination = newSlots)
        for (index in SLOT_TABLE_GROUP_SIZE..groupsEnd - 1 step SLOT_TABLE_GROUP_SIZE) {
            val slotRange = groups.groupSlotRange(index)
            if (slotRange != NULL_ADDRESS) {
                slotAddressAndSize(slotRange) { address, size ->
                    mover.move(
                        destinationOffset = current,
                        startIndex = address,
                        endIndex = address + size,
                    )
                    if (isLargeSlotRangeSize(size)) {
                        newLargeSizes[current] = size
                    }
                    groups.groupSlotRange(index, slotRangeFromAddressAndSize(current, size))
                    current += size
                }
            }
        }
        runtimeCheck(current == spaceUsed) {
            "Unexpected slot compaction result, computed we had $spaceUsed slots, but copied " +
                "$current slots"
        }
        this.slots = mover.done()
        this._largeSizes = newLargeSizes.takeIf { it.isNotEmpty() }
        this.unallocatedStart = current
        this.unallocatedEnd = newSlots.size
        this.freeSlotCount = 0
    }

    private fun IntArray.validateSlotReferences() {
        val map = mutableIntIntMapOf()
        val slotSize = slots.size
        fun slotRangeTextOf(groupAddress: GroupAddress) =
            slotAddressAndSize(groupSlotRange(groupAddress)) { address, size ->
                "$address-${address + size}"
            }
        val last = groupChild(0)
        for (groupAddress in SLOT_TABLE_GROUP_SIZE..last - 1 step SLOT_TABLE_GROUP_SIZE) {
            val range = groupSlotRange(groupAddress)
            if (range != NULL_ADDRESS) {
                slotAddressAndSize(range) { address, size ->
                    if (address < 0) error("Group $groupAddress has an invalid slot address")
                    if (address + size > slotSize) {
                        error("Group $groupAddress slot range extends beyond the slot size")
                    }
                    for (slotAddress in address until address + size) {
                        if (slotAddress in map) {
                            val group = map[slotAddress]
                            error(
                                "Group $groupAddress contains a slot address (${
                                    slotRangeTextOf(groupAddress)
                                }) that overlaps with group $group's address (${
                                    slotRangeTextOf(group)
                                })"
                            )
                        }
                        map[slotAddress] = groupAddress
                    }
                }
            }
        }
        val expectedFreeSlots = slots.size - map.size - (unallocatedEnd - unallocatedStart)
        if (freeSlotCount != expectedFreeSlots) {
            error("Unexpected freeSlotCount, $freeSlotCount, expected $expectedFreeSlots")
        }
    }

    companion object {
        private val EmptyGroupData = newGroupsArray(1 * SLOT_TABLE_GROUP_SIZE)
        private val EmptySlotData = newSlotsArray(0)
    }
}

internal class SlotMoveManager(val source: Array<Any?>, var destination: Array<Any?>) {
    private var pendingMoveOffset = -1
    private var pendingMoveStart = -1
    private var pendingMoveEnd = -1
    private var highest = -1

    fun move(destinationOffset: Int, startIndex: Int, endIndex: Int) {
        if (source === destination) {
            if (startIndex == destinationOffset) return
            val destinationEnd = destinationOffset + (endIndex - startIndex)
            if (!destination.allUnallocated(destinationOffset, destinationEnd)) {
                // One or more slots are allocated, we need to copy before proceeding
                destination = source.copyOf()
            }
        }
        if (pendingMoveEnd == startIndex) {
            pendingMoveEnd = endIndex
        } else {
            flush()
            pendingMoveOffset = destinationOffset
            pendingMoveStart = startIndex
            pendingMoveEnd = endIndex
        }
    }

    fun done(): Array<Any?> {
        flush()
        if (highest >= 0 && highest < destination.size) {
            destination.clearRange(highest, destination.size)
        }
        return destination
    }

    private fun flush() {
        if (pendingMoveOffset >= 0) {
            val source = source
            source.copyInto(
                destination = destination,
                destinationOffset = pendingMoveOffset,
                startIndex = pendingMoveStart,
                endIndex = pendingMoveEnd,
            )
            if (source === destination) {
                source.fill(Unallocated, pendingMoveStart, pendingMoveEnd)
            }
            val end = pendingMoveOffset + (pendingMoveEnd - pendingMoveStart)
            pendingMoveOffset = -1
            pendingMoveEnd = -1
            if (end > highest) highest = end
        }
    }
}

internal inline fun IntArray.groupKey(address: GroupAddress) =
    this[address + SLOT_TABLE_GROUP_KEY_OFFSET]

internal inline fun IntArray.groupNext(address: GroupAddress) =
    this[address + SLOT_TABLE_GROUP_NEXT_OFFSET]

internal inline fun IntArray.groupParent(address: GroupAddress) =
    this[address + SLOT_TABLE_GROUP_PARENT_OFFSET]

internal inline fun IntArray.groupChild(address: GroupAddress) =
    this[address + SLOT_TABLE_GROUP_CHILD_OFFSET]

internal inline fun IntArray.groupFlags(address: GroupAddress) =
    this[address + SLOT_TABLE_GROUP_FLAGS_OFFSET]

internal inline fun IntArray.groupSlotRange(address: GroupAddress) =
    this[address + SLOT_TABLE_GROUP_SLOTS_OFFSET]

internal inline fun IntArray.groupKey(address: GroupAddress, value: Int) {
    this[address + SLOT_TABLE_GROUP_KEY_OFFSET] = value
}

internal inline fun IntArray.groupNext(address: GroupAddress, value: Int) {
    this[address + SLOT_TABLE_GROUP_NEXT_OFFSET] = value
}

internal inline fun IntArray.groupParent(address: GroupAddress, value: Int) {
    this[address + SLOT_TABLE_GROUP_PARENT_OFFSET] = value
}

internal inline fun IntArray.groupChild(address: GroupAddress, value: Int) {
    this[address + SLOT_TABLE_GROUP_CHILD_OFFSET] = value
}

internal inline fun IntArray.groupFlags(address: GroupAddress, value: Int) {
    this[address + SLOT_TABLE_GROUP_FLAGS_OFFSET] = value
}

internal inline fun IntArray.groupNodeCount(address: GroupAddress): Int =
    groupFlagsNodeCount(groupFlags(address))

internal inline fun IntArray.groupChildNodeCount(address: GroupAddress): Int =
    groupFlagsChildNodeCount(groupFlags(address))

internal inline fun IntArray.groupChildNodeCount(address: GroupAddress, value: Int): Int {
    val newFlags = groupFlagsChildNodeCount(groupFlags(address), value)
    groupFlags(address, newFlags)
    return newFlags
}

internal inline fun IntArray.groupSlotRange(address: GroupAddress, value: Int) {
    this[address + SLOT_TABLE_GROUP_SLOTS_OFFSET] = value
}

private inline fun Array<Any?>.clearRange(start: SlotAddress, end: SlotAddress) {
    if (end == start + 1) this[start] = Unallocated else this.fill(Unallocated, start, end)
}

@Suppress("SameParameterValue")
private fun newGroupsArray(capacity: Int): IntArray {
    val array = IntArray(capacity)
    array.groupNext(0, NULL_ADDRESS)

    // Reserves the first group for the free list
    array.initGroups(SLOT_TABLE_GROUP_SIZE)
    return array
}

private fun IntArray?.initGroups(offset: Int) {
    if (this == null) return
    groupNext(0, NULL_ADDRESS)
    groupChild(0, offset)
}

private fun newSlotsArray(capacity: Int): Array<Any?> {
    return arrayOfNulls<Any?>(capacity).apply { fill(Unallocated) }
}

private fun IntArray?.groupAllocate(
    key: Int,
    parent: GroupAddress,
    flags: GroupFlags,
): GroupAddress {
    // Neither of these conditions can ever be true but asserting them here with an early
    // return helps the code generator also know these cannot be true in a way that is relatively
    // cheap.
    if (this == null || size < SLOT_TABLE_GROUP_SIZE) return -1

    // The first group is used to track the free space.
    // groupChild() is the first unused area of the array. It starts as 0 and will increment
    // linearly until the end of the space.
    // groupNext() is a linked list of free nodes. It starts as NULL_ADDRESS
    // If groupChild() >= size the entire array is used but some of it may be free in free list.
    // If groupNext() is NULL_ADDRESS then the free list is empty.
    // The algorithm here prefers to bump allocated the entire array before consulting the free
    // list. If the free list is empty, we will return -1 which will grow the array.
    val address =
        groupChild(0).let {
            if (it >= size) {
                val nextFree = groupNext(0)
                if (nextFree < 0) return -1
                groupNext(0, groupNext(nextFree))
                nextFree
            } else {
                groupChild(0, it + SLOT_TABLE_GROUP_SIZE)
                it
            }
        }
    groupKey(address, key)
    groupParent(address, parent)
    groupNext(address, NULL_ADDRESS)
    groupChild(address, NULL_ADDRESS)
    groupFlags(address, flags)
    groupSlotRange(address, NULL_ADDRESS)
    return address
}

private fun IntArray.validateFreeList(): Int {
    var currentFree = groupNext(0)
    val seen = mutableIntSetOf()
    while (currentFree >= 1) {
        if (currentFree in seen) error("Loop at $currentFree")
        seen.add(currentFree)
        val nextFree = groupNext(currentFree)
        if (nextFree == NULL_ADDRESS) break
        if (nextFree % SLOT_TABLE_GROUP_SIZE != 0 || nextFree < 0)
            error("Invalid free link at $currentFree")
        currentFree = nextFree
    }
    return seen.size
}

internal inline fun slotAddressOf(slotRange: SlotRange) = slotRange shr SLOT_TABLE_SLOT_SHIFT

internal inline fun slotSmallSizeOf(slotRange: SlotRange) =
    (slotRange and SLOT_TABLE_SLOT_SMALL_SIZE_MASK) + 1

internal inline fun isLargeSlotRangeSize(size: Int) = size > SLOT_TABLE_SLOT_MAX_SMALL_SIZE

internal fun slotRangeFromAddressAndSize(address: SlotAddress, size: Int) =
    (address shl SLOT_TABLE_SLOT_SHIFT) or
        if (size > SLOT_TABLE_SLOT_MAX_SMALL_SIZE) {
            SLOT_TABLE_SLOT_LARGE_SENTINEL
        } else {
            size - 1
        }

internal fun SlotTableAddressSpace.buildTrace(
    group: GroupAddress,
    child: Any?,
    traceBuilder: ComposeStackTraceBuilder,
): List<ComposeStackTraceFrame> {
    var childData = child
    traverseGroupAndParents(group) { currentGroup ->
        val flags = groups.groupFlags(currentGroup)
        val objectKey =
            if (HasObjectKeyFlag in flags)
                slots[
                    slotAddressOf(groups.groupSlotRange(currentGroup)) + objectKeySlotIndex(flags)]
            else null

        traceBuilder.processEdge(
            groupKey = groups.groupKey(currentGroup),
            objectKey = objectKey,
            sourceInformation = sourceInformationOf(currentGroup),
            childData = childData,
        )
        childData = anchorOfAddress(currentGroup)
    }
    return traceBuilder.trace()
}

private inline fun Array<Any?>.allUnallocated(start: Int, size: Int): Boolean {
    val end = start + size
    if (end >= this.size) return false
    for (i in start until end) {
        if (this[i] !== Unallocated) return false
    }
    return true
}

internal fun isUnallocated(value: Any?) = value == Unallocated

private val Unallocated: Any =
    object {
        override fun toString() = "Unallocated"
    }
