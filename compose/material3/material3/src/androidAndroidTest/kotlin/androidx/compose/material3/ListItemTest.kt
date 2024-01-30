/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.tokens.ListTokens
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt
import com.google.common.truth.Truth.assertThat

@MediumTest
@RunWith(AndroidJUnit4::class)
class ListItemTest {

    @get:Rule
    val rule = createComposeRule()

    val icon24x24 by lazy { ImageBitmap(width = 24.dp.toIntPx(), height = 24.dp.toIntPx()) }
    val icon40x40 by lazy { ImageBitmap(width = 40.dp.toIntPx(), height = 40.dp.toIntPx()) }

    @Test
    fun listItem_oneLine_size() {
        val expectedHeightNoIcon = ListTokens.ListItemOneLineContainerHeight
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(headlineContent = { Text("Primary text") })
            }
            .assertHeightIsEqualTo(expectedHeightNoIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_oneLine_withIcon_size() {
        val expectedHeightSmallIcon = ListTokens.ListItemOneLineContainerHeight
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    headlineContent = { Text("Primary text") },
                    leadingContent = { Icon(icon24x24, null) }
                )
            }
            .assertHeightIsEqualTo(expectedHeightSmallIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_twoLine_size() {
        val expectedHeightNoIcon = ListTokens.ListItemTwoLineContainerHeight
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    headlineContent = { Text("Primary text") },
                    supportingContent = { Text("Secondary text") }
                )
            }
            .assertHeightIsEqualTo(expectedHeightNoIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_twoLine_withIcon_size() {
        val expectedHeightWithIcon = ListTokens.ListItemTwoLineContainerHeight

        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    headlineContent = { Text("Primary text") },
                    supportingContent = { Text("Secondary text") },
                    leadingContent = { Icon(icon24x24, null) }
                )
            }
            .assertHeightIsEqualTo(expectedHeightWithIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_threeLine_size() {
        val expectedHeight = ListTokens.ListItemThreeLineContainerHeight
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    overlineContent = { Text("OVERLINE") },
                    headlineContent = { Text("Primary text") },
                    supportingContent = { Text("Secondary text") }
                )
            }
            .assertHeightIsEqualTo(expectedHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_oneLine_positioning_noIcon() {
        val listItemHeight = ListTokens.ListItemOneLineContainerHeight
        val expectedStartPadding = 16.dp
        val expectedEndPadding = 24.dp

        val textPosition = Ref<Offset>()
        val textSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()

        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    headlineContent = {
                        Text("Primary text", Modifier.saveLayout(textPosition, textSize))
                    },
                    trailingContent = {
                        Image(
                            icon24x24,
                            null,
                            Modifier.saveLayout(trailingPosition, trailingSize))
                    }
                )
            }
        }

        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx()
            )
            assertThat(textPosition.value!!.y).isWithin(0.5f).of(
                (listItemHeight.toPx() - textSize.value!!.height) / 2f
            )

            assertThat(trailingPosition.value!!.x).isWithin(0.5f).of(
                ds.width.toPx() - trailingSize.value!!.width - expectedEndPadding.toPx()
            )
            assertThat(trailingPosition.value!!.y).isWithin(0.5f).of(
                (listItemHeight.toPx() - trailingSize.value!!.height) / 2f
            )
        }
    }

    @Test
    fun listItem_oneLine_positioning_withIcon() {
        val listItemHeight = ListTokens.ListItemOneLineContainerHeight
        val expectedStartPadding = 16.dp
        val expectedTextStartPadding = 16.dp

        val textPosition = Ref<Offset>()
        val textSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    headlineContent = {
                        Text("Primary text", Modifier.saveLayout(textPosition, textSize))
                    },
                    leadingContent = {
                        Image(
                            icon24x24,
                            null,
                            Modifier.saveLayout(iconPosition, iconSize))
                    }
                )
            }
        }
        rule.runOnIdleWithDensity {
            assertThat(iconPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx()
            )
            assertThat(iconPosition.value!!.y).isWithin(0.5f).of(
                (listItemHeight.toPx() - iconSize.value!!.height) / 2f
            )

            assertThat(textPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx() +
                    iconSize.value!!.width +
                    expectedTextStartPadding.toPx()
            )
            assertThat(textPosition.value!!.y).isWithin(0.5f).of(
                (listItemHeight.toPx() - textSize.value!!.height) / 2f
            )
        }
    }

    @Test
    fun listItem_twoLine_positioning_noIcon() {
        val listItemHeight = ListTokens.ListItemTwoLineContainerHeight
        val expectedStartPadding = 16.dp
        val expectedEndPadding = 24.dp

        val textPosition = Ref<Offset>()
        val textSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    headlineContent = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize)
                        )
                    },
                    supportingContent = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(secondaryTextPosition, secondaryTextSize)
                        )
                    },
                    trailingContent = {
                        Text(
                            "meta",
                            Modifier.saveLayout(trailingPosition, trailingSize)
                        )
                    }
                )
            }
        }
        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            val totalTextHeight = textSize.value!!.height + secondaryTextSize.value!!.height

            assertThat(textPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx()
            )
            assertThat(textPosition.value!!.y).isWithin(0.5f).of(
                (listItemHeight.toPx() - totalTextHeight) / 2f
            )

            assertThat(secondaryTextPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx()
            )
            assertThat(secondaryTextPosition.value!!.y).isWithin(0.5f).of(
                (listItemHeight.toPx() - totalTextHeight) / 2f + textSize.value!!.height
            )

            assertThat(trailingPosition.value!!.x).isWithin(0.5f).of(
                ds.width.toPx() - trailingSize.value!!.width -
                    expectedEndPadding.toPx()
            )
            assertThat(trailingPosition.value!!.y).isWithin(0.5f).of(
                (listItemHeight.toPx() - trailingSize.value!!.height) / 2f
            )
        }
    }

    @Test
    fun listItem_twoLine_positioning_withIcon() {
        val listItemHeight = ListTokens.ListItemTwoLineContainerHeight
        val expectedStartPadding = 16.dp
        val expectedContentStartPadding = 16.dp

        val textPosition = Ref<Offset>()
        val textSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    headlineContent = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize)
                        )
                    },
                    supportingContent = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(secondaryTextPosition, secondaryTextSize)
                        )
                    },
                    leadingContent = {
                        Image(icon24x24, null, Modifier.saveLayout(iconPosition, iconSize))
                    }
                )
            }
        }
        rule.runOnIdleWithDensity {
            val totalTextHeight = textSize.value!!.height + secondaryTextSize.value!!.height

            assertThat(textPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx() + iconSize.value!!.width +
                    expectedContentStartPadding.toPx()
            )
            assertThat(textPosition.value!!.y).isWithin(0.5f).of(
                (listItemHeight.toPx() - totalTextHeight) / 2f
            )

            assertThat(secondaryTextPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx() + iconSize.value!!.width +
                    expectedContentStartPadding.toPx()
            )
            assertThat(secondaryTextPosition.value!!.y).isWithin(0.5f).of(
                (listItemHeight.toPx() - totalTextHeight) / 2f + textSize.value!!.height
            )

            assertThat(iconPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx()
            )
            assertThat(iconPosition.value!!.y).isWithin(0.5f).of(
                (listItemHeight.toPx() - iconSize.value!!.height) / 2f
            )
        }
    }

    @Test
    fun listItem_threeLine_positioning_noOverline_metaText() {
        val expectedStartPadding = 16.dp
        val expectedContentStartPadding = 16.dp
        val expectedEndPadding = 24.dp

        val textPosition = Ref<Offset>()
        val textSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    headlineContent = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize)
                        )
                    },
                    supportingContent = {
                        Text(
                            "Very long supporting text which will span two lines",
                            Modifier.saveLayout(secondaryTextPosition, secondaryTextSize)
                        )
                    },
                    leadingContent = {
                        Image(icon24x24, null, Modifier.saveLayout(iconPosition, iconSize))
                    },
                    trailingContent = {
                        Image(icon24x24, null, Modifier.saveLayout(trailingPosition, trailingSize))
                    }
                )
            }
        }
        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            // TODO(b/233782301): Test y positions when this is implemented as a 3-line ListItem
            assertThat(textPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx() + iconSize.value!!.width +
                    expectedContentStartPadding.toPx()
            )

            assertThat(secondaryTextPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx() + iconSize.value!!.width +
                    expectedContentStartPadding.toPx()
            )

            assertThat(iconPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx()
            )

            assertThat(trailingPosition.value!!.x).isWithin(0.5f).of(
                ds.width.toPx() - trailingSize.value!!.width -
                    expectedEndPadding.toPx()
            )
        }
    }

    @Test
    fun listItem_threeLine_positioning_overline_trailingIcon() {
        val expectedTopPadding = 12.dp
        val expectedStartPadding = 16.dp
        val expectedContentStartPadding = 16.dp
        val expectedEndPadding = 24.dp

        val textPosition = Ref<Offset>()
        val textSize = Ref<IntSize>()
        val overlineTextPosition = Ref<Offset>()
        val overlineTextSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    overlineContent = {
                        Text(
                            "OVERLINE",
                            Modifier.saveLayout(overlineTextPosition, overlineTextSize)
                        )
                    },
                    headlineContent = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize)
                        )
                    },
                    supportingContent = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(secondaryTextPosition, secondaryTextSize)
                        )
                    },
                    leadingContent = {
                        Image(
                            icon40x40,
                            null,
                            Modifier.saveLayout(iconPosition, iconSize)
                        )
                    },
                    trailingContent = {
                        Text(
                            "meta",
                            Modifier.saveLayout(trailingPosition, trailingSize)
                        )
                    }
                )
            }
        }

        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx() + iconSize.value!!.width +
                    expectedContentStartPadding.toPx()
            )
            assertThat(textPosition.value!!.y).isWithin(0.5f).of(
                expectedTopPadding.toPx() + overlineTextSize.value!!.height
            )

            assertThat(secondaryTextPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx() + iconSize.value!!.width +
                    expectedContentStartPadding.toPx()
            )
            assertThat(secondaryTextPosition.value!!.y).isWithin(0.5f).of(
                expectedTopPadding.toPx() + overlineTextSize.value!!.height +
                textSize.value!!.height
            )

            assertThat(iconPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx()
            )
            assertThat(iconPosition.value!!.y).isWithin(0.5f).of(
                expectedTopPadding.toPx()
            )

            assertThat(trailingPosition.value!!.x).isWithin(0.5f).of(
                ds.width.toPx() - trailingSize.value!!.width -
                    expectedEndPadding.toPx()
            )
            assertThat(trailingPosition.value!!.y).isWithin(0.5f).of(
                expectedTopPadding.toPx()
            )

            assertThat(overlineTextPosition.value!!.x).isWithin(0.5f).of(
                expectedStartPadding.toPx() + iconSize.value!!.width +
                    expectedContentStartPadding.toPx()
            )
            assertThat(overlineTextPosition.value!!.y).isWithin(0.5f).of(
                expectedTopPadding.toPx()
            )
        }
    }

    private fun Dp.toIntPx() = (this.value * rule.density.density).roundToInt()

    private fun Modifier.saveLayout(
        coords: Ref<Offset>,
        size: Ref<IntSize>,
    ): Modifier = onGloballyPositioned { coordinates: LayoutCoordinates ->
        coords.value = coordinates.localToRoot(Offset.Zero)
        size.value = coordinates.size
    }
}