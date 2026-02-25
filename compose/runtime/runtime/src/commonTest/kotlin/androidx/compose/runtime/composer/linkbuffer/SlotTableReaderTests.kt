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

import androidx.collection.mutableIntIntMapOf
import androidx.compose.runtime.Composer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SlotTableReaderTests {
    @Test
    fun canReadATable() {
        val table = SlotTable.build { group(10) { append(10) } }
        table.verifyWellFormed()
        table.read { group(10) { assertEquals(10, next()) } }
    }

    @Test
    fun canReadMultipleGroups() {
        val count = 100
        val table = SlotTable.build { group(1000) { repeat(count * 10) { group(100) } } }
        table.read { group(1000) { repeat(count * 10) { group(100) } } }
    }

    @Test
    fun canReadMultipleGroupsAndSlots() {
        val groupCount = 103
        val slotCount = 10
        val table =
            SlotTable.build {
                group(1000) {
                    repeat(slotCount) { append(it) }
                    repeat(groupCount) { key -> group(key) { repeat(slotCount) { append(it) } } }
                }
            }
        table.verifyWellFormed()
        table.read {
            group(1000) {
                repeat(slotCount) { assertEquals(it, next()) }
                repeat(groupCount) { key ->
                    group(key) {
                        repeat(slotCount) { assertEquals(it, next(), "Error in group $key") }
                    }
                }
            }
        }
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
        fun SlotTableReader.expect(contextKey: Int, groupKey: Int) {
            val handle = handle()
            val expectedContext = keyGroupMap.getOrElse(contextKey) { NULL_ADDRESS }
            val expectedGroup = keyGroupMap.getOrElse(groupKey) { NULL_ADDRESS }
            assertEquals(expectedContext, handle.context)
            assertEquals(expectedGroup, handle.group)
        }
        table.read {
            expect(-1, 1000)
            group(1000) {
                expect(-1, 1100)
                group(1100) {
                    expect(-1, 1110)
                    group(1110) {
                        expect(-1, 1111)
                        group(1111) { expect(1111, -1) }
                        expect(1111, 1112)
                        group(1112) { expect(1112, -1) }
                        expect(1112, 1113)
                        group(1113) { expect(1113, -1) }
                        expect(1110, -1)
                    }
                    expect(1110, 1120)
                    group(1120) {
                        expect(-1, 1121)
                        group(1121) { expect(1121, -1) }
                        expect(1121, 1122)
                        group(1122) { expect(1122, -1) }
                        expect(1122, 1123)
                        group(1123) { expect(1123, -1) }
                        expect(1120, -1)
                    }
                    expect(1100, -1)
                }
                expect(1000, -1)
            }
            expect(1000, 2000)
            group(2000) {
                expect(-1, 2100)
                group(2100) {
                    expect(-1, 2110)
                    group(2110) {
                        expect(-1, 2111)
                        group(2111) { expect(2111, -1) }
                        expect(2111, 2112)
                        group(2112) { expect(2112, -1) }
                        expect(2112, 2113)
                        group(2113) { expect(2113, -1) }
                        expect(2110, -2)
                    }
                    expect(2110, 2120)
                    group(2120) {
                        expect(-1, 2121)
                        group(2121) { expect(2121, -1) }
                        expect(2121, 2122)
                        group(2122) { expect(2122, -1) }
                        expect(2122, 2123)
                        group(2123) { expect(2123, -1) }
                        expect(2120, -1)
                    }
                    expect(2100, -1)
                }
                expect(2000, -1)
            }
        }
    }

    @Test
    fun canConditionallyTraverseChildren() {
        val table =
            SlotTable.build {
                group(0) {
                    group(10) {
                        group(100) {
                            group(key = 1000)
                            group(key = 1001)
                            group(key = 1002)
                        }
                        group(101) { group(1010) }
                        group(102) {
                            group(1020)
                            group(1021) { addFlags(IsRecompositionRequiredFlag) }
                            group(1022)
                            group(1023)
                        }
                    }
                    group(20) {
                        group(200) { group(2000) { addFlags(IsRecompositionRequiredFlag) } }
                    }
                }
            }

        fun traverse(
            enter: SlotTableReader.(group: GroupAddress) -> Boolean,
            block: SlotTableReader.(group: GroupAddress) -> Boolean,
        ): String {
            val result = buildString {
                var level = 0
                table.read {
                    fun emit(kind: Char, group: GroupAddress) {
                        repeat(level) { append(' ') }
                        append(kind)
                        append(':')
                        appendLine(groupKey(group))
                    }
                    traverseChildrenConditionally(
                        group = table.root,
                        enter = {
                            enter(it).also { r ->
                                if (r) {
                                    emit('I', it)
                                    level++
                                } else {
                                    emit('N', it)
                                }
                            }
                        },
                        block = {
                            block(it).also { r -> if (!r) emit('B', it) else emit('U', it) }
                        },
                        exit = {
                            level--
                            emit('O', it)
                        },
                        skip = { emit('S', it) },
                    )
                }
            }
            return result
        }

        // Traverse everything
        assertEquals(
            """
            B:10
            I:10
             B:100
             I:100
              B:1000
              S:1000
              B:1001
              S:1001
              B:1002
              S:1002
             O:100
             B:101
             I:101
              B:1010
              S:1010
             O:101
             B:102
             I:102
              B:1020
              S:1020
              B:1021
              S:1021
              B:1022
              S:1022
              B:1023
              S:1023
             O:102
            O:10
            B:20
            I:20
             B:200
             I:200
              B:2000
              S:2000
             O:200
            O:20
            """
                .trimIndent(),
            traverse(enter = { true }, block = { false }).trimIndent(),
        )

        // Don't enter group 10
        assertEquals(
            """
            B:10
            N:10
            B:20
            I:20
             B:200
             I:200
              B:2000
              S:2000
             O:200
            O:20
            """
                .trimIndent(),
            traverse(enter = { groupKey(it) != 10 }, block = { false }).trimIndent(),
        )

        // Only traverse invalidated groups
        assertEquals(
            """
            B:10
            I:10
             B:100
             N:100
             B:101
             N:101
             B:102
             I:102
              B:1020
              S:1020
              U:1021
              B:1022
              S:1022
              B:1023
              S:1023
             O:102
            O:10
            B:20
            I:20
             B:200
             I:200
              U:2000
             O:200
            O:20
            """
                .trimIndent(),
            traverse(
                    enter = { hasRecomposeRequired(it) },
                    block = { group ->
                        recomposeRequired(group).also {
                            if (it) {
                                removeFlag(group, HasRecompositionRequiredFlag)
                            }
                        }
                    },
                )
                .trimIndent(),
        )
    }
}

internal inline fun SlotTableReader.group(key: Int, block: SlotTableReader.() -> Unit = {}) {
    assertEquals(key, groupKey)
    startGroup()
    block()
    assertTrue(isGroupEnd)
    endGroup()
}

internal inline fun SlotTableReader.group(block: SlotTableReader.() -> Unit = {}) {
    startGroup()
    block()
    assertTrue(isGroupEnd)
    endGroup()
}

internal fun SlotTableReader.expectGroup(key: Int): Int {
    assertEquals(key, groupKey)
    return skipGroup()
}

internal fun SlotTableReader.expectGroup(key: Int, block: () -> Unit) {
    assertEquals(key, groupKey)
    startGroup()
    block()
    endGroup()
}

internal fun SlotTableReader.expectGroup(
    key: Int,
    objectKey: Any?,
    block: () -> Unit = { skipToGroupEnd() },
) {
    assertEquals(key, groupKey)
    assertEquals(objectKey, groupObjectKey)
    startGroup()
    block()
    assertTrue(isGroupEnd)
    endGroup()
}

internal inline fun SlotTableReader.expectNode(key: Int, node: Any, block: () -> Unit = {}) {
    assertEquals(key, groupObjectKey)
    assertEquals(node, groupNode)
    startNode()
    block()
    assertTrue(isGroupEnd)
    endGroup()
}

internal fun SlotTableReader.expectStrictGroup(key: Int, block: () -> Unit = {}) {
    assertEquals(key, groupKey)
    startGroup()
    block()
    expectData(Composer.Empty)
    assertTrue(isGroupEnd)
    endGroup()
}

internal fun SlotTableReader.expectData(data: Any) {
    assertEquals(data, next())
}
