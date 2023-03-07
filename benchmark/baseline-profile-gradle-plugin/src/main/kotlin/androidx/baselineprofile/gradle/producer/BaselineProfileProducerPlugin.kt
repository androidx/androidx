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

package androidx.baselineprofile.gradle.producer

import androidx.baselineprofile.gradle.utils.ATTRIBUTE_BASELINE_PROFILE_PLUGIN_VERSION
import androidx.baselineprofile.gradle.utils.ATTRIBUTE_USAGE_BASELINE_PROFILE
import androidx.baselineprofile.gradle.utils.ATTRIBUTE_TARGET_JVM_ENVIRONMENT
import androidx.baselineprofile.gradle.utils.BUILD_TYPE_BASELINE_PROFILE_PREFIX
import androidx.baselineprofile.gradle.attributes.BaselineProfilePluginVersionAttr
import androidx.baselineprofile.gradle.utils.CONFIGURATION_ARTIFACT_TYPE
import androidx.baselineprofile.gradle.utils.CONFIGURATION_NAME_BASELINE_PROFILES
import androidx.baselineprofile.gradle.utils.INTERMEDIATES_BASE_FOLDER
import androidx.baselineprofile.gradle.utils.TASK_NAME_SUFFIX
import androidx.baselineprofile.gradle.utils.agpVersionString
import androidx.baselineprofile.gradle.utils.camelCase
import androidx.baselineprofile.gradle.utils.checkAgpVersion
import androidx.baselineprofile.gradle.utils.createBuildTypeIfNotExists
import androidx.baselineprofile.gradle.utils.createExtendedBuildTypes
import androidx.baselineprofile.gradle.utils.isGradleSyncRunning
import com.android.build.api.attributes.AgpVersionAttr
import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.attributes.ProductFlavorAttr
import com.android.build.api.dsl.TestBuildType
import com.android.build.api.variant.TestAndroidComponentsExtension
import com.android.build.gradle.TestExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.DirectoryProperty

/**
 * This is the producer plugin for baseline profile generation. In order to generate baseline
 * profiles three plugins are needed: one is applied to the app or the library that should consume
 * the baseline profile when building (consumer), one is applied to the module that should supply
 * the under test app (app target) and the last one is applied to a test module containing the ui
 * test that generate the baseline profile on the device (producer).
 */
class BaselineProfileProducerPlugin : Plugin<Project> {

    companion object {
        private const val COLLECT_TASK_NAME = "collect"
        private const val RELEASE = "release"
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
                    applied. The `androidx.baselineprofile.producer` plugin supports only android
                    test modules. Please review your build.gradle to ensure this plugin is applied
                    to the correct module.
                    """.trimIndent()
                    )
                }
                project.logger.debug(
                    "[BaselineProfileProducerPlugin] afterEvaluate check: app plugin was applied"
                )
            }
        }
    }

    private fun configureWithAndroidPlugin(project: Project) {

        // Checks that the required AGP version is applied to this project.
        project.checkAgpVersion()

        // Prepares extensions used by the plugin
        val baselineProfileExtension = BaselineProfileProducerExtension.registerExtension(project)

        val testAndroidComponent = project.extensions.getByType(
            TestAndroidComponentsExtension::class.java
        )

        // We need the instrumentation apk to run as a separate process
        val testExtension = project.extensions.getByType(TestExtension::class.java)
        @Suppress("UnstableApiUsage")
        testExtension.experimentalProperties["android.experimental.self-instrumenting"] = true

        // Creates the new build types to match the app target. Note that release does not
        // exist by default so we need to create nonMinifiedRelease and map it manually to
        // `release`. All the existing build types beside `debug`, that is the default one, are
        // added manually in the configuration so we can assume they've been added for the purpose
        // of generating baseline profiles. We don't need to create a nonMinified build type from
        // `debug` since there will be no matching configuration with the app target module.

        val nonObfuscatedReleaseName = camelCase(BUILD_TYPE_BASELINE_PROFILE_PREFIX, RELEASE)
        val extendedTypeToOriginalTypeMapping = mutableMapOf(nonObfuscatedReleaseName to RELEASE)

        testAndroidComponent.finalizeDsl { ext ->

            // The test build types need to be debuggable and have the same singing config key to
            // be installed. We also disable the test coverage tracking since it's not important
            // here.
            val configureBlock: TestBuildType.() -> (Unit) = {
                isDebuggable = true
                enableAndroidTestCoverage = false
                enableUnitTestCoverage = false
                signingConfig = ext.buildTypes.getByName("debug").signingConfig
                matchingFallbacks += listOf(RELEASE)
            }

            // The variant names are used by the test module to request a specific apk artifact to
            // the under test app module (using configuration attributes). This is all handled by
            // the com.android.test plugin, as long as both modules have the same variants.
            // Unfortunately the test module cannot determine which variants are present in the
            // under test app module. As a result we need to replicate the same build types and
            // flavors, so that the same variant names are created.
            createExtendedBuildTypes(
                project = project,
                extension = ext,
                newBuildTypePrefix = BUILD_TYPE_BASELINE_PROFILE_PREFIX,
                extendedBuildTypeToOriginalBuildTypeMapping = extendedTypeToOriginalTypeMapping,
                configureBlock = configureBlock,
                filterBlock = {
                    // All the build types that have been added to the test module should be
                    // extended. This is because we can't know here which ones are actually
                    // release in the under test module. We can only exclude debug for sure.
                    it.name != "debug"
                }
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

                // Creates the configuration to handle this variant. Note that in the attributes
                // to match the configuration we use the original build type without `nonObfuscated`.
                val originalBuildTypeName = extendedTypeToOriginalTypeMapping[it.buildType] ?: ""
                val configurationName = createBaselineProfileConfigurationForVariant(
                    project = project,
                    productFlavors = it.productFlavors,
                    originalBuildTypeName = originalBuildTypeName,
                    flavorName = it.flavorName
                )

                // Prepares a block to execute later that creates the tasks for this variant
                createTaskBlocks.add {
                    createTasksForVariant(
                        project = project,
                        variantName = it.name,
                        flavorName = it.flavorName,
                        buildType = it.buildType,
                        configurationName = configurationName,
                        baselineProfileExtension = baselineProfileExtension
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
        buildType: String?,
        configurationName: String,
        baselineProfileExtension: BaselineProfileProducerExtension
    ) {

        // Prepares the devices list to use to generate the baseline profile.
        val devices = baselineProfileExtension.devices

        // The test task runs the ui tests
        val testTasks = devices.map {
            try {
                project.tasks.named(camelCase(it, variantName, "androidTest")).apply {
                    configure { t ->
                        // TODO: this is a bit hack-ish but we can rewrite if we decide to keep the
                        //  configuration [BaselineProfileProducerExtension.enableEmulatorDisplay]
                        if (t.hasProperty("enableEmulatorDisplay")) {
                            t.setProperty(
                                "enableEmulatorDisplay",
                                baselineProfileExtension.enableEmulatorDisplay
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
                    Please check the managed devices specified in the baseline profile
                    configuration.
                """.trimIndent(), e
                )
            }
        }

        // The collect task collects the baseline profile files from the ui test results
        val collectTaskProvider = project.tasks.register(
            camelCase(COLLECT_TASK_NAME, variantName, TASK_NAME_SUFFIX),
            CollectBaselineProfileTask::class.java
        ) {

            var outputDir = project
                .layout
                .buildDirectory
                .dir("$INTERMEDIATES_BASE_FOLDER/$flavorName/")
            if (!flavorName.isNullOrBlank()) {
                outputDir = outputDir.map { d -> d.dir(flavorName) }
            }
            if (!buildType.isNullOrBlank()) {
                outputDir = outputDir.map { d -> d.dir(buildType) }
            }

            // Sets the baseline-prof output path.
            it.outputFile.set(outputDir.map { d -> d.file("baseline-prof.txt") })

            // Sets the test results inputs
            it.testResultDirs.setFrom(testTasks.map { task ->
                task.flatMap { t -> t.property("resultsDir") as DirectoryProperty }
            })
        }

        // The artifacts are added to the configuration that exposes the generated baseline profile
        project.artifacts { artifactHandler ->
            artifactHandler.add(configurationName, collectTaskProvider) { artifact ->
                artifact.type = CONFIGURATION_ARTIFACT_TYPE
                artifact.builtBy(collectTaskProvider)
            }
        }
    }

    private fun createBaselineProfileConfigurationForVariant(
        project: Project,
        flavorName: String?,
        productFlavors: List<Pair<String, String>>,
        originalBuildTypeName: String,
    ): String {

        val configurationName =
            camelCase(flavorName ?: "", originalBuildTypeName, CONFIGURATION_NAME_BASELINE_PROFILES)
        project.configurations
            .maybeCreate(configurationName)
            .apply {
                isCanBeResolved = false
                isCanBeConsumed = true
                attributes {

                    // Main specialized attribute
                    it.attribute(
                        Usage.USAGE_ATTRIBUTE,
                        project.objects.named(
                            Usage::class.java, ATTRIBUTE_USAGE_BASELINE_PROFILE
                        )
                    )

                    // Build type
                    it.attribute(
                        BuildTypeAttr.ATTRIBUTE,
                        project.objects.named(
                            BuildTypeAttr::class.java, originalBuildTypeName)
                    )

                    // Jvm Environment
                    it.attribute(
                        TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                        project.objects.named(
                            TargetJvmEnvironment::class.java, ATTRIBUTE_TARGET_JVM_ENVIRONMENT
                        )
                    )

                    // Agp version
                    it.attribute(
                        AgpVersionAttr.ATTRIBUTE,
                        project.objects.named(
                            AgpVersionAttr::class.java, project.agpVersionString()
                        )
                    )

                    // Baseline Profile Plugin Version
                    it.attribute(
                        BaselineProfilePluginVersionAttr.ATTRIBUTE,
                        project.objects.named(
                            BaselineProfilePluginVersionAttr::class.java,
                            ATTRIBUTE_BASELINE_PROFILE_PLUGIN_VERSION
                        )
                    )

                    // Product flavors
                    productFlavors.forEach { (flavorName, flavorValue) ->
                        it.attribute(
                            @Suppress("UnstableApiUsage")
                            ProductFlavorAttr.of(flavorName),
                            project.objects.named(
                                ProductFlavorAttr::class.java, flavorValue
                            )
                        )
                    }
                }
            }
        return configurationName
    }
}
