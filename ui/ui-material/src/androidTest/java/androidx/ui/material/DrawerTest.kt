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

package androidx.ui.material

import android.os.SystemClock.sleep
import androidx.compose.emptyContent
import androidx.compose.mutableStateOf
import androidx.test.filters.MediumTest
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.onPositioned
import androidx.ui.core.testTag
import androidx.ui.foundation.Box
import androidx.ui.foundation.clickable
import androidx.ui.layout.fillMaxSize
import androidx.ui.test.createComposeRule
import androidx.ui.test.doGesture
import androidx.ui.test.findByTag
import androidx.ui.test.globalBounds
import androidx.ui.test.runOnIdleCompose
import androidx.ui.test.runOnUiThread
import androidx.ui.test.sendClick
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.height
import androidx.ui.unit.px
import androidx.ui.unit.round
import androidx.ui.unit.width
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@MediumTest
@RunWith(JUnit4::class)
class DrawerTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun modalDrawer_testOffset_whenOpened() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(DrawerState.Opened, {}, drawerContent = {
                Box(Modifier.fillMaxSize().onPositioned { coords: LayoutCoordinates ->
                    position = coords.localToGlobal(PxPosition.Origin)
                })
            }, bodyContent = emptyContent())
        }
        runOnIdleCompose {
            assertThat(position!!.x).isEqualTo(0f)
        }
    }

    @Test
    fun modalDrawer_testOffset_whenClosed() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(DrawerState.Closed, {}, drawerContent = {
                Box(Modifier.fillMaxSize().onPositioned { coords: LayoutCoordinates ->
                    position = coords.localToGlobal(PxPosition.Origin)
                })
            }, bodyContent = emptyContent())
        }
        val width = composeTestRule.displayMetrics.widthPixels
        runOnIdleCompose {
            assertThat(position!!.x.px.round().value).isEqualTo(-width)
        }
    }

    @Test
    fun modalDrawer_testEndPadding_whenOpened() {
        var size: IntPxSize? = null
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(DrawerState.Opened, {}, drawerContent = {
                Box(Modifier.fillMaxSize().onPositioned { coords: LayoutCoordinates ->
                    size = coords.size
                })
            }, bodyContent = emptyContent())
        }

        val width = composeTestRule.displayMetrics.widthPixels
        composeTestRule.runOnIdleComposeWithDensity {
            assertThat(size!!.width.value)
                .isEqualTo(width - 56.dp.toPx().roundToInt())
        }
    }

    @Test
    fun bottomDrawer_testOffset_whenOpened() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            BottomDrawerLayout(DrawerState.Opened, {}, drawerContent = {
                Box(Modifier.fillMaxSize().onPositioned { coords: LayoutCoordinates ->
                    position = coords.localToGlobal(PxPosition.Origin)
                })
            }, bodyContent = emptyContent())
        }

        val width = composeTestRule.displayMetrics.widthPixels
        val height = composeTestRule.displayMetrics.heightPixels
        // temporary calculation of landscape screen
        val expectedHeight = if (width > height) 0 else (height / 2f).roundToInt()
        runOnIdleCompose {
            assertThat(position!!.y.px.round().value).isEqualTo(expectedHeight)
        }
    }

    @Test
    fun bottomDrawer_testOffset_whenClosed() {
        var position: PxPosition? = null
        composeTestRule.setMaterialContent {
            BottomDrawerLayout(DrawerState.Closed, {}, drawerContent = {
                Box(Modifier.fillMaxSize().onPositioned { coords: LayoutCoordinates ->
                    position = coords.localToGlobal(PxPosition.Origin)
                })
            }, bodyContent = emptyContent())
        }
        val height = composeTestRule.displayMetrics.heightPixels
        runOnIdleCompose {
            assertThat(position!!.y.px.round().value).isEqualTo(height)
        }
    }

    @Test
    @Ignore("failing in postsubmit, fix in b/148751721")
    fun modalDrawer_openAndClose() {
        var contentWidth: IntPx? = null
        var openedLatch: CountDownLatch? = null
        var closedLatch: CountDownLatch? = CountDownLatch(1)
        val drawerState = mutableStateOf(DrawerState.Closed)
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(drawerState.value, { drawerState.value = it },
                drawerContent = {
                    Box(
                        Modifier.fillMaxSize().onPositioned { info: LayoutCoordinates ->
                            val pos = info.localToGlobal(PxPosition.Origin)
                            if (pos.x == 0.0f) {
                                // If fully opened, mark the openedLatch if present
                                openedLatch?.countDown()
                            } else if (-pos.x.px.round() == contentWidth) {
                                // If fully closed, mark the closedLatch if present
                                closedLatch?.countDown()
                            }
                        }
                    )
                },
                bodyContent = {
                    Box(Modifier.fillMaxSize()
                        .onPositioned { contentWidth = it.size.width })
                })
        }
        // Drawer should start in closed state
        assertThat(closedLatch!!.await(5, TimeUnit.SECONDS)).isTrue()

        // When the drawer state is set to Opened
        openedLatch = CountDownLatch(1)
        runOnIdleCompose {
            drawerState.value = DrawerState.Opened
        }
        // Then the drawer should be opened
        assertThat(openedLatch.await(5, TimeUnit.SECONDS)).isTrue()

        // When the drawer state is set to Closed
        closedLatch = CountDownLatch(1)
        runOnIdleCompose {
            drawerState.value = DrawerState.Closed
        }
        // Then the drawer should be closed
        assertThat(closedLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun modalDrawer_bodyContent_clickable() {
        var drawerClicks = 0
        var bodyClicks = 0
        val drawerState = mutableStateOf(DrawerState.Closed)
        composeTestRule.setMaterialContent {
            // emulate click on the screen
            ModalDrawerLayout(drawerState.value, { drawerState.value = it },
                drawerContent = {
                    Box(
                        Modifier.fillMaxSize().clickable { drawerClicks += 1 },
                        children = emptyContent()
                    )
                },
                bodyContent = {
                    Box(
                        Modifier.testTag("Drawer").fillMaxSize().clickable { bodyClicks += 1 },
                        children = emptyContent()
                    )
                })
        }

        // Click in the middle of the drawer (which is the middle of the body)
        findByTag("Drawer").doGesture { sendClick() }

        runOnIdleCompose {
            assertThat(drawerClicks).isEqualTo(0)
            assertThat(bodyClicks).isEqualTo(1)

            drawerState.value = DrawerState.Opened
        }
        sleep(100) // TODO(147586311): remove this sleep when opening the drawer triggers a wait

        // Click on the left-center pixel of the drawer
        findByTag("Drawer").doGesture {
            val left = 1.0f
            val centerY = (globalBounds.height / 2)
            sendClick(PxPosition(left, centerY))
        }

        runOnIdleCompose {
            assertThat(drawerClicks).isEqualTo(1)
            assertThat(bodyClicks).isEqualTo(1)
        }
    }

    @Test
    @Ignore("failing in postsubmit, fix in b/148751721")
    fun bottomDrawer_openAndClose() {
        var contentHeight: IntPx? = null
        var openedHeight: IntPx? = null
        var openedLatch: CountDownLatch? = null
        var closedLatch: CountDownLatch? = CountDownLatch(1)
        val drawerState = mutableStateOf(DrawerState.Closed)
        composeTestRule.setMaterialContent {
            BottomDrawerLayout(drawerState.value, { drawerState.value = it },
                drawerContent = {
                    Box(Modifier.fillMaxSize().onPositioned { info: LayoutCoordinates ->
                        val pos = info.localToGlobal(PxPosition.Origin)
                        if (pos.y.px.round() == openedHeight) {
                            // If fully opened, mark the openedLatch if present
                            openedLatch?.countDown()
                        } else if (pos.y.px.round() == contentHeight) {
                            // If fully closed, mark the closedLatch if present
                            closedLatch?.countDown()
                        }
                    })
                },
                bodyContent = {
                    Box(Modifier.fillMaxSize().onPositioned {
                        contentHeight = it.size.height
                        openedHeight = it.size.height * BottomDrawerOpenFraction
                    })
                }
            )
        }
        // Drawer should start in closed state
        assertThat(closedLatch!!.await(5, TimeUnit.SECONDS)).isTrue()

        // When the drawer state is set to Opened
        openedLatch = CountDownLatch(1)
        runOnIdleCompose {
            drawerState.value = DrawerState.Opened
        }
        // Then the drawer should be opened
        assertThat(openedLatch.await(5, TimeUnit.SECONDS)).isTrue()

        // When the drawer state is set to Closed
        closedLatch = CountDownLatch(1)
        runOnIdleCompose {
            drawerState.value = DrawerState.Closed
        }
        // Then the drawer should be closed
        assertThat(closedLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun bottomDrawer_bodyContent_clickable() {
        var drawerClicks = 0
        var bodyClicks = 0
        val drawerState = mutableStateOf(DrawerState.Closed)
        composeTestRule.setMaterialContent {
            // emulate click on the screen
            BottomDrawerLayout(drawerState.value, { drawerState.value = it },
                drawerContent = {
                    Box(
                        Modifier.fillMaxSize().clickable { drawerClicks += 1 },
                        children = emptyContent()
                    )
                },
                bodyContent = {
                    Box(
                        Modifier.testTag("Drawer").fillMaxSize().clickable { bodyClicks += 1 },
                        children = emptyContent()
                    )
                })
        }

        // Click in the middle of the drawer (which is the middle of the body)
        findByTag("Drawer").doGesture { sendClick() }

        runOnIdleCompose {
            assertThat(drawerClicks).isEqualTo(0)
            assertThat(bodyClicks).isEqualTo(1)
        }

        runOnUiThread {
            drawerState.value = DrawerState.Opened
        }
        sleep(100) // TODO(147586311): remove this sleep when opening the drawer triggers a wait

        // Click on the bottom-center pixel of the drawer
        findByTag("Drawer").doGesture {
            val bounds = globalBounds
            val centerX = bounds.width / 2
            val bottom = bounds.height - 1.0f
            sendClick(PxPosition(centerX, bottom))
        }

        assertThat(drawerClicks).isEqualTo(1)
        assertThat(bodyClicks).isEqualTo(1)
    }
}
