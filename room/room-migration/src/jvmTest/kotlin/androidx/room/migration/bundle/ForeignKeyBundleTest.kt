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

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ForeignKeyBundleTest {
    @Test
    fun schemaEquality_same_equal() {
        val bundle = ForeignKeyBundle("table", "onDelete",
                "onUpdate", listOf("col1", "col2"),
                listOf("target1", "target2")
        )
        val other = ForeignKeyBundle("table", "onDelete",
                "onUpdate", listOf("col1", "col2"),
            listOf("target1", "target2"))
        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }

    @Test
    fun schemaEquality_diffTable_notEqual() {
        val bundle = ForeignKeyBundle("table", "onDelete",
                "onUpdate", listOf("col1", "col2"),
            listOf("target1", "target2"))
        val other = ForeignKeyBundle("table2", "onDelete",
                "onUpdate", listOf("col1", "col2"),
            listOf("target1", "target2"))
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_diffOnDelete_notEqual() {
        val bundle = ForeignKeyBundle("table", "onDelete2",
                "onUpdate", listOf("col1", "col2"),
            listOf("target1", "target2"))
        val other = ForeignKeyBundle("table", "onDelete",
                "onUpdate", listOf("col1", "col2"),
            listOf("target1", "target2"))
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_diffOnUpdate_notEqual() {
        val bundle = ForeignKeyBundle("table", "onDelete",
                "onUpdate", listOf("col1", "col2"),
            listOf("target1", "target2"))
        val other = ForeignKeyBundle("table", "onDelete",
                "onUpdate2", listOf("col1", "col2"),
            listOf("target1", "target2"))
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_diffSrcOrder_notEqual() {
        val bundle = ForeignKeyBundle("table", "onDelete",
                "onUpdate", listOf("col2", "col1"),
            listOf("target1", "target2"))
        val other = ForeignKeyBundle("table", "onDelete",
                "onUpdate", listOf("col1", "col2"),
            listOf("target1", "target2"))
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_diffTargetOrder_notEqual() {
        val bundle = ForeignKeyBundle("table", "onDelete",
                "onUpdate", listOf("col1", "col2"),
            listOf("target1", "target2"))
        val other = ForeignKeyBundle("table", "onDelete",
                "onUpdate", listOf("col1", "col2"),
            listOf("target2", "target1"))
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }
}
