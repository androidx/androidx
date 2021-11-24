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
class FieldBundleTest {
    @Test
    fun schemaEquality_same_equal() {
        val bundle = FieldBundle("foo", "foo", "text", false, null)
        val copy = FieldBundle("foo", "foo", "text", false, null)
        assertThat(bundle.isSchemaEqual(copy), `is`(true))
    }

    @Test
    fun schemaEquality_diffNonNull_notEqual() {
        val bundle = FieldBundle("foo", "foo", "text", false, null)
        val copy = FieldBundle("foo", "foo", "text", true, null)
        assertThat(bundle.isSchemaEqual(copy), `is`(false))
    }

    @Test
    fun schemaEquality_diffColumnName_notEqual() {
        val bundle = FieldBundle("foo", "foo", "text", false, null)
        val copy = FieldBundle("foo", "foo2", "text", true, null)
        assertThat(bundle.isSchemaEqual(copy), `is`(false))
    }

    @Test
    fun schemaEquality_diffAffinity_notEqual() {
        val bundle = FieldBundle("foo", "foo", "text", false, null)
        val copy = FieldBundle("foo", "foo2", "int", false, null)
        assertThat(bundle.isSchemaEqual(copy), `is`(false))
    }

    @Test
    fun schemaEquality_diffPath_equal() {
        val bundle = FieldBundle("foo", "foo", "text", false, null)
        val copy = FieldBundle("foo>bar", "foo", "text", false, null)
        assertThat(bundle.isSchemaEqual(copy), `is`(true))
    }

    @Test
    fun schemeEquality_diffDefaultValue_notEqual() {
        val bundle = FieldBundle("foo", "foo", "text", true, null)
        val copy = FieldBundle("foo", "foo", "text", true, "bar")
        assertThat(bundle.isSchemaEqual(copy), `is`(false))
    }
}
