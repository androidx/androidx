/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.lifecycle

import androidx.kruth.assertThat
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.contains
import androidx.lifecycle.viewmodel.plus
import androidx.lifecycle.viewmodel.plusAssign
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Test
import org.junit.runner.RunWith

private val STRING_KEY_1 = CreationExtras.Key<String>()
private val STRING_KEY_2 = CreationExtras.Key<String>()

@RunWith(AndroidJUnit4::class)
@SmallTest
class CreationExtrasTest {

    @Test
    fun keyFactory_returnsDistinctInstances() {
        val key1 = CreationExtras.Key<String>()
        val key2 = CreationExtras.Key<String>()

        assertThat(key1).isNotEqualTo(key2)
    }

    @Test
    fun initialExtras_originalModifiedAfterCopy_copyRemainsUnchanged() {
        val otherExtras = MutableCreationExtras().apply { this[STRING_KEY_1] = "value1" }
        val underTest = MutableCreationExtras(initialExtras = otherExtras)
        otherExtras[STRING_KEY_1] = "value2"

        assertThat(otherExtras[STRING_KEY_1]).isEqualTo("value2")
        assertThat(underTest[STRING_KEY_1]).isEqualTo("value1")
    }

    @Test
    fun equals_sameValues_isEqual() {
        val underTest = MutableCreationExtras().apply { this[STRING_KEY_1] = "value1" }
        val otherExtras = MutableCreationExtras().apply { this[STRING_KEY_1] = "value1" }

        assertThat(underTest).isEqualTo(otherExtras)
    }

    @Test
    fun equals_differentValues_isNotEqual() {
        val underTest = MutableCreationExtras().apply { this[STRING_KEY_1] = "value1" }
        val otherExtras = MutableCreationExtras().apply { this[STRING_KEY_1] = "value2" }

        assertThat(underTest).isNotEqualTo(otherExtras)
    }

    @Test
    fun contains_returnsTrueForExistingKey() {
        val underTest = MutableCreationExtras().apply { this[STRING_KEY_1] = "value1" }

        val result = STRING_KEY_1 in underTest

        assertThat(result).isTrue()
    }

    @Test
    fun contains_returnsFalseForNonExistingKey() {
        val underTest = MutableCreationExtras().apply { this[STRING_KEY_1] = "value1" }

        val result = STRING_KEY_2 in underTest

        assertThat(result).isFalse()
    }

    @Test
    fun plus_addedTogetherWithUniqueKeys_combinesValues() {
        val extras1 = MutableCreationExtras().apply { this[STRING_KEY_1] = "value1" }
        val extras2 = MutableCreationExtras().apply { this[STRING_KEY_2] = "value2" }

        val underTest = extras1 + extras2

        assertThat(underTest[STRING_KEY_1]).isEqualTo(extras1[STRING_KEY_1])
        assertThat(underTest[STRING_KEY_2]).isEqualTo(extras2[STRING_KEY_2])
    }

    @Test
    fun plus_addedTogetherWithConflictingKeys_overridesFirstValue() {
        val extras1 = MutableCreationExtras().apply { this[STRING_KEY_1] = "value1" }
        val extras2 = MutableCreationExtras().apply { this[STRING_KEY_1] = "value2" }

        val underTest = extras1 + extras2

        assertThat(underTest[STRING_KEY_1]).isEqualTo(extras2[STRING_KEY_1])
    }

    @Test
    fun plusAssign_addedTogetherWithUniqueKeys_combinesValues() {
        val underTest = MutableCreationExtras().apply { this[STRING_KEY_1] = "value1" }
        val otherExtras = MutableCreationExtras().apply { this[STRING_KEY_2] = "value2" }

        underTest += otherExtras

        assertThat(underTest[STRING_KEY_1]).isEqualTo(underTest[STRING_KEY_1])
        assertThat(underTest[STRING_KEY_2]).isEqualTo(otherExtras[STRING_KEY_2])
    }

    @Test
    fun plusAssign_addedTogetherWithConflictingKeys_overridesFirstValue() {
        val underTest = MutableCreationExtras().apply { this[STRING_KEY_1] = "value1" }
        val otherExtras = MutableCreationExtras().apply { this[STRING_KEY_1] = "value2" }

        underTest += otherExtras

        assertThat(underTest[STRING_KEY_1]).isEqualTo(otherExtras[STRING_KEY_1])
    }
}
