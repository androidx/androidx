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

package androidx.privacysandbox.tools.apigenerator

import androidx.privacysandbox.tools.testing.CompilationTestHelper.assertCompiles
import androidx.room.compiler.processing.util.Source
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.name

fun compileIntoInterfaceDescriptorsJar(vararg sources: Source): Path {
    val tempDir = createTempDirectory("compile").toFile().also { it.deleteOnExit() }

    val result = assertCompiles(sources.toList())
    val sdkInterfaceDescriptors = File(tempDir, "sdk-interface-descriptors.jar")
    assertThat(sdkInterfaceDescriptors.createNewFile()).isTrue()

    ZipOutputStream(sdkInterfaceDescriptors.outputStream()).use { zipOutputStream ->
        result.outputClasspath.forEach { classPathDir ->
            classPathDir.walk().filter(File::isFile).forEach { file ->
                val zipEntry = ZipEntry(classPathDir.toPath().relativize(file.toPath()).name)
                zipOutputStream.putNextEntry(zipEntry)
                file.inputStream().copyTo(zipOutputStream)
                zipOutputStream.closeEntry()
            }
        }
    }

    return sdkInterfaceDescriptors.toPath()
}
