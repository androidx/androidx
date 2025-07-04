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

package androidx.xr.runtime

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AugmentedObjectCategoryTest {

    @Test
    fun equals_sameInstance_returnsTrue() {
        val category = AugmentedObjectCategory.UNKNOWN

        assertThat(category).isEqualTo(category)
    }

    @Test
    fun equals_differentInstance_returnsFalse() {
        val category1 = AugmentedObjectCategory.LAPTOP
        val category2 = AugmentedObjectCategory.MOUSE

        assertThat(category1).isNotEqualTo(category2)
    }

    @Test
    fun toString_returnsCorrectValues() {
        assertThat(AugmentedObjectCategory.LAPTOP.toString()).isEqualTo("Laptop")
        assertThat(AugmentedObjectCategory.MOUSE.toString()).isEqualTo("Mouse")
        assertThat(AugmentedObjectCategory.KEYBOARD.toString()).isEqualTo("Keyboard")
    }
}
