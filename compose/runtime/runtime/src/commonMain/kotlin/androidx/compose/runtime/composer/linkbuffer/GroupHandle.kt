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
 * A GroupHandle is an extension of [GroupAddress] that refers a positioning of the
 * [SlotTableEditor] such that it is unambiguous where an insert, move or remove will occur and
 * preserves enough information to restore the parent, current a previous sibling. The [GroupHandle]
 * is encoded as a [Long].
 *
 * In essence, [GroupHandle] is a coordinate tuple of (group, groupPredecessor, groupParent).
 *
 * This type is a bit-packing of two [GroupAddress]es in the following format:
 *
 * | Bit index/range | Description       |
 * |:---------------:|-------------------|
 * |      0-31       | The target group  |
 * |      31-63      | The group context |
 *
 * The target group points to a specific group in the table (always the `group` of the coordinate
 * tuple), but may be [NULL_ADDRESS] which indicates that the handle is for the end of context's
 * child list.
 *
 * The predecessor of a group may be incorrect when 1) the predecessor was removed, 2) the
 * predecessor was moved, or a new group was inserted between the predecessor and the group or when
 * it is explicitly set to [LAZY_ADDRESS]. If the predecessor is incorrect [SlotTableEditor.seek]
 * will scan the parent group list to find the correct predecessor.
 */
internal typealias GroupHandle = Long

/**
 * A GroupHandle expressing the absence of any direction into a SlotTable. This is the semantic
 * equivalent of null.
 *
 * The numeric value is equivalent to calling `GroupHandle(NULL_ADDRESS, NULL_ADDRESS)`.
 */
internal const val NULL_GROUP_HANDLE: GroupHandle = -1

/**
 * Bit-packs two groups to form a GroupHandle. [group] is the group that is pointed directly to by
 * the handle. The group may be [NULL_ADDRESS] which represents immediately after a sequence of
 * child groups of thr group referred to by [groupContext]. This form can only be used to describe
 * an insert location which means insert at the end of [groupContext]'s children, if any, or as the
 * first child, if not.
 *
 * If the group is specified, [groupContext] is a suggested predecessor for [group]. It will be used
 * as a starting point when the group's predecessor is required, but if this suggestion is wrong
 * then a full search will eventually be done. [LAZY_ADDRESS] is a special predecessor that is not
 * [NULL_ADDRESS] but will also not match an other group's address. [LAZY_ADDRESS] address can be
 * used when 1) the predecessor is not immediately available and would have to be calculated anyway,
 * or 2) the predecessor will not be needed for this handle.
 */
internal inline fun makeGroupHandle(groupContext: GroupAddress, group: GroupAddress): GroupHandle =
    (groupContext.toLong() shl Int.SIZE_BITS) or group.toUInt().toLong()

internal val GroupHandle.context
    get() = (this ushr Int.SIZE_BITS).toInt()
internal val GroupHandle.group
    get() = toInt()

/**
 * Creates a GroupHandle to represent the given coordinates of the [parent], [group], and
 * [predecessor]. (I) A group handle either points to a group or to the end of the parent child
 * list. This is enough to uniquely identify every group as well as all the insert locations. A
 * group handle for a remove will always have a non-[NULL_ADDRESS] group and the context is the
 * predecessor at the point the handle was created. This value is not guaranteed to be accurate so
 * must be checked before the handle is used and the correct predecessor found it is not correct. If
 * the group is [NULL_ADDRESS] then the context is the parent and the handle refers to the end of
 * the parent list.
 *
 * All move and remove cases have a valid, non-[NULL_ADDRESS] group and context is a predecessor. If
 * the group is [NULL_ADDRESS] the handle is invalid to use to move or remove a group.
 *
 * (II) For inserting there are three cases,
 * 1. Inserting into an empty child list
 * 2. Inserting at the end of a child list
 * 3. Inserting in front of a sibling group
 *
 * Cases (1) and (2) are identical except for where the new group's address is stored. The child
 * list is traversed to the end (in the empty case this is trivial) and the last group in the list
 * is set to be the predecessor. The next of the new group is set to [NULL_ADDRESS] and the next
 * sibling field of the predecessor is set to the group address unless the predecessor is
 * [NULL_ADDRESS], for which the first-child field of the parent is set to the group (it is case
 * II.1).
 *
 * Case (3) is handled by validating that predecessor's next points to the group. If not, the
 * predecessor is found by scanning groups parent's child list. Inserting is done by setting the new
 * group's next-sibling field to the predecessor's current next-sibling field, and then setting the
 * predecessor's next-sibling field to point to the new group. If the predecessor is [NULL_ADDRESS]
 * then the parent's first-child field is used and updated instead.
 *
 * ## Proof that [GroupHandle] is sufficient for move and remove.
 * 1. Given in order to move or remove a group it must be a valid group.
 * 2. Given (I) above the group is the group's address if the group is valid.
 * 3. The group to update is either the predecessor in the handle or found from the group's parent
 *    by traversal. This will always result in a valid predecessor, the parent, or the root.
 * 3. Therefore, [GroupHandle] is sufficient for moves and remove.
 *
 * ## Proof that [GroupHandle] is sufficient for insert
 * 1. Given (I), cases (II.1) and (II.2) are represented by a [NULL_ADDRESS] in group and the
 *    parent's address in context. As both (II.1) and (II.2) both insert at the end of the child
 *    list of parent it is sufficient to use the same encoding for both cases.
 * 2. Given (I), case (II.3) will always have a non-[NULL_ADDRESS] for the value and the new group
 *    will be inserted before this group specified in the handle.
 * 4. The predecessor field to update can be found like it is in the remove case.
 * 3. Therefore, [GroupHandle] is sufficient for inserts.
 */
internal fun makeGroupHandle(
    parent: GroupAddress,
    predecessor: GroupAddress,
    group: GroupAddress,
): GroupHandle =
    if (group >= 0) makeGroupHandle(groupContext = predecessor, group = group)
    else makeGroupHandle(groupContext = parent, group = NULL_ADDRESS)
