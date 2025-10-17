/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.window.runApplicationTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopWindowInfoTest {

    @Test
    fun windowInfoIsFocused() = runApplicationTest {
        lateinit var window1: ComposeWindow
        lateinit var window2: ComposeWindow
        lateinit var window1Info: WindowInfo
        lateinit var window2Info: WindowInfo

        launchTestApplication {
            Window(onCloseRequest = ::exitApplication) {
                window1 = window
                window1Info = LocalWindowInfo.current
            }

            Window(onCloseRequest = ::exitApplication) {
                window2 = window
                window2Info = LocalWindowInfo.current
            }
        }

        awaitIdle()
        assertThat(window1.isFocused).isEqualTo(window1Info.isWindowFocused)
        assertThat(window2.isFocused).isEqualTo(window2Info.isWindowFocused)

        window1.requestFocus()
        awaitIdle()
        assertThat(window1.isFocused).isEqualTo(window1Info.isWindowFocused)
        assertThat(window2.isFocused).isEqualTo(window2Info.isWindowFocused)

        window2.requestFocus()
        awaitIdle()
        assertThat(window1.isFocused).isEqualTo(window1Info.isWindowFocused)
        assertThat(window2.isFocused).isEqualTo(window2Info.isWindowFocused)
    }

    @Test
    fun windowInfoContainerSizeIsSet() = runApplicationTest {
        lateinit var windowInfo: WindowInfo
        launchTestWindowApplication {
            windowInfo = LocalWindowInfo.current
        }
        awaitIdle()

        val containerSize = windowInfo.containerSize
        assertThat(containerSize.width).isGreaterThan(0)
        assertThat(containerSize.height).isGreaterThan(0)

        val containerDpSize = windowInfo.containerDpSize
        assertThat(containerDpSize.width).isGreaterThan(0.dp)
        assertThat(containerDpSize.height).isGreaterThan(0.dp)
    }

    @Test
    fun windowInfoContainerSize() = runApplicationTest {
        lateinit var windowInfo: WindowInfo
        lateinit var density: Density
        launchTestApplication {
            val state = rememberWindowState(
                size = DpSize(234.dp, 432.dp)
            )
            Window(
                onCloseRequest = {},
                undecorated = true, // To match the size without a title bar.
                state = state
            ) {
                windowInfo = LocalWindowInfo.current
                density = LocalDensity.current
            }
        }
        awaitIdle()

        val containerSize = windowInfo.containerSize
        assertEquals(with(density) { 234.dp.toPx() }.roundToInt(), containerSize.width)
        assertEquals(with(density) { 432.dp.toPx() }.roundToInt(), containerSize.height)

        val containerDpSize = windowInfo.containerDpSize
        assertEquals(234.dp, containerDpSize.width)
        assertEquals(432.dp, containerDpSize.height)
    }
}
