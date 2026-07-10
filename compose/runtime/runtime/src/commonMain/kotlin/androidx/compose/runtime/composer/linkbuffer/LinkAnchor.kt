/*
 * Copyright 2025 The Android Open Source Project
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
import androidx.compose.runtime.composeRuntimeError

/**
 * An [LinkAnchor] tracks a group address as it moves between address spaces. If the group the
 * [LinkAnchor] is tracking is removed, directly or indirectly, [valid] will return false.
 *
 * A group can move between address spaces when the group moves using in movable content. An anchor
 * can also be requested for a group by the tooling API to uniquely identify the group within a slot
 * table and allow navigating to the group.
 */
internal class LinkAnchor(var address: GroupAddress) : Anchor {
    override val valid
        get() = address != NULL_ADDRESS

    override fun toString(): String {
        return "${super.toString()}{ address: $address }"
    }
}

internal fun Anchor.asLinkAnchor() =
    this as? LinkAnchor ?: composeRuntimeError("Inconsistent composition")

/** A placeholder anchor for NULL_ADDRESS. */
internal val NullAnchor = LinkAnchor(NULL_ADDRESS)

/** Å placeholder anchor for LAZY_ADDRESS */
internal val LazyAnchor = LinkAnchor(LAZY_ADDRESS)

internal class AnchorHandle(
    private val groupAnchor: LinkAnchor,
    private val contextAnchor: LinkAnchor,
) {
    fun toHandle() = makeGroupHandle(contextAnchor.address, groupAnchor.address)

    override fun toString(): String =
        "${super.toString()}: ${groupAnchor.address}:${contextAnchor.address}"

    internal fun ownedBy(addressSpace: SlotTableAddressSpace) =
        ownedBy(addressSpace, groupAnchor) && ownedBy(addressSpace, contextAnchor)

    private fun ownedBy(addressSpace: SlotTableAddressSpace, anchor: LinkAnchor) =
        when (anchor) {
            NullAnchor,
            LazyAnchor -> true
            else -> addressSpace.ownsAnchor(anchor)
        }
}

internal fun SlotTableAddressSpace.anchorHandle(handle: GroupHandle): AnchorHandle {
    fun anchorOf(address: GroupAddress) =
        when (address) {
            NULL_ADDRESS -> NullAnchor
            LAZY_ADDRESS -> LazyAnchor
            else -> this.anchorOfAddress(address)
        }
    return AnchorHandle(anchorOf(handle.group), anchorOf(handle.context))
}

internal fun SlotTableAddressSpace.ownsHandle(anchorHandle: AnchorHandle) =
    anchorHandle.ownedBy(this)
