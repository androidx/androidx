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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTvMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class CardLayoutScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    private val boxSizeModifier = Modifier.size(220.dp, 180.dp)
    private val standardCardLayoutSizeModifier = Modifier.size(150.dp, 120.dp)
    private val wideCardLayoutSizeModifier = Modifier.size(180.dp, 100.dp)

    @Test
    fun standardCardLayout_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardLayoutWrapperTag),
                    contentAlignment = Alignment.Center
                ) {
                    StandardCardLayout(
                        modifier = standardCardLayoutSizeModifier,
                        imageCard = { interactionSource ->
                            CardLayoutDefaults.ImageCard(
                                onClick = { },
                                interactionSource = interactionSource
                            ) {
                                SampleImage(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                )
                            }
                        },
                        title = { Text("Standard Card") }
                    )
                }
            }
        }

        assertAgainstGolden("standardCardLayout_lightTheme")
    }

    @Test
    fun standardCardLayout_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardLayoutWrapperTag),
                    contentAlignment = Alignment.Center
                ) {
                    StandardCardLayout(
                        modifier = standardCardLayoutSizeModifier,
                        imageCard = { interactionSource ->
                            CardLayoutDefaults.ImageCard(
                                onClick = { },
                                interactionSource = interactionSource
                            ) {
                                SampleImage(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                )
                            }
                        },
                        title = { Text("Standard Card") }
                    )
                }
            }
        }

        assertAgainstGolden("standardCardLayout_darkTheme")
    }

    @Test
    fun standardCardLayout_focused() {
        rule.setContent {
            Box(
                modifier = boxSizeModifier
                    .testTag(CardLayoutWrapperTag)
                    .semantics(mergeDescendants = true) {},
                contentAlignment = Alignment.Center
            ) {
                StandardCardLayout(
                    modifier = standardCardLayoutSizeModifier,
                    imageCard = { interactionSource ->
                        CardLayoutDefaults.ImageCard(
                            onClick = { },
                            interactionSource = interactionSource
                        ) {
                            SampleImage(
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                            )
                        }
                    },
                    title = { Text("Standard Card", Modifier.padding(top = 5.dp)) }
                )
            }
        }

        rule.onNodeWithTag(CardLayoutWrapperTag)
            .onChild()
            .performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()

        assertAgainstGolden("standardCardLayout_focused")
    }

    @Test
    fun wideCardLayout_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardLayoutWrapperTag),
                    contentAlignment = Alignment.Center
                ) {
                    WideCardLayout(
                        modifier = wideCardLayoutSizeModifier,
                        imageCard = { interactionSource ->
                            CardLayoutDefaults.ImageCard(
                                onClick = { },
                                interactionSource = interactionSource
                            ) {
                                SampleImage(
                                    Modifier
                                        .fillMaxHeight()
                                        .width(90.dp)
                                )
                            }
                        },
                        title = { Text("Wide Card", Modifier.padding(start = 8.dp)) },
                    )
                }
            }
        }

        assertAgainstGolden("wideCardLayout_lightTheme")
    }

    @Test
    fun wideCardLayout_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardLayoutWrapperTag),
                    contentAlignment = Alignment.Center
                ) {
                    WideCardLayout(
                        modifier = wideCardLayoutSizeModifier,
                        imageCard = { interactionSource ->
                            CardLayoutDefaults.ImageCard(
                                onClick = { },
                                interactionSource = interactionSource
                            ) {
                                SampleImage(
                                    Modifier
                                        .fillMaxHeight()
                                        .width(90.dp)
                                )
                            }
                        },
                        title = { Text("Wide Card", Modifier.padding(start = 8.dp)) },
                    )
                }
            }
        }

        assertAgainstGolden("wideCardLayout_darkTheme")
    }

    @Test
    fun wideCardLayout_focused() {
        rule.setContent {
            Box(
                modifier = boxSizeModifier
                    .testTag(CardLayoutWrapperTag)
                    .semantics(mergeDescendants = true) {},
                contentAlignment = Alignment.Center
            ) {
                WideCardLayout(
                    modifier = wideCardLayoutSizeModifier,
                    imageCard = { interactionSource ->
                        CardLayoutDefaults.ImageCard(
                            onClick = { },
                            interactionSource = interactionSource
                        ) {
                            SampleImage(
                                Modifier
                                    .fillMaxHeight()
                                    .width(90.dp)
                            )
                        }
                    },
                    title = { Text("Wide Card", Modifier.padding(start = 8.dp)) },
                )
            }
        }

        rule.onNodeWithTag(CardLayoutWrapperTag)
            .onChild()
            .performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()

        assertAgainstGolden("wideCardLayout_focused")
    }

    @Composable
    fun SampleImage(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .background(Color.Blue)
        )
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(CardLayoutWrapperTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}

private const val CardLayoutWrapperTag = "card_layout_wrapper"