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
package androidx.ui.core.test

import androidx.compose.Composable
import androidx.compose.emptyContent
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.Layout
import androidx.ui.core.LayoutTagParentData
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.drawBehind
import androidx.ui.core.setContent
import androidx.ui.core.tag
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.unit.ipx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class ParentDataModifierTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )
    private lateinit var activity: TestActivity
    private lateinit var drawLatch: CountDownLatch

    @Before
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        drawLatch = CountDownLatch(1)
    }

    // Test that parent data defaults to null
    @Test
    fun parentDataDefaultsToNull() {
        val parentData = Ref<Any?>()
        runOnUiThread {
            activity.setContent {
                Layout(children = {
                    SimpleDrawChild(drawLatch = drawLatch)
                }, measureBlock = { measurables, constraints, _ ->
                    assertEquals(1, measurables.size)
                    parentData.value = measurables[0].parentData

                    val placeable = measurables[0].measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0.ipx, 0.ipx)
                    }
                })
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertNull(parentData.value)
    }

    // Test that parent data doesn't flow to grandchild measurables. They must be
    // reset on every Layout level
    @Test
    fun parentDataIsReset() {
        val parentData = Ref<Any?>()
        runOnUiThread {
            activity.setContent {
                Layout(
                    modifier = Modifier.tag("Hello"),
                    children = {
                        SimpleDrawChild(drawLatch = drawLatch)
                    },
                    measureBlock = { measurables, constraints, _ ->
                        assertEquals(1, measurables.size)
                        parentData.value = measurables[0].parentData

                        val placeable = measurables[0].measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0.ipx, 0.ipx)
                        }
                    }
                )
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertNull(parentData.value)
    }

    @Test
    fun multiChildLayoutTest_doesNotOverrideChildrenParentData() {
        runOnUiThread {
            activity.setContent {
                val header = @Composable {
                    Layout(
                        modifier = Modifier.tag(0),
                        children = emptyContent()
                    ) { _, _, _ -> layout(0.ipx, 0.ipx) {} }
                }
                val footer = @Composable {
                    Layout(
                        modifier = Modifier.tag(1),
                        children = emptyContent()
                    ) { _, _, _ -> layout(0.ipx, 0.ipx) {} }
                }

                Layout({ header(); footer() }) { measurables, _, _ ->
                    assertEquals(0, ((measurables[0]).parentData as? LayoutTagParentData)?.tag)
                    assertEquals(1, ((measurables[1]).parentData as? LayoutTagParentData)?.tag)
                    layout(0.ipx, 0.ipx) { }
                }
            }
        }
    }

    // We only need this because IR compiler doesn't like converting lambdas to Runnables
    private fun runOnUiThread(block: () -> Unit) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                block()
            }
        }
        activityTestRule.runOnUiThread(runnable)
    }
}

@Composable
fun SimpleDrawChild(drawLatch: CountDownLatch) {
    AtLeastSize(size = 10.ipx, modifier = Modifier.drawBehind {
        drawRect(Color(0xFF008000))
        drawLatch.countDown()
    }) {}
}
