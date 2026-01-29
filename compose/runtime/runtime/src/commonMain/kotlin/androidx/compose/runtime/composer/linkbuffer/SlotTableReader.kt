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

import androidx.compose.runtime.Anchor
import androidx.compose.runtime.Composer
import androidx.compose.runtime.IntStack
import androidx.compose.runtime.composer.GroupSourceInformation
import androidx.compose.runtime.debugRuntimeCheck
import androidx.compose.runtime.runtimeCheck
import androidx.compose.runtime.tooling.ComposeStackTraceBuilder
import androidx.compose.runtime.tooling.ComposeStackTraceFrame

internal class SlotTableReader(val table: SlotTable) {
    private var addressSpace = table.addressSpace
    private var groups = addressSpace.groups
    private var slots: Array<Any?> = table.addressSpace.slots
    private var parent = NULL_ADDRESS
    private var _current = table.root
    private var current: GroupAddress
        get() = _current
        set(value) {
            _current = value
        }

    var slotCurrent = 0
    var slotEnd = 0
    val slotIndex
        get() = if (parent >= 0) slotCurrent - slotAddressOf(groups.groupSlotRange(parent)) else 0

    private val previousSlotCurrentOffset = IntStack()
    private var emptyCount = 0

    var hadNext = false
        private set

    var isClosed = false
        private set

    val isEmpty
        get() = table.isEmpty

    private var _previousSibling = NULL_ADDRESS
    var previousSibling: GroupAddress
        get() = _previousSibling
        private set(value) {
            _previousSibling = value
        }

    val remainingSlots
        get() = slotEnd - slotCurrent

    fun get(address: GroupAddress, slotIndex: Int): Any? {
        if (slotIndex >= 0) {
            val groups = groups
            val slots = slots
            val slotRange = groups.groupSlotRange(address)
            if (slotRange != NULL_ADDRESS) {
                val flags = groups.groupFlags(address)
                addressSpace.slotAddressAndSize(slotRange) { address, size ->
                    val offset = slotIndex + utilitySlotsCountForFlags(flags)
                    if (offset < size) return slots[address + offset]
                }
            }
        }
        return Composer.Empty
    }

    fun getOrNull(address: GroupAddress, slotIndex: Int) =
        get(address, slotIndex)?.let { if (it == Composer.Empty) null else it }

    fun nodeCount(address: GroupAddress) = groupFlagsNodeCount(groups.groupFlags(address))

    val parentCurrentSlotOffset: Int
        get() {
            val slotRange = groups.groupSlotRange(parent)
            if (slotRange == NULL_ADDRESS) return 0
            val slotAddress = slotAddressOf(slotRange)
            return slotCurrent - slotAddress
        }

    val groupAux
        get() = groupAux(current)

    val groupKey
        get() = current.let { if (it != NULL_ADDRESS) addressSpace.groups.groupKey(it) else 0 }

    val groupObjectKey: Any?
        get() = groupObjectKey(current)

    val groupNode: Any?
        get() = groupNode(current)

    fun groupAux(group: GroupAddress): Any? {
        val flags = groups.groupFlags(group)
        val slotRange = groups.groupSlotRange(group)
        return if (HasAuxSlotFlag in flags) {
            upToDateSlots()[slotAddressOf(slotRange) + auxSlotIndex(flags)]
        } else Composer.Empty
    }

    val hasObjectKey: Boolean
        get() = hasObjectKey(current)

    fun hasObjectKey(address: GroupAddress): Boolean =
        HasObjectKeyFlag in groups.groupFlags(address)

    fun groupObjectKey(address: GroupAddress): Any? {
        val flags = groups.groupFlags(address)
        val slotRange = groups.groupSlotRange(address)
        return if (HasObjectKeyFlag in flags) {
            upToDateSlots()[slotAddressOf(slotRange) + objectKeySlotIndex(flags)]
        } else null
    }

    fun groupNode(group: GroupAddress): Any? {
        val flags = groups.groupFlags(group)
        val slotRange = groups.groupSlotRange(group)
        return if (IsNodeFlag in flags) {
            upToDateSlots()[slotAddressOf(slotRange) + nodeSlotIndex(flags)]
        } else null
    }

    val isGroupEnd
        get() = current == NULL_ADDRESS && !inEmpty

    val isNode
        get() = IsNodeFlag in groups.groupFlags(current)

    val inEmpty
        get() = emptyCount > 0

    fun isNode(group: GroupAddress) = IsNodeFlag in groups.groupFlags(group)

    val currentGroup: GroupAddress
        get() = current

    val parentGroup: GroupAddress
        get() = parent

    val parentAnchor: LinkAnchor
        get() = addressSpace.anchorOfAddress(parentGroup)

    val parentHandle: GroupHandle
        get() = makeGroupHandle(LAZY_ADDRESS, parent)

    val parentNode
        get() = groupNode(parent)

    /** The number of nodes in the current group, NOT including the current group itself. */
    val nodeCount: Int
        get() = groupFlagsChildNodeCount(groups.groupFlags(current))

    val parentNodeCount: Int
        get() = if (parent != NULL_ADDRESS) groupFlagsNodeCount(groups.groupFlags(parent)) else 0

    val groupReferenceSlotStartAddress: SlotAddress
        get() = slotAddressOf(groups.groupSlotRange(parent))

    val nextParentSlotAddress
        get() = slotCurrent

    fun node(group: GroupAddress) = slots[slotAddressOf(groups.groupSlotRange(group))]

    fun maybeNode(group: GroupAddress) =
        if (IsNodeFlag in groups.groupFlags(group)) {
            upToDateSlots()[slotAddressOf(groups.groupSlotRange(group))]
        } else Composer.Empty

    fun parentOf(group: GroupAddress) = groups.groupParent(group)

    fun firstChildOf(group: GroupAddress) = groups.groupChild(group)

    fun nextSiblingOf(group: GroupAddress) = groups.groupNext(group)

    fun childNodeCountOf(group: GroupAddress) = groupFlagsChildNodeCount(groups.groupFlags(group))

    fun handle(): GroupHandle {
        val handle =
            makeGroupHandle(parent = parent, predecessor = previousSibling, group = current)
        return handle
    }

    fun rootHandle() = makeGroupHandle(NULL_ADDRESS, table.root)

    fun recomposeRequired(group: GroupAddress) =
        IsRecompositionRequiredFlag in groups.groupFlags(group)

    fun hasRecomposeRequired(group: GroupAddress) =
        ((HasRecompositionRequiredFlag or IsRecompositionRequiredFlag) and
            groups.groupFlags(group)) != 0

    fun flagsOf(address: GroupAddress): GroupFlags = groups.groupFlags(address)

    fun parentGroupFlags(): GroupFlags = flagsOf(parent)

    fun close() {
        if (!isClosed) {
            isClosed = true
            table.closeReader(this)
        }
    }

    fun startGroup() {
        val current = current
        parent = current
        val groups = groups
        if (current + SLOT_TABLE_GROUP_SIZE > groups.size) return
        this.current = groups.groupChild(current)
        previousSibling = NULL_ADDRESS
        previousSlotCurrentOffset.push(slotEnd - slotCurrent)
        val slotRange = groups.groupSlotRange(current)
        if (slotRange != NULL_ADDRESS) {
            val flags = groups.groupFlags(current)
            slotCurrent = slotAddressOf(slotRange) + utilitySlotsCountForFlags(flags)
            slotEnd = slotAddressOf(slotRange) + addressSpace.slotSize(slotRange)
        } else {
            slotCurrent = NULL_ADDRESS
            slotEnd = NULL_ADDRESS
        }
    }

    /** Enters a group like [startGroup] does, and verifies that the group is a node group. */
    fun startNode() {
        runtimeCheck(isNode) { "Expected a node group" }
        startGroup()
    }

    fun endGroup() {
        val parent = parent
        val array = groups
        if (parent + SLOT_TABLE_GROUP_SIZE > array.size) return
        val newCurrent = array.groupNext(parent)
        val newParent = array.groupParent(parent)
        this.parent = newParent
        previousSibling = parent
        current = newCurrent
        val slotRange = groups.groupSlotRange(newParent)
        slotEnd = slotAddressOf(slotRange) + addressSpace.slotSize(slotRange)
        slotCurrent = slotEnd - previousSlotCurrentOffset.popOr(0)
    }

    fun skipGroup(): Int {
        val current = current
        val groups = groups
        if (current + SLOT_TABLE_GROUP_SIZE > groups.size) return 0
        val nodes = groupFlagsNodeCount(groups.groupFlags(current))
        this.current = groups.groupNext(current)
        this.previousSibling = current
        return nodes
    }

    fun skipToGroupEnd() {
        current = NULL_ADDRESS
        previousSibling = LAZY_ADDRESS
        slotCurrent = 0
        slotEnd = 0
    }

    /**
     * Restore the parent to a previous parent but in a way that leaves the reader only in a
     * partially valid state. The only supported operations after this call are [skipToGroupEnd] or
     * [reposition].
     */
    fun restoreParent(parent: GroupAddress) {
        previousSibling = LAZY_ADDRESS
        this.parent = parent
        slotCurrent = 0
        slotEnd = 0
    }

    fun next(): Any? =
        if (inEmpty || slotCurrent >= slotEnd) {
            hadNext = false

            Composer.Empty
        } else {
            hadNext = true
            slots[slotCurrent++]
        }

    fun groupKey(group: GroupAddress) = groups.groupKey(group)

    fun get(index: Int) = get(current, index)

    fun beginEmpty() {
        emptyCount++
    }

    fun endEmpty() {
        runtimeCheck(emptyCount > 0) { "Unbalanced begin/end empty" }
        emptyCount--

        // The slot address could have changed due to compaction caused by building the
        // insert table. Update the cached values for the arrays and re-read the slot location for
        // the parent.
        if (emptyCount == 0) {
            slots = addressSpace.slots
            groups = addressSpace.groups
            val offset = slotEnd - slotCurrent
            val slotRange = groups.groupSlotRange(parent)
            if (slotRange != NULL_ADDRESS) {
                addressSpace.slotAddressAndSize(slotRange) { address, size ->
                    slotCurrent = address + size - offset
                    slotEnd = address + size
                }
            }
        }
    }

    fun reposition(group: GroupAddress) {
        debugRuntimeCheck(group > 0) { "Cannot reposition to group $group" }
        reposition(makeGroupHandle(LAZY_ADDRESS, group))
    }

    fun reposition(handle: GroupHandle) {
        runtimeCheck(!inEmpty) { "Cannot reposition while in an empty region" }
        current = handle.group
        previousSibling = handle.context
        parent = groups.groupParent(current)
    }

    /**
     * Extract the keys from this point to the end of the group. The current is left unaffected.
     * Must be called inside a group.
     */
    fun extractKeys(): MutableList<KeyInfo> {
        val result = mutableListOf<KeyInfo>()
        if (inEmpty) return result
        var predecessor = previousSibling
        var index = 0
        val groups = groups
        val slots = slots
        table.traverseSiblings(currentGroup) { address ->
            val flags = groups.groupFlags(address)
            val slotAddress = slotAddressOf(groups.groupSlotRange(address))
            result.add(
                KeyInfo(
                    key = groups.groupKey(address),
                    objectKey =
                        if (HasObjectKeyFlag in flags) {
                            slots[slotAddress + objectKeySlotIndex(flags)]
                        } else null,
                    handle = makeGroupHandle(predecessor, address),
                    nodes = groupFlagsNodeCount(flags),
                    index = index++,
                )
            )
            predecessor = address
        }
        return result
    }

    fun addFlag(groupAddress: GroupAddress = parentGroup, flags: GroupFlags) {
        val propagatingFlags = propagatingFlagsOf(flags)
        val groups = addressSpace.groups
        table.traverseGroupAndParents(groupAddress) { address ->
            val currentFlags = groups.groupFlags(address)
            val flagsToSet = if (address == groupAddress) flags else propagatingFlags

            if (flagsToSet in currentFlags) return
            groups.groupFlags(address, currentFlags or flagsToSet)
        }
    }

    fun removeFlag(flags: GroupFlags) {
        removeFlag(parent, flags)
    }

    fun removeFlag(group: GroupAddress, flags: GroupFlags) {
        val groups = addressSpace.groups
        val currentFlags = groups.groupFlags(group)
        if (flags !in currentFlags) return
        val newFlags = currentFlags and flags.inv()
        groups.groupFlags(group, newFlags)
        val propagatingFlags = propagatingFlagsOf(flags)
        if (newFlags and propagatingFlags != 0) return
        val checkFlags = propagatingFlags or flags
        run {
            addressSpace.traverseParents(group) { groupAddress ->
                val flags = groups.groupFlags(groupAddress)
                if (flags and propagatingFlags == 0) return@run
                addressSpace.traverseChildren(groupAddress) { child ->
                    if (checkFlags and groups.groupFlags(child) != 0) {
                        return@run
                    }
                }
                groups.groupFlags(groupAddress, flags and propagatingFlags.inv())
            }
        }
    }

    inline fun traverseGroupPartially(
        start: GroupAddress,
        includeSiblingsOfStartGroup: Boolean = false,
        visit: (group: GroupAddress) -> Boolean,
    ) = addressSpace.traverseGroupPartially(start, includeSiblingsOfStartGroup, visit)

    inline fun traverseChildrenConditionally(
        group: GroupAddress,
        enter: (group: GroupAddress) -> Boolean,
        block: (group: GroupAddress) -> Boolean,
        exit: (group: GroupAddress) -> Unit,
        skip: (group: GroupAddress) -> Unit,
    ) {
        var current = firstChildOf(group)
        while (current != NULL_ADDRESS) {
            val used = block(current)
            val firstChild = firstChildOf(current)
            if (!used && firstChild != NULL_ADDRESS && enter(current)) {
                current = firstChild
            } else {
                if (firstChild == NULL_ADDRESS && !used) skip(current)
                var next = nextSiblingOf(current)
                while (next == NULL_ADDRESS) {
                    current = parentOf(current)
                    if (current == NULL_ADDRESS || current == group) return
                    exit(current)
                    next = nextSiblingOf(current)
                    if (next == NULL_ADDRESS) continue
                }
                current = next
            }
        }
    }

    inline fun traverseChildrenByHandle(group: GroupAddress, block: (handle: GroupHandle) -> Unit) {
        var current = makeGroupHandle(NULL_ADDRESS, firstChildOf(group))
        while (current.group != NULL_ADDRESS) {
            block(current)
            current = makeGroupHandle(current.group, nextSiblingOf(current.group))
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun upToDateSlots(): Array<Any?> {
        if (emptyCount > 0) {
            // If the emptyCount is > 0 then a builder is active (that composer is building the
            // insert table. If a read of the slot table is performed in this state the slots array
            // may have moved. This ensures it is kept up-to-date.
            slots = addressSpace.slots
        }
        return slots
    }
}

internal fun SlotTableReader.buildTrace(): List<ComposeStackTraceFrame> {
    if (!isClosed && !isEmpty) {
        return table.addressSpace.buildTrace(parentGroup, slotIndex, ReaderTraceBuilder(this))
    }
    return emptyList()
}

internal fun SlotTableReader.traceForGroup(
    group: Int,
    child: Any?, /* Anchor | Int | null */
): List<ComposeStackTraceFrame> {
    val reader = this
    val traceBuilder = ReaderTraceBuilder(reader)
    val addressSpace = reader.table.addressSpace
    var childAnchor: Any? = child
    addressSpace.traverseGroupAndParents(group) { currentGroup ->
        traceBuilder.processEdge(
            groupKey = groupKey(currentGroup),
            objectKey = groupObjectKey(currentGroup),
            sourceInformation = addressSpace.sourceInformationOf(currentGroup),
            childData = childAnchor,
        )
        childAnchor = addressSpace.anchorOfAddress(currentGroup)
    }
    return traceBuilder.trace()
}

internal class ReaderTraceBuilder(private val reader: SlotTableReader) :
    ComposeStackTraceBuilder() {
    override fun sourceInformationOf(anchor: Anchor): GroupSourceInformation? =
        reader.table.addressSpace.sourceInformationOf(anchor.asLinkAnchor().address)

    override fun groupKeyOf(anchor: Anchor): Int = reader.groupKey(anchor.asLinkAnchor().address)
}
