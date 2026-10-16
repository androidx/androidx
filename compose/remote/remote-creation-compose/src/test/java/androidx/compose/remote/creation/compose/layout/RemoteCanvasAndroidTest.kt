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

package androidx.compose.remote.creation.compose.layout

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.state.RemoteBitmapFont
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.RemoteString.Companion.createNamedRemoteString
import androidx.compose.remote.creation.compose.state.asRemotePaint
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.util.MyRemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.util.TestRemoteComposeBuffer
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.graphics.shapes.RoundedPolygon
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteCanvasAndroidTest {
    private lateinit var creationState: RemoteComposeCreationState
    private lateinit var remoteCanvas: RemoteCanvas
    private lateinit var fakeBuffer: TestRemoteComposeBuffer

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
        remoteCanvas = RemoteCanvas(creationState)
    }

    @Test
    fun testDrawText() {
        val str = createNamedRemoteString("a", "hello")

        remoteCanvas.drawText(str, 5, 0f.rf, 0f.rf, Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

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
        remoteCanvas.drawText("hello", 0f.rf, 0f.rf, Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly("addPaint", "textData(42, \"hello\")", "addDrawTextRun(42)")
    }

    @Test
    fun testDrawBitmap_DoesNotDoubleBuffer() {
        val bitmap = RemoteImageBitmap.createForId(42)
        remoteCanvas.drawBitmap(bitmap, null, Rect(0, 0, 10, 10), Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls)
            .containsExactly("addPaint", "textData(42, \"\")", "addDrawBitmap(42)")
    }

    @Test
    fun testDrawBitmap_RemoteFloat_DoesNotDoubleBuffer() {
        val bitmap = RemoteImageBitmap.createForId(42)
        val left = createNamedRemoteFloat("left", 10f)
        val top = createNamedRemoteFloat("top", 20f)
        remoteCanvas.drawBitmap(bitmap, left, top, Paint())

        assertThat(fakeBuffer.calls).isEmpty()

        remoteCanvas.flush()

        assertThat(fakeBuffer.calls).contains("addPaint")
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawBitmap") }).isTrue()
    }

    @Test
    fun testDrawBitmapFontTextRun() {
        val str = createNamedRemoteString("str", "hello")
        val bitmapFont = RemoteBitmapFont(emptyList())

        remoteCanvas.drawBitmapFontTextRun(str, bitmapFont, 0, 5, 0f.rf, 0f.rf, 0f.rf, Paint())

        remoteCanvas.flush()

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

        remoteCanvas.drawRect(0f, 0f, 10f, 10f, paint1)
        remoteCanvas.usePaint(paint2.asRemotePaint())
        remoteCanvas.drawRect(0f, 0f, 10f, 10f, paint2)

        remoteCanvas.flush()

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
        remoteCanvas.drawColor(android.graphics.Color.RED)
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawRect") }).isTrue()
    }

    @Test
    fun testDrawOvalOverloads() {
        remoteCanvas.drawOval(0f, 0f, 100f, 100f, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawOval") }).isTrue()

        fakeBuffer.calls.clear()
        remoteCanvas.drawOval(0f.rf, 0f.rf, 100f.rf, 100f.rf, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawOval") }).isTrue()
    }

    @Test
    fun testDrawRoundRectOverloads() {
        remoteCanvas.drawRoundRect(0f, 0f, 100f, 100f, 10f, 10f, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawRoundRect") }).isTrue()

        fakeBuffer.calls.clear()
        remoteCanvas.drawRoundRect(0f.rf, 0f.rf, 100f.rf, 100f.rf, 10f.rf, 10f.rf, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawRoundRect") }).isTrue()
    }

    @Test
    fun testDrawCircleOverloads() {
        remoteCanvas.drawCircle(50f, 50f, 25f, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawCircle") }).isTrue()

        fakeBuffer.calls.clear()
        remoteCanvas.drawCircle(50f.rf, 50f.rf, 25f.rf, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawCircle") }).isTrue()
    }

    @Test
    fun testDrawArcOverloads() {
        remoteCanvas.drawArc(0f, 0f, 100f, 100f, 0f, 90f, false, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawArc") }).isTrue()

        fakeBuffer.calls.clear()
        remoteCanvas.drawArc(0f.rf, 0f.rf, 100f.rf, 100f.rf, 0f.rf, 90f.rf, true, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawSector") }).isTrue()
    }

    @Test
    fun testDrawPathAndDrawRPath() {
        val path =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(10f, 10f)
            }
        remoteCanvas.drawPath(path, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawPath") }).isTrue()

        fakeBuffer.calls.clear()
        val rPath =
            RemotePath().apply {
                moveTo(0f, 0f)
                lineTo(10f, 10f)
            }
        remoteCanvas.drawRPath(rPath, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawPath") }).isTrue()
    }

    @Test
    fun testDrawRoundedPolygonAndMorph() {
        val poly1 = RoundedPolygon(numVertices = 4)
        remoteCanvas.drawRoundedPolygon(poly1, null)
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawPath") }).isTrue()

        fakeBuffer.calls.clear()
        val poly2 = RoundedPolygon(numVertices = 4)
        remoteCanvas.drawRoundedPolygonMorph(poly1, poly2, 0.5f.rf, null)
        remoteCanvas.flush()
        assertThat(
                fakeBuffer.calls.any { it.startsWith("pathTween") || it.startsWith("addDrawPath") }
            )
            .isTrue()
    }

    @Test
    fun testDrawScaledBitmap() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        remoteCanvas.drawScaledBitmap(
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
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("drawScaledBitmap") }).isTrue()
    }

    @Test
    fun testDrawTextRunAndSpecializedText() {
        val str = "hello"
        remoteCanvas.drawTextRun(str, 0, 5, 0, 5, 10f, 10f, false, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTextRun") }).isTrue()

        fakeBuffer.calls.clear()
        val path =
            Path().apply {
                moveTo(0f, 0f)
                lineTo(100f, 100f)
            }
        remoteCanvas.drawTextOnPath(str, path, 0f, 0f, Paint())
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addDrawTextOnPath") }).isTrue()
    }

    @Test
    fun testClipRect() {
        remoteCanvas.clipRect(0f.rf, 0f.rf, 10f.rf, 10f.rf)
        remoteCanvas.flush()
        assertThat(fakeBuffer.calls.any { it.startsWith("addClipRect") }).isTrue()
    }
}
