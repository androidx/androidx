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
class IndexBundleTest {
    @Test
    fun schemaEquality_same_equal() {
        val bundle = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        val other = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }

    @Test
    fun schemaEquality_diffName_notEqual() {
        val bundle = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        val other = IndexBundle("index3", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_diffGenericName_equal() {
        val bundle = IndexBundle(IndexBundle.DEFAULT_PREFIX + "x", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        val other = IndexBundle(IndexBundle.DEFAULT_PREFIX + "y", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }

    @Test
    fun schemaEquality_diffUnique_notEqual() {
        val bundle = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        val other = IndexBundle("index1", true,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_diffColumns_notEqual() {
        val bundle = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        val other = IndexBundle("index1", false,
            listOf("col2", "col1"), listOf("ASC", "ASC"), "sql")
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_diffSql_equal() {
        val bundle = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        val other = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql22")
        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }

    @Test
    fun schemaEquality_diffSort_notEqual() {
        val bundle = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("ASC", "DESC"), "sql")
        val other = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("DESC", "ASC"), "sql")
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_sortNullVsAllAsc_isEqual() {
        val bundle = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        val other = IndexBundle("index1", false,
            listOf("col1", "col2"), null, "sql")
        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }

    @Test
    fun schemaEquality_sortEmptyVsAllAsc_isEqual() {
        val bundle = IndexBundle("index1", false,
            listOf("col1", "col2"), listOf("ASC", "ASC"), "sql")
        val other = IndexBundle("index1", false,
            listOf("col1", "col2"), emptyList(), "sql")
        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }
}
