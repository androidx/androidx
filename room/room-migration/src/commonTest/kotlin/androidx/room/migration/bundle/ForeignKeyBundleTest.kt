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

class ForeignKeyBundleTest {
    @Test
    fun schemaEquality_same_equal() {
        val bundle =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete",
                onUpdate = "onUpdate",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target1", "target2")
            )
        val other =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete",
                onUpdate = "onUpdate",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target1", "target2")
            )

        assertThat(bundle.isSchemaEqual(other)).isTrue()
    }

    @Test
    fun schemaEquality_diffTable_notEqual() {
        val bundle =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete",
                onUpdate = "onUpdate",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target1", "target2")
            )
        val other =
            ForeignKeyBundle(
                table = "table2",
                onDelete = "onDelete",
                onUpdate = "onUpdate",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target1", "target2")
            )
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_diffOnDelete_notEqual() {
        val bundle =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete2",
                onUpdate = "onUpdate",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target1", "target2")
            )
        val other =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete",
                onUpdate = "onUpdate",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target1", "target2")
            )
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_diffOnUpdate_notEqual() {
        val bundle =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete",
                onUpdate = "onUpdate",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target1", "target2")
            )
        val other =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete",
                onUpdate = "onUpdate2",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target1", "target2")
            )
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_diffSrcOrder_notEqual() {
        val bundle =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete",
                onUpdate = "onUpdate",
                columns = listOf("col2", "col1"),
                referencedColumns = listOf("target1", "target2")
            )
        val other =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete",
                onUpdate = "onUpdate",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target1", "target2")
            )
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }

    @Test
    fun schemaEquality_diffTargetOrder_notEqual() {
        val bundle =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete",
                onUpdate = "onUpdate",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target1", "target2")
            )
        val other =
            ForeignKeyBundle(
                table = "table",
                onDelete = "onDelete",
                onUpdate = "onUpdate",
                columns = listOf("col1", "col2"),
                referencedColumns = listOf("target2", "target1")
            )
        assertThat(bundle.isSchemaEqual(other)).isFalse()
    }
}
