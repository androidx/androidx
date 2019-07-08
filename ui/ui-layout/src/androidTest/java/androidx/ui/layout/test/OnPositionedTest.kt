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
import androidx.compose.compose
import androidx.compose.composer
import androidx.test.filters.SmallTest
import androidx.ui.core.CraneWrapper
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.OnPositioned
import androidx.ui.core.Px
import androidx.ui.core.PxPosition
import androidx.ui.core.ipx
import androidx.ui.core.px
import androidx.ui.core.withDensity
import androidx.ui.layout.Container
import androidx.ui.layout.Padding
import androidx.ui.layout.Row
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class OnPositionedTest : LayoutTest() {

    @Test
    fun simplePadding() = withDensity(density) {
        val paddingLeftPx = 100.px
        val paddingTopPx = 120.px
        var realLeft: Px? = null
        var realTop: Px? = null

        val drawLatch = CountDownLatch(1)
        show {
            Padding(left = paddingLeftPx.toDp(), top = paddingTopPx.toDp()) {
                Container(expanded = true) {
                    OnPositioned(onPositioned = {
                        realLeft = it.position.x
                        realTop = it.position.y
                        drawLatch.countDown()
                    })
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        assertThat(paddingLeftPx).isEqualTo(realLeft)
        assertThat(paddingTopPx).isEqualTo(realTop)
    }

    @Test
    fun simplePaddingWithChildPositioned() = withDensity(density) {
        val paddingLeftPx = 100.px
        val paddingTopPx = 120.px
        var realLeft: Px? = null
        var realTop: Px? = null

        val drawLatch = CountDownLatch(1)
        show {
            Padding(left = paddingLeftPx.toDp(), top = paddingTopPx.toDp()) {
                OnChildPositioned(onPositioned = {
                    realLeft = it.position.x
                    realTop = it.position.y
                    drawLatch.countDown()
                }) {
                    Container(expanded = true) {}
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        assertThat(realLeft).isEqualTo(paddingLeftPx)
        assertThat(realTop).isEqualTo(paddingTopPx)
    }

    @Test
    fun nestedLayoutCoordinates() = withDensity(density) {
        val firstPaddingPx = 10.px
        val secondPaddingPx = 20.px
        val thirdPaddingPx = 30.px
        var gpCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null

        val drawLatch = CountDownLatch(2)
        show {
            Padding(left = firstPaddingPx.toDp()) {
                Padding(left = secondPaddingPx.toDp()) {
                    OnPositioned(onPositioned = {
                        gpCoordinates = it
                        drawLatch.countDown()
                    })
                    Padding(left = thirdPaddingPx.toDp()) {
                        Container(expanded = true) {
                            OnPositioned(onPositioned = {
                                childCoordinates = it
                                drawLatch.countDown()
                            })
                        }
                    }
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        // global position
        val gPos = childCoordinates!!.localToGlobal(PxPosition.Origin).x
        assertThat(gPos).isEqualTo(firstPaddingPx + secondPaddingPx + thirdPaddingPx)
        // Position in grandparent Px(value=50.0)
        val gpPos = gpCoordinates!!.childToLocal(childCoordinates!!, PxPosition.Origin).x
        assertThat(gpPos).isEqualTo(secondPaddingPx + thirdPaddingPx)
        // local position
        assertThat(childCoordinates!!.position.x).isEqualTo(thirdPaddingPx)
    }

    @Test
    fun childPositionedForTwoContainers() = withDensity(density) {
        val size = 100.px
        val sizeDp = size.toDp()
        var firstCoordinates: LayoutCoordinates? = null
        var secondCoordinates: LayoutCoordinates? = null

        val drawLatch = CountDownLatch(2)
        show {
            Row {
                OnChildPositioned(onPositioned = {
                    firstCoordinates = it
                    drawLatch.countDown()
                }) {
                    Container(width = sizeDp, height = sizeDp) {}
                }
                OnChildPositioned(onPositioned = {
                    secondCoordinates = it
                    drawLatch.countDown()
                }) {
                    Container(width = sizeDp, height = sizeDp) {}
                }
            }
        }
        drawLatch.await(1, TimeUnit.SECONDS)

        assertThat(0.px).isEqualTo(firstCoordinates!!.position.x)
        assertThat(size).isEqualTo(secondCoordinates!!.position.x)
    }

    @Test
    fun globalCoordinatesAreInActivityCoordinates() = withDensity(density) {
        val padding = 30
        val localPosition = PxPosition.Origin
        val globalPosition = PxPosition(padding.ipx, padding.ipx)
        var realGlobalPosition: PxPosition? = null
        var realLocalPosition: PxPosition? = null

        val drawLatch = CountDownLatch(1)
        activityTestRule.runOnUiThread(object : Runnable {
            override fun run() {
                val frameLayout = FrameLayout(activity)
                frameLayout.setPadding(padding, padding, padding, padding)
                activity.setContentView(frameLayout)
                frameLayout.compose @Composable {
                    CraneWrapper {
                        OnChildPositioned(onPositioned = {
                            realGlobalPosition = it.localToGlobal(localPosition)
                            realLocalPosition = it.globalToLocal(globalPosition)
                            drawLatch.countDown()
                        }) {
                            Container(expanded = true) {}
                        }
                    }
                }
            }
        })
        drawLatch.await(1, TimeUnit.SECONDS)

        assertThat(realGlobalPosition).isEqualTo(globalPosition)
        assertThat(realLocalPosition).isEqualTo(localPosition)
    }
}
