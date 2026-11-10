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
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteStepperTest {
    @get:Rule val rule = RcPlayerTestRule()

    @Test
    fun defaultValues() {
        assertThat(RemoteStepperDefaults.IconSize.value.constantValue).isWithin(0.001f).of(24f)
        assertThat(RemoteStepperDefaults.ButtonWidth.value.constantValue).isWithin(0.001f).of(60f)
        assertThat(RemoteStepperDefaults.ButtonHeight.value.constantValue).isWithin(0.001f).of(48f)
        assertThat(RemoteStepperDefaults.VerticalSpacing.value.constantValue)
            .isWithin(0.001f)
            .of(8f)
        assertThat(RemoteStepperDefaults.ButtonWeight).isWithin(0.001f).of(0.35f)
        assertThat(RemoteStepperDefaults.ContentWeight).isWithin(0.001f).of(0.3f)
        assertThat(RemoteStepperDefaults.buttonShape).isEqualTo(RemoteCircleShape)
        assertThat(RemoteStepperDefaults.StepperButtonShape).isEqualTo(RemoteCircleShape)
    }

    @Test
    fun stepperColors_copy() {
        val original =
            RemoteStepperColors(
                contentColor = RemoteColor(Color.White),
                buttonContainerColor = RemoteColor(Color.Red),
                buttonIconColor = RemoteColor(Color.Blue),
                disabledContentColor = RemoteColor(Color.Gray),
                disabledButtonContainerColor = RemoteColor(Color.DarkGray),
                disabledButtonIconColor = RemoteColor(Color.LightGray),
            )

        val newContentColor = RemoteColor(Color.Yellow)
        val copy = original.copy(contentColor = newContentColor)

        assertThat(copy.contentColor).isEqualTo(newContentColor)
        assertThat(copy.buttonContainerColor).isEqualTo(original.buttonContainerColor)
        assertThat(copy.buttonIconColor).isEqualTo(original.buttonIconColor)
        assertThat(copy.disabledContentColor).isEqualTo(original.disabledContentColor)
        assertThat(copy.disabledButtonContainerColor)
            .isEqualTo(original.disabledButtonContainerColor)
        assertThat(copy.disabledButtonIconColor).isEqualTo(original.disabledButtonIconColor)
    }

    @Test
    fun stepperColors_equalsAndHashCode() {
        val colors1 =
            RemoteStepperColors(
                contentColor = RemoteColor(Color.White),
                buttonContainerColor = RemoteColor(Color.Red),
                buttonIconColor = RemoteColor(Color.Blue),
                disabledContentColor = RemoteColor(Color.Gray),
                disabledButtonContainerColor = RemoteColor(Color.DarkGray),
                disabledButtonIconColor = RemoteColor(Color.LightGray),
            )
        val colors2 = colors1.copy()
        val colors3 = colors1.copy(contentColor = RemoteColor(Color.Black))

        assertThat(colors2).isEqualTo(colors1)
        assertThat(colors2.hashCode()).isEqualTo(colors1.hashCode())
        assertThat(colors3).isNotEqualTo(colors1)
    }

    // GSB is drawn over the host window and reports a top inset. It shrinks
    // Configuration.screenHeightDp (here 225dp -> 195dp) but not the screen width, so the padding
    // is based on the window width of the round screen.
    @Test
    @Config(qualifiers = "w225dp-h225dp-xhdpi")
    fun fillMaxHeight_fullScreenPlayerUnderGsb_usesWindowWidth() {
        setStepperContent(
            modifier = RemoteModifier.fillMaxHeight(),
            screenHeightDp = GsbScreenHeightDp,
        ) { player ->
            Box(modifier = Modifier.size(225.dp)) { player() }
        }

        // 225dp * 5.2% = 11.7dp, rather than 195dp * 5.2% = 10.14dp from screenHeightDp.
        assertButtonPadding(stepperTop = 0f, stepperHeight = 225f, expectedPadding = 225f * 0.052f)
    }

    // A host that pads its player by the GSB inset still gets padding based on the screen width,
    // matching Wear Compose Material3 Stepper.
    @Test
    @Config(qualifiers = "w225dp-h225dp-xhdpi")
    fun fillMaxHeight_playerInsetBelowGsb_usesWindowWidth() {
        val gsbInset = 225f - GsbScreenHeightDp
        setStepperContent(
            modifier = RemoteModifier.fillMaxHeight(),
            screenHeightDp = GsbScreenHeightDp,
        ) { player ->
            Box(modifier = Modifier.size(225.dp)) {
                Box(modifier = Modifier.fillMaxSize().padding(top = gsbInset.dp)) { player() }
            }
        }

        assertButtonPadding(
            stepperTop = gsbInset,
            stepperHeight = 225f - gsbInset,
            expectedPadding = 225f * 0.052f,
        )
    }

    /**
     * Asserts the increase and decrease buttons are inset by [expectedPadding] from the top and
     * bottom of a [RemoteStepper] placed at [stepperTop] with [stepperHeight], in dp relative to
     * the root.
     */
    private fun assertButtonPadding(
        stepperTop: Float,
        stepperHeight: Float,
        expectedPadding: Float,
    ) {
        val increaseBounds =
            rule.onNodeWithContentDescription("Increase").getUnclippedBoundsInRoot()
        val decreaseBounds =
            rule.onNodeWithContentDescription("Decrease").getUnclippedBoundsInRoot()
        assertThat(increaseBounds.top.value - stepperTop).isWithin(0.5f).of(expectedPadding)
        assertThat(stepperTop + stepperHeight - decreaseBounds.bottom.value)
            .isWithin(0.5f)
            .of(expectedPadding)
    }

    /**
     * Captures a [RemoteStepper] document with a [Configuration] reporting [screenHeightDp], as
     * when GSB is shown, and plays it in the host layout given by [playComposableWrapper].
     */
    private fun setStepperContent(
        modifier: RemoteModifier,
        screenHeightDp: Int,
        playComposableWrapper: @Composable (player: @Composable () -> Unit) -> Unit,
    ) {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val configuration =
            Configuration(appContext.resources.configuration).apply {
                this.screenHeightDp = screenHeightDp
            }
        val context = appContext.createConfigurationContext(configuration)
        assertThat(context.resources.configuration.screenHeightDp).isEqualTo(screenHeightDp)

        rule.setRemoteContent(
            remoteCreationDisplayInfo = createCreationDisplayInfo(context),
            creationComposableWrapper = { creation ->
                CompositionLocalProvider(LocalContext provides context) { creation() }
            },
            playComposableWrapper = playComposableWrapper,
        ) {
            RemoteStepper(
                value = 5f.rf,
                modifier = modifier,
                increaseAction = hostAction("increase".rs),
                decreaseAction = hostAction("decrease".rs),
                increaseIcon = {
                    RemoteStepperDefaults.IncreaseIcon(contentDescription = "Increase".rs)
                },
                decreaseIcon = {
                    RemoteStepperDefaults.DecreaseIcon(contentDescription = "Decrease".rs)
                },
            ) {
                RemoteText("5".rs)
            }
        }
    }

    private companion object {
        /** `screenHeightDp` of a 225dp round watch while GSB is shown. */
        const val GsbScreenHeightDp = 195
    }
}
