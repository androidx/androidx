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

package androidx.baselineprofiles.gradle.consumer

import androidx.baselineprofiles.gradle.utils.ATTRIBUTE_BUILD_TYPE
import androidx.baselineprofiles.gradle.utils.ATTRIBUTE_CATEGORY_BASELINE_PROFILE
import androidx.baselineprofiles.gradle.utils.ATTRIBUTE_FLAVOR
import androidx.baselineprofiles.gradle.utils.CONFIGURATION_NAME_BASELINE_PROFILES
import androidx.baselineprofiles.gradle.utils.camelCase
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.TestedExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.tasks.StopExecutionException

/**
 * This is the consumer plugin for baseline profile generation. In order to generate baseline
 * profiles three plugins are needed: one is applied to the app or the library that should consume
 * the baseline profile when building (consumer), one is applied to the project that should supply
 * the apk under test (build provider) and the last one is applied to a library module containing
 * the ui test that generate the baseline profile on the device (producer).
 */
class BaselineProfilesConsumerPlugin : Plugin<Project> {

    companion object {

        // The output file for the HRF baseline profile file in `src/main`
        private const val BASELINE_PROFILE_SRC_MAIN_FILENAME = "baseline-prof.txt"
    }

    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") {
            configureWithAndroidPlugin(project = project, isApplication = true)
        }
        project.pluginManager.withPlugin("com.android.library") {
            configureWithAndroidPlugin(project = project, isApplication = false)
        }
    }

    private fun configureWithAndroidPlugin(project: Project, isApplication: Boolean) {

        // TODO (b/259737859): This code will be updated to use source sets for baseline profiles,
        //  as soon androidx repo is updated to use AGP 8.0-beta01.

        val androidComponent = project.extensions.getByType(
            AndroidComponentsExtension::class.java
        )

        val baselineProfilesExtension = BaselineProfilesConsumerExtension.registerExtension(project)

        // Creates all the configurations, one per variant.
        // Note that for this version of the plugin is not possible to rely entirely on the variant
        // api so the actual creation of the tasks is postponed to be executed when all the
        // agp tasks have been created, using the old api.
        val mainBaselineProfileConfiguration = createBaselineProfileConfigurationForVariant(
            project,
            variantName = "",
            flavorName = "",
            buildTypeName = "",
            mainConfiguration = null
        )
        val baselineProfileConfigurations = mutableListOf<Configuration>()
        val baselineProfileVariantNames = mutableListOf<String>()
        androidComponent.apply {
            onVariants {

                // Only create configurations for the build type expressed in the baseline profiles
                // extension. Note that this can be removed after b/265438201.
                if (it.buildType != baselineProfilesExtension.buildTypeName) {
                    return@onVariants
                }

                baselineProfileConfigurations.add(
                    createBaselineProfileConfigurationForVariant(
                        project,
                        variantName = it.name,
                        flavorName = it.flavorName ?: "",
                        buildTypeName = it.buildType ?: "",
                        mainConfiguration = mainBaselineProfileConfiguration
                    )
                )

                // Save this variant name so later we can use it to set a dependency on the
                // merge/prepare art profile task for it.
                baselineProfileVariantNames.add(it.name)
            }
        }

        // Now that the configurations are created, the tasks can be created. The consumer plugin
        // can only be applied to either applications or libraries.
        // Note that for this plugin does not use the new variant api as it tries to access to some
        // AGP tasks that don't yet exist in the new variant api callback (b/262007432).
        val extensionVariants =
            when (val tested = project.extensions.getByType(TestedExtension::class.java)) {
                is AppExtension -> tested.applicationVariants
                is LibraryExtension -> tested.libraryVariants
                else -> throw StopExecutionException(
                    """
                Unrecognized extension: $tested not of type AppExtension or LibraryExtension.
                """.trimIndent()
                )
            }

        // After variants have been resolved and the AGP tasks have been created add the plugin tasks.
        var applied = false
        extensionVariants.all {
            if (applied) return@all
            applied = true

            // Currently the plugin does not support generating a baseline profile for a specific
            // flavor: all the flavors are merged into one and copied in src/main/baseline-prof.txt.
            // This can be changed after b/239659205 when baseline profiles become a source set.
            val mergeBaselineProfilesTaskProvider = project.tasks.register(
                "generateBaselineProfiles", MergeBaselineProfileTask::class.java
            ) { task ->

                // These are all the configurations this task depends on, in order to consume their
                // artifacts.
                task.baselineProfileFileCollection.setFrom(baselineProfileConfigurations)

                // This is the output file where all the configurations will be merged in.
                // Note that this file is overwritten.
                task.baselineProfileFile.set(
                    project
                        .layout
                        .projectDirectory
                        .file("src/main/$BASELINE_PROFILE_SRC_MAIN_FILENAME")
                )
            }

            // If this is an application the mergeBaselineProfilesTask must run before the
            // tasks that handle the baseline profile packaging. Merge for applications, prepare
            // for libraries. Note that this will change with AGP 8.0 that should support
            // source sets for baseline profiles.
            for (variantName in baselineProfileVariantNames) {
                val taskProvider = if (isApplication) {
                    project.tasks.named(camelCase("merge", variantName, "artProfile"))
                } else {
                    project.tasks.named(camelCase("prepare", variantName, "artProfile"))
                }
                taskProvider.configure { it.mustRunAfter(mergeBaselineProfilesTaskProvider) }
            }
        }
    }

    private fun createBaselineProfileConfigurationForVariant(
        project: Project,
        variantName: String,
        flavorName: String,
        buildTypeName: String,
        mainConfiguration: Configuration?
    ): Configuration {

        val buildTypeConfiguration =
            if (buildTypeName.isNotBlank() && buildTypeName != variantName) {
                project
                    .configurations
                    .maybeCreate(
                        camelCase(
                            buildTypeName,
                            CONFIGURATION_NAME_BASELINE_PROFILES
                        )
                    )
                    .apply {
                        if (mainConfiguration != null) extendsFrom(mainConfiguration)
                        isCanBeResolved = true
                        isCanBeConsumed = false
                    }
            } else null

        val flavorConfiguration = if (flavorName.isNotBlank() && flavorName != variantName) {
            project
                .configurations
                .maybeCreate(camelCase(flavorName, CONFIGURATION_NAME_BASELINE_PROFILES))
                .apply {
                    if (mainConfiguration != null) extendsFrom(mainConfiguration)
                    isCanBeResolved = true
                    isCanBeConsumed = false
                }
        } else null

        return project
            .configurations
            .maybeCreate(camelCase(variantName, CONFIGURATION_NAME_BASELINE_PROFILES))
            .apply {

                // The variant specific configuration always extends from build type and flavor
                // configurations, when existing.
                val extendFrom = mutableListOf<Configuration>()
                if (mainConfiguration != null) {
                    extendFrom.add(mainConfiguration)
                }
                if (flavorConfiguration != null) {
                    extendFrom.add(flavorConfiguration)
                }
                if (buildTypeConfiguration != null) {
                    extendFrom.add(buildTypeConfiguration)
                }
                setExtendsFrom(extendFrom)

                isCanBeResolved = true
                isCanBeConsumed = false

                attributes {
                    it.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.objects.named(
                            Category::class.java,
                            ATTRIBUTE_CATEGORY_BASELINE_PROFILE
                        )
                    )
                    it.attribute(
                        ATTRIBUTE_BUILD_TYPE,
                        buildTypeName
                    )
                    it.attribute(
                        ATTRIBUTE_FLAVOR,
                        flavorName
                    )
                }
            }
    }
}