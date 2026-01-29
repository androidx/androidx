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

import androidx.compose.runtime.Composer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SlotTableBuilderTests {
    @Test
    fun canCreateAnBuilder() {
        val table = SlotTable.build {}
        table.verifyWellFormed()
    }

    @Test
    fun canBuildGroups() {
        val count = 100
        val table = SlotTable.build { group(1000) { repeat(count * 10) { group(100) } } }
        table.verifyWellFormed()
    }

    @Test
    fun canBuildWithSlots() {
        val groupCount = 100
        val slotCount = 10
        val table =
            SlotTable.build {
                group(1000) {
                    repeat(slotCount) { append(it) }
                    repeat(groupCount * 10) { group(100) { repeat(slotCount) { append(it) } } }
                }
            }
        table.verifyWellFormed()
    }

    @Test
    fun canBuildMultipleRootGroups() {
        val table =
            SlotTable.build {
                repeat(9) {
                    group(it + 1) { repeat(9) { child -> group((it + 1) * 100 + (child + 1)) } }
                }
            }
        table.verifyWellFormed()
    }

    @Test
    fun canAccessSpecialSlotsWhileBuildingGroup() {
        SlotTable.build {
            group(100, 1, 2) {
                assertEquals(100, groupKey(parentGroup))
                assertEquals(1, groupObjectKey(parentGroup))
                assertEquals(2, groupAux(parentGroup))
            }
        }
    }

    @Test
    fun oneRectBenchmarkSimulation() {
        val table = SlotTable()

        repeat(10000) {
            val insertTable =
                table.buildSubTable {
                    group(1000) {
                        group(100) {
                            group(1) { appendN(1) }
                            group(1) { appendN(1) }
                            group(1) { appendN(1) }
                            group(1) { appendN(11) }
                            group(1) { appendN(1) }
                            group(1) { appendN(2) }
                            group(1) { appendN(2) }
                            group(1) { appendN(2) }
                            group(1) { appendN(26) }
                            group(1) { appendN(1) }
                            group(1) { appendN(1) }
                            group(1) { appendN(3) }
                            group(1) { appendN(2) }
                            group(1) { appendN(2) }
                            group(1) { appendN(4) }
                            group(1) { appendN(1) }
                            group(1) { appendN(1) }
                            group(1) { appendN(3) }
                            group(1) { appendN(2) }
                            group(1) { appendN(2) }
                            group(1) { appendN(2) }
                            group(1) {
                                appendN(2)
                                group(1) { appendN(1) }
                                group(1) { appendN(2) }
                                group(1) { appendN(2) }
                                group(1) { appendN(2) }
                                group(1) { appendN(2) }
                                group(1) { appendN(3) }
                                group(1) { appendN(5) }
                                appendN(2)
                            }
                        }
                    }
                }
            insertTable.dispose()
        }

        table.verifyWellFormed()
    }

    @Test
    fun canGrowParentSlotsAfterChildren() {
        val childCount = 25
        val table =
            SlotTable.build {
                group(100) {
                    append("100 before 200")
                    group(200) {
                        repeat(childCount) {
                            val value = "200 at $it"
                            append(value)
                            group(300) { append("300 - child ($it)") }
                        }
                    }
                }
            }
        table.verifyWellFormed()
        table.read {
            group(100) {
                expectData("100 before 200")
                group(200) {
                    repeat(childCount) {
                        expectData("200 at $it")
                        group(300) { expectData("300 - child ($it)") }
                    }
                }
            }
        }
    }

    @Test
    fun canGrowSlotsLikeGroupWhenGroupsAreElided() {
        val repeatCount = 100
        val childCount = 101
        val table = SlotTable.build { group(100) }
        repeat(repeatCount) {
            var insertHandle: GroupHandle = NULL_GROUP_HANDLE
            table.read {
                group(100) {
                    insertHandle = handle()
                    skipToGroupEnd()
                }
            }
            var deleteHandle = NULL_GROUP_HANDLE
            table.edit {
                insert(insertHandle) {
                    group(200) {
                        deleteHandle = parentHandle
                        repeat(childCount) {
                            append("200 at $it")
                            group(300) { append("300 - child ($it)") }
                        }
                    }
                }
            }
            table.edit {
                seek(deleteHandle)
                removeGroup()
            }
        }
    }

    @Test
    fun canBuildARootNodeInASharedAddressSpace() {
        var insertHandle = NULL_GROUP_HANDLE
        val rootTable = SlotTable.build { group(1000) { group(200) } }
        rootTable.read { group(1000) { group(200) { insertHandle = handle() } } }
        rootTable.edit {
            seek(insertHandle)
            insert { node("SomeNode") { append("Node data") } }
        }
        rootTable.verifyWellFormed()
    }

    @Test
    fun canBuildByMovingATable() {
        var sourceHandle = NULL_GROUP_HANDLE
        val sourceTable =
            SlotTable.build {
                group(1000) {
                    group(200) {
                        sourceHandle = parentHandle
                        group(10)
                        group(20)
                        group(30)
                        group(40)
                    }
                }
            }

        val destinationTable =
            sourceTable.buildSubTable {
                group(1000) {
                    group(100) {
                        sourceTable.edit { this@buildSubTable.moveFrom(this, sourceHandle) }
                    }
                }
            }

        // Validate the groups were moved
        destinationTable.read {
            group(1000) {
                group(100) {
                    group(200) {
                        group(10)
                        group(20)
                        group(30)
                        group(40)
                    }
                }
            }
        }

        // Validate the groups were removed from the source
        sourceTable.read { group(1000) { assertTrue(isGroupEnd) } }
    }

    @Test
    fun canGrowCorrectlyAtBoundaryCondition() {
        val sourceTable =
            SlotTable.build {
                group(100) {
                    group(101) {
                        // Allocate enough slots so that `append(3001)` below occupies exactly 1024
                        // when it grows groups 1000 slot space to 16, temporarily, after a write to
                        // group 1000 (1001, 1002) after a child is created (group 2000).
                        //
                        // This specifically tests a boundary condition where when the slots grow
                        // while
                        // the group has an extended range, there are no wholes in the free slot
                        // tracking. That is, the extended slot space needs to be contracted to its
                        // currently used size when the slots are grown instead of leaving the
                        // extra,
                        // unused space in place. This is because the slots are first compacted
                        // which
                        // requires all references to be up-to-date as they likely will be moved and
                        // the
                        // free space count is checked to make sure that we have exactly the number
                        // of
                        // free slots we expected.
                        repeat(1024 - 16 - 2) { append(it) }
                    }
                    group(1000) {
                        append(1000)
                        group(2000) { append(2001) }
                        append(1001)
                        append(1002)
                        group(3000) {
                            append(3001)
                            append(3002)
                        }
                    }
                }
            }
        sourceTable.verifyWellFormed()
    }

    @Test
    fun canAppendSeveralValuesAfterEndGroup() {
        // From a transcript of builder activity in foundation
        val table =
            SlotTable.build {
                startGroup(100)
                appendN(1)
                startGroup(1000)
                appendN(1)
                startGroup(20)
                appendN(1)
                startGroup(1330788943)
                appendN(11)
                startGroup(-149765515)
                appendN(1)
                startGroup(201)
                appendN(2)
                startGroup(202)
                appendN(2)
                startGroup(-280240369)
                appendN(2)
                startGroup(-520299287)
                appendN(26)
                startGroup(415205898)
                appendN(1)
                startGroup(201)
                appendN(1)
                startGroup(204)
                appendN(3)
                endGroup()
                startGroup(202)
                appendN(2)
                startGroup(1059770793)
                appendN(2)
                startGroup(1925803616)
                appendN(4)
                startGroup(415205898)
                appendN(1)
                startGroup(201)
                appendN(1)
                startGroup(204)
                appendN(2)
                appendN(1)
                endGroup()
                startGroup(202)
                appendN(2)
                startGroup(-656146368)
                appendN(2)
                startGroup(420213850)
                appendN(2)
                startGroup(98586082)
                startGroup(-1864999003)
                appendN(2)
                startGroup(-149765515)
                appendN(1)
                startGroup(201)
                appendN(2)
                startGroup(202)
                appendN(2)
                startGroup(97316118)
                appendN(5)
                startGroup(771959668)
                appendN(11)
                startGroup(-149765515)
                appendN(1)
                startGroup(201)
                appendN(2)
                startGroup(202)
                appendN(2)
                startGroup(-291176396)
                appendN(3)
                startGroup(439770924)
                endGroup()
                startGroup(125)
                appendN(5)
                startGroup(1564932727)
                appendN(2)
                startGroup(469439921)
                appendN(4)
                startGroup(-127)
                appendN(10)
                endGroup()
                startGroup(965149429)
                appendN(2)
                startGroup(-127)
                endGroup()
                startGroup(-2038064986)
                appendN(1)
                endGroup()
                appendN(10)
                startGroup(-2037536751)
                endGroup()
                appendN(5)
                startGroup(-2035764848)
                startGroup(430530635)
                appendN(4)
                endNGroups(2)
                appendN(20)
                startGroup(439770924)
                endGroup()
                startGroup(125)
                appendN(6)
                startGroup(-579239002)
                appendN(2)
                startGroup(-1442752422)
                appendN(1)
                startGroup(-1299400858)
                startGroup(-1299356714)
                appendN(2)
                endGroup()
                startGroup(155925518)
                appendN(2)
                startGroup(-1976819146)
                startGroup(1392105195)
                appendN(1)
                startGroup(-714464401)
                appendN(8)
                startGroup(-149765515)
                appendN(1)
                startGroup(201)
                appendN(2)
                startGroup(202)
                appendN(2)
                startGroup(274270255)
                appendN(3)
                startGroup(439770924)
                endGroup()
                startGroup(125)
                appendN(5)
                startGroup(-673241599)
                appendN(3)
                startGroup(-2101003086)
                appendN(2)
                startGroup(1969169726)
                appendN(2)
                startGroup(439770924)
                startGroup(1219399079)
                startGroup(408240218)
                endGroup()
                startGroup(1582736677)
                appendN(9)
                endNGroups(3)
                startGroup(125)
                appendN(5)
                startGroup(-211209833)
                appendN(2)
                startGroup(439770924)
                endGroup()
                startGroup(125)
                appendN(5)
                endNGroups(2)
                startGroup(-810390690)
                endNGroups(4)
                endGroup()
                appendN(1)
                startGroup(723898654)
                appendN(2)
                endNGroups(40)
            }
        table.verifyWellFormed()
    }
}

internal inline fun SlotTableBuilder.group(key: Int, block: SlotTableBuilder.() -> Unit = {}) {
    startGroup(key)
    block()
    endGroup()
}

internal inline fun SlotTableBuilder.group(
    key: Int,
    dataKey: Any?,
    aux: Any? = Composer.Empty,
    block: SlotTableBuilder.() -> Unit = {},
) {
    startDataGroup(key, dataKey, aux)
    block()
    endGroup()
}

internal fun SlotTableBuilder.appendN(count: Int) {
    repeat(count) { append(it) }
}

internal fun SlotTableBuilder.endNGroups(count: Int) {
    repeat(count) { endGroup() }
}

internal inline fun SlotTableBuilder.node(node: Any?, block: SlotTableBuilder.() -> Unit = {}) {
    startNodeGroup(125, Composer.Empty, node)
    block()
    endGroup()
}

internal inline fun SlotTableBuilder.sgroup(
    key: Int,
    sourceInformation: String,
    block: () -> Unit = {},
) {
    startGroup(key)
    recordGroupSourceInformation(sourceInformation)
    block()
    endGroup()
}

internal inline fun SlotTableBuilder.grouplessCall(
    key: Int,
    sourceInformation: String,
    block: () -> Unit,
) {
    recordGrouplessCallSourceInformationStart(key, sourceInformation)
    block()
    recordGrouplessCallSourceInformationEnd()
}

internal const val NodeKey = 125

internal inline fun SlotTableBuilder.nodeGroup(key: Int, node: Any, block: () -> Unit = {}) {
    startNodeGroup(NodeKey, key, node)
    block()
    endGroup()
}

internal fun SlotTableBuilder.startNode(key: Any?) =
    startNodeGroup(key = NodeKey, objectKey = key, node = Composer.Empty)

internal fun SlotTableBuilder.startNode(key: Any?, node: Any?) =
    startNodeGroup(key = NodeKey, objectKey = key, node = node)
