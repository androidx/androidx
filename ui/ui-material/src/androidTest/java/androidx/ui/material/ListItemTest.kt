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
import androidx.ui.text.FirstBaseline
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.Modifier
import androidx.ui.core.Ref
import androidx.ui.core.onPositioned
import androidx.ui.foundation.Box
import androidx.ui.foundation.Image
import androidx.ui.foundation.Text
import androidx.ui.graphics.ImageAsset
import androidx.ui.test.createComposeRule
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.PxPosition
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class ListItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    val icon24x24 by lazy { ImageAsset(width = 24.dp.toIntPx(), height = 24.dp.toIntPx()) }
    val icon40x40 by lazy { ImageAsset(width = 40.dp.toIntPx(), height = 40.dp.toIntPx()) }
    val icon56x56 by lazy { ImageAsset(width = 56.dp.toIntPx(), height = 56.dp.toIntPx()) }

    @Test
    fun listItem_oneLine_size() {
        val dm = composeTestRule.displayMetrics
        val expectedHeightNoIcon = 48.dp
        composeTestRule
            .setMaterialContentAndCollectSizes {
                ListItem(text = "Primary text")
            }
            .assertHeightEqualsTo(expectedHeightNoIcon)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun listItem_oneLine_withIcon24_size() {
        val dm = composeTestRule.displayMetrics
        val expectedHeightSmallIcon = 56.dp
        composeTestRule
            .setMaterialContentAndCollectSizes {
                ListItem(text = "Primary text", icon = icon24x24)
            }
            .assertHeightEqualsTo(expectedHeightSmallIcon)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun listItem_oneLine_withIcon56_size() {
        val dm = composeTestRule.displayMetrics
        val expectedHeightLargeIcon = 72.dp
        composeTestRule
            .setMaterialContentAndCollectSizes {
                ListItem(text = "Primary text", icon = icon56x56)
            }
            .assertHeightEqualsTo(expectedHeightLargeIcon)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun listItem_twoLine_size() {
        val dm = composeTestRule.displayMetrics
        val expectedHeightNoIcon = 64.dp
        composeTestRule
            .setMaterialContentAndCollectSizes {
                ListItem(text = "Primary text", secondaryText = "Secondary text")
            }
            .assertHeightEqualsTo(expectedHeightNoIcon)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun listItem_twoLine_withIcon_size() {
        val dm = composeTestRule.displayMetrics
        val expectedHeightWithIcon = 72.dp

        composeTestRule
            .setMaterialContentAndCollectSizes {
                ListItem(
                    text = "Primary text",
                    secondaryText = "Secondary text",
                    icon = icon24x24
                )
            }
            .assertHeightEqualsTo(expectedHeightWithIcon)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun listItem_threeLine_size() {
        val dm = composeTestRule.displayMetrics
        val expectedHeight = 88.dp
        composeTestRule
            .setMaterialContentAndCollectSizes {
                ListItem(
                    overlineText = "OVERLINE",
                    text = "Primary text",
                    secondaryText = "Secondary text"
                )
            }
            .assertHeightEqualsTo(expectedHeight)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun listItem_threeLine_noSingleLine_size() {
        val dm = composeTestRule.displayMetrics
        val expectedHeight = 88.dp
        composeTestRule
            .setMaterialContentAndCollectSizes {
                ListItem(
                    text = "Primary text",
                    secondaryText = "Secondary text with long text",
                    singleLineSecondaryText = false
                )
            }
            .assertHeightEqualsTo(expectedHeight)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun listItem_threeLine_metaText_size() {
        val dm = composeTestRule.displayMetrics
        val expectedHeight = 88.dp
        composeTestRule
            .setMaterialContentAndCollectSizes {
                ListItem(
                    overlineText = "OVERLINE",
                    text = "Primary text",
                    secondaryText = "Secondary text",
                    metaText = "meta"
                )
            }
            .assertHeightEqualsTo(expectedHeight)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun listItem_threeLine_noSingleLine_metaText_size() {
        val dm = composeTestRule.displayMetrics
        val expectedHeight = 88.dp
        composeTestRule
            .setMaterialContentAndCollectSizes {
                ListItem(
                    text = "Primary text",
                    secondaryText = "Secondary text with long text",
                    singleLineSecondaryText = false,
                    metaText = "meta"
                )
            }
            .assertHeightEqualsTo(expectedHeight)
            .assertWidthEqualsTo { dm.widthPixels.ipx }
    }

    @Test
    fun listItem_oneLine_positioning_noIcon() {
        val listItemHeight = 48.dp
        val expectedLeftPadding = 16.dp
        val expectedRightPadding = 16.dp

        val textPosition = Ref<PxPosition>()
        val textSize = Ref<IntPxSize>()
        val trailingPosition = Ref<PxPosition>()
        val trailingSize = Ref<IntPxSize>()
        composeTestRule.setMaterialContent {
            Box {
                ListItem(
                    text = { Text("Primary text", saveLayout(textPosition, textSize)) },
                    trailing = {
                        Image(icon24x24, saveLayout(trailingPosition, trailingSize))
                    }
                )
            }
        }
        composeTestRule.runOnIdleComposeWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(expectedLeftPadding.toIntPx()
                .value.toFloat())
            assertThat(textPosition.value!!.y).isEqualTo(
                ((listItemHeight.toIntPx() - textSize.value!!.height) / 2).value.toFloat()
            )
            val dm = composeTestRule.displayMetrics
            assertThat(trailingPosition.value!!.x).isEqualTo(
                dm.widthPixels - trailingSize.value!!.width.value -
                        expectedRightPadding.toIntPx().value.toFloat()
            )
            assertThat(trailingPosition.value!!.y).isEqualTo(
                ((listItemHeight.toIntPx() - trailingSize.value!!.height) / 2).value.toFloat()
            )
        }
    }

    @Test
    fun listItem_oneLine_positioning_withIcon() {
        val listItemHeight = 56.dp
        val expectedLeftPadding = 16.dp
        val expectedTextLeftPadding = 32.dp

        val textPosition = Ref<PxPosition>()
        val textSize = Ref<IntPxSize>()
        val iconPosition = Ref<PxPosition>()
        val iconSize = Ref<IntPxSize>()
        composeTestRule.setMaterialContent {
            Box {
                ListItem(
                    text = { Text("Primary text", saveLayout(textPosition, textSize)) },
                    icon = { Image(icon24x24, saveLayout(iconPosition, iconSize)) }
                )
            }
        }
        composeTestRule.runOnIdleComposeWithDensity {
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(iconPosition.value!!.y).isEqualTo(
                ((listItemHeight.toIntPx() - iconSize.value!!.height) / 2).value.toFloat()
            )
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat() +
                        iconSize.value!!.width.value +
                        expectedTextLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(textPosition.value!!.y).isEqualTo(
                ((listItemHeight.toIntPx() - textSize.value!!.height) / 2).value.toFloat()
            )
        }
    }

    @Test
    fun listItem_twoLine_positioning_noIcon() {
        val expectedLeftPadding = 16.dp
        val expectedRightPadding = 16.dp
        val expectedTextBaseline = 28.dp
        val expectedSecondaryTextBaselineOffset = 20.dp

        val textPosition = Ref<PxPosition>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntPxSize>()
        val secondaryTextPosition = Ref<PxPosition>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntPxSize>()
        val trailingPosition = Ref<PxPosition>()
        val trailingBaseline = Ref<Float>()
        val trailingSize = Ref<IntPxSize>()
        composeTestRule.setMaterialContent {
            Box {
                ListItem(
                    text = {
                        Text("Primary text", saveLayout(textPosition, textSize, textBaseline))
                    },
                    secondaryText = {
                        Text(
                            "Secondary text",
                            saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    trailing = {
                        Text("meta", saveLayout(trailingPosition, trailingSize, trailingBaseline))
                    }
                )
            }
        }
        composeTestRule.runOnIdleComposeWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(textBaseline.value!!).isEqualTo(
                expectedTextBaseline.toIntPx().value.toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(secondaryTextBaseline.value!!).isEqualTo(
                expectedTextBaseline.toIntPx().value.toFloat() +
                        expectedSecondaryTextBaselineOffset.toIntPx().value.toFloat()
            )
            val dm = composeTestRule.displayMetrics
            assertThat(trailingPosition.value!!.x).isEqualTo(
                dm.widthPixels - trailingSize.value!!.width.value -
                        expectedRightPadding.toIntPx().value.toFloat()
            )
            assertThat(trailingBaseline.value!!).isEqualTo(
                expectedTextBaseline.toIntPx().value.toFloat()
            )
        }
    }

    @Test
    fun listItem_twoLine_positioning_withSmallIcon() {
        val expectedLeftPadding = 16.dp
        val expectedIconTopPadding = 16.dp
        val expectedContentLeftPadding = 32.dp
        val expectedTextBaseline = 32.dp
        val expectedSecondaryTextBaselineOffset = 20.dp

        val textPosition = Ref<PxPosition>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntPxSize>()
        val secondaryTextPosition = Ref<PxPosition>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntPxSize>()
        val iconPosition = Ref<PxPosition>()
        val iconSize = Ref<IntPxSize>()
        composeTestRule.setMaterialContent {
            Box {
                ListItem(
                    text = {
                        Text("Primary text", saveLayout(textPosition, textSize, textBaseline))
                    },
                    secondaryText = {
                        Text(
                            "Secondary text",
                            saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    icon = {
                        Image(icon24x24, saveLayout(iconPosition, iconSize))
                    }
                )
            }
        }
        composeTestRule.runOnIdleComposeWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat() + iconSize.value!!.width.value +
                        expectedContentLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(textBaseline.value!!).isEqualTo(
                expectedTextBaseline.toIntPx().value.toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat() +
                        iconSize.value!!.width.value +
                        expectedContentLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(secondaryTextBaseline.value!!).isEqualTo(
                expectedTextBaseline.toIntPx().value.toFloat() +
                        expectedSecondaryTextBaselineOffset.toIntPx().value.toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(iconPosition.value!!.y).isEqualTo(
                expectedIconTopPadding.toIntPx().value.toFloat()
            )
        }
    }

    @Test
    fun listItem_twoLine_positioning_withLargeIcon() {
        val listItemHeight = 72.dp
        val expectedLeftPadding = 16.dp
        val expectedIconTopPadding = 16.dp
        val expectedContentLeftPadding = 16.dp
        val expectedTextBaseline = 32.dp
        val expectedSecondaryTextBaselineOffset = 20.dp
        val expectedRightPadding = 16.dp

        val textPosition = Ref<PxPosition>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntPxSize>()
        val secondaryTextPosition = Ref<PxPosition>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntPxSize>()
        val iconPosition = Ref<PxPosition>()
        val iconSize = Ref<IntPxSize>()
        val trailingPosition = Ref<PxPosition>()
        val trailingSize = Ref<IntPxSize>()
        composeTestRule.setMaterialContent {
            Box {
                ListItem(
                    text = {
                        Text("Primary text", saveLayout(textPosition, textSize, textBaseline))
                    },
                    secondaryText = {
                        Text(
                            "Secondary text",
                            saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    icon = {
                        Image(icon40x40, saveLayout(iconPosition, iconSize))
                    },
                    trailing = {
                        Image(icon24x24, saveLayout(trailingPosition, trailingSize))
                    }
                )
            }
        }
        composeTestRule.runOnIdleComposeWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat() + iconSize.value!!.width.value +
                        expectedContentLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(textBaseline.value!!).isEqualTo(
                expectedTextBaseline.toIntPx().value.toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat() +
                        iconSize.value!!.width.value +
                        expectedContentLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(secondaryTextBaseline.value!!).isEqualTo(
                expectedTextBaseline.toIntPx().value.toFloat() +
                        expectedSecondaryTextBaselineOffset.toIntPx().value.toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(iconPosition.value!!.y).isEqualTo(
                expectedIconTopPadding.toIntPx().value.toFloat()
            )
            val dm = composeTestRule.displayMetrics
            assertThat(trailingPosition.value!!.x).isEqualTo(
                dm.widthPixels - trailingSize.value!!.width.value -
                        expectedRightPadding.toIntPx().value.toFloat()
            )
            assertThat(trailingPosition.value!!.y).isEqualTo(
                ((listItemHeight.toIntPx() - trailingSize.value!!.height) / 2).value.toFloat()
            )
        }
    }

    @Test
    fun listItem_threeLine_positioning_noOverline_metaText() {
        val expectedLeftPadding = 16.dp
        val expectedIconTopPadding = 16.dp
        val expectedContentLeftPadding = 32.dp
        val expectedTextBaseline = 28.dp
        val expectedSecondaryTextBaselineOffset = 20.dp
        val expectedRightPadding = 16.dp

        val textPosition = Ref<PxPosition>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntPxSize>()
        val secondaryTextPosition = Ref<PxPosition>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntPxSize>()
        val iconPosition = Ref<PxPosition>()
        val iconSize = Ref<IntPxSize>()
        val trailingPosition = Ref<PxPosition>()
        val trailingSize = Ref<IntPxSize>()
        composeTestRule.setMaterialContent {
            Box {
                ListItem(
                    text = {
                        Text("Primary text", saveLayout(textPosition, textSize, textBaseline))
                    },
                    secondaryText = {
                        Text(
                            "Secondary text",
                            saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    singleLineSecondaryText = false,
                    icon = {
                        Image(icon24x24, saveLayout(iconPosition, iconSize))
                    },
                    trailing = {
                        Image(icon24x24, saveLayout(trailingPosition, trailingSize))
                    }
                )
            }
        }
        composeTestRule.runOnIdleComposeWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat() + iconSize.value!!.width.value +
                        expectedContentLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(textBaseline.value!!).isEqualTo(
                expectedTextBaseline.toIntPx().value.toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat() + iconSize.value!!.width.value +
                        expectedContentLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(secondaryTextBaseline.value!!).isEqualTo(
                expectedTextBaseline.toIntPx().value.toFloat() +
                        expectedSecondaryTextBaselineOffset.toIntPx().value.toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(expectedLeftPadding.toIntPx()
                .value.toFloat())
            assertThat(iconPosition.value!!.y).isEqualTo(
                expectedIconTopPadding.toIntPx().value.toFloat()
            )
            val dm = composeTestRule.displayMetrics
            assertThat(trailingPosition.value!!.x).isEqualTo(
                dm.widthPixels - trailingSize.value!!.width.value.toFloat() -
                        expectedRightPadding.toIntPx().value.toFloat()
            )
            assertThat(trailingPosition.value!!.y).isEqualTo(
                expectedIconTopPadding.toIntPx().value.toFloat()
            )
        }
    }

    @Test
    fun listItem_threeLine_positioning_overline_trailingIcon() {
        val expectedLeftPadding = 16.dp
        val expectedIconTopPadding = 16.dp
        val expectedContentLeftPadding = 16.dp
        val expectedOverlineBaseline = 28.dp
        val expectedTextBaselineOffset = 20.dp
        val expectedSecondaryTextBaselineOffset = 20.dp
        val expectedRightPadding = 16.dp

        val textPosition = Ref<PxPosition>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntPxSize>()
        val overlineTextPosition = Ref<PxPosition>()
        val overlineTextBaseline = Ref<Float>()
        val overlineTextSize = Ref<IntPxSize>()
        val secondaryTextPosition = Ref<PxPosition>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntPxSize>()
        val iconPosition = Ref<PxPosition>()
        val iconSize = Ref<IntPxSize>()
        val trailingPosition = Ref<PxPosition>()
        val trailingSize = Ref<IntPxSize>()
        val trailingBaseline = Ref<Float>()
        composeTestRule.setMaterialContent {
            Box {
                ListItem(
                    overlineText = {
                        Text(
                            "OVERLINE",
                            saveLayout(
                                overlineTextPosition,
                                overlineTextSize,
                                overlineTextBaseline
                            )
                        )
                    },
                    text = {
                        Text("Primary text", saveLayout(textPosition, textSize, textBaseline))
                    },
                    secondaryText = {
                        Text(
                            "Secondary text",
                            saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    icon = {
                        Image(
                            icon40x40,
                            saveLayout(iconPosition, iconSize)
                        )
                    },
                    trailing = {
                        Text(
                            "meta",
                            saveLayout(
                                trailingPosition,
                                trailingSize,
                                trailingBaseline
                            )
                        )
                    }
                )
            }
        }
        composeTestRule.runOnIdleComposeWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat() +
                        iconSize.value!!.width.value +
                        expectedContentLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(textBaseline.value!!).isEqualTo(
                expectedOverlineBaseline.toIntPx().value.toFloat() +
                        expectedTextBaselineOffset.toIntPx().value.toFloat()
            )
            assertThat(overlineTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat() +
                        iconSize.value!!.width.value +
                        expectedContentLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(overlineTextBaseline.value!!).isEqualTo(
                expectedOverlineBaseline.toIntPx().value.toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat() +
                        iconSize.value!!.width.value +
                        expectedContentLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(secondaryTextBaseline.value!!).isEqualTo(
                expectedOverlineBaseline.toIntPx().value.toFloat() +
                        expectedTextBaselineOffset.toIntPx().value.toFloat() +
                        expectedSecondaryTextBaselineOffset.toIntPx().value.toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedLeftPadding.toIntPx().value.toFloat()
            )
            assertThat(iconPosition.value!!.y).isEqualTo(
                expectedIconTopPadding.toIntPx().value.toFloat()
            )
            val dm = composeTestRule.displayMetrics
            assertThat(trailingPosition.value!!.x).isEqualTo(
                dm.widthPixels - trailingSize.value!!.width.value -
                        expectedRightPadding.toIntPx().value.toFloat()
            )
            assertThat(trailingBaseline.value!!).isEqualTo(
                expectedOverlineBaseline.toIntPx().value.toFloat()
            )
        }
    }

    private fun Dp.toIntPx() = (this.value * composeTestRule.density.density).roundToInt()

    private fun saveLayout(
        coords: Ref<PxPosition>,
        size: Ref<IntPxSize>,
        baseline: Ref<Float> = Ref()
    ): Modifier = Modifier.onPositioned { coordinates: LayoutCoordinates ->
        coords.value = coordinates.localToGlobal(PxPosition.Origin)
        baseline.value = coordinates[FirstBaseline]?.value?.toFloat()?.let {
            it + coords.value!!.y
        }
        size.value = coordinates.size
    }
}
