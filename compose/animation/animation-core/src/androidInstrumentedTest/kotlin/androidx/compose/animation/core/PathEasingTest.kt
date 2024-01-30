/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.animation.core

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PathEasingTest {
    @Test
    fun pathEasing_Emphasized_BoundsCheck() {
        val path = Path()
        path.moveTo(0f, 0f)
        path.cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
        path.cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)

        val easing = PathEasing(path)
        assertThat(easing.transform(0f)).isZero()
        assertThat(easing.transform(1f)).isEqualTo(1f)

        assertEquals(0.77283f, easing.transform(0.25f), 0.0001f)
        assertEquals(0.95061f, easing.transform(0.5f), 0.0001f)
        assertEquals(0.99139f, easing.transform(0.75f), 0.0001f)
    }

    @Test
    fun pathEasing_CheckIncreasingXOverTime() {
        val path = Path()
        path.moveTo(0f, 0f)
        path.quadraticBezierTo(0f, 1.65f, 1f, -0.6f)

        val easing = PathEasing(path)
        assertThat(easing.transform(0f)).isZero()
        assertThat(easing.transform(1f)).isEqualTo(1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun pathEasing_CheckIncreasingXOverTime_InvalidPath() {
        val path = Path()
        path.addOval(Rect(0f, 0f, 1f, 1f))

        PathEasing(path)
    }

    @Test(expected = IllegalArgumentException::class)
    fun pathEasing_NoPathProvided_ThrowsIllegalArgument() {
        val emptyPath = Path()
        PathEasing(emptyPath)
    }
}
