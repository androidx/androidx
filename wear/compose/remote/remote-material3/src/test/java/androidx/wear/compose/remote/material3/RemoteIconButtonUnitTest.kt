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
import android.util.Log
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.contentDescription
import androidx.compose.remote.creation.compose.modifier.defaultMinSize
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.heightIn
import androidx.compose.remote.creation.compose.modifier.semantics
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.modifier.widthIn
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.player.compose.ExperimentalRemotePlayerApi
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.wear.compose.remote.material3.util.TestProfiles
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

@OptIn(ExperimentalRemotePlayerApi::class)
@Config(sdk = [35])
@RunWith(RobolectricTestRunner::class)
class RemoteIconButtonUnitTest {

    @get:Rule val playerRule = RcPlayerTestRule()

    @Before
    fun setUp() {
        ShadowLog.clear()
    }

    @Test
    fun defaultModifier_rendersAtDefaultButtonSize() {
        setRemoteContentWithDisplaySize {
            RemoteColumn {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    modifier = RemoteModifier.semantics { contentDescription = "withContent".rs },
                ) {
                    RemoteText("+".rs)
                }
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    modifier = RemoteModifier.semantics { contentDescription = "emptyContent".rs },
                ) {}
            }
        }

        assertNoDuplicateModifierWarnings()
        assertNodeSize("withContent", expectedWidth = 52.dp, expectedHeight = 52.dp)
        assertNodeSize("emptyContent", expectedWidth = 52.dp, expectedHeight = 52.dp)
    }

    @Test
    fun defaultModifier_expandsForContentLargerThanDefaultButtonSize() {
        setRemoteContentWithDisplaySize {
            RemoteIconButton(
                onClick = RemoteAction.None,
                modifier = RemoteModifier.semantics { contentDescription = "largeContent".rs },
            ) {
                RemoteBox(modifier = RemoteModifier.size(64.rdp))
            }
        }

        assertNoDuplicateModifierWarnings()
        assertNodeSize("largeContent", expectedWidth = 64.dp, expectedHeight = 64.dp)
    }

    @Test
    fun customWidthAndHeight_rendersCompactPillAndHandlesClickWithoutWarnings() {
        val clickedActions = mutableListOf<String>()
        setRemoteContentWithDisplaySize(onNamedAction = { clickedActions += it }) {
            RemoteIconButton(
                onClick = hostAction("click".rs),
                modifier =
                    RemoteModifier.width(48.rdp).height(32.rdp).semantics {
                        contentDescription = "pillButton".rs
                    },
                shapes = RemoteIconButtonDefaults.shapes(RemoteRoundedCornerShape(16.rdp)),
            ) {
                RemoteText("+".rs)
            }
        }

        playerRule
            .onNodeWithContentDescription("pillButton")
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(32.dp)
            .assertHasClickAction()
            .performClick()

        assertNoDuplicateModifierWarnings()
        assertThat(clickedActions).containsExactly("click")
    }

    @Test
    fun customSquareSizes_renderAtExpectedDimensionsWithoutWarnings() {
        setRemoteContentWithDisplaySize {
            RemoteColumn {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    modifier =
                        RemoteModifier.size(RemoteIconButtonDefaults.ExtraSmallButtonSize)
                            .semantics { contentDescription = "extraSmall".rs },
                ) {
                    RemoteText("+".rs)
                }
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    modifier =
                        RemoteModifier.size(RemoteIconButtonDefaults.SmallButtonSize).semantics {
                            contentDescription = "small".rs
                        },
                ) {
                    RemoteText("+".rs)
                }
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    modifier =
                        RemoteModifier.size(RemoteIconButtonDefaults.LargeButtonSize).semantics {
                            contentDescription = "large".rs
                        },
                ) {
                    RemoteText("+".rs)
                }
            }
        }

        assertNoDuplicateModifierWarnings()
        assertNodeSize("extraSmall", expectedWidth = 32.dp, expectedHeight = 32.dp)
        assertNodeSize("small", expectedWidth = 48.dp, expectedHeight = 48.dp)
        assertNodeSize("large", expectedWidth = 60.dp, expectedHeight = 60.dp)
    }

    @Test
    fun singleAxisOverride_preservesDefaultButtonSizeOnUnconstrainedAxis() {
        setRemoteContentWithDisplaySize {
            RemoteColumn {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    modifier =
                        RemoteModifier.width(48.rdp).semantics {
                            contentDescription = "widthOnly".rs
                        },
                ) {
                    RemoteText("+".rs)
                }
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    modifier =
                        RemoteModifier.height(32.rdp).semantics {
                            contentDescription = "heightOnly".rs
                        },
                ) {
                    RemoteText("+".rs)
                }
            }
        }

        assertNoDuplicateModifierWarnings()
        assertNodeSize("widthOnly", expectedWidth = 48.dp, expectedHeight = 52.dp)
        assertNodeSize("heightOnly", expectedWidth = 52.dp, expectedHeight = 32.dp)
    }

    @Test
    fun widthInAndHeightInOverride_notOverwrittenByDefaultButtonSize() {
        setRemoteContentWithDisplaySize {
            RemoteColumn {
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    modifier =
                        RemoteModifier.widthIn(min = 36.rdp, max = 44.rdp)
                            .heightIn(min = 28.rdp, max = 36.rdp)
                            .semantics { contentDescription = "constrainedRange".rs },
                ) {
                    RemoteText("+".rs)
                }
                RemoteIconButton(
                    onClick = RemoteAction.None,
                    modifier =
                        RemoteModifier.defaultMinSize(minWidth = 24.rdp, minHeight = 20.rdp)
                            .semantics { contentDescription = "customMinSize".rs },
                ) {}
            }
        }

        assertNoDuplicateModifierWarnings()
        assertNodeSize("constrainedRange", expectedWidth = 36.dp, expectedHeight = 28.dp)
        assertNodeSize("customMinSize", expectedWidth = 24.dp, expectedHeight = 20.dp)
    }

    @Test
    fun insideButtonGroupWithWeight_emitsNoDuplicateWidthWarning() {
        // Group width = 180dp, default horizontal padding = 11dp * 2 = 22dp, 1 spacing = 4dp ->
        // 154dp shared equally across 2 weighted buttons = 77dp each.
        setRemoteContentWithDisplaySize {
            RemoteColumn {
                RemoteButtonGroup(modifier = RemoteModifier.width(180.rdp)) {
                    RemoteIconButton(
                        onClick = RemoteAction.None,
                        modifier =
                            RemoteModifier.weight(1f.rf).semantics {
                                contentDescription = "groupDefaultHeight".rs
                            },
                    ) {
                        RemoteText("1".rs)
                    }
                    RemoteIconButton(
                        onClick = RemoteAction.None,
                        modifier =
                            RemoteModifier.weight(1f.rf).height(32.rdp).semantics {
                                contentDescription = "groupCompactHeight".rs
                            },
                    ) {
                        RemoteText("2".rs)
                    }
                }
            }
        }

        assertNoDuplicateModifierWarnings()
        assertNodeSize("groupDefaultHeight", expectedWidth = 77.dp, expectedHeight = 52.dp)
        assertNodeSize("groupCompactHeight", expectedWidth = 77.dp, expectedHeight = 32.dp)
    }

    private fun setRemoteContentWithDisplaySize(
        onNamedAction: (String) -> Unit = {},
        content: @Composable @RemoteComposable () -> Unit,
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val displayInfo = createCreationDisplayInfo(context, Size(500f, 500f))
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgets,
            onNamedAction = { name, _, _ -> onNamedAction(name) },
            remoteCreationDisplayInfo = displayInfo,
            content = content,
        )
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

    private fun assertNoDuplicateModifierWarnings() {
        val warnings =
            ShadowLog.getLogsForTag("RemoteCompose").filter {
                it.type >= Log.WARN && it.msg.contains("Ignoring duplicate")
            }
        assertThat(warnings).isEmpty()
    }
}
