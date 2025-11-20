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
package androidx.abbenchmarking

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// Data classes for metadata serialization
@Serializable
internal data class Metadata(
    val executionTimestamp: String,
    val revA: RevInfo,
    val revB: RevInfo,
    val deviceInfo: DeviceInfo? = null,
    val inputParameters: Map<String, String>,
)

@Serializable internal data class RevInfo(val name: String, val commit: String? = null)

// Data classes for parsing benchmark output JSON
@Serializable internal data class BenchmarkData(val context: Context)

@Serializable internal data class Context(val build: DeviceInfo)

@Serializable
internal data class DeviceInfo(val device: String, val model: String, val version: Version) {
    @Serializable data class Version(@SerialName("codename") val release: String, val sdk: Int)
}

/** Parses the benchmark output JSON and extracts the device information. */
internal fun getDeviceInfoFromOutputFile(jsonFile: File): DeviceInfo? {
    val jsonParser = Json { ignoreUnknownKeys = true }
    return try {
        val jsonString = jsonFile.readText()
        val benchmarkData = jsonParser.decodeFromString<BenchmarkData>(jsonString)
        return benchmarkData.context.build
    } catch (e: Exception) {
        System.err.println(
            "Warning: Could not decode or process JSON from file: $jsonFile. Error: ${e.message}"
        )
        null
    }
}

/** Creates the final .metadata.json file with all collected information. */
internal fun createMetadataFile(metadata: Metadata, outputDirPath: Path) {
    val json = Json { prettyPrint = true }
    val jsonString = json.encodeToString(metadata)
    Files.createDirectories(outputDirPath)
    val file = outputDirPath.resolve(".metadata.json").toFile()
    file.writeText(jsonString)
    println("Metadata file created at: ${file.absolutePath}")
}
