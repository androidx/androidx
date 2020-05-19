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

import androidx.compose.Composable
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
        val tree = slotTableRecord.store.first().asTree()
        val group = tree.firstOrNull {
            it.position?.contains("InspectableTests.kt") == true && it.box.right.value > 0
        } ?: error("Expected a group from this file")
        assertNotNull(group)

        // The group should have a non-empty bounding box
        assertEquals(0.ipx, group.box.top)
        assertEquals(0.ipx, group.box.left)
        assertNotEquals(0.ipx, group.box.right)
        assertNotEquals(0.ipx, group.box.bottom)
    }

    @Test
    fun parametersTest() {
        val slotTableRecord = SlotTableRecord.create()
        fun unknown(i: Int) = i

        show {
            Inspectable(slotTableRecord) {
                OneParameter(1)
                OneParameter(2)

                OneDefaultParameter()
                OneDefaultParameter(2)

                ThreeParameters(1, 2, 3)

                ThreeDefaultParameters()
                ThreeDefaultParameters(a = 1)
                ThreeDefaultParameters(b = 2)
                ThreeDefaultParameters(a = 1, b = 2)
                ThreeDefaultParameters(c = 3)
                ThreeDefaultParameters(a = 1, c = 3)
                ThreeDefaultParameters(b = 2, c = 3)
                ThreeDefaultParameters(a = 1, b = 2, c = 3)

                val ua = unknown(1)
                val ub = unknown(2)
                val uc = unknown(3)

                ThreeDefaultParameters()
                ThreeDefaultParameters(a = ua)
                ThreeDefaultParameters(b = ub)
                ThreeDefaultParameters(a = ua, b = ub)
                ThreeDefaultParameters(c = uc)
                ThreeDefaultParameters(a = ua, c = uc)
                ThreeDefaultParameters(b = ub, c = uc)
                ThreeDefaultParameters(a = ua, b = ub, c = uc)
            }
        }

        val tree = slotTableRecord.store.first().asTree()
        val list = tree.asList()
        val parameters = list.filter {
            it.parameters.isNotEmpty() && it.key.let {
                it is String && it.contains("InspectableTests")
            }
        }

        val callCursor = parameters.listIterator()
        class ParameterValidationReceiver(val parameterCursor: Iterator<ParameterInformation>) {
            fun parameter(
                name: String,
                value: Any,
                fromDefault: Boolean,
                static: Boolean,
                compared: Boolean
            ) {
                assertTrue(parameterCursor.hasNext())
                val parameter = parameterCursor.next()
                assertEquals(name, parameter.name)
                assertEquals(value, parameter.value)
                assertEquals(fromDefault, parameter.fromDefault)
                assertEquals(static, parameter.static)
                assertEquals(compared, parameter.compared)
            }
        }

        fun validate(block: ParameterValidationReceiver.() -> Unit) {
            assertTrue(callCursor.hasNext())
            val call = callCursor.next()
            val receiver = ParameterValidationReceiver(call.parameters.listIterator())
            receiver.block()
            assertFalse(receiver.parameterCursor.hasNext())
        }

        // OneParameter(1)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
        }

        // OneParameter(2)
        validate {
            parameter(name = "a", value = 2, fromDefault = false, static = true, compared = false)
        }

        // OneDefaultParameter()
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
        }

        // OneDefaultParameter(2)
        validate {
            parameter(name = "a", value = 2, fromDefault = false, static = true, compared = false)
        }

        // ThreeParameters(1, 2, 3)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = true, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = true, compared = false)
        }

        // ThreeDefaultParameters()
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(a = 1)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(b = 2)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = true, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(a = 1, b = 2)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = true, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(c = 3)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = true, compared = false)
        }

        // ThreeDefaultParameters(a = 1, c = 3)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = true, compared = false)
        }

        // ThreeDefaultParameters(b = 2, c = 3)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = true, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = true, compared = false)
        }

        // ThreeDefaultParameters(a = 1, b = 2, c = 3)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = true, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = true, compared = false)
        }

        // ThreeDefaultParameters()
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(a = ua)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = false, compared = true)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(b = ub)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = false, compared = true)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(a = ua, b = ub)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = false, compared = true)
            parameter(name = "b", value = 2, fromDefault = false, static = false, compared = true)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(c = uc)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = false, compared = true)
        }

        // ThreeDefaultParameters(a = ua, c = uc)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = false, compared = true)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = false, compared = true)
        }

        // ThreeDefaultParameters(b = ub, c = uc)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = false, compared = true)
            parameter(name = "c", value = 3, fromDefault = false, static = false, compared = true)
        }

        // ThreeDefaultParameters(a = ua, b = ub, c = uc)\
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = false, compared = true)
            parameter(name = "b", value = 2, fromDefault = false, static = false, compared = true)
            parameter(name = "c", value = 3, fromDefault = false, static = false, compared = true)
        }

        assertFalse(callCursor.hasNext())
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

@Suppress("UNUSED_PARAMETER")
@Composable
fun OneParameter(a: Int) {
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun OneDefaultParameter(a: Int = 1) {
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun ThreeParameters(a: Int, b: Int, c: Int) {
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun ThreeDefaultParameters(a: Int = 1, b: Int = 2, c: Int = 3) {
}

// BFS
internal fun Group.firstOrNull(predicate: (Group) -> Boolean): Group? {
    val stack = mutableListOf(this)
    while (stack.isNotEmpty()) {
        val next = stack.removeAt(0)
        if (predicate(next)) return next
        stack.addAll(next.children)
    }
    return null
}

internal fun Group.asList(): List<Group> {
    val result = mutableListOf<Group>()
    val stack = mutableListOf(this)
    while (stack.isNotEmpty()) {
        val next = stack.removeAt(stack.size - 1)
        result.add(next)
        stack.addAll(next.children.reversed())
    }
    return result
}

internal fun SlotTableRecord.findGroupForFile(fileName: String) =
    store.map { it.findGroupForFile(fileName) }.filterNotNull().firstOrNull()

fun SlotTable.findGroupForFile(fileName: String) = asTree().findGroupForFile(fileName)
fun Group.findGroupForFile(fileName: String): Group? {
    val position = position
    if (position != null && position.contains(fileName)) return this
    return children.map { it.findGroupForFile(fileName) }.filterNotNull().firstOrNull()
}
