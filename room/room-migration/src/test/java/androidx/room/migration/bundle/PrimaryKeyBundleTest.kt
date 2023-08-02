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
class PrimaryKeyBundleTest {
    @Test
    fun schemaEquality_same_equal() {
        val bundle = PrimaryKeyBundle(true,
                listOf("foo", "bar")
        )
        val other = PrimaryKeyBundle(true,
            listOf("foo", "bar"))
        assertThat(bundle.isSchemaEqual(other), `is`(true))
    }

    @Test
    fun schemaEquality_diffAutoGen_notEqual() {
        val bundle = PrimaryKeyBundle(true,
            listOf("foo", "bar"))
        val other = PrimaryKeyBundle(false,
            listOf("foo", "bar"))
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

    @Test
    fun schemaEquality_diffColumns_notEqual() {
        val bundle = PrimaryKeyBundle(true,
            listOf("foo", "baz"))
        val other = PrimaryKeyBundle(true,
            listOf("foo", "bar"))
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }

   @Test
   fun schemaEquality_diffColumnOrder_notEqual() {
        val bundle = PrimaryKeyBundle(true,
            listOf("foo", "bar"))
        val other = PrimaryKeyBundle(true,
            listOf("bar", "foo"))
        assertThat(bundle.isSchemaEqual(other), `is`(false))
    }
}
