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
import androidx.baselineprofiles.gradle.utils.camelCase
import androidx.baselineprofiles.gradle.utils.createBuildTypeIfNotExists
import androidx.baselineprofiles.gradle.utils.createNonObfuscatedBuildTypes
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
 * the apk under test (build provider) and the last one is applied to a library module containing
 * the ui test that generate the baseline profile on the device (producer).
 */
class BaselineProfilesProducerPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.test") {
            configureWithAndroidPlugin(project = project)
        }
    }

    private fun configureWithAndroidPlugin(project: Project) {

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
        // exist by default so we need to create nonObfuscatedRelease and map it manually to
        // `release`. All the existing build types beside `debug`, that is the default one, are
        // added manually in the configuration so we can assume they've been added for the purpose
        // of generating baseline profiles. We don't need to create a nonObfuscated build type from
        // `debug`.

        val nonObfuscatedReleaseName = camelCase(BUILD_TYPE_BASELINE_PROFILE_PREFIX, "release")
        val extendedTypeToOriginalTypeMapping = mutableMapOf(nonObfuscatedReleaseName to "release")

        testAndroidComponent.finalizeDsl { ext ->

            createNonObfuscatedBuildTypes(
                project = project,
                extension = ext,
                extendedBuildTypeToOriginalBuildTypeMapping = extendedTypeToOriginalTypeMapping,
                filterBlock = {
                    // TODO: Which build types to skip. In theory we want to skip only debug because
                    //  it's the default one. All the ones that have been manually added should be
                    //  considered for this.
                    it.name != "debug"
                },
                configureBlock = {
                    enableAndroidTestCoverage = false
                    enableUnitTestCoverage = false
                },
            )

            createBuildTypeIfNotExists(
                project = project,
                extension = ext,
                buildTypeName = nonObfuscatedReleaseName,
                configureBlock = {
                    enableAndroidTestCoverage = false
                    enableUnitTestCoverage = false
                    matchingFallbacks += listOf("release")
                }
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
                    it.buildType !in extendedTypeToOriginalTypeMapping.keys) {
                    return@onVariants
                }

                // Creates the configuration to handle this variant. Note that in the attributes
                // to match the configuration we use the original build type without `nonObfuscated`.
                val originalBuildTypeName = extendedTypeToOriginalTypeMapping[it.buildType] ?: ""
                val configurationName = createBaselineProfileConfigurationForVariant(
                    project = project,
                    variantName = it.name,
                    flavorName = it.flavorName ?: "",
                    originalBuildTypeName = originalBuildTypeName
                )

                // Prepares a block to execute later that creates the tasks for this variant
                createTaskBlocks.add {
                    createTasksForVariant(
                        project = project,
                        variantName = it.name,
                        flavorName = it.flavorName ?: "",
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
        flavorName: String,
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
                project.tasks.named(camelCase(it, variantName, "androidTest"))
            } catch (e: UnknownTaskException) {
                throw GradleException(
                    """
                    It wasn't possible to determine the test task for managed device `$it`.
                    Please check the managed devices specified in the baseline profiles configuration.
                """.trimIndent(), e
                )
            }
        }

        // Merge result protos task
        val mergeResultProtosTask = project.tasks.named(
            camelCase("merge", variantName, "testResultProtos")
        )

        // The collect task collects the baseline profile files from the ui test results
        val collectTaskProvider = project.tasks.register(
            camelCase("collect", variantName, "BaselineProfiles"),
            CollectBaselineProfilesTask::class.java
        ) {

            // Test tasks have to run before collect
            it.dependsOn(testTasks, mergeResultProtosTask)

            // Sets flavor name
            it.outputFile.set(
                project
                    .layout
                    .buildDirectory
                    .file("intermediates/baselineprofiles/$flavorName/baseline-prof.txt")
            )

            // Sets the connected test results location, if tests are supposed to run also on
            // connected devices.
            if (shouldExpectConnectedOutput) {
                it.connectedAndroidTestOutputDir.set(
                    if (flavorName.isEmpty()) {
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
                    if (flavorName.isEmpty()) {
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
        flavorName: String,
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
                    it.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.objects.named(
                            Category::class.java,
                            ATTRIBUTE_CATEGORY_BASELINE_PROFILE
                        )
                    )
                    it.attribute(
                        ATTRIBUTE_BUILD_TYPE,
                        originalBuildTypeName
                    )
                    it.attribute(
                        ATTRIBUTE_FLAVOR,
                        flavorName
                    )
                }
            }
        return configurationName
    }
}
