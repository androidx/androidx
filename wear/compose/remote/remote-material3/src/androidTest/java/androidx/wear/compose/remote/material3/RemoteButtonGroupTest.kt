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

package androidx.wear.compose.remote.material3

import android.content.Context
import androidx.collection.buildObjectIntMap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.RemoteImageVector
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.toRemoteImageVector
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxWidth
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.RemoteScreenshotTestRule
import androidx.compose.remote.player.view.RemoteComposePlayer
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.wear.compose.remote.material3.previews.RemoteButtonGroupThreeButtons
import androidx.wear.compose.remote.material3.previews.RemoteButtonGroupTwoButtons
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import androidx.wear.compose.remote.material3.util.TestProfiles
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteButtonGroupTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
        )
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    @Test
    fun buttonGroup_threeButton() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteButtonGroupThreeButtons() }
        }
    }

    @Test
    fun buttonGroup_threeButton_rtl() {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = creationDisplayInfo,
            creationComposableWrapper = ComposableWrappers.rtl,
        ) {
            ComponentContainer { RemoteButtonGroupThreeButtons() }
        }
    }

    @Test
    fun buttonGroup_twoButton() {
        remoteComposeTestRule.runScreenshotTest(remoteCreationDisplayInfo = creationDisplayInfo) {
            ComponentContainer { RemoteButtonGroupTwoButtons() }
        }
    }

    @Test
    fun buttonGroup_threeButton_firstButtonPressed() {
        runPressedScreenshotTest { ThreeButtonGroup(pressedItem = 0) }
    }

    @Test
    fun buttonGroup_threeButton_middleButtonPressed() {
        runPressedScreenshotTest { ThreeButtonGroup(pressedItem = 1) }
    }

    @Test
    fun buttonGroup_twoButton_firstButtonPressed() {
        runPressedScreenshotTest { TwoButtonGroup(pressedItem = 0) }
    }

    @Test
    fun buttonGroup_buttons_firstButtonPressed() {
        runPressedScreenshotTest {
            RemoteButtonGroupImpl(
                modifier = RemoteModifier.fillMaxWidth(),
                initialPressedItem = 0,
            ) {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.weight(1f.rf).animateWidth(),
                    colors = RemoteButtonDefaults.filledTonalButtonColors(),
                ) {
                    RemoteText("Cancel".rs)
                }
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.weight(1f.rf).animateWidth(),
                ) {
                    RemoteText("OK".rs)
                }
            }
        }
    }

    @Test
    fun buttonGroup_textButtons_firstButtonPressed() {
        lateinit var player: RemoteComposePlayer
        remoteComposeTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            update = { player = it },
        ) {
            ComponentContainer {
                RemoteButtonGroup(modifier = RemoteModifier.fillMaxWidth()) {
                    for (label in listOf("A", "B", "C")) {
                        RemoteTextButton(
                            onClick = testAction,
                            modifier = RemoteModifier.weight(1f.rf).animateWidth(),
                            shapes = RemoteTextButtonDefaults.animatedShapes(),
                            colors = RemoteTextButtonDefaults.filledTonalTextButtonColors(),
                        ) {
                            RemoteText(label.rs)
                        }
                    }
                }
            }
        }
        val root =
            remoteComposeTestRule.composeTestRule.onNodeWithTag(
                RemoteScreenshotTestRule.ROOT_TEST_TAG
            )
        root.performTouchInput { down(Offset(width * 0.2f, centerY)) }
        remoteComposeTestRule.composeTestRule.waitUntil(AnimationTimeoutMillis) {
            player.document.document.needsRepaint() < 0
        }
        remoteComposeTestRule.verifyScreenshot()
        root.performTouchInput { up() }
    }

    @Test
    fun buttonGroup_toggleButtons_firstButtonPressed() {
        runPressedScreenshotTest {
            RemoteButtonGroupImpl(
                modifier = RemoteModifier.fillMaxWidth(),
                initialPressedItem = 0,
            ) {
                RemoteIconToggleButton(
                    checked = true.rb,
                    onCheckedChange = testAction,
                    modifier = RemoteModifier.weight(1f.rf).animateWidth(),
                    shapes = RemoteIconToggleButtonDefaults.variantShapes(),
                ) {
                    RemoteIcon(
                        imageVector = Icons.Filled.MailOutline.toRemoteImageVector(),
                        contentDescription = null,
                    )
                }
                RemoteIconToggleButton(
                    checked = false.rb,
                    onCheckedChange = testAction,
                    modifier = RemoteModifier.weight(1f.rf).animateWidth(),
                    shapes = RemoteIconToggleButtonDefaults.variantShapes(),
                ) {
                    RemoteIcon(
                        imageVector = Icons.Filled.Favorite.toRemoteImageVector(),
                        contentDescription = null,
                    )
                }
                RemoteIconToggleButton(
                    checked = false.rb,
                    onCheckedChange = testAction,
                    modifier = RemoteModifier.weight(1f.rf).animateWidth(),
                    shapes = RemoteIconToggleButtonDefaults.variantShapes(),
                ) {
                    RemoteIcon(
                        imageVector = Icons.Filled.Call.toRemoteImageVector(),
                        contentDescription = null,
                    )
                }
            }
        }
    }

    @Test
    fun buttonGroup_dynamicColor() {
        val colorOverrides = buildObjectIntMap {
            put("WearM3.primary", Color(0xFFB8D0A0).toArgb())
            put("WearM3.onPrimary", Color(0xFF24361A).toArgb())
            put("WearM3.surfaceContainer", Color(0xFF1C1D1A).toArgb())
            put("WearM3.onSurface", Color(0xFFE2E3DC).toArgb())
        }
        remoteComposeTestRule.runScreenshotTest(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
            update = { player ->
                colorOverrides.forEach { name, colorInt ->
                    player.setUserLocalColor(name, colorInt)
                }
            },
        ) {
            ComponentContainer { RemoteButtonGroupThreeButtons() }
        }
    }

    /**
     * Runs a screenshot test of a group that starts in a pressed state. Players animate to it from
     * the rest state, on their own clock, so the screenshot waits until the document stops
     * requesting repaints.
     */
    private fun runPressedScreenshotTest(content: @Composable @RemoteComposable () -> Unit) {
        lateinit var player: RemoteComposePlayer
        remoteComposeTestRule.setContent(
            remoteCreationDisplayInfo = creationDisplayInfo,
            update = { player = it },
        ) {
            ComponentContainer { content() }
        }
        remoteComposeTestRule.composeTestRule.waitUntil(AnimationTimeoutMillis) {
            player.document.document.needsRepaint() < 0
        }
        remoteComposeTestRule.verifyScreenshot()
    }

    /** A group of 3 animated buttons, with the button at [pressedItem] pressed. */
    @Composable
    @RemoteComposable
    private fun ThreeButtonGroup(pressedItem: Int) {
        RemoteButtonGroupImpl(
            modifier = RemoteModifier.fillMaxWidth(),
            initialPressedItem = pressedItem,
        ) {
            Button(
                Icons.Filled.MailOutline.toRemoteImageVector(),
                RemoteModifier.weight(1f.rf).animateWidth(),
            )
            Button(
                Icons.Filled.Favorite.toRemoteImageVector(),
                RemoteModifier.weight(1.5f.rf).animateWidth(),
            )
            Button(
                Icons.Filled.Call.toRemoteImageVector(),
                RemoteModifier.weight(1f.rf).animateWidth(),
            )
        }
    }

    /** A group of 2 animated buttons, with the button at [pressedItem] pressed. */
    @Composable
    @RemoteComposable
    private fun TwoButtonGroup(pressedItem: Int) {
        RemoteButtonGroupImpl(
            modifier = RemoteModifier.fillMaxWidth(),
            initialPressedItem = pressedItem,
        ) {
            Button(
                Icons.Filled.MailOutline.toRemoteImageVector(),
                RemoteModifier.weight(1f.rf).animateWidth(),
            )
            Button(
                Icons.Filled.Call.toRemoteImageVector(),
                RemoteModifier.weight(1f.rf).animateWidth(),
            )
        }
    }

    @Composable
    @RemoteComposable
    private fun Button(imageVector: RemoteImageVector, modifier: RemoteModifier) {
        RemoteIconButton(
            onClick = testAction,
            modifier = modifier,
            enabled = true.rb,
            colors =
                RemoteIconButtonDefaults.iconButtonColors()
                    .copy(
                        containerColor = RemoteMaterialTheme.colorScheme.primary,
                        contentColor = RemoteMaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor =
                            RemoteMaterialTheme.colorScheme.primary.copy(alpha = 0.12f.rf),
                        disabledContentColor =
                            RemoteMaterialTheme.colorScheme.primary.copy(0.38f.rf),
                    ),
            shapes = RemoteIconButtonDefaults.shapes(RemoteButtonDefaults.shape),
        ) {
            RemoteIcon(imageVector = imageVector, contentDescription = null)
        }
    }

    private val testAction = hostAction("testAction".rs, 1.rf)

    private companion object {
        const val AnimationTimeoutMillis = 5_000L
    }
}
