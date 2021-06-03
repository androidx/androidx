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

import com.android.build.api.extension.AndroidComponentsExtension
import com.android.build.api.variant.DynamicFeatureVariant
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import java.io.File
import javax.inject.Inject

private const val PLUGIN_DIRNAME = "navigation-args"
internal const val GENERATED_PATH = "generated/source/$PLUGIN_DIRNAME"
internal const val INCREMENTAL_PATH = "intermediates/incremental"

abstract class SafeArgsPlugin protected constructor(
    val providerFactory: ProviderFactory
) : Plugin<Project> {

    abstract val generateKotlin: Boolean

    private fun forEachVariant(extension: BaseExtension, action: (BaseVariant) -> Unit) {
        when {
            extension is AppExtension -> extension.applicationVariants.all(action)
            extension is LibraryExtension -> {
                extension.libraryVariants.all(action)
            }
            else -> throw GradleException(
                "safeargs plugin must be used with android app," +
                    "library or feature plugin"
            )
        }
    }

    override fun apply(project: Project) {
        val extension = project.extensions.findByType(BaseExtension::class.java)
            ?: throw GradleException("safeargs plugin must be used with android plugin")
        val isKotlinProject =
            project.extensions.findByType(KotlinProjectExtension::class.java) != null
        if (!isKotlinProject && generateKotlin) {
            throw GradleException(
                "androidx.navigation.safeargs.kotlin plugin must be used with kotlin plugin"
            )
        }
        val applicationIds = mutableMapOf<String, Provider<String>>()
        val variantExtension =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
                ?: throw GradleException("safeargs plugin must be used with android plugin")
        variantExtension.onVariants { variant ->
            when (variant) {
                is ApplicationVariant, is DynamicFeatureVariant ->
                    applicationIds.getOrPut(variant.name) {
                        variant.applicationId
                    }
            }
        }

        forEachVariant(extension) { variant ->
            val task = project.tasks.create(
                "generateSafeArgs${variant.name.capitalize()}",
                ArgumentsGenerationTask::class.java
            ) { task ->
                task.applicationId.set(
                    // this will only put in the case where the extension is a Library module
                    // and should be superseded by `getNamespace()` in agp 7.0+
                    applicationIds.getOrPut(variant.name) {
                        providerFactory.provider { variant.applicationId }
                    }
                )
                task.rFilePackage.set(variant.rFilePackage())
                task.navigationFiles.setFrom(navigationFiles(variant, project))
                task.outputDir.set(File(project.buildDir, "$GENERATED_PATH/${variant.dirName}"))
                task.incrementalFolder.set(File(project.buildDir, "$INCREMENTAL_PATH/${task.name}"))
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
            variant.registerJavaGeneratingTask(task, task.outputDir.asFile.get())
        }
    }

    private fun BaseVariant.rFilePackage() = providerFactory.provider {
        val mainSourceSet = sourceSets.find { it.name == "main" }
        val sourceSet = mainSourceSet ?: sourceSets[0]
        val manifest = sourceSet.manifestFile
        @Suppress("DEPRECATION") // b/181913965
        val parsed = groovy.util.XmlSlurper(false, false).parse(manifest)
        parsed.getProperty("@package").toString()
    }

    private fun navigationFiles(
        variant: BaseVariant,
        project: Project
    ): ConfigurableFileCollection {
        val fileProvider = providerFactory.provider {
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
class SafeArgsJavaPlugin @Inject constructor(
    providerFactory: ProviderFactory
) : SafeArgsPlugin(providerFactory) {

    override val generateKotlin = false
}

@Suppress("unused")
class SafeArgsKotlinPlugin @Inject constructor(
    providerFactory: ProviderFactory
) : SafeArgsPlugin(providerFactory) {

    override val generateKotlin = true
}