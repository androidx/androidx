/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.kruth.assertThrows
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.test.Test
import kotlinx.serialization.SerializationException

class SerializationTest {

    @Test
    fun emptyStream() {
        // GSON had a specific exception for an empty file, but with Kotlin serialization it is
        // as any other serialization exception. We have a test for this since we expect the
        // exception to properly report an error in the annotation processor.
        assertThrows<SerializationException> {
            SchemaBundle.deserialize(ByteArrayInputStream(byteArrayOf()))
        }
    }

    @Test
    fun fileNotFound() {
        // This is mostly validating File streams throwing FileNotFoundException, but we expect
        // such exception when the schema file does not exist to properly report an error
        // in the annotation processor.
        assertThrows<FileNotFoundException> {
            FileInputStream("/fake/file/path").use { SchemaBundle.deserialize(it) }
        }
    }

    /**
     * Validates old schemas that didn't have [FieldBundle.isNonNull] ('notNull'), added in
     * ag/2579620
     */
    @Test
    fun missingFieldBundleNotNull() {
        getSchemaPath("missing_field_notnull").inputStream().use { SchemaBundle.deserialize(it) }
    }

    /**
     * Validates old schemas that didn't have [DatabaseBundle.views] ('views'), added in aosp/731045
     */
    @Test
    fun missingDatabaseBundleViews() {
        getSchemaPath("missing_database_views").inputStream().use { SchemaBundle.deserialize(it) }
    }

    /**
     * Validates old schemas that didn't have [FieldBundle.defaultValue] ('defaultValue'), added in
     * aosp/825803
     */
    @Test
    fun missingFieldBundleDefaultValue() {
        getSchemaPath("missing_field_defaultvalue").inputStream().use {
            SchemaBundle.deserialize(it)
        }
    }

    /**
     * Validates old schemas that didn't have [IndexBundle.orders] ('orders'), added in aosp/1707963
     */
    @Test
    fun missingIndexBundleOrders() {
        getSchemaPath("missing_index_orders").inputStream().use { SchemaBundle.deserialize(it) }
    }

    private fun getSchemaPath(name: String): Path {
        return Path("src/jvmTest/test-data/$name.json")
    }
}
