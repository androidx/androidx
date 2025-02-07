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

package androidx.appfunctions.metadata

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppFunctionSchemaMetadataTest {

    @Test
    fun appFunctionSchemaMetadata_equalsAndHashCode() {
        val schema1 =
            AppFunctionSchemaMetadata(category = "testCategory", name = "testName", version = 1L)
        val schema2 =
            AppFunctionSchemaMetadata(category = "testCategory", name = "testName", version = 1L)
        val schema3 =
            AppFunctionSchemaMetadata(
                category = "testCategory2", // Different category
                name = "testName",
                version = 1L
            )
        val schema4 =
            AppFunctionSchemaMetadata(
                category = "testCategory",
                name = "testName2", // Different name
                version = 1L
            )
        val schema5 =
            AppFunctionSchemaMetadata(
                category = "testCategory",
                name = "testName",
                version = 2L // Different version
            )

        assertThat(schema1).isEqualTo(schema2)
        assertThat(schema1.hashCode()).isEqualTo(schema2.hashCode())

        assertThat(schema1).isNotEqualTo(schema3)
        assertThat(schema1.hashCode()).isNotEqualTo(schema3.hashCode())

        assertThat(schema1).isNotEqualTo(schema4)
        assertThat(schema1.hashCode()).isNotEqualTo(schema4.hashCode())

        assertThat(schema1).isNotEqualTo(schema5)
        assertThat(schema1.hashCode()).isNotEqualTo(schema5.hashCode())
    }

    @Test
    fun appFunctionSchemaMetadata_toAppFunctionSchemaMetadataDocument_returnsCorrectDocument() {
        val schemaMetadata =
            AppFunctionSchemaMetadata(category = "testCategory", name = "testName", version = 1L)
        val expectedSchemaMetadataDocument =
            AppFunctionSchemaMetadataDocument(
                schemaCategory = schemaMetadata.category,
                schemaVersion = schemaMetadata.version,
                schemaName = schemaMetadata.name
            )

        val schemaMetadataDocument = schemaMetadata.toAppFunctionSchemaMetadataDocument()

        assertThat(schemaMetadataDocument).isEqualTo(expectedSchemaMetadataDocument)
    }
}
