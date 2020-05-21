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

package androidx.ui.layout.test

import android.widget.FrameLayout
import androidx.compose.Composable
import androidx.compose.Recomposer
import androidx.compose.State
import androidx.compose.emptyContent
import androidx.compose.mutableStateOf
import androidx.test.filters.SmallTest
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.core.onChildPositioned
import androidx.ui.core.onPositioned
import androidx.ui.core.positionInParent
import androidx.ui.core.positionInRoot
import androidx.ui.core.setContent
import androidx.ui.layout.Row
import androidx.ui.layout.Stack
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.padding
import androidx.ui.unit.Dp
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.min
import androidx.ui.unit.px
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class OnPositionedTest : LayoutTest() {

    @Test
    fun simplePadding() = with(density) {
        val paddingLeftPx = 100.0f
        val paddingTopPx = 120.0f
        var realLeft: Float? = null
        var realTop: Float? = null

        val positionedLatch = CountDownLatch(1)
        show {
            Container(
                Modifier.fillMaxSize()
                    .padding(start = paddingLeftPx.toDp(), top = paddingTopPx.toDp())
                    .onPositioned {
                        realLeft = it.positionInParent.x
                        realTop = it.positionInParent.y
                        positionedLatch.countDown()
                    }
            ) {
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertThat(paddingLeftPx).isEqualTo(realLeft)
        assertThat(paddingTopPx).isEqualTo(realTop)
    }

    @Test
    fun simplePaddingWithChildPositioned() = with(density) {
        val paddingLeftPx = 100.px
        val paddingTopPx = 120.px
        var realLeft: Float? = null
        var realTop: Float? = null

        val positionedLatch = CountDownLatch(1)
        show {
            Container(Modifier.onChildPositioned {
                realLeft = it.positionInParent.x
                realTop = it.positionInParent.y
                positionedLatch.countDown()
            }) {
                Container(
                    Modifier.fillMaxSize()
                        .padding(
                            start = paddingLeftPx.toDp(),
                            top = paddingTopPx.toDp()
                        ),
                    children = emptyContent()
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertThat(realLeft?.px).isEqualTo(paddingLeftPx)
        assertThat(realTop?.px).isEqualTo(paddingTopPx)
    }

    @Test
    fun nestedLayoutCoordinates() = with(density) {
        val firstPaddingPx = 10.px
        val secondPaddingPx = 20.px
        val thirdPaddingPx = 30.px
        var gpCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null

        val positionedLatch = CountDownLatch(2)
        show {
            Container(
                Modifier.padding(start = firstPaddingPx.toDp()) +
                    Modifier.onPositioned {
                        gpCoordinates = it
                        positionedLatch.countDown()
                    }
            ) {
                Container(Modifier.padding(start = secondPaddingPx.toDp())) {
                    Container(
                        Modifier.fillMaxSize()
                            .padding(start = thirdPaddingPx.toDp())
                            .onPositioned {
                                childCoordinates = it
                                positionedLatch.countDown()
                            }
                    ) {
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        // global position
        val gPos = childCoordinates!!.localToGlobal(PxPosition.Origin).x
        assertThat(gPos).isEqualTo((firstPaddingPx + secondPaddingPx + thirdPaddingPx).value)
        // Position in grandparent Px(value=50.0)
        val gpPos = gpCoordinates!!.childToLocal(childCoordinates!!, PxPosition.Origin).x
        assertThat(gpPos).isEqualTo((secondPaddingPx + thirdPaddingPx).value)
        // local position
        assertThat(childCoordinates!!.positionInParent.x).isEqualTo(thirdPaddingPx.value)
    }

    @Test
    fun childPositionedForTwoContainers() = with(density) {
        val size = 100.0f
        val sizeDp = size.toDp()
        var firstCoordinates: LayoutCoordinates? = null
        var secondCoordinates: LayoutCoordinates? = null

        val positionedLatch = CountDownLatch(2)
        show {
            Row(Modifier.onChildPositioned {
                if (firstCoordinates == null) {
                    firstCoordinates = it
                } else {
                    secondCoordinates = it
                }
                positionedLatch.countDown()
            }) {
                Container(width = sizeDp, height = sizeDp, children = emptyContent())
                Container(width = sizeDp, height = sizeDp, children = emptyContent())
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertThat(0.0f).isEqualTo(firstCoordinates!!.positionInParent.x)
        assertThat(size).isEqualTo(secondCoordinates!!.positionInParent.x)
    }

    @Test
    fun globalCoordinatesAreInActivityCoordinates() = with(density) {
        val padding = 30
        val localPosition = PxPosition.Origin
        val globalPosition = PxPosition(padding.ipx, padding.ipx)
        var realGlobalPosition: PxPosition? = null
        var realLocalPosition: PxPosition? = null

        val positionedLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread(object : Runnable {
            override fun run() {
                val frameLayout = FrameLayout(activity)
                frameLayout.setPadding(padding, padding, padding, padding)
                activity.setContentView(frameLayout)

                frameLayout.setContent(Recomposer.current()) {
                    Container(
                        Modifier.onPositioned {
                            realGlobalPosition = it.localToGlobal(localPosition)
                            realLocalPosition = it.globalToLocal(globalPosition)
                            positionedLatch.countDown()
                        },
                        expanded = true,
                        children = emptyContent()
                    )
                }
            }
        })
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        assertThat(realGlobalPosition).isEqualTo(globalPosition)
        assertThat(realLocalPosition).isEqualTo(localPosition)
    }

    @Test
    fun justAddedOnPositionedCallbackFiredWithoutLayoutChanges() = with(density) {
        val needCallback = mutableStateOf(false)

        val positionedLatch = CountDownLatch(1)
        show {
            val modifier = if (needCallback.value) {
                Modifier.onPositioned { positionedLatch.countDown() }
            } else {
                Modifier
            }
            Container(modifier, expanded = true) { }
        }

        activityTestRule.runOnUiThread { needCallback.value = true }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun testRepositionTriggersCallback() {
        val left = mutableStateOf(30.dp)
        var realLeft: Float? = null

        var positionedLatch = CountDownLatch(1)
        show {
            Stack {
                Container(
                    Modifier.onPositioned {
                        realLeft = it.positionInParent.x
                        positionedLatch.countDown()
                    }
                        .fillMaxSize()
                        .padding(start = left.value),
                    children = emptyContent()
                )
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        positionedLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { left.value = 40.dp }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        with(density) {
            assertThat(realLeft).isEqualTo(40.dp.toPx())
        }
    }

    @Test
    fun testGrandParentRepositionTriggersChildrenCallback() {
        // when we reposition any parent layout is causes the change in global
        // position of all the children down the tree(for example during the scrolling).
        // children should be able to react on this change.
        val left = mutableStateOf(20.dp)
        var realLeft: Float? = null
        var positionedLatch = CountDownLatch(1)
        show {
            Stack {
                Offset(left) {
                    Container(width = 10.dp, height = 10.dp) {
                        Container(width = 10.dp, height = 10.dp) {
                            Container(
                                Modifier.onPositioned {
                                    realLeft = it.positionInRoot.x
                                    positionedLatch.countDown()
                                },
                                width = 10.dp,
                                height = 10.dp
                            ) {
                            }
                        }
                    }
                }
            }
        }
        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))

        positionedLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { left.value = 40.dp }

        assertTrue(positionedLatch.await(1, TimeUnit.SECONDS))
        with(density) {
            assertThat(realLeft).isEqualTo(40.dp.toPx())
        }
    }

    @Test
    fun testAlignmentLinesArePresent() {
        val latch = CountDownLatch(1)
        val line = VerticalAlignmentLine(::min)
        val lineValue = 10.ipx
        show {
            val onPositioned = Modifier.onPositioned { coordinates: LayoutCoordinates ->
                Assert.assertEquals(1, coordinates.providedAlignmentLines.size)
                Assert.assertEquals(lineValue, coordinates[line])
                latch.countDown()
            }
            Layout(modifier = onPositioned, children = { }) { _, _, _ ->
                layout(0.ipx, 0.ipx, mapOf(line to lineValue)) { }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Composable
    private fun Offset(sizeModel: State<Dp>, children: @Composable () -> Unit) {
        // simple copy of Padding which doesn't recompose when the size changes
        Layout(children) { measurables, constraints, _ ->
            layout(constraints.maxWidth, constraints.maxHeight) {
                measurables.first().measure(constraints).place(
                    sizeModel.value.toPx().roundToInt().ipx,
                    0.ipx)
            }
        }
    }
}
