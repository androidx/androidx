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

package androidx.ui.core.selection

import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import androidx.ui.core.setContent
import androidx.ui.core.test.runOnUiThreadIR
import androidx.ui.core.test.waitAndScreenShot
import androidx.ui.framework.test.TestActivity
import androidx.ui.graphics.Color
import androidx.ui.graphics.toArgb
import androidx.ui.text.style.TextDirection
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(JUnit4::class)
class SelectionHandlesTest {
    @get:Rule
    val rule = ActivityTestRule<TestActivity>(TestActivity::class.java)
    private lateinit var activity: TestActivity

    private val HANDLE_COLOR = Color(0xFF2B28F5.toInt())
    // Due to the rendering effect of captured bitmap from activity, if we want the pixels from the
    // corners, we need a little bit offset from the edges of the bitmap.
    private val OFFSET_FROM_EDGE = 5

    private val selectionLtrHandleDirection = Selection(
        start = Selection.AnchorInfo(
            direction = TextDirection.Ltr,
            offset = 0,
            selectable = mock()
        ),
        end = Selection.AnchorInfo(
            direction = TextDirection.Ltr,
            offset = 0,
            selectable = mock()
        ),
        handlesCrossed = false
    )
    private val selectionRtlHandleDirection = Selection(
        start = Selection.AnchorInfo(
            direction = TextDirection.Ltr,
            offset = 0,
            selectable = mock()
        ),
        end = Selection.AnchorInfo(
            direction = TextDirection.Ltr,
            offset = 0,
            selectable = mock()
        ),
        handlesCrossed = true
    )

    @Before
    fun setup() {
        activity = rule.activity
        activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun StartSelectionHandle_left_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                StartSelectionHandle(selection = selectionLtrHandleDirection)
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isNotEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun StartSelectionHandle_right_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                StartSelectionHandle(selection = selectionRtlHandleDirection)
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isNotEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun EndSelectionHandle_right_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                EndSelectionHandle(selection = selectionLtrHandleDirection)
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isNotEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun EndSelectionHandle_left_pointing() {
        rule.runOnUiThreadIR {
            activity.setContent {
                EndSelectionHandle(selection = selectionRtlHandleDirection)
            }
        }

        val bitmap = rule.waitAndScreenShot()
        val pixelLeftTop = bitmap.getPixel(OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        val pixelRightTop = bitmap.getPixel(bitmap.width - OFFSET_FROM_EDGE, OFFSET_FROM_EDGE)
        assertThat(pixelLeftTop).isNotEqualTo(HANDLE_COLOR.toArgb())
        assertThat(pixelRightTop).isEqualTo(HANDLE_COLOR.toArgb())
    }

    @Test
    fun isHandleLtrDirection_ltr_handles_not_cross_return_true() {
        assertThat(isHandleLtrDirection(direction = TextDirection.Ltr, areHandlesCrossed = false))
            .isTrue()
    }

    @Test
    fun isHandleLtrDirection_ltr_handles_cross_return_false() {
        assertThat(isHandleLtrDirection(direction = TextDirection.Ltr, areHandlesCrossed = true))
            .isFalse()
    }

    @Test
    fun isHandleLtrDirection_rtl_handles_not_cross_return_false() {
        assertThat(isHandleLtrDirection(direction = TextDirection.Rtl, areHandlesCrossed = false))
            .isFalse()
    }

    @Test
    fun isHandleLtrDirection_rtl_handles_cross_return_true() {
        assertThat(isHandleLtrDirection(direction = TextDirection.Rtl, areHandlesCrossed = true))
    }
}
