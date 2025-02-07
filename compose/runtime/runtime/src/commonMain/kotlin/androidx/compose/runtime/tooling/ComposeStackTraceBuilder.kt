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
package androidx.compose.runtime.tooling

import androidx.compose.runtime.Anchor
import androidx.compose.runtime.ComposerImpl.CompositionContextHolder
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.GroupSourceInformation
import androidx.compose.runtime.SlotReader
import androidx.compose.runtime.SlotTable
import androidx.compose.runtime.SlotWriter
import androidx.compose.runtime.defaultsKey
import androidx.compose.runtime.reference
import androidx.compose.runtime.referenceKey
import androidx.compose.runtime.snapshots.fastForEach

internal class WriterTraceBuilder(private val writer: SlotWriter) : ComposeStackTraceBuilder() {
    override fun sourceInformationOf(anchor: Anchor): GroupSourceInformation? =
        writer.sourceInformationOf(writer.anchorIndex(anchor))

    override fun groupKeyOf(anchor: Anchor): Int = writer.groupKey(writer.anchorIndex(anchor))
}

internal class ReaderTraceBuilder(private val reader: SlotReader) : ComposeStackTraceBuilder() {
    override fun sourceInformationOf(anchor: Anchor): GroupSourceInformation? =
        reader.table.sourceInformationOf(reader.table.anchorIndex(anchor))

    override fun groupKeyOf(anchor: Anchor): Int = reader.groupKey(reader.table.anchorIndex(anchor))
}

internal abstract class ComposeStackTraceBuilder {
    private val trace = mutableListOf<ComposeStackTraceFrame>()

    fun trace(): List<ComposeStackTraceFrame> = trace

    private fun appendTraceFrame(groupSourceInformation: GroupSourceInformation, child: Any?) {
        val frame = extractTraceFrame(groupSourceInformation, child)
        if (frame != null) {
            trace += frame
        }
    }

    private fun extractTraceFrame(
        groupSourceInformation: GroupSourceInformation,
        targetChild: Any?
    ): ComposeStackTraceFrame? {
        val parsed = groupSourceInformation.sourceInformation?.let { parseSourceInfo(it) }
        if (parsed != null) {
            if (targetChild == null) {
                // no child specified
                return ComposeStackTraceFrame(parsed, null)
            }
            // calculate the call offset by checking source information of the children
            var callCount = 0
            val children = groupSourceInformation.groups
            if (children != null) {
                for (childIndex in children.indices) {
                    val child = children[childIndex]
                    if (child == targetChild) break
                    val sourceInfo = sourceInformationOf(child)

                    // Compiler did not generate source information for defaults before 2.1.0,
                    // which breaks call counter that does not take defaults group into account.
                    val isDefaultGroup =
                        sourceInfo != null &&
                            (sourceInfo.key == defaultsKey ||
                                (sourceInfo.key == 0 &&
                                    child is Anchor &&
                                    groupKeyOf(child) == defaultsKey))

                    // If sourceInformation is null, it means that default group does not capture
                    // source information yet.
                    if (isDefaultGroup && sourceInfo?.sourceInformation == null) {
                        // defaults group does not contain a separate source information string
                        // and should be calculated separately.
                        sourceInfo?.groups?.fastForEach {
                            if (sourceInformationOf(it)?.isCall() == true) {
                                callCount++
                            }
                        }
                    } else {
                        if (sourceInfo?.isCall() == true) {
                            callCount++
                        }
                    }
                }
            }
            return ComposeStackTraceFrame(parsed, callCount)
        }
        return null
    }

    private fun sourceInformationOf(group: Any) =
        when (group) {
            is Anchor -> sourceInformationOf(group)
            is GroupSourceInformation -> group
            else -> error("Unexpected child source info $group")
        }

    private fun GroupSourceInformation.isCall(): Boolean =
        sourceInformation?.startsWith("C") == true

    fun processEdge(
        sourceInformation: GroupSourceInformation?,
        childData: Any?, // (Anchor | Int | null)
    ) {
        if (sourceInformation != null) {
            if (childData == null) {
                appendTraceFrame(sourceInformation, null)
            } else {
                val found = findInGroupSourceInformation(sourceInformation, childData)
                if (!found && !sourceInformation.closed) {
                    // We found an incomplete group, very likely crash happened exactly
                    // at that location.
                    appendTraceFrame(sourceInformation, childData)
                }
            }
        }
    }

    private fun findInGroupSourceInformation(
        sourceInformation: GroupSourceInformation,
        target: Any // (Anchor | Int)
    ): Boolean {
        val children = sourceInformation.groups
        if (children == null) {
            if (!sourceInformation.closed) {
                // We found an incomplete group, very likely crash happened exactly
                // at that location.
                appendTraceFrame(sourceInformation, null)
                return true
            }
            // if the group is a leaf and we are searching for a data offset, check if it matches
            val slotStart = sourceInformation.dataStartOffset
            val slotEnd = sourceInformation.dataEndOffset
            if (target is Int) {
                // handle two cases, target is in slot range OR slot range is empty
                val found =
                    target in slotStart until slotEnd ||
                        (slotStart == slotEnd && slotStart == target)
                if (found) {
                    appendTraceFrame(sourceInformation, target)
                }
                return found
            }
            return false
        }
        children.fastForEach { child ->
            // find the edge that leads to target anchor
            when (child) {
                is Anchor -> {
                    // edge found, return
                    if (child == target) {
                        appendTraceFrame(sourceInformation, child)
                        return true
                    }
                }
                is GroupSourceInformation -> {
                    val found = findInGroupSourceInformation(child, target)
                    if (found) {
                        appendTraceFrame(sourceInformation, child)
                        return true
                    }
                }
                else -> error("Unexpected child source info $child")
            }
        }
        return false
    }

    abstract fun sourceInformationOf(anchor: Anchor): GroupSourceInformation?

    abstract fun groupKeyOf(anchor: Anchor): Int
}

private val EmptyIntArray = IntArray(0)

private fun parseSourceInfo(data: String): ParsedSourceInformation? {
    if (data.isEmpty()) {
        return null
    }

    var i = 0
    var functionName: String? = null
    var isCall = false
    if (data[i] == 'C') { // call
        i++
        isCall = true
        if (data[i] == 'C') { // inline call
            i++
        }
        // parse function name
        if (data[i] == '(') {
            var end = ++i
            while (data[end] != ')') end++
            functionName = data.substring(i, end)
            i = ++end
        } else {
            functionName = "<lambda>"
        }
        // skip over parameter info
        if (data[i] == 'P') {
            var end = ++i
            while (data[end] != ')') end++
            i = ++end
        }
    }

    // parse offset section ([*]<line-number>@<offset>L<length>[,])
    var callInfoEnd = i
    while (callInfoEnd < data.length && data[callInfoEnd] != ':') callInfoEnd++
    val lineInfo =
        if (i < callInfoEnd) {
            data
                .substring(i, callInfoEnd)
                .split(',')
                // points to end of previous line, so adding 1
                .map { it.substringBefore('@').substringAfter('*').toInt() + 1 }
                .toIntArray()
        } else {
            EmptyIntArray
        }

    // parse file name and package hash
    var fileName: String? = null
    i = callInfoEnd
    if (i < data.length) {
        var end = ++i
        while (data[end] != '#') end++
        fileName = data.substring(i, end)
        i = end
    }
    var packageHash: String? = null
    if (i < data.length) {
        packageHash = data.substring(++i)
    }

    return ParsedSourceInformation(
        isCall = isCall,
        functionName = functionName,
        lineNumbers = lineInfo,
        fileName = fileName,
        packageHash = packageHash,
        dataString = data
    )
}

internal fun SlotWriter.buildTrace(
    child: Any? = null,
    group: Int = currentGroup,
    parent: Int? = null
): List<ComposeStackTraceFrame> {
    val writer = this
    if (!writer.closed && writer.size != 0) {
        val traceBuilder = WriterTraceBuilder(writer)
        var currentGroup = group
        // sometimes in composition the parent is not completed, so we have to use writer.parent
        // whenever it is reasonably set.
        var parentGroup =
            parent ?: if (writer.parent < 0) writer.parent(currentGroup) else writer.parent
        var childData: Any? = child ?: writer.groupSlotIndex(currentGroup)
        while (currentGroup >= 0) {
            traceBuilder.processEdge(writer.sourceInformationOf(currentGroup), childData)
            childData = writer.anchor(currentGroup)
            currentGroup = parentGroup

            if (currentGroup >= 0) {
                parentGroup = writer.parent(currentGroup)
            }
        }
        return traceBuilder.trace()
    }
    return emptyList()
}

internal fun SlotReader.buildTrace(): List<ComposeStackTraceFrame> {
    val reader = this
    if (!reader.closed && reader.size != 0) {
        val traceBuilder = ReaderTraceBuilder(reader)
        var currentGroup = reader.parent
        var childAnchor: Any? = reader.slot
        while (currentGroup >= 0) {
            traceBuilder.processEdge(reader.table.sourceInformationOf(currentGroup), childAnchor)
            childAnchor = reader.anchor(currentGroup)
            val parentGroup = reader.parent(currentGroup)
            currentGroup = parentGroup
        }
        return traceBuilder.trace()
    }
    return emptyList()
}

internal fun SlotReader.traceForGroup(
    group: Int,
    child: Any? /* Anchor | Int | null */
): List<ComposeStackTraceFrame> {
    val reader = this
    val traceBuilder = ReaderTraceBuilder(reader)
    var currentGroup = group
    var parentGroup = reader.parent(group)
    var parentAnchor = reader.anchor(currentGroup)
    var childAnchor: Any? = child
    while (currentGroup >= 0) {
        traceBuilder.processEdge(reader.table.sourceInformationOf(currentGroup), childAnchor)
        currentGroup = parentGroup
        childAnchor = parentAnchor
        if (currentGroup >= 0) {
            parentAnchor = reader.anchor(parentGroup)
            parentGroup = reader.parent(parentGroup)
        }
    }

    return traceBuilder.trace()
}

internal fun SlotTable.findLocation(filter: (value: Any?) -> Boolean): ObjectLocation? {
    // potential optimization: indexOf in slots and binary search the group number
    read { reader ->
        var current = 0
        while (current < groupsSize) {
            if (reader.isNode(current) && filter(reader.node(current))) {
                return ObjectLocation(current, null)
            }

            repeat(reader.slotSize(current)) { slotIndex ->
                val slot = reader.groupGet(current, slotIndex)
                if (filter(slot)) {
                    return ObjectLocation(current, slotIndex)
                }
            }

            current++
        }
    }

    return null
}

internal fun SlotTable.findSubcompositionContextGroup(context: CompositionContext): Int? {
    read { reader ->
        fun scanGroup(group: Int, end: Int): Int? {
            var current = group
            while (current < end) {
                val next = current + reader.groupSize(current)
                if (
                    reader.hasMark(current) &&
                        reader.groupKey(current) == referenceKey &&
                        reader.groupObjectKey(current) == reference
                ) {
                    val contextHolder = reader.groupGet(current, 0) as? CompositionContextHolder
                    if (contextHolder != null && contextHolder.ref == context) {
                        return current
                    }
                }
                if (reader.containsMark(current)) {
                    scanGroup(current + 1, next)?.let {
                        return it
                    }
                }
                current = next
            }
            return null
        }
        return scanGroup(0, reader.size)
    }
}

internal data class ObjectLocation(val group: Int, val dataOffset: Int?)
