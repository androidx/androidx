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

package androidx.compose.foundation

import android.graphics.Rect
import android.graphics.Region
import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BorderSemanticsTest {

    @get:Rule val rule = createComposeRule()

    val testTag = "BorderTag"

    private lateinit var androidComposeView: View

    @Test
    fun rectangleShape_setsShapeSemanticsAndAccessibility() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .border(BorderStroke(1.dp, Color.Red), RectangleShape)
                    .testTag(testTag)
            ) {}
        }
        val virtualViewId = rule.onNodeWithTag(testTag).fetchSemanticsNode().id
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, RectangleShape))
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            info.extras
                .getRectParcelable(ExtraDataShapeRectKey)
                .assertBoundsEqualTo(left = 0.dp, top = 0.dp, right = 10.dp, bottom = 10.dp)
        }
    }

    @Test
    fun roundedCornerShape_setsShapeSemanticsAndAccessibility() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .border(
                        BorderStroke(1.dp, Color.Red),
                        RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp),
                    )
                    .testTag(testTag)
            ) {}
        }
        val virtualViewId = rule.onNodeWithTag(testTag).fetchSemanticsNode().id
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Shape,
                    RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp),
                )
            )
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            info.extras
                .getRectParcelable(ExtraDataShapeRectKey)
                .assertBoundsEqualTo(left = 0.dp, top = 0.dp, right = 10.dp, bottom = 10.dp)

            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
            val corners = info.extras.getFloatArray(ExtraDataShapeRectCornersKey)!!
            with(rule.density) {
                assertThat(corners)
                    .isEqualTo(
                        floatArrayOf(
                            1.dp.toPx(),
                            1.dp.toPx(),
                            2.dp.toPx(),
                            2.dp.toPx(),
                            3.dp.toPx(),
                            3.dp.toPx(),
                            4.dp.toPx(),
                            4.dp.toPx(),
                        )
                    )
            }
        }
    }

    @Test
    fun genericShape_setsShapeSemanticsAndAccessibility() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .border(BorderStroke(1.dp, Color.Red), CutCornerShape(2.dp))
                    .testTag(testTag)
            ) {}
        }
        val virtualViewId = rule.onNodeWithTag(testTag).fetchSemanticsNode().id
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CutCornerShape(2.dp)))
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            info.extras
                .getRegionParcelable(ExtraDataShapeRegionKey)
                .bounds
                .assertBoundsEqualTo(left = 0.dp, top = 0.dp, right = 10.dp, bottom = 10.dp)
        }
    }

    @Test
    fun rectangleShape_padding_shapeOffsetAndFillsDownsizedBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(100.dp).padding(10.dp)) {
                Box(
                    Modifier.size(10.dp)
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                        .border(BorderStroke(1.dp, Color.Red), RectangleShape)
                        .padding(2.dp)
                        .testTag(testTag)
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(testTag).fetchSemanticsNode().id
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, RectangleShape))
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            info.extras
                .getRectParcelable(ExtraDataShapeRectKey)
                .assertBoundsEqualTo(left = 1.dp, top = 2.dp, right = 9.dp, bottom = 8.dp)
        }
    }

    @Test
    fun roundedCornerShape_padding_shapeOffsetAndFillsDownsizedBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(100.dp).padding(10.dp)) {
                Box(
                    Modifier.size(10.dp)
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                        .border(
                            BorderStroke(1.dp, Color.Red),
                            RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp),
                        )
                        .padding(2.dp)
                        .testTag(testTag)
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(testTag).fetchSemanticsNode().id
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Shape,
                    RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp),
                )
            )
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            info.extras
                .getRectParcelable(ExtraDataShapeRectKey)
                .assertBoundsEqualTo(left = 1.dp, top = 2.dp, right = 9.dp, bottom = 8.dp)

            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
            val corners = info.extras.getFloatArray(ExtraDataShapeRectCornersKey)!!
            with(rule.density) {
                assertThat(corners)
                    .isEqualTo(
                        floatArrayOf(
                            1.dp.toPx(),
                            1.dp.toPx(),
                            2.dp.toPx(),
                            2.dp.toPx(),
                            3.dp.toPx(),
                            3.dp.toPx(),
                            4.dp.toPx(),
                            4.dp.toPx(),
                        )
                    )
            }
        }
    }

    @Test
    fun genericShape_padding_shapeOffsetAndFillsDownsizedBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(100.dp).padding(10.dp)) {
                Box(
                    Modifier.size(10.dp)
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                        .border(BorderStroke(1.dp, Color.Red), CutCornerShape(2.dp))
                        .padding(2.dp)
                        .testTag(testTag)
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(testTag).fetchSemanticsNode().id
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CutCornerShape(2.dp)))
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            info.extras
                .getRegionParcelable(ExtraDataShapeRegionKey)
                .bounds
                .assertBoundsEqualTo(left = 1.dp, top = 2.dp, right = 9.dp, bottom = 8.dp)
        }
    }

    @Test
    fun shapeChange_fromRectangle_invalidatesSemanticsProperty() {
        // Arrange.
        var shape by mutableStateOf(RectangleShape)
        rule.setContent {
            Box(
                Modifier.size(10.dp).border(BorderStroke(1.dp, Color.Red), shape).testTag(testTag)
            ) {}
        }
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, RectangleShape))

        // Act.
        rule.runOnIdle { shape = CircleShape }

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CircleShape))
    }

    @Test
    fun shapeChange_toRectangle_invalidatesSemanticsProperty() {
        // Arrange.
        var shape: Shape by mutableStateOf(CircleShape)
        rule.setContent {
            Box(
                Modifier.size(10.dp).border(BorderStroke(1.dp, Color.Red), shape).testTag(testTag)
            ) {}
        }
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CircleShape))

        // Act.
        rule.runOnIdle { shape = RectangleShape }

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, RectangleShape))
    }

    @Test
    fun shapeChange_betweenNonRectangles_invalidatesSemanticsProperty() {
        // Arrange.
        var shape: Shape by mutableStateOf(CircleShape)
        rule.setContent {
            Box(
                Modifier.size(10.dp).border(BorderStroke(1.dp, Color.Red), shape).testTag(testTag)
            ) {}
        }
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CircleShape))

        // Act.
        rule.runOnIdle { shape = CutCornerShape(1.dp) }

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CutCornerShape(1.dp)))
    }

    private fun ComposeContentTestRule.setContentWithAccessibilityEnabled(
        content: @Composable () -> Unit
    ) {
        setContent {
            androidComposeView = LocalView.current
            content()
        }
    }

    private val View.composeAccessibilityDelegate: AccessibilityDelegateCompat
        get() = ViewCompat.getAccessibilityDelegate(this)!!

    private fun addExtraDataToAccessibilityNodeInfo(
        virtualViewId: Int,
        info: AccessibilityNodeInfoCompat,
        extra: String,
    ) {
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)!!
            .addExtraDataToAccessibilityNodeInfo(virtualViewId, info, extra, Bundle())
    }

    private fun View.createAccessibilityNodeInfo(semanticsId: Int): AccessibilityNodeInfoCompat {
        val accNodeInfo = accessibilityNodeProvider.createAccessibilityNodeInfo(semanticsId)
        checkNotNull(accNodeInfo) { "Could not find semantics node with id = $semanticsId" }
        return AccessibilityNodeInfoCompat.wrap(accNodeInfo)
    }

    private fun Bundle.getRectParcelable(key: String): Rect {
        @Suppress("DEPRECATION")
        return getParcelable(key)!!
    }

    private fun Bundle.getRegionParcelable(key: String): Region {
        @Suppress("DEPRECATION")
        return getParcelable(key)!!
    }

    private fun Rect.assertBoundsEqualTo(left: Dp, top: Dp, right: Dp, bottom: Dp) {
        val dpRect = toDpRect()
        dpRect.left.assertIsEqualTo(left, "left")
        dpRect.top.assertIsEqualTo(top, "top")
        dpRect.right.assertIsEqualTo(right, "right")
        dpRect.bottom.assertIsEqualTo(bottom, "bottom")
    }

    private fun Rect.toDpRect(): DpRect =
        with(rule.density) { DpRect(left.toDp(), top.toDp(), right.toDp(), bottom.toDp()) }

    companion object {
        const val ExtraDataShapeRectKey = "androidx.compose.ui.semantics.shapeRect"
        const val ExtraDataShapeRectCornersKey = "androidx.compose.ui.semantics.shapeCorners"
        const val ExtraDataShapeRegionKey = "androidx.compose.ui.semantics.shapeRegion"
    }
}
