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

import org.apache.commons.io.FileUtils
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.konan.target.Family

/**
 * Generates the actual impl of `getExtensionFileName()` from LoadableExtension.kt that will
 * return the path to the given library file.
 */
abstract class GenerateLoadableExtensionSource extends DefaultTask {

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getLibraryFile()

    @OutputDirectory
    abstract DirectoryProperty getSourceDirectory()

    GenerateLoadableExtensionSource() {
        description = "Generates a source file with the path to the given library file."
        group = "build"
    }

    @TaskAction
    void generate() {
        File sourceFile = sourceDirectory.file("LoadableExtension.kt").get().asFile
        sourceFile.parentFile.mkdirs()
        sourceFile.text = """
        package androidx.sqlite.driver.test

        actual fun getExtensionFileName(): String = "${libraryFile.get().asFile.path}"
        """.stripIndent()
    }
}

void configureLoadableExtensionTask(
        Project project,
        KotlinTarget target,
        Provider<RegularFile> extensionFile) {
    if (target.konanTarget.family == Family.IOS) {
        // For iOS we need to move the native extension file into the iOS binary output test
        // directory that will be part of the resources that can be read with NSBundle.
        // This is necessary because there is no KGP API for bundling resources, see
        // https://youtrack.jetbrains.com/issue/KT-42418
        target.binaries.configureEach { binary ->
            binary.linkTaskProvider.configure { linkTask ->
                linkTask.inputs.files(extensionFile)
                        .withPropertyName("extensionFile")
                        .withPathSensitivity(PathSensitivity.RELATIVE)
                linkTask.doLast {
                    FileUtils.copyFileToDirectory(
                            extensionFile.get().asFile,
                            linkTask.destinationDirectory.get().asFile
                    )
                }
            }
        }
    } else {
        // For other native platforms, those that run in the host (Linux and Mac), then a Kotlin
        // source file is generated with the actual implementation of getExtensionFileName()
        // pointing to the build directory where the lib file is located.
        // This is necessary because there is no support for native library resources, see:
        // https://youtrack.jetbrains.com/issue/KT-39194 and
        // https://youtrack.jetbrains.com/issue/KT-46753
        def generateTask = tasks.register(
                "generateLoadableExtensionSource${target.name}",
                GenerateLoadableExtensionSource
        ) {
            it.libraryFile.set(extensionFile)
            it.sourceDirectory.set(
                    project.layout.buildDirectory.dir("generated/loadableExtension/${target.name}")
            )
        }
        // add generated source to source set
        target.compilations["test"].defaultSourceSet.kotlin.srcDir(
                generateTask.flatMap { it.sourceDirectory }
        )
    }
}

// export configure function
ext.configureLoadableExtensionTask = this.&configureLoadableExtensionTask