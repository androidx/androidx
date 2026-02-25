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

import androidx.collection.mutableLongListOf
import androidx.compose.runtime.Composer
import androidx.compose.runtime.snapshots.fastForEach
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlotTableTests {
    @Test
    fun canCreate() {
        SlotTable()
    }

    @Test
    fun testIsEmpty() {
        val table = SlotTable()
        assertTrue(table.isEmpty)
        val otherTable = SlotTable.build { group(100) }
        assertFalse(otherTable.isEmpty)
    }

    @Test
    fun canInsert() {
        val slots =
            SlotTable.build {
                startGroup(37, Composer.Empty)
                append("1")
                endGroup()
            }
        slots.verifyWellFormed()
    }

    @Test
    fun testValidateSlots() {
        val slots = testSlotsNumbered()
        slots.verifyWellFormed()
        slots.read {
            startGroup()
            repeat(100) {
                assertEquals(it, groupKey)
                skipGroup()
            }
            endGroup()
        }
    }

    @Test
    fun testCreatingPrimitiveSlots() {
        val slots =
            SlotTable.build {
                group(100) {
                    repeat(100) { iteration ->
                        val groupKey = iteration + 1
                        group(groupKey) { repeat(groupKey) { append(groupKey * 1000 + it) } }
                    }
                }
            }
        slots.verifyWellFormed()
        slots.read {
            group(100) {
                repeat(100) { iteration ->
                    val groupKey = iteration + 1
                    group(groupKey) {
                        repeat(groupKey) { assertEquals(groupKey * 1000 + it, next()) }
                    }
                }
            }
        }
    }

    @Test
    fun testCreatingReferenceSlots() {
        val addressSpace = SlotTableAddressSpace()
        val slots =
            SlotTable.build(addressSpace = addressSpace) {
                group(100) {
                    repeat(100) { iteration ->
                        val groupKey = iteration + 1
                        group(groupKey) { repeat(groupKey) { append("${1000 * groupKey + it}") } }
                    }
                }
            }
        slots.verifyWellFormed()
        slots.read {
            group(100) {
                repeat(100) { iteration ->
                    val groupKey = iteration + 1
                    group(groupKey) { repeat(groupKey) { expectData("${1000 * groupKey + it}") } }
                }
            }
        }
    }

    @Test
    fun testInsertAtTheStart() {
        val slots = testSlotsNumbered()
        slots.verifyWellFormed()
        var insertHandle: GroupHandle = NULL_GROUP_HANDLE
        val insertTable = slots.buildSubTable { group(-100) { insertHandle = parentHandle } }
        slots.edit { group { insertGroupFrom(insertTable, insertHandle) } }
        slots.verifyWellFormed()
        slots.read {
            group(treeRoot) {
                group(-100)
                repeat(100) { group(it) }
            }
        }
    }

    @Test
    fun testInsertAtEnd() {
        val slots = testSlotsNumbered()
        var groupHandle: GroupHandle = NULL_GROUP_HANDLE
        val insertTable = slots.buildSubTable { group(-100) { groupHandle = parentHandle } }
        slots.edit {
            group {
                skipToGroupEnd()
                insertGroupFrom(insertTable, groupHandle)
            }
        }
        slots.verifyWellFormed()
        slots.read {
            group(treeRoot) {
                repeat(100) { group(it) }
                group(-100)
            }
        }
    }

    @Test
    fun testInsertInTheMiddle() {
        val slots = testSlotsNumbered()
        var insertHandle: GroupHandle = NULL_GROUP_HANDLE
        val insertTable = slots.buildSubTable { group(-100) { insertHandle = parentHandle } }

        slots.edit {
            startGroup()
            repeat(50) { skipGroup() }
            insertGroupFrom(insertTable, insertHandle)
        }
        slots.verifyWellFormed()
        slots.read {
            startGroup()
            repeat(50) { skipGroup() }
            assertEquals(-100, groupKey)
            skipToGroupEnd()
            endGroup()
        }
    }

    @Test
    fun testRemoveAtTheStart() {
        val slots = testSlotsNumbered()
        slots.edit {
            startGroup()
            repeat(50) { removeGroup() }
        }
        slots.verifyWellFormed()
        slots.read {
            group(treeRoot) {
                for (i in 50 until 100) {
                    group(i)
                }
            }
        }
    }

    @Test
    fun testRemoveAtTheEnd() {
        val slots = testSlotsNumbered()
        slots.edit {
            startGroup()
            repeat(50) { skipGroup() }
            repeat(50) { removeGroup() }
            endGroup()
        }
        slots.verifyWellFormed()
        slots.read { group(treeRoot) { repeat(50) { group(it) } } }
    }

    @Test
    fun testRemoveInTheMiddle() {
        val slots = testSlotsNumbered()
        slots.edit {
            startGroup()
            repeat(25) { skipGroup() }
            repeat(50) { removeGroup() }
            endGroup()
        }
        slots.verifyWellFormed()
        slots.read {
            group(treeRoot) {
                for (i in 0 until 25) {
                    group(i)
                }
                for (i in 75 until 100) {
                    group(i)
                }
            }
        }
    }

    @Test
    fun testRemoveTwoSlices() {
        val slots = testSlotsNumbered()
        slots.edit {
            startGroup()
            repeat(40) { skipGroup() }
            repeat(10) { removeGroup() }
            repeat(20) { skipGroup() }
            repeat(10) { removeGroup() }
            skipToGroupEnd()
            endGroup()
        }
        slots.read {
            startGroup()
            for (i in 0 until 40) {
                assertEquals(i, groupKey)
                skipGroup()
            }
            for (i in 50 until 70) {
                assertEquals(i, groupKey)
                skipGroup()
            }
            for (i in 80 until 100) {
                assertEquals(i, groupKey)
                skipGroup()
            }
            endGroup()
        }
    }

    // Group with slots test

    @Test
    fun testEmptyLinkedSlotTable() {
        val slots = SlotTable()
        slots.verifyWellFormed()

        slots.read { assertEquals(0, groupKey) }
    }

    @Test
    fun testTestItems() {
        val slots = testItems()
        slots.verifyWellFormed()
        validateItems(slots)
    }

    @Test
    fun testExtractKeys() {
        val slots = testItems()
        slots.verifyWellFormed()
        val expectedLocations = mutableListOf<GroupAddress>()
        val expectedNodes = mutableListOf<Int>()
        slots.read {
            startGroup()
            while (!isGroupEnd) {
                expectedLocations.add(currentGroup)
                expectedNodes.add(if (isNode) 1 else nodeCount)
                skipGroup()
            }
            endGroup()
        }
        slots.read {
            startGroup()
            val keys = extractKeys()
            assertEquals(10, keys.size)
            keys.forEachIndexed { i, keyAndLocation ->
                assertEquals(i + 1, keyAndLocation.key)
                assertEquals(i + 1, keyAndLocation.objectKey)
                assertEquals(expectedLocations[i], keyAndLocation.address)
                assertEquals(expectedNodes[i], keyAndLocation.nodes)
                assertEquals(i, keyAndLocation.index)
            }
        }
    }

    @Test
    fun testInsertAnItem() {
        val slots = testItems()
        slots.edit {
            startGroup()
            skipGroup()
            insert {
                startGroup(1000, Composer.Empty)
                append("10")
                append("20")
                endGroup()
            }
        }
        slots.verifyWellFormed()
        slots.read {
            startGroup()
            skipGroup()
            assertEquals(1000, groupKey)
            startGroup()
            assertEquals("10", next())
            assertEquals("20", next())
            endGroup()
            skipToGroupEnd()
            endGroup()
        }
    }

    @Test
    fun removeAnItem() {
        val slots = testItems()
        slots.edit {
            group {
                skipGroup()
                removeGroup()
            }
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testMoveAnItem() {
        val slots = testItems()
        slots.edit {
            group {
                skipGroup()
                moveGroup(offset = 4)
            }
        }
        slots.verifyWellFormed()
        slots.read {
            startGroup()
            expectGroup(1)
            expectGroup(6)
            expectGroup(2)
            expectGroup(3)
            expectGroup(4)
            expectGroup(5)
            expectGroup(7)
            expectGroup(8)
            expectGroup(9)
            expectGroup(10)
            endGroup()
        }
    }

    @Test
    fun testCountNodes() {
        val slots = testItems()
        slots.read {
            startGroup()
            for (i in 1..10) {
                val count = expectGroup(i)
                assertEquals(i + 1, count)
            }
            endGroup()
        }
    }

    @Test
    fun testCountNestedNodes() {
        val slots =
            SlotTable.build {
                startGroup(treeRoot, Composer.Empty)
                startGroup(0, Composer.Empty)
                repeat(10) {
                    startGroup(0, Composer.Empty)
                    repeat(3) {
                        startNode(1, 1)
                        endGroup()
                    }
                    assertEquals(3, endGroup())
                }
                assertEquals(30, endGroup())
                endGroup()
            }
        slots.verifyWellFormed()

        slots.read {
            startGroup()
            assertEquals(30, expectGroup(0))
            endGroup()
        }
    }

    @Test
    fun testUpdateNestedNodeCountOnInsert() {
        val slots =
            SlotTable.build {
                startGroup(treeRoot, Composer.Empty)
                startGroup(0, Composer.Empty)
                repeat(10) {
                    startGroup(0, Composer.Empty)
                    repeat(3) {
                        startGroup(0, Composer.Empty)
                        startNode(1, 1)
                        endGroup()
                        assertEquals(1, endGroup())
                    }
                    assertEquals(3, endGroup())
                }
                assertEquals(30, endGroup())
                endGroup()
            }
        slots.verifyWellFormed()

        val insertedHandles = mutableLongListOf()
        val insertedContent =
            slots.buildSubTable {
                repeat(2) {
                    startGroup(-100, Composer.Empty)
                    insertedHandles += parentHandle
                    startNode(1, 1)
                    endGroup()
                    assertEquals(1, endGroup())
                }
            }

        slots.edit {
            startGroup()
            startGroup()
            skipGroup()
            startGroup()
            insertedHandles.forEach { group -> insertGroupFrom(insertedContent, group) }
            skipToGroupEnd()
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testUpdateNestedNodeCountOnRemove() {
        val slots =
            SlotTable.build {
                startGroup(treeRoot, Composer.Empty)
                startGroup(0, Composer.Empty)
                repeat(10) {
                    startGroup(0, Composer.Empty)
                    repeat(3) {
                        startGroup(0, Composer.Empty)
                        startNode(1, 1)
                        endGroup()
                        assertEquals(1, endGroup())
                    }
                    assertEquals(3, endGroup())
                }
                assertEquals(30, endGroup())
                endGroup()
            }
        slots.verifyWellFormed()

        slots.edit {
            startGroup()
            startGroup()
            skipGroup()
            group {
                removeGroup()
                removeGroup()
            }
        }
        slots.verifyWellFormed()
    }

    @Test
    fun testNodesResetNodeCount() {
        val slots =
            SlotTable.build {
                startGroup(treeRoot, Composer.Empty)
                startGroup(0, Composer.Empty)
                startNode(1, 1)
                repeat(10) {
                    startNode(1, 1)
                    startGroup(0, Composer.Empty)
                    repeat(3) {
                        startNode(1, 1)
                        endGroup()
                    }
                    assertEquals(3, endGroup())
                    endGroup()
                }
                endGroup()
                assertEquals(1, endGroup())
                endGroup()
            }
        slots.verifyWellFormed()
    }

    @Test
    fun testSkipANode() {
        val slots =
            SlotTable.build {
                startGroup(treeRoot, Composer.Empty)
                startGroup(0, Composer.Empty)
                startNode(1, 1)
                repeat(10) {
                    startNode(1, 1)
                    startGroup(0, Composer.Empty)
                    repeat(3) {
                        startNode(1, 1)
                        endGroup()
                    }
                    assertEquals(3, endGroup())
                    endGroup()
                }
                endGroup()
                assertEquals(1, endGroup())
                endGroup()
            }
        slots.verifyWellFormed()

        slots.read {
            startGroup()
            startGroup()
            assertEquals(1, skipGroup())
            endGroup()
            endGroup()
        }
    }

    @Test
    fun testStartEmpty() {
        val slots = SlotTable()
        slots.read {
            beginEmpty()
            startGroup()
            assertEquals(true, inEmpty)
            assertEquals(Composer.Empty, next())
            endGroup()
            endEmpty()
        }
    }

    @Test
    fun testMoveGroup() {
        val groups = mutableListOf<GroupAddress>()
        val slots =
            SlotTable.build {
                fun item(key: Int, block: () -> Unit) {
                    startGroup(key, key)
                    block()
                    endGroup()
                }

                fun element(key: Int, block: () -> Unit) {
                    startNode(key, key)
                    block()
                    endGroup()
                }

                fun value(value: Any) {
                    append(value)
                }

                fun innerItem(i: Int) {
                    item(i) {
                        value(i)
                        value(25)
                        item(26) {
                            item(28) {
                                value(30)
                                item(31) {
                                    item(33) {
                                        item(35) {
                                            value(36)
                                            item(37) {
                                                value(39)
                                                element(40) {
                                                    value(42)
                                                    value(43)
                                                    element(44) {
                                                        value(46)
                                                        value(47)
                                                    }
                                                    element(48) {
                                                        value(50)
                                                        value(51)
                                                        value(52)
                                                        value(53)
                                                    }
                                                    element(54) {
                                                        value(56)
                                                        value(57)
                                                        value(58)
                                                        value(59)
                                                    }
                                                    element(60) {
                                                        groups.add(parentGroup)
                                                        value(62)
                                                        value(63)
                                                        value(64)
                                                        value(65)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Build a slot table to that duplicates the structure of the slot table produced
                // in the code generation test testMovement()
                item(0) {
                    item(2) {
                        item(4) {
                            item(6) {
                                value(8)
                                item(9) {
                                    item(11) {
                                        item(12) {
                                            value(14)
                                            item(15) {
                                                value(17)
                                                element(18) {
                                                    value(20)
                                                    value(21)
                                                    for (i in 1..5) {
                                                        innerItem(i)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        fun validateSlots(range: List<Int>) {
            slots.verifyWellFormed()
            slots.read {
                fun value(value: Any?) {
                    assertEquals(value, next())
                }

                fun item(key: Int, block: () -> Unit) {
                    assertEquals(groupKey, key)
                    assertEquals(groupObjectKey, key)
                    startGroup()
                    block()
                    endGroup()
                }

                fun element(key: Int, block: () -> Unit) {
                    assertEquals(groupObjectKey, key)
                    startNode()
                    block()
                    endGroup()
                }

                fun innerItem(i: Int) {
                    item(i) {
                        value(i)
                        value(25)
                        item(26) {
                            item(28) {
                                value(30)
                                item(31) {
                                    item(33) {
                                        item(35) {
                                            value(36)
                                            item(37) {
                                                value(39)
                                                element(40) {
                                                    value(42)
                                                    value(43)
                                                    element(44) {
                                                        value(46)
                                                        value(47)
                                                    }
                                                    element(48) {
                                                        value(50)
                                                        value(51)
                                                        value(52)
                                                        value(53)
                                                    }
                                                    element(54) {
                                                        value(56)
                                                        value(57)
                                                        value(58)
                                                        value(59)
                                                    }
                                                    element(60) {
                                                        value(62)
                                                        value(63)
                                                        value(64)
                                                        value(65)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(0) {
                    item(2) {
                        item(4) {
                            item(6) {
                                value(8)
                                item(9) {
                                    item(11) {
                                        item(12) {
                                            value(14)
                                            item(15) {
                                                value(17)
                                                element(18) {
                                                    value(20)
                                                    value(21)
                                                    for (i in range) {
                                                        innerItem(i)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        fun moveItem5Up() {
            slots.edit {
                repeat(9) { startGroup() }
                repeat(3) { skipGroup() }
                moveGroup(offset = 1)
            }
        }

        validateSlots((1..5).toList())
        moveItem5Up()
        validateSlots(listOf(1, 2, 3, 5, 4))

        // Validate that the groups still refer to a slot with value 62
        slots.read {
            for (address in groups) {
                assertEquals(60, groupObjectKey(address))
            }
        }
    }

    @Test
    fun testValidateSlotTableIndexes() {
        val (slots, _) = narrowTrees()
        slots.verifyWellFormed()
    }

    @Test
    fun testRemoveRandomGroup() {
        val (slots, handles) = narrowTrees()
        slots.verifyWellFormed()
        val random = Random(1000)
        val slotsToRemove = handles.shuffled(random)
        slotsToRemove.forEach { handle ->
            if (handle.group in slots) {
                slots.edit {
                    // Remove the group. The handle might not be valid, and the new predecessor
                    // might need to be searched for.
                    seek(handle)
                    removeGroup()
                }
                slots.verifyWellFormed()
            }
        }
    }

    @Test
    fun testMovingEntireTableToSiblingTable() {
        val (sourceTable, _) = narrowTrees()
        val destinationTable = sourceTable.newTableInSameAddressSpace()
        val groupsSize = sourceTable.groupsSize()
        val slotsSize = sourceTable.slotsSize()
        destinationTable.edit { moveFrom(sourceTable, sourceTable.rootHandle()) }
        sourceTable.verifyWellFormed()
        destinationTable.verifyWellFormed()
        assertEquals(0, sourceTable.groupsSize())
        assertEquals(0, sourceTable.slotsSize())
        assertEquals(groupsSize, destinationTable.groupsSize())
        assertEquals(slotsSize, destinationTable.slotsSize())
    }

    @Test
    fun testMovingOneGroup() {
        val sourceTable =
            SlotTable.build {
                startGroup(10, Composer.Empty)
                append(100)
                append(200)
                endGroup()
            }
        sourceTable.verifyWellFormed()

        val destinationTable =
            sourceTable.buildSubTable {
                startGroup(treeRoot, Composer.Empty)
                startGroup(1000, Composer.Empty)
                endGroup()
                endGroup()
            }
        destinationTable.verifyWellFormed()

        destinationTable.edit {
            startGroup()
            startGroup()
            moveFrom(sourceTable, sourceTable.rootHandle())
        }
        destinationTable.verifyWellFormed()
        destinationTable.read {
            startGroup()
            startGroup()
            assertEquals(10, groupKey)
        }
        sourceTable.verifyWellFormed()
        assertTrue(sourceTable.isEmpty, "Expected the sourceTable to end as an empty SlotTable")
    }

    @Test
    fun testMovingANodeGroup() {
        val sourceTable =
            SlotTable.build {
                startNode(10, node = 10)
                append(100)
                append(200)
                append("abc")
                append("def")
                endGroup()
            }
        sourceTable.verifyWellFormed()

        val destinationTable =
            sourceTable.buildSubTable {
                startGroup(treeRoot, Composer.Empty)
                startGroup(1000, Composer.Empty)
                endGroup()
                endGroup()
            }
        destinationTable.verifyWellFormed()

        destinationTable.edit {
            startGroup()
            startGroup()
            moveFrom(sourceTable, sourceTable.rootHandle())
        }
        destinationTable.verifyWellFormed()
        destinationTable.read {
            startGroup()
            startGroup()
            assertEquals(125, groupKey)
            assertEquals(10, groupObjectKey)

            assertEquals(100, get(0))
            assertEquals(200, get(1))

            assertEquals("abc", get(2))
            assertEquals("def", get(3))
        }
        sourceTable.verifyWellFormed()
    }

    @Test
    fun testMovingMultipleRootGroups() {
        val moveCount = 5
        val sourceTable =
            SlotTable.build {
                repeat(moveCount) {
                    startGroup(10, Composer.Empty)
                    append(100)
                    append(200)
                    endGroup()
                }
            }
        sourceTable.verifyWellFormed()

        val destinationTable =
            sourceTable.buildSubTable {
                startGroup(treeRoot, Composer.Empty)
                startGroup(1000, Composer.Empty)
                endGroup()
                endGroup()
            }
        destinationTable.verifyWellFormed()

        // Move the first root of the source table to the destination table into the
        // group with key 1000
        destinationTable.edit {
            startGroup()
            startGroup()
            moveFrom(sourceTable, sourceTable.rootHandle())
        }
        destinationTable.verifyWellFormed()
        destinationTable.read {
            startGroup()
            startGroup()
            assertEquals(10, groupKey)
        }
        sourceTable.verifyWellFormed()
    }

    @Test
    fun testMovingGroups() {
        val random = Random(1116)
        val (sourceTable, sourceAnchors) = narrowTrees()
        val destinationTable =
            sourceTable.buildSubTable {
                startGroup(treeRoot, Composer.Empty)

                startGroup(1122, Composer.Empty)
                endGroup()

                endGroup()
            }

        val groupsToMove = sourceAnchors.shuffled(random)
        val movedGroups = mutableListOf<GroupAddress>()
        val sourceKeys = mutableListOf<Int>()
        val removedSrcGroups = mutableSetOf<GroupAddress>()

        groupsToMove.forEach { handle ->
            val group = handle.group
            if (group !in removedSrcGroups) {
                sourceTable.read {
                    sourceKeys += groupKey(group)

                    sourceTable.traverseGroup(group) { current -> removedSrcGroups += current }
                }

                destinationTable.edit {
                    startGroup()
                    startGroup()
                    skipToGroupEnd()
                    moveFrom(sourceTable, handle)
                    sourceTable.verifyWellFormed()
                    movedGroups += currentGroup
                }

                // Both the source and destinations should be well-formed.
                destinationTable.verifyWellFormed()
                sourceTable.verifyWellFormed()
            }
        }

        // Verify the addresses still point to the correct groups
        val movedKeys = destinationTable.read { movedGroups.map { groupKey(it) } }
        assertEquals(sourceKeys, movedKeys, "Group keys differed after moving content")
    }

    @Test
    fun testMovingFromMultiRootGroup() {
        val destinationTable = SlotTable()

        val handles = mutableListOf<GroupHandle>()
        val sourceTable =
            destinationTable.buildSubTable {
                group(10) {
                    handles += parentHandle
                    group(100) {
                        group(1000) {}
                        group(1001) {}
                        group(1002) {}
                        group(10003) {}
                    }
                }
                group(20) {
                    handles += parentHandle
                    group(200) {
                        group(2000) {}
                        group(2001) {}
                        group(2002) {}
                        group(20003) {}
                    }
                }
                group(30) {
                    handles += parentHandle
                    group(300) {
                        group(3000) {}
                        group(3001) {}
                        group(3002) {}
                        group(30003) {}
                    }
                }
            }
        sourceTable.verifyWellFormed()

        destinationTable.edit {
            for (groupAddress in handles) {
                moveFrom(sourceTable, groupAddress)
                sourceTable.verifyWellFormed()
            }
        }
        destinationTable.verifyWellFormed()
    }

    @Test
    fun testReaderParentNodes() {
        val slots = testItems()
        slots.read {
            fun testGroup(): Pair<Int, Int> {
                val isNode = isNode
                startGroup()
                var childNodes = 0
                var expectedNodes = 0
                while (!isGroupEnd) {
                    val (groupNodes, parentNodes) = testGroup()
                    childNodes += groupNodes
                    if (expectedNodes > 0) {
                        assertEquals(expectedNodes, parentNodes)
                    } else {
                        expectedNodes = parentNodes
                    }
                }
                assertEquals(expectedNodes, childNodes)
                endGroup()
                return (if (isNode) 1 else childNodes) to parentNodeCount
            }

            testGroup()
        }
    }

    @Test
    fun testReaderParent() {
        val slots = testItems()
        slots.read {
            fun testGroup(expectedParent: GroupAddress) {
                assertEquals(expectedParent, parentGroup)
                val current = currentGroup
                group {
                    while (!isGroupEnd) {
                        testGroup(current)
                    }
                }
            }
            testGroup(NULL_ADDRESS)
        }
    }

    @Test
    fun testWriterParent() {
        val slots = testItems()
        slots.edit {
            fun testGroup(expectedParent: GroupAddress) {
                assertEquals(expectedParent, parentGroup)
                val current = currentGroup
                group {
                    while (currentGroup >= 0) {
                        testGroup(current)
                    }
                }
            }
            testGroup(NULL_ADDRESS)
        }
    }

    @Test
    fun testReaderParentIndex() {
        val slots = testItems()
        slots.read {
            fun testGroup(address: GroupAddress, expectedParent: GroupAddress) {
                assertEquals(expectedParent, parentOf(address))
                var child = firstChildOf(address)
                while (child >= 0) {
                    testGroup(child, address)
                    child = nextSiblingOf(child)
                }
            }
            testGroup(slots.root, NULL_ADDRESS)
        }
    }

    @Test
    fun testEditorParentIndex() {
        val table = testItems()
        table.edit {
            fun testGroup(address: GroupAddress, expectedParent: GroupAddress) {
                assertEquals(expectedParent, parentOf(address))
                table.traverseChildren(address) { child -> testGroup(child, address) }
            }
            testGroup(table.root, NULL_ADDRESS)
        }
    }

    @Test
    fun testReaderIsNode() {
        val table = testItems()
        table.read {
            var count = 0
            fun countNodes() {
                if (isNode) {
                    count++
                    skipGroup()
                } else {
                    startGroup()
                    while (!isGroupEnd) countNodes()
                    endGroup()
                }
            }
            countNodes()

            assertEquals(count, childNodeCountOf(table.root))
        }
    }

    @Test
    fun testEditorIsNode() {
        val table = testItems()
        val expectedCount = table.read { childNodeCountOf(table.root) }

        table.edit {
            var count = 0
            fun countNodes() {
                if (isNode) {
                    count++
                    skipGroup()
                } else {
                    startGroup()
                    while (!isGroupEnd) countNodes()
                    endGroup()
                }
            }
            countNodes()

            assertEquals(expectedCount, count)
        }
    }

    @Test
    fun testReaderIsNodeIndex() {
        val table = testItems()
        table.read {
            var count = 0
            fun countNodes(address: GroupAddress) {
                if (isNode(address)) {
                    count++
                } else {
                    var current = firstChildOf(address)
                    while (current >= 0) {
                        countNodes(current)
                        current = nextSiblingOf(current)
                    }
                }
            }

            countNodes(table.root)
            assertEquals(childNodeCountOf(table.root), count)
        }
    }

    @Test
    fun testReaderNodeIndex() {
        val table = testItems()
        table.read {
            table.traverseTable { group ->
                if (isNode(group)) {
                    assertEquals(
                        expected = "node for key ${groupObjectKey(group)}",
                        actual = groupNode(group),
                    )
                }
            }
        }
    }

    @Test
    fun testEditorNodeIndex() {
        val table = testItems()
        table.edit {
            while (currentGroup >= 0) {
                startGroup()
                if (isNode) {
                    assertEquals(expected = "node for key $objectKey", actual = node)
                }
                while (isGroupEnd && parentGroup >= 0) endGroup()
            }
        }
    }

    @Test
    fun testReaderHasObjectKeyIndex() {
        val table = testItems()
        table.read {
            table.traverseTable { group ->
                if (!isNode(group) && groupObjectKey(group) != null) {
                    assertEquals(
                        groupKey(group),
                        groupObjectKey(group),
                        "Unexpected object key for group $group",
                    )
                }
            }
        }
    }

    @Test
    fun testGroupEndByIndex() {
        val table = testItems()
        table.read {
            table.traverseTable { group ->
                var expectedLastChild = firstChildOf(group)
                if (expectedLastChild < 0) {
                    expectedLastChild = group
                } else {
                    while (
                        firstChildOf(expectedLastChild) >= 0 ||
                            nextSiblingOf(expectedLastChild) >= 0
                    ) {
                        expectedLastChild =
                            nextSiblingOf(expectedLastChild).let {
                                if (it >= 0) it else firstChildOf(expectedLastChild)
                            }
                    }
                }
            }
        }
    }

    @Test
    fun testReaderGroupAux() {
        val object1 = object {}
        val object2 = object {}
        val table =
            SlotTable.build {
                startGroup(treeRoot, Composer.Empty)
                startDataGroup(key = 1, Composer.Empty, aux = object1)
                endGroup()
                startDataGroup(key = 2, objectKey = 2, aux = object2)
                endGroup()
                endGroup()
            }
        table.read {
            startGroup()
            assertEquals(object1, groupAux)
            skipGroup()
            assertEquals(object2, groupAux)
            skipGroup()
            endGroup()
        }
    }

    @Test
    fun testReaderGroupAuxByIndex() {
        val object1 = object {}
        val object2 = object {}
        var object1Index = NULL_ADDRESS
        var object2Index = NULL_ADDRESS
        val table =
            SlotTable.build {
                startGroup(treeRoot, Composer.Empty)
                startDataGroup(1, objectKey = Composer.Empty, aux = object1)
                object1Index = parentGroup
                endGroup()
                startDataGroup(2, 2, object2)
                object2Index = parentGroup
                endGroup()
                endGroup()
            }
        table.read {
            assertEquals(object1, groupAux(object1Index))
            assertEquals(object2, groupAux(object2Index))
        }
    }

    @Test
    fun testReposition() {
        val table = testItems()
        val parentsOf = mutableMapOf<GroupAddress, GroupAddress>()
        table.read {
            fun collectGroup() {
                parentsOf[currentGroup] = parentGroup
                startGroup()
                while (!isGroupEnd) {
                    collectGroup()
                }
                endGroup()
            }
            collectGroup()
        }

        table.read {
            for ((index, parent) in parentsOf) {
                reposition(index)
                assertEquals(parent, parent)
            }
        }
    }

    @Test
    fun testInsertAtTheStartOfAGroup() {
        val table = SlotTable()
        table.edit {
            insert {
                group(treeRoot) {
                    group(100) {
                        group(10) {
                            nodeGroup(5, 500)
                            nodeGroup(6, 600)
                        }
                    }
                }
            }
        }

        table.verifyWellFormed()

        // Insert a new group at the beginning of the group with key 10.
        table.edit { group { group { group { insert { nodeGroup(7, 700) } } } } }

        table.verifyWellFormed()

        table.read {
            group(treeRoot) {
                group(100) {
                    group(10) {
                        expectNode(7, 700) {}
                        expectNode(5, 500) {}
                        expectNode(6, 600) {}
                    }
                }
            }
        }
    }

    @Test
    fun testInsertInTheMiddleOfAGroup() {
        val table =
            SlotTable.build {
                group(treeRoot) {
                    group(100) {
                        group(10) {
                            nodeGroup(5, 500)
                            nodeGroup(6, 600)
                        }
                    }
                }
            }

        table.verifyWellFormed()

        table.edit {
            group {
                group {
                    group {
                        skipGroup()
                        insert { nodeGroup(7, 700) }
                    }
                }
            }
        }

        table.verifyWellFormed()

        table.read {
            group(treeRoot) {
                group(100) {
                    group(10) {
                        expectNode(5, 500) {}
                        expectNode(7, 700) {}
                        expectNode(6, 600) {}
                    }
                }
            }
        }
    }

    @Test
    fun testInsertAtTheEndOfAGroup() {
        val table =
            SlotTable.build {
                group(treeRoot) {
                    group(100) {
                        group(10) {
                            nodeGroup(5, 500)
                            nodeGroup(6, 600)
                        }
                    }
                }
            }

        table.verifyWellFormed()

        table.edit {
            group {
                group {
                    group {
                        skipToGroupEnd()
                        insert { nodeGroup(7, 700) }
                    }
                }
            }
        }

        table.verifyWellFormed()

        table.read {
            group(treeRoot) {
                group(100) {
                    group(10) {
                        expectNode(5, 500) {}
                        expectNode(6, 600) {}
                        expectNode(7, 700) {}
                    }
                }
            }
        }
    }

    @Test
    fun testUpdatingNodeWithUpdateParentNode() {
        val table =
            SlotTable.build {
                group(treeRoot) {
                    group(10) {
                        // start a node group with its node.
                        startNode(30)
                        endGroup()
                        // start another node the same way
                        startNode(40)
                        endGroup()
                    }
                }
            }

        table.edit {
            group {
                group {
                    group { updateParentNode(300) }
                    group { updateParentNode(400) }
                }
            }
        }

        table.read {
            group {
                group(10) {
                    assertEquals(30, groupObjectKey)
                    assertEquals(300, groupNode)
                    skipGroup()
                    assertEquals(40, groupObjectKey)
                    assertEquals(400, groupNode)
                    skipGroup()
                }
            }
        }
    }

    @Test
    fun testUpdatingAuxWithUpdateAux() {
        val table =
            SlotTable.build {
                group(treeRoot) {
                    group(10) {
                        startDataGroup(30, objectKey = Composer.Empty, aux = null)
                        endGroup()
                        startDataGroup(40, objectKey = Composer.Empty, aux = null)
                        endGroup()
                    }
                }
            }

        table.edit {
            group {
                group {
                    updateAux(300)
                    skipGroup()
                    updateAux(400)
                }
            }
        }

        table.read {
            group {
                group(10) {
                    assertEquals(30, groupKey)
                    assertEquals(300, groupAux)
                    skipGroup()
                    assertEquals(40, groupKey)
                    assertEquals(400, groupAux)
                    skipGroup()
                }
            }
        }
    }

    @Test
    fun testEditorSetByIndex() {
        val outerGroups = 10
        val outerGroupKeyBase = 100
        val innerGroups = 10
        val innerGroupKeyBase = 1000
        val dataCount = 5

        data class SlotInfo(val handle: GroupHandle, val address: SlotAddress, val value: Int)

        val table =
            SlotTable.build {
                group(treeRoot) {
                    repeat(outerGroups) { outerKey ->
                        group(outerKey + outerGroupKeyBase) {
                            repeat(innerGroups) { innerKey ->
                                group(innerKey + innerGroupKeyBase) {
                                    repeat(dataCount) { append(it as Any) }
                                }
                            }
                        }
                    }
                }
            }

        fun validate(dataOffset: Int = 0): List<SlotInfo> {
            val slotInfo = mutableListOf<SlotInfo>()
            table.read {
                group(treeRoot) {
                    repeat(outerGroups) { outerKey ->
                        group(outerKey + outerGroupKeyBase) {
                            repeat(innerGroups) { innerKey ->
                                group(innerKey + innerGroupKeyBase) {
                                    repeat(dataCount) {
                                        val address = nextParentSlotAddress
                                        val value = next() as Int
                                        slotInfo.add(SlotInfo(handle(), address, value))
                                        assertEquals(it + dataOffset, value)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return slotInfo
        }

        val slotInfo = validate()

        val dataOffset = 10
        for ((anchor, address, value) in slotInfo) {
            table.edit {
                seek(anchor)
                val previous = setAbsolute(address, value + dataOffset)
                assertEquals(value, previous)
            }
        }

        validate(dataOffset)
    }

    @Test
    fun testReaderSlot() {
        val groups = 10
        val items = 10
        val table =
            SlotTable.build {
                group(treeRoot) {
                    repeat(groups) { key ->
                        group(key) { repeat(items) { item -> append(item as Any) } }
                    }
                }
            }
        table.read {
            group {
                repeat(groups) { group { repeat(items) { item -> assertEquals(item, next()) } } }
            }
        }
    }

    @Test
    fun testEditorClosed() {
        val table = testItems()
        val editor = table.openEditor()
        assertFalse(editor.isClosed)
        editor.close()
        assertTrue(editor.isClosed)
    }

    @Test
    fun testMultipleRoots() {
        val anchors = mutableListOf<GroupAddress>()
        val table =
            SlotTable.build {
                repeat(10) {
                    startGroup(it + 100, Composer.Empty)
                    anchors.add(parentGroup)
                    repeat(it) { value -> append(value) }
                    repeat(it) { value ->
                        startGroup(value + 1000, Composer.Empty)
                        endGroup()
                    }
                    endGroup()
                }
            }
        table.verifyWellFormed()
    }

    @Test
    fun testCanRemoveRootGroup() {
        val table =
            SlotTable.build {
                startGroup(100, 100)
                endGroup()
                startGroup(200, 200)
                append("300")
                append("400")
                endGroup()
            }
        table.read {
            expectGroup(100, 100)
            expectGroup(200, 200) {
                assertEquals("300", next())
                assertEquals("400", next())
            }
        }
        table.edit { removeGroup() }
        table.read {
            expectGroup(200, 200) {
                assertEquals("300", next())
                assertEquals("400", next())
            }
        }
        table.edit { removeGroup() }
        assertTrue(table.isEmpty)
    }

    @Test
    fun testReplacesWithZeroSizeGroup() {
        val outerGroupCount = 10
        val outerKeyBase = 0
        val innerGroupCount = 10
        val innerKeyBase = 100
        val bottomGroupCount = 5
        val bottomKeyBase = 1000
        val replaceMod = 2
        val slots =
            SlotTable.build {
                startGroup(treeRoot, Composer.Empty)
                repeat(outerGroupCount) { outerKey ->
                    startGroup(outerKeyBase + outerKey, Composer.Empty)
                    repeat(innerGroupCount) { innerKey ->
                        startGroup(innerKeyBase + innerKey, Composer.Empty)
                        repeat(bottomGroupCount) { bottomKey ->
                            startGroup(bottomKeyBase + bottomKey, bottomKey)
                            append("Some data")
                            endGroup()
                        }
                        endGroup()
                    }
                    endGroup()
                }
                endGroup()
            }
        slots.verifyWellFormed()
        val sourceTable =
            slots.buildSubTable {
                repeat(outerGroupCount * innerGroupCount) {
                    startGroup(0, Composer.Empty)
                    endGroup()
                }
            }
        sourceTable.verifyWellFormed()
        slots.edit {
            startGroup()
            repeat(outerGroupCount) {
                startGroup()
                repeat(innerGroupCount) { innerGroupKey ->
                    if (innerGroupKey % replaceMod == 0) {
                        moveFrom(sourceTable, sourceTable.rootHandle())
                        skipGroup()
                        removeGroup()
                    } else {
                        skipGroup()
                    }
                }
                endGroup()
            }
            endGroup()
        }
        sourceTable.verifyWellFormed()
        slots.verifyWellFormed()
    }

    @Test
    fun testInsertOfZeroGroups() {
        val sourceHandles = mutableLongListOf()
        val sourceTable =
            SlotTable.build {
                startGroup(0, Composer.Empty)
                sourceHandles += parentHandle
                append("0: Some value")
                endGroup()
                startGroup(0, Composer.Empty)
                sourceHandles += parentHandle
                repeat(5) {
                    startGroup(1, Composer.Empty)
                    endGroup()
                }
                endGroup()
                startGroup(0, Composer.Empty)
                sourceHandles += parentHandle
                endGroup()
            }

        val destinationHandles = mutableLongListOf()
        val slots =
            sourceTable.buildSubTable {
                group(treeRoot) {
                    group(10) { append("10: Some data") }
                    group(100) {
                        group(500) { destinationHandles.add(parentHandle) }
                        group(1000) {
                            destinationHandles.add(parentHandle)
                            append("1000: Some data")
                        }
                        group(2000) {
                            destinationHandles.add(parentHandle)
                            append("2000: Some data")
                        }
                    }
                }
            }

        repeat(sourceHandles.size) { iteration ->
            slots.edit {
                seek(destinationHandles[iteration])
                insertGroupFrom(insertTable = sourceTable, handle = sourceHandles[iteration])
            }
        }

        slots.read {
            expectGroup(treeRoot) {
                expectGroup(10) { expectData("10: Some data") }
                expectGroup(100) {
                    expectGroup(0)
                    expectGroup(500)
                    expectGroup(0)
                    expectGroup(1000) { expectData("1000: Some data") }
                    expectGroup(0)
                    expectGroup(2000) { expectData("2000: Some data") }
                }
            }
        }
    }

    @Test
    fun testMoveOfZeroGroup() {
        val table =
            SlotTable.build {
                group(treeRoot) {
                    group(10) {
                        group(100) {
                            append("100: 1")
                            append("100: 2")
                        }
                        group(200) {
                            append("200: 1")
                            append("200: 2")
                        }
                        group(300) {
                            append("300: 1")
                            append("300: 2")
                        }
                        // Empty group
                        group(0) {}
                        group(400) {
                            append("400: 1")
                            append("400: 2")
                        }
                    }
                }
            }
        table.verifyWellFormed()
        table.read {
            expectGroup(treeRoot) {
                expectGroup(10) {
                    expectGroup(100) {
                        expectData("100: 1")
                        expectData("100: 2")
                    }
                    expectGroup(200) {
                        expectData("200: 1")
                        expectData("200: 2")
                    }
                    expectGroup(300) {
                        expectData("300: 1")
                        expectData("300: 2")
                    }
                    expectGroup(0)
                    expectGroup(400) {
                        expectData("400: 1")
                        expectData("400: 2")
                    }
                }
            }
        }
        table.edit {
            group {
                group {
                    skipGroup()
                    insert {
                        group(150) {
                            append("150: 1")
                            append("150: 2")
                        }
                    }
                    skipGroup()
                    skipGroup()
                    moveGroup(1)
                }
            }
        }
        table.verifyWellFormed()
        table.read {
            expectGroup(treeRoot) {
                expectGroup(10) {
                    expectGroup(100) {
                        expectData("100: 1")
                        expectData("100: 2")
                    }
                    expectGroup(150) {
                        expectData("150: 1")
                        expectData("150: 2")
                    }
                    expectGroup(200) {
                        expectData("200: 1")
                        expectData("200: 2")
                    }
                    expectGroup(0)
                    expectGroup(300) {
                        expectData("300: 1")
                        expectData("300: 2")
                    }
                    expectGroup(400) {
                        expectData("400: 1")
                        expectData("400: 2")
                    }
                }
            }
        }
    }

    @Test
    fun testReaderGet() {
        val table =
            SlotTable.build {
                group(treeRoot) {
                    group(10) {
                        append("10: 0")
                        append("10: 1")
                        append("10: 2")
                    }
                    group(20) {
                        append("20: 0")
                        append("20: 1")
                        append("20: 2")
                    }
                }
            }
        table.verifyWellFormed()
        table.read {
            startGroup()
            assertEquals("10: 0", get(0))
            assertEquals("10: 1", get(1))
            assertEquals("10: 2", get(2))
            skipGroup()
            assertEquals("20: 0", get(0))
            assertEquals("20: 1", get(1))
            assertEquals("20: 2", get(2))
            endGroup()
        }
    }

    @Test
    fun testRemoveDataBoundaryCondition() {
        // Remove when the slot table contains amount that would make the slotGapSize 0
        // Test insert exactly 64 data slots.
        val table =
            SlotTable.build {
                group(treeRoot) {
                    repeat(4) { count ->
                        group(count * 10 + 100) { repeat(8) { value -> append(value.toString()) } }
                    }
                    group(1000) { repeat(16) { value -> append(value.toString()) } }
                    repeat(2) { count ->
                        group(count * 10 + 200) { repeat(8) { value -> append(value.toString()) } }
                    }
                    repeat(10) { count -> group(300 + count) {} }
                }
            }
        table.verifyWellFormed()

        table.edit {
            group {
                repeat(4) { skipGroup() }
                removeGroup()
                skipGroup()
                startGroup()
                setRelative(4, "100")
            }
        }
        table.verifyWellFormed()
    }

    @Test
    fun testInsertDataBoundaryCondition() {
        // Test insert exactly 64 data slots.
        val table =
            SlotTable.build {
                group(treeRoot) {
                    group(10) {
                        group(100) { repeat(10) { item -> append(item.toString()) } }
                        group(200) { repeat(10) { item -> append(item.toString()) } }
                    }
                }
            }
        table.verifyWellFormed()

        val sourceTable =
            table.buildSubTable {
                group(150) { repeat(64) { item -> append("Inserted item $item") } }
            }
        sourceTable.verifyWellFormed()

        table.edit {
            group {
                group {
                    skipGroup()
                    moveFrom(sourceTable, sourceTable.rootHandle())
                }
            }
        }
        table.verifyWellFormed()

        table.read {
            expectGroup(treeRoot) {
                expectGroup(10) {
                    expectGroup(100) { repeat(10) { item -> expectData(item.toString()) } }
                    expectGroup(150) { repeat(64) { item -> expectData("Inserted item $item") } }
                    expectGroup(200) { repeat(10) { item -> expectData(item.toString()) } }
                }
            }
        }
    }

    @Test
    fun testGroupsBoundaryCondition() {
        // Test inserting exactly 32 groups with 2 data items each
        val table =
            SlotTable.build {
                group(treeRoot) {
                    group(10) {
                        group(100) { repeat(10) { item -> append(item.toString()) } }
                        group(200) { repeat(10) { item -> append(item.toString()) } }
                    }
                }
            }
        table.verifyWellFormed()

        val sourceTable =
            table.buildSubTable {
                group(150) {
                    repeat(2) { item -> append("Inserted item $item") }
                    repeat(31) { key ->
                        group(150 + key) { repeat(2) { item -> append("Inserted item $item") } }
                    }
                }
            }
        sourceTable.verifyWellFormed()

        table.edit {
            group {
                group {
                    skipGroup()
                    moveFrom(sourceTable, sourceTable.rootHandle())
                }
            }
        }
        table.verifyWellFormed()

        table.read {
            expectGroup(treeRoot) {
                expectGroup(10) {
                    expectGroup(100) { repeat(10) { item -> expectData(item.toString()) } }
                    expectGroup(150) {
                        repeat(2) { item -> expectData("Inserted item $item") }
                        repeat(31) { key ->
                            expectGroup(150 + key) {
                                repeat(2) { item -> expectData("Inserted item $item") }
                            }
                        }
                    }
                    expectGroup(200) { repeat(10) { item -> expectData(item.toString()) } }
                }
            }
        }
    }

    @Test
    fun canRepositionReaderPastEndOfTable() {
        var end: GroupHandle = NULL_GROUP_HANDLE
        val table =
            SlotTable.build {
                repeat(256) {
                    startGroup(0, Composer.Empty)
                    endGroup()
                }
                end = parentHandle
            }

        table.read {
            reposition(end)
            // Expect the above not to crash.
        }
    }

    @Test
    fun canRemoveFromFullTable() {
        // Create a table that is exactly 64 entries
        val table =
            SlotTable.build {
                repeat(7) { outer -> group(10 + outer) { repeat(8) { inner -> group(inner) {} } } }
                group(30) {}
            }
        table.verifyWellFormed()

        // Remove the first group
        table.edit { removeGroup() }
        table.verifyWellFormed()
    }

    @Test
    fun canInsertAuxData() {
        val slots =
            SlotTable.build(SlotTableAddressSpace()) {
                // Insert a normal aux data.
                startDataGroup(10, 10, "10")
                endGroup()

                // Insert using insertAux
                startGroup(20, Composer.Empty)
                insertAux("20")
                endGroup()

                // Insert using insertAux after a slot value was added.
                startGroup(30, Composer.Empty)
                append("300")
                insertAux("30")
                endGroup()

                // Insert using insertAux after a group with an object key
                startGroup(40, 40)
                insertAux("40")
                endGroup()

                // Insert aux into an object key with a value slot and then add another value.
                startGroup(50, 50)
                append("500")
                insertAux("50")
                append("501")
                endGroup()

                // Insert aux after two slot values and then add another value.
                startGroup(60, Composer.Empty)
                append("600")
                append("601")
                insertAux("60")
                append("602")
                endGroup()

                // Write a trail group to ensure that the slot table is valid after the
                // insertAux
                startGroup(1000, Composer.Empty)
                append("10000")
                append("10001")
                endGroup()
            }
        slots.verifyWellFormed()
        slots.read {
            assertEquals(10, groupKey)
            assertEquals(10, groupObjectKey)
            assertEquals("10", groupAux)
            skipGroup()
            assertEquals(20, groupKey)
            assertEquals("20", groupAux)
            skipGroup()
            assertEquals(30, groupKey)
            assertEquals("30", groupAux)
            startGroup()
            assertEquals("300", next())
            endGroup()
            assertEquals(40, groupKey)
            assertEquals(40, groupObjectKey)
            assertEquals("40", groupAux)
            skipGroup()
            assertEquals(50, groupKey)
            assertEquals(50, groupObjectKey)
            assertEquals("50", groupAux)
            startGroup()
            assertEquals("500", next())
            assertEquals("501", next())
            endGroup()
            assertEquals(60, groupKey)
            assertEquals("60", groupAux)
            startGroup()
            assertEquals("600", next())
            assertEquals("601", next())
            assertEquals("602", next())
            endGroup()
            assertEquals(1000, groupKey)
            startGroup()
            assertEquals("10000", next())
            assertEquals("10001", next())
            endGroup()
        }
    }

    @Test
    fun incorrectUsageReportsInternalException() =
        expectError("internal") {
            val table = SlotTable()
            table.edit { table.edit {} }
        }

    @Test
    fun canCheckAnEmptyTableForAMark() {
        val table = SlotTable()
        assertFalse(table.containsFlags(IsRecompositionRequiredFlag))
        assertFalse(table.containsFlags(HasRecompositionRequiredFlag))
        assertFalse(table.containsFlags(IsMovableContentFlag))
        assertFalse(table.containsFlags(HasMovableContentFlag))
        assertFalse(table.containsFlags(IsSubcompositionContextFlag))
    }

    @Test
    fun canMarkAGroup() {
        val table =
            SlotTable.build {
                group(0) {
                    group(1) {
                        group(2) { addFlags(flags = IsMovableContentFlag) }
                        group(3) { group(4) {} }
                    }
                    group(5) {
                        addFlags(flags = IsMovableContentFlag)
                        group(6) { addFlags(flags = IsMovableContentFlag) }
                    }
                }
            }
        table.verifyWellFormed()
        table.read {
            fun parentFlags() = flagsOf(parentGroup)
            fun assertMark() = assertTrue(IsMovableContentFlag in parentFlags())
            fun assertNoMark() = assertTrue(IsMovableContentFlag !in parentFlags())
            fun assertContainsMark() = assertTrue(HasMovableContentFlag in parentFlags())
            fun assertDoesNotContainMarks() = assertTrue(HasMovableContentFlag !in parentFlags())

            group(0) {
                assertNoMark()
                assertContainsMark()
                group(1) {
                    assertNoMark()
                    assertContainsMark()
                    group(2) {
                        assertMark()
                        assertDoesNotContainMarks()
                    }
                    group(3) {
                        assertNoMark()
                        assertDoesNotContainMarks()
                        group(4) {
                            assertNoMark()
                            assertDoesNotContainMarks()
                        }
                    }
                }
                group(5) {
                    assertMark()
                    assertContainsMark()
                    group(6) {
                        assertMark()
                        assertDoesNotContainMarks()
                    }
                }
            }
        }
    }

    @Test
    fun canRemoveAMarkedGroup() {
        val table =
            SlotTable.build {
                group(0) {
                    repeat(10) { key ->
                        group(key) {
                            if (key == 2) {
                                addFlags(flags = IsRecompositionRequiredFlag)
                            }
                        }
                    }
                }
            }
        table.verifyWellFormed()
        table.read { assertTrue(HasRecompositionRequiredFlag in flagsOf(table.root)) }

        table.edit {
            group {
                skipGroup()
                skipGroup()
                removeGroup()
            }
        }
        table.verifyWellFormed()

        table.read { assertFalse(HasRecompositionRequiredFlag in flagsOf(table.root)) }
    }

    @Test
    fun canInsertAMarkedGroup() {
        val table =
            SlotTable.build {
                group(0) { group(1) { group(2) { addFlags(flags = IsMovableContentFlag) } } }
            }
        table.verifyWellFormed()

        table.read {
            fun parentFlags() = flagsOf(parentGroup)
            fun assertMark() = assertTrue(IsMovableContentFlag in parentFlags())
            fun assertNoMark() = assertTrue(IsMovableContentFlag !in parentFlags())
            fun assertContainsMark() = assertTrue(HasMovableContentFlag in parentFlags())
            fun assertDoesNotContainMarks() = assertTrue(HasMovableContentFlag !in parentFlags())

            group(0) {
                assertNoMark()
                assertContainsMark()
                group(1) {
                    assertNoMark()
                    assertContainsMark()
                    group(2) {
                        assertMark()
                        assertDoesNotContainMarks()
                    }
                }
            }
        }
    }

    @Test
    fun canInsertAMarkedTableGroup() {
        val table = SlotTable.build { group(0) }
        table.verifyWellFormed()

        val insertTable =
            table.buildSubTable { group(1) { group(2) { addFlags(IsMovableContentFlag) } } }
        insertTable.verifyWellFormed()

        table.edit {
            group { moveFrom(sourceTable = insertTable, sourceHandle = insertTable.rootHandle()) }
        }
        table.verifyWellFormed()
        table.read { assertTrue(HasMovableContentFlag in flagsOf(table.root)) }
    }

    @Test
    fun canMoveTo() {
        val slots = SlotTable()
        var srcHandle = NULL_GROUP_HANDLE

        // Create a slot table
        slots.edit {
            insert {
                group(100) {
                    group(200) {
                        repeat(5) { iteration ->
                            group(1000 + iteration) {
                                group(2000 + iteration) {
                                    if (iteration == 3) {
                                        srcHandle = parentHandle
                                        addFlags(0)
                                    }
                                    repeat(iteration) { node -> nodeGroup(2000 + node, node) }
                                }
                            }
                        }
                    }
                }
            }
        }

        slots.verifyWellFormed()

        // Move the anchored group into another table
        val movedNodes = slots.newTableInSameAddressSpace()
        movedNodes.edit { moveFrom(slots, srcHandle) }

        // Validate the slot table
        slots.verifyWellFormed()
        movedNodes.verifyWellFormed()

        slots.read {
            expectGroup(100) {
                expectGroup(200) {
                    repeat(5) { iteration ->
                        expectGroup(1000 + iteration) {
                            if (iteration != 3) {
                                expectGroup(2000 + iteration) {
                                    repeat(iteration) { node -> expectNode(2000 + node, node) }
                                }
                            }
                        }
                    }
                }
            }
        }

        movedNodes.read {
            expectGroup(2003) { repeat(3) { node -> expectNode(2000 + node, node) } }
        }

        // Insert the nodes back
        srcHandle = movedNodes.rootHandle()
        slots.edit {
            group {
                group {
                    skipGroup()
                    skipGroup()
                    skipGroup()
                    group { moveFrom(movedNodes, srcHandle) }
                }
            }
        }

        // Validate the move back
        slots.verifyWellFormed()
        movedNodes.verifyWellFormed()

        assertEquals(0, movedNodes.groupsSize())

        slots.read {
            expectGroup(100) {
                expectGroup(200) {
                    repeat(5) {
                        expectGroup(1000 + it) {
                            expectGroup(2000 + it) {
                                repeat(it) { node -> expectNode(2000 + node, node) }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun canDeleteAGroupAfterMovingPartOfItsContent() {
        var moveSource = NULL_GROUP_HANDLE
        var removeSource = NULL_GROUP_HANDLE

        // Create a slot table
        val slots =
            SlotTable.build {
                group(100) {
                    group(200) {
                        group(300) {
                            group(400) {
                                group(500) {
                                    removeSource = parentHandle
                                    nodeGroup(501, 501) {
                                        group(600) {
                                            group(700) {
                                                addFlags(0)
                                                moveSource = parentHandle
                                                group(800) { nodeGroup(801, 801) }
                                                group(900) { nodeGroup(901, 901) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        val movedNodes = slots.newTableInSameAddressSpace()
        movedNodes.edit {
            val dstEditor = this
            slots.edit {
                val srcEditor = this
                dstEditor.moveFrom(srcEditor, moveSource)
                seek(removeSource)
                removeGroup()
            }
        }

        movedNodes.verifyWellFormed()
        slots.verifyWellFormed()

        // Validate slots
        slots.read {
            expectGroup(100) { expectGroup(200) { expectGroup(300) { expectGroup(400) } } }
        }

        // Validate moved nodes
        movedNodes.read {
            expectGroup(700) {
                expectGroup(800) { expectNode(801, 801) }
                expectGroup(900) { expectNode(901, 901) }
            }
        }
    }

    @Test
    fun canMoveAGroupFromATableIntoAnotherGroup() {
        // Create a slot table
        val slots =
            // TODO: Enable collecting source information
            SlotTable.build /* (SlotTableAddressSpace().apply { collectSourceInformation() }) */ {
                group(100) {
                    group(200) {
                        group(300) {
                            group(400) {
                                group(410) {
                                    append("1")
                                    append("2")
                                }
                                group(450) {}
                                group(460) {
                                    append("3")
                                    append("4")
                                }
                            }
                        }
                    }
                }
            }
        slots.verifyWellFormed()

        val insertTable =
            slots.buildSubTable {
                group(1000) {
                    append("100")
                    append("200")
                    nodeGroup(125, 1000)
                    nodeGroup(125, 2000)
                }
            }
        insertTable.verifyWellFormed()

        slots.edit {
            group {
                group {
                    group {
                        group {
                            skipGroup()
                            group { moveFrom(insertTable, insertTable.rootHandle()) }
                        }
                    }
                }
            }
        }
        slots.verifyWellFormed()

        slots.read {
            expectGroup(100) {
                expectGroup(200) {
                    expectGroup(300) {
                        expectGroup(400) {
                            expectGroup(410) {
                                expectData("1")
                                expectData("2")
                            }
                            expectGroup(450) {
                                expectGroup(1000) {
                                    expectData("100")
                                    expectData("200")
                                    expectNode(125, 1000)
                                    expectNode(125, 2000)
                                }
                            }
                            expectGroup(460) {
                                expectData("3")
                                expectData("4")
                            }
                        }
                    }
                }
            }
        }
    }

    // TODO: Copy over source information tests

    @Test
    fun canMoveAGroupFromATableIntoAnotherGroupAndModifyThatGroup() {
        // Create a slot table
        val slots =
            SlotTable.build(SlotTableAddressSpace()) {
                group(100) {
                    group(200) {
                        group(300) {
                            group(400) {
                                group(410) {
                                    append("1")
                                    append("2")
                                }
                                group(450) {}
                                group(460) {
                                    append("3")
                                    append("4")
                                }
                            }
                        }
                    }
                }
            }
        slots.verifyWellFormed()

        val insertTable =
            slots.buildSubTable {
                group(1000) {
                    append("100")
                    append("200")
                    nodeGroup(125, 1000)
                    nodeGroup(125, 2000)
                }
            }
        insertTable.verifyWellFormed()

        val (previous1, previous2) =
            slots.edit {
                startGroup()
                startGroup()
                startGroup()
                startGroup()
                skipGroup()
                startGroup()
                moveFrom(sourceTable = insertTable, sourceHandle = insertTable.rootHandle())
                startGroup()
                val previous1 = setRelative(0, "300")
                val previous2 = setRelative(1, "400")
                previous1 to previous2
            }
        slots.verifyWellFormed()

        assertEquals("100", previous1)
        assertEquals("200", previous2)

        slots.read {
            expectGroup(100) {
                expectGroup(200) {
                    expectGroup(300) {
                        expectGroup(400) {
                            expectGroup(410) {
                                expectData("1")
                                expectData("2")
                            }
                            expectGroup(450) {
                                expectGroup(1000) {
                                    expectData("300")
                                    expectData("400")
                                    expectNode(125, 1000)
                                    expectNode(125, 2000)
                                }
                            }
                            expectGroup(460) {
                                expectData("3")
                                expectData("4")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun canAddSlotsAfterChildGroupAdded() {
        val slotTable =
            SlotTable.build {
                group(1) {
                    group(10) {
                        append("10")
                        group(100) { append("100") }
                        group(200) { append("200") }
                        append("11")
                        append("12")
                        group(300) { append("300") }
                    }
                }
            }

        slotTable.verifyWellFormed()

        slotTable.read {
            expectGroup(1) {
                expectGroup(10) {
                    expectData("10")
                    expectGroup(100) { expectData("100") }
                    expectGroup(200) { expectData("200") }
                    expectData("11")
                    expectData("12")
                    expectGroup(300) { expectData("300") }
                }
            }
        }
    }

    @Test
    fun canAddSlotsAfterChildGroupAddedThenEmptyChildrenThenChildrenWithSlots() {
        val slotTable =
            SlotTable.build {
                group(1) {
                    group(10) {
                        append("10")
                        group(300) {}
                        group(400) {}
                        append("11")
                        append("12")
                        group(500) { append("500") }
                    }
                }
            }

        slotTable.verifyWellFormed()

        slotTable.read {
            expectGroup(1) {
                expectGroup(10) {
                    expectData("10")
                    expectGroup(300) {}
                    expectGroup(400) {}
                    expectData("11")
                    expectData("12")
                    expectGroup(500) { expectData("500") }
                }
            }
        }
    }

    @Test
    fun supportsAppendingSlots_first_empty() {
        val slots =
            SlotTable.build(SlotTableAddressSpace()) {
                group(100) {
                    group(200) {}
                    group(300) {}
                }
            }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectStrictGroup(200) {}
                expectStrictGroup(300) {}
            }
        }

        slots.edit {
            group {
                group {
                    appendSlot("200")
                    appendSlot("201")
                }
            }
        }

        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                }
                expectStrictGroup(300) {}
            }
        }
    }

    @Test
    fun supportsAppendingSlots_first_occupied() {
        val slots =
            SlotTable.build {
                group(100) {
                    group(200) {}
                    group(300) {
                        append("300")
                        append("301")
                        append("302")
                    }
                }
            }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectStrictGroup(200) {}
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                    expectData("302")
                }
            }
        }

        slots.edit {
            group {
                group {
                    appendSlot("200")
                    appendSlot("201")
                }
            }
        }

        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                }
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                    expectData("302")
                }
            }
        }
    }

    @Test
    fun supportsAppendingSlots_after_occupied() {
        val slots =
            SlotTable.build {
                group(100) {
                    group(200) {
                        append("200")
                        append("201")
                    }
                    group(300) {
                        append("300")
                        append("301")
                        append("302")
                    }
                }
            }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                }
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                    expectData("302")
                }
            }
        }

        slots.edit {
            group {
                group {
                    appendSlot("202")
                    appendSlot("203")
                }
            }
        }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                    expectData("202")
                    expectData("203")
                }
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                    expectData("302")
                }
            }
        }
    }

    @Test
    fun supportsAppendingSlots_middle() {
        val slots =
            SlotTable.build(SlotTableAddressSpace()) {
                group(100) {
                    append("100")
                    append("101")
                    group(200) {}
                    group(300) {
                        append("300")
                        append("301")
                    }
                    append("102")
                    append("103")
                }
            }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200) {}
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                }
                expectData("102")
                expectData("103")
            }
        }

        slots.edit {
            group {
                group {
                    appendSlot("200")
                    appendSlot("201")
                    appendSlot("202")
                }
            }
        }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                    expectData("202")
                }
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                }
                expectData("102")
                expectData("103")
            }
        }
    }

    @Test
    fun supportsAppendingSlots_end() {
        val slots =
            SlotTable.build {
                group(100) {
                    append("100")
                    append("101")
                    group(200) {}
                    group(300) {}
                    append("102")
                    append("103")
                }
            }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200) {}
                expectStrictGroup(300) {}
                expectData("102")
                expectData("103")
            }
        }

        slots.edit {
            group {
                group {
                    appendSlot("200")
                    appendSlot("201")
                    appendSlot("202")
                }
            }
        }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                    expectData("202")
                }
                expectStrictGroup(300) {}
                expectData("102")
                expectData("103")
            }
        }
    }

    @Test
    fun supportsAppendingSlots_canAdd2000Slots() {
        val slots =
            SlotTable.build {
                group(100) {
                    append("100")
                    append("101")
                    group(200) {}
                    group(300) {}
                    append("102")
                    append("103")
                }
            }
        slots.verifyWellFormed()

        var handle = NULL_GROUP_HANDLE
        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200) { handle = parentHandle }
                expectStrictGroup(300) {}
                expectData("102")
                expectData("103")
            }
        }

        repeat(200) { iteration ->
            slots.edit {
                seek(handle)
                group { repeat(10) { value -> appendSlot(value) } }
            }
            slots.verifyWellFormed()
            slots.read {
                expectStrictGroup(100) {
                    expectData("100")
                    expectData("101")
                    expectStrictGroup(200) {
                        repeat((iteration + 1) * 10) { value -> expectData(value % 10) }
                    }
                    expectStrictGroup(300) {}
                    expectData("102")
                    expectData("103")
                }
            }
        }
    }

    @Test
    fun supportsRemovingSlots_toEmpty() {
        val slots =
            SlotTable.build {
                group(100) {
                    append("100")
                    append("101")
                    group(200) {
                        append("200")
                        append("201")
                        append("202")
                    }
                    group(300) {
                        append("300")
                        append("301")
                        append("302")
                    }
                    append("102")
                    append("103")
                }
            }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                    expectData("202")
                }
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                    expectData("302")
                }
                expectData("102")
                expectData("103")
            }
        }

        slots.edit { group { group { trimSlots(3) } } }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200)
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                    expectData("302")
                }
                expectData("102")
                expectData("103")
            }
        }
    }

    @Test
    fun supportsRemovingSlots_trim() {
        val slots =
            SlotTable.build {
                group(100) {
                    append("100")
                    append("101")
                    group(200) {
                        append("200")
                        append("201")
                        append("202")
                    }
                    group(300) {
                        append("300")
                        append("301")
                        append("302")
                    }
                    append("102")
                    append("103")
                }
            }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                    expectData("202")
                }
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                    expectData("302")
                }
                expectData("102")
                expectData("103")
            }
        }

        slots.edit { group { group { trimSlots(1) } } }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                }
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                    expectData("302")
                }
                expectData("102")
                expectData("103")
            }
        }
    }

    @Test
    fun supportsRemovingSlots_toEmpty_withAux() {
        val slots =
            SlotTable.build {
                group(100) {
                    append("100")
                    append("101")
                    group(200) {
                        insertAux("200 Aux")
                        append("200")
                        append("201")
                        append("202")
                    }
                    group(300) {
                        append("300")
                        append("301")
                        append("302")
                    }
                    append("102")
                    append("103")
                }
            }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectAux("200 Aux")
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                    expectData("202")
                }
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                    expectData("302")
                }
                expectData("102")
                expectData("103")
            }
        }

        slots.edit { group { group { trimSlots(3) } } }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectAux("200 Aux")
                expectStrictGroup(200)
                expectStrictGroup(300) {
                    expectData("300")
                    expectData("301")
                    expectData("302")
                }
                expectData("102")
                expectData("103")
            }
        }
    }

    @Test
    fun supportsRemovingSlots_toEmpty_atEnd() {
        val slots =
            SlotTable.build(SlotTableAddressSpace()) {
                group(100) {
                    append("100")
                    append("101")
                    group(200) {
                        append("200")
                        append("201")
                        append("202")
                    }
                }
            }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200) {
                    expectData("200")
                    expectData("201")
                    expectData("202")
                }
            }
        }

        slots.edit { group { group { trimSlots(3) } } }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectData("100")
                expectData("101")
                expectStrictGroup(200)
            }
        }
    }

    @Test
    fun movingGroupsAtTheEndOfTheTable() {
        val slots =
            SlotTable.build {
                group(100) {
                    group(200) {
                        append("2000")
                        append("2001")
                        append("2002")
                    }
                    group(201) {
                        append("2010")
                        append("2011")
                        append("2012")
                    }
                    group(202) {
                        append("2020")
                        append("2021")
                        append("2022")
                        group(300) {}
                        group(300) {}
                    }
                    group(203) {}
                    group(204) {}
                    group(205) {}
                }
            }
        slots.verifyWellFormed()

        slots.read {
            expectStrictGroup(100) {
                expectStrictGroup(200) {
                    expectData("2000")
                    expectData("2001")
                    expectData("2002")
                }
                expectStrictGroup(201) {
                    expectData("2010")
                    expectData("2011")
                    expectData("2012")
                }
                expectStrictGroup(202) {
                    expectData("2020")
                    expectData("2021")
                    expectData("2022")
                    expectStrictGroup(300) {}
                    expectStrictGroup(300) {}
                }
                expectStrictGroup(203) {}
                expectStrictGroup(204) {}
                expectStrictGroup(205) {}
            }
        }

        slots.edit {
            group {
                skipGroup()
                skipGroup()
                insert { group(1000) {} }
                skipGroup()
                moveGroup(1)
            }
        }

        slots.read {
            expectStrictGroup(100) {
                expectStrictGroup(200) {
                    expectData("2000")
                    expectData("2001")
                    expectData("2002")
                }
                expectStrictGroup(201) {
                    expectData("2010")
                    expectData("2011")
                    expectData("2012")
                }
                expectStrictGroup(1000)
                expectStrictGroup(203) {}
                expectStrictGroup(202) {
                    expectData("2020")
                    expectData("2021")
                    expectData("2022")
                    expectStrictGroup(300) {}
                    expectStrictGroup(300) {}
                }
                expectStrictGroup(204) {}
                expectStrictGroup(205) {}
            }
        }
    }

    @Test
    fun trimmingDataAtTheEndOfTheTable() {
        val table =
            SlotTable.build(SlotTableAddressSpace()) {
                group(100) {
                    group(200) {}
                    group(300) {
                        append("301")
                        append("302")
                        append("303")
                        group(400) {
                            append("401")
                            append("402")
                            append("403")
                        }
                    }
                    group(500) {}
                }
            }
        table.verifyWellFormed()

        table.read {
            expectStrictGroup(100) {
                expectStrictGroup(200) {}
                expectStrictGroup(300) {
                    expectData("301")
                    expectData("302")
                    expectData("303")
                    expectStrictGroup(400) {
                        expectData("401")
                        expectData("402")
                        expectData("403")
                    }
                }
                expectStrictGroup(500) {}
            }
        }

        val insertHandles = mutableListOf<GroupHandle>()
        val insertTable =
            table.buildSubTable {
                group(1000) {
                    insertHandles.add(parentHandle)
                    append("1001")
                    append("1002")
                }
                group(1000) {
                    insertHandles.add(parentHandle)
                    append("1001")
                    append("1002")
                }
            }
        table.edit {
            group {
                // Insert group 1000 after group 200
                skipGroup()
                insertGroupFrom(insertTable = insertTable, handle = insertHandles.first())
                // Trim group 300's slots
                skipGroup()
                startGroup()
                trimSlots(3)
                endGroup()
                // Insert group 1000 after group 300
                insertGroupFrom(insertTable = insertTable, handle = insertHandles.drop(1).first())
            }
        }
        table.verifyWellFormed()

        table.read {
            expectStrictGroup(100) {
                expectStrictGroup(200) {}
                expectStrictGroup(1000) {
                    expectData("1001")
                    expectData("1002")
                }
                expectStrictGroup(300) {
                    expectStrictGroup(400) {
                        expectData("401")
                        expectData("402")
                        expectData("403")
                    }
                }
                expectStrictGroup(1000) {
                    expectData("1001")
                    expectData("1002")
                }
                expectStrictGroup(500) {}
            }
        }
    }

    @Test
    fun canMoveGroupsFromOneAddressSpaceToAnother() {
        var groupHandle: GroupHandle = NULL_GROUP_HANDLE
        val sourceTable =
            SlotTable.build {
                group(100) {
                    group(300) {
                        groupHandle = parentHandle
                        append("1")
                        append("2")
                        append("3")
                        group(1000) {
                            append("1001")
                            append("1002")
                            append("1003")
                        }
                    }
                }
            }

        val destTable =
            SlotTable.build {
                group(100) {
                    group(200)
                    group(400)
                }
            }

        destTable.edit {
            group {
                skipGroup()
                moveFrom(sourceTable, groupHandle)
                skipToGroupEnd()
            }
        }

        destTable.read {
            expectStrictGroup(100) {
                expectStrictGroup(200)
                expectStrictGroup(300) {
                    expectData("1")
                    expectData("2")
                    expectData("3")
                    expectStrictGroup(1000) {
                        expectData("1001")
                        expectData("1002")
                        expectData("1003")
                    }
                }
                expectStrictGroup(400)
            }
        }
    }

    @Test
    fun canReportNonGroupCallInformationWhileBuilding() {
        val slots =
            SlotTable.build {
                collectSourceInformation()
                group(100) {
                    sgroup(200, "C(200)") {
                        grouplessCall(300, "C(300)") {}
                        grouplessCall(301, "C(301)") {}
                        sgroup(302, "C(302)")
                        grouplessCall(303, "C(303)") {}
                        sgroup(304, "C(304)")
                        grouplessCall(305, "C(305)") {
                            sgroup(400, "C(400)")
                            sgroup(401, "C(401)")
                        }
                        grouplessCall(306, "C(306)") {
                            sgroup(402, "C(402)")
                            grouplessCall(403, "C(403)") {
                                sgroup(500, "C(500)")
                                sgroup(501, "C(501)")
                            }
                        }
                    }
                }
            }
        slots.verifyWellFormed()

        val expectedRoot =
            SourceGroup.group(100) {
                group(200, "C(200)") {
                    group(300, "C(300)") {}
                    group(301, "C(301)") {}
                    group(302, "C(302)") {}
                    group(303, "C(303)") {}
                    group(304, "C(304)") {}
                    group(305, "C(305)") {
                        group(400, "C(400)") {}
                        group(401, "C(401)") {}
                    }
                    group(306, "C(306)") {
                        group(402, "C(402)") {}
                        group(403, "C(403)") {
                            group(500, "C(500)") {}
                            group(501, "C(501)") {}
                        }
                    }
                }
            }

        val slotsRoot = SourceGroup.group(slots)
        assertEquals(expectedRoot, slotsRoot)
    }

    @Test
    fun reportsGrouplessDataInSourceInformationGroup() {
        val table =
            SlotTable.build {
                collectSourceInformation()
                group(100) {
                    sgroup(200, "C(200)") {
                        append(201)
                        append(202)
                        append(203)
                        sgroup(300, "C(300)") {
                            append(301)
                            append(302)
                            append(301)
                        }
                        grouplessCall(400, "C(400)") {
                            append(401)
                            append(402)
                            append(403)
                        }
                        sgroup(500, "C(500)") {
                            append(501)
                            append(502)
                            append(503)
                        }
                        grouplessCall(600, "C(600)") {
                            append(601)
                            grouplessCall(700, "C(700)") {
                                append(701)
                                append(702)
                                append(703)
                            }
                            append(602)
                            grouplessCall(800, "C(800)") {
                                append(801)
                                append(802)
                                append(803)
                            }
                            append(603)
                        }
                    }
                }
            }
        val expectedTree =
            SourceGroup.group(100) {
                group(200, "C(200)") {
                    data(201)
                    data(202)
                    data(203)
                    group(300, "C(300)") {
                        data(301)
                        data(302)
                        data(301)
                    }
                    group(400, "C(400)") {
                        data(401)
                        data(402)
                        data(403)
                    }
                    group(500, "C(500)") {
                        data(501)
                        data(502)
                        data(503)
                    }
                    group(600, "C(600)") {
                        data(601)
                        group(700, "C(700)") {
                            data(701)
                            data(702)
                            data(703)
                        }
                        data(602)
                        group(800, "C(800)") {
                            data(801)
                            data(802)
                            data(803)
                        }
                        data(603)
                    }
                }
            }
        val receivedTree = SourceGroup.group(table, includeData = true)
        assertEquals(expectedTree, receivedTree)
    }

    @Test
    fun canMoveSourceInformationFromAnotherTable() {
        val sourceTable =
            SlotTable.build {
                collectSourceInformation()
                sgroup(200, "C(200)") { grouplessCall(300, "C(300)") { sgroup(400, "C(400)") {} } }
            }
        sourceTable.verifyWellFormed()

        val mainTable =
            SlotTable.build {
                collectSourceInformation()
                group(100) { sgroup(201, "C(201)") {} }
            }

        mainTable.verifyWellFormed()

        mainTable.edit {
            startGroup()
            moveFrom(sourceTable, sourceTable.rootHandle())
            skipToGroupEnd()
            endGroup()
        }
        mainTable.verifyWellFormed()

        val expected =
            SourceGroup.group(100) {
                group(200, "C(200)") { group(300, "C(300)") { group(400, "C(400)") {} } }
                group(201, "C(201)") {}
            }
        val received = SourceGroup.group(mainTable)
        assertEquals(expected, received)
    }

    @Test
    fun canMoveSourceInformationIntoAGroupWithSourceInformation() {
        val sourceTable =
            SlotTable.build {
                collectSourceInformation()
                sgroup(300, "C(300)") { grouplessCall(400, "C(400)") { sgroup(500, "C(500)") {} } }
            }
        sourceTable.verifyWellFormed()

        val mainTable =
            SlotTable.build {
                collectSourceInformation()
                group(100) { sgroup(201, "C(201)") {} }
            }
        mainTable.verifyWellFormed()

        mainTable.edit {
            group {
                group(201) {
                    moveFrom(sourceTable, sourceTable.rootHandle())
                    skipToGroupEnd()
                }
                skipToGroupEnd()
            }
        }
        mainTable.verifyWellFormed()

        val expected =
            SourceGroup.group(100) {
                group(201, "C(201)") {
                    group(300, "C(300)") { group(400, "C(400)") { group(500, "C(500)") {} } }
                }
            }
        val received = SourceGroup.group(mainTable)
        assertEquals(expected, received)
    }

    @Test
    fun canRemoveAGroupBeforeAnEmptyGrouplessCall() {
        val slots =
            SlotTable.build {
                collectSourceInformation()
                group(100) {
                    sgroup(200, "C(2001)") {}
                    grouplessCall(201, "C(201)") {}
                    sgroup(202, "C(202)") {}
                }
            }
        slots.verifyWellFormed()

        slots.edit {
            group {
                removeGroup()
                skipToGroupEnd()
            }
        }
        slots.verifyWellFormed()

        val expected =
            SourceGroup.group(100) {
                group(201, "C(201)") {}
                group(202, "C(202)") {}
            }
        val received = SourceGroup.group(slots)
        assertEquals(expected, received)
    }

    @Test
    fun canRemoveAGroupWithSourceInformation() {
        val slots =
            SlotTable.build {
                collectSourceInformation()
                group(100) {
                    sgroup(200, "C(200)") {}
                    sgroup(201, "C(201)") {}
                    sgroup(202, "C(202)") {
                        grouplessCall(300, "C(300)") {
                            sgroup(400, "C(400)") { group(500, "C(500)") {} }
                        }
                    }
                }
            }
        slots.verifyWellFormed()

        slots.edit {
            group(100) {
                skipGroup()
                skipGroup()
                group {
                    group {
                        removeGroup() // Remove group 500
                        skipToGroupEnd()
                    }
                }
            }
        }
        slots.verifyWellFormed()

        val expected =
            SourceGroup.group(100) {
                group(200, "C(200)") {}
                group(201, "C(201)") {}
                group(202, "C(202)") { group(300, "C(300)") { group(400, "C(400)") {} } }
            }
        val received = SourceGroup.group(slots)

        assertEquals(expected, received)
    }

    @Test
    fun canAddAGrouplessCallToAGroupWithNoSourceInformation() {
        val slots =
            SlotTable.build {
                collectSourceInformation()
                group(100) {
                    group(200) {
                        sgroup(300, "C(300)")
                        sgroup(301, "C(301)")
                        grouplessCall(302, "C(302)") { sgroup(400, "C(400)") }
                    }
                    sgroup(201, "C(201)") {
                        group(303) {
                            sgroup(401, "C(401)")
                            grouplessCall(402, "C(402)") {}
                            sgroup(403, "C(403)")
                        }
                    }
                }
            }

        val expected =
            SourceGroup.group(100) {
                group(200) {
                    group(300, "C(300)") {}
                    group(301, "C(301)") {}
                    group(302, "C(302)") { group(400, "C(400)") {} }
                }
                group(201, "C(201)") {
                    group(303) {
                        group(401, "C(401)") {}
                        group(402, "C(402)") {}
                        group(403, "C(403)") {}
                    }
                }
            }
        val received = SourceGroup.group(slots)

        assertEquals(expected, received)
    }
}

private const val treeRoot = -1
private const val elementKey = 100

private fun testSlotsNumbered(): SlotTable {
    return SlotTable.build {
        startGroup(treeRoot, Composer.Empty)
        repeat(100) {
            startGroup(it, Composer.Empty)
            endGroup()
        }
        endGroup()
    }
}

// Creates 0 until 10 items each with 10 elements numbered 0...n with 0..n slots
private fun testItems(): SlotTable {
    return SlotTable.build {
        startGroup(treeRoot, Composer.Empty)

        fun item(key: Int, block: () -> Unit) {
            startGroup(key, key)
            block()
            endGroup()
        }

        fun element(key: Int, block: () -> Unit) {
            startNode(key, node = "node for key $key")
            block()
            endGroup()
        }

        for (key in 1..10) {
            item(key) {
                for (item in 0..key) {
                    element(key * elementKey + item) { for (element in 0..key) append(-element) }
                }
            }
        }

        endGroup()
    }
}

private fun validateItems(slots: SlotTable) {
    slots.read {
        check(groupKey == treeRoot) { "Invalid root key" }
        startGroup()

        fun item(key: Int, block: () -> Unit) {
            check(groupKey == key) {
                "Unexpected key at $currentGroup, expected $key, received $groupKey"
            }
            check(groupObjectKey == key) {
                "Unexpected data key at $currentGroup, expected $key, received $groupObjectKey"
            }
            startGroup()
            block()
            endGroup()
        }

        fun element(key: Int, block: () -> Unit) {
            check(isNode) { "Expected a node group" }
            check(groupObjectKey == key) { "Invalid node key at $currentGroup" }
            check(groupNode == "node for key $key") { "Unexpected node value at $currentGroup" }
            startNode()
            block()
            endGroup()
        }

        for (key in 1..10) {
            item(key) {
                for (item in 0..key) {
                    element(key * elementKey + item) {
                        for (element in 0..key) {
                            val received = next()
                            check(-element == received) {
                                "Unexpected slot value $element received $received"
                            }
                        }
                    }
                }
            }
        }

        endGroup()
    }
}

private fun narrowTrees(): Pair<SlotTable, List<GroupHandle>> {
    val anchors = mutableListOf<GroupHandle>()
    val slots =
        SlotTable.build {
            startGroup(treeRoot, Composer.Empty)

            fun item(key: Int, block: () -> Unit) {
                startGroup(key, Composer.Empty)
                block()
                endGroup()
            }

            fun element(key: Int, block: () -> Unit) {
                startNode(key, key)
                block()
                endGroup()
            }

            fun tree(key: Int, width: Int, depth: Int) {
                item(key) {
                    anchors.add(parentHandle)
                    when {
                        width > 0 ->
                            for (childKey in 1..width) {
                                tree(childKey, width - 1, depth + 1)
                            }
                        depth > 0 -> {
                            tree(1001, width, depth - 1)
                        }
                        else -> {
                            repeat(depth + 2) { element(-1) {} }
                        }
                    }
                }
            }

            element(1000) { tree(0, 5, 5) }
            endGroup()
        }

    return slots to anchors
}

private fun SlotTable.groupsSize(): Int {
    var count = 0
    addressSpace.traverseAllChildren(root) { count++ }
    return count
}

private fun SlotTable.slotsSize(): Int {
    var count = 0
    addressSpace.traverseAllChildren(root) {
        count += addressSpace.slotSize(addressSpace.groups.groupSlotRange(it))
    }
    return count
}

internal fun expectError(message: String, block: () -> Unit) {
    var exceptionThrown = false
    try {
        block()
    } catch (e: Throwable) {
        exceptionThrown = true
        assertTrue(
            e.message?.contains(message) == true,
            "Expected \"${e.message}\" to contain \"$message\"",
        )
    }
    assertTrue(exceptionThrown, "Expected test to throw an exception containing \"$message\"")
}

private fun SlotTableReader.expectAux(value: Any) {
    assertEquals(value, groupAux)
}

data class SourceGroup(
    val key: Any,
    val source: String?,
    val children: List<SourceGroup>,
    val data: List<Any?>,
) {
    override fun toString(): String = buildString { toStringBuilder(this, 0) }

    private fun toStringBuilder(builder: StringBuilder, indent: Int) {
        repeat(indent) { builder.append(' ') }
        builder.append("Group(")
        builder.append(key)
        builder.append(")")
        if (source != null) {
            builder.append(' ')
            builder.append(source)
        }
        if (data.isNotEmpty()) {
            builder.append(" [")
            var first = true
            for (item in data) {
                if (!first) builder.append(", ")
                first = false
                builder.append(item)
            }
            builder.append(']')
        }
        builder.appendLine()
        children.fastForEach { it.toStringBuilder(builder, indent + 2) }
    }

    data class BuilderScope(
        private val children: ArrayList<SourceGroup> = ArrayList(),
        private val data: ArrayList<Any?> = ArrayList(),
    ) {
        fun group(key: Int, source: String? = null, block: BuilderScope.() -> Unit) {
            val scope = BuilderScope()
            scope.block()
            this.children.add(SourceGroup(key, source, scope.children, scope.data))
        }

        fun data(value: Any?) {
            data.add(value)
        }
    }

    companion object {
        fun group(key: Int, block: BuilderScope.() -> Unit): SourceGroup {
            val children = ArrayList<SourceGroup>()
            val data = ArrayList<Any?>()
            val scope = BuilderScope(children, data)
            scope.block()
            return SourceGroup(key, null, children, data.toList())
        }

        fun group(compositionData: CompositionData, includeData: Boolean = false): SourceGroup =
            groupOf(compositionData.compositionGroups.first(), includeData)

        private fun groupOf(group: CompositionGroup, includeData: Boolean): SourceGroup =
            SourceGroup(
                group.key,
                group.sourceInfo,
                group.compositionGroups.map { groupOf(it, includeData) },
                if (includeData) group.data.toList() else emptyList(),
            )
    }
}
