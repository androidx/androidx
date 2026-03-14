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

import androidx.collection.mutableIntListOf
import androidx.compose.runtime.Composer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SlotTableAddressSpaceTests {
    @Test
    fun canCreateAnAddressSpace() {
        val addressSpace = SlotTableAddressSpace()
        assertNotNull(addressSpace)
        addressSpace.validate()
    }

    @Test
    fun canAllocateAGroup() {
        val addressSpace = SlotTableAddressSpace()
        val group = addressSpace.allocateGroup(100, -1, 0)
        assertNotEquals(-1, group)
        addressSpace.validate()
    }

    @Test
    fun callAllocateGroupsToForceAGrow() {
        val addressSpace = SlotTableAddressSpace()
        var previousGroup = -1
        repeat(2000) { previousGroup = addressSpace.allocateGroup(it, previousGroup, 0) }
        addressSpace.validate()
    }

    @Test
    fun canAllocateAGroupAndFreeIt() {
        val addressSpace = SlotTableAddressSpace()
        val group = addressSpace.allocateGroup(100, -1, 0)
        addressSpace.freeGroupTree(group)
        addressSpace.validate()
    }

    @Test
    fun canAllocateAndFreeMultipleGroups() {
        val addressSpace = SlotTableAddressSpace()
        val groups = mutableIntListOf()
        repeat(100) {
            val group = addressSpace.allocateGroup(it, -1, 0)
            groups.add(group)
        }
        groups.forEach { addressSpace.freeGroupTree(it) }
        addressSpace.validate()
    }

    @Test
    fun canAllocateAndFreeMultipleGroupsWithAllocationFromFreeList() {
        val addressSpace = SlotTableAddressSpace()
        val groups = mutableIntListOf()
        repeat(1000) {
            val group = addressSpace.allocateGroup(it, -1, 0)
            groups.add(group)
        }
        groups.forEach { addressSpace.freeGroupTree(it) }
        groups.clear()
        repeat(1000) {
            val group = addressSpace.allocateGroup(it, -1, 0)
            groups.add(group)
        }
        groups.forEach { addressSpace.freeGroupTree(it) }

        addressSpace.validate()
    }

    @Test
    fun canWriteASlot() {
        val addressSpace = SlotTableAddressSpace()
        addressSpace.withGroup { group ->
            val value = "Some value"
            val newIndex = addressSpace.writeSlot(group, 0, value)
            assertTrue(newIndex != -1)
            assertEquals(addressSpace.readSlot(group, 0), value)
            addressSpace.validate()
        }
    }

    @Test
    fun canWriteTwoSlots() {
        val addressSpace = SlotTableAddressSpace()
        addressSpace.withGroup { group ->
            val a = "Some value"
            val b = "Some other value"
            addressSpace.writeSlot(group, 0, a)
            addressSpace.writeSlot(group, 1, b)
            assertTrue(addressSpace.groups.groupSlotRange(group) != -1)
            assertEquals(a, addressSpace.readSlot(group, 0))
            assertEquals(b, addressSpace.readSlot(group, 1))
            addressSpace.validate()
        }
    }

    @Test
    fun canWriteSlotsOutOfOrder() {
        val addressSpace = SlotTableAddressSpace()
        val groupA = addressSpace.allocateGroup(100, -1, 0)
        val groupB = addressSpace.allocateGroup(100, -1, 0)
        addressSpace.writeSlot(groupA, 0, 0)
        addressSpace.writeSlot(groupB, 0, 10)
        addressSpace.writeSlot(groupA, 1, 1)
        addressSpace.writeSlot(groupB, 1, 11)
        assertEquals(0, addressSpace.readSlot(groupA, 0))
        assertEquals(1, addressSpace.readSlot(groupA, 1))
        assertEquals(10, addressSpace.readSlot(groupB, 0))
        assertEquals(11, addressSpace.readSlot(groupB, 1))
        addressSpace.validate()
    }

    @Test
    fun canAllocateALargeBlockAndFreeIt() {
        val addressSpace = SlotTableAddressSpace()
        addressSpace.withGroup { group ->
            addressSpace.populateSlots(group, 100)
            addressSpace.validate()
            assertNotEquals(NULL_ADDRESS, addressSpace.groups.groupSlotRange(group))
            addressSpace.freeGroupTree(group)
            addressSpace.validate()
        }
    }

    @Test
    fun canAllocatedAdjacentBlocksAndFreeThem() {
        val addressSpace = SlotTableAddressSpace()
        val groupA = addressSpace.allocateGroup(100, -1, 0)
        val groupB = addressSpace.allocateGroup(100, -1, 0)
        addressSpace.populateSlots(groupA, 100)
        addressSpace.populateSlots(groupB, 100)
        addressSpace.validate()
        addressSpace.freeGroupTree(groupA)
        addressSpace.freeGroupTree(groupB)
        addressSpace.validate()
    }

    @Test
    fun canAllocateAdjacentLargeBlocksAndFreeThemOutOfOrder() {
        val addressSpace = SlotTableAddressSpace()
        val groupA = addressSpace.allocateGroup(100, -1, 0)
        val groupB = addressSpace.allocateGroup(100, -1, 0)
        val groupC = addressSpace.allocateGroup(100, -1, 0)
        addressSpace.populateSlots(groupA, 100)
        addressSpace.populateSlots(groupB, 200)
        addressSpace.populateSlots(groupC, 300)
        addressSpace.validate()
        addressSpace.freeGroupTree(groupA)
        addressSpace.freeGroupTree(groupB)
        addressSpace.freeGroupTree(groupC)
        addressSpace.validate()
    }

    @Test
    fun canWrite100ItemsToASlotBlock() {
        val addressSpace = SlotTableAddressSpace()
        addressSpace.withGroup { group ->
            repeat(100) { addressSpace.writeSlot(group, it, it) }
            repeat(100) { assertEquals(it, addressSpace.readSlot(group, it)) }
        }
    }

    @Test
    fun canWriteSlotsToForceSlotsToGrow() {
        val count = 2000
        val addressSpace = SlotTableAddressSpace()
        val group = addressSpace.allocateGroup(100, NULL_ADDRESS, 0)
        repeat(count) { addressSpace.writeSlot(group, it, it) }
        repeat(count) { assertEquals(it, addressSpace.readSlot(group, it)) }
        addressSpace.validate()
    }

    @Test
    fun whenAllocatingSlotsInFullyUsedArray_canAllocatedLargeBlocksFromMiddleOfTheFreeList() {
        val addressSpace = SlotTableAddressSpace(groupsCapacity = 6, slotsCapacity = 256)
        val startSize = addressSpace.slots.size
        val smallSize = 32
        val largeSize = 64
        val groupA = addressSpace.allocateGroup(100, -1, 0)
        val groupB = addressSpace.allocateGroup(101, -1, 0)
        val groupC = addressSpace.allocateGroup(102, -1, 0)
        val groupD = addressSpace.allocateGroup(103, -1, 0)
        val groupE = addressSpace.allocateGroup(104, -1, 0)
        addressSpace.populateSlots(groupA, smallSize)
        addressSpace.populateSlots(groupB, smallSize)
        addressSpace.populateSlots(groupC, smallSize)
        addressSpace.populateSlots(groupD, largeSize)
        addressSpace.freeGroupTree(groupA)
        addressSpace.freeGroupTree(groupB)
        addressSpace.freeGroupTree(groupC)
        addressSpace.populateSlots(groupE, largeSize * 2)
        addressSpace.validate()
        assertEquals(startSize, addressSpace.slots.size)
    }

    @Test
    fun canAllocateAndGrowASlotRangeWithoutWritingToIt() {
        val addressSpace = SlotTableAddressSpace()
        val groupA = addressSpace.allocateGroup(1, -1, 0)
        val groupB = addressSpace.allocateGroup(2, -1, 0)

        // Does not write to the first slot so it is still Empty
        addressSpace.writeSlot(groupA, 1, Composer.Empty)
        addressSpace.writeSlot(groupB, 1, Composer.Empty)

        // Move the groups
        addressSpace.resizeSlotRangeAtGroup(groupA, 3)
        addressSpace.resizeSlotRangeAtGroup(groupB, 3)
        addressSpace.resizeSlotRangeAtGroup(groupA, 4)
        addressSpace.resizeSlotRangeAtGroup(groupB, 4)

        // Ensure leaving the slots as Empty does not cause the groups to overlap
        addressSpace.validate()

        // Ensure that all the values are Composer.Empty
        addressSpace.assertSlotsEmptyOrUnallocated(groupA)
        addressSpace.assertSlotsEmptyOrUnallocated(groupB)
    }
}

private inline fun SlotTableAddressSpace.withGroup(block: (Int) -> Unit) {
    val group = allocateGroup(100, -1, 0)
    block(group)
}

private fun SlotTableAddressSpace.populateSlots(group: GroupAddress, size: Int) {
    writeSlot(group, size - 1, 1)
}

private fun SlotTableAddressSpace.assertSlotsEmptyOrUnallocated(group: GroupAddress) {
    slotAddressAndSize(groups.groupSlotRange(group)) { address, size ->
        for (index in address until address + size) {
            val value = slots[index]
            val effectiveValue = if (isUnallocated(value)) Composer.Empty else value
            assertEquals(Composer.Empty, effectiveValue)
        }
    }
}
