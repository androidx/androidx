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
import androidx.compose.Model
import androidx.compose.emptyContent
import androidx.test.filters.SmallTest
import androidx.ui.core.Alignment
import androidx.ui.core.Layout
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.VerticalAlignmentLine
import androidx.ui.core.positionInRoot
import androidx.ui.core.setContent
import androidx.ui.layout.Align
import androidx.ui.layout.Center
import androidx.ui.layout.Container
import androidx.ui.layout.LayoutPadding
import androidx.ui.layout.LayoutSize
import androidx.ui.layout.Row
import androidx.ui.test.positionInParent
import androidx.ui.unit.Px
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

@SmallTest
@RunWith(JUnit4::class)
class OnPositionedTest : LayoutTest() {

    @Test
    fun simplePadding() = with(density) {
        val paddingLeftPx = 100.px
        val paddingTopPx = 120.px
        var realLeft: Px? = null
        var realTop: Px? = null

        val positionedLatch = CountDownLatch(1)
        show {
            Container(LayoutSize.Fill +
                    LayoutPadding(start = paddingLeftPx.toDp(), top = paddingTopPx.toDp())
            ) {
                OnPositioned(onPositioned = {
                    realLeft = it.positionInParent.x
                    realTop = it.positionInParent.y
                    positionedLatch.countDown()
                })
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertThat(paddingLeftPx).isEqualTo(realLeft)
        assertThat(paddingTopPx).isEqualTo(realTop)
    }

    @Test
    fun simplePaddingWithChildPositioned() = with(density) {
        val paddingLeftPx = 100.px
        val paddingTopPx = 120.px
        var realLeft: Px? = null
        var realTop: Px? = null

        val positionedLatch = CountDownLatch(1)
        show {
            Container {
                OnChildPositioned(onPositioned = {
                    realLeft = it.positionInParent.x
                    realTop = it.positionInParent.y
                    positionedLatch.countDown()
                }) {
                    Container(
                        LayoutSize.Fill + LayoutPadding(
                            start = paddingLeftPx.toDp(),
                            top = paddingTopPx.toDp()
                        ), children = emptyContent()
                    )
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertThat(realLeft).isEqualTo(paddingLeftPx)
        assertThat(realTop).isEqualTo(paddingTopPx)
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
            Container(LayoutPadding(start = firstPaddingPx.toDp())) {
                OnPositioned(onPositioned = {
                    gpCoordinates = it
                    positionedLatch.countDown()
                })
                Container(LayoutPadding(start = secondPaddingPx.toDp())) {
                    Container(LayoutSize.Fill + LayoutPadding(start = thirdPaddingPx.toDp())) {
                        OnPositioned(onPositioned = {
                            childCoordinates = it
                            positionedLatch.countDown()
                        })
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        // global position
        val gPos = childCoordinates!!.localToGlobal(PxPosition.Origin).x
        assertThat(gPos).isEqualTo(firstPaddingPx + secondPaddingPx + thirdPaddingPx)
        // Position in grandparent Px(value=50.0)
        val gpPos = gpCoordinates!!.childToLocal(childCoordinates!!, PxPosition.Origin).x
        assertThat(gpPos).isEqualTo(secondPaddingPx + thirdPaddingPx)
        // local position
        assertThat(childCoordinates!!.positionInParent.x).isEqualTo(thirdPaddingPx)
    }

    @Test
    fun childPositionedForTwoContainers() = with(density) {
        val size = 100.px
        val sizeDp = size.toDp()
        var firstCoordinates: LayoutCoordinates? = null
        var secondCoordinates: LayoutCoordinates? = null

        val positionedLatch = CountDownLatch(2)
        show {
            Row {
                OnChildPositioned(onPositioned = {
                    firstCoordinates = it
                    positionedLatch.countDown()
                }) {
                    Container(width = sizeDp, height = sizeDp, children = emptyContent())
                }
                OnChildPositioned(onPositioned = {
                    secondCoordinates = it
                    positionedLatch.countDown()
                }) {
                    Container(width = sizeDp, height = sizeDp, children = emptyContent())
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertThat(0.px).isEqualTo(firstCoordinates!!.positionInParent.x)
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

                frameLayout.setContent {
                    OnChildPositioned(onPositioned = {
                        realGlobalPosition = it.localToGlobal(localPosition)
                        realLocalPosition = it.globalToLocal(globalPosition)
                        positionedLatch.countDown()
                    }) {
                        Container(expanded = true, children = emptyContent())
                    }
                }
            }
        })
        positionedLatch.await(1, TimeUnit.SECONDS)

        assertThat(realGlobalPosition).isEqualTo(globalPosition)
        assertThat(realLocalPosition).isEqualTo(localPosition)
    }

    @Test
    fun justAddedOnPositionedCallbackFiredWithoutLayoutChanges() = with(density) {
        val needCallback = NeedCallback(false)

        val positionedLatch = CountDownLatch(1)
        show {
            Container(expanded = true) {
                if (needCallback.value) {
                    OnPositioned(onPositioned = {
                        positionedLatch.countDown()
                    })
                }
            }
        }

        activityTestRule.runOnUiThread { needCallback.value = true }

        assertThat(positionedLatch.await(1, TimeUnit.SECONDS)).isEqualTo(true)
    }

    @Test
    fun testRepositionTriggersCallback() {
        val modelLeft = SizeModel(30.dp)
        var realLeft: Px? = null

        var positionedLatch = CountDownLatch(1)
        show {
            Center {
                OnChildPositioned(onPositioned = {
                    realLeft = it.positionInParent.x
                    positionedLatch.countDown()
                }) {
                    Container(
                        LayoutSize.Fill + LayoutPadding(start = modelLeft.size),
                        children = emptyContent()
                    )
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        positionedLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { modelLeft.size = 40.dp }

        positionedLatch.await(1, TimeUnit.SECONDS)
        with(density) {
            assertThat(realLeft).isEqualTo(40.dp.toPx())
        }
    }

    @Test
    fun testGrandParentRepositionTriggersChildrenCallback() {
        // when we reposition any parent layout is causes the change in global
        // position of all the children down the tree(for example during the scrolling).
        // children should be able to react on this change.
        val modelLeft = SizeModel(20.dp)
        var realLeft: Px? = null
        var positionedLatch = CountDownLatch(1)
        show {
            Align(Alignment.TopLeft) {
                Offset(modelLeft) {
                    Container(width = 10.dp, height = 10.dp) {
                        Container(width = 10.dp, height = 10.dp) {
                            Container(width = 10.dp, height = 10.dp) {
                                OnPositioned(onPositioned = {
                                    realLeft = it.positionInRoot.x
                                    positionedLatch.countDown()
                                })
                            }
                        }
                    }
                }
            }
        }
        positionedLatch.await(1, TimeUnit.SECONDS)

        positionedLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread { modelLeft.size = 40.dp }

        positionedLatch.await(1, TimeUnit.SECONDS)
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
            val onPositioned = @Composable {
                OnPositioned { coordinates ->
                    Assert.assertEquals(1, coordinates.providedAlignmentLines.size)
                    Assert.assertEquals(lineValue, coordinates[line])
                    latch.countDown()
                }
            }
            Layout(onPositioned) { _, _ ->
                layout(0.ipx, 0.ipx, mapOf(line to lineValue)) { }
            }
        }
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Composable
    private fun Offset(sizeModel: SizeModel, children: @Composable() () -> Unit) {
        // simple copy of Padding which doesn't recompose when the size changes
        Layout(children) { measurables, constraints ->
            layout(constraints.maxWidth, constraints.maxHeight) {
                measurables.first().measure(constraints).place(sizeModel.size.toPx(), 0.px)
            }
        }
    }
}

@Model
private data class NeedCallback(var value: Boolean)
