/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@LargeTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class SplitButtonScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.Center)
    private val wrapperTestTag = "splitButtonWrapper"

    @Test
    @Ignore("b/355548641")
    fun splitButton() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                SplitButton(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(
                            modifier = Modifier.height(48.dp),
                            onClick = { /* Do Nothing */ },
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Localized description",
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text("My Button")
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.AnimatedTrailingButton(
                            modifier = Modifier.size(48.dp),
                            onClick = {},
                            expanded = false,
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                )
            }
        }

        assertAgainstGolden("splitButton_${scheme.name}")
    }

    @Test
    @Ignore("b/355548641")
    fun filledSplitButton_large() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledSplitButton(
                    onLeadingButtonClick = {},
                    onTrailingButtonClick = {},
                    expanded = false,
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Localized description",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button", fontSize = 18.sp)
                    },
                    trailingContent = {
                        Box(
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Localized description",
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }
                )
            }
        }

        assertAgainstGolden("filledSplitButton_${scheme.name}")
    }

    @Test
    @Ignore("b/355548641")
    fun filledSplitButtonExpanded() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                FilledSplitButton(
                    onLeadingButtonClick = {},
                    expanded = true,
                    onTrailingButtonClick = {},
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Localized description",
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button", fontSize = 18.sp)
                    },
                    trailingContent = {
                        Box(
                            modifier = Modifier.fillMaxHeight().aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                modifier =
                                    Modifier.size(38.dp).graphicsLayer { this.rotationZ = 180f },
                                contentDescription = "Localized description"
                            )
                        }
                    }
                )
            }
        }

        assertAgainstGolden("filledSplitButton_expanded_${scheme.name}")
    }

    @Test
    @Ignore("b/355548641")
    fun tonalSplitButton() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                TonalSplitButton(
                    onLeadingButtonClick = {},
                    onTrailingButtonClick = {},
                    expanded = false,
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Localized description",
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button")
                    },
                    trailingContent = {
                        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                )
            }
        }

        assertAgainstGolden("tonalSplitButton_${scheme.name}")
    }

    @Test
    @Ignore("b/355548641")
    fun elevatedSplitButton() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                ElevatedSplitButton(
                    onLeadingButtonClick = {},
                    onTrailingButtonClick = {},
                    expanded = false,
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Localized description",
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button")
                    },
                    trailingContent = {
                        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                )
            }
        }

        assertAgainstGolden("elevatedSplitButton_${scheme.name}")
    }

    @Test
    @Ignore("b/355548641")
    fun outlinedSplitButton() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                OutlinedSplitButton(
                    onLeadingButtonClick = {},
                    onTrailingButtonClick = {},
                    expanded = false,
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Localized description",
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("My Button")
                    },
                    trailingContent = {
                        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                )
            }
        }

        assertAgainstGolden("outlinedSplitButton_${scheme.name}")
    }

    @Test
    @Ignore("b/355548641")
    fun splitButton_iconLeadingButton() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                SplitButton(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(
                            onClick = { /* Do Nothing */ },
                        ) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Localized description",
                            )
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.AnimatedTrailingButton(
                            onClick = {},
                            expanded = false,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                )
            }
        }

        assertAgainstGolden("splitButton_iconLeadingButton_${scheme.name}")
    }

    @Test
    @Ignore("b/355548641")
    fun splitButton_textLeadingButton() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) {
                SplitButton(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(
                            onClick = { /* Do Nothing */ },
                        ) {
                            Text("My Button")
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.AnimatedTrailingButton(onClick = {}, expanded = false) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                contentDescription = "Localized description"
                            )
                        }
                    }
                )
            }
        }

        assertAgainstGolden("splitButton_textLeadingButton_${scheme.name}")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    // Provide the ColorScheme and their name parameter in a ColorSchemeWrapper.
    // This makes sure that the default method name and the initial Scuba image generated
    // name is as expected.
    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("lightTheme", lightColorScheme()),
                ColorSchemeWrapper("darkTheme", darkColorScheme()),
            )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}
