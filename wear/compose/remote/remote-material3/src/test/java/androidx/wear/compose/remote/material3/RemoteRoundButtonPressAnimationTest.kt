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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.Profile
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteLayoutDirection
import androidx.compose.remote.creation.compose.layout.RemoteOffset
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.shapes.RemoteCornerBasedShape
import androidx.compose.remote.creation.compose.shapes.RemoteCornerSize
import androidx.compose.remote.creation.compose.shapes.RemoteOutline
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.shapes.RemoteShape
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.RemoteAnimationSpec
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.remoteTween
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.samples.RemoteIconButtonWithCornerAnimationSample
import androidx.wear.compose.remote.material3.samples.RemoteTextButtonWithCornerAnimationSample
import androidx.wear.compose.remote.material3.util.TestProfiles
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [RemoteIconButton], [RemoteTextButton], [RemoteIconToggleButton], and
 * [RemoteTextToggleButton] pressed-shape animations driven through [RemoteRoundButton].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w200dp-h200dp-mdpi")
class RemoteRoundButtonPressAnimationTest {

    @get:Rule val playerRule = RcPlayerTestRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val namedActions = mutableListOf<String>()

    @Test
    fun iconButtonDefaults_shapesAndAnimatedShapes_resolveExpectedShapes() {
        val customShape = RemoteRoundedCornerShape(12.rdp)
        val customPressedShape = RemoteRoundedCornerShape(4.rdp)
        val customThemeSmall = RemoteRoundedCornerShape(6.rdp)

        var defaultShapes: RemoteIconButtonShapes? = null
        var customStaticShapes: RemoteIconButtonShapes? = null
        var defaultAnimatedShapes: RemoteIconButtonShapes? = null
        var customAnimatedShapes: RemoteIconButtonShapes? = null
        var themedPressedShape: RemoteShape? = null

        setButtonContent {
            RemoteMaterialTheme(shapes = RemoteShapes(small = customThemeSmall)) {
                defaultShapes = RemoteIconButtonDefaults.shapes()
                customStaticShapes = RemoteIconButtonDefaults.shapes(customShape)
                defaultAnimatedShapes = RemoteIconButtonDefaults.animatedShapes()
                customAnimatedShapes =
                    RemoteIconButtonDefaults.animatedShapes(
                        shape = customShape,
                        pressedShape = customPressedShape,
                    )
                themedPressedShape = RemoteIconButtonDefaults.pressedShape
            }
        }

        assertThat(defaultShapes).isEqualTo(RemoteIconButtonShapes(shape = RemoteCircleShape))
        assertThat(defaultShapes!!.pressedShape).isEqualTo(RemoteCircleShape)

        assertThat(customStaticShapes).isEqualTo(RemoteIconButtonShapes(shape = customShape))
        assertThat(customStaticShapes!!.pressedShape).isEqualTo(customShape)

        assertThat(themedPressedShape).isEqualTo(customThemeSmall)
        assertThat(defaultAnimatedShapes)
            .isEqualTo(
                RemoteIconButtonShapes(shape = RemoteCircleShape, pressedShape = customThemeSmall)
            )
        assertThat(customAnimatedShapes)
            .isEqualTo(
                RemoteIconButtonShapes(shape = customShape, pressedShape = customPressedShape)
            )
    }

    @Test
    fun textButtonDefaults_shapesAndAnimatedShapes_resolveExpectedShapes() {
        val customShape = RemoteRoundedCornerShape(12.rdp)
        val customPressedShape = RemoteRoundedCornerShape(4.rdp)
        val customThemeSmall = RemoteRoundedCornerShape(6.rdp)

        var defaultShapes: RemoteTextButtonShapes? = null
        var customStaticShapes: RemoteTextButtonShapes? = null
        var defaultAnimatedShapes: RemoteTextButtonShapes? = null
        var customAnimatedShapes: RemoteTextButtonShapes? = null
        var themedPressedShape: RemoteShape? = null

        setButtonContent {
            RemoteMaterialTheme(shapes = RemoteShapes(small = customThemeSmall)) {
                defaultShapes = RemoteTextButtonDefaults.shapes()
                customStaticShapes = RemoteTextButtonDefaults.shapes(customShape)
                defaultAnimatedShapes = RemoteTextButtonDefaults.animatedShapes()
                customAnimatedShapes =
                    RemoteTextButtonDefaults.animatedShapes(
                        shape = customShape,
                        pressedShape = customPressedShape,
                    )
                themedPressedShape = RemoteTextButtonDefaults.pressedShape
            }
        }

        assertThat(defaultShapes).isEqualTo(RemoteTextButtonShapes(shape = RemoteCircleShape))
        assertThat(defaultShapes!!.pressedShape).isEqualTo(RemoteCircleShape)

        assertThat(customStaticShapes).isEqualTo(RemoteTextButtonShapes(shape = customShape))
        assertThat(customStaticShapes!!.pressedShape).isEqualTo(customShape)

        assertThat(themedPressedShape).isEqualTo(customThemeSmall)
        assertThat(defaultAnimatedShapes)
            .isEqualTo(
                RemoteTextButtonShapes(shape = RemoteCircleShape, pressedShape = customThemeSmall)
            )
        assertThat(customAnimatedShapes)
            .isEqualTo(
                RemoteTextButtonShapes(shape = customShape, pressedShape = customPressedShape)
            )
    }

    @Test
    fun buttonShapes_copyEqualsAndHashCode() {
        val circle = RemoteCircleShape
        val small = RemoteShapeDefaults.Small
        val medium = RemoteShapeDefaults.Medium

        val iconShapes = RemoteIconButtonShapes(shape = circle, pressedShape = small)
        assertThat(iconShapes.copy()).isEqualTo(iconShapes)
        assertThat(iconShapes.copy().hashCode()).isEqualTo(iconShapes.hashCode())
        assertThat(iconShapes.copy(shape = medium))
            .isEqualTo(RemoteIconButtonShapes(shape = medium, pressedShape = small))
        assertThat(iconShapes.copy(pressedShape = medium))
            .isEqualTo(RemoteIconButtonShapes(shape = circle, pressedShape = medium))
        assertThat(iconShapes).isNotEqualTo(RemoteIconButtonShapes(shape = circle))

        val textShapes = RemoteTextButtonShapes(shape = circle, pressedShape = small)
        assertThat(textShapes.copy()).isEqualTo(textShapes)
        assertThat(textShapes.copy().hashCode()).isEqualTo(textShapes.hashCode())
        assertThat(textShapes.copy(shape = medium))
            .isEqualTo(RemoteTextButtonShapes(shape = medium, pressedShape = small))
        assertThat(textShapes.copy(pressedShape = medium))
            .isEqualTo(RemoteTextButtonShapes(shape = circle, pressedShape = medium))
        assertThat(textShapes).isNotEqualTo(RemoteTextButtonShapes(shape = circle))
    }

    @Test
    fun iconButton_animatedShapes_morphsCornersOnPressAndRestoresOnRelease() {
        setButtonContent {
            RemoteIconButton(
                onClick = hostAction("click".rs, 1.rf),
                shapes = RemoteIconButtonDefaults.animatedShapes(),
                colors =
                    RemoteIconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Red.rc,
                        contentColor = Color.Red.rc,
                    ),
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
            ) {}
        }

        val restingPixels = buttonPixelCount()
        assertCornerIsBackground()

        press()
        advance(SettledMillis)

        val pressedPixels = buttonPixelCount()
        assertCornerIsButton()
        // Outer corner (0, 0) remains clipped outside the 8dp pressed corner radius.
        assertThat(isButtonPixel(0, 0)).isFalse()
        assertThat(pressedPixels).isGreaterThan(restingPixels + MinMorphedPixelGain)

        release()
        advance(SettledMillis)

        assertCornerIsBackground()
        assertThat(buttonPixelCount()).isEqualTo(restingPixels)
        assertThat(namedActions).containsExactly("click")
    }

    @Test
    fun textButton_animatedShapes_morphsCornersOnPressAndRestoresOnCancel() {
        setButtonContent {
            RemoteTextButton(
                onClick = hostAction("click".rs, 1.rf),
                shapes = RemoteTextButtonDefaults.animatedShapes(),
                colors =
                    RemoteTextButtonDefaults.textButtonColors(
                        containerColor = Color.Red.rc,
                        contentColor = Color.Red.rc,
                    ),
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
            ) {}
        }

        val restingPixels = buttonPixelCount()
        assertCornerIsBackground()

        press()
        advance(SettledMillis)

        assertCornerIsButton()
        assertThat(buttonPixelCount()).isGreaterThan(restingPixels + MinMorphedPixelGain)

        cancel()
        advance(SettledMillis)

        assertCornerIsBackground()
        assertThat(buttonPixelCount()).isEqualTo(restingPixels)
        assertThat(namedActions).isEmpty()
    }

    @Test
    fun pressMorphIsFasterThanReleaseMorph() {
        setButtonContent {
            RemoteIconButton(
                onClick = RemoteAction.None,
                shapes = RemoteIconButtonDefaults.animatedShapes(),
                colors =
                    RemoteIconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Red.rc,
                        contentColor = Color.Red.rc,
                    ),
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
            ) {}
        }

        val restingPixels = buttonPixelCount()

        // Press uses fastSpatialSpec scaled by 3^2 (stiffness = 12_600f), so by 144ms it has
        // settled at the pressed shape; advance to SettledMillis so the slower slowSpatialSpec
        // release tracker has also caught up to 1f before releasing.
        press()
        advance(144)
        val pressedPixelsAt144ms = buttonPixelCount()
        advance(SettledMillis)
        val settledPressedPixels = buttonPixelCount()
        assertThat(pressedPixelsAt144ms).isEqualTo(settledPressedPixels)

        // Release uses slowSpatialSpec (stiffness = 260f), so 144ms after release it is still
        // partway back toward the resting circle.
        release()
        advance(144)
        val releasingPixelsAt144ms = buttonPixelCount()
        assertThat(releasingPixelsAt144ms).isGreaterThan(restingPixels)
        assertThat(releasingPixelsAt144ms).isLessThan(settledPressedPixels)

        advance(SettledMillis)
        assertThat(buttonPixelCount()).isEqualTo(restingPixels)
    }

    @Test
    fun motionSchemeOverride_customizesPressAndReleaseAnimations() {
        val customFastSpatialScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun fastSpatialSpec(): RemoteAnimationSpec = remoteTween(600)
            }
        val customSlowSpatialScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun slowSpatialSpec(): RemoteAnimationSpec = remoteTween(900)
            }
        val customFastEffectsScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun fastEffectsSpec(): RemoteAnimationSpec = remoteTween(600)
            }
        val customDefaultSpatialScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun defaultSpatialSpec(): RemoteAnimationSpec = remoteTween(900)
            }

        val defaultIconBytes = captureBytes {
            RemoteIconButton(
                onClick = RemoteAction.None,
                shapes = RemoteIconButtonDefaults.animatedShapes(),
            ) {}
        }
        val standardIconBytes = captureBytes {
            RemoteMaterialTheme(motionScheme = RemoteMotionScheme.standard()) {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    shapes = RemoteIconButtonDefaults.animatedShapes(),
                ) {}
            }
        }
        val customFastIconBytes = captureBytes {
            RemoteMaterialTheme(motionScheme = customFastSpatialScheme) {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    shapes = RemoteIconButtonDefaults.animatedShapes(),
                ) {}
            }
        }
        val customSlowIconBytes = captureBytes {
            RemoteMaterialTheme(motionScheme = customSlowSpatialScheme) {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    shapes = RemoteIconButtonDefaults.animatedShapes(),
                ) {}
            }
        }
        assertThat(defaultIconBytes).isEqualTo(standardIconBytes)
        assertThat(defaultIconBytes).isNotEqualTo(customFastIconBytes)
        assertThat(defaultIconBytes).isNotEqualTo(customSlowIconBytes)

        val defaultToggleBytes = captureBytes {
            RemoteIconToggleButton(
                checked = false.rb,
                onCheckedChange = RemoteAction.None,
                shapes = RemoteIconToggleButtonDefaults.animatedShapes(),
            ) {}
        }
        val customFastToggleBytes = captureBytes {
            RemoteMaterialTheme(motionScheme = customFastEffectsScheme) {
                RemoteIconToggleButton(
                    checked = false.rb,
                    onCheckedChange = RemoteAction.None,
                    shapes = RemoteIconToggleButtonDefaults.animatedShapes(),
                ) {}
            }
        }
        val customReleaseToggleBytes = captureBytes {
            RemoteMaterialTheme(motionScheme = customDefaultSpatialScheme) {
                RemoteIconToggleButton(
                    checked = false.rb,
                    onCheckedChange = RemoteAction.None,
                    shapes = RemoteIconToggleButtonDefaults.animatedShapes(),
                ) {}
            }
        }
        assertThat(defaultToggleBytes).isNotEqualTo(customFastToggleBytes)
        assertThat(defaultToggleBytes).isNotEqualTo(customReleaseToggleBytes)

        // Verify on the player that a slow custom motion scheme slows down the press morph.
        val slowPressAndReleaseScheme =
            object : RemoteMotionScheme by RemoteMotionScheme.standard() {
                override fun fastSpatialSpec(): RemoteAnimationSpec = remoteTween(800)

                override fun slowSpatialSpec(): RemoteAnimationSpec = remoteTween(800)
            }
        setButtonContent {
            RemoteMaterialTheme(motionScheme = slowPressAndReleaseScheme) {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    shapes = RemoteIconButtonDefaults.animatedShapes(),
                    colors =
                        RemoteIconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Red.rc,
                            contentColor = Color.Red.rc,
                        ),
                    modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
                ) {}
            }
        }
        val restingPixels = buttonPixelCount()
        press()
        advance(96)
        val midPressPixels = buttonPixelCount()
        advance(1_000)
        val settledPressedPixels = buttonPixelCount()
        assertThat(midPressPixels).isGreaterThan(restingPixels)
        assertThat(midPressPixels).isLessThan(settledPressedPixels)
    }

    @Test
    fun expressiveMotionScheme_morphsOnPressAndRestoresOnRelease() {
        setButtonContent {
            RemoteMaterialTheme(motionScheme = RemoteMotionScheme.expressive()) {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    shapes = RemoteIconButtonDefaults.animatedShapes(),
                    colors =
                        RemoteIconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Red.rc,
                            contentColor = Color.Red.rc,
                        ),
                    modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
                ) {}
            }
        }

        val restingPixels = buttonPixelCount()
        assertCornerIsBackground()

        press()
        advance(SettledMillis)
        assertCornerIsButton()
        assertThat(buttonPixelCount()).isGreaterThan(restingPixels + MinMorphedPixelGain)

        release()
        advance(SettledMillis)
        assertCornerIsBackground()
        assertThat(buttonPixelCount()).isEqualTo(restingPixels)
    }

    @Test
    fun iconButton_defaultStaticShapes_doesNotMorphOnPress() {
        setButtonContent {
            RemoteIconButton(
                onClick = RemoteAction.None,
                colors =
                    RemoteIconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Red.rc,
                        contentColor = Color.Red.rc,
                    ),
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
            ) {}
        }
        val restingPixels = buttonPixelCount()
        press()
        advance(SettledMillis)
        assertThat(buttonPixelCount()).isEqualTo(restingPixels)
    }

    @Test
    fun textButton_defaultStaticShapes_doesNotMorphOnPress() {
        setButtonContent {
            RemoteTextButton(
                onClick = RemoteAction.None,
                colors =
                    RemoteTextButtonDefaults.textButtonColors(
                        containerColor = Color.Red.rc,
                        contentColor = Color.Red.rc,
                    ),
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
            ) {}
        }
        val restingPixels = buttonPixelCount()
        press()
        advance(SettledMillis)
        assertThat(buttonPixelCount()).isEqualTo(restingPixels)
    }

    @Test
    fun disabledOrNonTouchProfileOrNonRoundedCornerShape_skipsPressAnimation() {
        // When the press animation is skipped, animated shapes capture the same document as static
        // shapes with the same resting shape. An enabled button on a touch profile is the control.
        val animatedBytes = captureBytes {
            RemoteIconButton(
                onClick = RemoteAction.None,
                shapes = RemoteIconButtonDefaults.animatedShapes(),
            ) {}
        }
        val staticBytes = captureBytes {
            RemoteIconButton(
                onClick = RemoteAction.None,
                shapes = RemoteIconButtonDefaults.shapes(),
            ) {}
        }
        assertThat(animatedBytes).isNotEqualTo(staticBytes)

        val disabledAnimatedBytes = captureBytes {
            RemoteIconButton(
                onClick = RemoteAction.None,
                enabled = false.rb,
                shapes = RemoteIconButtonDefaults.animatedShapes(),
            ) {}
        }
        val disabledStaticBytes = captureBytes {
            RemoteIconButton(
                onClick = RemoteAction.None,
                enabled = false.rb,
                shapes = RemoteIconButtonDefaults.shapes(),
            ) {}
        }
        assertThat(disabledAnimatedBytes).isEqualTo(disabledStaticBytes)

        val wearWidgetsAnimatedBytes =
            captureBytes(profile = TestProfiles.wearWidgets) {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    shapes = RemoteIconButtonDefaults.animatedShapes(),
                ) {}
            }
        val wearWidgetsStaticBytes =
            captureBytes(profile = TestProfiles.wearWidgets) {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    shapes = RemoteIconButtonDefaults.shapes(),
                ) {}
            }
        assertThat(wearWidgetsAnimatedBytes).isEqualTo(wearWidgetsStaticBytes)

        val nonCornerShape =
            object : RemoteShape {
                override fun createOutline(
                    size: RemoteSize,
                    layoutDirection: RemoteLayoutDirection,
                    density: RemoteDensity,
                ): RemoteOutline = RemoteOutline.Rectangle(RemoteOffset.Zero, size)
            }
        val nonCornerAnimatedBytes = captureBytes {
            RemoteIconButton(
                onClick = RemoteAction.None,
                shapes =
                    RemoteIconButtonShapes(
                        shape = nonCornerShape,
                        pressedShape = RemoteRectangleShape,
                    ),
            ) {}
        }
        val nonCornerStaticBytes = captureBytes {
            RemoteIconButton(
                onClick = RemoteAction.None,
                shapes = RemoteIconButtonShapes(shape = nonCornerShape),
            ) {}
        }
        assertThat(nonCornerAnimatedBytes).isEqualTo(nonCornerStaticBytes)

        // A RemoteCornerBasedShape subclass that is not RemoteRoundedCornerShape must neither add
        // the press animation nor coerce its outline into rounded corners.
        val customCornerShape = CustomCornerBasedShape(RemoteCornerSize(26.rdp))
        val customCornerAnimatedBytes = captureBytes {
            RemoteIconButton(
                onClick = RemoteAction.None,
                shapes =
                    RemoteIconButtonDefaults.animatedShapes(
                        shape = customCornerShape,
                        pressedShape = RemoteShapeDefaults.Small,
                    ),
            ) {}
        }
        val customCornerStaticBytes = captureBytes {
            RemoteIconButton(
                onClick = RemoteAction.None,
                shapes = RemoteIconButtonDefaults.shapes(customCornerShape),
            ) {}
        }
        assertThat(customCornerAnimatedBytes).isEqualTo(customCornerStaticBytes)

        setButtonContent {
            RemoteIconButton(
                onClick = RemoteAction.None,
                shapes =
                    RemoteIconButtonDefaults.animatedShapes(
                        shape = customCornerShape,
                        pressedShape = RemoteShapeDefaults.Small,
                    ),
                colors =
                    RemoteIconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Red.rc,
                        contentColor = Color.Red.rc,
                    ),
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
            ) {}
        }
        assertThat(isButtonPixel(0, 0)).isTrue()
    }

    @Test
    fun toggleButton_nonRoundedCornerBasedShapeWithDynamicChecked_fallsBackToUncheckedShape() {
        // Toggle buttons with dynamic checked state must fall back to uncheckedShape rather than
        // coercing a non-rounded RemoteCornerBasedShape into RemoteRoundedCornerShape.
        val customCornerShape = CustomCornerBasedShape(RemoteCornerSize(26.rdp))
        setButtonContent {
            val checked = remember { MutableRemoteBoolean(false) }
            RemoteIconToggleButton(
                checked = checked,
                onCheckedChange = valueChangeAction(checked, !checked),
                shapes =
                    RemoteIconToggleButtonDefaults.variantShapes(
                        uncheckedShape = customCornerShape,
                        checkedShape = RemoteShapeDefaults.Medium,
                    ),
                colors =
                    RemoteIconToggleButtonDefaults.colors(
                        checkedContainerColor = Color.Red.rc,
                        checkedContentColor = Color.Red.rc,
                        uncheckedContainerColor = Color.Red.rc,
                        uncheckedContentColor = Color.Red.rc,
                    ),
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
            ) {}
        }
        assertThat(isButtonPixel(0, 0)).isTrue()
    }

    @Test
    fun iconToggleButton_variantAnimatedShapes_morphsCornersThroughRemoteRoundButton() {
        setButtonContent {
            val checked = remember { MutableRemoteBoolean(true) }
            RemoteIconToggleButton(
                checked = checked,
                onCheckedChange = valueChangeAction(checked, !checked),
                shapes = RemoteIconToggleButtonDefaults.variantAnimatedShapes(),
                colors =
                    RemoteIconToggleButtonDefaults.colors(
                        checkedContainerColor = Color.Red.rc,
                        checkedContentColor = Color.Red.rc,
                        uncheckedContainerColor = Color.Red.rc,
                        uncheckedContentColor = Color.Red.rc,
                    ),
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
            ) {}
        }
        val checkedRestingPixels = buttonPixelCount()
        assertCornerIsBackground()

        press()
        advance(SettledMillis)
        assertCornerIsButton()
        assertThat(buttonPixelCount()).isGreaterThan(checkedRestingPixels)

        cancel()
        advance(SettledMillis)
        assertCornerIsBackground()
    }

    @Test
    fun textToggleButton_variantStaticShapesWithDynamicChecked_doesNotMorphOnPress() {
        setButtonContent {
            val checked = remember { MutableRemoteBoolean(false) }
            RemoteTextToggleButton(
                checked = checked,
                onCheckedChange = valueChangeAction(checked, !checked),
                shapes = RemoteTextToggleButtonDefaults.variantShapes(),
                colors =
                    RemoteTextToggleButtonDefaults.colors(
                        checkedContainerColor = Color.Red.rc,
                        checkedContentColor = Color.Red.rc,
                        uncheckedContainerColor = Color.Red.rc,
                        uncheckedContentColor = Color.Red.rc,
                    ),
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
            ) {}
        }
        assertCornerIsBackground()
        press()
        advance(SettledMillis)
        // The pressed indication dims the container, but the corners must not morph.
        assertCornerIsBackground()
    }

    @Test
    fun textToggleButton_animatedShapes_morphsCornersOnPress() {
        setButtonContent {
            val checked = remember { MutableRemoteBoolean(false) }
            RemoteTextToggleButton(
                checked = checked,
                onCheckedChange = valueChangeAction(checked, !checked),
                shapes = RemoteTextToggleButtonDefaults.animatedShapes(),
                colors =
                    RemoteTextToggleButtonDefaults.colors(
                        checkedContainerColor = Color.Red.rc,
                        checkedContentColor = Color.Red.rc,
                        uncheckedContainerColor = Color.Red.rc,
                        uncheckedContentColor = Color.Red.rc,
                    ),
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs },
            ) {}
        }
        assertCornerIsBackground()
        press()
        advance(SettledMillis)
        assertCornerIsButton()
    }

    @Test
    fun iconButtonWithCornerAnimationSample_morphsCornersOnPress() {
        setButtonContent {
            RemoteIconButtonWithCornerAnimationSample(
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs }
            )
        }
        assertCornerIsBackground()
        press()
        advance(SettledMillis)
        assertCornerIsNotBackground()
    }

    @Test
    fun textButtonWithCornerAnimationSample_morphsCornersOnPress() {
        setButtonContent {
            RemoteTextButtonWithCornerAnimationSample(
                modifier = RemoteModifier.semantics { contentDescription = ButtonLabel.rs }
            )
        }
        assertCornerIsBackground()
        press()
        advance(SettledMillis)
        assertCornerIsNotBackground()
    }

    private fun captureBytes(
        profile: Profile = Profile(),
        content: @Composable @RemoteComposable () -> Unit,
    ): ByteArray = runBlocking {
        captureRule.captureRecordedDocument(
            context = context,
            creationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width = ButtonSizePx,
                    height = ButtonSizePx,
                    densityDpi = 160,
                ),
            profile = profile,
            content = { RemoteMaterialTheme { content() } },
        )
    }
        .bytes

    private fun setButtonContent(content: @Composable @RemoteComposable () -> Unit) {
        namedActions.clear()
        playerRule.mainClock.autoAdvance = false
        playerRule.setRemoteContent(
            onNamedAction = { name, _, _ -> namedActions += name },
            remoteCreationDisplayInfo =
                RemoteCreationDisplayInfo(
                    width = ButtonSizePx,
                    height = ButtonSizePx,
                    densityDpi = 160,
                ),
            playComposableWrapper = { remoteContent ->
                Box(modifier = Modifier.size(ButtonSizePx.dp).testTag(PlayerTag)) {
                    remoteContent()
                }
            },
        ) {
            RemoteMaterialTheme {
                RemoteBox(
                    modifier = RemoteModifier.fillMaxSize().background(Color.Black.rc),
                    contentAlignment = RemoteAlignment.Center,
                ) {
                    content()
                }
            }
        }
        advance(100)
    }

    private fun advance(ms: Long) {
        var remainingMs = ms
        while (remainingMs > 0) {
            val stepMs = minOf(FrameMs, remainingMs)
            playerRule.mainClock.advanceTimeBy(stepMs)
            playerRule.waitForIdle()
            remainingMs -= stepMs
        }
    }

    private fun press() {
        playerRule.onNodeWithContentDescription(ButtonLabel).performTouchInput { down(center) }
    }

    private fun release() {
        playerRule.onNodeWithContentDescription(ButtonLabel).performTouchInput { up() }
    }

    private fun cancel() {
        playerRule.onNodeWithContentDescription(ButtonLabel).performTouchInput { cancel() }
    }

    private fun captureBitmap(): Bitmap =
        playerRule.onNodeWithTag(PlayerTag).captureToImage().asAndroidBitmap()

    /**
     * Checks whether `(x, y)` lies inside the red button container rather than the black `(0, 0,
     * 0)` background. While pressed, `RcPlayer`'s clickable indication dims/blends the red
     * container (e.g. `rgb=(178, 0, 0)`), whereas clipped background pixels remain `(0, 0, 0)`.
     */
    private fun isButtonPixel(x: Int, y: Int, bitmap: Bitmap = captureBitmap()): Boolean {
        val pixel = bitmap.getPixel(x, y)
        return AndroidColor.red(pixel) > 120 &&
            AndroidColor.red(pixel) > AndroidColor.green(pixel) + 60
    }

    private fun isBackgroundPixel(x: Int, y: Int, bitmap: Bitmap = captureBitmap()): Boolean {
        val pixel = bitmap.getPixel(x, y)
        return AndroidColor.red(pixel) < 30 &&
            AndroidColor.green(pixel) < 30 &&
            AndroidColor.blue(pixel) < 30
    }

    private fun assertCornerIsBackground() {
        val bitmap = captureBitmap()
        val pixel = bitmap.getPixel(CornerProbeX, CornerProbeY)
        assertWithMessage(
                "Expected background at ($CornerProbeX, $CornerProbeY), got rgb=(${AndroidColor.red(pixel)}, ${AndroidColor.green(pixel)}, ${AndroidColor.blue(pixel)})"
            )
            .that(isBackgroundPixel(CornerProbeX, CornerProbeY, bitmap))
            .isTrue()
    }

    private fun assertCornerIsButton() {
        val bitmap = captureBitmap()
        val pixel = bitmap.getPixel(CornerProbeX, CornerProbeY)
        assertWithMessage(
                "Expected button at ($CornerProbeX, $CornerProbeY), got rgb=(${AndroidColor.red(pixel)}, ${AndroidColor.green(pixel)}, ${AndroidColor.blue(pixel)})"
            )
            .that(isButtonPixel(CornerProbeX, CornerProbeY, bitmap))
            .isTrue()
    }

    /** Like [assertCornerIsButton], for buttons whose container color is not red. */
    private fun assertCornerIsNotBackground() {
        val bitmap = captureBitmap()
        val pixel = bitmap.getPixel(CornerProbeX, CornerProbeY)
        assertWithMessage(
                "Expected non-background at ($CornerProbeX, $CornerProbeY), got rgb=(${AndroidColor.red(pixel)}, ${AndroidColor.green(pixel)}, ${AndroidColor.blue(pixel)})"
            )
            .that(isBackgroundPixel(CornerProbeX, CornerProbeY, bitmap))
            .isFalse()
    }

    private fun buttonPixelCount(): Int {
        val bitmap = captureBitmap()
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (isButtonPixel(x, y, bitmap)) count++
            }
        }
        return count
    }

    private class CustomCornerBasedShape(
        topStart: RemoteCornerSize,
        topEnd: RemoteCornerSize = topStart,
        bottomEnd: RemoteCornerSize = topStart,
        bottomStart: RemoteCornerSize = topStart,
    ) : RemoteCornerBasedShape(topStart, topEnd, bottomEnd, bottomStart) {
        override fun createOutline(
            topStart: RemoteFloat,
            topEnd: RemoteFloat,
            bottomEnd: RemoteFloat,
            bottomStart: RemoteFloat,
            size: RemoteSize?,
            offset: RemoteOffset,
        ): RemoteOutline = RemoteOutline.Rectangle(offset, size ?: RemoteSize.Zero)

        override fun copy(
            topStart: RemoteCornerSize,
            topEnd: RemoteCornerSize,
            bottomEnd: RemoteCornerSize,
            bottomStart: RemoteCornerSize,
        ): RemoteCornerBasedShape = CustomCornerBasedShape(topStart, topEnd, bottomEnd, bottomStart)
    }

    private companion object {
        const val ButtonLabel = "roundButton"
        const val PlayerTag = "playerTag"
        const val ButtonSizePx = 52
        // Pixel (4, 4) in a 52x52px button: outside Circle (26px) and Medium (18px), inside Small
        // (8px) and Medium * 0.66 (11.88px).
        const val CornerProbeX = 4
        const val CornerProbeY = 4
        const val MinMorphedPixelGain = 300
        const val FrameMs = 16L
        const val SettledMillis = 1_500L
    }
}
