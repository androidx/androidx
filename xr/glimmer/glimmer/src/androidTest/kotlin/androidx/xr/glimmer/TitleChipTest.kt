/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isNotFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
// The expected min sdk is 35, but we test on 33 for wider device coverage (some APIs are not
// available below 33)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
class TitleChipTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @Test
    fun semantics() {
        rule.setGlimmerThemeContent {
            Box { TitleChip(modifier = Modifier.testTag("titleChip")) { Text("Messages") } }
        }

        rule.onNodeWithTag("titleChip").assert(isNotFocusable())
    }

    @Test
    fun shapeAndColorFromThemeIsUsed() {
        lateinit var expectedShape: Shape
        val surfaceColor = Color.Blue
        rule.setGlimmerThemeContent {
            GlimmerTheme(Colors(surface = surfaceColor)) {
                expectedShape = GlimmerTheme.shapes.large
                TitleChip(modifier = Modifier.testTag("titleChip"), border = null) {
                    Box(Modifier.size(100.dp, 100.dp))
                }
            }
        }

        rule
            .onNodeWithTag("titleChip")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = expectedShape,
                shapeColor = surfaceColor,
                backgroundColor = Color.Black,
                antiAliasingGap = with(rule.density) { 1.dp.toPx() },
            )
    }

    @Test
    fun setsLocalTextStyle() {
        lateinit var actualTextStyle: TextStyle
        lateinit var expectedTextStyle: TextStyle
        rule.setGlimmerThemeContent {
            expectedTextStyle = GlimmerTheme.typography.titleSmall
            TitleChip { actualTextStyle = LocalTextStyle.current }
        }

        rule.runOnIdle { assertThat(actualTextStyle).isEqualTo(expectedTextStyle) }
    }

    @Test
    fun setsContentColor() {
        var primary = Color.Unspecified
        var leadingIconContentColor = Color.Unspecified
        var contentContentColor = Color.Unspecified
        rule.setGlimmerThemeContent {
            primary = GlimmerTheme.colors.primary
            TitleChip(
                leadingIcon = {
                    Box(
                        DelegatableNodeProviderElement {
                            leadingIconContentColor = it?.currentContentColor() ?: Color.Unspecified
                        }
                    )
                }
            ) {
                Box(
                    DelegatableNodeProviderElement {
                        contentContentColor = it?.currentContentColor() ?: Color.Unspecified
                    }
                )
            }
        }

        rule.runOnIdle {
            assertThat(leadingIconContentColor).isEqualTo(primary)
            assertThat(contentContentColor).isEqualTo(Color.White)
        }
    }

    @Test
    fun setsLocalIconSize() {
        var actualLeadingIconSize: Dp? = null
        var expectedIconSize: Dp? = null
        rule.setGlimmerThemeContent {
            expectedIconSize = GlimmerTheme.iconSizes.medium
            TitleChip(leadingIcon = { actualLeadingIconSize = LocalIconSize.current }) {}
        }

        rule.runOnIdle { assertThat(actualLeadingIconSize!!).isEqualTo(expectedIconSize!!) }
    }

    @Test
    fun positioning() {
        rule.setGlimmerThemeContent {
            TitleChip(modifier = Modifier.testTag("titleChip")) {
                Text("Messages", modifier = Modifier.testTag("text"))
            }
        }

        val titleChipBounds =
            rule.onNodeWithTag("titleChip", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()

        (textBounds.left - titleChipBounds.left).assertIsEqualTo(
            16.dp,
            "padding between the start of the titleChip and the start of the text.",
        )

        (titleChipBounds.right - textBounds.right).assertIsEqualTo(
            16.dp,
            "padding between the end of the text and the end of the titleChip.",
        )

        titleChipBounds.height.assertIsEqualTo(56.dp, "height of titleChip.")
    }

    @Test
    fun positioning_withIcon() {
        rule.setGlimmerThemeContent {
            TitleChip(
                modifier = Modifier.testTag("titleChip"),
                leadingIcon = {
                    Icon(
                        FavoriteIcon,
                        contentDescription = "Localized description",
                        modifier = Modifier.testTag("leadingIcon"),
                    )
                },
            ) {
                Text("Messages", modifier = Modifier.testTag("text"))
            }
        }

        val textBounds =
            rule.onNodeWithTag("text", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val leadingIconBounds =
            rule.onNodeWithTag("leadingIcon", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val titleChipBounds =
            rule.onNodeWithTag("titleChip", useUnmergedTree = true).getUnclippedBoundsInRoot()

        (leadingIconBounds.left - titleChipBounds.left).assertIsEqualTo(
            8.dp,
            "Padding between start of titleChip and start of leading icon.",
        )

        (textBounds.left - leadingIconBounds.right).assertIsEqualTo(
            8.dp,
            "Padding between end of leading icon and start of text.",
        )

        (titleChipBounds.right - textBounds.right).assertIsEqualTo(
            16.dp,
            "padding between the end of the text and the end of the titleChip.",
        )

        titleChipBounds.height.assertIsEqualTo(56.dp, "height of titleChip.")
    }

    @Test
    fun minHeightAndMinWidthCanBeOverridden() {
        rule.setGlimmerThemeContent {
            TitleChip(
                contentPadding = PaddingValues(),
                modifier =
                    Modifier.requiredWidthIn(20.dp).requiredHeightIn(15.dp).testTag("titleChip"),
            ) {
                Spacer(Modifier.requiredSize(10.dp))
            }
        }

        rule.onNodeWithTag("titleChip").apply {
            with(getBoundsInRoot()) {
                width.assertIsEqualTo(20.dp, "width")
                height.assertIsEqualTo(15.dp, "height")
            }
        }
    }

    @Test
    fun hasMaxWidthAndFlexibleHeight() {
        rule.setGlimmerThemeContent {
            TitleChip(contentPadding = PaddingValues(), modifier = Modifier.testTag("titleChip")) {
                Spacer(Modifier.size(500.dp, height = 100.dp))
            }
        }

        rule.onNodeWithTag("titleChip").apply {
            with(getBoundsInRoot()) {
                width.assertIsEqualTo(352.dp, "width")
                height.assertIsEqualTo(100.dp, "height")
            }
        }
    }
}
