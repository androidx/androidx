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
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxHeight
import androidx.compose.remote.creation.compose.shapes.RemoteCircleShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.EnableEmbeddedPlayerRule
import java.io.ByteArrayInputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteStepperTest {
    @get:Rule val enableEmbeddedPlayer = EnableEmbeddedPlayerRule()

    @get:Rule val rule = createComposeRule()

    @Test
    fun defaultValues() {
        assertEquals(24f, RemoteStepperDefaults.IconSize.value.constantValue, 0.001f)
        assertEquals(60f, RemoteStepperDefaults.ButtonWidth.value.constantValue, 0.001f)
        assertEquals(48f, RemoteStepperDefaults.ButtonHeight.value.constantValue, 0.001f)
        assertEquals(8f, RemoteStepperDefaults.VerticalSpacing.value.constantValue, 0.001f)
        assertEquals(0.35f, RemoteStepperDefaults.ButtonWeight, 0.001f)
        assertEquals(0.3f, RemoteStepperDefaults.ContentWeight, 0.001f)
        assertEquals(RemoteCircleShape, RemoteStepperDefaults.buttonShape)
        assertEquals(RemoteCircleShape, RemoteStepperDefaults.StepperButtonShape)
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

        assertEquals(newContentColor, copy.contentColor)
        assertEquals(original.buttonContainerColor, copy.buttonContainerColor)
        assertEquals(original.buttonIconColor, copy.buttonIconColor)
        assertEquals(original.disabledContentColor, copy.disabledContentColor)
        assertEquals(original.disabledButtonContainerColor, copy.disabledButtonContainerColor)
        assertEquals(original.disabledButtonIconColor, copy.disabledButtonIconColor)
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

        assertEquals(colors1, colors2)
        assertEquals(colors1.hashCode(), colors2.hashCode())
        assertNotEquals(colors1, colors3)
    }

    // GSB is drawn over the host window and reports a top inset. It shrinks
    // Configuration.screenHeightDp (here 225dp -> 195dp) but not the screen width, so the padding
    // is based on the window width of the round screen.
    @Test
    @Config(qualifiers = "w225dp-h225dp-xhdpi")
    fun fillMaxHeight_fullScreenPlayerUnderGsb_usesWindowWidth() {
        val document =
            captureStepperDocument(
                modifier = RemoteModifier.fillMaxHeight(),
                screenHeightDp = GsbScreenHeightDp,
            )

        rule.setContent { Box(modifier = Modifier.size(225.dp)) { RcPlayer(document) } }
        rule.waitForIdle()

        // 225dp * 5.2% = 11.7dp, rather than 195dp * 5.2% = 10.14dp from screenHeightDp.
        assertButtonPadding(stepperTop = 0f, stepperHeight = 225f, expectedPadding = 225f * 0.052f)
    }

    // A host that pads its player by the GSB inset still gets padding based on the screen width,
    // matching Wear Compose Material3 Stepper.
    @Test
    @Config(qualifiers = "w225dp-h225dp-xhdpi")
    fun fillMaxHeight_playerInsetBelowGsb_usesWindowWidth() {
        val gsbInset = 225f - GsbScreenHeightDp
        val document =
            captureStepperDocument(
                modifier = RemoteModifier.fillMaxHeight(),
                screenHeightDp = GsbScreenHeightDp,
            )

        rule.setContent {
            Box(modifier = Modifier.size(225.dp)) {
                Box(modifier = Modifier.fillMaxSize().padding(top = gsbInset.dp)) {
                    RcPlayer(document)
                }
            }
        }
        rule.waitForIdle()

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
        assertEquals(expectedPadding, increaseBounds.top.value - stepperTop, 0.5f)
        assertEquals(
            expectedPadding,
            stepperTop + stepperHeight - decreaseBounds.bottom.value,
            0.5f,
        )
    }

    /**
     * Captures a [RemoteStepper] document. If [screenHeightDp] is set, capture runs with a
     * [Configuration] reporting that `screenHeightDp`, as when GSB is shown.
     */
    private fun captureStepperDocument(
        modifier: RemoteModifier = RemoteModifier,
        screenHeightDp: Int? = null,
    ): CoreDocument =
        captureDocument(screenHeightDp) {
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

    private fun captureDocument(
        screenHeightDp: Int? = null,
        content: @Composable @RemoteComposable () -> Unit,
    ): CoreDocument = runBlocking {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val context =
            if (screenHeightDp == null) {
                appContext
            } else {
                val configuration =
                    Configuration(appContext.resources.configuration).apply {
                        this.screenHeightDp = screenHeightDp
                    }
                appContext.createConfigurationContext(configuration)
            }
        assertEquals(
            screenHeightDp ?: appContext.resources.configuration.screenHeightDp,
            context.resources.configuration.screenHeightDp,
        )
        val bytes = captureSingleRemoteDocument(context = context, content = content).bytes
        CoreDocument().apply {
            ByteArrayInputStream(bytes).use {
                initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
            }
        }
    }

    private companion object {
        /** `screenHeightDp` of a 225dp round watch while GSB is shown. */
        const val GsbScreenHeightDp = 195
    }
}
