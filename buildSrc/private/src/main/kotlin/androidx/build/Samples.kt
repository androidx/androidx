/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.named
import org.gradle.work.DisableCachingByDefault

/**
 * Used to register a project that will be providing documentation samples for this project.
 * Can only be called once so only one samples library can exist per library b/318840087.
 */
fun AndroidXExtension.registerSamplesLibrary(samplesProject: Project) {
    fun Configuration.setResolveSources() {
        // While a sample library can have more dependencies than the library it has samples
        // for, in Studio sample code is not executable or inspectable, so we don't need them.
        isTransitive = false
        isCanBeConsumed = false
        attributes {
            it.attribute(
                Usage.USAGE_ATTRIBUTE,
                project.objects.named<Usage>(Usage.JAVA_RUNTIME)
            )
            it.attribute(
                Category.CATEGORY_ATTRIBUTE,
                project.objects.named<Category>(Category.DOCUMENTATION)
            )
            it.attribute(
                DocsType.DOCS_TYPE_ATTRIBUTE,
                project.objects.named<DocsType>(DocsType.SOURCES)
            )
            it.attribute(
                LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                project.objects.named<LibraryElements>(LibraryElements.JAR)
            )
        }
    }

    val samplesConfiguration = project.configurations.findByName("samples")
        ?: project.configurations.create("samples") {
            it.setResolveSources()
            it.isVisible = false
            it.isCanBeConsumed = false
            it.isCanBeResolved = true
        }
    project.dependencies.add("samples", samplesProject)

    copySampleSourceJarsTask =
        project.tasks.register("copySampleSourceJars", LazyInputsCopyTask::class.java) {
            it.inputJars.from(samplesConfiguration.incoming.artifactView { }.files)
            // This file will be renamed later by `publish`. We can't know the proper name before
            // this tasks's dependencies are known after configuration is finished.
            it.destinationDir.set(project.layout.buildDirectory.file("samples_sources.jar"))
        }
}

/**
 * This is necessary because we need to delay artifact resolution until after configuration.
 * If one sample is used by multiple libraries (e.g. paging-samples) it is copied several times.
 * This is to avoid caching failures. There should be a better way that avoids needing this.
 */
@DisableCachingByDefault(because = "caching large output files is more expensive than copying")
abstract class LazyInputsCopyTask : DefaultTask() {
    @get:[InputFiles PathSensitive(value = PathSensitivity.RELATIVE)]
    abstract val inputJars: ConfigurableFileCollection
    @get:OutputFile
    abstract val destinationDir: RegularFileProperty

    @TaskAction
    fun copyAction() {
        inputJars.files.single().copyTo(destinationDir.get().asFile, overwrite = true)
    }
}
