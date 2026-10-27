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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.PorterDuff
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.BitmapFontData
import androidx.compose.remote.core.operations.SoundExpression
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.platform.AndroidxRcPlatformServices
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Draw-stream behaviour of impulse blocks (gated on their start time and duration, with sound and
 * haptic side effects) and of bitmap-font text (drawn with the full paint state) in the embedded
 * player.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RcPlayerImpulseAndBitmapFontTest {

    @get:Rule val rule = RcPlayerTestRule()

    private val experimentalProfile =
        Profile(
            CoreDocument.DOCUMENT_API_LEVEL,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            AndroidxRcPlatformServices(),
        ) { creationDisplayInfo, profile, callback ->
            RemoteComposeWriterAndroid(creationDisplayInfo, null, profile, callback)
        }

    @Test
    fun impulseBodyIsNotDrawnBeforeItStarts() {
        rule.mainClock.autoAdvance = false
        play {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                document.impulse(0.5f, 1000f) {
                    (document as RemoteComposeWriterAndroid)
                        .getRcPaint()
                        .setColor(AndroidColor.RED)
                        .commit()
                    document.drawRect(0f, 0f, 100f, 100f)
                }
            }
        }

        repeat(3) {
            rule.mainClock.advanceTimeBy(32)
            assertThat(isRed(capture().getPixel(50, 50))).isFalse()
        }
    }

    @Test
    fun impulseDrawsOnlyInsideItsWindow() {
        rule.mainClock.autoAdvance = false
        play {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                // Active from 1s to 1.5s of animation time. The initial pass and the process both
                // draw, so every frame inside the window is red.
                document.impulse(0.5f, 1f) {
                    (document as RemoteComposeWriterAndroid)
                        .getRcPaint()
                        .setColor(AndroidColor.RED)
                        .commit()
                    document.drawRect(0f, 0f, 100f, 100f)
                    document.impulseProcess {
                        (document as RemoteComposeWriterAndroid)
                            .getRcPaint()
                            .setColor(AndroidColor.RED)
                            .commit()
                        document.drawRect(0f, 0f, 100f, 100f)
                    }
                }
            }
        }
        assertThat(isRed(capture().getPixel(50, 50))).isFalse()

        rule.mainClock.advanceTimeBy(1200)
        assertThat(isRed(capture().getPixel(50, 50))).isTrue()
        rule.mainClock.advanceTimeBy(100)
        assertThat(isRed(capture().getPixel(50, 50))).isTrue()

        rule.mainClock.advanceTimeBy(1000)
        assertThat(isRed(capture().getPixel(50, 50))).isFalse()
    }

    @Test
    fun hapticInAnInactiveImpulseIsNotPerformed() {
        rule.mainClock.autoAdvance = false
        var hostView: View? = null
        play(onView = { hostView = it }) {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                document.impulse(0.5f, 1000f) { document.performHaptic(1) }
            }
        }
        val shadowView = Shadows.shadowOf(requireNotNull(hostView))

        repeat(3) {
            rule.mainClock.advanceTimeBy(32)
            capture()
        }

        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isNotEqualTo(HapticFeedbackConstantsCompat.LONG_PRESS)
    }

    @Test
    fun hapticInAnImpulseIsPerformedOnceWhenItsWindowOpens() {
        rule.mainClock.autoAdvance = false
        var hostView: View? = null
        play(onView = { hostView = it }) {
            RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                document.impulse(0.5f, 1f) { document.performHaptic(1) }
            }
        }
        val view = requireNotNull(hostView)
        val shadowView = Shadows.shadowOf(view)
        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isNotEqualTo(HapticFeedbackConstantsCompat.LONG_PRESS)

        rule.mainClock.advanceTimeBy(1200)
        capture()
        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isEqualTo(HapticFeedbackConstantsCompat.LONG_PRESS)

        // Later frames in the same window don't repeat the initial pass.
        view.performHapticFeedback(HapticFeedbackConstantsCompat.CLOCK_TICK)
        repeat(3) {
            rule.mainClock.advanceTimeBy(32)
            capture()
        }
        assertThat(shadowView.lastHapticFeedbackPerformed())
            .isEqualTo(HapticFeedbackConstantsCompat.CLOCK_TICK)
    }

    @Test
    fun hapticInARootLevelImpulseIsNotPerformedAtLoad() {
        rule.mainClock.autoAdvance = false
        val document = buildDocument {
            writer.impulse(0.5f, 1000f) { writer.performHaptic(1) }
            writer.drawCircle(1f, 1f, 1f)
        }
        var hostView: View? = null
        rule.setContent {
            val view = LocalView.current
            view.isHapticFeedbackEnabled = true
            hostView = view
            Box(modifier = Modifier.size(100.dp)) { RcPlayer(document = document) }
        }
        repeat(3) {
            rule.mainClock.advanceTimeBy(32)
            capture()
        }

        assertThat(Shadows.shadowOf(requireNotNull(hostView)).lastHapticFeedbackPerformed())
            .isNotEqualTo(HapticFeedbackConstantsCompat.LONG_PRESS)
    }

    @Test
    fun soundInAnImpulseIsPlayedOnceWhenItsWindowOpens() {
        rule.mainClock.autoAdvance = false
        var soundId = 0
        val document =
            rule.setRemoteContent(profile = experimentalProfile) {
                RemoteCanvas(modifier = RemoteModifier.fillMaxSize()) {
                    val id =
                        document.addSoundExpression(
                            SoundExpression.TYPE_TONE,
                            440f,
                            0.1f,
                            0f,
                            1f,
                            1f,
                            1f,
                        )
                    soundId = id
                    document.impulse(0.5f, 1f) { document.playSound(id) }
                    document.impulse(0.5f, 1000f) { document.playSound(id) }
                    document.drawRect(0f, 0f, 1f, 1f)
                }
            }
        val loaded = mutableListOf<Int>()
        val played = mutableListOf<Int>()
        document.setSoundEngine(
            object : CoreDocument.SoundEngine {
                override fun loadSound(soundId: Int, data: ByteArray) {
                    loaded += soundId
                }

                override fun playSound(soundId: Int) {
                    played += soundId
                }
            }
        )
        // The tone was synthesized once at load and handed over when the engine was installed.
        assertThat(loaded).containsExactly(soundId)

        repeat(3) {
            rule.mainClock.advanceTimeBy(32)
            capture()
        }
        assertThat(played).isEmpty()

        rule.mainClock.advanceTimeBy(1200)
        capture()
        repeat(3) {
            rule.mainClock.advanceTimeBy(32)
            capture()
        }
        assertThat(played).containsExactly(soundId)
        assertThat(loaded).containsExactly(soundId)
    }

    @Test
    fun bitmapFontTextAppliesPaintAlphaAndColorFilter() {
        val bitmap = renderBitmapFont { textId, fontId ->
            writer.drawBitmapFontTextRun(textId, fontId, 0, -1, 10f, 10f, 0f)
        }
        // The 20x20 glyph covers (10, 10) - (30, 30).
        assertHalfAlphaRedOverWhite(bitmap.getPixel(20, 20))
    }

    @Test
    fun bitmapTextAnchoredAppliesPaintAlphaAndColorFilter() {
        val bitmap = renderBitmapFont { textId, fontId ->
            writer.drawBitmapTextAnchored(textId, fontId, 0f, -1f, 50f, 50f, 0f, 0f, 0f)
        }
        // Centered on (50, 50).
        assertHalfAlphaRedOverWhite(bitmap.getPixel(50, 50))
    }

    @Test
    fun bitmapFontTextOnPathAppliesPaintAlphaAndColorFilter() {
        val bitmap = renderBitmapFont { textId, fontId ->
            val path = writer.pathCreate(0f, 50f)
            writer.pathAppendLineTo(path, 100f, 50f)
            writer.drawBitmapFontTextRunOnPath(textId, fontId, path, 0, -1, 0f, 0f)
        }
        // The glyph is centered horizontally on the path midpoint and hangs below it: (40, 50) -
        // (60, 70).
        assertHalfAlphaRedOverWhite(bitmap.getPixel(50, 60))
    }

    /**
     * Renders one white 20x20 bitmap-font glyph "A" with a half-alpha paint and a red SRC_IN color
     * filter over a white background.
     */
    private fun renderBitmapFont(
        draw: RemoteComposeContextAndroid.(textId: Int, fontId: Int) -> Unit
    ): Bitmap {
        val glyphBitmap =
            Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.WHITE)
            }
        val document = buildDocument {
            val imageId = addBitmap(glyphBitmap)
            val fontId =
                writer.addBitmapFont(
                    arrayOf(BitmapFontData.Glyph("A", imageId, 0, 0, 0, 0, 20, 20))
                )
            val textId = writer.addText("A")
            painter
                .setAlpha(0.5f)
                .setPorterDuffColorFilter(AndroidColor.RED, PorterDuff.Mode.SRC_IN)
                .commit()
            draw(textId, fontId)
        }
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(modifier = Modifier.size(100.dp).background(Color.White)) {
                    RcPlayer(document = document)
                }
            }
        }
        return capture()
    }

    private fun assertHalfAlphaRedOverWhite(pixel: Int) {
        // Tinted red by the color filter and blended at half alpha over white: about (255, 128,
        // 128). Without the paint it stays white (green 255); without the alpha it is pure red.
        assertThat(AndroidColor.red(pixel)).isGreaterThan(200)
        assertThat(AndroidColor.green(pixel)).isIn(Range.open(60, 200))
        assertThat(AndroidColor.blue(pixel)).isIn(Range.open(60, 200))
    }

    private fun isRed(pixel: Int): Boolean =
        AndroidColor.red(pixel) > 200 &&
            AndroidColor.green(pixel) < 60 &&
            AndroidColor.alpha(pixel) > 200

    private fun capture(): Bitmap = rule.captureRootBitmap()

    private fun play(
        onView: (View) -> Unit = {},
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        rule.setRemoteContent(
            profile = RcPlatformProfiles.ANDROIDX,
            playComposableWrapper = { composable ->
                val view = LocalView.current
                view.isHapticFeedbackEnabled = true
                onView(view)
                Box(modifier = Modifier.size(100.dp)) { composable() }
            },
            content = content,
        )
    }
}
