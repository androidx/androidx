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
import androidx.compose.runtime.composer.GroupSourceInformation
import androidx.compose.runtime.defaultsKey
import androidx.compose.runtime.snapshots.fastForEach

internal abstract class ComposeStackTraceBuilder {
    private val _trace = mutableListOf<ComposeStackTraceFrame>()

    fun trace(): List<ComposeStackTraceFrame> = _trace

    private fun appendTraceFrame(groupSourceInformation: GroupSourceInformation, child: Any?) {
        val frame = extractTraceFrame(groupSourceInformation, child)
        if (frame != null) {
            _trace += frame
        }
    }

    @OptIn(ComposeToolingApi::class)
    private fun extractTraceFrame(
        groupSourceInformation: GroupSourceInformation,
        targetChild: Any?,
    ): ComposeStackTraceFrame? {
        val parsed = groupSourceInformation.sourceInformation?.let { parseSourceInformation(it) }
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
                    if (isDefaultGroup && sourceInfo.sourceInformation == null) {
                        // defaults group does not contain a separate source information string
                        // and should be calculated separately.
                        sourceInfo.groups?.fastForEach {
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
                    appendTraceFrame(sourceInformation, null)
                }
            }
        }
    }

    private fun findInGroupSourceInformation(
        sourceInformation: GroupSourceInformation,
        target: Any, // (Anchor | Int)
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
                    appendTraceFrame(sourceInformation, null)
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

internal data class ObjectLocation(val group: Int, val dataOffset: Int?)
