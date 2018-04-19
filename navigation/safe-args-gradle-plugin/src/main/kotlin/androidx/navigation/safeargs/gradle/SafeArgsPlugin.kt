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

package androidx.navigation.safeargs.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.BaseVariant
import groovy.util.XmlSlurper
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

private const val PLUGIN_DIRNAME = "navigation-args"
internal const val GENERATED_PATH = "generated/source/$PLUGIN_DIRNAME"
internal const val INTERMEDIATES_PATH = "intermediates/$PLUGIN_DIRNAME"

@Suppress("unused")
class SafeArgsPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val appExtension = project.extensions.findByType(AppExtension::class.java)
                ?: throw GradleException("safeargs plugin must be used with android plugin")
        appExtension.applicationVariants.all { variant ->
            val task = project.tasks.create("generateSafeArgs${variant.name.capitalize()}",
                    ArgumentsGenerationTask::class.java) { task ->
                task.rFilePackage = variant.rFilePackage()
                task.applicationId = variant.applicationId
                task.navigationFiles = navigationFiles(variant)
                task.outputDir = File(project.buildDir, "$GENERATED_PATH/${variant.dirName}")
                task.incrementalFolder = File(project.buildDir,
                        "$INTERMEDIATES_PATH/${variant.dirName}")
                task.variantName = variant.name
            }
            variant.registerJavaGeneratingTask(task, task.outputDir)
        }
    }
}

private fun navigationFiles(variant: BaseVariant) = variant.sourceSets
        .flatMap { it.resDirectories }
        .mapNotNull {
            File(it, "navigation").let { navFolder ->
                if (navFolder.exists() && navFolder.isDirectory) navFolder else null
            }
        }
        .flatMap { navFolder -> navFolder.listFiles().asIterable() }
        .groupBy { file -> file.name }
        .map { entry -> entry.value.last() }

private fun BaseVariant.rFilePackage(): String {
    val mainSourceSet = sourceSets.find { it.name == "main" }
    val sourceSet = mainSourceSet ?: sourceSets[0]
    val manifest = sourceSet.manifestFile
    val parsed = XmlSlurper(false, false).parse(manifest)
    return parsed.getProperty("@package").toString()
}