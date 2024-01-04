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

package androidx.compose.foundation.text.selection

import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.text.Handle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SelectionHandlesTest {
    @get:Rule
    val rule = createComposeRule()

    private val handleColor = Color(0xFF4286F4)
    private val backgroundColor = Color.White

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun SelectionHandleIcon_left_pointsTopRight() {
        rule.setContent {
            SelectionHandleIcon(
                modifier = Modifier.background(backgroundColor),
                iconVisible = { true },
                isLeft = true,
            )
        }

        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val pixelLeftTop = bitmap.getPixel(0, 0)
        val pixelRightTop = bitmap.getPixel(bitmap.width - 1, 0)
        assertThat(pixelLeftTop).isEqualTo(backgroundColor.toArgb())
        assertThat(pixelRightTop).isEqualTo(handleColor.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun SelectionHandleIcon_right_pointsTopLeft() {
        rule.setContent {
            SelectionHandleIcon(
                modifier = Modifier.background(backgroundColor),
                iconVisible = { true },
                isLeft = false,
            )
        }

        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val pixelLeftTop = bitmap.getPixel(0, 0)
        val pixelRightTop = bitmap.getPixel(bitmap.width - 1, 0)
        assertThat(pixelLeftTop).isEqualTo(handleColor.toArgb())
        assertThat(pixelRightTop).isEqualTo(backgroundColor.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun SelectionHandleIcon_left_notVisible() {
        rule.setContent {
            SelectionHandleIcon(
                modifier = Modifier.background(backgroundColor),
                iconVisible = { false },
                isLeft = true,
            )
        }
        assertThat(rule.onRoot().uniquePixels()).containsExactly(backgroundColor.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun SelectionHandleIcon_right_notVisible() {
        rule.setContent {
            SelectionHandleIcon(
                modifier = Modifier.background(backgroundColor),
                iconVisible = { false },
                isLeft = false,
            )
        }
        assertThat(rule.onRoot().uniquePixels()).containsExactly(backgroundColor.toArgb())
    }

    /**
     * When the offset changes to and from [Offset.Unspecified],
     * we want to ensure that the semantics and visibility change as expected. If the
     * semantics here aren't correct, many other tests will likely fail as well.
     *
     * If [Offset.Unspecified]: no SelectionHandleInfo semantics, not visible.
     *
     * else: SelectionHandleInfo semantics exists, is visible.
     *
     * The test tag should always be found because the layout will exist either way.
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun DefaultSelectionHandle_visibilityAndSemantics_changesFromOffsetState() {
        val tag = "testTag"
        val specifiedOffset = Offset(5f, 5f)
        var offsetState by mutableStateOf(specifiedOffset)
        rule.setContent {
            SelectionHandle(
                modifier = Modifier
                    .testTag(tag)
                    .background(backgroundColor),
                offsetProvider = { offsetState },
                isStartHandle = false,
                direction = ResolvedTextDirection.Ltr,
                handlesCrossed = false,
            )
        }

        rule.waitForIdle()
        val testNode = rule.onNodeWithTag(tag)
        val startHandleNode = rule.onNode(isSelectionHandle(Handle.SelectionStart))
        val endHandleNode = rule.onNode(isSelectionHandle(Handle.SelectionEnd))

        testNode.assertExists()
        assertThat(testNode.uniquePixels()).contains(handleColor.toArgb())
        startHandleNode.assertDoesNotExist()
        endHandleNode.assertExists()
        endHandleNode.assertVisible(visible = true)

        offsetState = Offset.Unspecified
        rule.waitForIdle()
        testNode.assertExists()
        assertThat(testNode.uniquePixels()).containsExactly(backgroundColor.toArgb())
        startHandleNode.assertDoesNotExist()
        endHandleNode.assertExists()
        endHandleNode.assertVisible(visible = false)

        offsetState = specifiedOffset
        rule.waitForIdle()
        assertThat(testNode.uniquePixels()).contains(handleColor.toArgb())
        testNode.assertExists()
        startHandleNode.assertDoesNotExist()
        endHandleNode.assertExists()
        endHandleNode.assertVisible(visible = true)
    }

    @Test
    @SmallTest
    fun isHandleLtrDirection_ltr_handles_not_cross_return_true() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Ltr, areHandlesCrossed = false)
        ).isTrue()
    }

    @Test
    @SmallTest
    fun isHandleLtrDirection_ltr_handles_cross_return_false() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Ltr, areHandlesCrossed = true)
        ).isFalse()
    }

    @Test
    @SmallTest
    fun isHandleLtrDirection_rtl_handles_not_cross_return_false() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Rtl, areHandlesCrossed = false)
        ).isFalse()
    }

    @Test
    @SmallTest
    fun isHandleLtrDirection_rtl_handles_cross_return_true() {
        assertThat(
            isHandleLtrDirection(direction = ResolvedTextDirection.Rtl, areHandlesCrossed = true)
        ).isTrue()
    }
}

private fun SemanticsNodeInteraction.assertVisible(visible: Boolean) {
    val isVisible = fetchSemanticsNode().getSelectionHandleInfo().visible
    assertThat(isVisible).let { if (visible) it.isTrue() else it.isFalse() }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun SemanticsNodeInteraction.uniquePixels(): Set<Int> =
    captureToImage().asAndroidBitmap().uniquePixels()

private fun Bitmap.uniquePixels(): Set<Int> = buildSet {
    for (x in 0 until width) {
        for (y in 0 until height) {
            add(getPixel(x, y))
        }
    }
}
