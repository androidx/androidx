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

import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.android.SdkConstants.DOT_KOTLIN_MODULE
import com.android.utils.appendCapitalized
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper

internal fun Project.validateKotlinModuleFiles(variantName: String, aar: Provider<RegularFile>) {
    if (
        !project.plugins.hasPlugin(KotlinBasePluginWrapper::class.java) &&
            !project.plugins.hasPlugin(KotlinMultiplatformPluginWrapper::class.java)
    ) {
        return
    }
    val validateKotlinModuleFiles =
        tasks.register(
            "validateKotlinModuleFilesFor".appendCapitalized(variantName),
            ValidateModuleFilesTask::class.java
        ) {
            it.aar.set(aar)
            it.cacheEvenIfNoOutputs()
        }
    project.addToBuildOnServer(validateKotlinModuleFiles)
}

@CacheableTask
abstract class ValidateModuleFilesTask() : DefaultTask() {

    @get:Inject abstract val archiveOperations: ArchiveOperations

    @get:PathSensitive(PathSensitivity.NONE) @get:InputFile abstract val aar: RegularFileProperty

    @get:Internal
    val fileName: String
        get() = aar.get().asFile.name

    @TaskAction
    fun execute() {
        val fileTree = archiveOperations.zipTree(aar)
        val classesJar =
            fileTree.find { it.name == "classes.jar" }
                ?: throw GradleException("Could not classes.jar in $fileName")
        val jarContents = archiveOperations.zipTree(classesJar)
        if (jarContents.files.size <= 1) {
            // only version file, stub project with no sources.
            return
        }
        jarContents.find { it.name.endsWith(DOT_KOTLIN_MODULE) }
            ?: throw GradleException("Could not find .kotlin_module file in $fileName")
    }
}
