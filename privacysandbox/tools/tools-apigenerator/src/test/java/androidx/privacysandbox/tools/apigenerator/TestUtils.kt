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

import androidx.privacysandbox.tools.apipackager.PrivacySandboxApiPackager
import androidx.privacysandbox.tools.testing.CompilationTestHelper.assertCompiles
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationResult
import java.nio.file.Files.createTempDirectory
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.writeBytes

/**
 * Compiles the given [sources] and creates a packaged SDK API descriptors jar.
 *
 * @param descriptorResources map of extra resources that will be added to descriptors jar keyed by
 *   their relative path.
 */
fun compileIntoInterfaceDescriptorsJar(
    sources: List<Source>,
    descriptorResources: Map<Path, ByteArray> = mapOf(),
): Path {
    val tempDir = createTempDirectory("compile").also { it.toFile().deleteOnExit() }
    val result = assertCompiles(sources.toList())
    val sdkInterfaceDescriptors = tempDir.resolve("sdk-interface-descriptors.jar")
    val outputClasspath = mergedClasspath(result)
    descriptorResources.forEach { (relativePath, contents) ->
        outputClasspath.resolve(relativePath).apply {
            parent?.createDirectories()
            createFile()
            writeBytes(contents)
        }
    }
    PrivacySandboxApiPackager().packageSdkDescriptors(outputClasspath, sdkInterfaceDescriptors)
    return sdkInterfaceDescriptors
}

/**
 * Merges all class paths from a compilation result into a single one.
 *
 * Room's compilation library returns different class paths for Kotlin and Java, so we need to merge
 * them for tests that depend on the two. This is a naive implementation that simply overwrites
 * classes that appear in multiple class paths.
 */
fun mergedClasspath(compilationResult: TestCompilationResult): Path {
    val outputClasspath = createTempDirectory("classpath").also { it.toFile().deleteOnExit() }
    compilationResult.outputClasspath.forEach { classpath ->
        classpath.copyRecursively(outputClasspath.toFile(), overwrite = true)
    }
    return outputClasspath
}
