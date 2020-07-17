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
import androidx.compose.foundation.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.ui.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.layout.fillMaxSize
import androidx.ui.layout.rtl
import androidx.ui.test.GestureScope
import androidx.ui.test.assertIsEqualTo
import androidx.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.ui.test.assertTopPositionInRootIsEqualTo
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.center
import androidx.ui.test.createComposeRule
import androidx.ui.test.performGesture
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.globalBounds
import androidx.ui.test.runOnIdle
import androidx.ui.test.runOnUiThread
import androidx.ui.test.click
import androidx.ui.test.swipe
import androidx.ui.test.swipeDown
import androidx.ui.test.swipeLeft
import androidx.ui.test.swipeRight
import androidx.ui.test.swipeUp
import androidx.ui.unit.dp
import androidx.ui.unit.height
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
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(DrawerState.Opened, {}, drawerContent = {
                Box(Modifier.fillMaxSize().testTag("content"))
            }, bodyContent = emptyContent())
        }

        onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(0.dp)
    }

    @Test
    fun modalDrawer_testOffset_whenClosed() {
        var position: Offset? = null
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(DrawerState.Closed, {}, drawerContent = {
                Box(Modifier.fillMaxSize().onPositioned { coords: LayoutCoordinates ->
                    position = coords.localToRoot(Offset.Zero)
                })
            }, bodyContent = emptyContent())
        }

        val width = rootWidth()
        composeTestRule.runOnIdleWithDensity {
            position!!.x.toDp().assertIsEqualTo(-width)
        }
    }

    @Test
    fun modalDrawer_testEndPadding_whenOpened() {
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(DrawerState.Opened, {}, drawerContent = {
                Box(Modifier.fillMaxSize().testTag("content"))
            }, bodyContent = emptyContent())
        }

        onNodeWithTag("content")
            .assertWidthIsEqualTo(rootWidth() - 56.dp)
    }

    @Test
    fun bottomDrawer_testOffset_whenOpened() {
        composeTestRule.setMaterialContent {
            BottomDrawerLayout(BottomDrawerState.Opened, {}, drawerContent = {
                Box(Modifier.fillMaxSize().testTag("content"))
            }, bodyContent = emptyContent())
        }

        val width = rootWidth()
        val height = rootHeight()
        val expectedHeight = if (width > height) 0.dp else (height / 2)
        onNodeWithTag("content")
            .assertTopPositionInRootIsEqualTo(expectedHeight)
    }

    @Test
    fun bottomDrawer_testOffset_whenClosed() {
        var position: Offset? = null
        composeTestRule.setMaterialContent {
            BottomDrawerLayout(BottomDrawerState.Closed, {}, drawerContent = {
                Box(Modifier.fillMaxSize().onPositioned { coords: LayoutCoordinates ->
                    position = coords.localToRoot(Offset.Zero)
                })
            }, bodyContent = emptyContent())
        }

        val height = rootHeight()
        composeTestRule.runOnIdleWithDensity {
            position!!.y.toDp().assertIsEqualTo(height)
        }
    }

    @Test
    @Ignore("failing in postsubmit, fix in b/148751721")
    fun modalDrawer_openAndClose() {
        var contentWidth: Int? = null
        var openedLatch: CountDownLatch? = null
        var closedLatch: CountDownLatch? = CountDownLatch(1)
        val drawerState = mutableStateOf(DrawerState.Closed)
        composeTestRule.setMaterialContent {
            ModalDrawerLayout(drawerState.value, { drawerState.value = it },
                drawerContent = {
                    Box(
                        Modifier.fillMaxSize().onPositioned { info: LayoutCoordinates ->
                            val pos = info.localToGlobal(Offset.Zero)
                            if (pos.x == 0.0f) {
                                // If fully opened, mark the openedLatch if present
                                openedLatch?.countDown()
                            } else if (-pos.x.roundToInt() == contentWidth) {
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
        runOnIdle {
            drawerState.value = DrawerState.Opened
        }
        // Then the drawer should be opened
        assertThat(openedLatch.await(5, TimeUnit.SECONDS)).isTrue()

        // When the drawer state is set to Closed
        closedLatch = CountDownLatch(1)
        runOnIdle {
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
        onNodeWithTag("Drawer").performGesture { click() }

        runOnIdle {
            assertThat(drawerClicks).isEqualTo(0)
            assertThat(bodyClicks).isEqualTo(1)

            drawerState.value = DrawerState.Opened
        }
        sleep(100) // TODO(147586311): remove this sleep when opening the drawer triggers a wait

        // Click on the left-center pixel of the drawer
        onNodeWithTag("Drawer").performGesture {
            val left = 1.0f
            val centerY = (globalBounds.height / 2)
            click(Offset(left, centerY))
        }

        runOnIdle {
            assertThat(drawerClicks).isEqualTo(1)
            assertThat(bodyClicks).isEqualTo(1)
        }
    }

    @Test
    @Ignore("failing in postsubmit, fix in b/148751721")
    fun bottomDrawer_openAndClose() {
        var contentHeight: Int? = null
        var openedHeight: Int? = null
        var openedLatch: CountDownLatch? = null
        var closedLatch: CountDownLatch? = CountDownLatch(1)
        val drawerState = mutableStateOf(BottomDrawerState.Closed)
        composeTestRule.setMaterialContent {
            BottomDrawerLayout(drawerState.value, { drawerState.value = it },
                drawerContent = {
                    Box(Modifier.fillMaxSize().onPositioned { info: LayoutCoordinates ->
                        val pos = info.localToGlobal(Offset.Zero)
                        if (pos.y.roundToInt() == openedHeight) {
                            // If fully opened, mark the openedLatch if present
                            openedLatch?.countDown()
                        } else if (pos.y.roundToInt() == contentHeight) {
                            // If fully closed, mark the closedLatch if present
                            closedLatch?.countDown()
                        }
                    })
                },
                bodyContent = {
                    Box(Modifier.fillMaxSize().onPositioned {
                        contentHeight = it.size.height
                        openedHeight = (it.size.height * BottomDrawerOpenFraction).roundToInt()
                    })
                }
            )
        }
        // Drawer should start in closed state
        assertThat(closedLatch!!.await(5, TimeUnit.SECONDS)).isTrue()

        // When the drawer state is set to Opened
        openedLatch = CountDownLatch(1)
        runOnIdle {
            drawerState.value = BottomDrawerState.Opened
        }
        // Then the drawer should be opened
        assertThat(openedLatch.await(5, TimeUnit.SECONDS)).isTrue()

        // When the drawer state is set to Closed
        closedLatch = CountDownLatch(1)
        runOnIdle {
            drawerState.value = BottomDrawerState.Closed
        }
        // Then the drawer should be closed
        assertThat(closedLatch.await(5, TimeUnit.SECONDS)).isTrue()
    }

    @Test
    fun bottomDrawer_bodyContent_clickable() {
        var drawerClicks = 0
        var bodyClicks = 0
        val drawerState = mutableStateOf(BottomDrawerState.Closed)
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
        onNodeWithTag("Drawer").performGesture { click() }

        runOnIdle {
            assertThat(drawerClicks).isEqualTo(0)
            assertThat(bodyClicks).isEqualTo(1)
        }

        runOnUiThread {
            drawerState.value = BottomDrawerState.Opened
        }
        sleep(100) // TODO(147586311): remove this sleep when opening the drawer triggers a wait

        // Click on the bottom-center pixel of the drawer
        onNodeWithTag("Drawer").performGesture {
            val bounds = globalBounds
            val centerX = bounds.width / 2
            val bottom = bounds.height - 1.0f
            click(Offset(centerX, bottom))
        }

        assertThat(drawerClicks).isEqualTo(1)
        assertThat(bodyClicks).isEqualTo(1)
    }

    @Test
    fun modalDrawer_openBySwipe() {
        val drawerState = mutableStateOf(DrawerState.Closed)
        composeTestRule.setMaterialContent {
            // emulate click on the screen
            Box(Modifier.testTag("Drawer")) {
                ModalDrawerLayout(drawerState.value, { drawerState.value = it },
                    drawerContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                    },
                    bodyContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Red))
                    })
            }
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeRight() }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(DrawerState.Opened)
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeLeft() }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(DrawerState.Closed)
        }
    }

    @Test
    fun modalDrawer_openBySwipe_rtl() {
        val drawerState = mutableStateOf(DrawerState.Closed)
        composeTestRule.setMaterialContent {
            // emulate click on the screen
            Box(Modifier.testTag("Drawer").rtl) {
                ModalDrawerLayout(drawerState.value, { drawerState.value = it },
                    drawerContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                    },
                    bodyContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Red))
                    })
            }
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeLeft() }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(DrawerState.Opened)
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeRight() }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(DrawerState.Closed)
        }
    }

    @Test
    fun bottomDrawer_openBySwipe() {
        val drawerState = mutableStateOf(BottomDrawerState.Closed)
        composeTestRule.setMaterialContent {
            // emulate click on the screen
            Box(Modifier.testTag("Drawer")) {
                BottomDrawerLayout(drawerState.value, { drawerState.value = it },
                    drawerContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Magenta))
                    },
                    bodyContent = {
                        Box(Modifier.fillMaxSize().background(color = Color.Red))
                    })
            }
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeUp() }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerState.Expanded)
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeDown() }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerState.Closed)
        }
    }

    @Test
    fun bottomDrawer_openBySwipe_thresholds() {
        val drawerState = mutableStateOf(BottomDrawerState.Closed)
        composeTestRule.setMaterialContent {
            // emulate click on the screen
            Box(Modifier.testTag("Drawer")) {
                BottomDrawerLayout(drawerState.value, { drawerState.value = it },
                    drawerContent = {
                        Box(Modifier.fillMaxSize().background(Color.Magenta))
                    },
                    bodyContent = {
                        Box(Modifier.fillMaxSize().background(Color.Red))
                    })
            }
        }
        val threshold = with (composeTestRule.density) { BottomDrawerThreshold.toPx() }

        onNodeWithTag("Drawer")
            .performGesture { swipeUpBy(threshold / 2) }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerState.Closed)
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeUpBy(threshold) }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerState.Opened)
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeUpBy(threshold / 2) }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerState.Opened)
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeUpBy(threshold) }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerState.Expanded)
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeDownBy(threshold / 2) }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerState.Expanded)
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeDownBy(threshold) }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerState.Opened)
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeDownBy(threshold / 2) }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerState.Opened)
        }

        onNodeWithTag("Drawer")
            .performGesture { swipeDownBy(threshold) }

        runOnIdle {
            assertThat(drawerState.value).isEqualTo(BottomDrawerState.Closed)
        }
    }

    private fun GestureScope.swipeUpBy(offset: Float) {
        swipe(center, center.copy(y = center.y - offset))
    }

    private fun GestureScope.swipeDownBy(offset: Float) {
        swipe(center, center.copy(y = center.y + offset))
    }
}
