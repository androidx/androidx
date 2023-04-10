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

package androidx.baselineprofiles.gradle.buildprovider

import androidx.baselineprofiles.gradle.utils.createNonObfuscatedBuildTypes
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * This is the build provider plugin for baseline profile generation. In order to generate baseline
 * profiles three plugins are needed: one is applied to the app or the library that should consume
 * the baseline profile when building (consumer), one is applied to the project that should supply
 * the test apk (build provider) and the last one is applied to a library module containing the ui
 * test that generate the baseline profile on the device (producer).
 *
 * TODO (b/265438721): build provider should be changed to apk provider.
 */
class BaselineProfilesBuildProviderPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.withPlugin("com.android.application") {
            configureWithAndroidPlugin(project = project)
        }
    }

    private fun configureWithAndroidPlugin(project: Project) {

        // Prepares extensions used by the plugin
        val baselineProfilesExtension =
            BaselineProfilesApkProviderExtension.registerExtension(project)

        val androidComponent = project.extensions.getByType(
            ApplicationAndroidComponentsExtension::class.java
        )

        // Create the non obfuscated release build types from the existing release ones.
        // We want to extend all the current release build types based on isDebuggable flag.
        // The map created here maps the non obfuscated build types newly created to the release
        // ones.
        val extendedTypeToOriginalTypeMapping = mutableMapOf<String, String>()
        androidComponent.finalizeDsl { applicationExtension ->

            val debugBuildType = applicationExtension.buildTypes.getByName("debug")
            createNonObfuscatedBuildTypes(
                project = project,
                extension = applicationExtension,
                extendedBuildTypeToOriginalBuildTypeMapping = extendedTypeToOriginalTypeMapping,
                filterBlock = { !it.isDebuggable },
                configureBlock = {
                    isJniDebuggable = false
                    isDebuggable = false
                    isMinifyEnabled = true
                    isShrinkResources = true
                    isProfileable = true
                    signingConfig = debugBuildType.signingConfig
                    enableAndroidTestCoverage = false
                    enableUnitTestCoverage = false

                    // The keep rule file is added later in the variants callback so that we can
                    // generate it on-the-fly in the intermediates folder, if it wasn't specified in
                    // the config.
                }
            )
        }

        // Creates a task to generate the keep rule file
        val genKeepRuleTaskProvider = project
            .tasks
            .register(
                "generateBaselineProfilesKeepRules",
                GenerateKeepRulesForBaselineProfilesTask::class.java
            ) {
                it.keepRuleFile.set(
                    project.layout.buildDirectory.file(
                        "intermediates/baselineprofiles/baseline-profile-keep-rules.pro"
                    )
                )
            }

        val keepRuleFileProvider = genKeepRuleTaskProvider.flatMap { it.keepRuleFile }

        // Sets the keep rule file for the baseline profile variants
        androidComponent.apply {

            onVariants {

                // We can skip the build types that were NOT created by this plugin.
                if (it.buildType !in extendedTypeToOriginalTypeMapping.keys) {
                    return@onVariants
                }

                // If the keep rule file was manually specified, then use that one
                if (baselineProfilesExtension.keepRulesFile != null) {
                    it.proguardFiles.add(
                        project
                            .layout
                            .projectDirectory
                            .file(baselineProfilesExtension.keepRulesFile!!)
                    )
                    return@onVariants
                }

                // Otherwise the keep rule file is generated and added to the list of keep rule
                // files for the variant.
                it.proguardFiles.add(keepRuleFileProvider)
            }
        }
    }
}
