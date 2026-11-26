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

package androidx.compose.remote.generator

import java.nio.file.Files
import java.nio.file.Paths
import org.json.JSONObject

/**
 * Internal entry point invoked by `:compose:remote:remote-generator:updateGeneratedCode` to
 * validate `operations.json`.
 *
 * Kotlin protocol, AST, and writer sources are generated at build time by
 * [RemoteKotlinCodegenMain].
 */
internal object AndroidXCodegenUpdater {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.isNotEmpty()) { "Expected path to compose/remote directory as first argument" }
        val remoteDir = Paths.get(args[0]).toAbsolutePath().normalize()
        require(Files.isDirectory(remoteDir)) { "compose/remote directory not found: $remoteDir" }

        val schemaText =
            checkNotNull(
                    AndroidXCodegenUpdater::class
                        .java
                        .getResourceAsStream("/schema/operations.json")
                ) {
                    "Missing bundled /schema/operations.json resource"
                }
                .bufferedReader()
                .use { it.readText() }

        val errors = JsonSchemaParser.validate(JSONObject(schemaText))
        check(errors.isEmpty()) {
            "operations.json failed validation:\n${errors.joinToString("\n")}"
        }
        println("Validated operations.json ($remoteDir)")
    }
}
