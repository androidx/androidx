/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.solidColor
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.screenshot.rule.RemoteComposeScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
class RemoteCanvasTest {
    @get:Rule
    val remoteComposeTestRule: RemoteComposeScreenshotTestRule by lazy {
        RemoteComposeScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            matcher = MSSIMMatcher(threshold = 0.9995),
        )
    }

    private val tests =
        listOf<@Composable () -> Unit>(
            ::TestDrawAnchoredText_colorAndTextSize,
            ::TestDrawAnchoredText_brushAndTextSize,
            ::TestDrawAnchoredText_colorExpression,
            ::TestClipRect_intersect,
            ::TestClipRect_difference,
            ::TestDrawPrimitives,
        )

    @Test
    fun test() {
        val chunkedTests = tests.chunked(3)
        remoteComposeTestRule.runScreenshotTest {
            RemoteColumn {
                for (testRow in chunkedTests) {
                    RemoteRow {
                        for (testItem in testRow) {
                            Container { testItem() }
                            RemoteBox(modifier = RemoteModifier.width(Padding))
                        }
                    }
                    RemoteBox(modifier = RemoteModifier.height(Padding))
                }
            }
        }
    }

    @RemoteComposable
    @Composable
    fun TestDrawAnchoredText_colorAndTextSize() {
        val text = "Hello".rs
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val w = remoteWidth
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 40f.rf,
                paint =
                    RemotePaint().apply {
                        color = Color.Red.toArgb()
                        textSize = SMALL_FONT_SIZE
                    },
            )
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 80f.rf,
                paint =
                    RemotePaint().apply {
                        color = Color.Green.toArgb()
                        textSize = MEDIUM_FONT_SIZE
                    },
            )
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 120f.rf,
                paint =
                    RemotePaint().apply {
                        color = Color.Blue.toArgb()
                        textSize = LARGE_FONT_SIZE
                    },
            )
        }
    }

    @RemoteComposable
    @Composable
    fun TestDrawPrimitives() {
        RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
            val paint = RemotePaint().apply { color = Color.Red.toArgb() }
            drawRect(paint = paint)
            drawCircle(
                paint = RemotePaint().apply { color = Color.Blue.toArgb() },
                center = RemoteOffset(50f.rf, 50f.rf),
                radius = 40f.rf,
            )
            drawArc(
                paint = RemotePaint().apply { color = Color.Green.toArgb() },
                startAngle = 0f.rf,
                sweepAngle = 90f.rf,
                useCenter = true,
                topLeft = RemoteOffset(10f.rf, 10f.rf),
                size = RemoteSize(80f.rf, 80f.rf),
            )
            drawLine(
                paint =
                    RemotePaint().apply {
                        color = Color.Yellow.toArgb()
                        strokeWidth = 5f
                    },
                start = RemoteOffset(0f.rf, 0f.rf),
                end = RemoteOffset(100f.rf, 100f.rf),
            )
        }
    }

    @RemoteComposable
    @Composable
    fun TestDrawAnchoredText_brushAndTextSize() {
        val text = RemoteString("Hello")
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val w = remoteWidth
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 40f.rf,
                paint =
                    RemotePaint().apply {
                        applyRemoteBrush(RemoteBrush.solidColor(Color.Red.rc), remoteSize)
                        textSize = SMALL_FONT_SIZE
                    },
            )
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 80f.rf,
                paint =
                    RemotePaint().apply {
                        applyRemoteBrush(RemoteBrush.solidColor(Color.Green.rc), remoteSize)
                        textSize = MEDIUM_FONT_SIZE
                    },
            )
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 120f.rf,
                paint =
                    RemotePaint().apply {
                        applyRemoteBrush(RemoteBrush.solidColor(Color.Blue.rc), remoteSize)
                        textSize = LARGE_FONT_SIZE
                    },
            )
        }
    }

    @RemoteComposable
    @Composable
    fun TestDrawAnchoredText_colorExpression() {
        val color = RemoteColor.fromARGB(0.9f.rf.createReference(), 0.8f.rf, 0.9f.rf, 0.9f.rf)
        val text = "Visible Hello".rs
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val w = remoteWidth
            drawAnchoredText(
                text = text,
                anchorX = w / 2f,
                anchorY = 40f.rf,
                paint =
                    RemotePaint().apply {
                        remoteColor = color
                        textSize = SMALL_FONT_SIZE
                    },
            )
        }
    }

    @RemoteComposable
    @Composable
    fun TestClipRect_intersect() {
        ClipRectTest(clipOp = ClipOp.Intersect)
    }

    @RemoteComposable
    @Composable
    fun TestClipRect_difference() {
        // It generates the same output as Intersect: b/464257438
        ClipRectTest(clipOp = ClipOp.Difference)
    }

    @RemoteComposable
    @Composable
    private fun ClipRectTest(clipOp: ClipOp) {
        RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
            val clipRect1Left = 20f.rf
            val clipRect1Top = 20f.rf
            val clipRect1Right = 60f.rf
            val clipRect1Bottom = 60f.rf

            val clipRect2Left = 40f.rf
            val clipRect2Top = 40f.rf
            val clipRect2Right = 80f.rf
            val clipRect2Bottom = 80f.rf

            clipRect(
                left = clipRect1Left,
                top = clipRect1Top,
                right = clipRect1Right,
                bottom = clipRect1Bottom,
            ) {
                clipRect(
                    left = clipRect2Left,
                    top = clipRect2Top,
                    right = clipRect2Right,
                    bottom = clipRect2Bottom,
                    clipOp = clipOp,
                ) {
                    drawRect(paint = RemotePaint().apply { color = Color.Red.toArgb() })
                }
            }
        }
    }

    @Composable
    @RemoteComposable
    private fun Container(
        modifier: RemoteModifier = RemoteModifier,
        content: @Composable () -> Unit,
    ) {
        RemoteBox(
            modifier = modifier.size(ContainerSize).background(ContainerColor),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
            verticalArrangement = RemoteArrangement.Center,
            content = content,
        )
    }

    private companion object {
        val Padding = 24.rdp
        val ContainerSize = 100.rdp
        val ContainerColor = Color(0xFFCFD8DC.toInt()).rc
        const val SMALL_FONT_SIZE = 16f
        const val MEDIUM_FONT_SIZE = 32f
        const val LARGE_FONT_SIZE = 48f
    }
}
