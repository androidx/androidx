/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.tooling

import androidx.compose.SlotTable
import androidx.test.filters.SmallTest
import androidx.ui.core.Modifier
import androidx.ui.core.drawBehind
import androidx.ui.foundation.Box
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.layout.Column
import androidx.ui.layout.preferredSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class InspectableTests : ToolingTest() {
    @Test
    fun simpleInspection() {
        val slotTableRecord = SlotTableRecord.create()
        show {
            Inspectable(slotTableRecord) {
                Column {
                    Box(Modifier.preferredSize(100.dp).drawBehind {
                        drawRect(Color(0xFF))
                    })
                }
            }
        }

        // Should be able to find the group for this test
        val group = slotTableRecord.findGroupForFile("InspectableTests")
        assertNotNull(group)

        // The group should have a non-empty bounding box
        assertEquals(0.ipx, group!!.box.top)
        assertEquals(0.ipx, group.box.left)
        assertNotEquals(0.ipx, group.box.right)
        assertNotEquals(0.ipx, group.box.bottom)
    }

    @Test
    fun inInspectionMode() {
        var displayed = false
        show {
            Inspectable(SlotTableRecord.create()) {
                Column {
                    InInspectionModeOnly {
                        Box(Modifier.preferredSize(100.dp).drawBackground(Color(0xFF)))
                        displayed = true
                    }
                }
            }
        }

        assertTrue(displayed)
    }

    @Test
    fun notInInspectionMode() {
        var displayed = false
        show {
            Column {
                InInspectionModeOnly {
                    Box(Modifier.preferredSize(100.dp).drawBackground(Color(0xFF)))
                    displayed = true
                }
            }
        }

        assertFalse(displayed)
    }
}

internal fun SlotTableRecord.findGroupForFile(fileName: String) =
    store.map { it.findGroupForFile(fileName) }.filterNotNull().firstOrNull()

fun SlotTable.findGroupForFile(fileName: String) = asTree().findGroupForFile(fileName)
fun Group.findGroupForFile(fileName: String): Group? {
    val position = position
    if (position != null && position.contains(fileName)) return this
    return children.map { it.findGroupForFile(fileName) }.filterNotNull().firstOrNull()
}