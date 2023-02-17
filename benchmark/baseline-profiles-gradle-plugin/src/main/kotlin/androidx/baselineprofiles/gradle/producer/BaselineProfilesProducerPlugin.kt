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

package androidx.baselineprofiles.gradle.producer

import androidx.baselineprofiles.gradle.utils.ATTRIBUTE_BUILD_TYPE
import androidx.baselineprofiles.gradle.utils.ATTRIBUTE_CATEGORY_BASELINE_PROFILE
import androidx.baselineprofiles.gradle.utils.ATTRIBUTE_FLAVOR
import androidx.baselineprofiles.gradle.utils.BUILD_TYPE_BASELINE_PROFILE_PREFIX
import androidx.baselineprofiles.gradle.utils.CONFIGURATION_NAME_BASELINE_PROFILES
import androidx.baselineprofiles.gradle.utils.INTERMEDIATES_BASE_FOLDER
import androidx.baselineprofiles.gradle.utils.camelCase
import androidx.baselineprofiles.gradle.utils.checkAgpVersion
import androidx.baselineprofiles.gradle.utils.createBuildTypeIfNotExists
import androidx.baselineprofiles.gradle.utils.createNonObfuscatedBuildTypes
import androidx.baselineprofiles.gradle.utils.isGradleSyncRunning
import com.android.build.api.dsl.TestBuildType
import com.android.build.api.variant.TestAndroidComponentsExtension
import com.android.build.gradle.TestExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.attributes.Category

/**
 * This is the producer plugin for baseline profile generation. In order to generate baseline
 * profiles three plugins are needed: one is applied to the app or the library that should consume
 * the baseline profile when building (consumer), one is applied to the project that should supply
 * the apk under test (build provider) and the last one is applied to a test module containing
 * the ui test that generate the baseline profile on the device (producer).
 */
class BaselineProfilesProducerPlugin : Plugin<Project> {

    companion object {
        private const val COLLECT_TASK_NAME = "collect"
    }

    override fun apply(project: Project) {
        var foundTestPlugin = false
        project.pluginManager.withPlugin("com.android.test") {
            foundTestPlugin = true
            configureWithAndroidPlugin(project = project)
        }

        // Only used to verify that the android test plugin has been applied.
        project.afterEvaluate {
            if (!project.isGradleSyncRunning()) {
                if (!foundTestPlugin) {
                    throw IllegalStateException(
                        """
                    The module ${project.name} does not have the `com.android.test` plugin
                    applied. The `androidx.baselineprofiles.producer` plugin supports only android
                    test modules. Please review your build.gradle to ensure this plugin is applied
                    to the correct module.
                    """.trimIndent()
                    )
                }
                project.logger.debug(
                    "[BaselineProfilesProducerPlugin] afterEvaluate check: app plugin was applied"
                )
            }
        }
    }

    private fun configureWithAndroidPlugin(project: Project) {

        // Checks that the required AGP version is applied to this project.
        project.checkAgpVersion()

        // Prepares extensions used by the plugin
        val baselineProfilesExtension =
            BaselineProfilesProducerExtension.registerExtension(project)

        val testAndroidComponent = project.extensions.getByType(
            TestAndroidComponentsExtension::class.java
        )

        // We need the instrumentation apk to run as a separate process
        val testExtension = project.extensions.getByType(TestExtension::class.java)
        testExtension.experimentalProperties["android.experimental.self-instrumenting"] = true

        // Creates the new build types to match the build provider. Note that release does not
        // exist by default so we need to create nonMinifiedRelease and map it manually to
        // `release`. All the existing build types beside `debug`, that is the default one, are
        // added manually in the configuration so we can assume they've been added for the purpose
        // of generating baseline profiles. We don't need to create a nonMinified build type from
        // `debug` since there will be no matching configuration with the apk provider module.

        val nonObfuscatedReleaseName = camelCase(BUILD_TYPE_BASELINE_PROFILE_PREFIX, "release")
        val extendedTypeToOriginalTypeMapping = mutableMapOf(nonObfuscatedReleaseName to "release")

        testAndroidComponent.finalizeDsl { ext ->

            // The test build types need to be debuggable and have the same singing config key to
            // be installed. We also disable the test coverage tracking since it's not important
            // here.
            val configureBlock: TestBuildType.() -> (Unit) = {
                isDebuggable = true
                enableAndroidTestCoverage = false
                enableUnitTestCoverage = false
                signingConfig = ext.buildTypes.getByName("debug").signingConfig
                matchingFallbacks += listOf("release")
            }

            // The variant names are used by the test module to request a specific apk artifact to
            // the under test app module (using configuration attributes). This is all handled by
            // the com.android.test plugin, as long as both modules have the same variants.
            // Unfortunately the test module cannot determine which variants are present in the
            // under test app module. As a result we need to replicate the same build types and
            // flavors, so that the same variant names are created.
            createNonObfuscatedBuildTypes(
                project = project,
                extension = ext,
                extendedBuildTypeToOriginalBuildTypeMapping = extendedTypeToOriginalTypeMapping,
                configureBlock = configureBlock,
                filterBlock = {
                    // All the build types that have been added to the test module be extended.
                    // This is because we can't know here which ones are actually release in the
                    // under test module.
                    it.name != "debug"
                },
            )
            createBuildTypeIfNotExists(
                project = project,
                extension = ext,
                buildTypeName = nonObfuscatedReleaseName,
                configureBlock = configureBlock
            )
        }

        // Makes sure that only the non obfuscated build type variant selected is enabled
        testAndroidComponent.apply {
            beforeVariants {
                it.enable = it.buildType in extendedTypeToOriginalTypeMapping.keys
            }
        }

        // Creates all the configurations, one per variant for the newly created build type.
        // Note that for this version of the plugin is not possible to rely entirely on the variant
        // api so the actual creation of the tasks is postponed to be executed when all the
        // agp tasks have been created, using the old api.
        val createTaskBlocks = mutableListOf<() -> (Unit)>()
        testAndroidComponent.apply {

            onVariants {

                // Creating configurations only for the extended build types.
                if (it.buildType == null ||
                    it.buildType !in extendedTypeToOriginalTypeMapping.keys
                ) {
                    return@onVariants
                }

                val flavorName =
                    if (it.flavorName == null || it.flavorName!!.isEmpty()) null else it.flavorName

                // Creates the configuration to handle this variant. Note that in the attributes
                // to match the configuration we use the original build type without `nonObfuscated`.
                val originalBuildTypeName = extendedTypeToOriginalTypeMapping[it.buildType] ?: ""
                val configurationName = createBaselineProfileConfigurationForVariant(
                    project = project,
                    variantName = it.name,
                    flavorName = flavorName,
                    originalBuildTypeName = originalBuildTypeName
                )

                // Prepares a block to execute later that creates the tasks for this variant
                createTaskBlocks.add {
                    createTasksForVariant(
                        project = project,
                        variantName = it.name,
                        flavorName = flavorName,
                        configurationName = configurationName,
                        baselineProfilesExtension = baselineProfilesExtension
                    )
                }
            }
        }

        // After variants have been resolved and the AGP tasks have been created, create the plugin
        // tasks.
        var applied = false
        testExtension.applicationVariants.all {
            if (applied) return@all
            applied = true
            createTaskBlocks.forEach { it() }
        }
    }

    private fun createTasksForVariant(
        project: Project,
        variantName: String,
        flavorName: String?,
        configurationName: String,
        baselineProfilesExtension: BaselineProfilesProducerExtension
    ) {

        // Prepares the devices list to use to generate baseline profiles.
        val devices = mutableSetOf<String>()
            .also { it.addAll(baselineProfilesExtension.managedDevices) }
        if (baselineProfilesExtension.useConnectedDevices) {
            devices.add("connected")
        }

        // Determines which test tasks should run based on configuration
        val shouldExpectConnectedOutput = devices.contains("connected")
        val shouldExpectManagedOutput = baselineProfilesExtension.managedDevices.isNotEmpty()

        // The test task runs the ui tests
        val testTasks = devices.map {
            try {
                project.tasks.named(camelCase(it, variantName, "androidTest")).apply {
                    configure { t ->
                        // TODO: this is a bit hack-ish but we can rewrite if we decide to keep the
                        //  configuration [BaselineProfilesProducerExtension.enableEmulatorDisplay]
                        if (t.hasProperty("enableEmulatorDisplay")) {
                            t.setProperty(
                                "enableEmulatorDisplay",
                                baselineProfilesExtension.enableEmulatorDisplay
                            )
                        }
                    }
                }
            } catch (e: UnknownTaskException) {

                // If gradle is syncing don't throw any exception and simply stop here. This plugin
                // will fail at build time instead. This allows not breaking project sync in ide.
                if (project.isGradleSyncRunning()) {
                    return
                }

                throw GradleException(
                    """
                    It wasn't possible to determine the test task for managed device `$it`.
                    Please check the managed devices specified in the baseline profiles
                    configuration.
                """.trimIndent(), e
                )
            }
        }

        // The collect task collects the baseline profile files from the ui test results
        val collectTaskProvider = project.tasks.register(
            camelCase(COLLECT_TASK_NAME, variantName, "BaselineProfiles"),
            CollectBaselineProfilesTask::class.java
        ) {

            // Test tasks have to run before collect
            it.dependsOn(testTasks)

            // Merge result protos task may not exist depending on gradle version
            val mergeTaskName = camelCase("merge", variantName, "testResultProtos")
            try {
                it.dependsOn(project.tasks.named(mergeTaskName))
            } catch (e: UnknownTaskException) {
                // Nothing to do.
                project.logger.info("Task $mergeTaskName does not exist.")
            }

            // Sets flavor name
            it.outputFile.set(
                project
                    .layout
                    .buildDirectory
                    .file("$INTERMEDIATES_BASE_FOLDER/$flavorName/baseline-prof.txt")
            )

            // Sets the connected test results location, if tests are supposed to run also on
            // connected devices.
            if (shouldExpectConnectedOutput) {
                it.connectedAndroidTestOutputDir.set(
                    if (flavorName == null) {
                        project.layout.buildDirectory
                            .dir("outputs/androidTest-results/connected")
                    } else {
                        project.layout.buildDirectory
                            .dir("outputs/androidTest-results/connected/flavors/$flavorName")
                    }
                )
            }

            // Sets the managed devices test results location, if tests are supposed to run
            // also on managed devices.
            if (shouldExpectManagedOutput) {
                it.managedAndroidTestOutputDir.set(
                    if (flavorName == null) {
                        project.layout.buildDirectory.dir(
                            "outputs/androidTest-results/managedDevice"
                        )
                    } else {
                        project.layout.buildDirectory.dir(
                            "outputs/androidTest-results/managedDevice/flavors/$flavorName"
                        )
                    }
                )
            }
        }

        // The artifacts are added to the configuration that exposes the generated baseline profile
        project.artifacts { artifactHandler ->
            artifactHandler.add(configurationName, collectTaskProvider) { artifact ->
                artifact.builtBy(collectTaskProvider)
            }
        }
    }

    private fun createBaselineProfileConfigurationForVariant(
        project: Project,
        variantName: String,
        flavorName: String?,
        originalBuildTypeName: String,
    ): String {
        val configurationName =
            camelCase(variantName, CONFIGURATION_NAME_BASELINE_PROFILES)
        project.configurations
            .maybeCreate(configurationName)
            .apply {
                isCanBeResolved = false
                isCanBeConsumed = true
                attributes {

                    // Main specialized attribute
                    it.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.objects.named(
                            Category::class.java,
                            ATTRIBUTE_CATEGORY_BASELINE_PROFILE
                        )
                    )

                    // Build type
                    it.attribute(ATTRIBUTE_BUILD_TYPE, originalBuildTypeName)

                    // Flavor if existing
                    if (flavorName != null) {
                        it.attribute(ATTRIBUTE_FLAVOR, flavorName)
                    }
                }
            }
        return configurationName
    }
}
