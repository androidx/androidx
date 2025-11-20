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

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.DynamicFeatureVariant
import com.android.build.api.variant.LibraryVariant
import com.android.build.api.variant.Variant
import java.io.File
import java.util.Locale
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal const val TASK_NAME_PREFIX = "generateSafeArgs"
internal const val INCREMENTAL_PATH = "intermediates/incremental"

abstract class SafeArgsPlugin protected constructor() : Plugin<Project> {

    abstract val generateKotlin: Boolean

    private val agpBasePluginId = "com.android.base"

    override fun apply(project: Project) {
        var isAndroidProject = false
        project.plugins.withId(agpBasePluginId) {
            isAndroidProject = true
            applySafeArgsPlugin(project)
        }
        val isKotlinProject = project.extensions.findByName("kotlin") != null
        project.afterEvaluate {
            if (!isAndroidProject) {
                throw GradleException("safeargs plugin must be used with android plugin")
            }
            if (!isKotlinProject && generateKotlin) {
                throw GradleException(
                    "androidx.navigation.safeargs.kotlin plugin must be used with kotlin plugin"
                )
            }
        }
    }

    private fun applySafeArgsPlugin(project: Project) {
        // TODO(b/366179719): Handle the case where AGP is not in the same classpath as SafeArgs
        //  Plugin due to compileOnly dep.
        val componentsExtension =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
                ?: throw GradleException("safeargs plugin must be used with android plugin")
        if (componentsExtension.pluginVersion < AndroidPluginVersion(8, 4)) {
            throw GradleException(
                "safeargs Gradle plugin is only compatible with Android " +
                    "Gradle plugin (AGP) version 8.4.0 or higher (found " +
                    "${componentsExtension.pluginVersion})."
            )
        }

        componentsExtension.onVariants { variant ->
            val applicationId =
                when (variant) {
                    is ApplicationVariant -> variant.applicationId
                    is DynamicFeatureVariant -> variant.applicationId
                    is LibraryVariant -> variant.namespace
                    else -> variant.namespace
                }

            val task =
                project.tasks.register(
                    "$TASK_NAME_PREFIX${variant.name.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                    }}",
                    ArgumentsGenerationTask::class.java,
                ) { task ->
                    task.applicationId.set(applicationId)
                    task.rFilePackage.set(variant.namespace)
                    task.navigationFiles.setFrom(navigationFiles(variant))
                    task.incrementalFolder.set(
                        project.layout.buildDirectory.dir("$INCREMENTAL_PATH/${task.name}")
                    )
                    // TODO: Remove this check once this moves to AGP 9.0+
                    task.useAndroidX.set(
                        (project.findProperty("android.useAndroidX") != "false").also {
                            if (!it) {
                                throw GradleException(
                                    "androidx.navigation.safeargs can only be used with an androidx " +
                                        "project"
                                )
                            }
                        }
                    )
                    task.generateKotlin.set(generateKotlin)
                }
            variant.sources.java?.addGeneratedSourceDirectory(
                task,
                ArgumentsGenerationTask::outputDir,
            )
        }
    }

    private fun navigationFiles(variant: Variant): Provider<List<File>> {
        return variant.sources.res!!.all.map { resSources ->
            resSources
                .flatten()
                .map { it.asFile }
                .mapNotNull { resDir ->
                    File(resDir, "navigation").let { navFolder ->
                        if (navFolder.exists() && navFolder.isDirectory) navFolder else null
                    }
                }
                .flatMap { navFolder -> navFolder.listFiles()?.asIterable() ?: emptyList() }
                .filter { file -> file.isFile }
                .groupBy { file -> file.name }
                .map { entry ->
                    entry.value.singleOrNull()
                        ?: entry.value.minBy { file ->
                            when {
                                // matches variant name exactly
                                file.path.substringBefore("/res/").endsWith(variant.name) -> 0
                                // matches variant flavor
                                variant.flavorName?.let {
                                    file.path.substringBefore("/res/").endsWith(it)
                                } == true -> 1
                                // matches variant buildType
                                variant.buildType?.let {
                                    file.path.substringBefore("/res/").endsWith(it)
                                } == true -> 2
                                // fall back to main
                                else -> 3
                            }
                        }
                }
        }
    }
}

@Suppress("unused")
class SafeArgsJavaPlugin @Inject constructor() : SafeArgsPlugin() {

    override val generateKotlin = false
}

@Suppress("unused")
class SafeArgsKotlinPlugin @Inject constructor() : SafeArgsPlugin() {

    override val generateKotlin = true
}
