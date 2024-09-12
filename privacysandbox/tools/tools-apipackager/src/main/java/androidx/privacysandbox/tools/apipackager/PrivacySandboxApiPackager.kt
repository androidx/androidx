/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.apipackager

import androidx.privacysandbox.tools.apipackager.AnnotationInspector.hasPrivacySandboxAnnotation
import androidx.privacysandbox.tools.apipackager.ClassReader.parseKotlinMetadata
import androidx.privacysandbox.tools.apipackager.ClassReader.toClassNode
import androidx.privacysandbox.tools.core.Metadata
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists
import kotlin.io.path.readBytes

class PrivacySandboxApiPackager {

    /**
     * Extracts API descriptors from SDKs annotated with Privacy Sandbox annotations.
     *
     * This function will output a file with SDK interface descriptors, which can later be used to
     * generate the client-side sources for communicating with this SDK over IPC through the privacy
     * sandbox.
     *
     * @param sdkClasspath path to the root directory that contains the SDK's compiled classes.
     *   Non-class files will be safely ignored.
     * @param sdkInterfaceDescriptorsOutput output path for SDK Interface descriptors file.
     */
    fun packageSdkDescriptors(
        sdkClasspath: Path,
        sdkInterfaceDescriptorsOutput: Path,
    ) {
        require(sdkClasspath.exists() && sdkClasspath.isDirectory()) {
            "$sdkClasspath is not a valid classpath."
        }
        require(sdkInterfaceDescriptorsOutput.notExists()) {
            "$sdkInterfaceDescriptorsOutput already exists."
        }

        val outputFile =
            sdkInterfaceDescriptorsOutput.toFile().also {
                it.parentFile.mkdirs()
                it.createNewFile()
            }

        val companionNames =
            sdkClasspath
                .toFile()
                .walk()
                .filter { it.isFile }
                .map { it.toPath() }
                .filter { shouldKeepFile(sdkClasspath, it) }
                .filter { it.extension == "class" }
                .mapNotNull { file ->
                    try {
                        val klass = parseKotlinMetadata(toClassNode(file.readBytes()))
                        val companionName = ("${klass.name}$${klass.companionObject}.class")
                        if (klass.companionObject == null) null else companionName
                    } catch (e: Exception) {
                        null
                    }
                }
                .toList()

        ZipOutputStream(outputFile.outputStream()).use { zipOutputStream ->
            sdkClasspath
                .toFile()
                .walk()
                .filter { it.isFile }
                .map { it.toPath() }
                .filter { shouldKeepFile(sdkClasspath, it, companionNames) }
                .forEach { file ->
                    val zipEntry = ZipEntry(sdkClasspath.relativize(file).toString())
                    zipOutputStream.putNextEntry(zipEntry)
                    file.inputStream().copyTo(zipOutputStream)
                    zipOutputStream.closeEntry()
                }
        }
    }

    private fun shouldKeepFile(
        sdkClasspath: Path,
        filePath: Path,
        companionNames: List<String> = listOf()
    ): Boolean {
        if (sdkClasspath.relativize(filePath) == Metadata.filePath) {
            return true
        }
        if (filePath.extension == "class" && hasPrivacySandboxAnnotation(filePath)) {
            return true
        }
        if (companionNames.any { filePath.endsWith(it) }) {
            return true
        }
        return false
    }
}
