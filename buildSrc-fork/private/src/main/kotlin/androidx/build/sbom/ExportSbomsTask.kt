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

package androidx.build.sbom

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Copies the project's SBOM file to the distribution directory. */
@DisableCachingByDefault(because = "Zip tasks are not worth caching according to Gradle")
abstract class ExportSbomsTask : DefaultTask() {
    @get:Inject abstract val fileSystemOperations: FileSystemOperations

    @get:OutputDirectory abstract val destinationDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sbomFile: RegularFileProperty

    @get:Input abstract val outputFileName: Property<String>

    @TaskAction
    fun copySboms() {
        if (!sbomFile.get().asFile.exists()) {
            throw GradleException("sbom file does not exist: ${sbomFile.get().asFile.path}")
        }
        destinationDir.get().asFile.mkdirs()
        fileSystemOperations.copy {
            it.from(sbomFile)
            it.into(destinationDir)
            it.rename(sbomFile.get().asFile.name, outputFileName.get())
        }
    }
}
