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

package androidx.wear.compose.remote.material3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteImageBitmap
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.remote.player.compose.embedded.RcPlayer
import androidx.compose.remote.player.compose.embedded.RcPlayerState
import androidx.compose.remote.testing.RemoteCaptureTestRule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.remote.material3.util.ComponentContainer
import androidx.wear.compose.remote.material3.util.TestProfiles
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteButtonTest {
    @get:Rule val playerRule = RcPlayerTestRule()

    @get:Rule val captureRule = RemoteCaptureTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo =
        RemoteCreationDisplayInfo(
            500,
            500,
            context.resources.displayMetrics.densityDpi,
            context.resources.configuration.fontScale,
        )

    @Test
    fun button_enabled_and_has_action_click_modifier_is_added(): Unit = runTest {
        val document =
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = creationDisplayInfo,
            ) {
                RemoteButton(
                    modifier = RemoteModifier.buttonSizeModifier(),
                    onClick = testAction,
                    enabled = true.rb,
                ) {
                    RemoteText("button_enabled".rs)
                }
            }
        val actualContent = document.displayHierarchy()

        assertThat(actualContent.normalizeWhiteSpace()).contains("CLICK_MODIFIER")
    }

    @Test
    fun button_disabled_click_modifier_is_not_added(): Unit = runTest {
        val document =
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = creationDisplayInfo,
            ) {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.buttonSizeModifier(),
                    enabled = false.rb,
                ) {
                    RemoteText("button_disabled".rs)
                }
            }
        val actualContent = document.displayHierarchy()

        assertThat(actualContent.normalizeWhiteSpace()).doesNotContain("CLICK_MODIFIER")
    }

    @Test
    fun button_border_width_is_scaled_with_density() {
        val displayInfo =
            RemoteCreationDisplayInfo(
                500,
                500,
                context.resources.displayMetrics.densityDpi,
                context.resources.configuration.fontScale,
            )
        val density = displayInfo.density
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = displayInfo,
            playComposableWrapper = { player ->
                Box(
                    modifier =
                        Modifier.size(displayInfo.widthDp, displayInfo.heightDp)
                            .testTag(ROOT_TEST_TAG)
                ) {
                    player()
                }
            },
        ) {
            ComponentContainer {
                RemoteButton(
                    modifier = RemoteModifier.size(100.rdp, 50.rdp),
                    onClick = testAction,
                    border = RemoteBorderStroke(width = 8.rdp, color = RemoteColor.Red),
                    colors = RemoteButtonDefaults.buttonColors(containerColor = RemoteColor.Black),
                    shape = RemoteRectangleShape,
                ) {
                    RemoteText("button".rs)
                }
            }
        }

        val bitmap = playerRule.onNodeWithTag(ROOT_TEST_TAG).captureToImage().asAndroidBitmap()

        val y = bitmap.height / 2
        var redPixelsCount = 0
        var firstRedX = -1
        var lastRedX = -1
        for (x in 0 until bitmap.width / 2) {
            val color = Color(bitmap.getPixel(x, y))
            if (color.red > 0.8f && color.green < 0.2f && color.blue < 0.2f) {
                redPixelsCount++
                if (firstRedX == -1) firstRedX = x
                lastRedX = x
            }
        }

        val expectedBorderWidthPx = (8 * density).toInt()
        assertWithMessage(
                "Expected border width of $expectedBorderWidthPx px (border=8.rdp * density=$density), " +
                    "found $redPixelsCount red pixels at y=$y in bitmap size ${bitmap.width}x${bitmap.height} " +
                    "(firstRedX=$firstRedX, lastRedX=$lastRedX)"
            )
            .that(abs(redPixelsCount - expectedBorderWidthPx))
            .isAtMost(1)
    }

    @Test
    fun button_provides_iconColor_to_icon_slot() = runTest {
        var actualIconContentColor: RemoteColor? = null
        var actualLabelContentColor: RemoteColor? = null
        val expectedIconColor = RemoteColor.Green
        val expectedContentColor = RemoteColor.Red

        captureRule.captureDocument(
            context = context,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteButton(
                onClick = testAction,
                colors =
                    RemoteButtonDefaults.buttonColors(
                        contentColor = expectedContentColor,
                        iconColor = expectedIconColor,
                    ),
                icon = { actualIconContentColor = LocalRemoteContentColor.current },
                label = { actualLabelContentColor = LocalRemoteContentColor.current },
            )
        }

        assertThat(actualIconContentColor?.constantValue).isEqualTo(expectedIconColor.constantValue)
        assertThat(actualLabelContentColor?.constantValue)
            .isEqualTo(expectedContentColor.constantValue)
    }

    @Test
    fun compactButton_provides_iconColor_to_icon_slot() = runTest {
        var actualIconContentColor: RemoteColor? = null
        var actualIconOnlyContentColor: RemoteColor? = null
        val expectedIconColor = RemoteColor.Green
        val expectedContentColor = RemoteColor.Red

        captureRule.captureDocument(
            context = context,
            creationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteCompactButton(
                onClick = testAction,
                colors =
                    RemoteButtonDefaults.buttonColors(
                        contentColor = expectedContentColor,
                        iconColor = expectedIconColor,
                    ),
                icon = { actualIconContentColor = LocalRemoteContentColor.current },
                label = { RemoteText("label".rs) },
            )
            RemoteCompactButton(
                onClick = testAction,
                colors =
                    RemoteButtonDefaults.buttonColors(
                        contentColor = expectedContentColor,
                        iconColor = expectedIconColor,
                    ),
                icon = { actualIconOnlyContentColor = LocalRemoteContentColor.current },
                label = null,
            )
        }

        assertThat(actualIconContentColor?.constantValue).isEqualTo(expectedIconColor.constantValue)
        assertThat(actualIconOnlyContentColor?.constantValue)
            .isEqualTo(expectedIconColor.constantValue)
    }

    @Test
    fun scrimBrush_defaultTheme_usesSurfaceContainerFadingToTransparent() {
        val bitmap = renderScrimButtonBitmap { background ->
            RemoteMaterialTheme {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.fillMaxSize(),
                    shape = RemoteRectangleShape,
                    containerPainter =
                        RemoteButtonDefaults.containerPainter(
                            image = background,
                            shape = RemoteRectangleShape,
                        ),
                ) {}
            }
        }

        assertScrimGradient(bitmap, ColorScheme().surfaceContainer)
    }

    @Test
    fun scrimBrushWithSize_defaultTheme_usesSurfaceContainerFadingToTransparent() {
        val widthPx = 100
        val heightPx = 100
        val bitmap =
            renderScrimButtonBitmap(widthPx = widthPx, heightPx = heightPx) { background ->
                RemoteMaterialTheme {
                    RemoteButton(
                        onClick = testAction,
                        modifier = RemoteModifier.fillMaxSize(),
                        shape = RemoteRectangleShape,
                        containerPainter =
                            RemoteButtonDefaults.containerPainter(
                                image = background,
                                scrim =
                                    RemoteButtonDefaults.scrimBrush(
                                        RemoteSize(widthPx.toFloat().rf, heightPx.toFloat().rf)
                                    ),
                                shape = RemoteRectangleShape,
                            ),
                    ) {}
                }
            }

        assertScrimGradient(bitmap, ColorScheme().surfaceContainer)
    }

    @Test
    fun scrimBrush_customTheme_updatesScrimColor() {
        val customColorScheme = RemoteColorScheme().copy(surfaceContainer = RemoteColor.Red)
        val bitmap = renderScrimButtonBitmap { background ->
            RemoteMaterialTheme(colorScheme = customColorScheme) {
                RemoteButton(
                    onClick = testAction,
                    modifier = RemoteModifier.fillMaxSize(),
                    shape = RemoteRectangleShape,
                    containerPainter =
                        RemoteButtonDefaults.containerPainter(
                            image = background,
                            shape = RemoteRectangleShape,
                        ),
                ) {}
            }
        }

        assertScrimGradient(bitmap, Color.Red)
    }

    @Test
    fun scrimBrush_namedColorOverride_updatesScrimColor() {
        val bitmap =
            renderScrimButtonBitmap(
                colorOverrides = mapOf("WearM3.surfaceContainer" to Color.Blue)
            ) { background ->
                RemoteMaterialTheme(colorScheme = RemoteColorScheme()) {
                    RemoteButton(
                        onClick = testAction,
                        modifier = RemoteModifier.fillMaxSize(),
                        shape = RemoteRectangleShape,
                        containerPainter =
                            RemoteButtonDefaults.containerPainter(
                                image = background,
                                shape = RemoteRectangleShape,
                            ),
                    ) {}
                }
            }

        assertScrimGradient(bitmap, Color.Blue)
    }

    private fun renderScrimButtonBitmap(
        widthPx: Int = 100,
        heightPx: Int = 100,
        colorOverrides: Map<String, Color> = emptyMap(),
        content: @Composable @RemoteComposable (RemoteImageBitmap) -> Unit,
    ): Bitmap {
        val displayInfo =
            RemoteCreationDisplayInfo(
                widthPx,
                heightPx,
                context.resources.displayMetrics.densityDpi,
                context.resources.configuration.fontScale,
            )
        val solidWhite =
            Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).apply {
                Canvas(this).drawColor(AndroidColor.WHITE)
            }
        val document = runBlocking {
            captureRule.captureDocument(
                context = context,
                creationDisplayInfo = displayInfo,
                profile = TestProfiles.wearWidgets,
            ) {
                val background = remember { RemoteImageBitmap(solidWhite.asImageBitmap()) }
                content(background)
            }
        }
        val playerState = RcPlayerState(document)
        colorOverrides.forEach { (name, color) -> playerState.colorState(name).value = color }
        playerRule.setContent {
            Box(
                modifier =
                    Modifier.size(displayInfo.widthDp, displayInfo.heightDp).testTag(ROOT_TEST_TAG)
            ) {
                RcPlayer(state = playerState)
            }
        }
        playerRule.waitForIdle()
        return playerRule.onNodeWithTag(ROOT_TEST_TAG).captureToImage().asAndroidBitmap()
    }

    private fun assertScrimGradient(bitmap: Bitmap, expectedStartColor: Color) {
        val topLeft = Color(bitmap.getPixel(1, 1))
        val bottomRight = Color(bitmap.getPixel(bitmap.width - 2, bitmap.height - 2))
        assertColorClose(topLeft, expectedStartColor, "top-left scrim start")
        assertColorClose(bottomRight, Color.White, "bottom-right scrim end")
    }

    private fun assertColorClose(actual: Color, expected: Color, label: String) {
        val tolerance = 0.04f
        assertWithMessage("$label red").that(actual.red).isWithin(tolerance).of(expected.red)
        assertWithMessage("$label green").that(actual.green).isWithin(tolerance).of(expected.green)
        assertWithMessage("$label blue").that(actual.blue).isWithin(tolerance).of(expected.blue)
        assertWithMessage("$label alpha").that(actual.alpha).isWithin(tolerance).of(expected.alpha)
    }

    private fun String.normalizeWhiteSpace() = this.replace(Regex("\\s+"), " ").trim()

    private val testAction = hostAction("testAction".rs, 1.rf)

    private companion object {
        const val ROOT_TEST_TAG = "ROOT_TEST_TAG"
    }
}
