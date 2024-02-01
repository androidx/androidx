/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation.text2.input.internal

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MathUtilsTest {

    @Test
    fun findClosestRect_findsClosestRect_withZeroSize() {
        val rect1 = Rect(Offset.Zero, Size.Zero)
        val rect2 = Rect(Offset(4f, 0f), Size.Zero)

        assertThat(Offset(1.9f, 0f).findClosestRect(rect1, rect2)).isEqualTo(-1)
        assertThat(Offset(2f, 0f).findClosestRect(rect1, rect2)).isEqualTo(0)
        assertThat(Offset(2.1f, 0f).findClosestRect(rect1, rect2)).isEqualTo(1)
    }

    @Test
    fun findClosestRect_findsClosestRect_withZeroWidth() {
        val rect1 = Rect(Offset.Zero, Size(0f, 10f))
        val rect2 = Rect(Offset(4f, 0f), Size(0f, 10f))

        assertThat(Offset(1.9f, 0f).findClosestRect(rect1, rect2)).isEqualTo(-1)
        assertThat(Offset(2f, 0f).findClosestRect(rect1, rect2)).isEqualTo(0)
        assertThat(Offset(2.1f, 0f).findClosestRect(rect1, rect2)).isEqualTo(1)
    }

    @Test
    fun findClosestRect_findsClosestRect_withZeroWidth_differentY() {
        val rect1 = Rect(Offset(0f, -10f), Size(0f, 9f))
        val rect2 = Rect(Offset(4f, 1f), Size(0f, 9f))

        assertThat(Offset(2f, -1f).findClosestRect(rect1, rect2)).isEqualTo(-1)
        assertThat(Offset(2f, 0f).findClosestRect(rect1, rect2)).isEqualTo(0)
        assertThat(Offset(2f, 1f).findClosestRect(rect1, rect2)).isEqualTo(1)
    }
}
