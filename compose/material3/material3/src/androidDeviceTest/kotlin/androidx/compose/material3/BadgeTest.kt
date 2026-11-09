/*
 * Copyright 2021 The Android Open Source Project
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.tokens.BadgeTokens
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onSibling
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class BadgeTest {

    private val icon = Icons.Filled.Favorite

    @get:Rule val rule = createComposeRule()

    @Test
    fun badge_noContent_size() {
        rule
            .setMaterialContentForSizeAssertions { Badge() }
            .assertHeightIsEqualTo(BadgeTokens.Size)
            .assertWidthIsEqualTo(BadgeTokens.Size)
    }

    @Test
    fun badge_shortContent_size() {
        rule
            .setMaterialContentForSizeAssertions { Badge { Text("1") } }
            .assertHeightIsEqualTo(BadgeTokens.LargeSize)
            .assertWidthIsEqualTo(BadgeTokens.LargeSize)
    }

    @Test
    fun badge_longContent_size() {
        rule
            .setMaterialContentForSizeAssertions { Badge { Text("999+") } }
            .assertHeightIsEqualTo(BadgeTokens.LargeSize)
            .assertWidthIsAtLeast(BadgeTokens.LargeSize)
    }

    @Test
    fun badge_shortContent_customSizeModifier_size() {
        val customWidth = 24.dp
        val customHeight = 6.dp
        rule
            .setMaterialContentForSizeAssertions {
                Badge(modifier = Modifier.size(customWidth, customHeight)) { Text("1") }
            }
            .assertHeightIsEqualTo(customHeight)
            .assertWidthIsEqualTo(customWidth)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun badge_noContent_shape() {
        var shape = RectangleShape
        var errorColor = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            shape = BadgeTokens.Shape.value
            errorColor = BadgeTokens.Color.value
            Badge(modifier = Modifier.testTag(TestBadgeTag))
        }

        rule
            .onNodeWithTag(TestBadgeTag)
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = shape,
                shapeColor = errorColor,
                backgroundColor = Color.White,
                antiAliasingGap = with(rule.density) { 1.dp.toPx() },
            )
    }

    @Test
    fun badgeBox_noContent_position() {
        rule.setMaterialContent(lightColorScheme()) {
            BadgedBox(badge = { Badge(Modifier.testTag(TestBadgeTag)) }) {
                Icon(icon, null, modifier = Modifier.testTag(TestAnchorTag))
            }
        }
        val badge = rule.onNodeWithTag(TestBadgeTag)
        val anchorBounds = rule.onNodeWithTag(TestAnchorTag).getUnclippedBoundsInRoot()
        badge.assertPositionInRootIsEqualTo(
            expectedLeft = anchorBounds.right - BadgeOffset,
            expectedTop = anchorBounds.top,
        )
    }

    @Test
    @SdkSuppress(maxSdkVersion = 34) // b/384973010: Failing on SDK 35
    fun badgeBox_shortContent_position() {
        rule.setMaterialContent(lightColorScheme()) {
            BadgedBox(badge = { Badge { Text("8") } }) {
                Icon(icon, null, modifier = Modifier.testTag(TestAnchorTag))
            }
        }
        val badge = rule.onNodeWithTag(TestAnchorTag).onSibling()
        val anchorBounds = rule.onNodeWithTag(TestAnchorTag).getUnclippedBoundsInRoot()
        val badgeBounds = badge.getUnclippedBoundsInRoot()

        val totalBadgeHorizontalOffset =
            -BadgeWithContentHorizontalOffset + BadgeWithContentHorizontalPadding
        badge.assertPositionInRootIsEqualTo(
            expectedLeft = anchorBounds.right + totalBadgeHorizontalOffset,
            expectedTop = -badgeBounds.height + BadgeWithContentVerticalOffset,
        )
    }

    @Test
    fun badgeBox_longContent_position() {
        rule.setMaterialContent(lightColorScheme()) {
            BadgedBox(badge = { Badge { Text("999+") } }) {
                Icon(icon, null, modifier = Modifier.testTag(TestAnchorTag))
            }
        }
        val badge = rule.onNodeWithTag(TestAnchorTag).onSibling()
        val anchorBounds = rule.onNodeWithTag(TestAnchorTag).getUnclippedBoundsInRoot()
        val badgeBounds = badge.getUnclippedBoundsInRoot()

        val totalBadgeHorizontalOffset =
            -BadgeWithContentHorizontalOffset + BadgeWithContentHorizontalPadding
        badge.assertPositionInRootIsEqualTo(
            expectedLeft = anchorBounds.right + totalBadgeHorizontalOffset,
            expectedTop = -badgeBounds.height + BadgeWithContentVerticalOffset,
        )
    }

    @Test
    fun badge_notMergingDescendants_withOwnContentDescription() {
        rule.setMaterialContent(lightColorScheme()) {
            BadgedBox(
                badge = { Badge { Text("99+") } },
                modifier =
                    Modifier.testTag(TestBadgeTag).semantics {
                        this.contentDescription = "more than 99 new email"
                    },
            ) {
                Text(
                    "inbox",
                    Modifier.semantics { this.contentDescription = "inbox" }.testTag(TestAnchorTag),
                )
            }
        }

        rule.onNodeWithTag(TestBadgeTag).assertContentDescriptionEquals("more than 99 new email")
        rule.onNodeWithTag(TestAnchorTag).assertContentDescriptionEquals("inbox")
    }

    @Test
    fun badgeBox_size() {
        rule
            .setMaterialContentForSizeAssertions {
                BadgedBox(badge = { Badge { Text("999+") } }) { Icon(icon, null) }
            }
            .assertWidthIsEqualTo(icon.defaultWidth)
            .assertHeightIsEqualTo(icon.defaultHeight)
    }

    @Test
    fun badgeBox_smallGreatGrandParentAndLargeAnchor_adjustedBadge() {
        val greatGrandParentTag = "greatGrandParentLayout"
        val badgeTag = "badgeTag"

        rule.setMaterialContent(lightColorScheme()) {
            Box(
                modifier =
                    Modifier.background(Color.Blue)
                        .badgeBounds()
                        .size(50.dp)
                        .testTag(greatGrandParentTag)
            ) {
                Box {
                    BadgedBox(
                        badge = { Badge(modifier = Modifier.testTag(badgeTag)) { Text("999+") } }
                    ) {
                        Icon(icon, null, modifier = Modifier.size(40.dp))
                    }
                }
            }
        }

        val badge = rule.onNodeWithTag(badgeTag)
        val badgeBounds = rule.onNodeWithTag(badgeTag).getUnclippedBoundsInRoot()
        val greatGrandParentBounds =
            rule.onNodeWithTag(greatGrandParentTag).getUnclippedBoundsInRoot()

        badge.assertTopPositionInRootIsEqualTo(greatGrandParentBounds.top)
        // Manually test the badge's right bound.
        assertThat(badgeBounds.right == greatGrandParentBounds.right).isTrue()
    }

    @Test
    fun badgeBox_resizedGreatGrandParent_updatesBadgePosition() {
        val greatGrandParentTag = "greatGrandParentLayout"
        val badgeTag = "badgeTag"
        var parentSize by mutableStateOf(50.dp)

        rule.setMaterialContent(lightColorScheme()) {
            Box(
                modifier =
                    Modifier.background(Color.Blue)
                        .badgeBounds()
                        .size(parentSize)
                        .testTag(greatGrandParentTag)
            ) {
                Box {
                    BadgedBox(
                        badge = { Badge(modifier = Modifier.testTag(badgeTag)) { Text("999+") } }
                    ) {
                        Icon(icon, null, modifier = Modifier.size(40.dp))
                    }
                }
            }
        }

        val initialBadgeBounds = rule.onNodeWithTag(badgeTag).getUnclippedBoundsInRoot()
        val initialParentBounds = rule.onNodeWithTag(greatGrandParentTag).getUnclippedBoundsInRoot()
        assertThat(initialBadgeBounds.right).isEqualTo(initialParentBounds.right)

        parentSize = 56.dp
        rule.waitForIdle()

        val updatedBadgeBounds = rule.onNodeWithTag(badgeTag).getUnclippedBoundsInRoot()
        val updatedParentBounds = rule.onNodeWithTag(greatGrandParentTag).getUnclippedBoundsInRoot()
        assertThat(updatedBadgeBounds.right).isEqualTo(updatedParentBounds.right)
        assertThat(updatedBadgeBounds.right).isGreaterThan(initialBadgeBounds.right)
    }

    @Test
    fun badgeBounds_forwardsIntrinsicsWithoutMeasuringChild() {
        var childMeasureCount = 0
        var minIntrinsicW = 0
        var maxIntrinsicW = 0
        var minIntrinsicH = 0
        var maxIntrinsicH = 0
        var childMeasureCountDuringIntrinsics = -1

        rule.setMaterialContent(lightColorScheme()) {
            Layout(
                content = {
                    Layout(
                        modifier = Modifier.badgeBounds(),
                        measurePolicy =
                            object : MeasurePolicy {
                                override fun MeasureScope.measure(
                                    measurables: List<Measurable>,
                                    constraints: Constraints,
                                ): MeasureResult {
                                    childMeasureCount++
                                    return layout(100, 50) {}
                                }

                                override fun IntrinsicMeasureScope.minIntrinsicWidth(
                                    measurables: List<IntrinsicMeasurable>,
                                    height: Int,
                                ): Int = 11

                                override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                                    measurables: List<IntrinsicMeasurable>,
                                    height: Int,
                                ): Int = 22

                                override fun IntrinsicMeasureScope.minIntrinsicHeight(
                                    measurables: List<IntrinsicMeasurable>,
                                    width: Int,
                                ): Int = 33

                                override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                                    measurables: List<IntrinsicMeasurable>,
                                    width: Int,
                                ): Int = 44
                            },
                    )
                }
            ) { measurables, constraints ->
                val measurable = measurables.first()
                minIntrinsicW = measurable.minIntrinsicWidth(Constraints.Infinity)
                maxIntrinsicW = measurable.maxIntrinsicWidth(Constraints.Infinity)
                minIntrinsicH = measurable.minIntrinsicHeight(Constraints.Infinity)
                maxIntrinsicH = measurable.maxIntrinsicHeight(Constraints.Infinity)
                childMeasureCountDuringIntrinsics = childMeasureCount
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
        }

        rule.waitForIdle()
        assertThat(minIntrinsicW).isEqualTo(11)
        assertThat(maxIntrinsicW).isEqualTo(22)
        assertThat(minIntrinsicH).isEqualTo(33)
        assertThat(maxIntrinsicH).isEqualTo(44)
        assertThat(childMeasureCountDuringIntrinsics).isEqualTo(0)
        assertThat(childMeasureCount).isEqualTo(1)
    }
}

private const val TestBadgeTag = "badge"
private const val TestAnchorTag = "anchor"
