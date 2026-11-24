/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.foundation.text.selection

import android.graphics.Rect
import android.graphics.Region
import android.os.Bundle
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.CursorHandle
import androidx.compose.foundation.text.CursorHandleHeight
import androidx.compose.foundation.text.CursorHandleWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@SdkSuppress(minSdkVersion = 26)
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalFoundationApi::class)
class TextHandleSemanticsTest {
    private val HandleTag = "handle"
    private val ShapeTypeKey = "androidx.compose.ui.semantics.shapeType"
    private val ShapeRegionKey = "androidx.compose.ui.semantics.shapeRegion"

    @get:Rule val rule = createComposeRule()

    private var previousImplementation = false

    @Before
    fun saveImplementation() {
        previousImplementation = ComposeFoundationFlags.isNewTextFieldModifierImplementationEnabled
    }

    @After
    fun restoreImplementation() {
        ComposeFoundationFlags.isNewTextFieldModifierImplementationEnabled = previousImplementation
    }

    @Test fun cursorHandle_unpadded_shape() = cursorShape(padded = false)

    @Test fun cursorHandle_padded_shape() = cursorShape(padded = true)

    @Test
    fun selectionStart_ltr_uncrossed_unpadded_newDrawing_shape() =
        selectionShape(
            isStartHandle = true,
            direction = ResolvedTextDirection.Ltr,
            handlesCrossed = false,
            padded = false,
            expectedLeft = true,
        )

    @Test
    fun selectionEnd_ltr_uncrossed_unpadded_newDrawing_shape() =
        selectionShape(
            isStartHandle = false,
            direction = ResolvedTextDirection.Ltr,
            handlesCrossed = false,
            padded = false,
            expectedLeft = false,
        )

    @Test
    fun selectionStart_rtl_uncrossed_padded_newDrawing_shape() =
        selectionShape(
            isStartHandle = true,
            direction = ResolvedTextDirection.Rtl,
            handlesCrossed = false,
            padded = true,
            expectedLeft = false,
        )

    @Test
    fun selectionEnd_ltr_crossed_padded_newDrawing_shape() =
        selectionShape(
            isStartHandle = false,
            direction = ResolvedTextDirection.Ltr,
            handlesCrossed = true,
            padded = true,
            expectedLeft = true,
        )

    @Test
    fun selectionHandle_unpadded_legacyDrawing_visibility() =
        selectionVisibility(
            padded = false,
            newImplementation = false,
        )

    @Test
    fun selectionHandle_unpadded_newDrawing_visibility() =
        selectionVisibility(
            padded = false,
            newImplementation = true,
        )

    @Test
    fun selectionHandle_padded_legacyDrawing_visibility() =
        selectionVisibility(
            padded = true,
            newImplementation = false,
        )

    @Test
    fun selectionHandle_padded_newDrawing_visibility() =
        selectionVisibility(
            padded = true,
            newImplementation = true,
        )

    @Test
    fun selectionHandle_padded_legacyDrawing_updatesShapeWhenCrossed() =
        selectionShapeUpdates(newImplementation = false)

    @Test
    fun selectionHandle_padded_newDrawing_updatesShapeWhenCrossed() =
        selectionShapeUpdates(newImplementation = true)

    @Test
    fun selectionHandleIcon_legacyDrawing_visibility() = iconVisibility(newImplementation = false)

    @Test fun selectionHandleIcon_newDrawing_visibility() = iconVisibility(newImplementation = true)

    private fun selectionShape(
        isStartHandle: Boolean,
        direction: ResolvedTextDirection,
        handlesCrossed: Boolean,
        padded: Boolean,
        expectedLeft: Boolean,
    ) {
        ComposeFoundationFlags.isNewTextFieldModifierImplementationEnabled = true
        rule.setContent {
            SelectionHandleContent(
                isStartHandle = isStartHandle,
                direction = direction,
                handlesCrossed = handlesCrossed,
                padded = padded,
            )
        }
        assertSelectionShape(padded, expectedLeft)
    }

    private fun cursorShape(padded: Boolean) {
        rule.setContent { CursorHandleContent(padded) }
        val width = with(rule.density) { CursorHandleWidth.roundToPx() }
        val height = with(rule.density) { CursorHandleHeight.roundToPx() }
        val left = if (padded) (touchSize() - width) / 2 else 0
        val region = assertShapeBounds(DpSize(CursorHandleWidth, CursorHandleHeight), padded, left)
        assertThat(region.contains(left + width / 2, height / 2)).isTrue()
        assertThat(region.contains(left + 3, 3)).isFalse()
        assertThat(region.contains(left + width - 4, 3)).isFalse()
        assertThat(region.contains(left + width / 2, 3)).isTrue()
    }

    private fun selectionVisibility(padded: Boolean, newImplementation: Boolean) {
        ComposeFoundationFlags.isNewTextFieldModifierImplementationEnabled = newImplementation
        val visibleOffset = Offset(150f, 150f)
        var offset by mutableStateOf(visibleOffset)
        rule.setContent {
            SelectionHandleContent(
                isStartHandle = false,
                direction = ResolvedTextDirection.Ltr,
                handlesCrossed = false,
                padded = padded,
                offsetProvider = { offset },
            )
        }
        val handle = rule.onNodeWithTag(HandleTag)
        val node = handle.fetchSemanticsNode()
        val originalSize = node.size
        assertThat(shapeNode().fetchSemanticsNode().id).isEqualTo(node.id)
        rule.runOnIdle {
            assertThat(nodeInfo(node).isVisibleToUser).isTrue()
            assertThat(nodeInfo(node).childCount).isEqualTo(0)
            offset = Offset.Unspecified
        }
        handle.assertExists()
        assertThat(handle.fetchSemanticsNode().id).isEqualTo(node.id)
        assertThat(handle.fetchSemanticsNode().size).isEqualTo(originalSize)
        assertThat(handle.fetchSemanticsNode().getSelectionHandleInfo().visible).isFalse()
        assertHidden(handle, true)
        rule.runOnIdle {
            assertThat(nodeInfo(node).isVisibleToUser).isFalse()
            offset = visibleOffset
        }
        assertThat(handle.fetchSemanticsNode().id).isEqualTo(node.id)
        assertThat(handle.fetchSemanticsNode().size).isEqualTo(originalSize)
        assertThat(handle.fetchSemanticsNode().getSelectionHandleInfo().visible).isTrue()
        assertHidden(handle, false)
        assertThat(shapeNode().fetchSemanticsNode().id).isEqualTo(node.id)
        rule.runOnIdle { assertThat(nodeInfo(node).isVisibleToUser).isTrue() }
    }

    private fun iconVisibility(newImplementation: Boolean) {
        ComposeFoundationFlags.isNewTextFieldModifierImplementationEnabled = newImplementation
        var visible by mutableStateOf(true)
        rule.setContent { SelectionHandleIconContent { visible } }
        val icon = rule.onNodeWithTag(HandleTag)
        val node = icon.fetchSemanticsNode()
        rule.runOnIdle {
            assertThat(nodeInfo(node).isVisibleToUser).isTrue()
            visible = false
        }
        assertHidden(icon, true)
        rule.runOnIdle {
            assertThat(nodeInfo(node).isVisibleToUser).isFalse()
            visible = true
        }
        assertHidden(icon, false)
        rule.runOnIdle { assertThat(nodeInfo(node).isVisibleToUser).isTrue() }
    }

    private fun selectionShapeUpdates(newImplementation: Boolean) {
        ComposeFoundationFlags.isNewTextFieldModifierImplementationEnabled = newImplementation
        var crossed by mutableStateOf(false)
        rule.setContent {
            SelectionHandleContent(
                isStartHandle = true,
                direction = ResolvedTextDirection.Ltr,
                handlesCrossed = crossed,
                padded = true,
            )
        }
        val node = rule.onNodeWithTag(HandleTag).fetchSemanticsNode()
        val originalSize = node.size
        assertSelectionShape(padded = true, isLeft = true)
        rule.runOnIdle { crossed = true }
        assertThat(rule.onNodeWithTag(HandleTag).fetchSemanticsNode().id).isEqualTo(node.id)
        assertThat(rule.onNodeWithTag(HandleTag).fetchSemanticsNode().size).isEqualTo(originalSize)
        assertSelectionShape(padded = true, isLeft = false)
    }

    @Composable
    private fun SelectionHandleContent(
        isStartHandle: Boolean,
        direction: ResolvedTextDirection,
        handlesCrossed: Boolean,
        padded: Boolean,
        offsetProvider: OffsetProvider = OffsetProvider { Offset(150f, 150f) },
    ) {
        Box(Modifier.fillMaxSize()) {
            SelectionHandle(
                offsetProvider = offsetProvider,
                isStartHandle = isStartHandle,
                direction = direction,
                handlesCrossed = handlesCrossed,
                minTouchTargetSize = touchTargetSize(padded),
                lineHeight = 0f,
                modifier = Modifier.testTag(HandleTag),
            )
        }
    }

    @Composable
    private fun CursorHandleContent(padded: Boolean) {
        Box(Modifier.fillMaxSize()) {
            CursorHandle(
                offsetProvider = { Offset(150f, 150f) },
                modifier = Modifier.testTag(HandleTag),
                minTouchTargetSize = touchTargetSize(padded),
            )
        }
    }

    @Composable
    private fun SelectionHandleIconContent(visible: () -> Boolean) {
        SelectionHandleIcon(
            modifier = Modifier.testTag(HandleTag),
            iconVisible = visible,
            isLeft = false,
        )
    }

    private fun touchTargetSize(padded: Boolean) =
        if (padded) DpSize(40.dp, 40.dp) else DpSize.Unspecified

    private fun touchSize() = with(rule.density) { 40.dp.roundToPx() }

    private fun assertSelectionShape(padded: Boolean, isLeft: Boolean) {
        val width = with(rule.density) { HandleWidth.roundToPx() }
        val height = with(rule.density) { HandleHeight.roundToPx() }
        val left = if (padded && isLeft) touchSize() - width else 0
        val region = assertShapeBounds(DpSize(HandleWidth, HandleHeight), padded, left)
        assertThat(region.contains(left + width / 2, height / 2)).isTrue()
        assertThat(region.contains(left + 3, 3)).isEqualTo(!isLeft)
        assertThat(region.contains(left + width - 4, 3)).isEqualTo(isLeft)
    }

    private fun assertShapeBounds(iconSize: DpSize, padded: Boolean, expectedLeft: Int): Region {
        val handle = rule.onNodeWithTag(HandleTag).fetchSemanticsNode()
        val width = with(rule.density) { iconSize.width.roundToPx() }
        val height = with(rule.density) { iconSize.height.roundToPx() }
        assertThat(handle.size.width).isEqualTo(if (padded) touchSize() else width)
        assertThat(handle.size.height).isEqualTo(if (padded) touchSize() else height)
        assertThat(shapeNode().fetchSemanticsNode().id).isEqualTo(handle.id)
        return rule.runOnIdle {
            val info = nodeInfo(handle)
            assertThat(info.childCount).isEqualTo(0)
            val bounds = Rect().also { info.getBoundsInScreen(it) }
            assertThat(bounds.width()).isEqualTo(handle.size.width)
            assertThat(bounds.height()).isEqualTo(handle.size.height)
            assertThat(info.availableExtraData).contains(ShapeTypeKey)
            val view = (handle.root as ViewRootForTest).view
            ViewCompat.getAccessibilityDelegate(view)!!.getAccessibilityNodeProvider(view)!!
                .addExtraDataToAccessibilityNodeInfo(handle.id, info, ShapeTypeKey, Bundle())
            assertThat(info.extras.getInt(ShapeTypeKey)).isEqualTo(2) // Generic outline.
            @Suppress("DEPRECATION") // replacement only available in API 33, our min is 26.
            val region = info.extras.getParcelable<Region>(ShapeRegionKey)!!
            assertThat(region.bounds.left.toFloat()).isWithin(1f).of(expectedLeft.toFloat())
            assertThat(region.bounds.top).isEqualTo(0)
            assertThat(region.bounds.width().toFloat()).isWithin(1f).of(width.toFloat())
            assertThat(region.bounds.right.toFloat())
                .isWithin(1f)
                .of((expectedLeft + width).toFloat())
            assertThat(region.bounds.bottom.toFloat()).isWithin(1f).of(height.toFloat())
            assertThat(info.isImportantForAccessibility).isFalse()
            assertThat(info.isScreenReaderFocusable).isFalse()
            assertThat(info.isFocusable).isFalse()
            region
        }
    }

    private fun shapeNode() =
        rule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Shape),
            useUnmergedTree = true,
        )

    private fun assertHidden(node: SemanticsNodeInteraction, hidden: Boolean) {
        assertThat(
                node.fetchSemanticsNode().config.contains(SemanticsProperties.HideFromAccessibility)
            )
            .isEqualTo(hidden)
    }

    private fun nodeInfo(node: SemanticsNode): AccessibilityNodeInfoCompat {
        val view = (node.root as ViewRootForTest).view
        return AccessibilityNodeInfoCompat.wrap(
            checkNotNull(view.accessibilityNodeProvider.createAccessibilityNodeInfo(node.id))
        )
    }
}
