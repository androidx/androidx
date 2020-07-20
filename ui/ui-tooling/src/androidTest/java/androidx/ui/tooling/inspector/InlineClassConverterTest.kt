/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.tooling.inspector

import androidx.test.filters.SmallTest
import androidx.compose.foundation.Text
import androidx.ui.graphics.Color
import androidx.ui.material.Button
import androidx.ui.material.Surface
import androidx.ui.tooling.Group
import androidx.ui.tooling.Inspectable
import androidx.ui.tooling.SlotTableRecord
import androidx.ui.tooling.ToolingTest
import androidx.ui.tooling.asTree
import androidx.ui.tooling.position
import androidx.ui.unit.Dp
import androidx.ui.unit.TextUnit
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class InlineClassConverterTest : ToolingTest() {

    @Test
    fun parameterValueTest() {
        val slotTableRecord = SlotTableRecord.create()
        show {
            Inspectable(slotTableRecord) {
                Surface {
                    Button(onClick = {}) {
                        Text(text = "OK", fontSize = TextUnit.Sp(12))
                    }
                }
            }
        }

        val tree = slotTableRecord.store.first().asTree()
        val groups = flatten(tree)
        val surface = find(groups, "Surface")
        val button = find(groups, "Button")
        val text = find(groups, "Text")

        val mapper = InlineClassConverter()

        fun validate(function: String, caller: Group, parameterName: String, valueType: Class<*>) {
            val callee = caller.children.first { it.position?.contains(function) == true }
            val functionName = callee.position!!.substringBefore(" (")
            val parameter = caller.parameters.single { it.name == parameterName }
            val value = mapper.castParameterValue(functionName, parameterName, parameter.value)
            assertThat(value).isInstanceOf(valueType)
        }

        validate("Surface", surface, "color", Color::class.java)
        validate("Surface", surface, "elevation", Dp::class.java)
        validate("Button", button, "backgroundColor", Color::class.java)
        validate("Button", button, "elevation", Dp::class.java)
        validate("Text", text, "color", Color::class.java)
        validate("Text", text, "fontSize", TextUnit::class.java)
    }

    private fun flatten(group: Group): Sequence<Group> =
        sequenceOf(group).plus(group.children.asSequence().flatMap { flatten(it) })

    private fun find(groups: Sequence<Group>, calleeName: String) =
        groups.first {
            it.parameters.isNotEmpty() &&
                    it.position?.contains(InlineClassConverterTest::class.java.name) ?: false &&
                    it.children.any { callee -> callee.position?.contains(calleeName) ?: false }
        }
}
