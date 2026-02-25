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

import androidx.collection.IntList
import androidx.collection.mutableIntIntMapOf
import androidx.collection.mutableIntListOf
import androidx.collection.mutableIntObjectMapOf
import androidx.collection.mutableLongListOf
import androidx.collection.mutableLongObjectMapOf
import androidx.collection.mutableLongSetOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val count = 100

class SlotTableEditorTests {
    @Test
    fun canEdit() {
        val table = SlotTable.build { group(100) }
        table.verifyWellFormed()
        assertFalse(table.hasEditor)
        table.edit { assertTrue(table.hasEditor) }
        assertFalse(table.hasEditor)
    }

    @Test
    fun canInsert() {
        val table = SlotTable.build { group(100) { repeat(count) { group(200) } } }
        table.verifyWellFormed()
        val handles = mutableListOf<GroupHandle>()
        val insertTable =
            table.buildSubTable { repeat(count) { group(300) { handles.add(parentHandle) } } }

        table.edit {
            group { repeat(count) { group { insertGroupFrom(insertTable, handles[it]) } } }
        }
        table.verifyWellFormed()

        table.read { group(100) { repeat(count) { group(200) { group(300) } } } }

        // insertTable.close()
        table.dispose()
    }

    @Test
    fun canRemoveMany() {
        val table = SlotTable.build { group(100) { repeat(count) { group(200) { group(300) } } } }
        table.verifyWellFormed()
        table.read { group(100) { repeat(count) { group(200) { group(300) } } } }
        table.edit { group { repeat(count) { group { removeGroup() } } } }
        table.verifyWellFormed()
        table.read { group(100) { repeat(count) { group(200) } } }
    }

    @Test
    fun canInsertRemoveInsert() {
        val table = SlotTable.build { group(100) { repeat(count) { group(200) } } }
        fun validateWithout() {
            table.verifyWellFormed()
            table.read { group(100) { repeat(count) { group(200) } } }
        }

        fun validateWith() {
            table.verifyWellFormed()
            table.read { group(100) { repeat(count) { group(200) { group(300) } } } }
        }

        validateWithout()

        fun insert() {
            val handles = mutableListOf<GroupHandle>()
            val insertTable =
                table.buildSubTable { repeat(count) { group(300) { handles.add(parentHandle) } } }

            table.edit {
                group { repeat(count) { group { insertGroupFrom(insertTable, handles[it]) } } }
            }
            table.verifyWellFormed()
        }

        fun remove() {
            table.edit { group { repeat(count) { group { removeGroup() } } } }
            table.verifyWellFormed()
        }

        insert()
        validateWith()
        remove()
        validateWithout()
        insert()
        validateWith()
        remove()
        validateWithout()
    }

    @Test
    fun canReorder() {
        val table =
            SlotTable.build { group(100) { repeat(count) { group(200 + it) { group(300) } } } }

        table.verifyWellFormed()
        table.read { group(100) { repeat(count) { group(200 + it) { group(300) } } } }

        val moveHandles = LongArray(count - 1)
        table.read {
            group {
                skipGroup()
                repeat(count - 1) {
                    moveHandles[it] = handle()
                    skipGroup()
                }
            }
        }
        moveHandles.reverse()

        table.edit {
            group {
                repeat(count - 1) {
                    moveGroup(moveHandles[it])
                    skipGroup()
                }
            }
        }

        table.verifyWellFormed()
        table.read { group(100) { repeat(count) { group(200 + (count - it - 1)) { group(300) } } } }
    }

    @Test
    fun testHandles() {
        val keyGroupMap = mutableIntIntMapOf()
        fun SlotTableBuilder.g(key: Int, block: () -> Unit = {}) {
            group(key) {
                keyGroupMap[key] = parentGroup
                block()
            }
        }
        val table =
            SlotTable.build {
                g(1000) {
                    g(1100) {
                        g(1110) {
                            g(1111)
                            g(1112)
                            g(1113)
                        }
                        g(1120) {
                            g(1121)
                            g(1122)
                            g(1123)
                        }
                    }
                }
                g(2000) {
                    g(2100) {
                        g(2110) {
                            g(2111)
                            g(2112)
                            g(2113)
                        }
                        g(2120) {
                            g(2121)
                            g(2122)
                            g(2123)
                        }
                    }
                }
            }
        data class ReaderState(val parent: Int, val previous: Int, val current: Int)
        val handles = mutableLongListOf()
        val states = mutableLongObjectMapOf<ReaderState>()
        fun SlotTableReader.recordState() {
            val handle = handle()
            if (handle != NULL_GROUP_HANDLE) {
                handles.add(handle)
                states[handle] = ReaderState(parentGroup, previousSibling, currentGroup)
            }
        }
        fun SlotTableReader.collectGroupState() {
            recordState()
            while (!isGroupEnd) {
                startGroup()
                collectGroupState()
                endGroup()
                recordState()
            }
        }
        table.verifyWellFormed()
        table.read { collectGroupState() }

        fun SlotTableEditor.validateGroupState(handle: GroupHandle) {
            val state = states[handle] ?: error("Unexpected handle $handle")
            seek(handle)
            assertEquals(state.parent, parentGroup, "Parent of ${handle.context}:${handle.group}")
            assertEquals(
                state.previous,
                previousSibling,
                "Previous of ${
                handle.context}:${handle.group}",
            )
            assertEquals(
                state.current,
                currentGroup,
                "Current of ${
                handle.context}:${handle.group}",
            )
        }
        table.edit { handles.forEach(::validateGroupState) }
    }

    @Test
    fun testCoherenceOfHandlesWithSkipToEndGroup() {
        val table =
            SlotTable.build {
                group(1000) {
                    group(1100) {
                        group(1110) {
                            group(1111)
                            group(1112)
                            group(1113)
                        }
                        group(1120) {
                            group(1121)
                            group(1122)
                            group(1123)
                        }
                    }
                }
                group(2000) {
                    group(2100) {
                        group(2110) {
                            group(2111)
                            group(2112)
                            group(2113)
                        }
                        group(2120) {
                            group(2121)
                            group(2122)
                            group(2123)
                        }
                    }
                }
            }
        data class ReaderState(val parent: Int, val previous: Int, val current: Int)
        var keys: IntList =
            table.read {
                val keys = mutableIntListOf()
                table.addressSpace.traverseGroup(table.root, includeSiblingsOfStartGroup = true) {
                    keys.add(this.groupKey(it))
                }
                keys
            }
        val seen = mutableLongSetOf()
        keys.forEach { key ->
            val (handle, state) =
                table.read {
                    while (true) {
                        if (groupKey == key) {
                            startGroup()
                            skipToGroupEnd()
                            return@read handle() to
                                ReaderState(parentGroup, previousSibling, currentGroup)
                        }
                        if (isGroupEnd) {
                            if (parentGroup == NULL_ADDRESS) break
                            endGroup()
                        } else startGroup()
                    }
                    error("Could not find the group for key $key")
                }
            assertFalse(handle in seen, "Duplicate handle produced for  $key")
            seen.add(handle)
            table.edit {
                seek(handle)
                assertEquals(state.parent, parentGroup, "Parent incorrect for key $key")
                assertTrue(
                    state.previous == 0 || state.previous == previousSibling,
                    "Previous sibling incorrect for $key",
                )
                assertEquals(state.current, currentGroup, "Current incorrect for $key")
            }
        }
    }

    @Test
    fun canRemoveAndInsertAtTheEndOfAGroup() {
        val table =
            SlotTable.build {
                group(1000) {
                    group(1100) {
                        group(1110) {
                            group(1111)
                            group(1112)
                            group(1113)
                        }
                    }
                }
            }

        val removeHandles = mutableListOf<GroupHandle>()
        val insertHandles = mutableListOf<GroupHandle>()
        table.read {
            group(1000) {
                group(1100) {
                    group(1110) {
                        removeHandles.add(handle())
                        skipGroup()
                        insertHandles.add(handle())
                        removeHandles.add(handle())
                        skipGroup()
                        insertHandles.add(handle())
                        removeHandles.add(handle())
                        skipGroup()
                        insertHandles.add(handle())
                    }
                }
            }
        }

        table.edit {
            var group = 4111
            removeHandles.zip(insertHandles).forEach { (removeHandle, insertHandle) ->
                seek(removeHandle)
                removeGroup()
                seek(insertHandle)
                insert { group(group++) }
            }
        }

        table.verifyWellFormed()
        table.read {
            group(1000) {
                group(1100) {
                    group(1110) {
                        group(4111)
                        group(4112)
                        group(4113)
                    }
                }
            }
        }
    }

    @Test
    fun canMoveFromOneTableToAnother_sameAddressSpace() {
        // Build the source table
        var sourceHandle = NULL_GROUP_HANDLE
        val sourceTable =
            SlotTable.build {
                group(1000) {
                    group(100) {
                        sourceHandle = parentHandle
                        group(10) {
                            group(1)
                            group(2)
                            group(3)
                            group(4)
                        }
                    }
                }
            }

        // Verify the source table is what was expected.
        sourceTable.read {
            group(1000) {
                group(100) {
                    group(10) {
                        group(1)
                        group(2)
                        group(3)
                        group(4)
                    }
                }
            }
        }

        // Build the destination table
        val destinationTable = sourceTable.buildSubTable { group(2000) }

        // Validate the destination table a capture the insert location
        var destinationLocation = NULL_GROUP_HANDLE
        destinationTable.read { group(2000) { destinationLocation = handle() } }

        // Move the source group to the destination table
        destinationTable.edit {
            seek(destinationLocation)
            moveFrom(sourceTable, sourceHandle)
        }

        // Validate the group left the original table
        sourceTable.read { group(1000) }

        // Validate thr group arrived in the new table
        destinationTable.read {
            group(2000) {
                group(100) {
                    group(10) {
                        group(1)
                        group(2)
                        group(3)
                        group(4)
                    }
                }
            }
        }
    }

    @Test
    fun canMoveFromOneTableToAnother_differentAddressSpaces() {
        // Build the source table
        var sourceHandle = NULL_GROUP_HANDLE
        val sourceTable =
            SlotTable.build {
                group(1000) {
                    group(100) {
                        sourceHandle = parentHandle
                        group(10) {
                            group(1)
                            group(2)
                            group(3)
                            group(4)
                        }
                    }
                }
            }

        // Verify the source table is what was expected.
        sourceTable.read {
            group(1000) {
                group(100) {
                    group(10) {
                        group(1)
                        group(2)
                        group(3)
                        group(4)
                    }
                }
            }
        }

        // Build the destination table
        val destinationTable = SlotTable.build { group(2000) }

        // Validate the destination table a capture the insert location
        var destinationLocation = NULL_GROUP_HANDLE
        destinationTable.read { group(2000) { destinationLocation = handle() } }

        // Move the source group to the destination table
        destinationTable.edit {
            seek(destinationLocation)
            moveFrom(sourceTable, sourceHandle)
        }

        // Validate the group left the original table
        sourceTable.read { group(1000) }

        // Validate thr group arrived in the new table
        destinationTable.read {
            group(2000) {
                group(100) {
                    group(10) {
                        group(1)
                        group(2)
                        group(3)
                        group(4)
                    }
                }
            }
        }
    }

    @Test
    fun canMoveFromOneTableToAnother_differentAddressSpaces_validateAnchors() {
        // Build the source table
        var sourceHandle = NULL_GROUP_HANDLE
        val sourceTable =
            SlotTable.build {
                group(1000) {
                    group(100) {
                        sourceHandle = parentHandle
                        group(10) {
                            group(1)
                            group(2)
                            group(3)
                            group(4)
                        }
                    }
                }
            }

        val anchors = mutableIntObjectMapOf<LinkAnchor>()
        // Verify the source table is what was expected.
        sourceTable.read {
            group(1000) {
                group(100) {
                    anchors[100] = table.addressSpace.anchorOfAddress(parentGroup)
                    group(10) {
                        anchors[10] = table.addressSpace.anchorOfAddress(parentGroup)
                        group(1) { anchors[1] = table.addressSpace.anchorOfAddress(parentGroup) }
                        group(2) { anchors[2] = table.addressSpace.anchorOfAddress(parentGroup) }
                        group(3) { anchors[3] = table.addressSpace.anchorOfAddress(parentGroup) }
                        group(4) { anchors[4] = table.addressSpace.anchorOfAddress(parentGroup) }
                    }
                }
            }
        }

        // Build the destination table
        val destinationTable = SlotTable.build { group(2000) }

        // Validate the destination table a capture the insert location
        var destinationLocation = NULL_GROUP_HANDLE
        destinationTable.read { group(2000) { destinationLocation = handle() } }

        // Move the source group to the destination table
        destinationTable.edit {
            seek(destinationLocation)
            moveFrom(sourceTable, sourceHandle)
        }

        // Validate the group left the original table
        sourceTable.read { group(1000) }

        // Validate thr group arrived in the new table
        destinationTable.read {
            group(2000) {
                group(100) {
                    assertEquals(anchors[100], table.addressSpace.anchorOfAddress(parentGroup))
                    group(10) {
                        assertEquals(anchors[10], table.addressSpace.anchorOfAddress(parentGroup))
                        group(1) {
                            assertEquals(
                                anchors[1],
                                table.addressSpace.anchorOfAddress(parentGroup),
                            )
                        }
                        group(2) {
                            assertEquals(
                                anchors[2],
                                table.addressSpace.anchorOfAddress(parentGroup),
                            )
                        }
                        group(3) {
                            assertEquals(
                                anchors[3],
                                table.addressSpace.anchorOfAddress(parentGroup),
                            )
                        }
                        group(4) {
                            assertEquals(
                                anchors[4],
                                table.addressSpace.anchorOfAddress(parentGroup),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal inline fun SlotTableEditor.group(block: SlotTableEditor.() -> Unit = {}) {
    startGroup()
    block()
    endGroup()
}

internal inline fun SlotTableEditor.group(key: Int, block: SlotTableEditor.() -> Unit = {}) {
    assertEquals(key, groupKey)
    startGroup()
    block()
    endGroup()
}

internal inline fun SlotTableEditor.insert(block: SlotTableBuilder.() -> Unit) {
    val insertTable = buildInsertTable(block)
    insertGroupFrom(insertTable, insertTable.rootHandle())
    insertTable.verifyWellFormed()
}

internal inline fun SlotTableEditor.insert(
    handle: GroupHandle,
    block: SlotTableBuilder.() -> Unit,
) {
    seek(handle)
    val insertTable = buildInsertTable(block)
    insertGroupFrom(insertTable, insertTable.rootHandle())
    insertTable.verifyWellFormed()
}
