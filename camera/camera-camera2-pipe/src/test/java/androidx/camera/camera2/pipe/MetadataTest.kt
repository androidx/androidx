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

package androidx.camera.camera2.pipe

import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MetadataTest {

    @Test
    fun keysWithSameNameAreSameInstance() {
        val key1 = Metadata.Key.create<String>("metadata.test.key")
        val key2 = Metadata.Key.create<String>("metadata.test.key")
        assertThat(key1).isSameInstanceAs(key2)
    }

    @Test
    fun keysWithDifferentNamesAreNotSameInstance() {
        val key1 = Metadata.Key.create<String>("metadata.test.key1")
        val key2 = Metadata.Key.create<String>("metadata.test.key2")
        assertThat(key1).isNotSameInstanceAs(key2)
    }

    @Test
    fun keysWithSameNameAndDifferentTypesThrowsExceptions() {
        val key1 = Metadata.Key.create<String>("metadata.test.key")
        assertThrows<IllegalStateException> {
            val key2 = Metadata.Key.create<Int>("metadata.test.key")
        }
    }
}
