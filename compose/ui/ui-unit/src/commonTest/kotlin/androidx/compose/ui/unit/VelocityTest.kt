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

package androidx.compose.ui.unit

import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertEquals

class VelocityTest {
    private val velocity1 = Velocity(3f, -7f)
    private val velocity2 = Velocity(5f, 13f)

    @Test
    fun operatorUnaryMinus() {
        assertThat(-velocity1).isEqualTo(Velocity(-3f, 7f))
        assertThat(-velocity2).isEqualTo(Velocity(-5f, -13f))
    }

    @Test
    fun operatorPlus() {
        assertThat(velocity2 + velocity1).isEqualTo(Velocity(8f, 6f))
        assertThat(velocity1 + velocity2).isEqualTo(Velocity(8f, 6f))
    }

    @Test
    fun operatorMinus() {
        assertThat(velocity1 - velocity2).isEqualTo(Velocity(-2f, -20f))
        assertThat(velocity2 - velocity1).isEqualTo(Velocity(2f, 20f))
    }

    @Test
    fun operatorDivide() {
        assertThat(velocity1 / 10f).isEqualTo(Velocity(0.3f, -0.7f))
    }

    @Test
    fun operatorTimes() {
        assertThat(velocity1 * 10f).isEqualTo(Velocity(30f, -70f))
    }

    @Test
    fun operatorRem() {
        assertThat(velocity1 % 3f).isEqualTo(Velocity(0f, -1f))
    }

    @Test
    fun components() {
        val (x, y) = velocity1
        assertThat(x).isEqualTo(3f)
        assertThat(y).isEqualTo(-7f)
    }

    @Test
    fun xy() {
        assertThat(velocity1.x).isEqualTo(3f)
        assertThat(velocity1.y).isEqualTo(-7f)
    }

    @Test
    fun testOffsetCopy() {
        val offset = Velocity(100f, 200f)
        assertEquals(offset, offset.copy())
    }

    @Test
    fun testOffsetCopyOverwriteX() {
        val offset = Velocity(100f, 200f)
        val copy = offset.copy(x = 50f)
        assertEquals(50f, copy.x)
        assertEquals(200f, copy.y)
    }

    @Test
    fun testOffsetCopyOverwriteY() {
        val offset = Velocity(100f, 200f)
        val copy = offset.copy(y = 300f)
        assertEquals(100f, copy.x)
        assertEquals(300f, copy.y)
    }
}
