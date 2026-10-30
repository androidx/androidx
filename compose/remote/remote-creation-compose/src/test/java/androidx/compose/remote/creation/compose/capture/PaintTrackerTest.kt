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
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.PaintContext
import androidx.compose.remote.core.operations.PaintData
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.core.operations.paint.PaintChanges
import androidx.compose.remote.creation.compose.ExperimentalRemoteCreationComposeApi
import androidx.compose.remote.creation.compose.RemoteComposeCreationComposeFlags
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.shaders.RemoteShader
import androidx.compose.remote.creation.compose.state.CompatAndroidRemotePaint
import androidx.compose.remote.creation.compose.state.RemoteFilterQuality
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3.Companion.createIdentity
import androidx.compose.remote.creation.compose.state.RemoteMatrix3x3.Companion.createRotate
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.asRemotePaint
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.compose.remote.creation.compose.text.RemoteTypeface.Companion.create
import androidx.compose.remote.player.core.platform.AndroidRemoteContext
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
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
        tracker.updateWithPaint(paint, bundle, creationState)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.mColor).isEqualTo(Color.Red.toArgb())
        assertThat(changes.mStrokeWidth).isEqualTo(5f)
    }

    @Test
    fun testInitialSync_WhiteColor() {
        val paint = RemotePaint { color = Color.White.rc }
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, creationState)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.colorSet).isTrue()
        assertThat(changes.mColor).isEqualTo(Color.White.toArgb())
    }

    @Test
    fun testDeltaOptimization() {
        val paint1 = RemotePaint {
            color = Color.Red.rc
            strokeWidth = 5f.rf
        }
        tracker.updateWithPaint(paint1, PaintBundle(), creationState)
        tracker.reset(force = false)

        val paint2 = RemotePaint {
            color = Color.Red.rc // Unchanged
            strokeWidth = 10f.rf // Changed
        }
        val bundle2 = PaintBundle()
        tracker.updateWithPaint(paint2, bundle2, creationState)

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
        tracker.updateWithPaint(paint, PaintBundle(), creationState)

        tracker.reset(force = true)

        val bundle2 = PaintBundle()
        tracker.updateWithPaint(paint, bundle2, creationState)

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
        tracker.updateWithPaint(paint, bundle, creationState)

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
        tracker.updateWithPaint(paint1, PaintBundle(), creationState)
        tracker.reset(force = false)

        // Sync with Android Paint
        val androidPaint =
            android.graphics.Paint().apply {
                color = android.graphics.Color.GREEN
                style = android.graphics.Paint.Style.FILL
            }
        val bundle2 = PaintBundle()
        tracker.updateWithPaint(androidPaint.asRemotePaint(), bundle2, creationState)

        assertThat(tracker.isChanged).isTrue()
        val changes = TestPaintChanges()
        bundle2.applyPaintChange(paintContext, changes)
        assertThat(changes.mColor).isEqualTo(android.graphics.Color.GREEN)
    }

    @Test
    fun testTypefaceSync() {
        val paint = RemotePaint { typeface = RemoteTypeface.Monospace }
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, creationState)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.mFontType).isEqualTo(3) // FONT_TYPE_MONOSPACE
    }

    @Test
    fun testNamedTypefaceSync() {
        val paint = RemotePaint {
            typeface = RemoteTypeface.Named("RobotoFlex", weight = 700, isItalic = true)
        }
        val bundle = PaintBundle()
        val typefaceId = creationState.document.addText("RobotoFlex")
        remoteContext.loadText(typefaceId, "RobotoFlex")

        tracker.updateWithPaint(paint, bundle, creationState)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.mFontString).isEqualTo("RobotoFlex")
        assertThat(changes.mWeight).isEqualTo(700)
        assertThat(changes.mItalic).isTrue()
    }

    @Test
    fun testNamedTypefaceMapsToFontType() {
        val paintDefault = RemotePaint { typeface = create("default") }
        val paintMono = RemotePaint { typeface = create("monospace") }
        val paintSerif = RemotePaint { typeface = create("serif") }
        val paintSansSerif = RemotePaint { typeface = create("sans-serif") }

        val bundleDefault = PaintBundle()
        val bundleMono = PaintBundle()
        val bundleSerif = PaintBundle()
        val bundleSansSerif = PaintBundle()

        val changesDefault = TestPaintChanges()
        val changesMono = TestPaintChanges()
        val changesSerif = TestPaintChanges()
        val changesSansSerif = TestPaintChanges()

        tracker.updateWithPaint(paintDefault, bundleDefault, creationState)
        tracker.updateWithPaint(paintMono, bundleMono, creationState)
        tracker.updateWithPaint(paintSerif, bundleSerif, creationState)
        tracker.updateWithPaint(paintSansSerif, bundleSansSerif, creationState)

        bundleDefault.applyPaintChange(paintContext, changesDefault)
        bundleMono.applyPaintChange(paintContext, changesMono)
        bundleSerif.applyPaintChange(paintContext, changesSerif)
        bundleSansSerif.applyPaintChange(paintContext, changesSansSerif)

        assertThat(changesDefault.mFontType).isEqualTo(0) // FONT_TYPE_DEFAULT
        assertThat(changesMono.mFontType).isEqualTo(3) // FONT_TYPE_MONOSPACE
        assertThat(changesSerif.mFontType).isEqualTo(2) // FONT_TYPE_SERIF
        assertThat(changesSansSerif.mFontType).isEqualTo(1) // FONT_TYPE_SANS_SERIF
    }

    @Test
    fun testTypefaceStyleSync() {
        val paintNormal = RemotePaint {
            typeface = create("sans-serif", RemoteTypeface.Style.Normal)
        }
        val paintBold = RemotePaint { typeface = create("sans-serif", RemoteTypeface.Style.Bold) }
        val paintItalic = RemotePaint {
            typeface = create("sans-serif", RemoteTypeface.Style.Italic)
        }
        val paintBoldItalic = RemotePaint {
            typeface = create("sans-serif", RemoteTypeface.Style.BoldItalic)
        }

        val bundleNormal = PaintBundle()
        val bundleBold = PaintBundle()
        val bundleItalic = PaintBundle()
        val bundleBoldItalic = PaintBundle()

        val changesNormal = TestPaintChanges()
        val changesBold = TestPaintChanges()
        val changesItalic = TestPaintChanges()
        val changesBoldItalic = TestPaintChanges()

        tracker.updateWithPaint(paintNormal, bundleNormal, creationState)
        tracker.updateWithPaint(paintBold, bundleBold, creationState)
        tracker.updateWithPaint(paintItalic, bundleItalic, creationState)
        tracker.updateWithPaint(paintBoldItalic, bundleBoldItalic, creationState)

        bundleNormal.applyPaintChange(paintContext, changesNormal)
        bundleBold.applyPaintChange(paintContext, changesBold)
        bundleItalic.applyPaintChange(paintContext, changesItalic)
        bundleBoldItalic.applyPaintChange(paintContext, changesBoldItalic)

        assertThat(changesNormal.mWeight).isEqualTo(400)
        assertThat(changesNormal.mItalic).isFalse()

        assertThat(changesBold.mWeight).isEqualTo(700)
        assertThat(changesBold.mItalic).isFalse()

        assertThat(changesItalic.mWeight).isEqualTo(400)
        assertThat(changesItalic.mItalic).isTrue()

        assertThat(changesBoldItalic.mWeight).isEqualTo(700)
        assertThat(changesBoldItalic.mItalic).isTrue()
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

        tracker.updateWithPaint(paint, bundle, creationState)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.fontVariationAxesSet).isTrue()
        assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("wght", "wdth"))
        assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(500f, 100f))
    }

    @Test
    fun testFontFeatureSettingsSync() {
        val paint = RemotePaint { fontFeatureSettings = "tnum" }
        val bundle = PaintBundle()

        val tnumId = creationState.document.addText("tnum")
        remoteContext.loadText(tnumId, "tnum")

        tracker.updateWithPaint(paint, bundle, creationState)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.fontVariationAxesSet).isTrue()
        assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("tnum"))
        assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(1f))
    }

    @Test
    fun testMultipleFontFeatureSettingsSync() {
        val paint = RemotePaint { fontFeatureSettings = "'tnum' 1, 'zero' 1" }
        val bundle = PaintBundle()

        val tnumId = creationState.document.addText("tnum")
        val zeroId = creationState.document.addText("zero")
        remoteContext.loadText(tnumId, "tnum")
        remoteContext.loadText(zeroId, "zero")

        tracker.updateWithPaint(paint, bundle, creationState)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.fontVariationAxesSet).isTrue()
        assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("tnum", "zero"))
        assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(1f, 1f))
    }

    @Test
    fun testFontFeatureAndVariationSettingsCombinedSync() {
        val settings = FontVariation.Settings(FontVariation.weight(600))
        val paint = RemotePaint {
            fontFeatureSettings = "tnum"
            fontVariationSettings = settings
        }
        val bundle = PaintBundle()

        val wghtId = creationState.document.addText("wght")
        val tnumId = creationState.document.addText("tnum")
        remoteContext.loadText(wghtId, "wght")
        remoteContext.loadText(tnumId, "tnum")

        tracker.updateWithPaint(paint, bundle, creationState)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.fontVariationAxesSet).isTrue()
        assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("wght", "tnum"))
        assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(600f, 1f))
    }

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testFontVariationSettingsSync_FallbackToWeight400() {
        val originalFlag = RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis
        RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis = false
        try {
            val paint = RemotePaint { fontVariationSettings = null }
            val bundle = PaintBundle()

            val wghtId = creationState.document.addText("wght")
            remoteContext.loadText(wghtId, "wght")

            tracker.updateWithPaint(paint, bundle, creationState)

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

    @Test
    @OptIn(ExperimentalRemoteCreationComposeApi::class)
    fun testFontVariationSettingsSync_FallbackToWeight400NotResentForEqualWeight() {
        val originalFlag = RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis
        RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis = false
        try {
            tracker.updateWithPaint(
                RemotePaint { fontVariationSettings = null },
                PaintBundle(),
                creationState,
            )
            tracker.reset(force = false)

            val paint = RemotePaint {
                fontVariationSettings = FontVariation.Settings(FontVariation.weight(400))
            }
            tracker.updateWithPaint(paint, PaintBundle(), creationState)

            assertThat(tracker.isChanged).isFalse()
        } finally {
            RemoteComposeCreationComposeFlags.allowSendingEmptyFontAxis = originalFlag
        }
    }

    @Test
    fun testFontVariationSettingsSync_NegativeZero() {
        val wghtId = creationState.document.addText("wght")
        remoteContext.loadText(wghtId, "wght")
        val negativeZeroPaint = RemotePaint {
            fontVariationSettings = FontVariation.Settings(FontVariation.Setting("wght", -0f))
        }
        val bundle = PaintBundle()
        tracker.updateWithPaint(negativeZeroPaint, bundle, creationState)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)
        assertThat(changes.mFontAxisValues!!.single().toRawBits()).isEqualTo((-0f).toRawBits())

        tracker.reset(force = false)
        val zeroPaint = RemotePaint {
            fontVariationSettings = FontVariation.Settings(FontVariation.Setting("wght", 0f))
        }
        tracker.updateWithPaint(zeroPaint, PaintBundle(), creationState)

        // 0f and -0f are equal axis values, so the axes are not sent again.
        assertThat(tracker.isChanged).isFalse()
    }

    @Test
    fun testFontFeatureSettingsOverrideVariationAxesInPlace() {
        val paint = RemotePaint {
            fontVariationSettings =
                FontVariation.Settings(FontVariation.width(100f), FontVariation.weight(700))
            fontFeatureSettings = "'wght' 300, tnum"
        }
        val bundle = PaintBundle()
        val wdthId = creationState.document.addText("wdth")
        val wghtId = creationState.document.addText("wght")
        val tnumId = creationState.document.addText("tnum")
        remoteContext.loadText(wdthId, "wdth")
        remoteContext.loadText(wghtId, "wght")
        remoteContext.loadText(tnumId, "tnum")

        tracker.updateWithPaint(paint, bundle, creationState)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)
        assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("wdth", "wght", "tnum"))
        assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(100f, 300f, 1f))
    }

    @Test
    fun testDuplicateFontFeatureSettings() {
        val featuresOnly = RemotePaint { fontFeatureSettings = "'tnum' 0, 'tnum' 1" }
        assertFailsWith<IllegalArgumentException> {
            tracker.updateWithPaint(featuresOnly, PaintBundle(), creationState)
        }

        val wghtId = creationState.document.addText("wght")
        val tnumId = creationState.document.addText("tnum")
        remoteContext.loadText(wghtId, "wght")
        remoteContext.loadText(tnumId, "tnum")
        val withVariations = RemotePaint {
            fontVariationSettings = FontVariation.Settings(FontVariation.weight(400))
            fontFeatureSettings = "'tnum' 0, 'tnum' 1"
        }
        val bundle = PaintBundle()
        tracker.reset(force = true)
        tracker.updateWithPaint(withVariations, bundle, creationState)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)
        assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("wght", "tnum"))
        assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(400f, 1f))
    }

    @Test
    fun testFontVariationSettingsSync_DensityDependentSettingThrows() {
        val paint = RemotePaint {
            fontVariationSettings = FontVariation.Settings(FontVariation.opticalSizing(16.sp))
        }

        val exception =
            assertFailsWith<IllegalArgumentException> {
                tracker.updateWithPaint(paint, PaintBundle(), creationState)
            }
        assertThat(exception).hasMessageThat().isEqualTo("density must not be null")
    }

    @Test
    fun testFontVariationSettingsSync_UnchangedDensityDependentSettingIsNotResolvedAgain() {
        val paint = RemotePaint {
            fontVariationSettings = FontVariation.Settings(FontVariation.opticalSizing(16.sp))
        }
        assertFailsWith<IllegalArgumentException> {
            tracker.updateWithPaint(paint, PaintBundle(), creationState)
        }

        tracker.reset(force = false)
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, creationState)

        // The tracker already holds these settings, so it does not resolve or send them again.
        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)
        assertThat(changes.fontVariationAxesSet).isFalse()
    }

    @Test
    fun testFontFeatureSettingsOverrideDensityDependentVariationAxis() {
        val paint = RemotePaint {
            fontVariationSettings = FontVariation.Settings(FontVariation.opticalSizing(16.sp))
            fontFeatureSettings = "'opsz' 12"
        }
        val bundle = PaintBundle()
        val opszId = creationState.document.addText("opsz")
        remoteContext.loadText(opszId, "opsz")

        tracker.updateWithPaint(paint, bundle, creationState)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)
        assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("opsz"))
        assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(12f))
    }

    // Note: The following tests are partially disabled out because Robolectric does not support
    // reading fontVariationSettings from android.graphics.Paint (the getter returns null).
    // See https://github.com/robolectric/robolectric/issues/11126
    // This prevents us from verifying that settings applied to Android Paint are picked up
    // by PaintTracker in unit tests. These should work on a real device or emulator (API 26+).
    //

    @Test
    fun testFontVariationSettingsSync_RemotePaint() {
        assumeTrue(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)

        val settings = FontVariation.Settings(FontVariation.weight(500), FontVariation.width(100f))
        val paint = RemotePaint { fontVariationSettings = settings }
        val bundle = PaintBundle()

        // TODO fix after https://github.com/robolectric/robolectric/issues/11126
        //         assertThat(paint.fontVariationSettings).isNotNull()

        val wghtId = creationState.document.addText("wght")
        val wdthId = creationState.document.addText("wdth")
        remoteContext.loadText(wghtId, "wght")
        remoteContext.loadText(wdthId, "wdth")

        tracker.updateWithPaint(paint, bundle, creationState)

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

        val paint = androidPaint.asRemotePaint()
        val bundle = PaintBundle()

        val wghtId = creationState.document.addText("wght")
        val wdthId = creationState.document.addText("wdth")
        remoteContext.loadText(wghtId, "wght")
        remoteContext.loadText(wdthId, "wdth")

        tracker.updateWithPaint(paint, bundle, creationState)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        // TODO fix after https://github.com/robolectric/robolectric/issues/11126
        //         assertThat(changes.fontVariationAxesSet).isTrue()
        //         assertThat(changes.mFontAxisTags).isEqualTo(arrayOf("wght", "wdth"))
        //         assertThat(changes.mFontAxisValues).isEqualTo(floatArrayOf(500f, 100f))
    }

    @Test
    fun paintSnapshotTest() {
        val paint = RemotePaint { color = Color.Red.rc }
        val remoteCanvas = RemoteCanvas(creationState)
        remoteCanvas.drawRect(0f.rf, 0f.rf, 100f.rf, 100f.rf, paint)

        // Modify the input paint immediately after capturing the draw operation
        paint.color = Color.Blue.rc

        // Verify that modifying the input paint does not alter the emitted UsePaint operations
        remoteCanvas.flush()
        val buffer = creationState.document.buffer
        val operations = ArrayList<Operation>()
        buffer.buffer.index = 0
        buffer.inflateFromBuffer(operations)

        val paintData = operations.filterIsInstance<PaintData>().first()
        val changes = TestPaintChanges()
        paintData.mPaintData.applyPaintChange(paintContext, changes)

        // If snapshotting failed, the mutation to Color.BLUE would have leaked into the recorded
        // operation
        assertThat(changes.mColor).isEqualTo(Color.Red.toArgb())
    }

    @Test
    fun testShaderMatrix_identityMatrix_noShaderMatrixSet() {
        val paint = RemotePaint { shader = DummyShader(createIdentity()) }
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, creationState)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.mShaderMatrix).isNull()
    }

    @Test
    fun testShaderMatrix_nonIdentityMatrix_setsShaderMatrix() {
        val paint = RemotePaint { shader = DummyShader(createRotate(45f.rf)) }
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, creationState)

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.mShaderMatrix).isNotNull()
        assertThat(changes.mShaderMatrix).isNotEqualTo(0f)
    }

    @Test
    fun testShaderMatrix_transitionFromNonIdentityToIdentity_resetsShaderMatrix() {
        val paint1 = RemotePaint { shader = DummyShader(createRotate(45f.rf)) }
        val bundle1 = PaintBundle()
        tracker.updateWithPaint(paint1, bundle1, creationState)

        val paint2 = RemotePaint { shader = DummyShader(createIdentity()) }
        val bundle2 = PaintBundle()
        tracker.updateWithPaint(paint2, bundle2, creationState)

        val changes = TestPaintChanges()
        bundle2.applyPaintChange(paintContext, changes)

        assertThat(changes.mShaderMatrix).isEqualTo(0f)
    }

    @Test
    fun testShaderMatrix_transitionFromIdentityToIdentity_doesNotSetShaderMatrix() {
        val paint1 = RemotePaint { shader = DummyShader(createIdentity()) }
        val bundle1 = PaintBundle()
        tracker.updateWithPaint(paint1, bundle1, creationState)

        val paint2 = RemotePaint { shader = DummyShader(createIdentity()) }
        val bundle2 = PaintBundle()
        tracker.updateWithPaint(paint2, bundle2, creationState)

        val changes = TestPaintChanges()
        bundle2.applyPaintChange(paintContext, changes)

        assertThat(changes.mShaderMatrix).isNull()
    }

    @Test
    fun testFilterQuality_initialSync_default() {
        val paint = RemotePaint {}
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, creationState)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.filterBitmapSet).isTrue()
        assertThat(changes.mFilterBitmap).isTrue() // Defaults to Low -> true
    }

    @Test
    fun testFilterQuality_initialSync_none() {
        val paint = RemotePaint { filterQuality = RemoteFilterQuality.None }
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, creationState)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.filterBitmapSet).isTrue()
        assertThat(changes.mFilterBitmap).isFalse()
    }

    @Test
    fun testFilterQuality_initialSync_low() {
        val paint = RemotePaint { filterQuality = RemoteFilterQuality.Low }
        val bundle = PaintBundle()
        tracker.updateWithPaint(paint, bundle, creationState)

        assertThat(tracker.isChanged).isTrue()

        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)

        assertThat(changes.filterBitmapSet).isTrue()
        assertThat(changes.mFilterBitmap).isTrue()
    }

    @Test
    fun testFilterQuality_deltaOptimization() {
        // Initial draw with Low (filtering on)
        val paint1 = RemotePaint { filterQuality = RemoteFilterQuality.Low }
        tracker.updateWithPaint(paint1, PaintBundle(), creationState)
        tracker.reset(force = false)

        // Second draw with None (filtering off) -> should emit update
        val paint2 = RemotePaint { filterQuality = RemoteFilterQuality.None }
        val bundle2 = PaintBundle()
        tracker.updateWithPaint(paint2, bundle2, creationState)

        assertThat(tracker.isChanged).isTrue()
        val changes2 = TestPaintChanges()
        bundle2.applyPaintChange(paintContext, changes2)
        assertThat(changes2.filterBitmapSet).isTrue()
        assertThat(changes2.mFilterBitmap).isFalse()

        // Third draw with None -> delta optimization should NOT re-emit filterBitmap
        tracker.reset(force = false)
        val paint3 = RemotePaint { filterQuality = RemoteFilterQuality.None }
        val bundle3 = PaintBundle()
        tracker.updateWithPaint(paint3, bundle3, creationState)
        val changes3 = TestPaintChanges()
        bundle3.applyPaintChange(paintContext, changes3)
        assertThat(changes3.filterBitmapSet).isFalse()
    }

    @Test
    fun testFilterQuality_compatAndroidRemotePaintFilterBitmapFalse() {
        val compatPaint = CompatAndroidRemotePaint().apply { isFilterBitmap = false }
        val bundle = PaintBundle()
        tracker.updateWithPaint(compatPaint.asRemotePaint(), bundle, creationState)

        assertThat(tracker.isChanged).isTrue()
        val changes = TestPaintChanges()
        bundle.applyPaintChange(paintContext, changes)
        assertThat(changes.filterBitmapSet).isTrue()
        assertThat(changes.mFilterBitmap).isFalse()
    }

    private class DummyShader(override var remoteMatrix3x3: RemoteMatrix3x3) : RemoteShader() {
        override fun apply(creationState: RemoteComposeCreationState, paintBundle: PaintBundle) {
            paintBundle.setShader(1)
        }
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
        var mFontString: String? = null
        var mWeight: Int = -1
        var mItalic: Boolean = false

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
            this.mWeight = weight
            this.mItalic = italic
        }

        override fun setTextSize(size: Float) {}

        override fun setTypeFace(fontString: String, weight: Int, italic: Boolean) {
            this.mFontString = fontString
            this.mWeight = weight
            this.mItalic = italic
        }

        override fun setFallbackTypeFace(fontType: Int, weight: Int, italic: Boolean) {}

        override fun setShader(shader: Int) {}

        override fun setImageFilterQuality(quality: Int) {}

        override fun setBlendMode(mode: Int) {}

        var mFilterBitmap: Boolean = true
        var filterBitmapSet: Boolean = false

        override fun setFilterBitmap(filter: Boolean) {
            this.mFilterBitmap = filter
            this.filterBitmapSet = true
        }

        override fun setAntiAlias(aa: Boolean) {}

        var mShaderMatrix: Float? = null
        var mColorFilterColor: Int = 0
        var mColorFilterMode: Int = -1
        var colorFilterSet: Boolean = false
        var mClearedMask: Long = 0L

        override fun setColorFilter(color: Int, mode: Int) {
            this.mColorFilterColor = color
            this.mColorFilterMode = mode
            this.colorFilterSet = true
        }

        override fun setShaderMatrix(matrixId: Float) {
            this.mShaderMatrix = matrixId
        }

        override fun clear(mask: Long) {
            this.mClearedMask = this.mClearedMask or mask
        }

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

        override fun setRadialGradient(
            colors: IntArray,
            stops: FloatArray?,
            startX: Float,
            startY: Float,
            startRadius: Float,
            endX: Float,
            endY: Float,
            endRadius: Float,
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
