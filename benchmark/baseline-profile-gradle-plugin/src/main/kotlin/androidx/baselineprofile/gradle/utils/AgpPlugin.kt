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

package androidx.baselineprofile.gradle.utils

import com.android.build.api.AndroidPluginVersion
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.api.dsl.TestedExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.ApplicationVariantBuilder
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.LibraryVariant
import com.android.build.api.variant.LibraryVariantBuilder
import com.android.build.api.variant.TestAndroidComponentsExtension
import com.android.build.api.variant.TestVariant
import com.android.build.api.variant.TestVariantBuilder
import com.android.build.api.variant.Variant
import com.android.build.api.variant.VariantBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.TaskProvider

/**
 * Defines callbacks and utility methods to create a plugin that utilizes AGP apis.
 * Callbacks with the configuration lifecycle of the agp plugins are provided.
 */
internal abstract class AgpPlugin(
    private val project: Project,
    private val supportedAgpPlugins: Set<AgpPluginId>,
    private val minAgpVersion: AndroidPluginVersion,
    private val maxAgpVersion: AndroidPluginVersion,
) {

    protected val logger: Logger
        get() = project.logger

    private val afterVariantBlocks = mutableListOf<() -> (Unit)>()

    fun onApply() {

        val foundPlugins = mutableSetOf<AgpPluginId>()

        // Try to configure with the supported plugins.
        for (agpPluginId in supportedAgpPlugins) {
            project.pluginManager.withPlugin(agpPluginId.value) {
                foundPlugins.add(agpPluginId)
                configureWithAndroidPlugin()
            }
        }

        // Only used to verify that the android application plugin has been applied.
        // Note that we don't want to throw any exception if gradle sync is in progress.
        project.afterEvaluate {
            if (!isGradleSyncRunning()) {
                if (foundPlugins.isEmpty()) {
                    onAgpPluginNotFound(foundPlugins)
                } else {
                    onAgpPluginFound(foundPlugins)
                }
            }
        }
    }

    private fun configureWithAndroidPlugin() {

        checkAgpVersion(min = minAgpVersion, max = maxAgpVersion)

        onBeforeFinalizeDsl()

        testAndroidComponentExtension()?.let { testComponent ->
            testComponent.finalizeDsl { onTestFinalizeDsl(it) }
            testComponent.beforeVariants { onTestBeforeVariants(it) }
            testComponent.onVariants { onTestVariants(it) }
        }

        applicationAndroidComponentsExtension()?.let { applicationComponent ->
            applicationComponent.finalizeDsl { onApplicationFinalizeDsl(it) }
            applicationComponent.beforeVariants { onApplicationBeforeVariants(it) }
            applicationComponent.onVariants { onApplicationVariants(it) }
        }

        libraryAndroidComponentsExtension()?.let { libraryComponent ->
            libraryComponent.finalizeDsl { onLibraryFinalizeDsl(it) }
            libraryComponent.beforeVariants { onLibraryBeforeVariants(it) }
            libraryComponent.onVariants { onLibraryVariants(it) }
        }

        androidComponentsExtension()?.let { commonComponent ->
            commonComponent.finalizeDsl { onFinalizeDsl(commonComponent) }
            commonComponent.beforeVariants { onBeforeVariants(it) }
            commonComponent.onVariants { onVariants(it) }
        }

        // Runs the after variants callback that is module type dependent
        val testedExtension = testedExtension()
        val testExtension = testExtension()

        val variants = when {
            testedExtension != null &&
                testedExtension is com.android.build.gradle.AppExtension -> {
                testedExtension.applicationVariants
            }

            testedExtension != null &&
                testedExtension is com.android.build.gradle.LibraryExtension -> {
                testedExtension.libraryVariants
            }

            testExtension != null -> {
                testExtension.applicationVariants
            }

            else -> {
                if (isGradleSyncRunning()) return
                // This cannot happen because of user configuration because the plugin is only
                // applied if there is an android gradle plugin.
                throw GradleException("Module `${project.path}` is not a supported android module.")
            }
        }

        var applied = false
        variants.all {
            if (applied) return@all
            applied = true

            // Execute all the scheduled variant blocks
            afterVariantBlocks.forEach { it() }

            // Execute the after variant callback if scheduled.
            onAfterVariants()
        }
    }

    // Utility methods

    protected fun <T : Task> addArtifactToConfiguration(
        configurationName: String,
        taskProvider: TaskProvider<T>,
        artifactType: String
    ) {
        project.artifacts { artifactHandler ->
            artifactHandler.add(configurationName, taskProvider) { artifact ->
                artifact.type = artifactType
                artifact.builtBy(taskProvider)
            }
        }
    }

    protected fun isGradleSyncRunning() = project.isGradleSyncRunning()

    protected fun afterVariants(block: () -> (Unit)) = afterVariantBlocks.add(block)

    protected fun agpVersion() = project.agpVersion()

    private fun checkAgpVersion(min: AndroidPluginVersion, max: AndroidPluginVersion) {
        val agpVersion = project.agpVersion()
        if (agpVersion < min || agpVersion > max) {
            throw GradleException(
                """
        This version of the Baseline Profile Gradle Plugin only works with Android Gradle plugin
        between versions $MIN_AGP_VERSION_REQUIRED and $MAX_AGP_VERSION_REQUIRED. Current version
        is $agpVersion."
            """.trimIndent()
            )
        }
    }

    protected fun supportsFeature(feature: AgpFeature) = agpVersion() >= feature.version

    protected fun isTestModule() = testAndroidComponentExtension() != null
    protected fun isLibraryModule() = libraryAndroidComponentsExtension() != null
    protected fun isApplicationModule() = applicationAndroidComponentsExtension() != null

    // Plugin application callbacks

    protected open fun onAgpPluginNotFound(pluginIds: Set<AgpPluginId>) {}

    protected open fun onAgpPluginFound(pluginIds: Set<AgpPluginId>) {}

    // Test callbacks

    protected open fun onTestFinalizeDsl(extension: TestExtension) {}

    protected open fun onTestBeforeVariants(variantBuilder: TestVariantBuilder) {}

    protected open fun onTestVariants(variant: TestVariant) {}

    // Application callbacks

    protected open fun onApplicationFinalizeDsl(extension: ApplicationExtension) {}

    protected open fun onApplicationBeforeVariants(variantBuilder: ApplicationVariantBuilder) {}

    protected open fun onApplicationVariants(variant: ApplicationVariant) {}

    // Library callbacks

    protected open fun onLibraryFinalizeDsl(extension: LibraryExtension) {}

    protected open fun onLibraryBeforeVariants(variantBuilder: LibraryVariantBuilder) {}

    protected open fun onLibraryVariants(variant: LibraryVariant) {}

    // Shared callbacks

    protected open fun onBeforeFinalizeDsl() {}

    protected open fun onFinalizeDsl(extension: AndroidComponentsExtension<*, *, *>) {}

    protected open fun onBeforeVariants(variantBuilder: VariantBuilder) {}

    protected open fun onVariants(variant: Variant) {}

    protected open fun onAfterVariants() {}

    // Quick access to extension methods

    private fun testAndroidComponentExtension(): TestAndroidComponentsExtension? =
        project
            .extensions
            .findByType(TestAndroidComponentsExtension::class.java)

    private fun applicationAndroidComponentsExtension(): ApplicationAndroidComponentsExtension? =
        project
            .extensions
            .findByType(ApplicationAndroidComponentsExtension::class.java)

    private fun libraryAndroidComponentsExtension(): LibraryAndroidComponentsExtension? =
        project
            .extensions
            .findByType(LibraryAndroidComponentsExtension::class.java)

    private fun androidComponentsExtension(): AndroidComponentsExtension<*, *, *>? =
        project
            .extensions
            .findByType(AndroidComponentsExtension::class.java)

    private fun testedExtension(): TestedExtension? =
        project
            .extensions
            .findByType(TestedExtension::class.java)

    private fun testExtension(): com.android.build.gradle.TestExtension? =
        project
            .extensions
            .findByType(com.android.build.gradle.TestExtension::class.java)
}

private val gradleSyncProps by lazy {
    listOf(
        "android.injected.build.model.v2",
        "android.injected.build.model.only",
        "android.injected.build.model.only.advanced",
    )
}

internal fun Project.isGradleSyncRunning() = gradleSyncProps.any {
    it in project.properties && project.properties[it].toString().toBoolean()
}

/**
 * Enumerates the supported android plugins.
 */
internal enum class AgpPluginId(val value: String) {
    ID_ANDROID_APPLICATION_PLUGIN("com.android.application"),
    ID_ANDROID_LIBRARY_PLUGIN("com.android.library"),
    ID_ANDROID_TEST_PLUGIN("com.android.test")
}
