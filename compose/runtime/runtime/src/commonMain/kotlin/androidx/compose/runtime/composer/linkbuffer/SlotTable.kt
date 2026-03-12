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

import androidx.collection.MutableIntSet
import androidx.collection.ObjectList
import androidx.collection.ScatterMap
import androidx.collection.mutableIntSetOf
import androidx.collection.mutableScatterMapOf
import androidx.compose.runtime.Anchor
import androidx.compose.runtime.Applier
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.InvalidationResult
import androidx.compose.runtime.MovableContentState
import androidx.compose.runtime.MovableContentStateReference
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.RecomposeScopeImpl
import androidx.compose.runtime.RecomposeScopeOwner
import androidx.compose.runtime.RememberObserverHolder
import androidx.compose.runtime.ReusableRememberObserverHolder
import androidx.compose.runtime.ScopeInvalidated
import androidx.compose.runtime.SlotStorage
import androidx.compose.runtime.composeImmediateRuntimeError
import androidx.compose.runtime.composeRuntimeError
import androidx.compose.runtime.composer.RememberManager
import androidx.compose.runtime.composer.gapbuffer.BitVector
import androidx.compose.runtime.movableContentKey
import androidx.compose.runtime.runtimeCheck
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.tooling.ObjectLocation
import kotlin.math.min

internal class SlotTable(
    var root: Int = NULL_ADDRESS,
    val addressSpace: SlotTableAddressSpace = SlotTableAddressSpace(),
    var recordSourceInformation: Boolean = false,
    var recordCallByInformation: Boolean = false,
) : SlotStorage(), CompositionData, Iterable<CompositionGroup> {
    private var currentEditor: SlotTableEditor? = null
    private var openReaders = 0

    fun rootHandle() = makeGroupHandle(NULL_ADDRESS, root)

    /**
     * An internal version that is incremented whenever an editor is created. If a reader or
     * iterator was initialized at a version that differs from the current value, a concurrent
     * modification has occurred and an exception may be thrown.
     */
    var version = 0
        private set

    val hasEditor
        get() = currentEditor != null

    override fun dispose() {
        if (root != NULL_ADDRESS) {
            addressSpace.freeGroupTree(root)
            root = NULL_ADDRESS
        }
    }

    inline fun buildSubTable(block: SlotTableBuilder.() -> Unit): SlotTable =
        build(addressSpace, block)

    inline fun <T> edit(block: SlotTableEditor.() -> T): T =
        with(openEditor()) {
            try {
                block()
            } finally {
                close()
            }
        }

    inline fun <T> read(block: SlotTableReader.() -> T): T =
        with(openReader()) {
            try {
                block()
            } finally {
                close()
            }
        }

    fun openReader(): SlotTableReader {
        runtimeCheck(!hasEditor) { "Cannot read while a writer is pending" }
        openReaders++
        return SlotTableReader(this)
    }

    fun closeReader(reader: SlotTableReader) {
        runtimeCheck(reader.table === this && openReaders > 0) { "Unexpected reader close()" }
        openReaders--
    }

    fun openEditor(): SlotTableEditor {
        runtimeCheck(!hasEditor) { "Cannot start a writer when another writer is pending" }
        runtimeCheck(openReaders <= 0) { "Cannot start a writer when a reader is pending" }
        version++
        val editor = SlotTableEditor(this)
        currentEditor = editor
        return editor
    }

    fun closeEditor(editor: SlotTableEditor) {
        runtimeCheck(currentEditor === editor) {
            "Attempted to close an editor that was not the current editor"
        }
        currentEditor = null
    }

    operator fun contains(group: GroupAddress): Boolean {
        if (group < 0 || group !in addressSpace) return false
        addressSpace.traverseParents(group) { parent -> if (parent == root) return true }
        return false
    }

    operator fun contains(anchor: LinkAnchor): Boolean =
        anchor.valid && addressSpace.ownsAnchor(anchor) && anchor.address in this

    /** @return Whether the root (or any of its siblings) is carrying the specified [flags]. */
    fun containsFlags(flags: GroupFlags): Boolean =
        if (isEmpty) false else flags in addressSpace.groups.groupFlags(root)

    fun hasRecomposeScopes(group: GroupAddress): Boolean {
        val groups = addressSpace.groups
        val slots = addressSpace.slots
        traverseGroup(group) { child ->
            val recomposeScope = slots.recomposeScopeOrNullInRegion(groups.groupSlotRange(child))
            if (recomposeScope != null) return true
        }
        return false
    }

    fun inGroup(groupAnchor: LinkAnchor, childAnchor: LinkAnchor): Boolean {
        if (!groupAnchor.valid) return false
        if (!childAnchor.valid) return false
        if (groupAnchor == childAnchor) return true
        val addressSpace = addressSpace
        if (!addressSpace.ownsAnchor(childAnchor)) return false
        if (!addressSpace.ownsAnchor(groupAnchor)) return false
        val group = groupAnchor.address
        val childGroup = childAnchor.address
        if (group !in addressSpace) return false
        if (childGroup !in addressSpace) return false
        addressSpace.traverseParents(childGroup) { parent ->
            if (parent == group) return true
            if (group <= 0) return false
        }
        return false
    }

    override fun clear(rememberManager: RememberManager) {
        edit { removeCurrentGroup(rememberManager) }
    }

    override fun deactivateAll(rememberManager: RememberManager) {
        edit { deactivateCurrentGroup(rememberManager) }
    }

    @OptIn(InternalComposeApi::class)
    override fun extractNestedStates(
        applier: Applier<*>,
        references: ObjectList<MovableContentStateReference>,
    ): ScatterMap<MovableContentStateReference, MovableContentState> {
        val result = mutableScatterMapOf<MovableContentStateReference, MovableContentState>()
        edit {
            references.forEach { reference ->
                val anchor = reference.anchor.asLinkAnchor()
                if (anchor in table) {
                    seek(anchor)
                    result[reference] =
                        extractMovableContentAtCurrent(
                            composition = reference.composition,
                            reference = reference,
                            slots = this,
                            applier = applier,
                        )
                }
            }
        }
        return result
    }

    override fun find(identityToFind: Any): CompositionGroup? =
        SlotTableGroup(this, root, version).find(identityToFind)

    override fun groupContainsAnchor(group: Int, anchor: Anchor): Boolean {
        val anchor = anchor.asLinkAnchor()
        return addressSpace.ownsAnchor(anchor) && isGroupAChildOf(anchor.address, group)
    }

    override fun inGroup(parent: Anchor, child: Anchor) =
        inGroup(parent.asLinkAnchor(), child.asLinkAnchor())

    @OptIn(InternalComposeApi::class)
    override fun disposeUnusedMovableContent(
        rememberManager: RememberManager,
        state: MovableContentState,
    ) {
        edit { removeCurrentGroup(rememberManager) }
    }

    override fun invalidateAll() = forEachSlot { (it as? RecomposeScope)?.invalidate() }

    override fun invalidateGroupsWithKey(target: Int): List<RecomposeScopeImpl>? {
        val groups = MutableIntSet()
        val scopes = mutableListOf<RecomposeScopeImpl>()
        var allScopesFound = true
        val set =
            MutableIntSet().apply {
                add(target)
                add(LIVE_EDIT_INVALID_KEY)
                addressSpace.calledByMap?.get(target)?.let { addAll(it) }
            }

        // Invalidate groups with the target key
        traverseGroup(root) { group ->
            val key = addressSpace.groups.groupKey(group)
            if (key in set) {
                if (key != LIVE_EDIT_INVALID_KEY) groups += group
                if (allScopesFound) {
                    val nearestScope = findEffectiveRecomposeScope(group)
                    if (nearestScope != null) {
                        scopes += nearestScope
                        val address = nearestScope.anchor?.asLinkAnchor()?.address
                        if (address == group) {
                            // For the group that contains the restart group then, in some
                            // cases, such as when the parameter names fo a function change,
                            // the restart lambda can be invalid if it is called. To avoid this
                            // the scope parent scope needs to be invalidated too.
                            val parent = addressSpace.groups.groupParent(group)
                            val parentScope = findEffectiveRecomposeScope(parent)
                            parentScope?.let { scopes += it }
                        }
                    } else {
                        allScopesFound = false
                        scopes.clear()
                    }
                }
            }
        }

        // Bash groups even if we could not invalidate it. This call is responsible for ensuring
        // the group is recomposed when this happens.
        edit {
            groups.forEach { group ->
                seek(makeGroupHandle(LAZY_ADDRESS, group))
                bashGroup(LIVE_EDIT_INVALID_KEY)
            }
        }

        return scopes.takeIf { allScopesFound }
    }

    override fun ownsRecomposeScope(scope: RecomposeScopeImpl): Boolean =
        scope.anchor?.let {
            val anchor = it.asLinkAnchor()
            addressSpace.ownsAnchor(anchor) && isGroupAChildOf(anchor.address, root)
        } == true

    private fun findEffectiveRecomposeScope(group: GroupAddress): RecomposeScopeImpl? {
        traverseGroupAndParents(group) { parent ->
            getRecomposeScopeOrNull(parent)?.let { recomposeScope ->
                if (recomposeScope.used && parent != group) {
                    return recomposeScope
                } else {
                    recomposeScope.forcedRecompose = true
                }
            }
        }
        return null
    }

    internal fun getRecomposeScopeOrNull(group: GroupAddress): RecomposeScopeImpl? {
        val addressSpace = addressSpace
        val groups = addressSpace.groups
        val slots = addressSpace.slots
        val slotRegion = groups.groupSlotRange(group)
        if (slotRegion == NULL_ADDRESS) return null
        return addressSpace.slotAddressAndSize(slotRegion) { address, size ->
            val slotEndAddress = address + size
            val recomposeScopeSlotAddress =
                address + utilitySlotsCountForFlags(groups.groupFlags(group))

            if (recomposeScopeSlotAddress <= slotEndAddress) {
                slots[recomposeScopeSlotAddress] as? RecomposeScopeImpl
            } else null
        }
    }

    internal inline fun traverseSiblings(
        group: GroupAddress,
        visit: (group: GroupAddress) -> Unit,
    ) = addressSpace.traverseSiblings(group, visit)

    internal inline fun traverseGroupAndParents(
        group: GroupAddress,
        visit: (group: GroupAddress) -> Unit,
    ) = addressSpace.traverseGroupAndParents(group, visit = visit)

    internal inline fun traverseChildren(
        group: GroupAddress,
        visit: (group: GroupAddress) -> Unit,
    ) = addressSpace.traverseChildren(group, visit)

    internal inline fun traverseGroup(
        group: GroupAddress,
        includeSiblingsOfStartGroup: Boolean = false,
        visit: (group: GroupAddress) -> Unit,
    ) = addressSpace.traverseGroup(group, includeSiblingsOfStartGroup, visit)

    internal inline fun traverseTable(visit: (group: GroupAddress) -> Unit) {
        addressSpace.traverseGroup(root, true, visit)
    }

    internal fun isGroupAChildOf(child: GroupAddress, parent: GroupAddress): Boolean {
        traverseGroupAndParents(child) { group -> if (group == parent) return true }
        return false
    }

    internal inline fun forEachSlot(action: (slot: Any?) -> Unit) {
        runtimeCheck(!hasEditor) { "Cannot read while an editor is pending" }
        traverseTable { group -> forEachGroupSlot(group) { slot, _ -> action(slot) } }
    }

    internal inline fun forEachGroupSlot(
        group: GroupAddress,
        action: (slot: Any?, index: Int) -> Unit,
    ) {
        val slotRange = groups.groupSlotRange(group)
        if (slotRange != NULL_ADDRESS) {
            addressSpace.slotAddressAndSize(slotRange) { address, size ->
                for (index in 0 until size) {
                    val value = slots[address + index]
                    // The value can be [Composer.Empty] in large slot ranges that are not fully
                    // used as they are allocated in chunks which may larger than are used by the
                    // group that allocates the range. Once an Empty slot is encountered the rest
                    // of the range is unused.
                    if (value == Composer.Empty) return@slotAddressAndSize
                    action(value, index)
                }
            }
        }
    }

    fun newTableInSameAddressSpace(): SlotTable =
        SlotTable(
            addressSpace = addressSpace,
            recordSourceInformation = recordSourceInformation,
            recordCallByInformation = recordCallByInformation,
        )

    override fun collectSourceInformation() {
        recordSourceInformation = true
    }

    override fun collectCalledByInformation() {
        recordCallByInformation = true
    }

    override fun toDebugString(): String = toDebugString(true)

    @Suppress("unused")
    fun toDebugString(includeSlots: Boolean): String = buildString {
        append("SlotTable(")
        appendLine()

        fun dumpGroup(address: GroupAddress, indent: String) {
            append(indent)
            val groups = addressSpace.groups
            append("Group($address) key: ${groups.groupKey(address)}")
            val slots = addressSpace.slots
            val slotRange = groups.groupSlotRange(address)
            val flags = groups.groupFlags(address)
            val childNodes = groupFlagsChildNodeCount(flags)
            if (childNodes > 0) {
                append(" Nodes: ")
                append(childNodes)
            }

            if (flags and (IsMarkFlags or HasMarkFlags) != 0) {
                append(" Marks: ")
                if (IsMovableContentFlag in flags) {
                    append('C')
                }
                if (HasMovableContentFlag in flags) {
                    append('c')
                }
                if (IsSubcompositionContextFlag in flags) {
                    append('S')
                }
                if (HasSubcompositionContextFlag in flags) {
                    append('s')
                }
                if (IsRecompositionRequiredFlag in flags) {
                    append('R')
                }
                if (HasRecompositionRequiredFlag in flags) {
                    append('r')
                }
            }
            if (includeSlots) {
                var currentSlot = slotAddressOf(slotRange)
                if (slotRange != NULL_ADDRESS) {
                    val slotEnd = currentSlot + addressSpace.slotSize(slotRange)
                    if (IsNodeFlag in flags) {
                        append(" Node: ")
                        append(slots[currentSlot++].summarize(10))
                    }
                    if (HasObjectKeyFlag in flags) {
                        append(" Key: ")
                        append(slots[currentSlot++].summarize(10))
                    }
                    if (HasAuxSlotFlag in flags) {
                        append(" Aux: ")
                        append(slots[currentSlot++].summarize(10))
                    }
                    if (currentSlot < slotEnd) {
                        append(" ($currentSlot-$slotEnd)[")
                        while (currentSlot < slotEnd) {
                            append(slots[currentSlot++].summarize(10))
                            if (currentSlot < slotEnd) append(", ")
                        }
                        append("]")
                    }
                }
            }
            appendLine(':')

            val childIndent = "$indent  "
            traverseChildren(address) { childAddress -> dumpGroup(childAddress, childIndent) }
        }

        traverseSiblings(root) { dumpGroup(it, "  ") }
        append(")")
    }

    override fun verifyWellFormed() {
        addressSpace.validate()
        val groups = addressSpace.groups
        val slots = addressSpace.slots
        val groupsSeen = mutableIntSetOf()

        fun validateSlotRange(group: Int, slotRange: SlotRange) {
            if (slotRange == NULL_ADDRESS) return
            addressSpace.slotAddressAndSize(slotRange) { address, size ->
                if (address < 0 || address >= slots.size) {
                    error("Slot index for group $group out of bounds: $address")
                }
            }
        }

        fun validateGroup(parent: Int, group: Int): Int {
            if (group == NULL_ADDRESS) return 0
            if (group in groupsSeen) {
                error("Circular group encountered at $group")
            }
            groupsSeen.add(group)
            if (group % SLOT_TABLE_GROUP_SIZE != 0) {
                error("Invalid group address: $group")
            }
            if (groups.groupParent(group) != parent) {
                error("Invalid parent link in group $group")
            }
            validateSlotRange(group, groups.groupSlotRange(group))

            var nodeCount = 0
            var expectedHasFlags: GroupFlags = 0
            val groupHasFlags = groups.groupFlags(group) and HasMarkFlags
            traverseChildren(group) { currentChild ->
                nodeCount += validateGroup(group, currentChild)
                val childFlags = groups.groupFlags(currentChild)
                val propagateFlags = propagatingFlagsOf(childFlags)
                expectedHasFlags = expectedHasFlags or propagateFlags
                if (propagateFlags != 0 && groupHasFlags and propagateFlags == 0) {
                    error(
                        "Group $currentChild contains a flag that the parent, $group, " +
                            "is not recorded as having, ${propagateFlags.toString(16)} ${flagsNames(propagateFlags)}"
                    )
                }
            }
            val receivedNodeCount = groups.groupChildNodeCount(group)
            if (receivedNodeCount != nodeCount) {
                error(
                    "Unexpected node count for group $group, expected $nodeCount, received: $receivedNodeCount"
                )
            }
            if (groupHasFlags != expectedHasFlags) {
                error(
                    "Unexpected has mark flags for group $group, expected ${
                    expectedHasFlags.toString(16)
                } ${flagsNames(expectedHasFlags)}, received ${
                    groupHasFlags.toString(16)
                } ${flagsNames(groupHasFlags)}"
                )
            }
            return groups.groupNodeCount(group)
        }

        traverseSiblings(root) { validateGroup(NULL_ADDRESS, it) }
    }

    override val compositionGroups: Iterable<CompositionGroup>
        get() = this

    override val isEmpty
        get() = root == NULL_ADDRESS

    override fun iterator(): Iterator<CompositionGroup> = GroupIterator(this, root)

    internal fun nextSiblingOf(group: GroupAddress) = groups.groupNext(group)

    internal fun firstChildOf(group: GroupAddress) = groups.groupChild(group)

    internal fun groupHasAux(group: GroupAddress) = HasAuxSlotFlag in groups.groupFlags(group)

    internal fun groupSlotAtIndex(group: GroupAddress, index: Int): Any? =
        groups.groupSlotRange(group).let { range ->
            if (range == NULL_ADDRESS) return null
            if (index < 0) return null
            val slotRange = groups.groupSlotRange(group)
            return addressSpace.slotAddressAndSize(slotRange) { address, size ->
                if (index >= size) return null
                slots[address + index]
            }
        }

    internal fun groupObjectKey(group: GroupAddress): Any? {
        val flags = groups.groupFlags(group)
        if (HasObjectKeyFlag !in flags) return null
        return groupSlotAtIndex(group, objectKeySlotIndex(flags))
    }

    internal fun groupAux(group: GroupAddress): Any? {
        val flags = groups.groupFlags(group)
        if (HasAuxSlotFlag !in flags) return null
        return groupSlotAtIndex(group, auxSlotIndex(flags))
    }

    internal fun groupNode(group: GroupAddress): Any? {
        val flags = groups.groupFlags(group)
        if (IsNodeFlag !in flags) return null
        return groupSlotAtIndex(group, nodeSlotIndex(flags))
    }

    internal fun groupKeyOf(group: GroupAddress) = groups.groupKey(group)

    internal fun groupSlotRange(group: GroupAddress) = groups.groupSlotRange(group)

    internal fun groupFlags(group: GroupAddress) = groups.groupFlags(group)

    private val groups
        get() = addressSpace.groups

    private val slots
        get() = addressSpace.slots

    override fun getSlots(): Iterable<Any?> =
        object : Iterable<Any?> {
            override fun iterator() = iterator { forEachSlot { yield(it) } }
        }

    @Suppress("UNUSED") // Used during debugging
    internal fun toDebugTree() =
        sequence { traverseSiblings(root) { yield(DebugGroup(it)) } }
            .toList()
            .let { if (it.size == 1) it.first() else it }

    @Suppress("UNUSED", "MemberVisibilityCanBePrivate") // Used during debugging
    internal inner class DebugGroup(val address: GroupAddress) {
        val children
            get() = sequence { traverseChildren(address) { yield(DebugGroup(it)) } }.toList()

        val slots
            get() =
                sequence {
                        val range = slotRange
                        for (address in
                            range.address + utilitySlotsCountForFlags(flags) until range.end) {
                            yield(this@SlotTable.slots[address])
                        }
                    }
                    .toList()

        val key
            get() = groups.groupKey(address)

        val flags
            get() = groups.groupFlags(address)

        val slotRange
            get() = DebugSlotRange(groups.groupSlotRange(address))

        val objectKey
            get() =
                if (HasObjectKeyFlag in flags) {
                    this@SlotTable.slots[slotRange.address + objectKeySlotIndex(flags)]
                } else Composer.Empty

        val node
            get() =
                if (IsNodeFlag in flags) {
                    this@SlotTable.slots[slotRange.address + nodeSlotIndex(flags)]
                } else Composer.Empty

        val aux
            get() =
                if (HasAuxSlotFlag in flags) {
                    this@SlotTable.slots[slotRange.address + auxSlotIndex(flags)]
                } else Composer.Empty

        val isNode
            get() = IsNodeFlag in flags

        val isMovableContent
            get() = IsMovableContentFlag in flags

        val hasMovableContent
            get() = HasMovableContentFlag in flags

        val isSubComposition
            get() = IsRecompositionRequiredFlag in flags

        val hasSubComposition
            get() = HasSubcompositionContextFlag in flags

        val isRecomposeRequired
            get() = IsRecompositionRequiredFlag in flags

        val hasRecomposeRequired
            get() = HasRecompositionRequiredFlag in flags

        override fun toString(): String = buildString {
            append("Group(")
            append(key)
            if (flags and (HasMarkFlags or IsMarkFlags) != 0) {
                append(", flags=")
                if (isMovableContent) append('C')
                if (hasMovableContent) append('c')
                if (isSubComposition) append('S')
                if (hasSubComposition) append('s')
                if (isRecomposeRequired) append('R')
                if (hasRecomposeRequired) append('r')
            }
            if (HasObjectKeyFlag in flags) append(", object key")
            if (HasAuxSlotFlag in flags) append(", aux")
            if (isNode) append(", node")
            if (groups.groupChild(address) != NULL_ADDRESS) {
                var count = 0
                traverseChildren(address) { count++ }
                append(", ")
                append(count)
                if (count == 1) append(" child") else append(" children")
            }
            if (groups.groupSlotRange(address) != NULL_ADDRESS) {
                append(", ")
                append(slotRange.size)
                append(" slots")
            }
            append(')')
        }
    }

    @Suppress("UNUSED", "MemberVisibilityCanBePrivate") // Used during debugging
    internal inner class DebugSlotRange(val range: SlotRange) {
        val address
            get() = slotAddressOf(range)

        val size
            get() = addressSpace.slotSize(range)

        val end
            get() = address + size
    }

    companion object {
        inline fun build(
            addressSpace: SlotTableAddressSpace = SlotTableAddressSpace(),
            block: SlotTableBuilder.() -> Unit,
        ): SlotTable {
            val builder =
                SlotTableBuilder(
                    addressSpace,
                    recordSourceInformation = false,
                    recordCallByInformation = false,
                )
            builder.buildStart()
            builder.block()
            return builder.build()
        }
    }
}

private fun flagsNames(flags: GroupFlags): String {
    var result = ""
    if (IsNodeFlag in flags) result += "N"
    if (HasObjectKeyFlag in flags) result += "O"
    if (HasAuxSlotFlag in flags) result += "A"
    if (IsRecompositionRequiredFlag in flags) result += "R"
    if (HasRecompositionRequiredFlag in flags) result += "r"
    if (IsMovableContentFlag in flags) result += "C"
    if (HasMovableContentFlag in flags) result += "c"
    if (IsSubcompositionContextFlag in flags) result += "S"
    if (HasSubcompositionContextFlag in flags) result += "s"
    val childCount = groupFlagsChildNodeCount(flags)
    if (childCount != 0) {
        if (flags and GroupFlagsSpec.CHILD_NODE_COUNT_MASK.inv() != 0) result += " "
        result += "CC($childCount)"
    }
    return result
}

private class SourceInformationSlotTableGroup(
    val table: SlotTable,
    val parent: Int,
    val sourceInformation: LinkGroupSourceInformation,
    val identityPath: SourceInformationGroupPath,
) : CompositionGroup, Iterable<CompositionGroup> {
    override val key: Any = sourceInformation.key
    override val sourceInfo: String?
        get() = sourceInformation.sourceInformation

    override val node: Any?
        get() = null

    override val data: Iterable<Any?>
        get() = SourceInformationGroupDataIterator(table, parent, sourceInformation)

    override val compositionGroups: Iterable<CompositionGroup> = this
    override val identity: Any
        get() = identityPath.getIdentity(table.addressSpace)

    override val isEmpty: Boolean
        get() = sourceInformation.groups?.isEmpty() != false

    override fun iterator(): Iterator<CompositionGroup> =
        SourceInformationGroupIterator(table, parent, sourceInformation, identityPath)

    override fun equals(other: Any?): Boolean =
        other is SourceInformationSlotTableGroup &&
            // sourceInformation is intentionally omitted from this list as its value is implied
            // by parent, table and identityPath. In other words, these form a key to the
            // sourceInformation and it will never compare unequal when the others are equal.
            other.parent == parent &&
            other.table == table &&
            other.identityPath == identityPath

    override fun hashCode(): Int {
        var result = parent * 31 + table.hashCode()
        result = result * 31 + identityPath.hashCode()
        return result
    }
}

private class GroupIterator(val table: SlotTable, address: GroupAddress) :
    Iterator<CompositionGroup> {
    private var nextGroup = address
    private val version = table.version

    init {
        if (table.hasEditor) throwConcurrentModificationException()
    }

    override fun hasNext() = nextGroup != NULL_ADDRESS

    override fun next(): CompositionGroup {
        validateRead()
        val current = nextGroup
        nextGroup = table.nextSiblingOf(current)

        return SlotTableGroup(table, current, version)
    }

    private fun validateRead() {
        if (table.version != version) {
            throwConcurrentModificationException()
        }
    }
}

private class SlotTableGroup(
    val table: SlotTable,
    val group: GroupAddress,
    val version: Int = table.version,
) : CompositionGroup, Iterable<CompositionGroup> {
    override val isEmpty: Boolean
        get() = table.firstChildOf(group) == NULL_ADDRESS

    override val key: Any
        get() = table.groupObjectKey(group) ?: table.groupKeyOf(group)

    override val sourceInfo: String?
        get() =
            if (table.groupHasAux(group)) table.groupAux(group) as? String
            else table.addressSpace.sourceInformationOf(group)?.sourceInformation

    override val node: Any?
        get() = table.groupNode(group)

    override val data: Iterable<Any?>
        get() =
            table.addressSpace.sourceInformationOf(group)?.let {
                SourceInformationGroupDataIterator(table, group, it)
            } ?: DataIterator(table, group)

    override val identity: Any
        get() {
            validateRead()
            return table.addressSpace.anchorOfAddress(group)
        }

    override val compositionGroups: Iterable<CompositionGroup>
        get() = this

    override fun iterator(): Iterator<CompositionGroup> {
        validateRead()
        val sourceInformation = table.addressSpace.sourceInformationOf(group)
        return if (sourceInformation != null) {
            SourceInformationGroupIterator(
                table,
                group,
                sourceInformation,
                AnchoredGroupPath(group),
            )
        } else {
            GroupIterator(table, table.firstChildOf(group))
        }
    }

    override val groupSize: Int
        get() {
            var result = 0
            table.addressSpace.traverseGroup(group) { result++ }
            return result
        }

    override val slotsSize: Int
        get() {
            var result = 0
            table.addressSpace.traverseGroup(group) {
                val range = table.addressSpace.groups.groupSlotRange(it)
                if (range == NULL_ADDRESS) return@traverseGroup
                result += table.addressSpace.slotSize(range)
            }
            return result
        }

    private fun validateRead() {
        if (table.version != version) {
            throwConcurrentModificationException()
        }
    }

    override fun find(identityToFind: Any): CompositionGroup? {
        fun findAnchoredGroup(anchor: LinkAnchor): CompositionGroup? {
            val addressSpace = table.addressSpace
            if (addressSpace.ownsAnchor(anchor)) {
                val anchorGroup = anchor.address
                if (anchorGroup == group) return this
                addressSpace.traverseParents(anchorGroup) { parent ->
                    if (parent == group) {
                        return SlotTableGroup(table, anchorGroup, version)
                    }
                }
            }
            return null
        }

        fun findRelativeGroup(group: CompositionGroup, index: Int): CompositionGroup? =
            group.compositionGroups.drop(index).firstOrNull()

        return when (identityToFind) {
            is LinkAnchor -> findAnchoredGroup(identityToFind)
            is SourceInformationSlotTableGroupIdentity ->
                find(identityToFind.parentIdentity)?.let {
                    findRelativeGroup(it, identityToFind.index)
                }
            else -> null
        }
    }

    override fun equals(other: Any?): Boolean =
        other is SlotTableGroup &&
            other.group == group &&
            other.version == version &&
            other.table == table

    override fun hashCode() = group + 31 * table.hashCode()
}

private class SourceInformationGroupIterator(
    val table: SlotTable,
    val parent: Int,
    val group: LinkGroupSourceInformation,
    val path: SourceInformationGroupPath,
) : Iterator<CompositionGroup> {
    private val version = table.version
    private var index = 0

    override fun hasNext(): Boolean = group.groups?.let { index < it.size } ?: false

    override fun next(): CompositionGroup {
        return when (val group = group.groups?.get(index++)) {
            is LinkAnchor -> SlotTableGroup(table, group.address, version)
            is LinkGroupSourceInformation ->
                SourceInformationSlotTableGroup(
                    table = table,
                    parent = parent,
                    sourceInformation = group,
                    identityPath = RelativeGroupPath(path, index - 1),
                )
            else -> composeRuntimeError("Unexpected group information structure")
        }
    }
}

private class DataIterator(val table: SlotTable, val group: Int) : Iterable<Any?>, Iterator<Any?> {
    val end = table.addressSpace.slotSize(table.groupSlotRange(group))
    var index = utilitySlotsCountForFlags(table.groupFlags(group))

    override fun iterator(): Iterator<Any?> = this

    override fun hasNext(): Boolean = index < end

    override fun next(): Any? = table.groupSlotAtIndex(group, index++)
}

private class SourceInformationGroupDataIterator(
    val table: SlotTable,
    group: Int,
    sourceInformation: LinkGroupSourceInformation,
) : Iterable<Any?>, Iterator<Any?> {
    private val base = slotAddressOf(table.addressSpace.groups.groupSlotRange(group))
    private val start: Int = sourceInformation.dataStartOffset
    private val end: Int =
        sourceInformation.dataEndOffset.let {
            if (it > 0) it else with(table.addressSpace) { slotSize(groups.groupSlotRange(group)) }
        }

    private val filter =
        BitVector().also {
            // Filter any groups
            val groups = sourceInformation.groups ?: return@also
            groups.fastForEach { info ->
                if (info is LinkGroupSourceInformation) {
                    it.setRange(info.dataStartOffset, info.dataEndOffset)
                }
            }
        }

    private var index = filter.nextClear(start)

    override fun iterator(): Iterator<Any?> = this

    override fun hasNext() = index < end

    override fun next(): Any? =
        (if (index in 0 until end) table.addressSpace.slots[base + index] else null).also {
            index = filter.nextClear(index + 1)
        }
}

internal fun SlotStorage.asLinkBufferSlotTable() =
    this as? SlotTable ?: composeRuntimeError("Inconsistent composer")

private data class SourceInformationSlotTableGroupIdentity(val parentIdentity: Any, val index: Int)

private sealed class SourceInformationGroupPath {
    abstract fun getIdentity(addressSpace: SlotTableAddressSpace): Any
}

private class AnchoredGroupPath(val group: GroupAddress) : SourceInformationGroupPath() {
    override fun getIdentity(addressSpace: SlotTableAddressSpace): Any {
        return addressSpace.anchorOfAddress(group)
    }

    override fun equals(other: Any?): Boolean = other is AnchoredGroupPath && other.group == group

    override fun hashCode(): Int = group * 31
}

private class RelativeGroupPath(val parent: SourceInformationGroupPath, val index: Int) :
    SourceInformationGroupPath() {
    override fun getIdentity(addressSpace: SlotTableAddressSpace): Any {
        return SourceInformationSlotTableGroupIdentity(parent.getIdentity(addressSpace), index)
    }

    override fun equals(other: Any?): Boolean =
        other is RelativeGroupPath && other.parent == parent && other.index == index

    override fun hashCode(): Int = index * 31 + parent.hashCode()
}

internal fun throwConcurrentModificationException() {
    throw ConcurrentModificationException()
}

internal fun SlotTable.compositionGroupOf(group: GroupAddress): CompositionGroup {
    return SlotTableGroup(this, group, this.version)
}

internal fun SlotTableEditor.removeCurrentGroup(rememberManager: RememberManager) {
    // Notify the lifecycle manager of any observers leaving the slot table
    // The notification order should ensure that listeners are notified of leaving
    // in opposite order that they are notified of entering.

    // To ensure this order, we call `enters` as a pre-order traversal
    // of the group tree, and then call `leaves` in the inverse order.

    visitSlotsInRememberOrder(currentGroup) { _, _, slot ->
        // even that in the documentation we claim ComposeNodeLifecycleCallback should be only
        // implemented on the nodes we do not really enforce it here as doing so will be expensive.
        if (slot is ComposeNodeLifecycleCallback) {
            rememberManager.releasing(slot)
        }
        if (slot is RememberObserverHolder) {
            rememberManager.forgetting(slot)
        }
        if (slot is RecomposeScopeImpl) {
            slot.release()
        }

        false // Don't remove now as all of the slots will be removed in removeGroup() below.
    }

    removeGroup()
}

internal fun SlotTableEditor.deactivateCurrentGroup(rememberManager: RememberManager) {
    // Notify the lifecycle manager of any observers leaving the slot table
    // The notification order should ensure that listeners are notified of leaving
    // in opposite order that they are notified of entering.

    // To ensure this order, we call `enters` as a pre-order traversal
    // of the group tree, and then call `leaves` in the inverse order.
    visitSlotsInRememberOrder(currentGroup) { _, _, data ->
        when (data) {
            is ComposeNodeLifecycleCallback -> {
                rememberManager.deactivating(data)
                false
            }
            is ReusableRememberObserverHolder -> {
                // do nothing, the value should be preserved on reuse
                false
            }
            is RememberObserverHolder -> {
                rememberManager.forgetting(data)
                true
            }
            is RecomposeScopeImpl -> {
                data.release()
                true
            }
            else -> false
        }
    }
}

/**
 * The node index of a group is the index of the first node in the group is in its container. This
 * can be calculated by adding number of nodes contributed by all the siblings before this group and
 * all of the nodes contributed by sibling of any ancestor up to the first node group.
 */
internal fun nodeIndexOf(groupAddress: GroupAddress, table: SlotTable): Int {
    var current = groupAddress
    var nodeIndex = 0
    val addressSpace = table.addressSpace
    val groups = addressSpace.groups
    while (current > 0) {
        val parent = groups.groupParent(current)
        run {
            addressSpace.traverseChildren(parent) {
                if (it == current) return@run
                nodeIndex += groups.groupNodeCount(current)
            }
        }
        if (IsNodeFlag in groups.groupFlags(parent)) break
        current = parent
    }
    return nodeIndex
}

/**
 * Extract the state of movable content from the given writer. A new slot table is created and the
 * content is removed from [slots] (leaving a movable content group that, if composed over, will
 * create new content) and added to this new slot table. The invalidations that occur to recompose
 * scopes in the movable content state will be collected and forwarded to the new if the state is
 * used.
 */
@OptIn(InternalComposeApi::class)
private fun extractMovableContentAtCurrent(
    composition: ControlledComposition,
    reference: MovableContentStateReference,
    slots: SlotTableEditor,
    applier: Applier<*>?,
): MovableContentState {
    // If an applier is provided then we are extracting a state from the middle of an
    // already extracted state. If the group has nodes then the nodes need to be removed
    // from their parent so they can potentially be inserted into a destination.
    val currentGroup = slots.currentGroup
    if (applier != null && slots.nodeCountOf(currentGroup) > 0) {
        @Suppress("UNCHECKED_CAST")
        applier as Applier<Any?>

        // Find the parent node by going up until the first node group
        val parentNodeGroup = run {
            slots.table.addressSpace.traverseParents(slots.parentGroup) {
                if (slots.isNode(it)) return@run it
            }
            NULL_ADDRESS
        }
        // If we don't find a node group the nodes in the state have already been removed
        // as they are the nodes that were removed when the state was removed from the original
        // table.
        if (parentNodeGroup >= 0 && slots.isNode(parentNodeGroup)) {
            val node =
                slots.node(parentNodeGroup)
                    ?: composeImmediateRuntimeError("Invalid slot table structure")

            // Find the node index
            val nodeIndex = nodeIndexOf(currentGroup, slots.table)

            // Remove the nodes
            val count = slots.nodeCountOf(currentGroup)
            applier.down(node)
            applier.remove(nodeIndex, count)
            applier.up()
        }
    }

    val slotTable =
        slots.table.buildSubTable {
            startGroup(movableContentKey, reference.content)
            addFlags(flags = IsMovableContentFlag)
            append(reference.parameter)
            val contentAddress = slots.firstChildOf(reference.anchor.asLinkAnchor().address)
            moveFrom(slots, contentAddress.toGroupHandle())
            endGroup()
        }

    val state = MovableContentState(slotTable)

    var newOwner: RecomposeScopeOwner? = null

    slotTable.forEachSlot { slot ->
        if (slot is RecomposeScopeImpl) {
            val owner =
                newOwner
                    ?: object : RecomposeScopeOwner {
                            override fun invalidate(
                                scope: RecomposeScopeImpl,
                                instance: Any?,
                            ): InvalidationResult {
                                // Try sending this to the original owner first.
                                val result =
                                    (composition as? RecomposeScopeOwner)?.invalidate(
                                        scope,
                                        instance,
                                    ) ?: InvalidationResult.IGNORED

                                // If the original owner ignores this then we need to record it
                                // in the reference
                                if (result == InvalidationResult.IGNORED) {
                                    reference.invalidations +=
                                        scope to (instance ?: ScopeInvalidated)
                                    return InvalidationResult.SCHEDULED
                                }
                                return result
                            }

                            // The only reason [recomposeScopeReleased] is called is when the
                            // recompose scope is removed from the table. First, this never
                            // happens for content that is moving, and 2) even if it did the
                            // only reason we tell the composer is to clear tracking tables that
                            // contain this information which is not relevant here.
                            override fun recomposeScopeReleased(scope: RecomposeScopeImpl) {
                                // Nothing to do
                            }

                            // [recordReadOf] this is also something that would happen only
                            // during active recomposition which doesn't happened to a slot
                            // table that is moving.
                            override fun recordReadOf(value: Any) {
                                // Nothing to do
                            }
                        }
                        .also { newOwner = it }
            slot.adoptedBy(owner)
        }
    }
    return state
}

internal fun SlotTable.findLocation(filter: (value: Any?) -> Boolean): ObjectLocation? {
    read {
        traverseGroup(root) { group ->
            if (isNode(group) && filter(node(group))) {
                return ObjectLocation(group, null)
            }
            forEachGroupSlot(group) { slot, index ->
                if (filter(slot)) {
                    return ObjectLocation(group, index)
                }
            }
        }
    }

    return null
}

internal fun SlotTable.adoptScopesInGroupToNewParent(
    group: GroupAddress,
    newOwner: RecomposeScopeOwner,
) {
    val groups = addressSpace.groups
    val slots = addressSpace.slots
    traverseGroup(group) { child ->
        slots
            .recomposeScopeOrNullInRegion(slotRegion = groups.groupSlotRange(child))
            ?.adoptedBy(newOwner)
    }
}

private fun Array<Any?>.recomposeScopeOrNullInRegion(slotRegion: SlotRange): RecomposeScopeImpl? {
    if (slotRegion < 0) return null
    val slotStart = slotAddressOf(slotRegion)

    // The recompose scope is always at slot 0 of a restart group.
    return this[slotStart] as? RecomposeScopeImpl
}

private inline fun GroupAddress.toGroupHandle(): GroupHandle = makeGroupHandle(LAZY_ADDRESS, this)

private fun Any?.summarize(size: Int) =
    this as? String
        ?: this.toString()
            .replace("androidx.", "a.")
            .replace("compose.", "c.")
            .replace("runtime.", "r.")
            .replace("internal.", "ι.")
            .replace("ui.", "u.")
            .replace("foundation.", "f.")
            .replace("Modifier", "μ")
            .replace("material.", "m.")
            .replace("Function", "λ")
            .replace("OpaqueKey", "κ")
            .replace("MutableState", "σ")
            .let { it.substring(0, min(size, it.length)) }

private const val LIVE_EDIT_INVALID_KEY = -3
