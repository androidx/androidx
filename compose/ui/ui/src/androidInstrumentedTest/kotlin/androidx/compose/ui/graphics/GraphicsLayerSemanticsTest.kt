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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semanticsId
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class GraphicsLayerSemanticsTest {
    @get:Rule val rule = createComposeRule()
    private val tag = "semantics-test-tag"
    private lateinit var androidComposeView: AndroidComposeView

    @Test
    fun rectangleShape_clip_doesNotSetShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp).graphicsLayer(shape = RectangleShape, clip = true).testTag(tag)
            )
        }
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
    }

    @Test
    fun block_rectangleShape_clip_doesNotSetShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer {
                        shape = RectangleShape
                        clip = true
                    }
                    .testTag(tag)
            )
        }
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
    }

    @Test
    fun roundedCornerShape_clip_setsShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = RoundedCornerShape(1.dp), clip = true)
                    .testTag(tag)
            )
        }
        rule
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
    }

    @Test
    fun block_roundedCornerShape_clip_setsShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer {
                        shape = RoundedCornerShape(1.dp)
                        clip = true
                    }
                    .testTag(tag)
            )
        }
        rule
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
    }

    @Test
    fun roundedCornerShape_noClip_doesNotSetShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = RoundedCornerShape(1.dp), clip = false)
                    .testTag(tag)
            )
        }
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
    }

    @Test
    fun block_roundedCornerShape_noClip_doesNotSetShapeSemanticsProperty() {
        rule.setContent {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer {
                        shape = RoundedCornerShape(1.dp)
                        clip = false
                    }
                    .testTag(tag)
            )
        }
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun roundedCornerShape_clip_rectFillsBoundsInScreen() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            @Suppress("DEPRECATION")
            val rect = info.extras.getParcelable<Rect>(ExtraDataShapeRectKey)!!
            rect.assertBoundsEqualTo(left = 0.dp, top = 0.dp, right = 10.dp, bottom = 10.dp)
            assertThat(rect.width()).isEqualTo(boundsInScreen.width())
            assertThat(rect.height()).isEqualTo(boundsInScreen.height())
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

    @Ignore // TODO(b/407772600): re-enable once shape supports padding
    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun roundedCornerShape_clipWithPadding_rectFillsDownsizedBoundsInScreen() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .padding(2.dp)
                    .graphicsLayer(shape = RoundedCornerShape(1.dp, 2.dp, 3.dp, 4.dp), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectCornersKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isTrue()
            @Suppress("DEPRECATION")
            val rect = info.extras.getParcelable<Rect>(ExtraDataShapeRectKey)!!
            rect.assertBoundsEqualTo(left = 2.dp, top = 2.dp, right = 8.dp, bottom = 8.dp)
            assertThat(rect.width()).isEqualTo(boundsInScreen.width())
            assertThat(rect.height()).isEqualTo(boundsInScreen.height())
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
    fun genericShape_cutCornerShapeClip_boundingRectFillsBoundsInScreen() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = CutCornerShape(2.dp), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            @Suppress("DEPRECATION")
            val region = info.extras.getParcelable<Region>(ExtraDataShapeRegionKey)!!
            val regionBounds = region.bounds
            regionBounds.assertBoundsEqualTo(left = 0.dp, top = 0.dp, right = 10.dp, bottom = 10.dp)
            assertThat(regionBounds.width()).isEqualTo(boundsInScreen.width())
            assertThat(regionBounds.height()).isEqualTo(boundsInScreen.height())
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun genericShape_insetRectangleClip_shiftsBoundingRectWithinBoundsInScreen() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer(shape = InsetRectangle(insetPx = 1), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            @Suppress("DEPRECATION")
            val region = info.extras.getParcelable<Region>(ExtraDataShapeRegionKey)!!
            val regionBounds = region.bounds
            with(rule.density) {
                assertThat(regionBounds.left).isEqualTo(1)
                assertThat(regionBounds.top).isEqualTo(1)
                regionBounds.right.toDp().assertIsEqualTo(10.dp)
                regionBounds.bottom.toDp().assertIsEqualTo(10.dp)
                assertThat(regionBounds.width()).isEqualTo(boundsInScreen.width() - 1)
                assertThat(regionBounds.height()).isEqualTo(boundsInScreen.height() - 1)
            }
        }
    }

    @Ignore // TODO(b/407772600): re-enable once shape supports padding
    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun genericShape_insetRectangleClipWithPadding_shiftsBoundingRectWithinBoundsInScreen() {
        // Arrange.
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .padding(2.dp)
                    .graphicsLayer(shape = InsetRectangle(insetPx = 1), clip = true)
                    .testTag(tag)
            )
        }
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        val boundsInScreen = Rect()
        info.getBoundsInScreen(boundsInScreen)

        // Act.
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRegionKey)

        // Assert.
        rule.runOnIdle {
            assertThat(info.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
            @Suppress("DEPRECATION")
            val region = info.extras.getParcelable<Region>(ExtraDataShapeRegionKey)!!
            val regionBounds = region.bounds
            with(rule.density) {
                assertThat(regionBounds.left).isEqualTo(2.dp.roundToPx() + 1)
                assertThat(regionBounds.top).isEqualTo(2.dp.roundToPx() + 1)
                regionBounds.right.toDp().assertIsEqualTo(8.dp)
                regionBounds.bottom.toDp().assertIsEqualTo(6.dp)
                assertThat(regionBounds.width()).isEqualTo(boundsInScreen.width() - 1)
                assertThat(regionBounds.height()).isEqualTo(boundsInScreen.height() - 1)
            }
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
                    .graphicsLayer(shape = RoundedCornerShape(1.dp), clip = shouldClip)
                    .testTag(tag)
            )
        }
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
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
            .onNodeWithTag(tag)
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
    fun block_clipChange_invalidatesSemanticsProperty() {
        // Arrange.
        var shouldClip by mutableStateOf(false)
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer {
                        shape = RoundedCornerShape(1.dp)
                        clip = shouldClip
                    }
                    .testTag(tag)
            )
        }
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
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
            .onNodeWithTag(tag)
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
            Box(Modifier.size(10.dp).graphicsLayer(shape = currentShape, clip = true).testTag(tag))
        }
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        rule.runOnIdle {
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }

        // Act.
        rule.runOnIdle { currentShape = RoundedCornerShape(1.dp) }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectCornersKey)

        // Assert.
        rule
            .onNodeWithTag(tag)
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
    fun block_shapeChange_fromRectangle_invalidatesSemanticsProperty() {
        // Arrange.
        var currentShape by mutableStateOf(RectangleShape)
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer {
                        shape = currentShape
                        clip = true
                    }
                    .testTag(tag)
            )
        }
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
        val info = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info, ExtraDataShapeRectKey)
        rule.runOnIdle {
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }

        // Act.
        rule.runOnIdle { currentShape = RoundedCornerShape(1.dp) }
        val info2 = rule.runOnIdle { androidComposeView.createAccessibilityNodeInfo(virtualViewId) }
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectKey)
        addExtraDataToAccessibilityNodeInfo(virtualViewId, info2, ExtraDataShapeRectCornersKey)

        // Assert.
        rule
            .onNodeWithTag(tag)
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
            Box(Modifier.size(10.dp).graphicsLayer(shape = currentShape, clip = true).testTag(tag))
        }
        rule
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
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
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
        rule.runOnIdle {
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectKey)).isFalse()
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun block_shapeChange_toRectangle_invalidatesSemanticsProperty() {
        // Arrange.
        var currentShape: Shape by mutableStateOf(RoundedCornerShape(1.dp))
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer {
                        shape = currentShape
                        clip = true
                    }
                    .testTag(tag)
            )
        }
        rule
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
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
        rule.onNodeWithTag(tag).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Shape))
        rule.runOnIdle {
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectKey)).isFalse()
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    fun shapeChange_betweenNonRectangle_invalidatesSemanticsProperty() {
        // Arrange.
        var currentShape: Shape by mutableStateOf(RoundedCornerShape(1.dp))
        rule.setContentWithAccessibilityEnabled {
            Box(Modifier.size(10.dp).graphicsLayer(shape = currentShape, clip = true).testTag(tag))
        }
        rule
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
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
            .onNodeWithTag(tag)
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
    fun block_shapeChange_betweenNonRectangle_invalidatesSemanticsProperty() {
        // Arrange.
        var currentShape: Shape by mutableStateOf(RoundedCornerShape(1.dp))
        rule.setContentWithAccessibilityEnabled {
            Box(
                Modifier.size(10.dp)
                    .graphicsLayer {
                        shape = currentShape
                        clip = true
                    }
                    .testTag(tag)
            )
        }
        rule
            .onNodeWithTag(tag)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Shape, RoundedCornerShape(1.dp))
            )
        val virtualViewId = rule.onNodeWithTag(tag).semanticsId()
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
            .onNodeWithTag(tag)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Shape, CutCornerShape(1.dp)))
        rule.runOnIdle {
            assertThat(info2.availableExtraData.contains(ExtraDataShapeRegionKey)).isTrue()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectKey)).isFalse()
            assertThat(info2.extras.containsKey(ExtraDataShapeRectCornersKey)).isFalse()
            assertThat(info2.extras.containsKey(ExtraDataShapeRegionKey)).isTrue()
        }
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
