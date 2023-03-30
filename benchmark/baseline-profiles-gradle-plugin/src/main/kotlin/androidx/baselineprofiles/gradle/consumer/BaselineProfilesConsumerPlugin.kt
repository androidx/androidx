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
import androidx.baselineprofiles.gradle.utils.BUILD_TYPE_BASELINE_PROFILE_PREFIX
import androidx.baselineprofiles.gradle.utils.CONFIGURATION_NAME_BASELINE_PROFILES
import androidx.baselineprofiles.gradle.utils.INTERMEDIATES_BASE_FOLDER
import androidx.baselineprofiles.gradle.utils.camelCase
import androidx.baselineprofiles.gradle.utils.checkAgpVersion
import androidx.baselineprofiles.gradle.utils.isGradleSyncRunning
import androidx.baselineprofiles.gradle.utils.maybeRegister
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Category
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

/**
 * This is the consumer plugin for baseline profile generation. In order to generate baseline
 * profiles three plugins are needed: one is applied to the app or the library that should consume
 * the baseline profile when building (consumer), one is applied to the project that should supply
 * the apk under test (apk provider) and the last one is applied to a test module containing
 * the ui test that generate the baseline profile on the device (producer).
 */
class BaselineProfilesConsumerPlugin : Plugin<Project> {

    companion object {
        private const val GENERATE_TASK_NAME = "generate"
        private const val RELEASE = "release"
    }

    override fun apply(project: Project) {
        var foundAppOrLibraryPlugin = false
        project.pluginManager.withPlugin("com.android.application") {
            foundAppOrLibraryPlugin = true
            configureWithAndroidPlugin(project = project, isApplication = true)
        }
        project.pluginManager.withPlugin("com.android.library") {
            foundAppOrLibraryPlugin = true
            configureWithAndroidPlugin(project = project, isApplication = false)
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

    private fun configureWithAndroidPlugin(project: Project, isApplication: Boolean) {

        // Checks that the required AGP version is applied to this project.
        project.checkAgpVersion()

        // Prepares extensions used by the plugin

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

        // If that's the case, we check that we're generating for a non debuggable build type.
        val buildTypes = mutableListOf<String>()

        // Determines which build types are available if this module is an application.
        // This extension exists only if the module is an application.
        project
            .extensions
            .findByType(ApplicationAndroidComponentsExtension::class.java)
            ?.finalizeDsl { ext ->
                buildTypes.addAll(ext.buildTypes
                    .filter {
                        !it.isDebuggable &&
                            !it.name.startsWith(BUILD_TYPE_BASELINE_PROFILE_PREFIX)
                    }
                    .map { it.name })
            }

        // Determines which build types are available if this module is an application.
        // This extension exists only if the module is a library.
        project
            .extensions
            .findByType(LibraryAndroidComponentsExtension::class.java)
            ?.finalizeDsl { ext ->
                buildTypes.addAll(ext.buildTypes
                    .filter {

                        // Note that library build types don't have a `debuggable` flag.
                        it.name != "debug" &&
                            !it.name.startsWith(BUILD_TYPE_BASELINE_PROFILE_PREFIX)
                    }
                    .map { it.name })
            }

        // Iterate variants to create per-variant tasks and configurations. Note that the src set
        // api for variants are marked as unstable but there is no other way to do this.
        @Suppress("UnstableApiUsage")
        project
            .extensions
            .getByType(AndroidComponentsExtension::class.java)
            .apply {
                onVariants { variant ->

                    if (variant.buildType !in buildTypes) {
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

                    // There are 2 different ways in which the output task can merge the baseline
                    // profile rules, according to [BaselineProfilesConsumerExtension#mergeIntoMain].
                    // When mergeIntoMain is `true` the first variant will create a task shared across
                    // all the variants to merge, while the next variants will simply add the additional
                    // baseline profile artifacts, modifying the existing task.
                    // When mergeIntoMain is `false` each variants has its own task with a single
                    // artifact per task, specific for that variant.
                    // When mergeIntoMain is not specified, it's by default true for libraries and false
                    // for apps.
                    val mergeIntoMain = baselineProfilesExtension.mergeIntoMain ?: !isApplication

                    // TODO: When `mergeIntoMain` is true it lazily triggers the generation of all
                    //  the variants for all the build types. Due to b/265438201, that fails when
                    //  there are multiple build types. As temporary workaround, when `mergeIntoMain`
                    //  is true, calling a generation task for a specific build type will merge
                    //  profiles for all the variants of that build type and output it in the `main`
                    //  folder.
                    val (taskName, outputVariantFolder) = if (mergeIntoMain) {
                        listOf(variant.buildType ?: "", "main")
                    } else {
                        listOf(variant.name, variant.name)
                    }

                    // Creates the task to merge the baseline profile artifacts coming from different
                    // configurations. Note that this is the last task of the chain that triggers the
                    // whole generation, hence it's called `generate`. The name is generated according
                    // to the value of the `merge`.
                    val genBaselineProfilesTaskProvider = project
                        .tasks
                        .maybeRegister<GenerateBaselineProfileTask>(
                            GENERATE_TASK_NAME, taskName, "baselineProfiles",
                        ) { task ->

                            // These are all the configurations this task depends on,
                            // in order to consume their artifacts. Note that if this task already
                            // exist (for example if `merge` is `all`) the new artifact will be
                            // added to the existing list.
                            task.baselineProfileFileCollection
                                .from
                                .add(baselineProfileConfiguration)

                            // This is the task output for the generated baseline profile
                            task.baselineProfileDir.set(
                                baselineProfilesExtension.baselineProfileOutputDir(
                                    project = project,
                                    variantName = outputVariantFolder,
                                    outputDir = baselineProfilesExtension.baselineProfileOutputDir
                                )
                            )

                            // Sets the package filter rules. If this is the first task
                            task.filterRules.addAll(
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
                    if (baselineProfilesExtension.onDemandGeneration) {
                        variant.sources.baselineProfiles?.apply {
                            addGeneratedSourceDirectory(
                                genBaselineProfilesTaskProvider,
                                GenerateBaselineProfileTask::baselineProfileDir
                            )
                        }
                    } else {
                        val baselineProfileSourcesFile = baselineProfilesExtension
                            .baselineProfileOutputDir(
                                project = project,
                                variantName = outputVariantFolder,
                                outputDir = baselineProfilesExtension.baselineProfileOutputDir
                            )
                            .get()
                            .asFile

                        // If the folder does not exist it means that the profile has not been generated
                        // so we don't need to add to sources.
                        if (baselineProfileSourcesFile.exists()) {
                            variant.sources.baselineProfiles?.addStaticSourceDirectory(
                                baselineProfileSourcesFile.absolutePath
                            )
                        }
                    }

                    // Here we create a task hierarchy to trigger generations for all the variants
                    // of a specific build type, flavor or all of them. If `mergeIntoMain` is true,
                    // only one generation task exists so there is no need to create parent tasks.
                    if (!mergeIntoMain && variant.name != variant.buildType) {
                        maybeCreateParentGenTask<Task>(
                            project,
                            variant.buildType,
                            genBaselineProfilesTaskProvider
                        )
                    }

                    // TODO: Due to b/265438201 we cannot have a global task
                    //  `generateBaselineProfiles` that triggers generation for all the
                    //  variants when there are multiple build types. The temporary workaround
                    //  is to generate baseline profiles only for variants with the `release`
                    //  build type until that bug is fixed, when running the global task
                    //  `generateBaselineProfiles`. This can be removed after fix.
                    if (variant.buildType == RELEASE) {
                        maybeCreateParentGenTask<MainGenerateBaselineProfileTask>(
                            project,
                            "",
                            genBaselineProfilesTaskProvider
                        )
                    }
                }
            }
    }

    private inline fun <reified T : Task> maybeCreateParentGenTask(
        project: Project,
        parentName: String?,
        childGenerationTaskProvider: TaskProvider<GenerateBaselineProfileTask>
    ) {
        if (parentName == null) return
        project.tasks.maybeRegister<T>(GENERATE_TASK_NAME, parentName, "baselineProfiles") {
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

    private fun BaselineProfilesConsumerExtension.baselineProfileOutputDir(
        project: Project,
        outputDir: String,
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
                .dir("$INTERMEDIATES_BASE_FOLDER/$variantName/$outputDir")
        } else {

            // In periodic mode the baseline profile generation is manually triggered.
            // The baseline profile is stored in the baseline profile sources for
            // the variant.
            project.providers.provider {
                project
                    .layout
                    .projectDirectory
                    .dir("src/$variantName/$outputDir/")
            }
        }
}

@CacheableTask
abstract class MainGenerateBaselineProfileTask : DefaultTask() {

    @TaskAction
    fun exec() {
        this.logger.warn(
            """
                The task `generateBaselineProfiles` cannot currently support
                generation for all the variants when there are multiple build
                types without improvements planned for a future version of the
                Android Gradle Plugin.
                Until then, `generateBaselineProfiles` will only generate
                baseline profiles for the variants of the release build type,
                behaving like `generateReleaseBaselineProfiles`.
                If you intend to generate profiles for multiple build types
                you'll need to run separate gradle commands for each build type.
                For example: `generateReleaseBaselineProfiles` and
                `generateAnotherReleaseBaselineProfiles`.

                Details on https://issuetracker.google.com/issue?id=270433400.
                """.trimIndent()
        )
    }
}