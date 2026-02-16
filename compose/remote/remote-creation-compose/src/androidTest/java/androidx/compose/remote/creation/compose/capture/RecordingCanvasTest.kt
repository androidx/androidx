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
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RecordingRemoteComposeBuffer
import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.SystemClock
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.compose.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.compose.remote.creation.compose.shaders.RemoteLinearShader
import androidx.compose.remote.creation.compose.shaders.RemoteSweepShader
import androidx.compose.remote.creation.compose.state.RemoteBitmap
import androidx.compose.remote.creation.compose.state.RemoteBlendModeColorFilter
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.tween
import androidx.compose.remote.creation.compose.test.R
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.assertAgainstGolden
import com.google.common.truth.Truth.assertThat
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

private const val WIDTH = 400
private const val HEIGHT = 400

/** Emulator-based screenshot test of [RecordingCanvas]. */
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(AndroidJUnit4::class)
@MediumTest
class RecordingCanvasTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    private val recordingBuffer = RecordingRemoteComposeBuffer()
    private val profile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriter(
                profile,
                recordingBuffer,
                RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                RemoteComposeWriter.hTag(Header.DOC_PROFILES, RcProfiles.PROFILE_ANDROIDX),
            )
        }

    private val creationState =
        RemoteComposeCreationState(Size(WIDTH.toFloat(), HEIGHT.toFloat()), profile)

    private val recordingCanvas =
        RecordingCanvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    private val remoteContext = AndroidRemoteContext()
    private val timeZone = ZoneId.of("America/New_York")
    private val clock =
        SystemClock(
            Clock.fixed(
                ZonedDateTime.of(LocalDateTime.of(2025, 11, 20, 10, 30, 25), timeZone).toInstant(),
                timeZone,
            )
        )

    @Before
    fun setUp() {
        recordingCanvas.setRemoteComposeCreationState(creationState)
    }

    @Test
    fun remotePaint() {
        val paint = RemotePaint()
        paint.remoteColorFilter =
            RemoteBlendModeColorFilter(RemoteColor(0xffffee70.toInt()), BlendMode.MULTIPLY)
        val bitmap =
            BitmapFactory.decodeResource(context.getResources(), R.drawable.android_image)
                .asImageBitmap()
        recordingCanvas.drawBitmap(bitmap, 0f, 0f, paint)
        val document = constructDocument()
        assertScreenshot(document, "remotePaint")
    }

    @Test
    fun drawConditionally_true() {
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)
        val document = constructSimpleConditionalDocument(flag)
        assertScreenshot(document, "drawConditonally_true")
    }

    @Test
    fun drawConditionally_false() {
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", false)
        val document = constructSimpleConditionalDocument(flag)
        assertScreenshot(document, "drawConditonally_false")
    }

    private fun constructSimpleConditionalDocument(flag: RemoteBoolean): CoreDocument {
        val angle = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) * 6f % 360.0f
        recordingCanvas.drawConditionally(flag) {
            recordingCanvas.save()
            recordingCanvas.rotate(angle, 150f.rf, 150f.rf)
            recordingCanvas.drawRect(
                10f.rf,
                10f.rf,
                300f.rf,
                300f.rf,
                Paint().apply { color = Color.YELLOW },
            )
            recordingCanvas.drawText(
                "True",
                10.rf,
                80.rf,
                Paint().apply {
                    color = Color.GREEN
                    textSize = 80f
                },
            )
            recordingCanvas.restore()
        }
        recordingCanvas.drawConditionally(!flag) {
            recordingCanvas.save()
            recordingCanvas.rotate(angle, 150f.rf, 150f.rf)
            recordingCanvas.drawRect(
                10f.rf,
                10f.rf,
                300f.rf,
                300f.rf,
                Paint().apply { color = Color.YELLOW },
            )

            recordingCanvas.drawText(
                "False",
                10.rf,
                80.rf,
                Paint().apply {
                    color = Color.RED
                    textSize = 80f
                },
            )
            recordingCanvas.restore()
        }

        return constructDocument()
    }

    private class Hues(val hue1: RemoteString, val hue2: RemoteString)

    private fun createConditionalHues(flag: RemoteBoolean): Hues {
        val tweenFactor = RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC) / 30f % 1f
        val colorRamp = tween(ComposeColor.Red.rc, ComposeColor.Blue.rc, tweenFactor)
        val hue = colorRamp.hue
        val hueString1 = hue.toRemoteString(1)
        val hueString2 = RemoteString("hue") + hue.toRemoteString(1)
        // Conditional drop shadow.
        recordingCanvas.drawConditionally(flag) {
            recordingCanvas.drawText(
                hueString1,
                -1,
                10f.rf,
                80f.rf,
                Paint().apply {
                    color = Color.GREEN
                    textSize = 80f
                },
            )
        }
        recordingCanvas.drawText(
            hueString2,
            -1,
            12f.rf,
            82f.rf,
            Paint().apply {
                color = Color.RED
                textSize = 80f
            },
        )
        return Hues(hueString1, hueString2)
    }

    @Test
    fun conditionalColorAttribute_true() {
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", true)
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
        val flag = RemoteBoolean.createNamedRemoteBoolean("flag", false)
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
        paint.remoteColor =
            RemoteColor.fromARGB(
                RemoteFloat(1f),
                RemoteFloat(0.8f),
                RemoteFloat(0.7f),
                RemoteFloat(0.5f),
            )

        recordingCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString()).contains("Color(0xffccb380)")
    }

    @Test
    fun remotePaintColorInt() {
        val paint = RemotePaint()
        paint.color = Color.RED

        recordingCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString()).contains("Color(0xffff0000)")
    }

    @Test
    fun paintColorInt() {
        val paint = Paint()
        paint.color = Color.RED

        recordingCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString()).contains("Color(0xffff0000)")
    }

    @Test
    fun remotePaintSetColor_colorExpression() {
        val paint = RemotePaint()
        paint.remoteColor =
            RemoteColor.fromARGB(
                RemoteFloat(1f),
                RemoteFloat(0.8f),
                RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
                RemoteFloat(0.5f),
            )

        recordingCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString()).containsMatch("ColorId\\(\\[\\d+]\\)")
    }

    @Test
    fun remotePaintSetRemoteColorFilter_constantColor() {
        val paint = RemotePaint()
        paint.remoteColorFilter =
            RemoteBlendModeColorFilter(
                RemoteColor.fromARGB(
                    RemoteFloat(1f),
                    RemoteFloat(0.8f),
                    RemoteFloat(0.7f),
                    RemoteFloat(0.5f),
                ),
                BlendMode.MULTIPLY,
            )

        recordingCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString())
            .contains("ColorFilter(color=0xffccb380, mode=MULTIPLY)")
    }

    @Test
    fun remotePaintSetRemoteColorFilter_colorExpression() {
        val paint = RemotePaint()
        paint.remoteColorFilter =
            RemoteBlendModeColorFilter(
                RemoteColor.fromARGB(
                    RemoteFloat(1f),
                    RemoteFloat(0.8f),
                    RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
                    RemoteFloat(0.5f),
                ),
                BlendMode.MULTIPLY,
            )

        recordingCanvas.usePaint(paint)

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString())
            .containsMatch("ColorFilterID\\(color=\\[\\d+], mode=MULTIPLY\\)")
    }

    @Test
    fun remotePaintSetRemoteColorFilter_clearColorExpression() {
        val paint = RemotePaint()
        paint.remoteColorFilter =
            RemoteBlendModeColorFilter(
                RemoteColor.fromARGB(
                    RemoteFloat(1f),
                    RemoteFloat(0.8f),
                    RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
                    RemoteFloat(0.5f),
                ),
                BlendMode.MULTIPLY,
            )
        recordingCanvas.usePaint(paint)

        recordingCanvas.usePaint(Paint())

        val operations = inflateOperations()
        val paintOp = operations[operations.size - 1] as PaintData
        assertThat(paintOp.mPaintData.toString()).contains("clearColorFilter")
    }

    @Test
    fun remotePaintCopyConstructor() {
        val paint = RemotePaint()
        paint.remoteColor =
            RemoteColor.fromARGB(
                RemoteFloat(1f),
                RemoteFloat(0.8f),
                RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
                RemoteFloat(0.5f),
            )
        paint.remoteColorFilter =
            RemoteBlendModeColorFilter(
                RemoteColor.fromARGB(
                    RemoteFloat(1f),
                    RemoteFloat(0.8f),
                    RemoteFloat(RemoteContext.FLOAT_CONTINUOUS_SEC),
                    RemoteFloat(0.5f),
                ),
                BlendMode.MULTIPLY,
            )

        val paintCopy = RemotePaint(paint)

        assertThat(paintCopy.remoteColor).isEqualTo(paint.remoteColor)
        assertThat(paintCopy.remoteColorFilter).isEqualTo(paint.remoteColorFilter)
    }

    @Test
    fun drawToOffscreenBitmap() {
        recordingCanvas.drawRect(
            0.rf,
            0.rf,
            WIDTH.rf,
            HEIGHT.rf,
            Paint().apply { color = Color.BLACK },
        )
        recordingCanvas.drawRect(
            20.rf,
            20.rf,
            (WIDTH - 20).rf,
            (HEIGHT - 20).rf,
            Paint().apply { color = Color.YELLOW },
        )
        val bitmap = RemoteBitmap.createOffscreenRemoteBitmap(WIDTH, HEIGHT)
        recordingCanvas.drawToOffscreenBitmap(bitmap, Color.TRANSPARENT) {
            recordingCanvas.drawOval(
                20.rf,
                20.rf,
                (WIDTH - 20).rf,
                (HEIGHT - 20).rf,
                Paint().apply { color = Color.RED },
            )
            recordingCanvas.drawText(
                "HI",
                20.rf,
                (HEIGHT - 50).rf,
                Paint().apply {
                    textSize = 380f
                    typeface = Typeface.DEFAULT_BOLD
                    blendMode = BlendMode.CLEAR
                },
            )
        }
        val rect = Rect(0, 0, WIDTH, HEIGHT)
        recordingCanvas.drawBitmap(
            bitmap,
            rect,
            rect,
            Paint().apply { blendMode = BlendMode.SRC_OVER },
        )

        val document = constructDocument()
        assertScreenshot(document, "offscreenBitmap")
    }

    @Test
    fun drawRepeatedlyToOffscreenBitmap() {
        recordingCanvas.drawRect(
            0.rf,
            0.rf,
            WIDTH.rf,
            HEIGHT.rf,
            Paint().apply { color = Color.BLACK },
        )
        recordingCanvas.drawRect(
            20.rf,
            20.rf,
            (WIDTH - 20).rf,
            (HEIGHT - 20).rf,
            Paint().apply { color = Color.YELLOW },
        )
        val bitmap = RemoteBitmap.createOffscreenRemoteBitmap(WIDTH, HEIGHT)
        recordingCanvas.drawToOffscreenBitmap(bitmap, Color.TRANSPARENT) {
            recordingCanvas.drawOval(
                20.rf,
                20.rf,
                (WIDTH - 20).rf,
                (HEIGHT - 20).rf,
                Paint().apply { color = Color.RED },
            )
        }
        recordingCanvas.drawToOffscreenBitmap(bitmap) {
            recordingCanvas.drawText(
                "TEXT",
                20.rf,
                (HEIGHT - 50).rf,
                Paint().apply {
                    textSize = 380f
                    typeface = Typeface.DEFAULT_BOLD
                    blendMode = BlendMode.CLEAR
                },
            )
        }
        val rect = Rect(0, 0, WIDTH, HEIGHT)
        recordingCanvas.drawBitmap(
            bitmap,
            rect,
            rect,
            Paint().apply { blendMode = BlendMode.SRC_OVER },
        )

        val document = constructDocument()
        assertScreenshot(document, "offscreenBitmapRepeated")
    }

    @Test
    fun drawToOffscreenBitmap_nested() {
        recordingCanvas.drawRect(
            0.rf,
            0.rf,
            WIDTH.rf,
            HEIGHT.rf,
            Paint().apply { color = Color.BLACK },
        )

        // Create the outer offscreen bitmap.
        val outerBitmap = RemoteBitmap.createOffscreenRemoteBitmap(WIDTH, HEIGHT)
        recordingCanvas.drawToOffscreenBitmap(outerBitmap, Color.TRANSPARENT) {
            // Draw a blue background on the outer bitmap.
            recordingCanvas.drawRect(
                0.rf,
                0.rf,
                WIDTH.rf,
                HEIGHT.rf,
                Paint().apply { color = Color.BLUE },
            )

            recordingCanvas.save()

            // Create the inner (nested) offscreen bitmap.
            val innerBitmap = RemoteBitmap.createOffscreenRemoteBitmap(WIDTH / 2, HEIGHT / 2)
            recordingCanvas.drawToOffscreenBitmap(innerBitmap, Color.TRANSPARENT) {
                // Draw a red circle in the inner bitmap.
                recordingCanvas.drawOval(
                    0f,
                    0f,
                    (WIDTH / 2).toFloat(),
                    (HEIGHT / 2).toFloat(),
                    Paint().apply { color = Color.RED },
                )
            }

            // Draw the inner bitmap onto the outer bitmap. This tests that the canvas context
            // was restored correctly to the outer bitmap's canvas.
            val innerRect = Rect(0, 0, WIDTH / 2, HEIGHT / 2)
            val dstRect = Rect(100, 100, 100 + WIDTH / 2, 100 + HEIGHT / 2)
            recordingCanvas.drawBitmap(innerBitmap, innerRect, dstRect, Paint())

            // This restore isn't strictly needed, but if we're drawing to the wrong canvas it
            // will lead to an exception.
            recordingCanvas.restore()
        }

        val outerRect = Rect(0, 0, WIDTH, HEIGHT)
        recordingCanvas.drawBitmap(outerBitmap, outerRect, outerRect, Paint())

        val document = constructDocument()
        assertScreenshot(document, "offscreenBitmap_nested")
    }

    @Test
    fun setShaderMatrixCalledOnce() {
        val remoteShader =
            RemoteSweepShader(
                    100f.rf,
                    100f.rf,
                    listOf(ComposeColor.Red.rc, ComposeColor.Green.rc, ComposeColor.Blue.rc),
                    null,
                )
                .apply { remoteMatrix3x3 = RemoteMatrix3x3.createRotate(90f.rf) }
        val paintWithShader = RemotePaint().apply { shader = remoteShader }
        val paintWithShader2 =
            RemotePaint().apply {
                shader =
                    RemoteLinearShader(
                        10f.rf,
                        100f.rf,
                        200f.rf,
                        200f.rf,
                        listOf(ComposeColor.Red.rc, ComposeColor.Green.rc, ComposeColor.Blue.rc),
                        null,
                        TileMode.Repeated,
                    )
            }
        val paintWithShader3 =
            RemotePaint().apply {
                shader =
                    RemoteLinearShader(
                        10f.rf,
                        100f.rf,
                        100f.rf,
                        200f.rf,
                        listOf(ComposeColor.Red.rc, ComposeColor.Blue.rc),
                        null,
                        TileMode.Repeated,
                    )
            }
        recordingCanvas.usePaint(paintWithShader)
        recordingCanvas.usePaint(paintWithShader2)
        recordingCanvas.usePaint(paintWithShader3)
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

    private fun constructDocument() =
        CoreDocument(clock).apply {
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
        recordingBuffer.writeToBuffer()
        val buffer = creationState.document.buffer
        buffer.buffer.index = 0
        val operations = ArrayList<Operation>()
        buffer.inflateFromBuffer(operations)
        return operations
    }
}
