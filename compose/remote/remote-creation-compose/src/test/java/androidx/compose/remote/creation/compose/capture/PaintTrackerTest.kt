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
import android.graphics.Typeface
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintChanges
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
import com.google.common.truth.Truth.assertThat
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
    private lateinit var paintContext: DummyPaintContext

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
        paintContext = DummyPaintContext(AndroidRemoteContext())
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
        bundle.applyPaintChange(DummyPaintContext(AndroidRemoteContext()), changes)

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
        bundle2.applyPaintChange(DummyPaintContext(AndroidRemoteContext()), changes)

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
        bundle2.applyPaintChange(DummyPaintContext(AndroidRemoteContext()), changes)
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
        bundle.applyPaintChange(DummyPaintContext(AndroidRemoteContext()), changes)

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
        bundle2.applyPaintChange(DummyPaintContext(AndroidRemoteContext()), changes)
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
        bundle2.applyPaintChange(DummyPaintContext(AndroidRemoteContext()), changes)
        assertThat(changes.mColor).isEqualTo(Color.Yellow.toArgb())
    }

    @Test
    fun testTypefaceSync() {
        val paint = RemotePaint { typeface = Typeface.MONOSPACE }
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, recordingCanvas)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(DummyPaintContext(AndroidRemoteContext()), changes)

        assertThat(changes.mFontType).isEqualTo(3) // FONT_TYPE_MONOSPACE
    }

    // Helper classes for verification
    private class TestPaintChanges : PaintChanges {
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

        override fun setFontVariationAxes(tags: Array<out String>, values: FloatArray) {}

        override fun setTextureShader(
            bitmapId: Int,
            tileX: Short,
            tileY: Short,
            filterMode: Short,
            maxAnisotropy: Short,
        ) {}

        override fun setPathEffect(pathEffect: FloatArray?) {}
    }

    private class DummyPaintContext(context: androidx.compose.remote.core.RemoteContext) :
        PaintContext(context) {
        override fun drawBitmap(
            imageId: Int,
            srcLeft: Int,
            srcTop: Int,
            srcRight: Int,
            srcBottom: Int,
            dstLeft: Int,
            dstTop: Int,
            dstRight: Int,
            dstBottom: Int,
            cdId: Int,
        ) {}

        override fun scale(scaleX: Float, scaleY: Float) {}

        override fun translate(translateX: Float, translateY: Float) {}

        override fun drawArc(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            startAngle: Float,
            sweepAngle: Float,
        ) {}

        override fun drawSector(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            startAngle: Float,
            sweepAngle: Float,
        ) {}

        override fun drawBitmap(id: Int, left: Float, top: Float, right: Float, bottom: Float) {}

        override fun drawCircle(centerX: Float, centerY: Float, radius: Float) {}

        override fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float) {}

        override fun drawOval(left: Float, top: Float, right: Float, bottom: Float) {}

        override fun drawPath(id: Int, start: Float, end: Float) {}

        override fun drawRect(left: Float, top: Float, right: Float, bottom: Float) {}

        override fun savePaint() {}

        override fun restorePaint() {}

        override fun replacePaint(paintBundle: PaintBundle) {}

        override fun drawRoundRect(
            left: Float,
            top: Float,
            right: Float,
            bottom: Float,
            radiusX: Float,
            radiusY: Float,
        ) {}

        override fun drawTextOnPath(textId: Int, pathId: Int, hOffset: Float, vOffset: Float) {}

        override fun getTextBounds(
            textId: Int,
            start: Int,
            end: Int,
            flags: Int,
            @NonNull bounds: FloatArray,
        ) {}

        override fun layoutComplexText(
            textId: Int,
            start: Int,
            end: Int,
            alignment: Int,
            overflow: Int,
            maxLines: Int,
            maxWidth: Float,
            maxHeight: Float,
            letterSpacing: Float,
            lineHeightAdd: Float,
            lineHeightMultiplier: Float,
            lineBreakStrategy: Int,
            hyphenationFrequency: Int,
            justificationMode: Int,
            useUnderline: Boolean,
            strikethrough: Boolean,
            flags: Int,
        ): androidx.compose.remote.core.RcPlatformServices.ComputedTextLayout? = null

        override fun drawTextRun(
            textId: Int,
            start: Int,
            end: Int,
            contextStart: Int,
            contextEnd: Int,
            x: Float,
            y: Float,
            rtl: Boolean,
        ) {}

        override fun drawComplexText(
            @Nullable
            computedTextLayout: androidx.compose.remote.core.RcPlatformServices.ComputedTextLayout?
        ) {}

        override fun drawTweenPath(
            path1Id: Int,
            path2Id: Int,
            tween: Float,
            start: Float,
            end: Float,
        ) {}

        override fun tweenPath(out: Int, path1: Int, path2: Int, tween: Float) {}

        override fun combinePath(out: Int, path1: Int, path2: Int, operation: Byte) {}

        override fun applyPaint(@NonNull mPaintData: PaintBundle) {}

        override fun matrixScale(scaleX: Float, scaleY: Float, centerX: Float, centerY: Float) {}

        override fun matrixTranslate(translateX: Float, translateY: Float) {}

        override fun matrixSkew(skewX: Float, skewY: Float) {}

        override fun matrixRotate(rotate: Float, pivotX: Float, pivotY: Float) {}

        override fun matrixSave() {}

        override fun matrixRestore() {}

        override fun clipRect(left: Float, top: Float, right: Float, bottom: Float) {}

        override fun clipPath(pathId: Int, regionOp: Int) {}

        override fun roundedClipRect(
            width: Float,
            height: Float,
            topStart: Float,
            topEnd: Float,
            bottomStart: Float,
            bottomEnd: Float,
        ) {}

        override fun reset() {}

        override fun startGraphicsLayer(w: Int, h: Int) {}

        override fun setGraphicsLayer(@NonNull attributes: HashMap<Int, Any>) {}

        override fun endGraphicsLayer() {}

        override fun getText(id: Int): String? = null

        override fun matrixFromPath(pathId: Int, fraction: Float, vOffset: Float, flags: Int) {}

        override fun drawToBitmap(bitmapId: Int, mode: Int, color: Int) {}
    }
}
