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
package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutIdModifier
import androidx.compose.ui.layout.LayoutIdParentData
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.node.Ref
import androidx.compose.ui.semantics.elementFor
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ParentDataModifierTest {
    @get:Rule val rule = createAndroidComposeRule<TestActivity>(StandardTestDispatcher())
    private lateinit var activity: TestActivity

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
    }

    // Test that parent data defaults to null
    @Test
    fun parentDataDefaultsToNull() {
        val parentData = Ref<Any?>()
        rule.setContent {
            Layout(
                content = { SimpleDrawChild() },
                measurePolicy = { measurables, constraints ->
                    assertEquals(1, measurables.size)
                    parentData.value = measurables[0].parentData

                    val placeable = measurables[0].measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
            )
        }
        rule.runOnIdle { assertNull(parentData.value) }
    }

    // Test that parent data doesn't flow to grandchild measurables. They must be
    // reset on every Layout level
    @Test
    fun parentDataIsReset() {
        val parentData = Ref<Any?>()
        rule.setContent {
            Layout(
                modifier = Modifier.layoutId("Hello"),
                content = { SimpleDrawChild() },
                measurePolicy = { measurables, constraints ->
                    assertEquals(1, measurables.size)
                    parentData.value = measurables[0].parentData

                    val placeable = measurables[0].measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
            )
        }
        rule.runOnIdle { assertNull(parentData.value) }
    }

    @Test
    fun multiChildLayoutTest_doesNotOverrideChildrenParentData() {
        var parentData0: Any? = null
        var parentData1: Any? = null
        rule.setContent {
            val header =
                @Composable {
                    Layout(modifier = Modifier.layoutId(0), content = {}) { _, _ ->
                        layout(0, 0) {}
                    }
                }
            val footer =
                @Composable {
                    Layout(modifier = Modifier.layoutId(1), content = {}) { _, _ ->
                        layout(0, 0) {}
                    }
                }

            Layout({
                header()
                footer()
            }) { measurables, _ ->
                parentData0 = ((measurables[0]).parentData as? LayoutIdParentData)?.layoutId
                parentData1 = ((measurables[1]).parentData as? LayoutIdParentData)?.layoutId
                layout(0, 0) {}
            }
        }
        rule.runOnIdle {
            assertEquals(0, parentData0)
            assertEquals(1, parentData1)
        }
    }

    @Test
    fun parentDataOnPlaceable() {
        var parentDataValue: Any? = null
        rule.setContent {
            Layout({
                Layout(modifier = Modifier.layoutId("data"), content = {}) { _, _ ->
                    layout(0, 0) {}
                }
            }) { measurables, constraints ->
                val placeable = measurables[0].measure(constraints)
                parentDataValue = (placeable.parentData as? LayoutIdParentData)?.layoutId
                layout(0, 0) {}
            }
        }
        rule.runOnIdle { assertEquals("data", parentDataValue) }
    }

    @Test
    fun delegatedParentData() {
        val node =
            object : DelegatingNode() {
                val pd = delegate(LayoutIdModifier("data"))
            }
        var parentDataValue: Any? = null
        rule.setContent {
            Layout({
                Layout(modifier = Modifier.elementFor(node), content = {}) { _, _ ->
                    layout(0, 0) {}
                }
            }) { measurables, constraints ->
                val placeable = measurables[0].measure(constraints)
                parentDataValue = (placeable.parentData as? LayoutIdParentData)?.layoutId
                layout(0, 0) {}
            }
        }
        rule.runOnIdle { assertEquals("data", parentDataValue) }
    }

    @Test
    fun implementingBothParentDataAndLayoutModifier() {
        val parentData = "data"
        var parentDataValue: Any? = null
        rule.setContent {
            Layout({
                Layout(modifier = ParentDataAndLayoutElement(parentData), content = {}) { _, _ ->
                    layout(0, 0) {}
                }
            }) { measurables, _ ->
                parentDataValue = measurables[0].parentData
                layout(0, 0) {}
            }
        }
        rule.runOnIdle { assertEquals("data", parentDataValue) }
    }
}

@Composable
private fun SimpleDrawChild() {
    AtLeastSize(size = 10, modifier = Modifier.drawBehind { drawRect(Color(0xFF008000)) }) {}
}

private data class ParentDataAndLayoutElement(val data: String) :
    ModifierNodeElement<ParentDataAndLayoutNode>() {
    override fun create() = ParentDataAndLayoutNode(data)

    override fun update(node: ParentDataAndLayoutNode) {
        node.data = data
    }
}

private class ParentDataAndLayoutNode(var data: String) :
    Modifier.Node(), LayoutModifierNode, ParentDataModifierNode {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun Density.modifyParentData(parentData: Any?) = data
}
