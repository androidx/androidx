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

package androidx.compose.foundation

import android.graphics.PorterDuff
import android.os.Build
import android.view.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertPixels
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import kotlin.math.roundToInt
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@RunWith(AndroidJUnit4::class)
class EmbeddedGraphicsSurfaceTest {
    @get:Rule
    val rule = createComposeRule()

    val size = 48.dp

    @Test
    fun testOnSurface() {
        var surfaceRef: Surface? = null
        var surfaceWidth = 0
        var surfaceHeight = 0
        var expectedSize = 0

        rule.setContent {
            expectedSize = with(LocalDensity.current) {
                size.toPx().roundToInt()
            }

            EmbeddedGraphicsSurface(modifier = Modifier.size(size)) {
                onSurface { surface, width, height ->
                    surfaceRef = surface
                    surfaceWidth = width
                    surfaceHeight = height
                }
            }
        }

        rule.onRoot()
            .assertWidthIsEqualTo(size)
            .assertHeightIsEqualTo(size)
            .assertIsDisplayed()

        rule.runOnIdle {
            assertNotNull(surfaceRef)
            assertEquals(expectedSize, surfaceWidth)
            assertEquals(expectedSize, surfaceHeight)
        }
    }

    @Test
    fun testOnSurfaceChanged() {
        var surfaceWidth = 0
        var surfaceHeight = 0
        var expectedSize = 0

        var desiredSize by mutableStateOf(size)

        rule.setContent {
            expectedSize = with(LocalDensity.current) {
                desiredSize.toPx().roundToInt()
            }

            EmbeddedGraphicsSurface(modifier = Modifier.size(desiredSize)) {
                onSurface { surface, initWidth, initHeight ->
                    surfaceWidth = initWidth
                    surfaceHeight = initHeight

                    surface.onChanged { newWidth, newHeight ->
                        surfaceWidth = newWidth
                        surfaceHeight = newHeight
                    }
                }
            }
        }

        rule.onRoot()
            .assertWidthIsEqualTo(desiredSize)
            .assertHeightIsEqualTo(desiredSize)

        rule.runOnIdle {
            assertEquals(expectedSize, surfaceWidth)
            assertEquals(expectedSize, surfaceHeight)
        }

        desiredSize = size * 2
        val prevSurfaceWidth = surfaceWidth
        val prevSurfaceHeight = surfaceHeight

        rule.onRoot()
            .assertWidthIsEqualTo(desiredSize)
            .assertHeightIsEqualTo(desiredSize)

        rule.runOnIdle {
            assertNotEquals(prevSurfaceWidth, surfaceWidth)
            assertNotEquals(prevSurfaceHeight, surfaceHeight)
            assertEquals(expectedSize, surfaceWidth)
            assertEquals(expectedSize, surfaceHeight)
        }
    }

    @Test
    fun testOnSurfaceDestroyed() {
        var surfaceRef: Surface? = null
        var visible by mutableStateOf(true)

        rule.setContent {
            if (visible) {
                EmbeddedGraphicsSurface(modifier = Modifier.size(size)) {
                    onSurface { surface, _, _ ->
                        surfaceRef = surface

                        surface.onDestroyed {
                            surfaceRef = null
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertNotNull(surfaceRef)
        }

        visible = false

        rule.runOnIdle {
            assertNull(surfaceRef)
        }
    }

    @Test
    fun testOnSurfaceRecreated() {
        var surfaceCreatedCount = 0
        var surfaceDestroyedCount = 0
        var visible by mutableStateOf(true)

        // NOTE: TextureView only destroys the surface when TextureView is detached from
        // the window, and only creates when it gets attached to the window
        rule.setContent {
            if (visible) {
                EmbeddedGraphicsSurface(modifier = Modifier.size(size)) {
                    onSurface { surface, _, _ ->
                        surfaceCreatedCount++
                        surface.onDestroyed {
                            surfaceDestroyedCount++
                        }
                    }
                }
            }
        }

        rule.runOnIdle {
            assertEquals(1, surfaceCreatedCount)
            assertEquals(0, surfaceDestroyedCount)
            visible = false
        }

        rule.runOnIdle {
            assertEquals(1, surfaceCreatedCount)
            assertEquals(1, surfaceDestroyedCount)
            visible = true
        }

        rule.runOnIdle {
            assertEquals(2, surfaceCreatedCount)
            assertEquals(1, surfaceDestroyedCount)
        }
    }

    @Test
    fun testRender() {
        var surfaceRef: Surface? = null
        var expectedSize = 0

        rule.setContent {
            expectedSize = with(LocalDensity.current) {
                size.toPx().roundToInt()
            }
            EmbeddedGraphicsSurface(modifier = Modifier.size(size)) {
                onSurface { surface, _, _ ->
                    surfaceRef = surface
                    surface.lockHardwareCanvas().apply {
                        drawColor(Color.Blue.toArgb())
                        surface.unlockCanvasAndPost(this)
                    }
                }
            }
        }

        rule.runOnIdle {
            assertNotNull(surfaceRef)
        }

        surfaceRef!!
            .captureToImage(expectedSize, expectedSize)
            .assertPixels { Color.Blue }
    }

    @Test
    fun testNotOpaque() {
        val translucentRed = Color(1.0f, 0.0f, 0.0f, 0.5f).toArgb()

        rule.setContent {
            Box(modifier = Modifier.size(size)) {
                Canvas(modifier = Modifier.size(size)) {
                    drawRect(Color.White)
                }
                EmbeddedGraphicsSurface(
                    modifier = Modifier
                        .size(size)
                        .testTag("EmbeddedGraphicSurface"),
                    isOpaque = false
                ) {
                    onSurface { surface, _, _ ->
                        surface.lockHardwareCanvas().apply {
                            drawColor(0x00000000, PorterDuff.Mode.CLEAR)
                            drawColor(translucentRed)
                            surface.unlockCanvasAndPost(this)
                        }
                    }
                }
            }
        }

        val expectedColor = Color(ColorUtils.compositeColors(translucentRed, Color.White.toArgb()))

        rule
            .onNodeWithTag("EmbeddedGraphicSurface")
            .captureToImage()
            .assertPixels { expectedColor }
    }

    @Test
    fun testOpaque() {
        rule.setContent {
            Box(modifier = Modifier.size(size)) {
                Canvas(modifier = Modifier.size(size)) {
                    drawRect(Color.Green)
                }
                EmbeddedGraphicsSurface(
                    modifier = Modifier
                        .size(size)
                        .testTag("EmbeddedGraphicSurface")
                ) {
                    onSurface { surface, _, _ ->
                        surface.lockHardwareCanvas().apply {
                            drawColor(Color.Red.toArgb())
                            surface.unlockCanvasAndPost(this)
                        }
                    }
                }
            }
        }

        rule
            .onNodeWithTag("EmbeddedGraphicSurface")
            .captureToImage()
            .assertPixels { Color.Red }
    }
}
