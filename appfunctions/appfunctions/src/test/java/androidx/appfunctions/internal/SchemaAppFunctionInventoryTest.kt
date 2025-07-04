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

package androidx.appfunctions.internal

import androidx.appfunctions.metadata.AppFunctionComponentsMetadata
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata
import androidx.appfunctions.metadata.AppFunctionResponseMetadata
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata
import androidx.appfunctions.metadata.CompileTimeAppFunctionMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SchemaAppFunctionInventoryTest {

    @Test
    fun testEmpty() {
        val schemaMetadata =
            AppFunctionSchemaMetadata(category = "note", name = "createNote", version = 1L)
        val schemaInventory =
            object : SchemaAppFunctionInventory() {
                override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata>
                    get() = emptyMap()
            }

        assertThat(schemaInventory.schemaFunctionsMap[schemaMetadata]).isNull()
    }

    @Test
    fun testNonEmpty_schemaMetadataExist() {
        val schemaMetadata =
            AppFunctionSchemaMetadata(category = "note", name = "createNote", version = 1L)
        val functionMetadata =
            CompileTimeAppFunctionMetadata(
                id = "createNote",
                isEnabledByDefault = false,
                schema = schemaMetadata,
                parameters = listOf(),
                response =
                    AppFunctionResponseMetadata(
                        AppFunctionReferenceTypeMetadata("test", isNullable = false)
                    ),
                components = AppFunctionComponentsMetadata(emptyMap()),
                description = "Creates a new note.",
            )
        val schemaInventory =
            object : SchemaAppFunctionInventory() {
                override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata>
                    get() = mapOf("createNote" to functionMetadata)
            }

        assertThat(schemaInventory.schemaFunctionsMap[schemaMetadata]).isEqualTo(functionMetadata)
    }

    @Test
    fun testNonEmpty_schemaMetadataNotExist() {
        val schemaMetadata =
            AppFunctionSchemaMetadata(category = "note", name = "createNote", version = 1L)
        val functionMetadata =
            CompileTimeAppFunctionMetadata(
                id = "createNote",
                isEnabledByDefault = false,
                schema = schemaMetadata,
                parameters = listOf(),
                response =
                    AppFunctionResponseMetadata(
                        AppFunctionReferenceTypeMetadata("test", isNullable = false)
                    ),
                components = AppFunctionComponentsMetadata(emptyMap()),
                description = "Creates a new note.",
            )
        val schemaInventory =
            object : SchemaAppFunctionInventory() {
                override val functionIdToMetadataMap: Map<String, CompileTimeAppFunctionMetadata>
                    get() = mapOf("createNote" to functionMetadata)
            }

        assertThat(
                schemaInventory.schemaFunctionsMap[
                        AppFunctionSchemaMetadata("note", "updateNote", 1L)]
            )
            .isNull()
    }
}
