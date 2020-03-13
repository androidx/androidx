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

package androidx.ui.core.test

import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.boundsInRoot
import androidx.ui.core.drawLayer
import androidx.ui.core.globalBounds
import androidx.ui.core.globalPosition
import androidx.ui.core.onPositioned
import androidx.ui.core.positionInRoot
import androidx.ui.core.setContent
import androidx.ui.framework.test.TestActivity
import androidx.ui.unit.PxBounds
import androidx.ui.unit.PxPosition
import androidx.ui.unit.height
import androidx.ui.unit.ipx
import androidx.ui.unit.px
import androidx.ui.unit.width
import org.junit.Assert.assertEquals
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
class DrawLayerTest {
    @get:Rule
    val activityTestRule = ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )

    private lateinit var activity: TestActivity
    private lateinit var positionLatch: CountDownLatch
    private lateinit var layoutCoordinates: LayoutCoordinates

    private val positioner = onPositioned {
        layoutCoordinates = it
        positionLatch.countDown()
    }

    @Before
    fun setup() {
        activity = activityTestRule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        positionLatch = CountDownLatch(1)
    }

    @Test
    fun testLayerBoundsPosition() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                FixedSize(30.ipx, PaddingModifier(10.ipx) + drawLayer() + positioner) {
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            assertEquals(PxPosition(10.px, 10.px), layoutCoordinates.positionInRoot)
            val bounds = layoutCoordinates.boundsInRoot
            assertEquals(PxBounds(10.px, 10.px, 40.px, 40.px), bounds)
            val global = layoutCoordinates.globalBounds
            val position = layoutCoordinates.globalPosition
            assertEquals(position.x, global.left)
            assertEquals(position.y, global.top)
            assertEquals(30.px, global.width)
            assertEquals(30.px, global.height)
        }
    }

    @Test
    fun testScale() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(10.ipx) {
                    FixedSize(
                        10.ipx,
                        drawLayer(scaleX = 2f, scaleY = 3f) + positioner
                    ) {
                    }
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            val bounds = layoutCoordinates.boundsInRoot
            assertEquals(PxBounds(5.px, 0.px, 25.px, 30.px), bounds)
            assertEquals(PxPosition(5.px, 0.px), layoutCoordinates.positionInRoot)
        }
    }

    @Test
    fun testRotation() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(10.ipx) {
                    FixedSize(
                        10.ipx,
                        drawLayer(scaleY = 3f, rotationZ = 90f) + positioner
                    ) {
                    }
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            val bounds = layoutCoordinates.boundsInRoot
            assertEquals(PxBounds(0.px, 10.px, 30.px, 20.px), bounds)
            assertEquals(PxPosition(30.px, 10.px), layoutCoordinates.positionInRoot)
        }
    }

    @Test
    fun testClip() {
        activityTestRule.runOnUiThreadIR {
            activity.setContent {
                Padding(10.ipx) {
                    FixedSize(10.ipx, drawLayer(clipToBounds = true)) {
                        FixedSize(
                            10.ipx,
                            drawLayer(scaleX = 2f) + positioner
                        ) {
                        }
                    }
                }
            }
        }

        assertTrue(positionLatch.await(1, TimeUnit.SECONDS))
        activity.runOnUiThread {
            val bounds = layoutCoordinates.boundsInRoot
            assertEquals(PxBounds(10.px, 10.px, 20.px, 20.px), bounds)
            // Positions aren't clipped
            assertEquals(PxPosition(5.px, 10.px), layoutCoordinates.positionInRoot)
        }
    }
}