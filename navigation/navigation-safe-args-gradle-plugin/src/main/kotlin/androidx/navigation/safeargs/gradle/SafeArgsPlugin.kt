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
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import java.io.File
import java.util.Locale
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory

private const val PLUGIN_DIRNAME = "navigation-args"
internal const val GENERATED_PATH = "generated/source/$PLUGIN_DIRNAME"
internal const val INCREMENTAL_PATH = "intermediates/incremental"

abstract class SafeArgsPlugin protected constructor(private val providerFactory: ProviderFactory) :
    Plugin<Project> {

    abstract val generateKotlin: Boolean

    private val agpBasePluginId = "com.android.base"

    private val kgpPluginIds =
        listOf(
            "org.jetbrains.kotlin.jvm",
            "org.jetbrains.kotlin.android",
            "org.jetbrains.kotlin.multiplatform"
        )

    @Suppress("DEPRECATION") // For BaseVariant should be replaced in later studio versions
    private fun forEachVariant(
        extension: BaseExtension,
        action: (com.android.build.gradle.api.BaseVariant) -> Unit
    ) {
        when {
            extension is AppExtension -> extension.applicationVariants.configureEach(action)
            extension is LibraryExtension -> {
                extension.libraryVariants.configureEach(action)
            }
            else ->
                throw GradleException(
                    "safeargs plugin must be used with android app," + "library or feature plugin"
                )
        }
    }

    override fun apply(project: Project) {
        var isAndroidProject = false
        project.plugins.withId(agpBasePluginId) {
            isAndroidProject = true
            applySafeArgsPlugin(project)
        }
        var isKotlinProject = false
        kgpPluginIds.forEach { kgpPluginId ->
            project.plugins.withId(kgpPluginId) { isKotlinProject = true }
        }
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
        if (componentsExtension.pluginVersion < AndroidPluginVersion(7, 3)) {
            throw GradleException(
                "safeargs Gradle plugin is only compatible with Android " +
                    "Gradle plugin (AGP) version 7.3.0 or higher (found " +
                    "${componentsExtension.pluginVersion})."
            )
        }
        // applicationId determines the location of the generated class
        val applicationIds = mutableMapOf<String, Provider<String>>()
        // namespace determines the package of the R file
        val namespaces = mutableMapOf<String, Provider<String>>()
        componentsExtension.onVariants { variant ->
            when (variant) {
                is ApplicationVariant -> {
                    applicationIds.getOrPut(variant.name) { variant.applicationId }
                    namespaces.getOrPut(variant.name) { variant.namespace }
                }
                is DynamicFeatureVariant -> {
                    applicationIds.getOrPut(variant.name) { variant.applicationId }
                    namespaces.getOrPut(variant.name) { variant.namespace }
                }
                is LibraryVariant ->
                    // we are putting the library names space in applicationId because
                    // we want the generated class to use the namespace to determine its package
                    applicationIds.getOrPut(variant.name) { variant.namespace }
            }
        }

        val oldVariantExtension =
            project.extensions.findByType(BaseExtension::class.java)
                ?: throw GradleException("safeargs plugin must be used with android plugin")
        forEachVariant(oldVariantExtension) { variant ->
            val task =
                project.tasks.register(
                    "generateSafeArgs${variant.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
                }}",
                    ArgumentsGenerationTask::class.java
                ) { task ->
                    task.applicationId.set(
                        applicationIds.getOrPut(variant.name) {
                            providerFactory.provider { variant.applicationId }
                        }
                    )
                    // If there is a namespace available, we should always use that to reference the
                    // package of the R file, otherwise we assume the R file is in the same location
                    // as
                    // the class
                    task.rFilePackage.set(namespaces[variant.name] ?: task.applicationId)
                    task.navigationFiles.setFrom(navigationFiles(variant, project))
                    task.outputDir.set(
                        project.layout.buildDirectory.dir("$GENERATED_PATH/${variant.dirName}")
                    )
                    task.incrementalFolder.set(
                        project.layout.buildDirectory.dir("$INCREMENTAL_PATH/${task.name}")
                    )
                    task.useAndroidX.set(
                        (project.findProperty("android.useAndroidX") == "true").also {
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
            @Suppress("DEPRECATION") // For BaseVariant should be replaced in later studio versions
            variant.registerJavaGeneratingTask(task, task.get().outputDir.asFile.get())
        }
    }

    @Suppress("DEPRECATION") // For BaseVariant should be replaced in later studio versions
    private fun navigationFiles(
        variant: com.android.build.gradle.api.BaseVariant,
        project: Project
    ): ConfigurableFileCollection {
        val fileProvider =
            providerFactory.provider {
                variant.sourceSets
                    .flatMap { it.resDirectories }
                    .mapNotNull {
                        File(it, "navigation").let { navFolder ->
                            if (navFolder.exists() && navFolder.isDirectory) navFolder else null
                        }
                    }
                    .flatMap { navFolder -> navFolder.listFiles().asIterable() }
                    .filter { file -> file.isFile }
                    .groupBy { file -> file.name }
                    .map { entry -> entry.value.last() }
            }
        return project.files(fileProvider)
    }
}

@Suppress("unused")
class SafeArgsJavaPlugin @Inject constructor(providerFactory: ProviderFactory) :
    SafeArgsPlugin(providerFactory) {

    override val generateKotlin = false
}

@Suppress("unused")
class SafeArgsKotlinPlugin @Inject constructor(providerFactory: ProviderFactory) :
    SafeArgsPlugin(providerFactory) {

    override val generateKotlin = true
}
