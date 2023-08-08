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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.requestFocus
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
class CardScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    private val boxSizeModifier = Modifier.size(220.dp, 180.dp)
    private val verticalCardSizeModifier = Modifier.size(150.dp, 120.dp)
    private val horizontalCardSizeModifier = Modifier.size(180.dp, 100.dp)

    @Test
    fun card_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardWrapperTag),
                    contentAlignment = Center
                ) {
                    Card(
                        modifier = verticalCardSizeModifier,
                        onClick = { }
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Text("Card", Modifier.align(Center))
                        }
                    }
                }
            }
        }

        assertAgainstGolden("card_lightTheme")
    }

    @Test
    fun card_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardWrapperTag),
                    contentAlignment = Center
                ) {
                    Card(
                        modifier = verticalCardSizeModifier,
                        onClick = { }
                    ) {
                        Box(Modifier.fillMaxSize()) {
                            Text("Card", Modifier.align(Center))
                        }
                    }
                }
            }
        }

        assertAgainstGolden("card_darkTheme")
    }

    @Test
    fun card_focused() {
        rule.setContent {
            Box(
                modifier = boxSizeModifier.testTag(CardWrapperTag),
                contentAlignment = Center
            ) {
                Card(
                    modifier = verticalCardSizeModifier,
                    onClick = { }
                ) {
                    Box(Modifier.fillMaxSize()) {
                        Text("Card", Modifier.align(Center))
                    }
                }
            }
        }

        rule.onNodeWithTag(CardWrapperTag)
            .onChild()
            .requestFocus()
        rule.waitForIdle()

        assertAgainstGolden("card_focused")
    }

    @Test
    fun classicCard_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardWrapperTag),
                    contentAlignment = Center
                ) {
                    ClassicCard(
                        modifier = verticalCardSizeModifier,
                        image = {
                            SampleImage(
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                            )
                        },
                        title = { Text("Classic Card") },
                        contentPadding = PaddingValues(8.dp),
                        onClick = { }
                    )
                }
            }
        }

        assertAgainstGolden("classicCard_lightTheme")
    }

    @Test
    fun classicCard_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardWrapperTag),
                    contentAlignment = Center
                ) {
                    ClassicCard(
                        modifier = verticalCardSizeModifier,
                        image = {
                            SampleImage(
                                Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                            )
                        },
                        title = { Text("Classic Card") },
                        contentPadding = PaddingValues(8.dp),
                        onClick = { }
                    )
                }
            }
        }

        assertAgainstGolden("classicCard_darkTheme")
    }

    @Test
    fun classicCard_focused() {
        rule.setContent {
            Box(
                modifier = boxSizeModifier.testTag(CardWrapperTag),
                contentAlignment = Center
            ) {
                ClassicCard(
                    modifier = verticalCardSizeModifier,
                    image = {
                        SampleImage(
                            Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )
                    },
                    title = { Text("Classic Card") },
                    contentPadding = PaddingValues(8.dp),
                    onClick = { }
                )
            }
        }

        rule.onNodeWithTag(CardWrapperTag)
            .onChild()
            .requestFocus()
        rule.waitForIdle()

        assertAgainstGolden("classicCard_focused")
    }

    @Test
    fun compactCard_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardWrapperTag),
                    contentAlignment = Center
                ) {
                    CompactCard(
                        modifier = verticalCardSizeModifier,
                        image = { SampleImage(Modifier.fillMaxSize()) },
                        title = { Text("Compact Card", Modifier.padding(8.dp)) },
                        onClick = { }
                    )
                }
            }
        }

        assertAgainstGolden("compactCard_lightTheme")
    }

    @Test
    fun compactCard_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardWrapperTag),
                    contentAlignment = Center
                ) {
                    CompactCard(
                        modifier = verticalCardSizeModifier,
                        image = { SampleImage(Modifier.fillMaxSize()) },
                        title = { Text("Compact Card", Modifier.padding(8.dp)) },
                        onClick = { }
                    )
                }
            }
        }

        assertAgainstGolden("compactCard_darkTheme")
    }

    @Test
    fun compactCard_focused() {
        rule.setContent {
            Box(
                modifier = boxSizeModifier.testTag(CardWrapperTag),
                contentAlignment = Center
            ) {
                CompactCard(
                    modifier = verticalCardSizeModifier,
                    image = { SampleImage(Modifier.fillMaxSize()) },
                    title = { Text("Compact Card", Modifier.padding(8.dp)) },
                    onClick = { }
                )
            }
        }

        rule.onNodeWithTag(CardWrapperTag)
            .onChild()
            .requestFocus()
        rule.waitForIdle()

        assertAgainstGolden("compactCard_focused")
    }

    @Test
    fun wideClassicCard_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardWrapperTag),
                    contentAlignment = Center
                ) {
                    WideClassicCard(
                        modifier = horizontalCardSizeModifier,
                        image = {
                            SampleImage(
                                Modifier
                                    .fillMaxHeight()
                                    .width(80.dp)
                            )
                        },
                        title = { Text("Wide Classic Card", Modifier.padding(start = 8.dp)) },
                        contentPadding = PaddingValues(8.dp),
                        onClick = { }
                    )
                }
            }
        }

        assertAgainstGolden("wideClassicCard_lightTheme")
    }

    @Test
    fun wideClassicCard_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                Box(
                    modifier = boxSizeModifier.testTag(CardWrapperTag),
                    contentAlignment = Center
                ) {
                    WideClassicCard(
                        modifier = horizontalCardSizeModifier,
                        image = {
                            SampleImage(
                                Modifier
                                    .fillMaxHeight()
                                    .width(80.dp)
                            )
                        },
                        title = { Text("Wide Classic Card", Modifier.padding(start = 8.dp)) },
                        contentPadding = PaddingValues(8.dp),
                        onClick = { }
                    )
                }
            }
        }

        assertAgainstGolden("wideClassicCard_darkTheme")
    }

    @Test
    fun wideClassicCard_focused() {
        rule.setContent {
            Box(
                modifier = boxSizeModifier.testTag(CardWrapperTag),
                contentAlignment = Center
            ) {
                WideClassicCard(
                    modifier = horizontalCardSizeModifier,
                    image = {
                        SampleImage(
                            Modifier
                                .fillMaxHeight()
                                .width(80.dp)
                        )
                    },
                    title = { Text("Wide Classic Card", Modifier.padding(start = 8.dp)) },
                    contentPadding = PaddingValues(8.dp),
                    onClick = { }
                )
            }
        }

        rule.onNodeWithTag(CardWrapperTag)
            .onChild()
            .requestFocus()
        rule.waitForIdle()

        assertAgainstGolden("wideClassicCard_focused")
    }

    @Composable
    fun SampleImage(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .background(Color.Blue)
        )
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(CardWrapperTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}

private const val CardWrapperTag = "card_wrapper"
