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

package androidx.wear.compose.remote.material3

import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.RemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteEasing
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.wear.compose.remote.material3.samples.RemoteCheckboxButtonSample
import androidx.wear.compose.remote.material3.samples.RemoteRadioButtonSample
import androidx.wear.compose.remote.material3.samples.RemoteSplitCheckboxButtonSample
import androidx.wear.compose.remote.material3.samples.RemoteSplitRadioButtonSample
import androidx.wear.compose.remote.material3.samples.RemoteSplitSwitchButtonSample
import androidx.wear.compose.remote.material3.samples.RemoteSwitchButtonSample
import androidx.wear.compose.remote.material3.util.TestImageVectors
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [35], qualifiers = "w1000dp-h500dp-xxxhdpi")
@RunWith(RobolectricTestRunner::class)
class RemoteSelectionButtonAnimationTest {

    @get:Rule val painterRule = DocumentPainterRule()

    @Test
    fun selectionButtons_followThemeMotionScheme() {
        for (sample in Samples) {
            val defaultDocument = painterRule.captureBytes { RemoteMaterialTheme { sample() } }
            val standardDocument = painterRule.captureBytes {
                RemoteMaterialTheme(motionScheme = RemoteMotionScheme.standard()) { sample() }
            }
            val slowerControlDocument = painterRule.captureBytes {
                RemoteMaterialTheme(motionScheme = FastEffectsAsSlowScheme) { sample() }
            }
            val fasterColorsDocument = painterRule.captureBytes {
                RemoteMaterialTheme(motionScheme = SlowEffectsAsFastScheme) { sample() }
            }

            assertArrayEquals(defaultDocument, standardDocument)
            // The selection control animates with the fast effects spec, and the container and
            // content colors with the slow effects spec.
            assertFalse(defaultDocument.contentEquals(slowerControlDocument))
            assertFalse(defaultDocument.contentEquals(fasterColorsDocument))
        }
    }

    @Test
    fun checkboxButton_containerColorAnimatesWithSlowEffects() {
        val gray = containerGrayAfterChecking(RemoteMotionScheme.standard())
        val grayWithFastEffects = containerGrayAfterChecking(SlowEffectsAsFastScheme)

        // The container has started to turn from black to white, but more slowly than it would
        // with the fast effects spec of the selection control.
        assertThat(gray).isGreaterThan(0.1f)
        assertThat(gray).isLessThan(grayWithFastEffects - 0.1f)
    }

    @Test
    fun checkboxButton_colorsFollowOverriddenSlowEffectsSpec() {
        val gray = containerGrayAfterChecking(LinearSlowEffectsScheme, HalfLinearSpecMillis)
        val standardGray =
            containerGrayAfterChecking(RemoteMotionScheme.standard(), HalfLinearSpecMillis)

        // Halfway through the linear spec, the container is halfway from black to white, while
        // with the standard spring it has already turned white by then.
        assertThat(gray).isWithin(GrayTolerance).of(HalfwayGray)
        assertThat(standardGray).isWithin(GrayTolerance).of(1f)
    }

    @Test
    fun checkboxButton_checkboxFollowsOverriddenFastEffectsSpec() {
        val boxPixels = boxPixelsAfterChecking(LinearFastEffectsScheme, HalfLinearSpecMillis)
        val standardBoxPixels =
            boxPixelsAfterChecking(RemoteMotionScheme.standard(), HalfLinearSpecMillis)
        val filledBoxPixels = boxPixelsAfterChecking(LinearFastEffectsScheme, AfterLinearSpecMillis)

        // Halfway through the linear spec, the box is still fading in, while with the standard
        // spring it has already been filled by then.
        assertThat(filledBoxPixels).isGreaterThan(0)
        assertThat(standardBoxPixels).isEqualTo(filledBoxPixels)
        assertThat(boxPixels).isLessThan(filledBoxPixels / 10)
    }

    @Test
    fun selectionButtons_tintIconWithIconColor() {
        for (button in ButtonsWithIcon) {
            val painter = painterRule.paintDocument {
                button(RemoteBoolean.createNamedRemoteBoolean(CheckedName, false))
            }

            assertThat(painter.pixelsWhere(::isUnselectedIconColor)).isNotEmpty()
            assertThat(painter.pixelsWhere(::isSelectedIconColor)).isEmpty()

            painter.setBoolean(CheckedName, true)
            painter.advanceTimeBy(SettledMillis)

            assertThat(painter.pixelsWhere(::isUnselectedIconColor)).isEmpty()
            assertThat(painter.pixelsWhere(::isSelectedIconColor)).isNotEmpty()
        }
    }

    @Test
    fun checkbox_tickRotatesOnlyWhileBeingChecked() {
        val painter = painterRule.paintDocument {
            // On black, the red channel shows how opaque the red tick is in each pixel.
            RemoteBox(modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc)) {
                RemoteCheckboxControl(
                    checked = RemoteBoolean.createNamedRemoteBoolean(CheckedName, true),
                    boxColor = Color.Transparent.rc,
                    checkmarkColor = Color.Red.rc,
                    progress = RemoteFloat.createNamedRemoteFloat(ProgressName, 1f),
                )
            }
        }
        val restingTick = painter.pixelsWhere(::isTick)

        painter.setFloat(ProgressName, PartialProgress)
        val checkingTick = painter.pixelsWhere(::isPartialTick)

        painter.setBoolean(CheckedName, false)
        val uncheckingTick = painter.pixelsWhere(::isPartialTick)

        // While being checked, the partly drawn tick is rotated away from where the tick rests.
        assertThat(checkingTick).isNotEmpty()
        assertThat(checkingTick - restingTick).isNotEmpty()
        // While being unchecked, it isn't rotated, so it lies within the resting tick.
        assertThat(uncheckingTick).isNotEmpty()
        assertThat(uncheckingTick - restingTick).isEmpty()
    }

    /**
     * Renders a checkbox button with [colors] and [motionScheme], checks it, and returns the
     * painter [afterMillis] later.
     */
    private fun checkboxButtonAfterChecking(
        motionScheme: RemoteMotionScheme,
        afterMillis: Long,
        colors: @Composable @RemoteComposable () -> RemoteCheckboxButtonColors,
    ): DocumentPainterRule.DocumentPainter {
        val painter = painterRule.paintDocument {
            RemoteMaterialTheme(motionScheme = motionScheme) {
                RemoteCheckboxButton(
                    checked = RemoteBoolean.createNamedRemoteBoolean(CheckedName, false),
                    onCheckedChange = hostAction("toggle".rs),
                    modifier = RemoteModifier.fillMaxSize(),
                    colors = colors(),
                    label = {},
                )
            }
        }
        painter.setBoolean(CheckedName, true)
        painter.advanceTimeBy(afterMillis)
        return painter
    }

    /**
     * Checks a checkbox button whose container turns from black to white, with [motionScheme], and
     * returns the gray level of the container [afterMillis] later.
     */
    private fun containerGrayAfterChecking(
        motionScheme: RemoteMotionScheme,
        afterMillis: Long = EarlyMillis,
    ): Float {
        val painter =
            checkboxButtonAfterChecking(motionScheme, afterMillis) {
                RemoteCheckboxButtonDefaults.checkboxButtonColors(
                    uncheckedContainerColor = Color.Black.rc,
                    checkedContainerColor = Color.White.rc,
                )
            }
        // The middle of the start half of the button, away from the checkbox.
        return painter.colorAt(x = painter.width / 4, y = painter.height / 2).red
    }

    /**
     * Checks a checkbox button on black, whose box is red, with [motionScheme], and returns how
     * many pixels have the box color [afterMillis] later. While the box fades in, it is partly
     * transparent, so it doesn't have the box color yet.
     */
    private fun boxPixelsAfterChecking(motionScheme: RemoteMotionScheme, afterMillis: Long): Int {
        val painter =
            checkboxButtonAfterChecking(motionScheme, afterMillis) {
                RemoteCheckboxButtonDefaults.checkboxButtonColors(
                    checkedContainerColor = Color.Black.rc,
                    uncheckedContainerColor = Color.Black.rc,
                    checkedBoxColor = Color.Red.rc,
                    uncheckedBoxColor = Color.Red.rc,
                    checkedCheckmarkColor = Color.Black.rc,
                )
            }
        return painter.countPixels(::isBoxColor)
    }

    private fun isTick(color: Color): Boolean = color.red > 0f

    private fun isPartialTick(color: Color): Boolean = color.red > 0.1f

    private fun isBoxColor(color: Color): Boolean =
        color.red > 0.9f && color.green < 0.1f && color.blue < 0.1f

    private fun isUnselectedIconColor(color: Color): Boolean =
        color.red > 0.9f && color.green < 0.1f && color.blue < 0.1f

    private fun isSelectedIconColor(color: Color): Boolean =
        color.blue > 0.9f && color.red < 0.1f && color.green < 0.1f

    private companion object {
        const val CheckedName = "checked"
        const val ProgressName = "progress"

        /** A progress partway through drawing the tick, while it's still rotated. */
        const val PartialProgress = 0.5f

        /** A moment into the selection animation, before it settles. */
        const val EarlyMillis = 50L

        /** Long after the selection animation settles. */
        const val SettledMillis = 3000L

        /** How long [LinearSpec] takes, in milliseconds. */
        const val LinearSpecMillis = 1000

        /** Halfway through [LinearSpec], in milliseconds. */
        const val HalfLinearSpecMillis = LinearSpecMillis / 2L

        /** Once [LinearSpec] has finished, in milliseconds. */
        const val AfterLinearSpecMillis = LinearSpecMillis + EarlyMillis

        /** How much a measured gray level may differ from the expected one. */
        const val GrayTolerance = 0.05f

        /** The gray level halfway from black to white, as Remote Compose interpolates colors. */
        val HalfwayGray: Float =
            Color(Utils.interpolateColor(Color.Black.toArgb(), Color.White.toArgb(), 0.5f)).red

        /** A spec that animates at a constant rate, unlike the springs of the standard scheme. */
        val LinearSpec: RemoteAnimationSpec =
            remoteTween(durationMillis = LinearSpecMillis, easing = RemoteEasing.Linear)

        /** The standard scheme, with [LinearSpec] as its slow effects spec. */
        val LinearSlowEffectsScheme: RemoteMotionScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun slowEffectsSpec(): RemoteAnimationSpec = LinearSpec
            }

        /** The standard scheme, with [LinearSpec] as its fast effects spec. */
        val LinearFastEffectsScheme: RemoteMotionScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun fastEffectsSpec(): RemoteAnimationSpec = LinearSpec
            }

        /** A scheme whose fast effects are as slow as its slow effects. */
        val FastEffectsAsSlowScheme: RemoteMotionScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun fastEffectsSpec(): RemoteAnimationSpec = slowEffectsSpec()
            }

        /** A scheme whose slow effects are as fast as its fast effects. */
        val SlowEffectsAsFastScheme: RemoteMotionScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun slowEffectsSpec(): RemoteAnimationSpec = fastEffectsSpec()
            }

        val Samples: List<@Composable @RemoteComposable () -> Unit> =
            listOf(
                { RemoteCheckboxButtonSample() },
                { RemoteSwitchButtonSample() },
                { RemoteRadioButtonSample() },
                { RemoteSplitCheckboxButtonSample() },
                { RemoteSplitSwitchButtonSample() },
                { RemoteSplitRadioButtonSample() },
            )

        /** Selection buttons whose icon is red when unselected, and blue when selected. */
        val ButtonsWithIcon: List<@Composable @RemoteComposable (selected: RemoteBoolean) -> Unit> =
            listOf(
                { selected ->
                    RemoteCheckboxButton(
                        checked = selected,
                        onCheckedChange = hostAction("toggle".rs),
                        colors =
                            RemoteCheckboxButtonDefaults.checkboxButtonColors(
                                uncheckedIconColor = Color.Red.rc,
                                checkedIconColor = Color.Blue.rc,
                            ),
                        icon = { RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null) },
                        label = {},
                    )
                },
                { selected ->
                    RemoteSwitchButton(
                        checked = selected,
                        onCheckedChange = hostAction("toggle".rs),
                        colors =
                            RemoteSwitchButtonDefaults.switchButtonColors(
                                uncheckedIconColor = Color.Red.rc,
                                checkedIconColor = Color.Blue.rc,
                            ),
                        icon = { RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null) },
                        label = {},
                    )
                },
                { selected ->
                    RemoteRadioButton(
                        selected = selected,
                        onSelect = hostAction("select".rs),
                        colors =
                            RemoteRadioButtonDefaults.radioButtonColors(
                                unselectedIconColor = Color.Red.rc,
                                selectedIconColor = Color.Blue.rc,
                            ),
                        icon = { RemoteIcon(TestImageVectors.VolumeUp, contentDescription = null) },
                        label = {},
                    )
                },
            )
    }
}
