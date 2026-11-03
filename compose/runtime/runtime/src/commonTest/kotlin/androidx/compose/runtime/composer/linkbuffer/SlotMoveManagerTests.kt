/*
 * Copyright 2026 The Android Open Source Project
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

import kotlin.test.Test
import kotlin.test.assertContentEquals

class SlotMoveManagerTests {

    @Test
    fun moveDoesNotMergeNonContiguousDestinationMovesAcrossUnmovedSlots() {
        val addressSpace =
            SlotTableAddressSpace(groupsCapacity = 6, slotsCapacity = 14).slots.apply {
                this[2] = "**"
                this[10] = "A0"
                this[11] = "A1"
                this[12] = "B0"
                this[13] = "B1"
            }

        // [  ,   , **,   ,   ,   ,   ,   ,   ,   , A0, A1, B0, B1]
        val manager = SlotMoveManager(source = addressSpace, destination = addressSpace)
        // [A0, A1, **,   ,   ,   ,   ,   ,   ,   ,   ,   , B0, B1]
        manager.move(destinationOffset = 0, startIndex = 10, endIndex = 12)
        // [A0, A1, **,   ,   ,   ,   ,   ,   ,   ,   ,   , B0, B1]
        manager.move(destinationOffset = 2, startIndex = 2, endIndex = 3)
        // [A0, A1, **, B0, B1,   ,   ,   ,   ,   ,   ,   ,   ,   ]
        manager.move(destinationOffset = 3, startIndex = 12, endIndex = 14)
        // [A0, A1, **, B0, B1,   ,   ,   ,   ,   ,   ,   ,   ,   ]

        assertContentEquals(
            arrayOfNulls<Any?>(14).apply {
                this[0] = "A0"
                this[1] = "A1"
                this[2] = "**"
                this[3] = "B0"
                this[4] = "B1"
            },
            manager.done().filterUnallocated(),
        )
    }

    @Test
    fun movePreservesTrailingUnmovedSlots() {
        val addressSpace =
            SlotTableAddressSpace(groupsCapacity = 6, slotsCapacity = 7).slots.apply {
                this[2] = "*"
                this[3] = "**"
                this[5] = "A0"
                this[6] = "A1"
            }

        // [  ,   ,  *, **,   , A0, A1]
        val manager = SlotMoveManager(source = addressSpace, destination = addressSpace)
        // [A0, A1,  *, **,   ,   ,   ]
        manager.move(destinationOffset = 0, startIndex = 5, endIndex = 7)
        // [A0, A1,  *, **,   ,   ,   ]
        manager.move(destinationOffset = 2, startIndex = 2, endIndex = 4)
        // [A0, A1,  *, **,   ,   ,   ]

        assertContentEquals(
            arrayOfNulls<Any?>(7).apply {
                this[0] = "A0"
                this[1] = "A1"
                this[2] = "*"
                this[3] = "**"
            },
            manager.done().filterUnallocated(),
        )
    }

    private fun Array<Any?>.filterUnallocated() =
        Array(size) { this[it].takeUnless(::isUnallocated) }
}
