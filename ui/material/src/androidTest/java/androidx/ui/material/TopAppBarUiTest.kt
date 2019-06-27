/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import androidx.test.filters.SmallTest
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.dp
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.Container
import com.google.common.truth.Truth
import androidx.compose.composer
import androidx.compose.unaryPlus
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.currentTextStyle
import androidx.ui.core.ipx
import androidx.ui.painting.TextStyle
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertDoesNotExist
import androidx.ui.test.assertIsVisible
import androidx.ui.test.createComposeRule
import androidx.ui.test.findAll
import androidx.ui.test.findByTag
import androidx.ui.test.findByText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class TopAppBarUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultHeight = 56.dp

    @Test
    fun topAppBarTest_ExpandsToScreen() {
        val dm = composeTestRule.displayMetrics
        composeTestRule
            .setMaterialContentAndTestSizes {
                TopAppBar()
            }
            .assertHeightEqualsTo(defaultHeight)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun topAppBarTest_LeadingIconPresent() {
        composeTestRule.setMaterialContent {
            TopAppBar()
        }
        findByTag("Leading icon").assertIsVisible()
    }

    @Test
    fun topAppBarTest_TitleTextLabel_noTitle() {
        composeTestRule.setMaterialContent {
            TopAppBar()
        }
        assertDoesNotExist { testTag == "Title" }
    }

    @Test
    fun topAppBarTest_TitleTextLabel_withTitle() {
        val title = "Title"
        composeTestRule.setMaterialContent {
            TopAppBar(title = title)
        }
        findByText(title).assertIsVisible()
    }

    @Test
    fun topAppBar_defaultPositioning() {
        var leadingIconInfo: LayoutCoordinates? = null
        var titleLabelInfo: LayoutCoordinates? = null
        var trailingIconInfo: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                TopAppBar(
                    leadingIcon = {
                        OnChildPositioned(onPositioned = { position ->
                            leadingIconInfo = position
                        }) {
                            AppBarLeadingIcon()
                        }
                    },
                    titleTextLabel = {
                        OnChildPositioned(onPositioned = { position ->
                            titleLabelInfo = position
                        }) {
                            TopAppBarTitleTextLabel("title")
                        }
                    },
                    trailingIcons = {
                        OnChildPositioned(onPositioned = { position ->
                            trailingIconInfo = position
                        }) {
                            TopAppBarTrailingIcons(listOf(24.dp, 24.dp))
                        }
                    }
                )
            }
        }

        withDensity(composeTestRule.density) {
            val dm = composeTestRule.displayMetrics

            // Leading icon should be in the front
            val leadingIconPositionX = leadingIconInfo?.position?.x!!.value
            val leadingIconExpectedPositionX = 0f
            Truth.assertThat(leadingIconPositionX).isEqualTo(leadingIconExpectedPositionX)

            // Title should be next
            val titleLabelPositionX = titleLabelInfo?.position?.x!!
            val titleLabelExpectedPositionX = leadingIconInfo?.size?.width!! + 32.dp.toIntPx()
            Truth.assertThat(titleLabelPositionX).isEqualTo(titleLabelExpectedPositionX)

            // Trailing icons should be in the second half of the screen
            val trailingIconPositionX = trailingIconInfo?.position?.x!!
            val totalSpaceMiddle = dm.widthPixels / 2f
            Truth.assertThat(trailingIconPositionX.value).isGreaterThan(totalSpaceMiddle)
        }
    }

    @Test
    fun topAppBar_noTitlePositioning() {
        var leadingIconInfo: LayoutCoordinates? = null
        var trailingIconInfo: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                TopAppBar(
                    leadingIcon = {
                        OnChildPositioned(onPositioned = { position ->
                            leadingIconInfo = position
                        }) {
                            AppBarLeadingIcon()
                        }
                    },
                    titleTextLabel = {},
                    trailingIcons = {
                        OnChildPositioned(onPositioned = { position ->
                            trailingIconInfo = position
                        }) {
                            TopAppBarTrailingIcons(listOf(24.dp, 24.dp))
                        }
                    }
                )
            }
        }

        withDensity(composeTestRule.density) {
            val dm = composeTestRule.displayMetrics

            // Leading icon should be in the front
            val leadingIconPositionX = leadingIconInfo?.position?.x!!.value
            val leadingIconExpectedPositionX = 0f
            Truth.assertThat(leadingIconPositionX).isEqualTo(leadingIconExpectedPositionX)

            // Trailing icons should be in the second half of the screen
            val trailingIconPositionX = trailingIconInfo?.position?.x!!
            val totalSpaceMiddle = dm.widthPixels / 2f
            Truth.assertThat(trailingIconPositionX.value).isGreaterThan(totalSpaceMiddle)
        }
    }

    @Test
    fun topAppBar_titleDefaultStyle() {
        var textStyle: TextStyle? = null
        var h6Style: TextStyle? = null
        composeTestRule.setMaterialContent {
            Container {
                TopAppBar(
                    leadingIcon = {},
                    titleTextLabel = {
                        textStyle = +currentTextStyle()
                        h6Style = +themeTextStyle { h6 }
                    },
                    trailingIcons = {}
                )
            }
        }
        Truth.assertThat(textStyle!!.fontSize).isEqualTo(h6Style!!.fontSize)
        Truth.assertThat(textStyle!!.fontFamily).isEqualTo(h6Style!!.fontFamily)
    }

    @Test
    fun topAppBarTrailingIcons_noIcons() {
        var trailingIconInfo: LayoutCoordinates? = null
        composeTestRule.setMaterialContent {
            Container {
                OnChildPositioned(onPositioned = { position ->
                    trailingIconInfo = position
                }) {
                    TopAppBarTrailingIcons(emptyList())
                }
            }
        }
        assertDoesNotExist { testTag == "Trailing icon" }

        withDensity(composeTestRule.density) {
            val trailingIconWidth = trailingIconInfo?.size?.width?.round()
            Truth.assertThat(trailingIconWidth).isNull()
        }
    }

    @Test
    fun topAppBarTrailingIcons_oneIcon() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                TopAppBarTrailingIcons(icons = listOf(24.dp))
            }
            .assertWidthEqualsTo { 24.dp.toIntPx() + 24.dp.toIntPx() }

        findByTag("Trailing icon").assertIsVisible()
        assertDoesNotExist { testTag == "Overflow icon" }
    }

    @Test
    fun topAppBarTrailingIcons_twoIcons() {

        composeTestRule.setMaterialContentAndTestSizes {
            TopAppBarTrailingIcons(icons = listOf(24.dp, 24.dp))
        }.assertWidthEqualsTo { (24.dp.toIntPx() * 2) + (24.dp.toIntPx() * 2) }

        findAll { testTag == "Trailing icon" }.apply {
            forEach {
                it.assertIsVisible()
            }
        }.assertCountEquals(2)
        assertDoesNotExist { testTag == "Overflow icon" }
    }

    @Test
    fun topAppBarTrailingIcons_threeIcons() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                TopAppBarTrailingIcons(icons = listOf(24.dp, 24.dp, 24.dp))
            }
            .assertWidthEqualsTo { 24.dp.toIntPx() * 2 + 24.dp.toIntPx() * 3 + 12.dp.toIntPx() }
        // icons and spacers + 12.dp overflow

        findAll { testTag == "Trailing icon" }.apply {
            forEach {
                it.assertIsVisible()
            }
        }.assertCountEquals(2)
        findByTag("Overflow icon").assertIsVisible()
    }
}