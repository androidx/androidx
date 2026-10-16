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

package androidx.compose.remote.creation.compose.capture

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.CompatAndroidRemotePaint
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatArray
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteIntArray
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.creation.compose.state.RemoteStringArray
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.test.utils.TestPlayer
import androidx.graphics.shapes.RoundedPolygon
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordingCanvasTest {
    private lateinit var creationState: RemoteComposeCreationState
    private lateinit var recordingCanvas: RecordingCanvas
    private lateinit var fakeBuffer: TestRemoteComposeBuffer

    private enum class DummyEnum {
        VALUE
    }

    private class MyRemoteComposeWriterAndroid(
        profile: Profile,
        buffer: RemoteComposeBuffer,
        vararg tags: RemoteComposeWriter.HTag,
    ) : RemoteComposeWriterAndroid(profile, buffer, *tags)

    @Before
    fun setUp() {
        fakeBuffer = TestRemoteComposeBuffer()

        val platform = AndroidxRcPlatformServices()
        val testProfileMask = RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL
        val profile =
            Profile(CoreDocument.DOCUMENT_API_LEVEL, testProfileMask, platform) {
                creationDisplayInfo,
                profile,
                callbacks ->
                MyRemoteComposeWriterAndroid(
                    profile,
                    fakeBuffer,
                    RemoteComposeWriter.hTag(Header.DOC_WIDTH, creationDisplayInfo.width),
                    RemoteComposeWriter.hTag(Header.DOC_HEIGHT, creationDisplayInfo.height),
                    RemoteComposeWriter.hTag(Header.DOC_PROFILES, testProfileMask),
                )
            }

        creationState =
            RemoteComposeCreationState(RemoteCreationDisplayInfo(500, 500, 160, 1f), null, profile)

        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        recordingCanvas = RecordingCanvas(bitmap, creationState)
    }

    @Test
    fun testDrawText() {
        val str = createNamedRemoteString("a", "hello")

        recordingCanvas.drawText(str, 5, 0f.rf, 0f.rf, Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

        // Verify that it calls addDrawTextRun once!
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addPaint",
                "setNamedVariable(42, \"USER:a\", 0)",
                "addDrawTextRun(42)",
            )
    }

    @Test
    fun testDrawText_StringOverload_DoesNotDoubleBuffer() {
        recordingCanvas.drawText("hello", 0f.rf, 0f.rf, Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        // This test guards against double buffering. If drawText calls another buffered method
        // (like drawTextRun) inside its action, that inner method will record a new operation
        // during flush instead of executing drawing calls immediately. This causes the operation
        // to be deferred to the next flush or lost.
        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly("addPaint", "textData(42, \"hello\")", "addDrawTextRun(42)")
    }

    @Test
    fun testDrawBitmap_DoesNotDoubleBuffer() {
        val bitmap = RemoteImageBitmap.createForId(42)
        recordingCanvas.drawBitmap(bitmap, null, android.graphics.Rect(0, 0, 10, 10), Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        // This test guards against double buffering of paint operations. If a buffered draw method
        // calls another buffered method (like usePaint) inside its action, the paint operation
        // will be deferred, causing the draw operation to be executed with the wrong paint state.
        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly("addPaint", "textData(42, \"\")", "addDrawBitmap(42)")
    }

    @Test
    fun testDrawBitmap_RemoteFloat_DoesNotDoubleBuffer() {
        val bitmap = RemoteImageBitmap.createForId(42)
        val left = createNamedRemoteFloat("left", 10f)
        val top = createNamedRemoteFloat("top", 20f)
        recordingCanvas.drawBitmap(bitmap, left, top, Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addPaint")
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawBitmap") }).isTrue()
    }

    @Test
    fun testDrawTweenPath_PathOverload_DoesNotDoubleBuffer() {
        val path1 = RemotePath().asComposePath()
        val path2 = RemotePath().asComposePath()
        val tween = createNamedRemoteFloat("tween", 0.5f)
        val start = RemoteFloat(0f)
        val stop = RemoteFloat(1f)
        recordingCanvas.drawTweenPath(
            path1,
            path2,
            tween,
            start,
            stop,
            androidx.compose.ui.graphics.Paint(),
        )

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addPaint")
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTweenPath") }).isTrue()
    }

    @Test
    fun testDrawTweenPath_RemotePathOverload_DoesNotDoubleBuffer() {
        val path1 = RemotePath()
        val path2 = RemotePath()
        val tween = createNamedRemoteFloat("tween", 0.5f)
        val start = RemoteFloat(0f)
        val stop = RemoteFloat(1f)
        recordingCanvas.drawTweenPath(
            path1,
            path2,
            tween,
            start,
            stop,
            androidx.compose.ui.graphics.Paint(),
        )

        assertThat(fakeBuffer.calls).isEmpty()

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addPaint")
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTweenPath") }).isTrue()
    }

    @Test
    fun testDrawBitmapFontTextRun() {
        val str = createNamedRemoteString("str", "hello")
        val bitmapFont = RemoteBitmapFont(emptyList())

        recordingCanvas.drawBitmapFontTextRun(str, bitmapFont, 0, 5, 0f.rf, 0f.rf, 0f.rf, Paint())

        recordingCanvas.flush()

        // Verify that it calls addDrawBitmapFontTextRun!
        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addPaint",
                "setNamedVariable(42, \"USER:str\", 0)",
                "addBitmapFont(43)",
                "addDrawBitmapFontTextRun(42, 43)",
            )
    }

    @Test
    fun testUnbufferedUsePaint_Bug() {
        val paint1 = Paint().apply { color = 0xFFFF0000.toInt() }
        val paint2 = Paint().apply { color = 0xFF0000FF.toInt() }

        recordingCanvas.drawRect(0f, 0f, 10f, 10f, paint1)
        recordingCanvas.usePaint(paint2)
        recordingCanvas.drawRect(0f, 0f, 10f, 10f, paint2)

        recordingCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly(
                "addPaint",
                "addDrawRect(0.0, 0.0, 10.0, 10.0)",
                "addPaint",
                "addDrawRect(0.0, 0.0, 10.0, 10.0)",
            )
    }

    @Test
    fun testDrawColor() {
        recordingCanvas.drawColor(android.graphics.Color.RED)
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawRect") }).isTrue()
    }

    @Test
    fun testDrawOvalOverloads() {
        recordingCanvas.drawOval(0f, 0f, 100f, 100f, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawOval") }).isTrue()

        fakeBuffer.calls.clear()
        recordingCanvas.drawOval(0f.rf, 0f.rf, 100f.rf, 100f.rf, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawOval") }).isTrue()
    }

    @Test
    fun testDrawRoundRectOverloads() {
        recordingCanvas.drawRoundRect(0f, 0f, 100f, 100f, 10f, 10f, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawRoundRect") }).isTrue()

        fakeBuffer.calls.clear()
        recordingCanvas.drawRoundRect(0f.rf, 0f.rf, 100f.rf, 100f.rf, 10f.rf, 10f.rf, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawRoundRect") }).isTrue()
    }

    @Test
    fun testDrawCircleOverloads() {
        recordingCanvas.drawCircle(50f, 50f, 25f, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawCircle") }).isTrue()

        fakeBuffer.calls.clear()
        recordingCanvas.drawCircle(50f.rf, 50f.rf, 25f.rf, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawCircle") }).isTrue()
    }

    @Test
    fun testDrawArcOverloads() {
        recordingCanvas.drawArc(0f, 0f, 100f, 100f, 0f, 90f, false, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawArc") }).isTrue()

        fakeBuffer.calls.clear()
        recordingCanvas.drawArc(0f.rf, 0f.rf, 100f.rf, 100f.rf, 0f.rf, 90f.rf, true, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawSector") }).isTrue()
    }

    @Test
    fun testDrawPathAndDrawRPath() {
        val path =
            android.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(10f, 10f)
            }
        recordingCanvas.drawPath(path, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawPath") }).isTrue()

        fakeBuffer.calls.clear()
        val rPath =
            RemotePath().apply {
                moveTo(0f, 0f)
                lineTo(10f, 10f)
            }
        recordingCanvas.drawRPath(rPath, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawPath") }).isTrue()
    }

    @Test
    fun testDrawRoundedPolygonAndMorph() {
        val poly1 = RoundedPolygon(numVertices = 4)
        recordingCanvas.drawRoundedPolygon(poly1, null)
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawPath") }).isTrue()

        fakeBuffer.calls.clear()
        val poly2 = RoundedPolygon(numVertices = 4)
        recordingCanvas.drawRoundedPolygonMorph(poly1, poly2, 0.5f.rf, null)
        recordingCanvas.flush()
        assertThat(
                fakeBuffer.calls.any { it.startsWith("pathTween") || it.startsWith("addDrawPath") }
            )
            .isTrue()
    }

    @Test
    fun testDrawScaledBitmap() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        recordingCanvas.drawScaledBitmap(
            bitmap,
            0f.rf,
            0f.rf,
            10f.rf,
            10f.rf,
            0f.rf,
            0f.rf,
            100f.rf,
            100f.rf,
            0,
            1f.rf,
            null,
        )
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("drawScaledBitmap") }).isTrue()
    }

    @Test
    fun testDrawTextRunAndSpecializedText() {
        val str = "hello"
        recordingCanvas.drawTextRun(str, 0, 5, 0, 5, 10f, 10f, false, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTextRun") }).isTrue()

        fakeBuffer.calls.clear()
        val path =
            android.graphics.Path().apply {
                moveTo(0f, 0f)
                lineTo(100f, 100f)
            }
        recordingCanvas.drawTextOnPath(str, path, 0f, 0f, Paint())
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTextOnPath") }).isTrue()
    }

    @Test
    fun testClipRectOverloadsAndBounds() {
        recordingCanvas.clipRect(0f.rf, 0f.rf, 10f.rf, 10f.rf)
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addClipRect") }).isTrue()

        fakeBuffer.calls.clear()
        recordingCanvas.clipRect(android.graphics.Rect(0, 0, 5, 5))
        recordingCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addClipRect") }).isTrue()

        val bounds = android.graphics.Rect()
        assertThat(recordingCanvas.getClipBounds(bounds)).isTrue()
        assertThat(bounds).isEqualTo(android.graphics.Rect(0, 0, 2048, 2048))
    }

    private fun captureDrawCalls(): List<String> {
        val wireBuffer = fakeBuffer.buffer
        val bytes = wireBuffer.buffer.copyOfRange(0, wireBuffer.size)
        val player = TestPlayer.fromBytes(bytes, 500f, 500f)
        return player.paint()
    }

    @Test
    fun testDefinePattern() {
        val macro =
            recordingCanvas.definePattern { x: RemoteFloat, y: RemoteFloat, c: RemoteColor ->
                recordingCanvas.drawRect(
                    x,
                    y,
                    x + 50f.rf,
                    y + 50f.rf,
                    CompatAndroidRemotePaint().apply { remoteColor = c },
                )
            }

        macro(10f.rf, 10f.rf, RemoteColor(Color.RED), null)
        macro(10f.rf, 100f.rf, RemoteColor(Color.GREEN), null)
        macro(100f.rf, 10f.rf, RemoteColor(Color.BLUE), null)
        macro(100f.rf, 100f.rf, RemoteColor(Color.YELLOW), null)

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter { it.startsWith("drawRect") || it.contains("paint.setColor") }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF0000)",
                "drawRect(10.000000, 10.000000, 60.000000, 60.000000)",
                "  paint.setColor(0xFF00FF00)",
                "drawRect(10.000000, 100.000000, 60.000000, 150.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawRect(100.000000, 10.000000, 150.000000, 60.000000)",
                "  paint.setColor(0xFFFFFF00)",
                "drawRect(100.000000, 100.000000, 150.000000, 150.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_sixArguments() {
        val macro =
            recordingCanvas.definePattern {
                x: RemoteFloat,
                y: RemoteFloat,
                w: RemoteFloat,
                h: RemoteFloat,
                c: RemoteColor,
                condition: RemoteBoolean ->
                recordingCanvas.drawConditionally(condition) {
                    recordingCanvas.drawRect(
                        x,
                        y,
                        x + w,
                        y + h,
                        CompatAndroidRemotePaint().apply { remoteColor = c },
                    )
                }
            }

        macro(10f.rf, 10f.rf, 50f.rf, 50f.rf, RemoteColor(Color.RED), RemoteBoolean(true), null)
        // Note this will get elided due to the condition being false.
        macro(20f.rf, 20f.rf, 60f.rf, 60f.rf, RemoteColor(Color.GREEN), RemoteBoolean(false), null)

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter { it.startsWith("drawRect") || it.contains("paint.setColor") }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF0000)",
                "drawRect(10.000000, 10.000000, 60.000000, 60.000000)",
            )
            .inOrder()
    }

    @Test
    fun testRecordingCanvas_forEach_singleArg() {
        recordingCanvas.forEach(listOf(10f.rf, 30f.rf, 50f.rf)) { cx ->
            recordingCanvas.drawCircle(
                cx,
                50f.rf,
                10f.rf,
                CompatAndroidRemotePaint().apply { remoteColor = RemoteColor(Color.BLUE) },
            )
        }

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(10.000000, 50.000000, 10.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(30.000000, 50.000000, 10.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(50.000000, 50.000000, 10.000000)",
            )
            .inOrder()
    }

    @Ignore("Requires remote-core tuple alias support for multi-argument forEach")
    @Test
    fun testRecordingCanvas_forEach_multiArgs() {
        recordingCanvas.forEach(
            listOf(10f.rf, 40f.rf),
            listOf(15f.rf, 45f.rf),
            listOf(RemoteColor(Color.RED), RemoteColor(Color.GREEN)),
        ) { x, y, color ->
            recordingCanvas.drawRect(
                x,
                y,
                x + 20f.rf,
                y + 20f.rf,
                CompatAndroidRemotePaint().apply { remoteColor = color },
            )
        }

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter { it.startsWith("drawRect") || it.contains("paint.setColor") }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF0000)",
                "drawRect(10.000000, 15.000000, 30.000000, 35.000000)",
                "  paint.setColor(0xFF00FF00)",
                "drawRect(40.000000, 45.000000, 60.000000, 65.000000)",
            )
            .inOrder()
    }

    @Test
    fun testRecordingCanvas_forEach_floatArray() {
        val paint = CompatAndroidRemotePaint().apply { remoteColor = RemoteColor(Color.RED) }
        val array = RemoteFloatArray(listOf(100f.rf, 200f.rf))

        recordingCanvas.forEach(array) { x -> recordingCanvas.drawCircle(x, 50f.rf, 10f.rf, paint) }

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF0000)",
                "drawCircle(100.000000, 50.000000, 10.000000)",
                "  paint.setColor(0xFFFF0000)",
                "drawCircle(200.000000, 50.000000, 10.000000)",
            )
            .inOrder()
    }

    @Test
    fun testRecordingCanvas_forEach_intArray() {
        val paint = CompatAndroidRemotePaint().apply { remoteColor = RemoteColor(Color.BLUE) }
        val intArray = RemoteIntArray(10, 20, 30)

        recordingCanvas.forEach(intArray) { intVal ->
            recordingCanvas.drawCircle(intVal.toRemoteFloat(), 50f.rf, 5f.rf, paint)
        }

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(10.000000, 50.000000, 5.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(20.000000, 50.000000, 5.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(30.000000, 50.000000, 5.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_withRemoteIntArray() {
        val paint = CompatAndroidRemotePaint().apply { remoteColor = RemoteColor(Color.GREEN) }
        val listPattern = recordingCanvas.definePattern { offsets: RemoteIntArray ->
            recordingCanvas.forEach(offsets) { offset ->
                recordingCanvas.drawCircle(offset.toRemoteFloat(), 25f.rf, 5f.rf, paint)
            }
        }

        listPattern(RemoteIntArray(50, 100))

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF00FF00)",
                "drawCircle(50.000000, 25.000000, 5.000000)",
                "  paint.setColor(0xFF00FF00)",
                "drawCircle(100.000000, 25.000000, 5.000000)",
            )
            .inOrder()
    }

    @Test
    fun testRecordingCanvas_forEach_stringArray() {
        val paint = CompatAndroidRemotePaint().apply { remoteColor = RemoteColor(Color.BLACK) }
        val stringArray = RemoteStringArray(listOf("One".rs, "Two".rs))

        recordingCanvas.forEach(stringArray) { str ->
            recordingCanvas.drawText(str, 3, 0f.rf, 10f.rf, paint)
        }

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawTextRun") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF000000)",
                "drawTextRun(text=\"One\", start=0, end=3, x=0.000000, y=10.000000)",
                "  paint.setColor(0xFF000000)",
                "drawTextRun(text=\"Two\", start=0, end=3, x=0.000000, y=10.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_inForEach_remoteFloatArray() {
        recordingCanvas.forEach(RemoteFloatArray(listOf(10f.rf, 30f.rf))) { cx ->
            recordingCanvas.drawCircle(
                cx,
                50f.rf,
                10f.rf,
                CompatAndroidRemotePaint().apply { remoteColor = RemoteColor(Color.BLUE) },
            )
        }

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(10.000000, 50.000000, 10.000000)",
                "  paint.setColor(0xFF0000FF)",
                "drawCircle(30.000000, 50.000000, 10.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_inForEach_remoteIntArray() {
        recordingCanvas.forEach(RemoteIntArray(10, 30)) { cx ->
            recordingCanvas.drawCircle(
                cx.toRemoteFloat(),
                50f.rf,
                10f.rf,
                CompatAndroidRemotePaint().apply { remoteColor = RemoteColor(Color.GREEN) },
            )
        }

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF00FF00)",
                "drawCircle(10.000000, 50.000000, 10.000000)",
                "  paint.setColor(0xFF00FF00)",
                "drawCircle(30.000000, 50.000000, 10.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_inForEach_remoteStringArray() {
        recordingCanvas.forEach(RemoteStringArray(listOf("Hello".rs, "World".rs))) { text ->
            recordingCanvas.drawText(
                text,
                5,
                0f.rf,
                10f.rf,
                CompatAndroidRemotePaint().apply { remoteColor = RemoteColor(Color.BLACK) },
            )
        }

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawTextRun") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFF000000)",
                "drawTextRun(text=\"Hello\", start=0, end=5, x=0.000000, y=10.000000)",
                "  paint.setColor(0xFF000000)",
                "drawTextRun(text=\"World\", start=0, end=5, x=0.000000, y=10.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_withRemoteStringArray() {
        val paint = CompatAndroidRemotePaint().apply { remoteColor = RemoteColor(Color.MAGENTA) }
        val listPattern = recordingCanvas.definePattern { texts: RemoteStringArray ->
            recordingCanvas.forEach(texts) { text ->
                recordingCanvas.drawText(text, 5, 0f.rf, 20f.rf, paint)
            }
        }

        listPattern(RemoteStringArray(listOf("Alpha".rs, "Omega".rs)))

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawTextRun") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF00FF)",
                "drawTextRun(text=\"Alpha\", start=0, end=5, x=0.000000, y=20.000000)",
                "  paint.setColor(0xFFFF00FF)",
                "drawTextRun(text=\"Omega\", start=0, end=5, x=0.000000, y=20.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_withRemoteFloatArray() {
        val paint = CompatAndroidRemotePaint().apply { remoteColor = RemoteColor(Color.RED) }
        val listPattern = recordingCanvas.definePattern { offsets: RemoteFloatArray ->
            recordingCanvas.forEach(offsets) { offset ->
                recordingCanvas.drawCircle(offset, 25f.rf, 5f.rf, paint)
            }
        }

        listPattern(RemoteFloatArray(listOf(50f.rf, 100f.rf)))

        recordingCanvas.flush()

        val drawAndPaintCommands =
            captureDrawCalls().filter {
                it.startsWith("drawCircle") || it.contains("paint.setColor")
            }
        assertThat(drawAndPaintCommands)
            .containsExactly(
                "  paint.setColor(0xFFFF0000)",
                "drawCircle(50.000000, 25.000000, 5.000000)",
                "  paint.setColor(0xFFFF0000)",
                "drawCircle(100.000000, 25.000000, 5.000000)",
            )
            .inOrder()
    }

    @Test
    fun testDefinePattern_unsupportedType_throwsIllegalArgumentException() {
        val pattern = recordingCanvas.definePattern { dp: RemoteDp ->
            recordingCanvas.drawCircle(dp.value, 25f.rf, 5f.rf, CompatAndroidRemotePaint())
        }

        assertThrows(IllegalArgumentException::class.java) { pattern(10.rdp) }
    }
}

private fun formatFloat(f: Float): String {
    return if (f.isNaN()) {
        val mathName = AnimatedFloatExpression.toMathName(f)
        if (mathName != null) {
            mathName
        } else {
            val id = Utils.idFromNan(f)
            if (id == 0) {
                "NaN"
            } else {
                "ID($id)"
            }
        }
    } else {
        f.toString()
    }
}

private fun FloatArray.formatToString(): String {
    return this.joinToString(prefix = "[", postfix = "]") { formatFloat(it) }
}

private open class RecordingTestRemoteComposeBuffer(val calls: ArrayList<String>) :
    RemoteComposeBuffer(CoreDocument.DOCUMENT_API_LEVEL) {

    override fun drawOnBitmap(bitmapId: Int, mode: Int, color: Int) {
        calls.add("drawOnBitmap($bitmapId, $mode, $color)")
        super.drawOnBitmap(bitmapId, mode, color)
    }

    override fun addMatrixSave() {
        calls.add("addMatrixSave")
        super.addMatrixSave()
    }

    override fun addMatrixRestore() {
        calls.add("addMatrixRestore")
        super.addMatrixRestore()
    }

    override fun addMatrixTranslate(dx: Float, dy: Float) {
        calls.add("addMatrixTranslate(${formatFloat(dx)}, ${formatFloat(dy)})")
        super.addMatrixTranslate(dx, dy)
    }

    override fun addMatrixScale(scaleX: Float, scaleY: Float) {
        calls.add("addMatrixScale(${formatFloat(scaleX)}, ${formatFloat(scaleY)})")
        super.addMatrixScale(scaleX, scaleY)
    }

    override fun addMatrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {
        calls.add(
            "addMatrixScale(${formatFloat(scaleX)}, ${formatFloat(scaleY)}, ${formatFloat(centerX)}, ${formatFloat(centerY)})"
        )
        super.addMatrixScale(scaleX, scaleY, centerX, centerY)
    }

    override fun addMatrixRotate(angle: Float, centerX: Float, centerY: Float) {
        calls.add(
            "addMatrixRotate(${formatFloat(angle)}, ${formatFloat(centerX)}, ${formatFloat(centerY)})"
        )
        super.addMatrixRotate(angle, centerX, centerY)
    }

    override fun addMatrixSkew(skewX: Float, skewY: Float) {
        calls.add("addMatrixSkew(${formatFloat(skewX)}, ${formatFloat(skewY)})")
        super.addMatrixSkew(skewX, skewY)
    }

    override fun addClipRect(left: Float, top: Float, right: Float, bottom: Float) {
        calls.add(
            "addClipRect(${formatFloat(left)}, ${formatFloat(top)}, ${formatFloat(right)}, ${formatFloat(bottom)})"
        )
        super.addClipRect(left, top, right, bottom)
    }

    override fun addDrawRect(left: Float, top: Float, right: Float, bottom: Float) {
        calls.add(
            "addDrawRect(${formatFloat(left)}, ${formatFloat(top)}, ${formatFloat(right)}, ${formatFloat(bottom)})"
        )
        super.addDrawRect(left, top, right, bottom)
    }

    override fun addDrawLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        calls.add(
            "addDrawLine(${formatFloat(x1)}, ${formatFloat(y1)}, ${formatFloat(x2)}, ${formatFloat(y2)})"
        )
        super.addDrawLine(x1, y1, x2, y2)
    }

    override fun addDrawBitmap(
        bitmapId: Int,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        descriptionId: Int,
    ) {
        calls.add("addDrawBitmap($bitmapId)")
        super.addDrawBitmap(bitmapId, left, top, right, bottom, descriptionId)
    }

    override fun addPaint(paint: PaintBundle) {
        calls.add("addPaint")
        super.addPaint(paint)
    }

    override fun setTheme(theme: Int) {
        calls.add("setTheme($theme)")
        super.setTheme(theme)
    }

    override fun addLoopStart(indexId: Int, from: Float, step: Float, until: Float) {
        calls.add(
            "addLoopStart($indexId, ${formatFloat(from)}, ${formatFloat(step)}, ${formatFloat(until)})"
        )
        super.addLoopStart(indexId, from, step, until)
    }

    override fun addLoopEnd() {
        calls.add("addLoopEnd")
        super.addLoopEnd()
    }

    override fun addConditionalOperations(type: Byte, a: Float, b: Float) {
        calls.add("addConditionalOperations($type, ${formatFloat(a)}, ${formatFloat(b)})")
        super.addConditionalOperations(type, a, b)
    }

    override fun endConditionalOperations() {
        calls.add("endConditionalOperations")
        super.endConditionalOperations()
    }

    override fun setNamedVariable(id: Int, name: String, type: Int) {
        calls.add("setNamedVariable($id, \"$name\", $type)")
        super.setNamedVariable(id, name, type)
    }

    override fun addIntegerExpression(id: Int, mask: Int, value: IntArray) {
        calls.add("addIntegerExpression($id, $mask, ${value.toList()})")
        super.addIntegerExpression(id, mask, value)
    }

    override fun addColorExpression(id: Int, alpha: Float, red: Float, green: Float, blue: Float) {
        calls.add(
            "addColorExpression($id, ${formatFloat(alpha)}, ${formatFloat(red)}, ${formatFloat(green)}, ${formatFloat(blue)})"
        )
        super.addColorExpression(id, alpha, red, green, blue)
    }

    override fun addText(id: Int, text: String) {
        calls.add("textData($id, \"$text\")")
        super.addText(id, text)
    }

    override fun addList(id: Int, value: IntArray) {
        calls.add("addList($id, ${value.toList()})")
        super.addList(id, value)
    }

    override fun createTextFromFloat(
        textId: Int,
        value: Float,
        before: Short,
        after: Short,
        flags: Int,
    ): Int {
        calls.add("createTextFromFloat($textId, ${formatFloat(value)}, $before, $after, $flags)")
        return super.createTextFromFloat(textId, value, before, after, flags)
    }

    override fun textLookup(textId: Int, stringListId: Float, indexId: Float) {
        calls.add("textLookup($textId, ${formatFloat(stringListId)}, ${formatFloat(indexId)})")
        super.textLookup(textId, stringListId, indexId)
    }

    override fun textLookup(textId: Int, stringListId: Float, indexId: Int) {
        calls.add("textLookup($textId, ${formatFloat(stringListId)}, $indexId)")
        super.textLookup(textId, stringListId, indexId)
    }

    override fun addDrawTextRun(
        textId: Int,
        start: Int,
        end: Int,
        contextStart: Int,
        contextEnd: Int,
        x: Float,
        y: Float,
        rtl: Boolean,
    ) {
        calls.add("addDrawTextRun($textId)")
        super.addDrawTextRun(textId, start, end, contextStart, contextEnd, x, y, rtl)
    }

    override fun addAnimatedFloat(id: Int, value: FloatArray) {
        val labels = arrayOfNulls<String>(value.size)
        for (i in 0 until value.size) {
            if (value[i].isNaN()) {
                labels[i] = "[" + Utils.idFromNan(value[i]) + "]"
            }
        }
        val exprStr = AnimatedFloatExpression.toString(value, labels)
        calls.add("addAnimatedFloat($id) = ($exprStr)")
        super.addAnimatedFloat(id, *value)
    }

    override fun addAnimatedFloat(id: Int, value: FloatArray, animation: FloatArray?) {
        val labels = arrayOfNulls<String>(value.size)
        for (i in 0 until value.size) {
            if (value[i].isNaN()) {
                labels[i] = "[" + Utils.idFromNan(value[i]) + "]"
            }
        }
        val exprStr = AnimatedFloatExpression.toString(value, labels)
        calls.add("addAnimatedFloat($id) = ($exprStr)")
        super.addAnimatedFloat(id, value, animation)
    }
}

private class OptimizingTestRemoteComposeBuffer(calls: ArrayList<String> = ArrayList()) :
    RecordingTestRemoteComposeBuffer(calls)
