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

package androidx.compose.remote.creation.compose.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RecordingRemoteComposeBuffer
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.core.operations.ConditionalOperations
import androidx.compose.remote.core.operations.DrawRect
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.shaders.RemoteLinearShader
import androidx.compose.remote.creation.compose.shaders.RemoteSweepShader
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemoteBlendModeColorFilter
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteBoolean.Companion.createNamedRemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap.Companion.createOffscreenRemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3.Companion.createRotate
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTileMode
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.remote.creation.compose.state.StandardRemotePaint
import androidx.compose.remote.creation.compose.state.lerp
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.test.R
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode.Companion.Multiply
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import com.google.common.truth.Truth.assertThat
import java.text.DecimalFormat
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

private const val WIDTH = 400
private const val HEIGHT = 400

/** Emulator-based screenshot test of [RemoteCanvas]. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemoteCanvasCaptureTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    private val recordingBuffer = RecordingRemoteComposeBuffer()
    private val profileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
    private val profile =
        Profile(CoreDocument.DOCUMENT_API_LEVEL, profileMask, AndroidxRcPlatformServices()) {
            creationDisplayInfo,
            profile,
            callback ->
            RemoteComposeWriter(
                profile,
                recordingBuffer,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, profileMask),
            )
        }

    private val creationState =
        RemoteComposeCreationState(Size(WIDTH.toFloat(), HEIGHT.toFloat()), profile)

    private val remoteCanvas = RemoteCanvas(creationState)

    private val remoteContext = AndroidRemoteContext()
    private val timeZone = ZoneId.of("America/New_York")
    private val clock =
        SystemClock(
            Clock.fixed(
                ZonedDateTime.of(LocalDateTime.of(2025, 11, 20, 10, 30, 25), timeZone).toInstant(),
                timeZone,
            )
        )

    @Test
    fun creationDisplayInfo() {
        assertThat(remoteCanvas.creationDisplayInfo.size.width).isEqualTo(WIDTH.toFloat())
        assertThat(remoteCanvas.creationDisplayInfo.size.height).isEqualTo(HEIGHT.toFloat())
        assertThat(remoteCanvas.creationDisplayInfo.density.density).isEqualTo(1f)
    }

    @Test
    fun remotePaint() {
        val paint = RemotePaint {
            colorFilter =
                RemoteBlendModeColorFilter(
                    Color(0xffffee70).rc,
                    RemoteBlendMode.Multiply,
                )
        }
        val bitmap =
            BitmapFactory.decodeResource(context.resources, R.drawable.android_image)
                .asImageBitmap()
        remoteCanvas.drawBitmap(
            bitmap.rb,
            0f.rf,
            0f.rf,
            bitmap.width.toFloat().rf,
            bitmap.height.toFloat().rf,
            paint,
        )
        val document = constructDocument()
        assertScreenshot(document, "remotePaint")
    }

    @Test
    fun drawConditionally_constantTrue() {
        remoteCanvas.drawConditionally(true.rb) {
            remoteCanvas.drawRect(
                10f.rf,
                10f.rf,
                300f.rf,
                300f.rf,
                RemotePaint { color = Color.Yellow.rc },
            )
        }

        val operations = inflateOperations()
        // Should not contain ConditionalOperations
        assertThat(operations.any { it is ConditionalOperations }).isFalse()
        // Should contain the drawRect
        assertThat(operations.any { it is DrawRect }).isTrue()
    }

    @Test
    fun drawConditionally_constantFalse() {
        remoteCanvas.drawConditionally(false.rb) {
            remoteCanvas.drawRect(
                10f.rf,
                10f.rf,
                300f.rf,
                300f.rf,
                RemotePaint { color = Color.Yellow.rc },
            )
        }

        val operations = inflateOperations()
        // Should not contain ConditionalOperations
        assertThat(operations.any { it is ConditionalOperations }).isFalse()
        // Should NOT contain the drawRect
        assertThat(operations.any { it is DrawRect }).isFalse()
    }

    @Test
    fun drawConditionally_true() {
        val flag = createNamedRemoteBoolean("flag", true)
        val document = constructSimpleConditionalDocument(flag)
        assertScreenshot(document, "drawConditonally_true")
    }

    @Test
    fun drawConditionally_false() {
        val flag = createNamedRemoteBoolean("flag", false)
        val document = constructSimpleConditionalDocument(flag)
        assertScreenshot(document, "drawConditonally_false")
    }

    private fun constructSimpleConditionalDocument(flag: RemoteBoolean): CoreDocument {
        val angle = RemoteTimeVariables.continuousSeconds * 6f % 360.0f
        remoteCanvas.drawConditionally(flag) {
            remoteCanvas.save()
            remoteCanvas.rotate(angle, 150f.rf, 150f.rf)
            remoteCanvas.drawRect(
                10f.rf,
                10f.rf,
                300f.rf,
                300f.rf,
                RemotePaint { color = Color.Yellow.rc },
            )
            remoteCanvas.drawText(
                "True",
                10.rf,
                80.rf,
                RemotePaint {
                    color = Color.Green.rc
                    textSize = 80f.rf
                },
            )
            remoteCanvas.restore()
        }
        remoteCanvas.drawConditionally(!flag) {
            remoteCanvas.save()
            remoteCanvas.rotate(angle, 150f.rf, 150f.rf)
            remoteCanvas.drawRect(
                10f.rf,
                10f.rf,
                300f.rf,
                300f.rf,
                RemotePaint { color = Color.Yellow.rc },
            )

            remoteCanvas.drawText(
                "False",
                10.rf,
                80.rf,
                RemotePaint {
                    color = Color.Red.rc
                    textSize = 80f.rf
                },
            )
            remoteCanvas.restore()
        }

        return constructDocument()
    }

    private class Hues(val hue1: RemoteString, val hue2: RemoteString)

    private fun createConditionalHues(flag: RemoteBoolean): Hues {
        val tweenFactor = RemoteTimeVariables.continuousSeconds / 30f % 1f
        val colorRamp = lerp(Color.Red.rc, Color.Blue.rc, tweenFactor)
        val hue = colorRamp.hue
        val hueString1 = hue.toRemoteString(DecimalFormat("0.00"))
        val hueString2 = RemoteString("hue") + hue.toRemoteString(DecimalFormat("0.00"))
        // Conditional drop shadow.
        remoteCanvas.drawConditionally(flag) {
            remoteCanvas.drawText(
                hueString1,
                10f.rf,
                80f.rf,
                RemotePaint {
                    color = Color.Green.rc
                    textSize = 80f.rf
                },
            )
        }
        remoteCanvas.drawText(
            hueString2,
            12f.rf,
            82f.rf,
            RemotePaint {
                color = Color.Red.rc
                textSize = 80f.rf
            },
        )
        return Hues(hueString1, hueString2)
    }

    @Test
    fun conditionalColorAttribute_true() {
        val flag = createNamedRemoteBoolean("flag", true)
        val hues = createConditionalHues(flag)
        val hueId1 = hues.hue1.getIdForCreationState(creationState)
        val hueId2 = hues.hue2.getIdForCreationState(creationState)
        remoteContext.useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))

        val document = constructDocument()
        document.paint(remoteContext, 0)

        assertThat(remoteContext.getText(hueId1)).isEqualTo("0.75")
        assertThat(remoteContext.getText(hueId2)).isEqualTo("hue0.75")
    }

    @Test
    fun conditionalColorAttribute_false() {
        val flag = createNamedRemoteBoolean("flag", false)
        val hues = createConditionalHues(flag)
        val hueId2 = hues.hue2.getIdForCreationState(creationState)
        remoteContext.useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))

        val document = constructDocument()
        document.paint(remoteContext, 0)

        assertThat(remoteContext.getText(hueId2)).isEqualTo("hue0.75")
    }

    @Test
    fun remotePaintSetColor_constantColor() {
        val paint = RemotePaint()
        paint.color =
            RemoteColor.rgb(
                red = RemoteFloat(0.8f),
                green = RemoteFloat(0.7f),
                blue = RemoteFloat(0.5f),
                alpha = RemoteFloat(1f),
            )

        remoteCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString()).contains("Color(0xffccb380)")
    }

    @Test
    fun remotePaintColorInt() {
        val paint = RemotePaint()
        paint.color = Color.Red.rc

        remoteCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString()).contains("Color(0xffff0000)")
    }

    @Test
    fun paintColorInt() {
        val paint = RemotePaint { color = Color.Red.rc }

        remoteCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString()).contains("Color(0xffff0000)")
    }

    @Test
    fun remotePaintSetColor_colorExpression() {
        val paint = RemotePaint()
        paint.color =
            RemoteColor.rgb(
                RemoteFloat(0.8f),
                RemoteTimeVariables.continuousSeconds,
                RemoteFloat(0.5f),
                RemoteFloat(1f),
            )

        remoteCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString()).containsMatch("ColorId\\(\\[\\d+]\\)")
    }

    @Test
    fun remotePaintSetRemoteColorFilter_constantColor() {
        val paint = RemotePaint()
        paint.colorFilter =
            RemoteBlendModeColorFilter(
                RemoteColor.rgb(
                    RemoteFloat(0.8f),
                    RemoteFloat(0.7f),
                    RemoteFloat(0.5f),
                    RemoteFloat(1f),
                ),
                Multiply,
            )

        remoteCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString())
            .contains("ColorFilter(color=0xffccb380, mode=MULTIPLY)")
    }

    @Test
    fun remotePaintSetRemoteColorFilter_colorExpression() {
        val paint = RemotePaint()
        paint.colorFilter =
            RemoteBlendModeColorFilter(
                RemoteColor.rgb(
                    RemoteFloat(0.8f),
                    RemoteTimeVariables.continuousSeconds,
                    RemoteFloat(0.5f),
                    RemoteFloat(1f),
                ),
                Multiply,
            )

        remoteCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString())
            .containsMatch("ColorFilterID\\(color=\\[\\d+], mode=MULTIPLY\\)")
    }

    @Test
    fun remotePaintSetRemoteColorFilter_clearColorExpression() {
        val paint = RemotePaint()
        paint.colorFilter =
            RemoteBlendModeColorFilter(
                RemoteColor.rgb(
                    RemoteFloat(0.8f),
                    RemoteTimeVariables.continuousSeconds,
                    RemoteFloat(0.5f),
                    RemoteFloat(1f),
                ),
                Multiply,
            )
        remoteCanvas.usePaint(paint)

        remoteCanvas.usePaint(RemotePaint())

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString()).contains("clearColorFilter")
    }

    @Test
    fun remotePaintCopyConstructor() {
        val paint = RemotePaint()
        paint.color =
            RemoteColor.rgb(
                RemoteFloat(0.8f),
                RemoteTimeVariables.continuousSeconds,
                RemoteFloat(0.5f),
                RemoteFloat(1f),
            )
        paint.colorFilter =
            RemoteBlendModeColorFilter(
                RemoteColor.rgb(
                    RemoteFloat(0.8f),
                    RemoteTimeVariables.continuousSeconds,
                    RemoteFloat(0.5f),
                    RemoteFloat(1f),
                ),
                Multiply,
            )

        val paintCopy = StandardRemotePaint(paint)

        assertThat(paintCopy.color).isEqualTo(paint.color)
        assertThat(paintCopy.colorFilter).isEqualTo(paint.colorFilter)
    }

    @Test
    fun drawToOffscreenBitmap() {
        remoteCanvas.drawRect(
            0.rf,
            0.rf,
            WIDTH.rf,
            HEIGHT.rf,
            RemotePaint { color = Color.Black.rc },
        )
        remoteCanvas.drawRect(
            20.rf,
            20.rf,
            (WIDTH - 20).rf,
            (HEIGHT - 20).rf,
            RemotePaint { color = Color.Yellow.rc },
        )
        val bitmap = createOffscreenRemoteBitmap(WIDTH, HEIGHT)
        remoteCanvas.drawToOffscreenBitmap(bitmap, AndroidColor.TRANSPARENT) {
            remoteCanvas.drawOval(
                20.rf,
                20.rf,
                (WIDTH - 20).rf,
                (HEIGHT - 20).rf,
                RemotePaint { color = Color.Red.rc },
            )
            remoteCanvas.drawText(
                "HI",
                20.rf,
                (HEIGHT - 50).rf,
                RemotePaint {
                    textSize = 380f.rf
                    typeface = RemoteTypeface.DefaultBold
                    remoteBlendMode = RemoteBlendMode.Clear
                },
            )
        }
        remoteCanvas.drawBitmap(
            bitmap,
            0f.rf,
            0f.rf,
            WIDTH.rf,
            HEIGHT.rf,
            RemotePaint { remoteBlendMode = RemoteBlendMode.SrcOver },
        )

        val document = constructDocument()
        assertScreenshot(document, "offscreenBitmap")
    }

    @Test
    fun drawRepeatedlyToOffscreenBitmap() {
        remoteCanvas.drawRect(
            0.rf,
            0.rf,
            WIDTH.rf,
            HEIGHT.rf,
            RemotePaint { color = Color.Black.rc },
        )
        remoteCanvas.drawRect(
            20.rf,
            20.rf,
            (WIDTH - 20).rf,
            (HEIGHT - 20).rf,
            RemotePaint { color = Color.Yellow.rc },
        )
        val bitmap = createOffscreenRemoteBitmap(WIDTH, HEIGHT)
        remoteCanvas.drawToOffscreenBitmap(bitmap, AndroidColor.TRANSPARENT) {
            remoteCanvas.drawOval(
                20.rf,
                20.rf,
                (WIDTH - 20).rf,
                (HEIGHT - 20).rf,
                RemotePaint { color = Color.Red.rc },
            )
        }
        remoteCanvas.drawToOffscreenBitmap(bitmap) {
            remoteCanvas.drawText(
                "TEXT",
                20.rf,
                (HEIGHT - 50).rf,
                RemotePaint {
                    textSize = 380f.rf
                    typeface = RemoteTypeface.DefaultBold
                    remoteBlendMode = RemoteBlendMode.Clear
                },
            )
        }
        remoteCanvas.drawBitmap(
            bitmap,
            0f.rf,
            0f.rf,
            WIDTH.rf,
            HEIGHT.rf,
            RemotePaint { remoteBlendMode = RemoteBlendMode.SrcOver },
        )

        val document = constructDocument()
        assertScreenshot(document, "offscreenBitmapRepeated")
    }

    @Test
    fun drawToOffscreenBitmap_nested() {
        remoteCanvas.drawRect(
            0.rf,
            0.rf,
            WIDTH.rf,
            HEIGHT.rf,
            RemotePaint { color = Color.Black.rc },
        )

        // Create the outer offscreen bitmap.
        val outerBitmap = createOffscreenRemoteBitmap(WIDTH, HEIGHT)
        remoteCanvas.drawToOffscreenBitmap(outerBitmap, AndroidColor.TRANSPARENT) {
            // Draw a blue background on the outer bitmap.
            remoteCanvas.drawRect(
                0.rf,
                0.rf,
                WIDTH.rf,
                HEIGHT.rf,
                RemotePaint { color = Color.Blue.rc },
            )

            remoteCanvas.save()

            // Create the inner (nested) offscreen bitmap.
            val innerBitmap = createOffscreenRemoteBitmap(WIDTH / 2, HEIGHT / 2)
            remoteCanvas.drawToOffscreenBitmap(innerBitmap, AndroidColor.TRANSPARENT) {
                // Draw a red circle in the inner bitmap.
                remoteCanvas.drawOval(
                    0f.rf,
                    0f.rf,
                    (WIDTH / 2).rf,
                    (HEIGHT / 2).rf,
                    RemotePaint { color = Color.Red.rc },
                )
            }

            // Draw the inner bitmap onto the outer bitmap. This tests that the canvas context
            // was restored correctly to the outer bitmap's canvas.
            remoteCanvas.drawBitmap(
                innerBitmap,
                100f.rf,
                100f.rf,
                (100 + WIDTH / 2).rf,
                (100 + HEIGHT / 2).rf,
                RemotePaint(),
            )

            // This restore isn't strictly needed, but if we're drawing to the wrong canvas it
            // will lead to an exception.
            remoteCanvas.restore()
        }

        remoteCanvas.drawBitmap(
            outerBitmap,
            0f.rf,
            0f.rf,
            WIDTH.rf,
            HEIGHT.rf,
            RemotePaint(),
        )

        val document = constructDocument()
        assertScreenshot(document, "offscreenBitmap_nested")
    }

    @Test
    fun setShaderMatrixCalledOnce() {
        val remoteShader =
            RemoteSweepShader(
                    100f.rf,
                    100f.rf,
                    listOf(Color.Red.rc, Color.Green.rc, Color.Blue.rc),
                    null,
                )
                .apply { remoteMatrix3x3 = createRotate(90f.rf) }
        val paintWithShader = RemotePaint { shader = remoteShader }
        val paintWithShader2 = RemotePaint {
            shader =
                RemoteLinearShader(
                    10f.rf,
                    100f.rf,
                    200f.rf,
                    200f.rf,
                    listOf(Color.Red.rc, Color.Green.rc, Color.Blue.rc),
                    null,
                    RemoteTileMode.Repeated,
                )
        }
        val paintWithShader3 = RemotePaint {
            shader =
                RemoteLinearShader(
                    10f.rf,
                    100f.rf,
                    100f.rf,
                    200f.rf,
                    listOf(Color.Red.rc, Color.Blue.rc),
                    null,
                    RemoteTileMode.Repeated,
                )
        }
        remoteCanvas.usePaint(paintWithShader)
        remoteCanvas.usePaint(paintWithShader2)
        remoteCanvas.usePaint(paintWithShader3)
        val operations = inflateOperations()

        val shaderMatricies =
            operations
                .filter { it is PaintData }
                .map {
                    val mockPaintContext = mock<PaintContext>()
                    val captor = argumentCaptor<PaintBundle>()
                    (it as PaintData).paint(mockPaintContext)
                    verify(mockPaintContext).applyPaint(captor.capture())

                    captor.lastValue
                        .toString()
                        .split("\n")
                        .filter { it.contains("ShaderMatrix") }
                        .map {
                            val trimmed = it.trim()
                            trimmed.subSequence(0, trimmed.length - 1)
                        }
                }

        assertThat(shaderMatricies.joinToString(","))
            .matches("\\[ShaderMatrix\\(\\[\\d+]\\)],\\[ShaderMatrix\\(0.0\\)],\\[]")
    }

    @Test
    fun drawConditionally_colorFilterState() {
        val flag = createNamedRemoteBoolean("flag", true)

        // 1. Draw something with a color filter
        val paintWithFilter = RemotePaint {
            colorFilter = RemoteBlendModeColorFilter(Color.Red.rc, RemoteBlendMode.SrcIn)
        }
        remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, paintWithFilter)

        // 2. Draw conditionally, without color filter inside
        remoteCanvas.drawConditionally(flag) {
            val paintNoFilter = RemotePaint { color = Color.Blue.rc }
            remoteCanvas.drawRect(10f.rf, 10f.rf, 20f.rf, 20f.rf, paintNoFilter)
        }

        // 3. Draw something after, without color filter
        val paintNoFilterPost = RemotePaint { color = Color.Green.rc }
        remoteCanvas.drawRect(20f.rf, 20f.rf, 30f.rf, 30f.rf, paintNoFilterPost)

        val operations = inflateOperations()
        val paintOps = operations.filterIsInstance<PaintData>()
        assertThat(paintOps.size).isAtLeast(3)

        // The first paint should have a color filter
        assertThat(paintOps[0].mPaintData.toString()).contains("ColorFilter")

        // The second paint (inside conditional) should EXPLICITLY clear the color filter
        // because forceSendingPaint(true) was called before it.
        assertThat(paintOps[1].mPaintData.toString()).contains("clearColorFilter")

        // The third paint (after conditional) should also EXPLICITLY clear the color filter
        // because forceSendingPaint(true) was called after the conditional block.
        assertThat(paintOps[2].mPaintData.toString()).contains("clearColorFilter")
    }

    @Test
    fun drawConditionally_colorFilterState_differentFilterInside() {
        val flag = createNamedRemoteBoolean("flag", true)

        // 1. Draw something with color filter A
        val paintWithFilterA = RemotePaint {
            colorFilter = RemoteBlendModeColorFilter(Color.Red.rc, RemoteBlendMode.SrcIn)
        }
        remoteCanvas.drawRect(0f.rf, 0f.rf, 10f.rf, 10f.rf, paintWithFilterA)

        // 2. Draw conditionally, with color filter B inside
        remoteCanvas.drawConditionally(flag) {
            val paintWithFilterB = RemotePaint {
                colorFilter = RemoteBlendModeColorFilter(Color.Blue.rc, RemoteBlendMode.SrcIn)
            }
            remoteCanvas.drawRect(10f.rf, 10f.rf, 20f.rf, 20f.rf, paintWithFilterB)
        }

        // 3. Draw something after, without color filter
        val paintNoFilterPost = RemotePaint { color = Color.Green.rc }
        remoteCanvas.drawRect(20f.rf, 20f.rf, 30f.rf, 30f.rf, paintNoFilterPost)

        val operations = inflateOperations()
        val paintOps = operations.filterIsInstance<PaintData>()
        assertThat(paintOps.size).isAtLeast(3)

        // The first paint should have color filter A (Red)
        assertThat(paintOps[0].mPaintData.toString()).contains("ColorFilter(color=0xffff0000")

        // The second paint (inside conditional) should have color filter B (Blue)
        assertThat(paintOps[1].mPaintData.toString()).contains("ColorFilter(color=0xff0000ff")

        // The third paint (after conditional) should EXPLICITLY clear the color filter
        // because forceSendingPaint(true) was called after the conditional block.
        assertThat(paintOps[2].mPaintData.toString()).contains("clearColorFilter")
    }

    @Test
    fun patternCrossGrid_golden() {
        val paint = RemotePaint { color = Color.Blue.rc }
        val crossPattern =
            remoteCanvas.definePattern { x: RemoteFloat, y: RemoteFloat, size: RemoteFloat ->
                val halfSize = size * 0.5f
                val halfBar = size * 0.15f
                val radius = halfSize

                // Horizontal rect intersecting circle
                remoteCanvas.save()
                remoteCanvas.clipRect(x - halfSize, y - halfBar, x + halfSize, y + halfBar)
                remoteCanvas.drawCircle(x, y, radius, paint)
                remoteCanvas.restore()

                // Vertical rect intersecting circle
                remoteCanvas.save()
                remoteCanvas.clipRect(x - halfBar, y - halfSize, x + halfBar, y + halfSize)
                remoteCanvas.drawCircle(x, y, radius, paint)
                remoteCanvas.restore()
            }

        val baseSize = 28f
        for (row in 0 until 10) {
            for (col in 0 until 10) {
                val x = 20f + col * 40f
                val y = 20f + row * 40f
                // +/- 25% size variation across the grid
                val variation = -0.25f + 0.5f * ((row + col) % 5) / 4f
                val size = baseSize * (1f + variation)
                crossPattern(x.rf, y.rf, size.rf)
            }
        }

        val document = constructDocument()
        assertScreenshot(document, "patternCrossGrid_golden")
    }

    private fun constructDocument() =
        CoreDocument(clock).apply {
            // Needed because RemoteCanvas buffers up operations to facilitate global CSE &
            // hoisting passes.
            remoteCanvas.flush()
            recordingBuffer.writeToBuffer()
            val buffer = creationState.document.buffer
            buffer.buffer.index = 0
            initFromBuffer(buffer)
        }

    private fun assertScreenshot(document: CoreDocument, filename: String) {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        remoteContext.useCanvas(canvas)
        document.paint(remoteContext, 0)
        bitmap.assertAgainstGolden(screenshotRule, "${this::class.simpleName}_$filename")
    }

    private fun inflateOperations(): ArrayList<Operation> {
        // Needed because RemoteCanvas buffers up operations to facilitate global CSE &
        // hoisting passes.
        remoteCanvas.flush()
        recordingBuffer.writeToBuffer()
        val buffer = creationState.document.buffer
        buffer.buffer.index = 0
        val operations = ArrayList<Operation>()
        buffer.inflateFromBuffer(operations)
        return operations
    }
}
