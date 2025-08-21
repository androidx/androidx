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

package androidx.compose.ui.graphics

import android.graphics.Rect
import android.graphics.Region
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.assertIsEqualTo
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.AndroidComposeView
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ExtraDataShapeRectCornersKey
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ExtraDataShapeRectKey
import androidx.compose.ui.platform.AndroidComposeViewAccessibilityDelegateCompat.Companion.ExtraDataShapeRegionKey
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import kotlin.math.roundToInt
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class GraphicsLayerSemanticsTest(private val modifierVariant: ModifierVariant) {

    enum class ModifierVariant {
        Simple,
        Block,
    }

    companion object {
        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() = ModifierVariant.entries.toTypedArray()
    }

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val testTag = "semantics-test-tag"
    private lateinit var androidComposeView: AndroidComposeView
    private lateinit var rootView: View

    @Test
    fun shape_clip_setsShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .parameterizedGraphicsLayer(shape = RectangleShape, clip = true)
                    .testTag(testTag)
            )
        }

        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, RectangleShape))
    }

    @Test
    fun shape_noClip_doesNotSetShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .parameterizedGraphicsLayer(shape = RectangleShape, clip = false)
                    .testTag(testTag)
            )
        }

        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun rectangleShape_clip_shapeFillsNodeBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .parameterizedGraphicsLayer(shape = RectangleShape, clip = true)
                    .testTag(testTag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            info.extras
                .getRectParcelable(ExtraDataShapeRectKey)
                .toScreenBounds(info.boundsInScreen)
                .subtractRootViewOffset()
                .assertBoundsEqualTo(left = 0.dp, top = 0.dp, right = 10.dp, bottom = 10.dp)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun roundedCornerShape_clip_shapeFillsNodeBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .parameterizedGraphicsLayer(
                        shape = RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp),
                        clip = true,
                    )
                    .testTag(testTag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            info.extras
                .getRectParcelable(ExtraDataShapeRectKey)
                .toScreenBounds(info.boundsInScreen)
                .subtractRootViewOffset()
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
    @SdkSuppress(minSdkVersion = 26)
    fun genericShape_clip_shapeFillsNodeBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .parameterizedGraphicsLayer(shape = CutCornerShape(2.dp), clip = true)
                    .testTag(testTag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            info.extras
                .getRegionParcelable(ExtraDataShapeRegionKey)
                .toScreenBounds(info.boundsInScreen)
                .subtractRootViewOffset()
                .assertBoundsEqualTo(left = 0.dp, top = 0.dp, right = 10.dp, bottom = 10.dp)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun rectangleShape_padding_shapeOffsetAndFillsDownsizedBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(100.dp).padding(10.dp)) {
                Box(
                    Modifier.size(10.dp)
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                        .parameterizedGraphicsLayer(shape = RectangleShape, clip = true)
                        .padding(2.dp)
                        .testTag(testTag)
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            info.extras
                .getRectParcelable(ExtraDataShapeRectKey)
                .toScreenBounds(info.boundsInScreen)
                .subtractRootViewOffset()
                .assertBoundsEqualTo(left = 11.dp, top = 12.dp, right = 19.dp, bottom = 18.dp)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun roundedCornerShape_padding_shapeOffsetAndFillsDownsizedBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(100.dp).padding(10.dp)) {
                Box(
                    Modifier.size(10.dp)
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                        .parameterizedGraphicsLayer(
                            shape = RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp),
                            clip = true,
                        )
                        .padding(2.dp)
                        .testTag(testTag)
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            info.extras
                .getRectParcelable(ExtraDataShapeRectKey)
                .toScreenBounds(info.boundsInScreen)
                .subtractRootViewOffset()
                .assertBoundsEqualTo(left = 11.dp, top = 12.dp, right = 19.dp, bottom = 18.dp)
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
    @SdkSuppress(minSdkVersion = 26)
    fun genericShape_padding_shapeOffsetAndFillsDownsizedBounds() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(100.dp).padding(10.dp)) {
                Box(
                    Modifier.size(10.dp)
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                        .parameterizedGraphicsLayer(shape = CutCornerShape(2.dp), clip = true)
                        .padding(2.dp)
                        .testTag(testTag)
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            info.extras
                .getRegionParcelable(ExtraDataShapeRegionKey)
                .toScreenBounds(info.boundsInScreen)
                .subtractRootViewOffset()
                .assertBoundsEqualTo(left = 11.dp, top = 12.dp, right = 19.dp, bottom = 18.dp)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun genericShape_insetRectangle_regionHasInset() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .parameterizedGraphicsLayer(shape = InsetRectangle(insetPx = 1), clip = true)
                    .testTag(testTag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            val regionBounds =
                info.extras
                    .getRegionParcelable(ExtraDataShapeRegionKey)
                    .toScreenBounds(info.boundsInScreen)
                    .subtractRootViewOffset()
            with(rule.density) {
                assertThat(regionBounds.left).isEqualTo(1)
                assertThat(regionBounds.top).isEqualTo(1)
                regionBounds.right.toDp().assertIsEqualTo(10.dp)
                regionBounds.bottom.toDp().assertIsEqualTo(10.dp)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun genericShape_insetRectangleWithPadding_regionHasInsetAndOffset() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(100.dp).padding(10.dp)) {
                Box(
                    Modifier.size(10.dp)
                        .padding(horizontal = 1.dp, vertical = 2.dp)
                        .parameterizedGraphicsLayer(
                            shape = InsetRectangle(insetPx = 3),
                            clip = true,
                        )
                        .padding(2.dp)
                        .testTag(testTag)
                )
            }
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            val regionBounds =
                info.extras
                    .getRegionParcelable(ExtraDataShapeRegionKey)
                    .toScreenBounds(info.boundsInScreen)
                    .subtractRootViewOffset()
            with(rule.density) {
                assertThat(regionBounds.left - 11.dp.toPx() - 3).isLessThan(0.5.dp.toPx())
                assertThat(regionBounds.top - 12.dp.toPx() - 3).isLessThan(0.5.dp.toPx())
                regionBounds.right.toDp().assertIsEqualTo(19.dp)
                regionBounds.bottom.toDp().assertIsEqualTo(18.dp)
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun rectangleShape_partiallyOffScreen_shapeOffsetCorrectly() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .offset(x = (-5).dp)
                    .parameterizedGraphicsLayer(shape = RectangleShape, clip = true)
                    .testTag(testTag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            info.extras
                .getRectParcelable(ExtraDataShapeRectKey)
                .toScreenBounds(info.boundsInScreen)
                .subtractRootViewOffset()
                .assertBoundsEqualTo(left = (-5).dp, top = 0.dp, right = 5.dp, bottom = 10.dp)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun viewInterop_shapePositionRespectsHostViewPosition() {
        // Arrange.
        rule.activityRule.scenario.onActivity { activity ->
            val paddedRootView =
                FrameLayout(activity).apply {
                    val padding = 10.dp.toPxInt(rule.density)
                    setPadding(padding, padding, padding, padding)
                    setBackgroundColor(android.graphics.Color.MAGENTA)
                }
            val composeHostView =
                ComposeView(activity).apply {
                    setContent {
                        androidComposeView = LocalView.current as AndroidComposeView
                        with(androidComposeView.composeAccessibilityDelegate) {
                            accessibilityForceEnabledForTesting = true
                            onSendAccessibilityEvent = { false }
                        }
                        Box(Modifier.size(100.dp).padding(10.dp).background(Color.Green)) {
                            Box(
                                Modifier.size(10.dp)
                                    .padding(horizontal = 1.dp, vertical = 2.dp)
                                    .parameterizedGraphicsLayer(shape = RectangleShape, clip = true)
                                    .padding(2.dp)
                                    .testTag(testTag)
                            )
                        }
                    }
                }
            paddedRootView.addView(composeHostView)
            activity.setContentView(paddedRootView)
            rootView = paddedRootView
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            info.extras
                .getRectParcelable(ExtraDataShapeRectKey)
                .toScreenBounds(info.boundsInScreen)
                .subtractRootViewOffset()
                .assertBoundsEqualTo(left = 21.dp, top = 22.dp, right = 29.dp, bottom = 28.dp)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun multipleShapes_outerShapeWins() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .padding(2.dp)
                    .parameterizedGraphicsLayer(shape = CutCornerShape(2.dp), clip = true)
                    .padding(2.dp)
                    .parameterizedGraphicsLayer(shape = RectangleShape, clip = true)
                    .testTag(testTag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            info.extras
                .getRegionParcelable(ExtraDataShapeRegionKey)
                .toScreenBounds(info.boundsInScreen)
                .subtractRootViewOffset()
                .assertBoundsEqualTo(left = 2.dp, top = 2.dp, right = 8.dp, bottom = 8.dp)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun clipChange_invalidatesSemanticsProperty() {
        // Arrange.
        var shouldClip by mutableStateOf(false)
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .parameterizedGraphicsLayer(shape = RoundedCornerShape(1.dp), clip = shouldClip)
                    .testTag(testTag)
            )
        }
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        rule.runOnIdle {
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }

        // Act.
        rule.runOnIdle { shouldClip = true }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectCornersKey)

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
        rule.runOnIdle {
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectKey)).isTrue()
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun shapeChange_fromRectangle_invalidatesSemanticsProperty() {
        // Arrange.
        var currentShape by mutableStateOf(RectangleShape)
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .parameterizedGraphicsLayer(shape = currentShape, clip = true)
                    .testTag(testTag)
            )
        }
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, RectangleShape))
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        rule.runOnIdle {
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectKey)).isTrue()
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
        }

        // Act.
        rule.runOnIdle { currentShape = RoundedCornerShape(1.dp) }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectCornersKey)

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
        rule.runOnIdle {
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectKey)).isTrue()
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun shapeChange_toRectangle_invalidatesSemanticsProperty() {
        // Arrange.
        var currentShape: Shape by mutableStateOf(RoundedCornerShape(1.dp))
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .parameterizedGraphicsLayer(shape = currentShape, clip = true)
                    .testTag(testTag)
            )
        }
        rule
            .onNodeWithTag(testTag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)
        rule.runOnIdle {
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectKey)).isTrue()
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isTrue()
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
        }

        // Act.
        rule.runOnIdle { currentShape = RectangleShape }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectCornersKey)

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, RectangleShape))
        rule.runOnIdle {
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun shapeChange_betweenNonRectangle_invalidatesSemanticsProperty() {
        // Arrange.
        var currentShape: Shape by mutableStateOf(RoundedCornerShape(1.dp))
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .parameterizedGraphicsLayer(shape = currentShape, clip = true)
                    .testTag(testTag)
            )
        }
        rule
            .onNodeWithTag(testTag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)
        rule.runOnIdle {
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectKey)).isTrue()
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isTrue()
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isTrue()
        }

        // Act.
        rule.runOnIdle { currentShape = CutCornerShape(1.dp) }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectCornersKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRegionKey)

        // Assert.
        rule
            .onNodeWithTag(testTag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CutCornerShape(1.dp)))
        rule.runOnIdle {
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRegionKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info2.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun alphaChange_invalidatesSemanticsVisibility() {
        // Arrange.
        var alpha by mutableStateOf(0f)
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(10.dp).parameterizedGraphicsLayer(alpha = alpha).testTag(testTag))
        }
        val virtualViewId = rule.onNodeWithTag(testTag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        rule.runOnIdle { assertThat(info.isVisibleToUser).isFalse() }

        // Act.
        rule.runOnIdle { alpha = 1f }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }

        // Assert.
        rule.runOnIdle { assertThat(info2.isVisibleToUser).isTrue() }
    }

    @Test
    fun blockGraphicsLayer_invalidateSemantics_setsGraphicsLayerScopeProperties() {
        // Arrange.
        val densityLog = mutableListOf<Float>()
        val sizeLog = mutableListOf<Size>()
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp).graphicsLayer {
                    densityLog.add(density)
                    sizeLog.add(size)
                }
            )
        }

        // Assert.
        rule.runOnIdle {
            assertThat(densityLog).containsExactly(rule.density.density, rule.density.density)

            assertThat(sizeLog).hasSize(2)
            // The size is not yet measured when semantics are invalidated the first time.
            assertThat(sizeLog[0]).isEqualTo(Size.Zero)
            // The second semantics invalidation happens after layout, so the size is now measured.
            with(rule.density) {
                sizeLog[1].width.toDp().assertIsEqualTo(10.dp)
                sizeLog[1].height.toDp().assertIsEqualTo(10.dp)
            }
        }
    }

    private fun Modifier.parameterizedGraphicsLayer(shape: Shape, clip: Boolean) =
        when (modifierVariant) {
            ModifierVariant.Simple -> this.graphicsLayer(shape = shape, clip = clip)
            ModifierVariant.Block ->
                this.graphicsLayer {
                    this.shape = shape
                    this.clip = clip
                }
        }

    private fun Modifier.parameterizedGraphicsLayer(alpha: Float) =
        when (modifierVariant) {
            ModifierVariant.Simple -> this.graphicsLayer(alpha = alpha)
            ModifierVariant.Block -> this.graphicsLayer { this.alpha = alpha }
        }

    private class InsetRectangle(val insetPx: Int) : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
            Outline.Generic(
                Path().apply {
                    moveTo(insetPx.toFloat(), insetPx.toFloat())
                    lineTo(size.width, insetPx.toFloat())
                    lineTo(size.width, size.height)
                    lineTo(insetPx.toFloat(), size.height)
                    close()
                }
            )
    }

    private fun ComposeContentTestRule.setContentWithAccessibilityEnabled(
        content: @Composable () -> Unit
    ) {
        setContent {
            rootView = LocalView.current
            androidComposeView = LocalView.current as AndroidComposeView
            with(androidComposeView.composeAccessibilityDelegate) {
                accessibilityForceEnabledForTesting = true
                onSendAccessibilityEvent = { false }
            }
            content()
        }
    }

    private val View.composeAccessibilityDelegate: AndroidComposeViewAccessibilityDelegateCompat
        get() =
            ViewCompat.getAccessibilityDelegate(this)
                as AndroidComposeViewAccessibilityDelegateCompat

    private fun addExtraDataToAccessibilityNodeInfo(
        virtualViewId: Int,
        info: AccessibilityNodeInfoCompat,
        extra: String,
    ) {
        androidComposeView.composeAccessibilityDelegate
            .getAccessibilityNodeProvider(androidComposeView)
            .addExtraDataToAccessibilityNodeInfo(virtualViewId, info, extra, Bundle())
    }

    private fun AndroidComposeView.createAccessibilityNodeInfo(
        semanticsId: Int
    ): AccessibilityNodeInfoCompat {
        onSemanticsChange()
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

    private val AccessibilityNodeInfoCompat.boundsInScreen: Rect
        get() {
            val boundsInScreen = Rect()
            getBoundsInScreen(boundsInScreen)
            return boundsInScreen
        }

    private fun Dp.toPxInt(density: Density): Int = with(density) { this@toPxInt.toPx().toInt() }

    private fun Rect.toScreenBounds(nodeBoundsInScreen: Rect): Rect = apply {
        offset(nodeBoundsInScreen.left, nodeBoundsInScreen.top)
    }

    private fun Region.toScreenBounds(nodeBoundsInScreen: Rect): Rect {
        translate(nodeBoundsInScreen.left, nodeBoundsInScreen.top)
        return bounds
    }

    private fun Rect.subtractRootViewOffset(): Rect = apply {
        val positionArray = intArrayOf(0, 0)
        rootView.getLocationOnScreen(positionArray)
        val screenX = positionArray[0].toFloat()
        val screenY = positionArray[1].toFloat()
        offset(-screenX.roundToInt(), -screenY.roundToInt())
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
}
