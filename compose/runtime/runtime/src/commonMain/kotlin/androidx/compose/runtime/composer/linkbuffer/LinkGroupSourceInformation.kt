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
package androidx.compose.runtime.composer.linkbuffer

import androidx.compose.runtime.composer.GroupSourceInformation
import androidx.compose.runtime.snapshots.fastAny

internal class LinkGroupSourceInformation(
    override val key: Int,
    override var sourceInformation: String?,
    override val dataStartOffset: Int,
) : GroupSourceInformation {
    override var groups: ArrayList<Any /* Anchor | GroupSourceInformation */>? = null
        private set

    override var closed = false

    override var dataEndOffset: Int = 0

    fun startGrouplessCall(key: Int, sourceInformation: String, dataOffset: Int) {
        openInformation().add(LinkGroupSourceInformation(key, sourceInformation, dataOffset))
    }

    fun endGrouplessCall(dataOffset: Int) {
        openInformation().close(dataOffset)
    }

    fun reportGroup(anchor: LinkAnchor) {
        openInformation().add(anchor)
    }

    fun addGroupAfter(predecessor: LinkAnchor?, group: LinkAnchor) {
        val groups = groups ?: ArrayList<Any>().also { groups = it }
        val index =
            if (predecessor != null) {
                groups.fastIndexOf {
                    it == predecessor ||
                        (it is LinkGroupSourceInformation && it.hasGroup(predecessor))
                }
            } else 0
        groups.add(index, group)
    }

    fun close(dataOffset: Int) {
        closed = true
        dataEndOffset = dataOffset
    }

    // Return the current open nested source information or this.
    private fun openInformation(): LinkGroupSourceInformation =
        (groups?.let { groups ->
                groups.fastLastOrNull { it is LinkGroupSourceInformation && !it.closed }
            } as LinkGroupSourceInformation?)
            ?.openInformation() ?: this

    private fun add(group: Any /* Anchor | GroupSourceInformation */) {
        val groups = groups ?: ArrayList<Any>().also { this.groups = it }
        groups.add(group)
    }

    private fun hasGroup(anchor: LinkAnchor): Boolean =
        groups?.fastAny {
            it == anchor || (it is LinkGroupSourceInformation && it.hasGroup(anchor))
        } == true

    fun removeGroup(anchor: LinkAnchor): Boolean {
        val groups = groups
        if (groups != null) {
            var index = groups.size - 1
            while (index >= 0) {
                when (val item = groups[index]) {
                    is LinkAnchor -> if (item == anchor) groups.removeAt(index)
                    is LinkGroupSourceInformation ->
                        if (!item.removeGroup(anchor)) {
                            groups.removeAt(index)
                        }
                }
                index--
            }
            if (groups.isEmpty()) {
                this.groups = null
                return false
            }
            return true
        }
        return true
    }
}

private inline fun <T> ArrayList<T>.fastLastOrNull(predicate: (T) -> Boolean): T? {
    var index = size - 1
    while (index >= 0) {
        val value = get(index)
        if (predicate(value)) return value
        index--
    }
    return null
}

private inline fun <T> ArrayList<T>.fastIndexOf(predicate: (T) -> Boolean): Int {
    var index = 0
    val size = size
    while (index < size) {
        val value = get(index)
        if (predicate(value)) return index
        index++
    }
    return -1
}
