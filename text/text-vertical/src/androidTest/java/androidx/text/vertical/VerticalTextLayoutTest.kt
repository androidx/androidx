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
        TextPaint().apply {
            textSize = 10f // make 1em = 10px
        }

    val JP_TEXT = "吾輩は猫である。\n1904年(明治39年)生まれである。\n英名はI Am a Catである。"

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun constructor_Api36() {
        val layout = createVerticalTextLayout()
        assertThat(layout.width).isGreaterThan(0f)
        assertThat(layout.impl).isInstanceOf(VerticalTextLayoutApi36Impl::class.java)
        (layout.impl as VerticalTextLayoutApi36Impl).run {
            assertThat(text).isEqualTo(JP_TEXT)
            assertThat(start).isEqualTo(0)
            assertThat(end).isEqualTo(JP_TEXT.length)
            assertThat(paint).isSameInstanceAs(PAINT)
            assertThat(height).isEqualTo(100f)
            assertThat(orientation).isEqualTo(TextOrientation.Mixed)
        }
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun constructor_CreateDefaultParams_BelowApi36() {
        val layout = createVerticalTextLayout()
        assertThat(layout.width).isEqualTo(0f) // fallback to default params
        assertThat(layout.impl).isInstanceOf(VerticalTextLayoutNoOpImpl::class.java)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun isVerticalTextSupported_Api36() {
        assertThat(createVerticalTextLayout().isVerticalTextSupported()).isTrue()
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun isVerticalTextSupported_BelowApi36() {
        assertThat(createVerticalTextLayout().isVerticalTextSupported()).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun lineCount_singleColumn() {
        val layout = VerticalTextLayout("あ", 0, 1, PAINT, 100f)
        assertThat(layout.lineCount).isEqualTo(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun lineCount_multipleColumns() {
        val text = "吾輩は猫である。名前はまだ無い。"
        val layout = VerticalTextLayout(text, 0, text.length, PAINT, 30f)
        assertThat(layout.lineCount).isGreaterThan(1)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    fun lineCount_isConsistentWithWidth() {
        val text = "吾輩は猫である。名前はまだ無い。"
        val layout = VerticalTextLayout(text, 0, text.length, PAINT, 30f)
        assertThat(layout.lineCount).isGreaterThan(0)
        assertThat(layout.width).isGreaterThan(0f)
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun lineCount_noOp_belowApi36() {
        val layout = VerticalTextLayout("あ", 0, 1, PAINT, 100f)
        assertThat(layout.lineCount).isEqualTo(0)
    }

    private fun createVerticalTextLayout() =
        VerticalTextLayout(JP_TEXT, 0, JP_TEXT.length, PAINT, 100f)
}
