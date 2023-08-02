/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTvMaterial3Api::class)
class ChipScreenshotTest(private val scheme: ColorSchemeWrapper) {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    private val leadingIcon = @Composable {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Favourite icon",
        )
    }

    private val trailingIcon = @Composable {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = "Favourite icon",
        )
    }

    private val wrapperTestTag = "chipWrapper"

    private val wrapperBoxModifier = Modifier
        .testTag(wrapperTestTag)
        .background(if (scheme.name == lightThemeName) Color.White else Color.Black)
        .padding(20.dp)

    @Test
    fun assistChip_default() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                AssistChip(onClick = { }) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("assistChip_default_${scheme.name}")
    }

    @Test
    fun assistChip_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                AssistChip(onClick = { }, enabled = false) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("assistChip_disabled_${scheme.name}")
    }

    @Test
    fun assistChip_leadingIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                AssistChip(onClick = { }, leadingIcon = leadingIcon) {
                    Text(text = "Like")
                }
            }
        }

        assertAgainstGolden("assistChip_leadingIcon_${scheme.name}")
    }

    @Test
    fun assistChip_trailingIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                AssistChip(onClick = { }, trailingIcon = trailingIcon) {
                    Text(text = "Like")
                }
            }
        }

        assertAgainstGolden("assistChip_trailingIcon_${scheme.name}")
    }

    @Test
    fun suggestionChip_default() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                SuggestionChip(onClick = { }) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("suggestionChip_default_${scheme.name}")
    }

    @Test
    fun suggestionChip_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                SuggestionChip(onClick = { }, enabled = false) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("suggestionChip_disabled_${scheme.name}")
    }

    @Test
    fun filterChip_selected_default() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                FilterChip(selected = true, onClick = { }) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("filterChip_selected_default_${scheme.name}")
    }

    @Test
    fun filterChip_unselected_default() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                FilterChip(selected = false, onClick = { }) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("filterChip_unselected_default_${scheme.name}")
    }

    @Test
    fun filterChip_selected_enabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                FilterChip(selected = true, onClick = { }, enabled = true) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("filterChip_selected_enabled_${scheme.name}")
    }

    @Test
    fun filterChip_selected_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                FilterChip(selected = true, onClick = { }, enabled = false) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("filterChip_selected_disabled_${scheme.name}")
    }

    @Test
    fun filterChip_unselected_enabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                FilterChip(selected = false, onClick = { }, enabled = true) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("filterChip_unselected_enabled_${scheme.name}")
    }

    @Test
    fun filterChip_unselected_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                FilterChip(selected = false, onClick = { }, enabled = false) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("filterChip_unselected_disabled_${scheme.name}")
    }

    @Test
    fun filterChip_selected_leadingIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                FilterChip(selected = true, onClick = { }, leadingIcon = leadingIcon) {
                    Text(text = "Like")
                }
            }
        }

        assertAgainstGolden("filterChip_selected_leadingIcon_${scheme.name}")
    }

    @Test
    fun filterChip_unselected_leadingIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                FilterChip(selected = false, onClick = { }, leadingIcon = leadingIcon) {
                    Text(text = "Like")
                }
            }
        }

        assertAgainstGolden("filterChip_unselected_leadingIcon_${scheme.name}")
    }

    @Test
    fun filterChip_selected_trailingIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                FilterChip(selected = true, onClick = { }, trailingIcon = trailingIcon) {
                    Text(text = "Like")
                }
            }
        }

        assertAgainstGolden("filterChip_selected_trailingIcon_${scheme.name}")
    }

    @Test
    fun filterChip_unselected_trailingIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                FilterChip(selected = false, onClick = { }, trailingIcon = trailingIcon) {
                    Text(text = "Like")
                }
            }
        }

        assertAgainstGolden("filterChip_unselected_trailingIcon_${scheme.name}")
    }

    @Test
    fun inputChip_selected_default() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                InputChip(selected = true, onClick = { }) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("inputChip_selected_default_${scheme.name}")
    }

    @Test
    fun inputChip_unselected_default() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                InputChip(selected = false, onClick = { }) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("inputChip_unselected_default_${scheme.name}")
    }

    @Test
    fun inputChip_selected_enabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                InputChip(selected = true, onClick = { }, enabled = true) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("inputChip_selected_enabled_${scheme.name}")
    }

    @Test
    fun inputChip_selected_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                InputChip(selected = true, onClick = { }, enabled = false) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("inputChip_selected_disabled_${scheme.name}")
    }

    @Test
    fun inputChip_unselected_enabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                InputChip(selected = false, onClick = { }, enabled = true) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("inputChip_unselected_enabled_${scheme.name}")
    }

    @Test
    fun inputChip_unselected_disabled() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                InputChip(selected = false, onClick = { }, enabled = false) {
                    Text(text = "Label")
                }
            }
        }

        assertAgainstGolden("inputChip_unselected_disabled_${scheme.name}")
    }

    @Test
    fun inputChip_selected_leadingIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                InputChip(selected = true, onClick = { }, leadingIcon = leadingIcon) {
                    Text(text = "Like")
                }
            }
        }

        assertAgainstGolden("inputChip_selected_leadingIcon_${scheme.name}")
    }

    @Test
    fun inputChip_unselected_leadingIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                InputChip(selected = false, onClick = { }, leadingIcon = leadingIcon) {
                    Text(text = "Like")
                }
            }
        }

        assertAgainstGolden("inputChip_unselected_leadingIcon_${scheme.name}")
    }

    @Test
    fun inputChip_selected_trailingIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                InputChip(selected = true, onClick = { }, trailingIcon = trailingIcon) {
                    Text(text = "Like")
                }
            }
        }

        assertAgainstGolden("inputChip_selected_trailingIcon_${scheme.name}")
    }

    @Test
    fun inputChip_unselected_trailingIcon() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrapperBoxModifier) {
                InputChip(selected = false, onClick = { }, trailingIcon = trailingIcon) {
                    Text(text = "Like")
                }
            }
        }

        assertAgainstGolden("inputChip_unselected_trailingIcon_${scheme.name}")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @OptIn(ExperimentalTvMaterial3Api::class)
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = arrayOf(
            ColorSchemeWrapper(lightThemeName, lightColorScheme()),
            ColorSchemeWrapper(darkThemeName, darkColorScheme()),
        )
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    class ColorSchemeWrapper constructor(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}

private val lightThemeName = "lightTheme"
private val darkThemeName = "darkTheme"
