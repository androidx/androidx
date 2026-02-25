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

/**
 * A packed bitmask stored for each group in the [SlotTable]. The bits are interpreted as follows
 * (bit 0 is the least significant bit; ranges are inclusive on both ends):
 *
 * | Bit index/range | Description                                                          |
 * |:---------------:|----------------------------------------------------------------------|
 * |      0-22       | The sum of [groupFlagsChildNodeCount] for all children of this group |
 * |       23        | The [IsNodeFlag] flag                                                |
 * |       24        | The [HasObjectKeyFlag] flag                                          |
 * |       25        | The [HasAuxSlotFlag] flag                                            |
 * |       26        | The [IsRecompositionRequiredFlag] flag                               |
 * |       27        | The [HasRecompositionRequiredFlag] flag                              |
 * |       28        | The [IsMovableContentFlag] flag                                      |
 * |       29        | The [HasMovableContentFlag] flag                                     |
 * |       30        | The [IsSubcompositionContextFlag] flag                               |
 * |       31        | The [HasSubcompositionContextFlag] flag                              |
 */
internal typealias GroupFlags = Int

/**
 * A group will have the `IsNode` flag if it has a corresponding `Node` used by the
 * [Applier][androidx.compose.runtime.Applier]. Node groups retain the instance of their associated
 * node object in the SlotTable.
 */
internal const val IsNodeFlag: GroupFlags = 1 shl (GroupFlagsSpec.FLAGS_START)

/**
 * When the `HasObjectKey` flag is present on the group, it indicates that the group has a slot that
 * contains an object key. The object key can be used to identify the group and supplements the Int
 * key that is included on all groups.
 */
internal const val HasObjectKeyFlag: GroupFlags = 1 shl (GroupFlagsSpec.FLAGS_START + 1)

/**
 * When the `HasAuxSlot` flag is present on the group, it indicates that the group has a slot
 * designated for auxiliary data. The auxiliary data slot can be set independently of the rest of a
 * group's slots.
 */
internal const val HasAuxSlotFlag: GroupFlags = 1 shl (GroupFlagsSpec.FLAGS_START + 2)

/**
 * A flag indicating that the associated group is out-of-date and should be recomposed on the next
 * frame that composition is performed.
 */
internal const val IsRecompositionRequiredFlag: GroupFlags = 1 shl (GroupFlagsSpec.FLAGS_START + 3)

/**
 * Presence flag for [IsRecompositionRequiredFlag]. When set on a group, this flag indicates that
 * there is a child of this group somewhere in the hierarchy that has the
 * [IsRecompositionRequiredFlag] flag set.
 */
internal const val HasRecompositionRequiredFlag: GroupFlags = 1 shl (GroupFlagsSpec.FLAGS_START + 4)

/**
 * A flag indicating that the associated group is a
 * [MovableContent][androidx.compose.runtime.MovableContent] instance.
 */
internal const val IsMovableContentFlag: GroupFlags = 1 shl (GroupFlagsSpec.FLAGS_START + 5)

/**
 * Presence flag for [IsMovableContentFlag]. When set on a group, this flag indicates that there is
 * a child of this group somewhere in the hierarchy that has the [IsMovableContentFlag] flag set.
 */
internal const val HasMovableContentFlag: GroupFlags = 1 shl (GroupFlagsSpec.FLAGS_START + 6)

/**
 * A flag indicating that the associated group specifies a different
 * [CompositionContext][androidx.compose.runtime.CompositionContext] that should be used at this
 * point of the tree downwards.
 */
internal const val IsSubcompositionContextFlag: GroupFlags = 1 shl (GroupFlagsSpec.FLAGS_START + 7)

/**
 * Presence flag for [IsSubcompositionContextFlag]. When set on a group, this flag indicates that
 * there is a child of this group somewhere in the hierarchy that has the
 * [IsSubcompositionContextFlag] flag set.
 */
internal const val HasSubcompositionContextFlag: GroupFlags = 1 shl (GroupFlagsSpec.FLAGS_START + 8)

internal const val HasMarkFlags =
    HasRecompositionRequiredFlag or HasMovableContentFlag or HasSubcompositionContextFlag

internal const val IsMarkFlags =
    IsRecompositionRequiredFlag or IsMovableContentFlag or IsSubcompositionContextFlag

/**
 * Returns a "translated" count of how many nodes are in this group for the sake of the applier.
 * This translation will map to the effective number of nodes that an operation affects. For
 * example, if a node group with many children is removed, the applier only needs to be notified of
 * one node removal, hence this field would contain `1`. But if a general group containing several
 * nodes is removed, the group acts as a passthrough and the applier will be notified of each node
 * separately (So the node count of the group is the sum of all its direct children's node counts).
 */
internal inline fun groupFlagsNodeCount(flags: GroupFlags): Int =
    if (IsNodeFlag in flags) 1 else groupFlagsChildNodeCount(flags)

/**
 * For a given group's flags, this function will return summed total of [groupFlagsNodeCount] for
 * all children of the group. This is usually not the count you want to use (unless updating counts
 * themselves), since this result does not count the group itself.
 *
 * @see groupFlagsNodeCount
 */
internal inline fun groupFlagsChildNodeCount(flags: GroupFlags): Int =
    flags and GroupFlagsSpec.CHILD_NODE_COUNT_MASK

/**
 * For a given group's flags produce an updated version of the group child node count with the value
 * [count] as the count of nodes.
 *
 * @see groupFlagsChildNodeCount
 */
internal inline fun groupFlagsChildNodeCount(flags: GroupFlags, count: Int): GroupFlags =
    (flags and GroupFlagsSpec.CHILD_NODE_COUNT_MASK.inv()) or count

/** When a group is deleted the child count is set to [GroupFlagsSpec.CHILD_NODE_COUNT_MASK] */
internal inline fun groupFlagsIsMarkedDeleted(flags: GroupFlags) =
    flags and GroupFlagsSpec.CHILD_NODE_COUNT_MASK == GroupFlagsSpec.CHILD_NODE_COUNT_MASK

/**
 * Given a [GroupFlags] value of [flags], this function will return a new `GroupFlagSet` value
 * containing all of the flags that must be present on all parents for the SlotTable to be valid.
 *
 * Effectively, this serves as a way to convert all the flags present on the given value into
 * presence flags. Calling this function with a [flags] value that has already been converted to
 * propagating flags will return the input (i.e. the propagating flags of a flag set that only
 * contains propagating flags is itself).
 */
internal fun propagatingFlagsOf(flags: GroupFlags): GroupFlags =
    // Retain all propagating flags...
    (flags and HasMarkFlags) or
        // ... and add the necessary propagating flags for the self-flags. (To convert a
        // self-flag to its propagating flag, we can left shift by 1 bit)
        ((flags and IsMarkFlags) shl 1)

/**
 * Returns the index of the auxiliary data slot. This value is undefined if [flags] does not contain
 * [HasAuxSlotFlag].
 *
 * @return The slot index of the auxiliary data slot for the associated group.
 */
internal inline fun auxSlotIndex(flags: GroupFlags): Int =
    (flags and (IsNodeFlag or HasObjectKeyFlag)).countOneBits()

/**
 * Returns the offset of the object key from the beginning of the slot range. This value is
 * undefined if [flags] does not [HasObjectKeyFlag].
 */
internal inline fun objectKeySlotIndex(flags: GroupFlags): Int =
    (flags and IsNodeFlag).countOneBits()

/**
 * Returns the offset of the node from the beginning of the slot range.
 *
 * Only call this if it is known that [IsNodeFlag] is in [flags]
 *
 * This is always 0 as, if a node is stored, it is always stored first.
 */
internal inline fun nodeSlotIndex(@Suppress("UNUSED") flags: GroupFlags): Int = 0

internal object GroupFlagsSpec {
    /**
     * The number of bits to reserve for flags. Currently 9 for [IsNodeFlag], [HasObjectKeyFlag],
     * [HasAuxSlotFlag], [IsRecompositionRequiredFlag], [HasRecompositionRequiredFlag],
     * [IsMovableContentFlag], [HasMovableContentFlag], [IsSubcompositionContextFlag], and
     * [HasSubcompositionContextFlag],
     */
    const val NUMBER_OF_FLAGS = 9

    /** The least significant bit in a [GroupFlags] instance that is allocated to track flags. */
    const val FLAGS_START = Int.SIZE_BITS - NUMBER_OF_FLAGS

    /**
     * The number of bits allocated to track the value of [groupFlagsChildNodeCount]. This field
     * will currently get all remaining bits leftover after allocating the flag bits.
     */
    const val CHILD_NODE_COUNT_BIT_SIZE = Int.SIZE_BITS - NUMBER_OF_FLAGS
    const val CHILD_NODE_COUNT_MASK = (-1 shl CHILD_NODE_COUNT_BIT_SIZE).inv()
}

/**
 * A mask that can be used to isolate the flags used to determine if and how many of the above
 * utility slots a group has.
 */
internal const val UtilitySlotsMask: GroupFlags = IsNodeFlag or HasAuxSlotFlag or HasObjectKeyFlag

internal fun utilitySlotsCountForFlags(flags: GroupFlags): Int =
    (flags and UtilitySlotsMask).countOneBits()
