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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.capture.heightDp
import androidx.compose.remote.creation.compose.capture.widthDp
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.defaultMinSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.shapes.RemoteRectangleShape
import androidx.compose.remote.creation.compose.state.MutableRemoteBoolean
import androidx.compose.remote.creation.compose.state.rb
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.remote.player.compose.test.utils.ComposableWrapper
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.TestProfiles
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalRemotePlayerApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteToggleButtonUnitTest {

    @get:Rule val playerRule = RcPlayerTestRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(500f, 500f))

    private val playerBoxWrapper: ComposableWrapper = { player ->
        Box(
            modifier =
                Modifier.size(creationDisplayInfo.widthDp, creationDisplayInfo.heightDp)
                    .background(Color.Black)
                    .testTag(PlayerTag)
        ) {
            player()
        }
    }

    @Test
    fun defaultModifier_rendersAtDefaultButtonSize() {
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteIconToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier = RemoteModifier.semantics { contentDescription = "iconDefault".rs },
                ) {
                    RemoteText("+".rs)
                }
                RemoteTextToggleButton(
                    checked = false.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier = RemoteModifier.semantics { contentDescription = "textDefault".rs },
                ) {
                    RemoteText("A".rs)
                }
            }
        }

        assertNodeSize("iconDefault", expectedWidth = 52.dp, expectedHeight = 52.dp)
        assertNodeSize("textDefault", expectedWidth = 52.dp, expectedHeight = 52.dp)
    }

    @Test
    fun defaultModifier_expandsForContentLargerThanDefaultButtonSize() {
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteIconToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.semantics { contentDescription = "iconLargeContent".rs },
                ) {
                    RemoteBox(modifier = RemoteModifier.size(64.rdp))
                }
                RemoteTextToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.semantics { contentDescription = "textLargeContent".rs },
                ) {
                    RemoteBox(modifier = RemoteModifier.size(64.rdp))
                }
            }
        }

        assertNodeSize("iconLargeContent", expectedWidth = 64.dp, expectedHeight = 64.dp)
        assertNodeSize("textLargeContent", expectedWidth = 64.dp, expectedHeight = 64.dp)
    }

    @Test
    fun explicitWidthAndHeight_respectsCallerDimensions() {
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteIconToggleButton(
                    checked = false.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.width(48.rdp).height(32.rdp).semantics {
                            contentDescription = "iconPill".rs
                        },
                ) {
                    RemoteText("+".rs)
                }
                RemoteTextToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.width(44.rdp).height(36.rdp).semantics {
                            contentDescription = "textPill".rs
                        },
                ) {
                    RemoteText("A".rs)
                }
            }
        }

        assertNodeSize("iconPill", expectedWidth = 48.dp, expectedHeight = 32.dp)
        assertNodeSize("textPill", expectedWidth = 44.dp, expectedHeight = 36.dp)
    }

    @Test
    fun explicitSquareSizes_renderAtRecommendedDimensions() {
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteIconToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.size(RemoteIconToggleButtonDefaults.SmallSize).semantics {
                            contentDescription = "iconSmall".rs
                        },
                ) {
                    RemoteText("+".rs)
                }
                RemoteIconToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.size(RemoteIconToggleButtonDefaults.LargeSize).semantics {
                            contentDescription = "iconLarge".rs
                        },
                ) {
                    RemoteText("+".rs)
                }
                RemoteIconToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.size(RemoteIconToggleButtonDefaults.ExtraLargeSize)
                            .semantics { contentDescription = "iconExtraLarge".rs },
                ) {
                    RemoteText("+".rs)
                }
                RemoteTextToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.size(RemoteTextToggleButtonDefaults.LargeSize).semantics {
                            contentDescription = "textLarge".rs
                        },
                ) {
                    RemoteText("A".rs)
                }
                RemoteTextToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.size(RemoteTextToggleButtonDefaults.ExtraLargeSize)
                            .semantics { contentDescription = "textExtraLarge".rs },
                ) {
                    RemoteText("A".rs)
                }
            }
        }

        assertNodeSize("iconSmall", expectedWidth = 48.dp, expectedHeight = 48.dp)
        assertNodeSize("iconLarge", expectedWidth = 60.dp, expectedHeight = 60.dp)
        assertNodeSize("iconExtraLarge", expectedWidth = 72.dp, expectedHeight = 72.dp)
        assertNodeSize("textLarge", expectedWidth = 60.dp, expectedHeight = 60.dp)
        assertNodeSize("textExtraLarge", expectedWidth = 72.dp, expectedHeight = 72.dp)
    }

    @Test
    fun singleAxisWidth_appliesDefaultHeightOnly() {
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteIconToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.width(48.rdp).semantics {
                            contentDescription = "iconWidthOnly".rs
                        },
                ) {
                    RemoteText("+".rs)
                }
                RemoteTextToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.width(44.rdp).semantics {
                            contentDescription = "textWidthOnly".rs
                        },
                ) {
                    RemoteText("A".rs)
                }
            }
        }

        assertNodeSize("iconWidthOnly", expectedWidth = 48.dp, expectedHeight = 52.dp)
        assertNodeSize("textWidthOnly", expectedWidth = 44.dp, expectedHeight = 52.dp)
    }

    @Test
    fun singleAxisHeight_appliesDefaultWidthOnly() {
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteIconToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.height(32.rdp).semantics {
                            contentDescription = "iconHeightOnly".rs
                        },
                ) {
                    RemoteText("+".rs)
                }
                RemoteTextToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.height(36.rdp).semantics {
                            contentDescription = "textHeightOnly".rs
                        },
                ) {
                    RemoteText("A".rs)
                }
            }
        }

        assertNodeSize("iconHeightOnly", expectedWidth = 52.dp, expectedHeight = 32.dp)
        assertNodeSize("textHeightOnly", expectedWidth = 52.dp, expectedHeight = 36.dp)
    }

    @Test
    fun rangeConstraints_areNotOverriddenByDefaultMinSize() {
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteIconToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.widthIn(min = 36.rdp, max = 44.rdp)
                            .heightIn(min = 28.rdp, max = 36.rdp)
                            .semantics { contentDescription = "iconRange".rs },
                ) {
                    RemoteText("+".rs)
                }
                RemoteTextToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.widthIn(min = 32.rdp, max = 40.rdp)
                            .heightIn(min = 24.rdp, max = 32.rdp)
                            .semantics { contentDescription = "textRange".rs },
                ) {
                    RemoteText("A".rs)
                }
            }
        }

        assertNodeSize("iconRange", expectedWidth = 36.dp, expectedHeight = 28.dp)
        assertNodeSize("textRange", expectedWidth = 32.dp, expectedHeight = 24.dp)
    }

    @Test
    fun callerDefaultMinSize_isNotOverriddenByComponentDefaultMinSize() {
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteIconToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.defaultMinSize(minWidth = 24.rdp, minHeight = 20.rdp)
                            .semantics { contentDescription = "iconMinSize".rs },
                ) {}
                RemoteTextToggleButton(
                    checked = true.rb,
                    onCheckedChange = RemoteAction.None,
                    modifier =
                        RemoteModifier.defaultMinSize(minWidth = 28.rdp, minHeight = 22.rdp)
                            .semantics { contentDescription = "textMinSize".rs },
                ) {}
            }
        }

        assertNodeSize("iconMinSize", expectedWidth = 24.dp, expectedHeight = 20.dp)
        assertNodeSize("textMinSize", expectedWidth = 28.dp, expectedHeight = 22.dp)
    }

    @Test
    fun insideButtonGroupWithWeight_respectsNarrowGroupWidthAndCallerHeight() {
        // Group width = 120dp, default horizontal padding = 11dp * 2 = 22dp, 1 spacing = 4dp ->
        // 94dp shared equally across 2 weighted buttons = 47dp each (< 52dp default Size).
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteButtonGroup(modifier = RemoteModifier.width(120.rdp)) {
                    RemoteIconToggleButton(
                        checked = true.rb,
                        onCheckedChange = RemoteAction.None,
                        modifier =
                            RemoteModifier.weight(1f.rf).minWidth(36.rdp).semantics {
                                contentDescription = "groupIcon".rs
                            },
                    ) {
                        RemoteText("1".rs)
                    }
                    RemoteTextToggleButton(
                        checked = false.rb,
                        onCheckedChange = RemoteAction.None,
                        modifier =
                            RemoteModifier.weight(1f.rf).minWidth(36.rdp).height(32.rdp).semantics {
                                contentDescription = "groupTextCompact".rs
                            },
                    ) {
                        RemoteText("2".rs)
                    }
                }
            }
        }

        assertNodeSize("groupIcon", expectedWidth = 47.dp, expectedHeight = 52.dp)
        assertNodeSize("groupTextCompact", expectedWidth = 47.dp, expectedHeight = 32.dp)
    }

    @Test
    fun iconToggleButton_whenEnabled_triggersOnCheckedChangeOnClick() {
        val clickedActions = mutableListOf<String>()
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            onNamedAction = { name, _, _ -> clickedActions += name },
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteIconToggleButton(
                checked = false.rb,
                onCheckedChange = hostAction("iconToggle".rs),
                modifier = RemoteModifier.semantics { contentDescription = "iconButton".rs },
            ) {
                RemoteText("+".rs)
            }
        }

        playerRule.onNodeWithContentDescription("iconButton").assertHasClickAction().performClick()

        assertThat(clickedActions).containsExactly("iconToggle")
    }

    @Test
    fun textToggleButton_whenEnabled_triggersOnCheckedChangeOnClick() {
        val clickedActions = mutableListOf<String>()
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            onNamedAction = { name, _, _ -> clickedActions += name },
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteTextToggleButton(
                checked = true.rb,
                onCheckedChange = hostAction("textToggle".rs),
                modifier = RemoteModifier.semantics { contentDescription = "textButton".rs },
            ) {
                RemoteText("A".rs)
            }
        }

        playerRule.onNodeWithContentDescription("textButton").assertHasClickAction().performClick()

        assertThat(clickedActions).containsExactly("textToggle")
    }

    @Test
    fun toggleButtons_whenDisabled_ignoreClicks() {
        val clickedActions = mutableListOf<String>()
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            onNamedAction = { name, _, _ -> clickedActions += name },
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteIconToggleButton(
                    checked = false.rb,
                    onCheckedChange = hostAction("iconDisabled".rs),
                    enabled = false.rb,
                    modifier = RemoteModifier.semantics { contentDescription = "disabledIcon".rs },
                ) {
                    RemoteText("+".rs)
                }
                RemoteTextToggleButton(
                    checked = true.rb,
                    onCheckedChange = hostAction("textDisabled".rs),
                    enabled = false.rb,
                    modifier = RemoteModifier.semantics { contentDescription = "disabledText".rs },
                ) {
                    RemoteText("A".rs)
                }
            }
        }
        val iconNode = playerRule.onNodeWithContentDescription("disabledIcon")
        val textNode = playerRule.onNodeWithContentDescription("disabledText")

        iconNode.performTouchInput { click() }
        textNode.performTouchInput { click() }

        iconNode.assertHasNoClickAction()
        textNode.assertHasNoClickAction()
        assertThat(clickedActions).isEmpty()
    }

    @Test
    fun toggleButtons_haveCheckboxRoleSemantics() {
        val hasCheckboxRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                RemoteIconToggleButton(
                    checked = false.rb,
                    onCheckedChange = hostAction("icon".rs),
                ) {
                    RemoteText("iconLabel".rs)
                }
                RemoteTextToggleButton(
                    checked = true.rb,
                    onCheckedChange = hostAction("text".rs),
                ) {
                    RemoteText("textLabel".rs)
                }
            }
        }

        playerRule.onNodeWithText("iconLabel").assert(hasCheckboxRole)
        playerRule.onNodeWithText("textLabel").assert(hasCheckboxRole)
    }

    @Test
    fun iconToggleButton_dynamicCheckedWithValueChangeAction_togglesColorOnClick() {
        playerRule.mainClock.autoAdvance = false
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
            playComposableWrapper = playerBoxWrapper,
        ) {
            val checked = remember { MutableRemoteBoolean(false) }
            RemoteIconToggleButton(
                checked = checked,
                onCheckedChange = valueChangeAction(checked, !checked),
                colors =
                    RemoteIconToggleButtonDefaults.colors(
                        checkedContainerColor = Color.Blue.rc,
                        uncheckedContainerColor = Color.Red.rc,
                    ),
                shapes = RemoteIconToggleButtonDefaults.shapes(RemoteRectangleShape),
                modifier = RemoteModifier.semantics { contentDescription = "dynamicIcon".rs },
            ) {}
        }

        assertCenterPixelIsRed(capturePlayerBitmap())

        playerRule.onNodeWithContentDescription("dynamicIcon").performClick()
        advanceClock(ColorTransitionSettledMs)
        assertCenterPixelIsBlue(capturePlayerBitmap())

        playerRule.onNodeWithContentDescription("dynamicIcon").performClick()
        advanceClock(ColorTransitionSettledMs)
        assertCenterPixelIsRed(capturePlayerBitmap())
    }

    @Test
    fun textToggleButton_dynamicCheckedWithValueChangeAction_togglesColorOnClick() {
        playerRule.mainClock.autoAdvance = false
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
            playComposableWrapper = playerBoxWrapper,
        ) {
            val checked = remember { MutableRemoteBoolean(false) }
            RemoteTextToggleButton(
                checked = checked,
                onCheckedChange = valueChangeAction(checked, !checked),
                colors =
                    RemoteTextToggleButtonDefaults.colors(
                        checkedContainerColor = Color.Blue.rc,
                        uncheckedContainerColor = Color.Red.rc,
                    ),
                shapes = RemoteTextToggleButtonDefaults.shapes(RemoteRectangleShape),
                modifier = RemoteModifier.semantics { contentDescription = "dynamicText".rs },
            ) {}
        }

        assertCenterPixelIsRed(capturePlayerBitmap())

        playerRule.onNodeWithContentDescription("dynamicText").performClick()
        advanceClock(ColorTransitionSettledMs)
        assertCenterPixelIsBlue(capturePlayerBitmap())

        playerRule.onNodeWithContentDescription("dynamicText").performClick()
        advanceClock(ColorTransitionSettledMs)
        assertCenterPixelIsRed(capturePlayerBitmap())
    }

    @Test
    fun iconToggleButtonDefaults_iconSizeFor_returnsExpectedSizes() {
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            remoteCreationDisplayInfo = creationDisplayInfo,
        ) {
            RemoteColumn {
                val cases =
                    listOf(
                        "belowSmall" to 40.rdp,
                        "small" to RemoteIconToggleButtonDefaults.SmallSize,
                        "default" to RemoteIconToggleButtonDefaults.Size,
                        "large" to RemoteIconToggleButtonDefaults.LargeSize,
                        "extraLarge" to RemoteIconToggleButtonDefaults.ExtraLargeSize,
                        "aboveExtraLarge" to 80.rdp,
                    )
                for ((label, buttonSize) in cases) {
                    RemoteBox(
                        modifier =
                            RemoteModifier.size(
                                    RemoteIconToggleButtonDefaults.iconSizeFor(buttonSize)
                                )
                                .semantics { contentDescription = label.rs }
                    )
                }
            }
        }

        assertNodeSize("belowSmall", expectedWidth = 24.dp, expectedHeight = 24.dp)
        assertNodeSize("small", expectedWidth = 24.dp, expectedHeight = 24.dp)
        assertNodeSize("default", expectedWidth = 26.dp, expectedHeight = 26.dp)
        assertNodeSize("large", expectedWidth = 32.dp, expectedHeight = 32.dp)
        assertNodeSize("extraLarge", expectedWidth = 36.dp, expectedHeight = 36.dp)
        assertNodeSize("aboveExtraLarge", expectedWidth = 40.dp, expectedHeight = 40.dp)
    }

    private fun assertNodeSize(
        contentDescription: String,
        expectedWidth: Dp,
        expectedHeight: Dp,
    ) {
        playerRule
            .onNodeWithContentDescription(contentDescription)
            .assertWidthIsEqualTo(expectedWidth)
            .assertHeightIsEqualTo(expectedHeight)
    }

    private fun capturePlayerBitmap(): Bitmap =
        playerRule.onNodeWithTag(PlayerTag).captureToImage().asAndroidBitmap()

    private fun advanceClock(ms: Long) {
        var remainingMs = ms
        while (remainingMs > 0) {
            val stepMs = minOf(FrameMs, remainingMs)
            playerRule.mainClock.advanceTimeBy(stepMs)
            playerRule.waitForIdle()
            remainingMs -= stepMs
        }
    }

    private fun assertCenterPixelIsRed(bitmap: Bitmap) {
        val mid = (26f * creationDisplayInfo.density).toInt()
        val pixel = bitmap.getPixel(mid, mid)
        assertWithMessage(
                "Expected red center pixel at ($mid, $mid), got (${AndroidColor.red(pixel)}, ${AndroidColor.green(pixel)}, ${AndroidColor.blue(pixel)})"
            )
            .that(isRedPixel(pixel))
            .isTrue()
    }

    private fun assertCenterPixelIsBlue(bitmap: Bitmap) {
        val mid = (26f * creationDisplayInfo.density).toInt()
        val pixel = bitmap.getPixel(mid, mid)
        assertWithMessage(
                "Expected blue center pixel at ($mid, $mid), got (${AndroidColor.red(pixel)}, ${AndroidColor.green(pixel)}, ${AndroidColor.blue(pixel)})"
            )
            .that(
                AndroidColor.blue(pixel) > 200 &&
                    AndroidColor.red(pixel) < 50 &&
                    AndroidColor.green(pixel) < 50
            )
            .isTrue()
    }

    private fun isRedPixel(pixel: Int): Boolean =
        AndroidColor.red(pixel) > 200 &&
            AndroidColor.green(pixel) < 50 &&
            AndroidColor.blue(pixel) < 50

    private companion object {
        const val PlayerTag = "playerTag"
        const val FrameMs = 16L
        const val ColorTransitionSettledMs = 300L
    }
}
