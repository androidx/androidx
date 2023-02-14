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
import androidx.baselineprofiles.gradle.utils.INTERMEDIATES_BASE_FOLDER
import androidx.baselineprofiles.gradle.utils.camelCase
import androidx.baselineprofiles.gradle.utils.checkAgpVersion
import androidx.baselineprofiles.gradle.utils.isGradleSyncRunning
import androidx.baselineprofiles.gradle.utils.maybeRegister
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.Variant
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

/**
 * This is the consumer plugin for baseline profile generation. In order to generate baseline
 * profiles three plugins are needed: one is applied to the app or the library that should consume
 * the baseline profile when building (consumer), one is applied to the project that should supply
 * the apk under test (build provider) and the last one is applied to a test module containing
 * the ui test that generate the baseline profile on the device (producer).
 */
class BaselineProfilesConsumerPlugin : Plugin<Project> {

    companion object {
        private const val GENERATE_TASK_NAME = "generate"
    }

    override fun apply(project: Project) {
        var foundAppOrLibraryPlugin = false
        project.pluginManager.withPlugin("com.android.application") {
            foundAppOrLibraryPlugin = true
            configureWithAndroidPlugin(project = project)
        }
        project.pluginManager.withPlugin("com.android.library") {
            foundAppOrLibraryPlugin = true
            configureWithAndroidPlugin(project = project)
        }

        // Only used to verify that the android application plugin has been applied.
        // Note that we don't want to throw any exception if gradle sync is in progress.
        project.afterEvaluate {
            if (!project.isGradleSyncRunning()) {
                if (!foundAppOrLibraryPlugin) {
                    throw IllegalStateException(
                        """
                    The module ${project.name} does not have the `com.android.application` or
                    `com.android.library` plugin applied. The `androidx.baselineprofiles.consumer`
                    plugin supports only android application and library modules. Please review
                    your build.gradle to ensure this plugin is applied to the correct module.
                    """.trimIndent()
                    )
                }
                project.logger.debug(
                    """
                    [BaselineProfilesConsumerPlugin] afterEvaluate check: app or library plugin
                    was applied""".trimIndent()
                )
            }
        }
    }

    private fun configureWithAndroidPlugin(project: Project) {

        // Checks that the required AGP version is applied to this project.
        project.checkAgpVersion()

        // Prepares extensions used by the plugin
        val androidComponent = project.extensions.getByType(
            AndroidComponentsExtension::class.java
        )

        val baselineProfilesExtension =
            BaselineProfilesConsumerExtension.registerExtension(project)

        // Creates the main baseline profiles configuration
        val mainBaselineProfileConfiguration = createBaselineProfileConfigurationForVariant(
            project,
            variantName = "",
            flavorName = "",
            buildTypeName = "",
            mainConfiguration = null
        )

        // Checks that the extended build type exists
        androidComponent.finalizeDsl {
            it.buildTypes.findByName(baselineProfilesExtension.buildTypeName)
                ?: if (!project.isGradleSyncRunning()) {
                    throw IllegalArgumentException(
                        """
                            The build type `${baselineProfilesExtension.buildTypeName}` to generate
                            baseline profiles does not exist. Please review build.gradle for the
                            module ${project.name} and specify a release build type.
                            """.trimIndent()
                    )
                } else {
                    return@finalizeDsl
                }
        }

        // This extension exists only if this is an android application module.
        // If that's the case, we check that we're generating for a non debuggable build type.
        project
            .extensions
            .findByType(ApplicationAndroidComponentsExtension::class.java)
            ?.finalizeDsl {
                if (it
                        .buildTypes
                        .findByName(baselineProfilesExtension.buildTypeName)
                        ?.isDebuggable == true
                ) {
                    project.logger.warn(
                        """
                    The build type `${baselineProfilesExtension.buildTypeName}` to generate baseline
                    profiles is a debuggable build type. In order to generate a baseline profile is
                    advisable to utilize a release build type, to include release dependencies.
                """.trimIndent()
                    )
                }
            }

        // Iterate variants to create per-variant tasks and configurations
        androidComponent.apply {
            onVariants { variant ->

                // Only create configurations for the build type expressed in the baseline profiles
                // extension. Note that this can be removed after b/265438201.
                if (variant.buildType != baselineProfilesExtension.buildTypeName) {
                    return@onVariants
                }

                // Creates the configuration to carry the specific variant artifact
                val baselineProfileConfiguration =
                    createBaselineProfileConfigurationForVariant(
                        project,
                        variantName = variant.name,
                        flavorName = variant.flavorName ?: "",
                        buildTypeName = variant.buildType ?: "",
                        mainConfiguration = mainBaselineProfileConfiguration
                    )

                // Creates the task to merge the baseline profile artifacts coming from different
                // configurations. Note that this is the last task of the chain that triggers the
                // whole generation, hence it's called `generate`.
                val genBaselineProfilesTaskProvider = project.tasks.register(
                    camelCase(GENERATE_TASK_NAME, variant.name, "baselineProfiles"),
                    GenerateBaselineProfileTask::class.java
                ) { task ->

                    // These are all the configurations this task depends on, in order to consume their
                    // artifacts.
                    task.baselineProfileFileCollection.setFrom(baselineProfileConfiguration)

                    // This is the task output for the generated baseline profile
                    task.baselineProfileDir.set(
                        baselineProfilesExtension.baselineProfileOutputDir(
                            project = project,
                            variantName = variant.name
                        )
                    )

                    // Sets the package filter rules
                    task.filterRules.set(
                        baselineProfilesExtension.filterRules
                            .filter {
                                it.key in listOfNotNull(
                                    "main",
                                    variant.flavorName,
                                    variant.buildType,
                                    variant.name
                                )
                            }
                            .flatMap { it.value.rules }
                    )
                }

                // The output folders for variant and main profiles are added as source dirs using
                // source sets api. This cannot be done in the `configure` block of the generation
                // task. The `onDemand` flag is checked here and the src set folder is chosen
                // accordingly: if `true`, baseline profiles are saved in the src folder so they
                // can be committed with srcs, if `false` they're stored in the generated build
                // files.
                variant.setBaselineProfileSrcSet(
                    project = project,
                    baselineProfilesExtension = baselineProfilesExtension,
                    genBaselineProfilesTaskProvider = genBaselineProfilesTaskProvider
                )

                // Creates a build and flavor specific tasks, if not existing, to trigger all the
                // variants for this build or flavor. Note that it's possible for the variant
                // to be equal to the build type, if no flavor is specified.
                if (variant.name != variant.buildType) {
                    maybeCreateParentGenTask(
                        project,
                        variant.buildType,
                        genBaselineProfilesTaskProvider
                    )
                }
                maybeCreateParentGenTask(
                    project = project,
                    parentName = variant.flavorName,
                    childGenerationTaskProvider = genBaselineProfilesTaskProvider
                )
                maybeCreateParentGenTask(
                    project = project,
                    parentName = "",
                    childGenerationTaskProvider = genBaselineProfilesTaskProvider
                )
            }
        }
    }

    private fun maybeCreateParentGenTask(
        project: Project,
        parentName: String?,
        childGenerationTaskProvider: TaskProvider<GenerateBaselineProfileTask>
    ) {
        if (parentName == null) return
        project.tasks.maybeRegister<Task>(GENERATE_TASK_NAME, parentName, "baselineProfiles") {
            it.group = "Baseline Profiles"
            it.description = "Generates baseline profiles."
            it.dependsOn(childGenerationTaskProvider)
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

fun BaselineProfilesConsumerExtension.baselineProfileOutputDir(
    project: Project,
    variantName: String
): Provider<Directory> =
    if (onDemandGeneration) {

        // In on demand mode, the baseline profile is regenerated when building
        // release and it's not saved in the module sources. To achieve this
        // we can create an intermediate folder for the profile and add the
        // generation task to src sets.
        project
            .layout
            .buildDirectory
            .dir("$INTERMEDIATES_BASE_FOLDER/$variantName/")
    } else {

        // In periodic mode the baseline profile generation is manually triggered.
        // The baseline profile is stored in the baseline profile sources for
        // the variant.
        project.providers.provider {
            project
                .layout
                .projectDirectory
                .dir("src/$variantName/$baselineProfileDir/")
        }
    }

@Suppress("UnstableApiUsage")
fun Variant.setBaselineProfileSrcSet(
    project: Project,
    baselineProfilesExtension: BaselineProfilesConsumerExtension,
    genBaselineProfilesTaskProvider: TaskProvider<GenerateBaselineProfileTask>,
) {

    if (baselineProfilesExtension.onDemandGeneration) {
        this.sources.baselineProfiles?.apply {
            addGeneratedSourceDirectory(
                genBaselineProfilesTaskProvider,
                GenerateBaselineProfileTask::baselineProfileDir
            )
        }
    } else {
        val baselineProfileSourcesFile = baselineProfilesExtension
            .baselineProfileOutputDir(project = project, variantName = name)
            .get()
            .asFile

        // If the folder does not exist it means that the profile has not been generated
        // so we don't need to add to sources.
        if (baselineProfileSourcesFile.exists()) {
            this.sources.baselineProfiles?.addStaticSourceDirectory(
                baselineProfileSourcesFile.absolutePath
            )
        }
    }
}