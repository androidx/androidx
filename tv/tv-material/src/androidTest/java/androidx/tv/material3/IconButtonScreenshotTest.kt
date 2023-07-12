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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChild
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
@OptIn(ExperimentalTestApi::class, ExperimentalTvMaterial3Api::class)
class IconButtonScreenshotTest {

    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(TV_GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)
    private val wrapperTestTag = "iconButtonWrapper"

    @Test
    fun iconButton_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }
        assertAgainstGolden("iconButton_lightTheme")
    }

    @Test
    fun iconButton_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = "Localized description"
                        )
                    }
                }
            }
        }
        assertAgainstGolden("iconButton_darkTheme")
    }

    @Test
    fun iconButton_lightTheme_disabled() {
        rule.setContent {
            LightMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    IconButton(onClick = { /* doSomething() */ }, enabled = false) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }
        assertAgainstGolden("iconButton_lightTheme_disabled")
    }

    @Test
    fun iconButton_darkTheme_disabled() {
        rule.setContent {
            DarkMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    IconButton(onClick = { /* doSomething() */ }, enabled = false) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }
        assertAgainstGolden("iconButton_darkTheme_disabled")
    }

    @Test
    fun iconButton_lightTheme_pressed() {
        rule.setContent {
            LightMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(hasClickAction())
            .performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        assertAgainstGolden("iconButton_lightTheme_pressed")
    }

    @Test
    fun iconButton_darkTheme_pressed() {
        rule.setContent {
            DarkMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }

        rule.mainClock.autoAdvance = false
        rule.onNode(hasClickAction())
            .performTouchInput { down(center) }

        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle() // Wait for measure
        rule.mainClock.advanceTimeBy(milliseconds = 200)

        assertAgainstGolden("iconButton_darkTheme_pressed")
    }

    @Test
    fun iconButton_lightTheme_hovered() {
        rule.setContent {
            LightMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }
        rule.onNodeWithTag(wrapperTestTag).performMouseInput {
            enter(center)
        }

        assertAgainstGolden("iconButton_lightTheme_hovered")
    }

    @Test
    fun iconButton_darkTheme_hovered() {
        rule.setContent {
            DarkMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }
        rule.onNodeWithTag(wrapperTestTag).performMouseInput {
            enter(center)
        }

        assertAgainstGolden("iconButton_darkTheme_hovered")
    }

    @Test
    fun iconButton_lightTheme_focused() {
        val focusRequester = FocusRequester()

        rule.setContent {
            LightMaterialTheme {
                Box(Modifier.size(50.dp).testTag(wrapperTestTag)) {
                    IconButton(
                        onClick = { /* doSomething() */ },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .focusRequester(focusRequester)
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }

        rule.onNodeWithTag(wrapperTestTag)
            .onChild()
            .performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()

        assertAgainstGolden("iconButton_lightTheme_focused")
    }

    @Test
    fun iconButton_darkTheme_focused() {
        val focusRequester = FocusRequester()

        rule.setContent {
            DarkMaterialTheme {
                Box(Modifier.size(50.dp).testTag(wrapperTestTag)) {
                    IconButton(
                        onClick = { /* doSomething() */ },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .focusRequester(focusRequester)
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Localized description")
                    }
                }
            }
        }

        rule.onNodeWithTag(wrapperTestTag)
            .onChild()
            .performSemanticsAction(SemanticsActions.RequestFocus)
        rule.waitForIdle()

        assertAgainstGolden("iconButton_darkTheme_focused")
    }

    @Test
    fun iconButton_largeContentClipped() {
        rule.setContent {
            LightMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    IconButton(onClick = { /* doSomething() */ }) {
                        Box(
                            Modifier
                                .size(100.dp)
                                .background(Color.Blue))
                    }
                }
            }
        }
        assertAgainstGolden("iconButton_largeContentClipped")
    }

    @Test
    fun outlinedIconButton_lightTheme() {
        rule.setContent {
            LightMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    OutlinedIconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Localized description"
                        )
                    }
                }
            }
        }
        assertAgainstGolden("outlinedIconButton_lightTheme")
    }

    @Test
    fun outlinedIconButton_darkTheme() {
        rule.setContent {
            DarkMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    OutlinedIconButton(onClick = { /* doSomething() */ }) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Localized description"
                        )
                    }
                }
            }
        }
        assertAgainstGolden("outlinedIconButton_darkTheme")
    }

    @Test
    fun outlinedIconButton_lightTheme_disabled() {
        rule.setContent {
            LightMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    OutlinedIconButton(onClick = { /* doSomething() */ }, enabled = false) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Localized description"
                        )
                    }
                }
            }
        }
        assertAgainstGolden("outlinedIconButton_lightTheme_disabled")
    }

    @Test
    fun outlinedIconButton_darkTheme_disabled() {
        rule.setContent {
            DarkMaterialTheme {
                Box(wrap.testTag(wrapperTestTag)) {
                    OutlinedIconButton(onClick = { /* doSomething() */ }, enabled = false) {
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = "Localized description"
                        )
                    }
                }
            }
        }
        assertAgainstGolden("outlinedIconButton_darkTheme_disabled")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
