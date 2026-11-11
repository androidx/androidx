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

import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.capture.RemoteCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteFontStyle
import androidx.compose.remote.creation.compose.text.RemoteFontVariation
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.remote.creation.compose.text.RemoteTextOverflow
import androidx.compose.remote.player.compose.RcPlayerTestRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.wear.compose.remote.material3.util.TestProfiles
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w200dp-h200dp-xxhdpi")
class RemoteTextUnitTest {
    @get:Rule val playerRule = RcPlayerTestRule()

    private val displayInfo = RemoteCreationDisplayInfo(width = 600, height = 600, densityDpi = 480)

    @Test
    fun remoteText_withExplicitRemoteStylingParameters_capturesAndPlaysBack() {
        val label = "Styled RemoteText"
        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = displayInfo,
        ) {
            RemoteMaterialTheme {
                RemoteText(
                    text = label.rs,
                    fontSize = 16.rsp,
                    fontStyle = RemoteFontStyle.Italic,
                    fontWeight = RemoteFontWeight.Bold,
                    textAlign = RemoteTextAlign.Center,
                    overflow = RemoteTextOverflow.Ellipsis,
                    fontVariationSettings =
                        RemoteFontVariation.Settings(
                            RemoteFontVariation.weight(700.rf),
                            RemoteFontVariation.width(110f.rf),
                            RemoteFontVariation.slant((-10f).rf),
                            RemoteFontVariation.opticalSizing(16f.rf),
                            RemoteFontVariation.grade(150.rf),
                        ),
                )
            }
        }

        playerRule.onNodeWithText(label).assertExists()

        val layoutInput = fetchTextLayoutInput(label)
        assertThat(layoutInput.style.fontSize).isEqualTo(16.sp)
        assertThat(layoutInput.style.fontStyle).isEqualTo(FontStyle.Italic)
        assertThat(layoutInput.style.fontWeight).isEqualTo(FontWeight.Bold)
        assertThat(layoutInput.style.textAlign).isEqualTo(TextAlign.Center)
        assertThat(layoutInput.overflow).isEqualTo(TextOverflow.Ellipsis)

        val font =
            (layoutInput.style.fontFamily as FontListFontFamily).fonts.single() as AndroidFont
        val axes =
            font.variationSettings.settings.associate {
                it.axisName to it.toVariationValue(Density(1f))
            }
        assertThat(axes).containsEntry("wght", 700f)
        assertThat(axes).containsEntry("wdth", 110f)
        assertThat(axes).containsEntry("slnt", -10f)
        assertThat(axes).containsEntry("opsz", 16f)
        assertThat(axes).containsEntry("GRAD", 150f)
    }

    @Test
    fun remoteButton_ambientTextConfiguration_propagatesToLabelAndSecondaryLabel() {
        var labelConfigWithoutSecondary: RemoteTextConfiguration? = null
        var labelConfigWithSecondary: RemoteTextConfiguration? = null
        var secondaryConfig: RemoteTextConfiguration? = null

        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = displayInfo,
        ) {
            RemoteMaterialTheme {
                RemoteColumn {
                    // Button without secondary label or icon: centered, maxLines = 3
                    RemoteButton(
                        onClick = RemoteAction.None,
                        label = {
                            labelConfigWithoutSecondary = LocalRemoteTextConfiguration.current
                            RemoteText("Primary Only".rs)
                        },
                    )

                    // Button with secondary label: start aligned, maxLines = 3 for label and 2 for
                    // secondary
                    RemoteButton(
                        onClick = RemoteAction.None,
                        label = {
                            labelConfigWithSecondary = LocalRemoteTextConfiguration.current
                            RemoteText("Primary With Secondary".rs)
                        },
                        secondaryLabel = {
                            secondaryConfig = LocalRemoteTextConfiguration.current
                            RemoteText("Secondary".rs)
                        },
                    )
                }
            }
        }

        // Verify ambient RemoteTextConfiguration values
        assertThat(labelConfigWithoutSecondary?.textAlign).isEqualTo(RemoteTextAlign.Center)
        assertThat(labelConfigWithoutSecondary?.overflow).isEqualTo(RemoteTextOverflow.Ellipsis)
        assertThat(labelConfigWithoutSecondary?.maxLines).isEqualTo(3)

        assertThat(labelConfigWithSecondary?.textAlign).isEqualTo(RemoteTextAlign.Start)
        assertThat(labelConfigWithSecondary?.overflow).isEqualTo(RemoteTextOverflow.Ellipsis)
        assertThat(labelConfigWithSecondary?.maxLines).isEqualTo(3)

        assertThat(secondaryConfig?.textAlign).isEqualTo(RemoteTextAlign.Start)
        assertThat(secondaryConfig?.overflow).isEqualTo(RemoteTextOverflow.Ellipsis)
        assertThat(secondaryConfig?.maxLines).isEqualTo(2)

        val primaryOnlyInput = fetchTextLayoutInput("Primary Only")
        assertThat(primaryOnlyInput.style.textAlign).isEqualTo(TextAlign.Center)
        assertThat(primaryOnlyInput.overflow).isEqualTo(TextOverflow.Ellipsis)
        assertThat(primaryOnlyInput.maxLines).isEqualTo(3)

        val primaryWithSecondaryInput = fetchTextLayoutInput("Primary With Secondary")
        assertThat(primaryWithSecondaryInput.style.textAlign).isEqualTo(TextAlign.Start)
        assertThat(primaryWithSecondaryInput.overflow).isEqualTo(TextOverflow.Ellipsis)
        assertThat(primaryWithSecondaryInput.maxLines).isEqualTo(3)

        val secondaryInput = fetchTextLayoutInput("Secondary")
        assertThat(secondaryInput.style.textAlign).isEqualTo(TextAlign.Start)
        assertThat(secondaryInput.overflow).isEqualTo(TextOverflow.Ellipsis)
        assertThat(secondaryInput.maxLines).isEqualTo(2)
    }

    @Test
    fun remoteCompactButton_ambientTextConfiguration_propagatesMaxLinesAndAlignment() {
        var compactConfigWithoutIcon: RemoteTextConfiguration? = null
        var compactConfigWithIcon: RemoteTextConfiguration? = null

        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = displayInfo,
        ) {
            RemoteMaterialTheme {
                RemoteColumn {
                    // CompactButton with label only: centered, maxLines = 1
                    RemoteCompactButton(
                        onClick = RemoteAction.None,
                        label = {
                            compactConfigWithoutIcon = LocalRemoteTextConfiguration.current
                            RemoteText("Compact No Icon".rs)
                        },
                    )

                    // CompactButton with icon and label: start aligned, maxLines = 1
                    RemoteCompactButton(
                        onClick = RemoteAction.None,
                        icon = { RemoteBox(RemoteModifier.fillMaxSize()) },
                        label = {
                            compactConfigWithIcon = LocalRemoteTextConfiguration.current
                            RemoteText("Compact With Icon".rs)
                        },
                    )
                }
            }
        }

        assertThat(compactConfigWithoutIcon?.textAlign).isEqualTo(RemoteTextAlign.Center)
        assertThat(compactConfigWithoutIcon?.overflow).isEqualTo(RemoteTextOverflow.Ellipsis)
        assertThat(compactConfigWithoutIcon?.maxLines).isEqualTo(1)

        assertThat(compactConfigWithIcon?.textAlign).isEqualTo(RemoteTextAlign.Start)
        assertThat(compactConfigWithIcon?.overflow).isEqualTo(RemoteTextOverflow.Ellipsis)
        assertThat(compactConfigWithIcon?.maxLines).isEqualTo(1)

        val compactNoIconInput = fetchTextLayoutInput("Compact No Icon")
        assertThat(compactNoIconInput.style.textAlign).isEqualTo(TextAlign.Center)
        assertThat(compactNoIconInput.overflow).isEqualTo(TextOverflow.Ellipsis)
        assertThat(compactNoIconInput.maxLines).isEqualTo(1)

        val compactWithIconInput = fetchTextLayoutInput("Compact With Icon")
        assertThat(compactWithIconInput.style.textAlign).isEqualTo(TextAlign.Start)
        assertThat(compactWithIconInput.overflow).isEqualTo(TextOverflow.Ellipsis)
        assertThat(compactWithIconInput.maxLines).isEqualTo(1)
    }

    @Test
    fun localRemoteTextConfiguration_explicitProvider_propagatesToRemoteText() {
        val label = "Explicit Config Text"
        val customConfig =
            RemoteTextConfiguration(
                textAlign = RemoteTextAlign.End,
                overflow = RemoteTextOverflow.Ellipsis,
                maxLines = 2,
            )

        playerRule.setRemoteContent(
            profile = TestProfiles.wearWidgetsWithCoreText,
            remoteCreationDisplayInfo = displayInfo,
        ) {
            RemoteMaterialTheme {
                CompositionLocalProvider(LocalRemoteTextConfiguration provides customConfig) {
                    RemoteText(label.rs)
                }
            }
        }

        playerRule.onNodeWithText(label).assertExists()

        val layoutInput = fetchTextLayoutInput(label)
        assertThat(layoutInput.style.textAlign).isEqualTo(TextAlign.End)
        assertThat(layoutInput.overflow).isEqualTo(TextOverflow.Ellipsis)
        assertThat(layoutInput.maxLines).isEqualTo(2)
    }

    private fun fetchTextLayoutInput(text: String): TextLayoutInput {
        val node = playerRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(results)
        return results.single().layoutInput
    }
}
