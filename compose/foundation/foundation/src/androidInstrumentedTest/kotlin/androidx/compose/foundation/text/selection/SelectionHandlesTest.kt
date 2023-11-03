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

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
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

    private val selectionLtrHandleDirection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = 0
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = 0
        ),
        handlesCrossed = false
    )

    private val selectionRtlHandleDirection = Selection(
        start = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = 0
        ),
        end = Selection.AnchorInfo(
            direction = ResolvedTextDirection.Ltr,
            offset = 0,
            selectableId = 0
        ),
        handlesCrossed = true
    )

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun StartSelectionHandle_left_pointing() {
        rule.setContent {
            DefaultSelectionHandle(
                modifier = Modifier,
                isStartHandle = true,
                direction = selectionLtrHandleDirection.start.direction,
                handlesCrossed = selectionLtrHandleDirection.handlesCrossed
            )
        }

        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val pixelLeftTop = bitmap.getPixel(0, 0)
        val pixelRightTop = bitmap.getPixel(bitmap.width - 1, 0)
        assertThat(pixelLeftTop).isNotEqualTo(handleColor.toArgb())
        assertThat(pixelRightTop).isEqualTo(handleColor.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun StartSelectionHandle_right_pointing() {
        rule.setContent {
            DefaultSelectionHandle(
                modifier = Modifier,
                isStartHandle = true,
                direction = selectionRtlHandleDirection.start.direction,
                handlesCrossed = selectionRtlHandleDirection.handlesCrossed
            )
        }

        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val pixelLeftTop = bitmap.getPixel(0, 0)
        val pixelRightTop = bitmap.getPixel(bitmap.width - 1, 0)
        assertThat(pixelLeftTop).isEqualTo(handleColor.toArgb())
        assertThat(pixelRightTop).isNotEqualTo(handleColor.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun EndSelectionHandle_right_pointing() {
        rule.setContent {
            DefaultSelectionHandle(
                modifier = Modifier,
                isStartHandle = false,
                direction = selectionLtrHandleDirection.end.direction,
                handlesCrossed = selectionLtrHandleDirection.handlesCrossed
            )
        }

        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val pixelLeftTop = bitmap.getPixel(0, 0)
        val pixelRightTop = bitmap.getPixel(bitmap.width - 1, 0)
        assertThat(pixelLeftTop).isEqualTo(handleColor.toArgb())
        assertThat(pixelRightTop).isNotEqualTo(handleColor.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun EndSelectionHandle_left_pointing() {
        rule.setContent {
            DefaultSelectionHandle(
                modifier = Modifier,
                isStartHandle = false,
                direction = selectionRtlHandleDirection.end.direction,
                handlesCrossed = selectionRtlHandleDirection.handlesCrossed
            )
        }

        rule.waitForIdle()
        val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
        val pixelLeftTop = bitmap.getPixel(0, 0)
        val pixelRightTop = bitmap.getPixel(bitmap.width - 1, 0)
        assertThat(pixelLeftTop).isNotEqualTo(handleColor.toArgb())
        assertThat(pixelRightTop).isEqualTo(handleColor.toArgb())
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
