/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.remote.core.documentation

import androidx.compose.remote.generator.JsonSchemaParser
import androidx.compose.remote.generator.OperationSchema
import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Test

class SchemaSynchronizationTest {
    private fun findSchemaSourceFile(): File? {
        val candidates =
            listOf(
                File("compose/remote/remote-generator/src/main/resources/schema/operations.json"),
                File("../remote-generator/src/main/resources/schema/operations.json"),
                File(
                    "../../compose/remote/remote-generator/src/main/resources/schema/operations.json"
                ),
                File("src/main/resources/schema/operations.json"),
            )
        return candidates.firstOrNull { it.exists() }
    }

    private fun loadSchema(schemaFile: File): OperationSchema {
        return JsonSchemaParser.parse(schemaFile.inputStream())
    }

    @Test
    fun testSchemaSynchronization() {
        val schemaFile = findSchemaSourceFile()
        assertNotNull("Failed to locate operations.json", schemaFile)
        val schema = loadSchema(schemaFile!!)
        val synchronizer = SchemaSynchronizer(schemaFile)
        val discrepancies = synchronizer.findDiscrepancies(schema)

        if (discrepancies.isNotEmpty()) {
            val shouldUpdate =
                System.getProperty("updateRemoteComposeSchema") == "true" ||
                    System.getenv("UPDATE_REMOTE_COMPOSE_SCHEMA") == "true"

            if (shouldUpdate) {
                val updatedCount = synchronizer.updateSchemaFile()
                println(
                    "[SUCCESS] Automatically updated ${schemaFile.path} with $updatedCount new/updated operations."
                )
                return
            }

            val errorMessage = buildString {
                appendLine()
                appendLine(
                    "================================================================================"
                )
                appendLine("RemoteCompose Schema Synchronization Mismatch!")
                appendLine(
                    "================================================================================"
                )
                appendLine(
                    "The following operations are registered in Java but missing or mismatched in operations.json:"
                )
                discrepancies.forEach { appendLine("  - $it") }
                appendLine()
                appendLine(
                    "To automatically update operations.json from Java operation definitions, run:"
                )
                appendLine(
                    "  ./gradlew :compose:remote:remote-core:test -PupdateRemoteComposeSchema"
                )
                appendLine(
                    "================================================================================"
                )
            }
            throw AssertionError(errorMessage)
        }
    }
}
