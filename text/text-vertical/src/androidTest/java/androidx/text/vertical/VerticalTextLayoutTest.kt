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

import android.text.TextPaint
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
    fun `create default params`() {
        VerticalTextLayout.Builder(JP_TEXT, 0, JP_TEXT.length, PAINT, 100f).build().run {
            assertThat(text).isEqualTo(JP_TEXT)
            assertThat(start).isEqualTo(0)
            assertThat(end).isEqualTo(JP_TEXT.length)
            assertThat(paint).isSameInstanceAs(PAINT)
            assertThat(height).isEqualTo(100f)
            assertThat(orientation).isEqualTo(TextOrientation.MIXED)
        }
    }

    @Test
    fun `create upright orientation`() {
        VerticalTextLayout.Builder(JP_TEXT, 0, JP_TEXT.length, PAINT, 100f)
            .setOrientation(TextOrientation.UPRIGHT)
            .build()
            .run {
                assertThat(text).isEqualTo(JP_TEXT)
                assertThat(start).isEqualTo(0)
                assertThat(end).isEqualTo(JP_TEXT.length)
                assertThat(paint).isSameInstanceAs(PAINT)
                assertThat(height).isEqualTo(100f)
                assertThat(orientation).isEqualTo(TextOrientation.UPRIGHT)
            }
    }
}
