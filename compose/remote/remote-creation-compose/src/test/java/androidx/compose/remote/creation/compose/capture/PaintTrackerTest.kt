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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintChanges
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.state.AndroidRemotePaint
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.asRemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontVariation
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PaintTrackerTest {
    private lateinit var creationState: RemoteComposeCreationState
    private lateinit var recordingCanvas: RecordingCanvas
    private lateinit var tracker: PaintTracker
    private lateinit var remoteContext: AndroidRemoteContext
    private lateinit var paintContext: PaintContext

    @Before
    fun setUp() {
        val size = Size(500f, 500f)
        creationState =
            RemoteComposeCreationState(
                androidx.compose.remote.creation.platform.AndroidxRcPlatformServices(),
                size,
            )
        val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
        recordingCanvas = RecordingCanvas(bitmap)
        recordingCanvas.creationState = creationState
        tracker = PaintTracker()
        remoteContext = AndroidRemoteContext()
        remoteContext.useCanvas(Canvas(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)))
        paintContext = remoteContext.paintContext!!
    }

    @Test
    fun testInitialSync() {
        val paint = RemotePaint {
            color = Color.Red.rc
            strokeWidth = 5f.rf
        }
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, recordingCanvas)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.mColor).isEqualTo(Color.Red.toArgb())
        assertThat(changes.mStrokeWidth).isEqualTo(5f)
    }

    @Test
    fun testDeltaOptimization() {
        val paint1 = RemotePaint {
            color = Color.Red.rc
            strokeWidth = 5f.rf
        }
        tracker.updateWithPaint(paint1, PaintBundle(), recordingCanvas)
        tracker.reset(force = false)

        val paint2 = RemotePaint {
            color = Color.Red.rc // Unchanged
            strokeWidth = 10f.rf // Changed
        }
        val bundle2 = PaintBundle()
        tracker.updateWithPaint(paint2, bundle2, recordingCanvas)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle2.applyPaintChange(paintContext, changes)

        assertThat(changes.colorSet).isFalse()
        assertThat(changes.strokeWidthSet).isTrue()
        assertThat(changes.mStrokeWidth).isEqualTo(10f)
    }

    @Test
    fun testForceSync() {
        val paint = RemotePaint { color = Color.Red.rc }
        tracker.updateWithPaint(paint, PaintBundle(), recordingCanvas)

        tracker.reset(force = true)

        val bundle2 = PaintBundle()
        tracker.updateWithPaint(paint, bundle2, recordingCanvas)

        assertThat(tracker.isChanged).isTrue()
        val changes = TestPaintChanges()
        bundle2.applyPaintChange(paintContext, changes)
        assertThat(changes.colorSet).isTrue()
        assertThat(changes.mColor).isEqualTo(Color.Red.toArgb())
    }

    @Test
    fun testStyleSync() {
        val paint = RemotePaint {
            style = PaintingStyle.Stroke
            strokeCap = StrokeCap.Round
            strokeJoin = StrokeJoin.Bevel
        }
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, recordingCanvas)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.mStyle).isEqualTo(1) // STYLE_STROKE
        assertThat(changes.mStrokeCap).isEqualTo(1) // ROUND
        assertThat(changes.mStrokeJoin).isEqualTo(2) // BEVEL
    }

    @Test
    fun testComposeToAndroidTransition() {
        // Sync with DefaultRemotePaint first
        val paint1 = RemotePaint { color = Color.Blue.rc }
        tracker.updateWithPaint(paint1, PaintBundle(), recordingCanvas)
        tracker.reset(force = false)

        // Sync with Android Paint
        val androidPaint =
            android.graphics.Paint().apply {
                color = android.graphics.Color.GREEN
                style = android.graphics.Paint.Style.FILL
            }
        val bundle2 = PaintBundle()
        tracker.updateWithPaint(androidPaint.asRemotePaint(), bundle2, recordingCanvas)

        assertThat(tracker.isChanged).isTrue()
        val changes = TestPaintChanges()
        bundle2.applyPaintChange(paintContext, changes)
        assertThat(changes.mColor).isEqualTo(android.graphics.Color.GREEN)
    }

    @Test
    fun testComposeToComposeTransition() {
        // Sync with DefaultRemotePaint first
        val paint1 = RemotePaint { color = Color.Blue.rc }
        tracker.updateWithPaint(paint1, PaintBundle(), recordingCanvas)
        tracker.reset(force = false)

        // Sync with Compose Paint
        val composePaint = Paint().apply { color = Color.Yellow }
        val bundle2 = PaintBundle()
        tracker.updateWithPaint(composePaint.asRemotePaint(), bundle2, recordingCanvas)

        assertThat(tracker.isChanged).isTrue()
        val changes = TestPaintChanges()
        bundle2.applyPaintChange(paintContext, changes)
        assertThat(changes.mColor).isEqualTo(Color.Yellow.toArgb())
    }

    @Test
    fun testTypefaceSync() {
        val paint = RemotePaint { typeface = Typeface.MONOSPACE }
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, recordingCanvas)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.mFontType).isEqualTo(3) // FONT_TYPE_MONOSPACE
    }

    @Test
    fun testFontVariationSettingsSync() {
        val settings = FontVariation.Settings(FontVariation.weight(500), FontVariation.width(100f))
        val paint = RemotePaint { fontVariationSettings = settings }
        val bundle = PaintBundle()

        val wghtId = creationState.document.addText("wght")
        val wdthId = creationState.document.addText("wdth")
        remoteContext.loadText(wghtId, "wght")
        remoteContext.loadText(wdthId, "wdth")

        tracker.updateWithPaint(paint, bundle, recordingCanvas)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.fontVariationAxesSet).isTrue()
        assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("wght", "wdth"))
        assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(500f, 100f))
    }

    @Test
    @OptIn(androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi::class)
    fun testFontVariationSettingsSync_FallbackToWeight400() {
        val originalFlag = RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis
        RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis = false
        try {
            val paint = RemotePaint { fontVariationSettings = null }
            val bundle = PaintBundle()

            val wghtId = creationState.document.addText("wght")
            remoteContext.loadText(wghtId, "wght")

            tracker.updateWithPaint(paint, bundle, recordingCanvas)

            assertThat(tracker.isChanged).isTrue()

            val changes = TestPaintChanges()
            bundle.applyPaintChange(paintContext, changes)

            assertThat(changes.fontVariationAxesSet).isTrue()
            assertThat(changes.mFontAxisTags?.single()).isEqualTo("wght")
            assertThat(changes.mFontAxisValues?.single()).isEqualTo(400f)
        } finally {
            RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis = originalFlag
        }
    }

    // Note: The following tests are partially disabled out because Robolectric does not support
    // reading fontVariationSettings from android.graphics.Paint (the getter returns null).
    // See https://github.com/robolectric/robolectric/issues/11126
    // This prevents us from verifying that settings applied to Android Paint are picked up
    // by PaintTracker in unit tests. These should work on a real device or emulator (API 26+).
    //

    @Test
    fun testFontVariationSettingsSync_AndroidRemotePaint() {
        assumeTrue(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)

        val androidPaint = android.graphics.Paint()
        val paint = AndroidRemotePaint(androidPaint)
        val bundle = PaintBundle()

        val settings = FontVariation.Settings(FontVariation.weight(500), FontVariation.width(100f))
        paint.fontVariationSettings = settings

        // TODO fix after https://github.com/robolectric/robolectric/issues/11126
        //         assertThat(paint.fontVariationSettings).isNotNull()

        val wghtId = creationState.document.addText("wght")
        val wdthId = creationState.document.addText("wdth")
        remoteContext.loadText(wghtId, "wght")
        remoteContext.loadText(wdthId, "wdth")

        tracker.updateWithPaint(paint, bundle, recordingCanvas)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        // TODO fix after https://github.com/robolectric/robolectric/issues/11126
        //         assertThat(changes.fontVariationAxesSet).isTrue()
        //         assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("wght", "wdth"))
        //         assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(500f, 100f))
    }

    @Test
    fun testFontVariationSettingsSync_AndroidPaintDirect() {
        assumeTrue(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)

        val androidPaint = android.graphics.Paint()
        androidPaint.fontVariationSettings = "'wght' 500.0, 'wdth' 100.0"

        val paint = AndroidRemotePaint(androidPaint)
        val bundle = PaintBundle()

        val wghtId = creationState.document.addText("wght")
        val wdthId = creationState.document.addText("wdth")
        remoteContext.loadText(wghtId, "wght")
        remoteContext.loadText(wdthId, "wdth")

        tracker.updateWithPaint(paint, bundle, recordingCanvas)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        // TODO fix after https://github.com/robolectric/robolectric/issues/11126
        //         assertThat(changes.fontVariationAxesSet).isTrue()
        //         assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("wght", "wdth"))
        //         assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(500f, 100f))
    }

    // Helper classes for verification
    class TestPaintChanges : PaintChanges {
        var mColor: Int = 0
        var colorSet = false
        var mStrokeWidth: Float = 0f
        var strokeWidthSet = false
        var mAlpha: Float = 1f
        var alphaSet = false
        var mStyle: Int = -1
        var mStrokeCap: Int = -1
        var mStrokeJoin: Int = -1
        var mStrokeMiter: Float = 0f
        var mFontType: Int = -1
        var mFontAxisTags: Array<out String>? = null
        var mFontAxisValues: FloatArray? = null
        var fontVariationAxesSet = false

        override fun setColor(color: Int) {
            this.mColor = color
            this.colorSet = true
        }

        override fun setStrokeWidth(width: Float) {
            this.mStrokeWidth = width
            this.strokeWidthSet = true
        }

        override fun setAlpha(a: Float) {
            this.mAlpha = a
            this.alphaSet = true
        }

        override fun setStyle(style: Int) {
            this.mStyle = style
        }

        override fun setStrokeCap(cap: Int) {
            this.mStrokeCap = cap
        }

        override fun setStrokeJoin(join: Int) {
            this.mStrokeJoin = join
        }

        override fun setStrokeMiter(miter: Float) {
            this.mStrokeMiter = miter
        }

        override fun setTypeFace(fontType: Int, weight: Int, italic: Boolean) {
            this.mFontType = fontType
        }

        override fun setTextSize(size: Float) {}

        override fun setTypeFace(fontString: String, weight: Int, italic: Boolean) {}

        override fun setFallbackTypeFace(fontType: Int, weight: Int, italic: Boolean) {}

        override fun setShader(shader: Int) {}

        override fun setImageFilterQuality(quality: Int) {}

        override fun setBlendMode(mode: Int) {}

        override fun setFilterBitmap(filter: Boolean) {}

        override fun setAntiAlias(aa: Boolean) {}

        override fun setShaderMatrix(matrixId: Float) {}

        override fun setColorFilter(color: Int, mode: Int) {}

        override fun clear(mask: Long) {}

        override fun setLinearGradient(
            colorsArray: IntArray,
            stopsArray: FloatArray?,
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
            tileMode: Int,
        ) {}

        override fun setRadialGradient(
            colorsArray: IntArray,
            stopsArray: FloatArray?,
            centerX: Float,
            centerY: Float,
            radius: Float,
            tileMode: Int,
        ) {}

        override fun setSweepGradient(
            colorsArray: IntArray,
            stopsArray: FloatArray?,
            centerX: Float,
            centerY: Float,
        ) {}

        override fun setFontVariationAxes(tags: Array<out String>, values: FloatArray) {
            this.mFontAxisTags = tags
            this.mFontAxisValues = values
            this.fontVariationAxesSet = true
        }

        override fun setTextureShader(
            bitmapId: Int,
            tileX: Short,
            tileY: Short,
            filterMode: Short,
            maxAnisotropy: Short,
        ) {}

        override fun setPathEffect(pathEffect: FloatArray?) {}
    }
}
