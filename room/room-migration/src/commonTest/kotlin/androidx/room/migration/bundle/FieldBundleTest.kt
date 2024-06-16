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

class FieldBundleTest {
    @Test
    fun schemaEquality_same_equal() {
        val bundle =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo",
                affinity = "text",
                isNonNull = false,
                defaultValue = null
            )
        val copy =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo",
                affinity = "text",
                isNonNull = false,
                defaultValue = null
            )
        assertThat(bundle.isSchemaEqual(copy)).isTrue()
    }

    @Test
    fun schemaEquality_diffNonNull_notEqual() {
        val bundle =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo",
                affinity = "text",
                isNonNull = false,
                defaultValue = null
            )
        val copy =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo",
                affinity = "text",
                isNonNull = true,
                defaultValue = null
            )
        assertThat(bundle.isSchemaEqual(copy)).isFalse()
    }

    @Test
    fun schemaEquality_diffColumnName_notEqual() {
        val bundle =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo",
                affinity = "text",
                isNonNull = false,
                defaultValue = null
            )
        val copy =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo2",
                affinity = "text",
                isNonNull = true,
                defaultValue = null
            )
        assertThat(bundle.isSchemaEqual(copy)).isFalse()
    }

    @Test
    fun schemaEquality_diffAffinity_notEqual() {
        val bundle =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo",
                affinity = "text",
                isNonNull = false,
                defaultValue = null
            )
        val copy =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo2",
                affinity = "int",
                isNonNull = false,
                defaultValue = null
            )
        assertThat(bundle.isSchemaEqual(copy)).isFalse()
    }

    @Test
    fun schemaEquality_diffPath_equal() {
        val bundle =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo",
                affinity = "text",
                isNonNull = false,
                defaultValue = null
            )
        val copy =
            FieldBundle(
                fieldPath = "foo>bar",
                columnName = "foo",
                affinity = "text",
                isNonNull = false,
                defaultValue = null
            )
        assertThat(bundle.isSchemaEqual(copy)).isTrue()
    }

    @Test
    fun schemeEquality_diffDefaultValue_notEqual() {
        val bundle =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo",
                affinity = "text",
                isNonNull = true,
                defaultValue = null
            )
        val copy =
            FieldBundle(
                fieldPath = "foo",
                columnName = "foo",
                affinity = "text",
                isNonNull = true,
                defaultValue = "bar"
            )
        assertThat(bundle.isSchemaEqual(copy)).isFalse()
    }
}
