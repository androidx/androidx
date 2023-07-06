/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.baselineprofile.gradle.consumer

import androidx.baselineprofile.gradle.configuration.ConfigurationManager
import androidx.baselineprofile.gradle.consumer.task.MainGenerateBaselineProfileTask
import androidx.baselineprofile.gradle.consumer.task.MergeBaselineProfileTask
import androidx.baselineprofile.gradle.consumer.task.PrintConfigurationForVariantTask
import androidx.baselineprofile.gradle.consumer.task.PrintMapPropertiesForVariantTask
import androidx.baselineprofile.gradle.consumer.task.maybeCreateGenerateTask
import androidx.baselineprofile.gradle.utils.AgpFeature
import androidx.baselineprofile.gradle.utils.AgpPlugin
import androidx.baselineprofile.gradle.utils.AgpPluginId
import androidx.baselineprofile.gradle.utils.BUILD_TYPE_BASELINE_PROFILE_PREFIX
import androidx.baselineprofile.gradle.utils.BUILD_TYPE_BENCHMARK_PREFIX
import androidx.baselineprofile.gradle.utils.CONFIGURATION_NAME_BASELINE_PROFILES
import androidx.baselineprofile.gradle.utils.INTERMEDIATES_BASE_FOLDER
import androidx.baselineprofile.gradle.utils.KOTLIN_MULTIPLATFORM_PLUGIN_ID
import androidx.baselineprofile.gradle.utils.KotlinMultiPlatformUtils
import androidx.baselineprofile.gradle.utils.MAX_AGP_VERSION_REQUIRED
import androidx.baselineprofile.gradle.utils.MIN_AGP_VERSION_REQUIRED
import androidx.baselineprofile.gradle.utils.R8Utils
import androidx.baselineprofile.gradle.utils.RELEASE
import androidx.baselineprofile.gradle.utils.camelCase
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.Variant
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration

/**
 * This is the consumer plugin for baseline profile generation. In order to generate baseline
 * profiles three plugins are needed: one is applied to the app or the library that should consume
 * the baseline profile when building (consumer), one is applied to the module that should supply
 * the under test app (app target) and the last one is applied to a test module containing the ui
 * test that generate the baseline profile on the device (producer).
 */
class BaselineProfileConsumerPlugin : Plugin<Project> {
    override fun apply(project: Project) = BaselineProfileConsumerAgpPlugin(project).onApply()
}

private class BaselineProfileConsumerAgpPlugin(private val project: Project) : AgpPlugin(
    project = project,
    supportedAgpPlugins = setOf(
        AgpPluginId.ID_ANDROID_APPLICATION_PLUGIN,
        AgpPluginId.ID_ANDROID_LIBRARY_PLUGIN
    ),
    minAgpVersion = MIN_AGP_VERSION_REQUIRED,
    maxAgpVersion = MAX_AGP_VERSION_REQUIRED
) {

    // List of the non debuggable build types
    private val nonDebuggableBuildTypes = mutableListOf<String>()

    // Offers quick access to configuration extension, hiding the property override and merge logic
    private val perVariantBaselineProfileExtensionManager =
        PerVariantConsumerExtensionManager(BaselineProfileConsumerExtension.register(project))

    // Manages creation of configurations
    private val configurationManager = ConfigurationManager(project)

    // Manages r8 properties
    private val r8Utils = R8Utils(project)

    // Global baseline profile configuration. Note that created here it can be directly consumed
    // in the dependencies block.
    private val mainBaselineProfileConfiguration = configurationManager.maybeCreate(
        nameParts = listOf(CONFIGURATION_NAME_BASELINE_PROFILES),
        canBeConsumed = false,
        canBeResolved = true,
        buildType = null,
        productFlavors = null
    )

    private val Variant.benchmarkVariantName: String
        get() {
            val parts = listOfNotNull(flavorName, BUILD_TYPE_BENCHMARK_PREFIX, buildType)
                .filter { it.isNotBlank() }
            return camelCase(*parts.toTypedArray())
        }

    override fun onAgpPluginNotFound(pluginIds: Set<AgpPluginId>) {
        throw IllegalStateException(
            """
            The module ${project.name} does not have the `com.android.application` or
            `com.android.library` plugin applied. The `androidx.baselineprofile.consumer`
            plugin supports only android application and library modules. Please review
            your build.gradle to ensure this plugin is applied to the correct module.
            """.trimIndent()
        )
    }

    override fun onAgpPluginFound(pluginIds: Set<AgpPluginId>) {
        project.logger.debug(
            """
            [BaselineProfileConsumerPlugin] afterEvaluate check: app or library plugin was applied
            """.trimIndent()
        )
    }

    override fun onApplicationFinalizeDsl(extension: ApplicationExtension) {

        // Here we select the build types we want to process if this is an application,
        // i.e. non debuggable build types that have not been created by the app target plugin.
        // Also exclude the build types starting with baseline profile prefix, in case the app
        // target plugin is also applied.

        nonDebuggableBuildTypes.addAll(extension.buildTypes
            .filter {
                !it.isDebuggable &&
                    !it.name.startsWith(BUILD_TYPE_BASELINE_PROFILE_PREFIX) &&
                    !it.name.startsWith(BUILD_TYPE_BENCHMARK_PREFIX)
            }
            .map { it.name }
        )
    }

    override fun onLibraryFinalizeDsl(extension: LibraryExtension) {

        // Here we select the build types we want to process if this is a library.
        // Libraries don't have a `debuggable` flag. Also we don't need to exclude build types
        // prefixed with the baseline profile prefix. Ideally on the `debug` type should be
        // excluded.

        nonDebuggableBuildTypes.addAll(extension.buildTypes
            .filter { it.name != "debug" }
            .map { it.name }
        )
    }

    @Suppress("UnstableApiUsage")
    override fun onVariants(variant: Variant) {

        // Process only the non debuggable build types we previously selected.
        if (variant.buildType !in nonDebuggableBuildTypes) return

        // This allows quick access to this variant configuration according to the override
        // and merge rules implemented in the PerVariantConsumerExtensionManager.
        val variantConfiguration = perVariantBaselineProfileExtensionManager.variant(variant)

        // For test only: this registers a print task with the configuration of the variant.
        PrintConfigurationForVariantTask.registerForVariant(
            project = project,
            variant = variant,
            variantConfig = variantConfiguration
        )
        PrintMapPropertiesForVariantTask.registerForVariant(
            project = project,
            variant = variant
        )

        // Sets the r8 rewrite baseline profile for the non debuggable variant.
        variantConfiguration.baselineProfileRulesRewrite?.let {
            r8Utils.setRulesRewriteForVariantEnabled(variant, it)
        }

        // Sets the r8 startup dex optimization profile for the non debuggable variant.
        variantConfiguration.dexLayoutOptimization?.let {
            r8Utils.setDexLayoutOptimizationEnabled(variant, it)
        }

        // Check if this variant has any direct dependency
        val variantDependencies = variantConfiguration.dependencies

        // Creates the configuration to carry the specific variant artifact
        val baselineProfileConfiguration = createConfigurationForVariant(
            variant = variant,
            mainConfiguration = mainBaselineProfileConfiguration
        )

        // Adds the custom dependencies for baseline profiles. Note that dependencies
        // for global, build type, flavor and variant specific are all merged.
        variantDependencies.forEach {
            val targetProjectDependency = project.dependencyFactory.create(it)
            baselineProfileConfiguration.dependencies.add(targetProjectDependency)
        }

        // There are 2 different ways in which the output task can merge the baseline
        // profile rules, according to [BaselineProfileConsumerExtension#mergeIntoMain].
        // When mergeIntoMain is `true` the first variant will create a task shared across
        // all the variants to merge, while the next variants will simply add the additional
        // baseline profile artifacts, modifying the existing task.
        // When mergeIntoMain is `false` each variants has its own task with a single
        // artifact per task, specific for that variant.
        // When mergeIntoMain is not specified, it's by default true for libraries and false
        // for apps.
        val mergeIntoMain = variantConfiguration.mergeIntoMain ?: isLibraryModule()

        // Determines the target name for the Android target in kotlin multiplatform projects.
        // Note that KotlinMultiPlatformUtils references the kmp extension that exists only if the
        // multiplatform plugin has been applied.
        val androidTargetName =
            if (project.plugins.hasPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID)) {
                KotlinMultiPlatformUtils.androidTargetName(project)
            } else {
                ""
            }

        // This part changes according to the AGP version of the module. `mergeIntoMain` merges
        // the profile of the generated profile for this variant into the main one. This can be
        // applied to the `main` configuration or to single variants. On Agp 8.0, since it's not
        // possible to run tests on multiple build types in the same run, when `mergeIntoMain` is
        // true only variants of the specific build type invoked are merged. This means that on
        // AGP 8.0 the `main` baseline profile is generated by only the build type `release` when
        // calling `generateReleaseBaselineProfiles`. On Agp 8.1 instead, it works as intended and
        // we can merge all the variants with `mergeIntoMain` true, independently from the build
        // type.
        data class TaskAndFolderName(
            val taskVariantName: String,
            val folderVariantName: String
        )
        val (mergeAwareTaskName, mergeAwareVariantOutput) = if (mergeIntoMain) {
            if (supportsFeature(AgpFeature.TEST_MODULE_SUPPORTS_MULTIPLE_BUILD_TYPES)) {
                TaskAndFolderName(
                    taskVariantName = "",
                    folderVariantName = camelCase(androidTargetName, "main")
                )
            } else {
                // Note that the exception here cannot happen because all the variants have a build
                // type in Android.
                TaskAndFolderName(
                    taskVariantName = variant.buildType
                        ?: throw IllegalStateException("Found variant without build type."),
                    folderVariantName = camelCase(androidTargetName, "main")
                )
            }
        } else {
            TaskAndFolderName(
                taskVariantName = variant.name,
                folderVariantName = camelCase(androidTargetName, variant.name)
            )
        }

        // Creates the task to merge the baseline profile artifacts coming from
        // different configurations.
        val mergedTaskOutputDir = project
            .layout
            .buildDirectory
            .dir("$INTERMEDIATES_BASE_FOLDER/$mergeAwareVariantOutput/merged")

        val mergeTaskProvider = MergeBaselineProfileTask.maybeRegisterForMerge(
            project = project,
            variantName = variant.name,
            mergeAwareTaskName = mergeAwareTaskName,
            hasDependencies = baselineProfileConfiguration.allDependencies.isNotEmpty(),
            sourceProfilesFileCollection = baselineProfileConfiguration,
            outputDir = mergedTaskOutputDir,
            filterRules = variantConfiguration.filterRules,
            library = isLibraryModule(),

            // Note that the merge task is the last task only if saveInSrc is disabled. When
            // saveInSrc is enabled an additional task is created to copy the profile in the sources
            // folder.
            isLastTask = !variantConfiguration.saveInSrc
        )

        // If `saveInSrc` is true, we create an additional task to copy the output
        // of the merge task in the src folder.
        val lastTaskProvider = if (variantConfiguration.saveInSrc) {

            val baselineProfileOutputDir = perVariantBaselineProfileExtensionManager
                .variant(variant)
                .baselineProfileOutputDir
            val srcOutputDir = project
                .layout
                .projectDirectory
                .dir("src/$mergeAwareVariantOutput/$baselineProfileOutputDir/")

            // This task copies the baseline profile generated from the merge task.
            // Note that we're reutilizing the [MergeBaselineProfileTask] because
            // if the flag `mergeIntoMain` is true tasks will have the same name
            // and we just want to add more file to copy to the same output. This is
            // already handled in the MergeBaselineProfileTask.
            val copyTaskProvider = MergeBaselineProfileTask.maybeRegisterForCopy(
                project = project,
                variantName = variant.name,
                mergeAwareTaskName = mergeAwareTaskName,
                library = isLibraryModule(),
                sourceDir = mergeTaskProvider.flatMap { it.baselineProfileDir },
                outputDir = project.provider { srcOutputDir },
                isLastTask = true
            )

            // Applies the source path for this variant
            srcOutputDir.asFile.apply {
                mkdirs()
                variant
                    .sources
                    .baselineProfiles?.addStaticSourceDirectory(absolutePath)
            }

            // If this is an application, we need to ensure that:
            // If `automaticGenerationDuringBuild` is true, building a release build
            // should trigger the generation of the profile. This is done through a
            // dependsOn rule.
            // If `automaticGenerationDuringBuild` is false and the user calls both
            // tasks to generate and assemble, assembling the release should wait of the
            // generation to be completed. This is done through a `mustRunAfter` rule.
            // Depending on whether the flag `automaticGenerationDuringBuild` is enabled
            // Note that we cannot use the variant src set api
            // `addGeneratedSourceDirectory` since that overwrites the outputDir,
            // that would be re-set in the build dir.
            // Also this is specific for applications: doing this for a library would
            // trigger a circular task dependency since the library would require
            // the profile in order to build the aar for the sample app and generate
            // the profile.
            if (isApplicationModule()) {

                // Sets the task dependency according to the configuration flag.
                val automaticGeneration = perVariantBaselineProfileExtensionManager
                    .variant(variant)
                    .automaticGenerationDuringBuild

                // Defines a function to apply the baseline profile source sets to a variant.
                val applySourceSetsFunc: (String) -> (Unit) = { variantName ->
                    project
                        .tasks
                        .named(camelCase("merge", variantName, "artProfile"))
                        .configure { t ->

                            // TODO: this causes a circular task dependency when the producer points
                            //  to a consumer that does not have the appTarget plugin. (b/272851616)
                            if (automaticGeneration) {
                                t.dependsOn(copyTaskProvider)
                            } else {
                                t.mustRunAfter(copyTaskProvider)
                            }
                        }
                }

                afterVariants {

                    // Apply the source sets to the variant.
                    applySourceSetsFunc(variant.name)

                    // Apply the source sets to the benchmark variant if supported.
                    if (supportsFeature(AgpFeature.TEST_MODULE_SUPPORTS_MULTIPLE_BUILD_TYPES)) {
                        applySourceSetsFunc(variant.benchmarkVariantName)
                    }
                }
            }

            // In this case the last task is the copy task.
            copyTaskProvider
        } else {

            if (variantConfiguration.automaticGenerationDuringBuild) {

                // If the flag `automaticGenerationDuringBuild` is true, we can set the
                // merge task to provide generated sources for the variant, using the
                // src set variant api. This means that we don't need to manually depend
                // on the merge or prepare art profile task.

                // Defines a function to apply the baseline profile source sets to a variant.
                val applySourceSetsFunc: (Variant) -> (Unit) = { v ->
                    v.sources.baselineProfiles?.addGeneratedSourceDirectory(
                        taskProvider = mergeTaskProvider,
                        wiredWith = MergeBaselineProfileTask::baselineProfileDir
                    )
                }

                // Apply the source sets to the variant.
                applySourceSetsFunc(variant)

                // Apply the source sets to the benchmark variant if supported and this the
                // consumer is an app (libraries don't have benchmark type).
                if (isApplicationModule() &&
                    supportsFeature(AgpFeature.TEST_MODULE_SUPPORTS_MULTIPLE_BUILD_TYPES)
                ) {

                    // Note that there is no way to access directly a specific variant, so we
                    // schedule a callback for later, when the variant is processed. Note that
                    // because the benchmark build type is created after the baseline profile
                    // build type, its variants will also come after the ones for baseline profile.
                    onVariant(variant.benchmarkVariantName) { v: ApplicationVariant ->
                        applySourceSetsFunc(v)
                    }
                }
            } else {

                // This is the case of `saveInSrc` and `automaticGenerationDuringBuild`
                // both false, that is unsupported. In this case we simply throw an
                // error.
                if (!isGradleSyncRunning()) {
                    throw GradleException(
                        """
                The current configuration of flags `saveInSrc` and `automaticGenerationDuringBuild`
                is not supported. At least one of these should be set to `true`. Please review your
                baseline profile plugin configuration in your build.gradle.
                    """.trimIndent()
                    )
                }
            }

            // In this case the last task is the merge task.
            mergeTaskProvider
        }

        // Here we create the final generate task that triggers the whole generation for this
        // variant and all the parent tasks. For this one the child task is either copy or merge,
        // depending on the configuration.
        maybeCreateGenerateTask<Task>(
            project = project,
            variantName = mergeAwareTaskName,
            lastTaskProvider = lastTaskProvider
        )

        // Create the build type task. For example `generateReleaseBaselineProfile`
        // The variant name is equal to the build type name if there are no flavors.
        // Note that if `mergeIntoMain` is `true` the build type task already exists.
        if (!mergeIntoMain &&
            !variant.buildType.isNullOrBlank() &&
            variant.buildType != variant.name
        ) {
            maybeCreateGenerateTask<Task>(
                project = project,
                variantName = variant.buildType!!,
                lastTaskProvider = lastTaskProvider
            )
        }

        if (supportsFeature(AgpFeature.TEST_MODULE_SUPPORTS_MULTIPLE_BUILD_TYPES)) {

            // Generate a flavor task for the assembled flavor name and each flavor dimension.
            // For example for variant `freeRelease` (build type `release`, flavor `free`):
            // `generateFreeBaselineProfile`.
            // For example for variant `freeRedRelease` (build type `release`, flavor dimensions
            // `free` and `red`): `generateFreeBaselineProfile`, `generateRedBaselineProfile` and
            // `generateFreeRedBaselineProfile`.
            if (!mergeIntoMain) {
                listOfNotNull(
                    variant.flavorName,
                    *variant.productFlavors.map { it.second }.toTypedArray()
                )
                    .filter { it != variant.name && it.isNotBlank() }
                    .toSet()
                    .forEach {
                        maybeCreateGenerateTask<Task>(
                            project = project,
                            variantName = it,
                            lastTaskProvider = lastTaskProvider
                        )
                    }
            }

            // Generate the main global tasks `generateBaselineProfile
            maybeCreateGenerateTask<Task>(
                project = project,
                variantName = "",
                lastTaskProvider = lastTaskProvider
            )
        } else {
            // Due to b/265438201 we cannot have a global task `generateBaselineProfile` that
            // triggers generation for all the variants when there are multiple build types.
            // So for version of AGP that don't support that, invoking `generateBaselineProfile`
            // will run generation for `release` build type only, that is the same behavior of
            // `generateReleaseBaselineProfile`. For this same reason we cannot have a flavor
            // task, such as `generateFreeBaselineProfile` because that would run generation for
            // all the build types with flavor free, that is not as well supported.
            if (variant.buildType == RELEASE) {
                maybeCreateGenerateTask<MainGenerateBaselineProfileTask>(
                    project = project,
                    variantName = "",
                    lastTaskProvider = lastTaskProvider
                )
            }
        }
    }

    private fun createConfigurationForVariant(
        variant: Variant,
        mainConfiguration: Configuration
    ) = configurationManager.maybeCreate(
        nameParts = listOf(variant.name, CONFIGURATION_NAME_BASELINE_PROFILES),
        canBeResolved = true,
        canBeConsumed = false,
        extendFromConfigurations = listOf(mainConfiguration),
        buildType = variant.buildType ?: "",
        productFlavors = variant.productFlavors
    )
}
