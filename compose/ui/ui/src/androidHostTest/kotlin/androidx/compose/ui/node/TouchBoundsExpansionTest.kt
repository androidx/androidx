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

package androidx.compose.ui.node

import com.google.common.truth.Truth.assertThat
import kotlin.test.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TouchBoundsExpansionTest {
    @Test
    fun create() {
        val touchBoundsExpansion = TouchBoundsExpansion(1, 2, 3, 4)

        assertThat(touchBoundsExpansion.start).isEqualTo(1)
        assertThat(touchBoundsExpansion.top).isEqualTo(2)
        assertThat(touchBoundsExpansion.end).isEqualTo(3)
        assertThat(touchBoundsExpansion.bottom).isEqualTo(4)
        assertThat(touchBoundsExpansion.isLayoutDirectionAware).isTrue()
    }

    @Test
    fun create_absolute() {
        val touchBoundsExpansion = TouchBoundsExpansion.Absolute(1, 2, 3, 4)

        assertThat(touchBoundsExpansion.start).isEqualTo(1)
        assertThat(touchBoundsExpansion.top).isEqualTo(2)
        assertThat(touchBoundsExpansion.end).isEqualTo(3)
        assertThat(touchBoundsExpansion.bottom).isEqualTo(4)
        assertThat(touchBoundsExpansion.isLayoutDirectionAware).isFalse()
    }

    @Test
    fun throwIllegalArgumentException_whenValueIsNegative() {
        try {
            TouchBoundsExpansion(start = -1)
            fail("Expect IllegalArgumentException when start is negative")
        } catch (_: IllegalArgumentException) {}

        try {
            TouchBoundsExpansion(top = -1)
            fail("Expect IllegalArgumentException when top is negative")
        } catch (_: IllegalArgumentException) {}

        try {
            TouchBoundsExpansion(end = -1)
            fail("Expect IllegalArgumentException when end is negative")
        } catch (_: IllegalArgumentException) {}

        try {
            TouchBoundsExpansion(bottom = -1)
            fail("Expect IllegalArgumentException when bottom is negative")
        } catch (_: IllegalArgumentException) {}
    }

    @Test
    fun throwIllegalArgumentException_whenValueExceedsMax() {
        assertThat(TouchBoundsExpansion(start = 32767).start).isEqualTo(32767)
        try {
            TouchBoundsExpansion(start = 32768)
            fail("Expect IllegalArgumentException when start is too large")
        } catch (_: IllegalArgumentException) {}

        assertThat(TouchBoundsExpansion(top = 32767).top).isEqualTo(32767)
        try {
            TouchBoundsExpansion(top = 32768)
            fail("Expect IllegalArgumentException when top is too large")
        } catch (_: IllegalArgumentException) {}

        assertThat(TouchBoundsExpansion(end = 32767).end).isEqualTo(32767)
        try {
            TouchBoundsExpansion(end = 32768)
            fail("Expect IllegalArgumentException when end is too large")
        } catch (_: IllegalArgumentException) {}

        assertThat(TouchBoundsExpansion(bottom = 32767).bottom).isEqualTo(32767)
        try {
            TouchBoundsExpansion(bottom = 32768)
            fail("Expect IllegalArgumentException when bottom is too large")
        } catch (_: IllegalArgumentException) {}
    }
}
