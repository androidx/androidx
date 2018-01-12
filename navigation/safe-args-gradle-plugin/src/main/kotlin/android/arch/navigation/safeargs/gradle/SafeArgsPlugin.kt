/*
 * Copyright 2018 The Android Open Source Project
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

package android.arch.navigation.safeargs.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

private const val GENERATED_PATH = "generated/source/args"

@Suppress("unused")
class SafeArgsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val appExtension = project.extensions.findByType(AppExtension::class.java)
                ?: throw GradleException("safeargs plugin must be used with android plugin")
        appExtension.applicationVariants.all { variant ->
            val task = project.tasks.create("generateSafeArgs${variant.name.capitalize()}",
                    ArgumentsGenerationTask::class.java) { task ->
                task.applicationId = variant.applicationId
                task.navigationFiles = navigationFiles(variant)
                task.outputDir = File(project.buildDir, GENERATED_PATH)
            }
            variant.registerJavaGeneratingTask(task, task.outputDir)
        }
    }

    //TODO: make it real
    private fun navigationFiles(variant: BaseVariant) = variant.sourceSets
            .flatMap { it.resDirectories }
            .mapNotNull {
                File(it, "navigation").let { navFolder ->
                    if (navFolder.exists() && navFolder.isDirectory) navFolder else null
                }
            }
            .flatMap { navFolder -> navFolder.listFiles().asIterable() }
}
