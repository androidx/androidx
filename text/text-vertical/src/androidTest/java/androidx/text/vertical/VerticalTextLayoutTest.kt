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

package androidx.text.vertical

import android.os.Build
import android.text.TextPaint
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VerticalTextLayoutTest {
    // The detailed behavior tests are written in LineBreakerTests and underlying LayoutRunTests.
    // In this test case, just check the set in builder and get in instance.

    val PAINT =
        TextPaint().apply() {
            textSize = 10f // make 1em = 10px
        }

    val JP_TEXT = "吾輩は猫である。\n1904年(明治39年)生まれである。\n英名はI Am a Catである。"

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun verticalTextLayout_Create_Api36() {
        val layout = createVerticalTextLayout()
        assertThat(layout.width).isGreaterThan(0f)
        assertThat(layout.impl).isInstanceOf(VerticalTextLayoutApi36Impl::class.java)
        (layout.impl as VerticalTextLayoutApi36Impl).run {
            assertThat(text).isEqualTo(JP_TEXT)
            assertThat(start).isEqualTo(0)
            assertThat(end).isEqualTo(JP_TEXT.length)
            assertThat(paint).isSameInstanceAs(PAINT)
            assertThat(height).isEqualTo(100f)
            assertThat(orientation).isEqualTo(TextOrientation.MIXED)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun verticalTextLayout_isVerticalTextSupported_Api36() {
        assertThat(createVerticalTextLayout().isVerticalTextSupported()).isTrue()
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun verticalTextLayout_CreateDefaultParams_UnderApi36() {
        val layout = createVerticalTextLayout()
        assertThat(layout.width).isEqualTo(0f) // fallback to default params
        assertThat(layout.impl).isInstanceOf(VerticalTextLayoutNoOpImpl::class.java)
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun verticalTextLayout_isVerticalTextSupported_UnderApi36() {
        assertThat(createVerticalTextLayout().isVerticalTextSupported()).isFalse()
    }

    private fun createVerticalTextLayout() =
        VerticalTextLayout(JP_TEXT, 0, JP_TEXT.length, PAINT, 100f)
}
