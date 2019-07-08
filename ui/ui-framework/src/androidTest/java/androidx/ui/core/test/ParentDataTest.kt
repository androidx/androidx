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

import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.CraneWrapper
import androidx.ui.core.Draw
import androidx.ui.core.Layout
import androidx.ui.core.ParentData
import androidx.ui.core.Ref
import androidx.ui.core.ipx
import androidx.ui.core.toRect
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
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
class ParentDataTest {
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
                CraneWrapper {
                    Layout(children = {
                        SimpleDrawChild(drawLatch = drawLatch)
                    }, layoutBlock = { measurables, constraints ->
                        assertEquals(1, measurables.size)
                        parentData.value = measurables[0].parentData

                        val placeable = measurables[0].measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0.ipx, 0.ipx)
                        }
                    })
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertNull(parentData.value)
    }

    // Test that parent data can be set to something non-null
    @Test
    fun nonNullParentData() {
        val parentData = Ref<Any?>()
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    Layout(children = {
                        ParentData(data = "Hello") {
                            SimpleDrawChild(drawLatch = drawLatch)
                        }
                    }, layoutBlock = { measurables, constraints ->
                        assertEquals(1, measurables.size)
                        parentData.value = measurables[0].parentData

                        val placeable = measurables[0].measure(constraints)
                        layout(placeable.width, placeable.height) {
                            placeable.place(0.ipx, 0.ipx)
                        }
                    })
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertEquals("Hello", parentData.value)
    }

    // Test that parent data doesn't flow to grandchild measurables. They must be
    // reset on every Layout level
    @Test
    fun parentDataIsReset() {
        val parentData = Ref<Any?>()
        runOnUiThread {
            activity.setContent {
                CraneWrapper {
                    ParentData(data = "Hello") {
                        Layout(children = {
                            SimpleDrawChild(drawLatch = drawLatch)
                        }, layoutBlock = { measurables, constraints ->
                            assertEquals(1, measurables.size)
                            parentData.value = measurables[0].parentData

                            val placeable = measurables[0].measure(constraints)
                            layout(placeable.width, placeable.height) {
                                placeable.place(0.ipx, 0.ipx)
                            }
                        })
                    }
                }
            }
        }
        assertTrue(drawLatch.await(1, TimeUnit.SECONDS))
        assertNull(parentData.value)
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
    AtLeastSize(size = 10.ipx) {
        Draw { canvas, parentSize ->
            val paint = Paint()
            paint.color = Color(0xFF008000.toInt())
            canvas.drawRect(parentSize.toRect(), paint)
            drawLatch.countDown()
        }
    }
}
