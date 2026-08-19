/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.player.compose.embedded

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteClock
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remotePath
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.vector.RemotePathScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertWithMessage
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Unit tests for path building DSL and operations rendered with the embedded [RcPlayer]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class RcPlayerPathBuildingTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    private fun renderPlayerToBitmap(
        widthDp: Dp = 100.dp,
        heightDp: Dp = 100.dp,
        content: @Composable @RemoteComposable () -> Unit,
    ): Bitmap {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bytes = runBlocking {
            captureSingleRemoteDocument(context = context, content = content).bytes
        }
        val document =
            CoreDocument(RemoteClock.SYSTEM).apply {
                ByteArrayInputStream(bytes).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        rule.setContent {
            Box(modifier = Modifier.size(widthDp, heightDp).testTag("playerBox")) {
                RcPlayer(document = document)
            }
        }
        rule.waitForIdle()
        return rule.onNodeWithTag("playerBox").captureToImage().asAndroidBitmap()
    }

    private fun Bitmap.getPixelAt(xDp: Int, yDp: Int): Int {
        val d = rule.density.density
        val px = (xDp * d).toInt().coerceIn(0, width - 1)
        val py = (yDp * d).toInt().coerceIn(0, height - 1)
        return getPixel(px, py)
    }

    private fun isRed(pixel: Int): Boolean =
        AndroidColor.red(pixel) > 200 &&
            AndroidColor.green(pixel) < 60 &&
            AndroidColor.blue(pixel) < 60

    private fun isWhite(pixel: Int): Boolean =
        AndroidColor.red(pixel) > 200 &&
            AndroidColor.green(pixel) > 200 &&
            AndroidColor.blue(pixel) > 200

    private fun Bitmap.assertRedAt(xDp: Int, yDp: Int, label: String = "") {
        val pixel = getPixelAt(xDp, yDp)
        assertWithMessage(
                "Expected RED at ($xDp, $yDp) [$label] (got 0x${Integer.toHexString(pixel)})"
            )
            .that(isRed(pixel))
            .isTrue()
    }

    private fun Bitmap.assertWhiteAt(xDp: Int, yDp: Int, label: String = "") {
        val pixel = getPixelAt(xDp, yDp)
        assertWithMessage(
                "Expected WHITE at ($xDp, $yDp) [$label] (got 0x${Integer.toHexString(pixel)})"
            )
            .that(isWhite(pixel))
            .isTrue()
    }

    @Test
    fun drawPath_linesAndMove_rendersClosedPolygon() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val triangle = remotePath {
                        moveTo(50f.rf, 10f.rf)
                        lineTo(90f.rf, 90f.rf)
                        lineTo(10f.rf, 90f.rf)
                        close()
                    }
                    drawPath(
                        path = triangle,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // Interior centroid and base points
        bitmap.assertRedAt(50, 40, "triangle interior top")
        bitmap.assertRedAt(50, 60, "triangle interior centroid")
        bitmap.assertRedAt(50, 80, "triangle interior bottom")
        bitmap.assertRedAt(25, 85, "triangle interior left base")
        bitmap.assertRedAt(75, 85, "triangle interior right base")

        // Exterior points around the triangle
        bitmap.assertWhiteAt(10, 10, "outside top-left")
        bitmap.assertWhiteAt(90, 10, "outside top-right")
        bitmap.assertWhiteAt(50, 5, "outside above apex")
        bitmap.assertWhiteAt(50, 95, "outside below base")
        bitmap.assertWhiteAt(5, 95, "outside bottom-left")
        bitmap.assertWhiteAt(95, 95, "outside bottom-right")
    }

    @Test
    fun drawPath_relativeLines_rendersRectangle() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val rect = remotePath {
                        moveTo(20f.rf, 20f.rf)
                        relativeHorizontalTo(60f.rf)
                        relativeVerticalTo(60f.rf)
                        relativeHorizontalLineTo((-60f).rf)
                        close()
                    }
                    drawPath(
                        path = rect,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // All 4 inner quadrants and center
        bitmap.assertRedAt(50, 50, "rect center")
        bitmap.assertRedAt(25, 25, "rect top-left inner")
        bitmap.assertRedAt(75, 25, "rect top-right inner")
        bitmap.assertRedAt(75, 75, "rect bottom-right inner")
        bitmap.assertRedAt(25, 75, "rect bottom-left inner")

        // Just outside all 4 corners and 4 edges
        bitmap.assertWhiteAt(15, 15, "outside top-left corner")
        bitmap.assertWhiteAt(85, 15, "outside top-right corner")
        bitmap.assertWhiteAt(85, 85, "outside bottom-right corner")
        bitmap.assertWhiteAt(15, 85, "outside bottom-left corner")
        bitmap.assertWhiteAt(50, 15, "outside top edge")
        bitmap.assertWhiteAt(50, 85, "outside bottom edge")
        bitmap.assertWhiteAt(15, 50, "outside left edge")
        bitmap.assertWhiteAt(85, 50, "outside right edge")
    }

    @Test
    fun drawPath_quadraticCurves_rendersFilledShape() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val curve = remotePath {
                        moveTo(10f.rf, 50f.rf)
                        quadraticTo(50f.rf, 10f.rf, 90f.rf, 50f.rf)
                        relativeQuadTo((-40f).rf, 40f.rf, (-80f).rf, 0f.rf)
                        close()
                    }
                    drawPath(
                        path = curve,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // Interior points along both quadratic bulges
        bitmap.assertRedAt(50, 50, "curve center")
        bitmap.assertRedAt(50, 35, "upper quad curve inner apex")
        bitmap.assertRedAt(50, 65, "lower quad curve inner apex")
        bitmap.assertRedAt(30, 50, "left flank inner")
        bitmap.assertRedAt(70, 50, "right flank inner")

        // Exterior points outside the curve bulges and corners
        bitmap.assertWhiteAt(50, 15, "outside above upper quad")
        bitmap.assertWhiteAt(50, 85, "outside below lower quad")
        bitmap.assertWhiteAt(10, 10, "outside top-left corner")
        bitmap.assertWhiteAt(90, 10, "outside top-right corner")
        bitmap.assertWhiteAt(10, 90, "outside bottom-left corner")
        bitmap.assertWhiteAt(90, 90, "outside bottom-right corner")
    }

    @Test
    fun drawPath_cubicCurves_rendersFilledShape() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val curve = remotePath {
                        moveTo(10f.rf, 50f.rf)
                        cubicTo(10f.rf, 10f.rf, 90f.rf, 10f.rf, 90f.rf, 50f.rf)
                        relativeCubicTo(0f.rf, 40f.rf, (-80f).rf, 40f.rf, (-80f).rf, 0f.rf)
                        close()
                    }
                    drawPath(
                        path = curve,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // Interior points along both cubic bulges
        bitmap.assertRedAt(50, 50, "cubic center")
        bitmap.assertRedAt(50, 30, "upper cubic inner bulge")
        bitmap.assertRedAt(50, 70, "lower cubic inner bulge")
        bitmap.assertRedAt(25, 50, "left flank inner")
        bitmap.assertRedAt(75, 50, "right flank inner")

        // Exterior points outside the cubic curves
        bitmap.assertWhiteAt(50, 10, "outside above upper cubic")
        bitmap.assertWhiteAt(50, 90, "outside below lower cubic")
        bitmap.assertWhiteAt(5, 5, "outside top-left corner")
        bitmap.assertWhiteAt(95, 5, "outside top-right corner")
        bitmap.assertWhiteAt(5, 95, "outside bottom-left corner")
        bitmap.assertWhiteAt(95, 95, "outside bottom-right corner")
    }

    @Test
    fun drawPath_conicCurves_rendersFilledShape() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val curve = remotePath {
                        moveTo(10f.rf, 50f.rf)
                        conicTo(50f.rf, 10f.rf, 90f.rf, 50f.rf, 0.707f.rf)
                        relativeConicTo((-40f).rf, 40f.rf, (-80f).rf, 0f.rf, 0.707f.rf)
                        close()
                    }
                    drawPath(
                        path = curve,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // Interior points
        bitmap.assertRedAt(50, 50, "conic center")
        bitmap.assertRedAt(50, 35, "upper conic inner apex")
        bitmap.assertRedAt(50, 65, "lower conic inner apex")

        // Exterior points
        bitmap.assertWhiteAt(50, 15, "outside above upper conic")
        bitmap.assertWhiteAt(50, 85, "outside below lower conic")
        bitmap.assertWhiteAt(5, 5, "outside top-left corner")
        bitmap.assertWhiteAt(95, 5, "outside top-right corner")
        bitmap.assertWhiteAt(5, 95, "outside bottom-left corner")
        bitmap.assertWhiteAt(95, 95, "outside bottom-right corner")
    }

    @Test
    fun drawPath_arcTo_rendersArc() {
        // Bottom semicircle: center (50, 50), radius 40, sweep 180 deg from 0 (right) to 180 (left)
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val arcPath = remotePath {
                        moveTo(50f.rf, 50f.rf)
                        arcTo(
                            left = 10f.rf,
                            top = 10f.rf,
                            right = 90f.rf,
                            bottom = 90f.rf,
                            startAngle = 0f.rf,
                            sweepAngle = 180f.rf,
                            forceMoveTo = false,
                        )
                        close()
                    }
                    drawPath(
                        path = arcPath,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // Interior points in the bottom semicircle
        bitmap.assertRedAt(50, 65, "bottom semicircle center")
        bitmap.assertRedAt(50, 80, "bottom semicircle lower radius")
        bitmap.assertRedAt(35, 65, "bottom semicircle left quadrant")
        bitmap.assertRedAt(65, 65, "bottom semicircle right quadrant")

        // Crucial: The top half of the circle's bounding box MUST remain white
        bitmap.assertWhiteAt(50, 35, "top half of circle box (unswept)")
        bitmap.assertWhiteAt(35, 35, "top-left unswept area")
        bitmap.assertWhiteAt(65, 35, "top-right unswept area")

        // Exterior corners
        bitmap.assertWhiteAt(15, 85, "outside bottom-left corner")
        bitmap.assertWhiteAt(85, 85, "outside bottom-right corner")
        bitmap.assertWhiteAt(5, 5, "outside top-left corner")
        bitmap.assertWhiteAt(95, 5, "outside top-right corner")
    }

    @Test
    fun drawPath_addArc_rendersArc() {
        // Top semicircle: center (50, 50), radius 40, sweep 180 deg from 180 (left) to 360 (right)
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val arcPath = remotePath {
                        addArc(
                            topLeft = RemoteOffset(10f.rf, 10f.rf),
                            size = RemoteSize(80f.rf, 80f.rf),
                            startAngle = 180f.rf,
                            sweepAngle = 180f.rf,
                        )
                        close()
                    }
                    drawPath(
                        path = arcPath,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // Interior points in the top semicircle
        bitmap.assertRedAt(50, 35, "top semicircle center")
        bitmap.assertRedAt(50, 20, "top semicircle upper radius")
        bitmap.assertRedAt(35, 35, "top semicircle left quadrant")
        bitmap.assertRedAt(65, 35, "top semicircle right quadrant")

        // Crucial: The bottom half of the circle's bounding box MUST remain white
        bitmap.assertWhiteAt(50, 65, "bottom half of circle box (unswept)")
        bitmap.assertWhiteAt(35, 65, "bottom-left unswept area")
        bitmap.assertWhiteAt(65, 65, "bottom-right unswept area")

        // Exterior corners
        bitmap.assertWhiteAt(15, 15, "outside top-left corner")
        bitmap.assertWhiteAt(85, 15, "outside top-right corner")
        bitmap.assertWhiteAt(5, 95, "outside bottom-left corner")
        bitmap.assertWhiteAt(95, 95, "outside bottom-right corner")
    }

    @Test
    fun drawPath_addRect_rendersFilledRect() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val path = remotePath {
                        addRect(RemoteOffset(20f.rf, 20f.rf), RemoteSize(60f.rf, 60f.rf))
                    }
                    drawPath(
                        path = path,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // 4 interior corners and center
        bitmap.assertRedAt(50, 50, "rect center")
        bitmap.assertRedAt(22, 22, "rect top-left inner corner")
        bitmap.assertRedAt(78, 22, "rect top-right inner corner")
        bitmap.assertRedAt(22, 78, "rect bottom-left inner corner")
        bitmap.assertRedAt(78, 78, "rect bottom-right inner corner")

        // Just outside 4 corners
        bitmap.assertWhiteAt(18, 18, "outside top-left corner")
        bitmap.assertWhiteAt(82, 18, "outside top-right corner")
        bitmap.assertWhiteAt(18, 82, "outside bottom-left corner")
        bitmap.assertWhiteAt(82, 82, "outside bottom-right corner")

        // Just outside 4 edges
        bitmap.assertWhiteAt(50, 15, "outside top edge")
        bitmap.assertWhiteAt(50, 85, "outside bottom edge")
        bitmap.assertWhiteAt(15, 50, "outside left edge")
        bitmap.assertWhiteAt(85, 50, "outside right edge")
    }

    @Test
    fun drawPath_addOvalAndCircle_rendersFilledCircle() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val path = remotePath { addCircle(RemoteOffset(50f.rf, 50f.rf), 30f.rf) }
                    drawPath(
                        path = path,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // Center and 4 cardinal inner points (radius 25 < 30)
        bitmap.assertRedAt(50, 50, "circle center")
        bitmap.assertRedAt(50, 25, "circle inner top")
        bitmap.assertRedAt(50, 75, "circle inner bottom")
        bitmap.assertRedAt(25, 50, "circle inner left")
        bitmap.assertRedAt(75, 50, "circle inner right")

        // 4 diagonal inner points (distance ~21.2 < 30)
        bitmap.assertRedAt(35, 35, "circle inner top-left diagonal")
        bitmap.assertRedAt(65, 35, "circle inner top-right diagonal")
        bitmap.assertRedAt(35, 65, "circle inner bottom-left diagonal")
        bitmap.assertRedAt(65, 65, "circle inner bottom-right diagonal")

        // Cardinal points just outside circle radius (radius 35 > 30)
        bitmap.assertWhiteAt(50, 15, "outside top")
        bitmap.assertWhiteAt(50, 85, "outside bottom")
        bitmap.assertWhiteAt(15, 50, "outside left")
        bitmap.assertWhiteAt(85, 50, "outside right")

        // 4 bounding box corners
        bitmap.assertWhiteAt(10, 10, "outside top-left")
        bitmap.assertWhiteAt(90, 10, "outside top-right")
        bitmap.assertWhiteAt(10, 90, "outside bottom-left")
        bitmap.assertWhiteAt(90, 90, "outside bottom-right")
    }

    @Test
    fun drawPath_addRoundRect_rendersRoundedRect() {
        // Rect [10, 10, 90, 90] with corner radii (25, 25)
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val path = remotePath {
                        addRoundRect(
                            topLeft = RemoteOffset(10f.rf, 10f.rf),
                            size = RemoteSize(80f.rf, 80f.rf),
                            radiusX = 25f.rf,
                            radiusY = 25f.rf,
                        )
                    }
                    drawPath(
                        path = path,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // Center and 4 inner cross edges
        bitmap.assertRedAt(50, 50, "round rect center")
        bitmap.assertRedAt(50, 15, "round rect inner top edge")
        bitmap.assertRedAt(50, 85, "round rect inner bottom edge")
        bitmap.assertRedAt(15, 50, "round rect inner left edge")
        bitmap.assertRedAt(85, 50, "round rect inner right edge")
        bitmap.assertRedAt(35, 35, "round rect inner body top-left")
        bitmap.assertRedAt(65, 35, "round rect inner body top-right")
        bitmap.assertRedAt(35, 65, "round rect inner body bottom-left")
        bitmap.assertRedAt(65, 65, "round rect inner body bottom-right")

        // 4 extreme corners outside rect bounds
        bitmap.assertWhiteAt(5, 5, "outside top-left corner")
        bitmap.assertWhiteAt(95, 5, "outside top-right corner")
        bitmap.assertWhiteAt(5, 95, "outside bottom-left corner")
        bitmap.assertWhiteAt(95, 95, "outside bottom-right corner")

        // 4 corner regions inside bounding rect [10..90] but clipped off by corner radius 25:
        // Corner arc center is at (35, 35). Distance from (15, 15) to (35, 35) is sqrt(20^2 + 20^2)
        // = 28.3 > 25.
        bitmap.assertWhiteAt(15, 15, "clipped top-left rounded corner")
        bitmap.assertWhiteAt(85, 15, "clipped top-right rounded corner")
        bitmap.assertWhiteAt(15, 85, "clipped bottom-left rounded corner")
        bitmap.assertWhiteAt(85, 85, "clipped bottom-right rounded corner")
    }

    @Test
    fun drawPath_addPath_combinesPaths() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val combined = remotePath {
                        val p1 =
                            RemotePathScope().apply {
                                addRect(RemoteOffset(10f.rf, 10f.rf), RemoteSize(30f.rf, 30f.rf))
                            }
                        val p2 =
                            RemotePathScope().apply {
                                addRect(RemoteOffset(60f.rf, 60f.rf), RemoteSize(30f.rf, 30f.rf))
                            }
                        addPath(p1)
                        addPath(p2)
                    }
                    drawPath(
                        path = combined,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Fill
                            },
                    )
                }
            }
        }

        // Rect 1 [10, 10, 40, 40] interior points
        bitmap.assertRedAt(25, 25, "rect1 center")
        bitmap.assertRedAt(15, 15, "rect1 top-left")
        bitmap.assertRedAt(35, 35, "rect1 bottom-right")

        // Rect 2 [60, 60, 90, 90] interior points
        bitmap.assertRedAt(75, 75, "rect2 center")
        bitmap.assertRedAt(65, 65, "rect2 top-left")
        bitmap.assertRedAt(85, 85, "rect2 bottom-right")

        // Gap between the two rects and other quadrants
        bitmap.assertWhiteAt(50, 50, "middle gap between rects")
        bitmap.assertWhiteAt(25, 75, "bottom-left empty area")
        bitmap.assertWhiteAt(75, 25, "top-right empty area")
        bitmap.assertWhiteAt(5, 5, "top-left outer corner")
        bitmap.assertWhiteAt(95, 95, "bottom-right outer corner")
    }

    @Test
    fun drawPath_remoteOffsetOverloads_rendersSuccessfully() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val path = remotePath {
                        moveTo(RemoteOffset(10f.rf, 10f.rf))
                        lineTo(RemoteOffset(90f.rf, 10f.rf))
                        quadTo(RemoteOffset(90f.rf, 50f.rf), RemoteOffset(50f.rf, 50f.rf))
                        curveTo(
                            RemoteOffset(30f.rf, 50f.rf),
                            RemoteOffset(10f.rf, 70f.rf),
                            RemoteOffset(10f.rf, 90f.rf),
                        )
                        conicTo(
                            RemoteOffset(50f.rf, 90f.rf),
                            RemoteOffset(90f.rf, 90f.rf),
                            0.707f.rf,
                        )
                    }
                    drawPath(
                        path = path,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Stroke
                                strokeWidth = 6f.rf
                            },
                    )
                }
            }
        }

        // Stroked points along the path
        bitmap.assertRedAt(50, 10, "stroke along horizontal line")
        bitmap.assertRedAt(50, 50, "stroke at quad end point")
        bitmap.assertRedAt(10, 75, "stroke along cubic curve")
        bitmap.assertRedAt(80, 90, "stroke along bottom conic curve")

        // Unpainted background points away from strokes
        bitmap.assertWhiteAt(50, 30, "empty area below top line")
        bitmap.assertWhiteAt(50, 70, "empty area above bottom line")
        bitmap.assertWhiteAt(5, 5, "top-left corner")
    }

    @Test
    fun clipPath_withBuiltPath_clipsCanvasDrawing() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val circlePath = remotePath { addCircle(RemoteOffset(50f.rf, 50f.rf), 35f.rf) }
                    clipPath(circlePath) {
                        drawRect(
                            paint =
                                RemotePaint().apply {
                                    color = Color.Red.rc
                                    style = PaintingStyle.Fill
                                },
                            topLeft = RemoteOffset(0f.rf, 0f.rf),
                            size = RemoteSize(100f.rf, 100f.rf),
                        )
                    }
                }
            }
        }

        // Inside clipped circle
        bitmap.assertRedAt(50, 50, "clipped circle center")
        bitmap.assertRedAt(50, 25, "clipped circle top")
        bitmap.assertRedAt(50, 75, "clipped circle bottom")
        bitmap.assertRedAt(25, 50, "clipped circle left")
        bitmap.assertRedAt(75, 50, "clipped circle right")

        // Outside clipped circle (corners of the 100x100 box that were drawn but clipped)
        bitmap.assertWhiteAt(5, 5, "clipped top-left canvas corner")
        bitmap.assertWhiteAt(95, 5, "clipped top-right canvas corner")
        bitmap.assertWhiteAt(5, 95, "clipped bottom-left canvas corner")
        bitmap.assertWhiteAt(95, 95, "clipped bottom-right canvas corner")
        bitmap.assertWhiteAt(15, 15, "clipped outer diagonal top-left")
        bitmap.assertWhiteAt(85, 15, "clipped outer diagonal top-right")
        bitmap.assertWhiteAt(15, 85, "clipped outer diagonal bottom-left")
        bitmap.assertWhiteAt(85, 85, "clipped outer diagonal bottom-right")
    }

    @Test
    fun drawPath_strokedPath_rendersStrokedDiamond() {
        val bitmap = renderPlayerToBitmap {
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.White.rc)) {
                RemoteCanvas(modifier = RemoteModifier.size(100.rdp)) {
                    val diamond = remotePath {
                        moveTo(50f.rf, 10f.rf)
                        lineTo(90f.rf, 50f.rf)
                        lineTo(50f.rf, 90f.rf)
                        lineTo(10f.rf, 50f.rf)
                        close()
                    }
                    drawPath(
                        path = diamond,
                        paint =
                            RemotePaint().apply {
                                color = Color.Red.rc
                                style = PaintingStyle.Stroke
                                strokeWidth = 6f.rf
                            },
                    )
                }
            }
        }

        // Diamond vertices & edge midpoints
        bitmap.assertRedAt(50, 10, "top diamond vertex")
        bitmap.assertRedAt(90, 50, "right diamond vertex")
        bitmap.assertRedAt(50, 90, "bottom diamond vertex")
        bitmap.assertRedAt(10, 50, "left diamond vertex")
        bitmap.assertRedAt(30, 30, "top-left diamond edge")
        bitmap.assertRedAt(70, 30, "top-right diamond edge")
        bitmap.assertRedAt(30, 70, "bottom-left diamond edge")
        bitmap.assertRedAt(70, 70, "bottom-right diamond edge")

        // Interior inside diamond
        bitmap.assertWhiteAt(50, 50, "diamond interior center")

        // 4 outer corners outside diamond
        bitmap.assertWhiteAt(5, 5, "outside top-left corner")
        bitmap.assertWhiteAt(95, 5, "outside top-right corner")
        bitmap.assertWhiteAt(5, 95, "outside bottom-left corner")
        bitmap.assertWhiteAt(95, 95, "outside bottom-right corner")
    }
}
