/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.room.migration.bundle

import androidx.kruth.assertThat
import kotlin.test.Test

class PrimaryKeyBundleTest {
    @Test
    fun schemaEquality_same_equal() {
        val bundle = PrimaryKeyBundle(isAutoGenerate = true, columnNames = listOf("foo", "bar"))
        val other = PrimaryKeyBundle(isAutoGenerate = true, columnNames = listOf("foo", "bar"))
        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }

    @Test
    fun schemaEquality_diffAutoGen_notEqual() {
        val bundle = PrimaryKeyBundle(isAutoGenerate = true, columnNames = listOf("foo", "bar"))
        val other = PrimaryKeyBundle(isAutoGenerate = false, columnNames = listOf("foo", "bar"))
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_diffColumns_notEqual() {
        val bundle = PrimaryKeyBundle(isAutoGenerate = true, columnNames = listOf("foo", "baz"))
        val other = PrimaryKeyBundle(isAutoGenerate = true, columnNames = listOf("foo", "bar"))
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_diffColumnOrder_notEqual() {
        val bundle = PrimaryKeyBundle(isAutoGenerate = true, columnNames = listOf("foo", "bar"))
        val other = PrimaryKeyBundle(isAutoGenerate = true, columnNames = listOf("bar", "foo"))
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }
}
