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
package androidx.glance.layout

import androidx.glance.Modifier
import androidx.glance.findModifier
import androidx.glance.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaddingTest {
    @Test
    fun buildPadding() {
        val modifiers = Modifier.padding(
            start = 1.dp,
            top = 2.dp,
            end = 3.dp,
            bottom = 4.dp
        )

        // Find the padding modifier...
        val paddingModifier = checkNotNull(modifiers.findModifier<PaddingModifier>())

        assertThat(paddingModifier.rtlAware).isTrue()
        assertThat(paddingModifier.start).isEqualTo(1.dp)
        assertThat(paddingModifier.top).isEqualTo(2.dp)
        assertThat(paddingModifier.end).isEqualTo(3.dp)
        assertThat(paddingModifier.bottom).isEqualTo(4.dp)
    }

    @Test
    fun buildVerticalHorizontalPadding() {
        val modifiers = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)

        val paddingModifier = checkNotNull(modifiers.findModifier<PaddingModifier>())

        assertThat(paddingModifier.start).isEqualTo(4.dp)
        assertThat(paddingModifier.top).isEqualTo(2.dp)
        assertThat(paddingModifier.end).isEqualTo(4.dp)
        assertThat(paddingModifier.bottom).isEqualTo(2.dp)
    }

    @Test
    fun buildAllPadding() {
        val modifiers = Modifier.padding(all = 5.dp)

        val paddingModifier = checkNotNull(modifiers.findModifier<PaddingModifier>())

        assertThat(paddingModifier.start).isEqualTo(5.dp)
        assertThat(paddingModifier.top).isEqualTo(5.dp)
        assertThat(paddingModifier.end).isEqualTo(5.dp)
        assertThat(paddingModifier.bottom).isEqualTo(5.dp)
    }

    @Test
    fun buildAbsolutePadding() {
        val modifiers = Modifier.absolutePadding(
            left = 1.dp,
            top = 2.dp,
            right = 3.dp,
            bottom = 4.dp
        )

        val paddingModifier = checkNotNull(modifiers.findModifier<PaddingModifier>())

        assertThat(paddingModifier.rtlAware).isFalse()
        assertThat(paddingModifier.start).isEqualTo(1.dp)
        assertThat(paddingModifier.top).isEqualTo(2.dp)
        assertThat(paddingModifier.end).isEqualTo(3.dp)
        assertThat(paddingModifier.bottom).isEqualTo(4.dp)
    }
}
