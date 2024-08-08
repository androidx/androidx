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
import org.gradle.api.tasks.TaskProvider

/**
 * Defines callbacks and utility methods to create a plugin that utilizes AGP apis. Callbacks with
 * the configuration lifecycle of the agp plugins are provided.
 */
internal abstract class AgpPlugin(
    private val project: Project,
    private val supportedAgpPlugins: Set<AgpPluginId>,
    private val minAgpVersionInclusive: AndroidPluginVersion,
    private val maxAgpVersionExclusive: AndroidPluginVersion,
) {

    // Properties that can be specified by cmd line using -P<property_name> when invoking gradle.
    val testMaxAgpVersion by lazy {
        project.properties["androidx.benchmark.test.maxagpversion"]?.let { str ->
            val parts = str.toString().split(".").map { it.toInt() }
            return@lazy AndroidPluginVersion(parts[0], parts[1], parts[2])
        } ?: return@lazy null
    }

    // Logger
    protected val logger = BaselineProfilePluginLogger(project.logger)

    // Defines a list of block to be executed after all the onVariants callback
    private val afterVariantsBlocks = mutableListOf<() -> (Unit)>()

    // Callback schedulers for each variant type
    private val onVariantBlockScheduler = OnVariantBlockScheduler<Variant>("common")
    private val onAppVariantBlockScheduler =
        OnVariantBlockScheduler<ApplicationVariant>("application")
    private val onLibraryVariantBlockScheduler = OnVariantBlockScheduler<LibraryVariant>("library")
    private val onTestVariantBlockScheduler = OnVariantBlockScheduler<TestVariant>("test")

    private var checkedAgpVersion = false

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

        onBeforeFinalizeDsl()

        testAndroidComponentExtension()?.let { testComponent ->
            testComponent.finalizeDsl {
                onTestFinalizeDsl(it)

                // This can be done only here, since warnings may depend on user configuration
                // that is ready only after `finalizeDsl`.
                getWarnings()?.let { warnings -> logger.setWarnings(warnings) }
                checkAgpVersion()
            }
            testComponent.beforeVariants { onTestBeforeVariants(it) }
            testComponent.onVariants {
                onTestVariantBlockScheduler.onVariant(it)
                onTestVariants(it)
            }
        }

        applicationAndroidComponentsExtension()?.let { applicationComponent ->
            applicationComponent.finalizeDsl {
                onApplicationFinalizeDsl(it)

                // This can be done only here, since warnings may depend on user configuration
                // that is ready only after `finalizeDsl`.
                getWarnings()?.let { warnings -> logger.setWarnings(warnings) }
                checkAgpVersion()
            }
            applicationComponent.beforeVariants { onApplicationBeforeVariants(it) }
            applicationComponent.onVariants {
                onAppVariantBlockScheduler.onVariant(it)
                onApplicationVariants(it)
            }
        }

        libraryAndroidComponentsExtension()?.let { libraryComponent ->
            libraryComponent.finalizeDsl {
                onLibraryFinalizeDsl(it)

                // This can be done only here, since warnings may depend on user configuration
                // that is ready only after `finalizeDsl`.
                getWarnings()?.let { warnings -> logger.setWarnings(warnings) }
                checkAgpVersion()
            }
            libraryComponent.beforeVariants { onLibraryBeforeVariants(it) }
            libraryComponent.onVariants {
                onLibraryVariantBlockScheduler.onVariant(it)
                onLibraryVariants(it)
            }
        }

        androidComponentsExtension()?.let { commonComponent ->
            commonComponent.finalizeDsl {
                onFinalizeDsl(commonComponent)

                // This can be done only here, since warnings may depend on user configuration
                // that is ready only after `finalizeDsl`.
                getWarnings()?.let { warnings -> logger.setWarnings(warnings) }
                checkAgpVersion()
            }
            commonComponent.beforeVariants { onBeforeVariants(it) }
            commonComponent.onVariants {
                onVariantBlockScheduler.onVariant(it)
                onVariants(it)
            }
        }

        // Runs the after variants callback that is module type dependent
        val testedExtension = testedExtension()
        val testExtension = testExtension()

        val variants =
            when {
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
                    throw GradleException(
                        "Module `${project.path}` is not a supported android module."
                    )
                }
            }

        var applied = false
        variants.configureEach {
            if (applied) return@configureEach
            applied = true

            // Execute all the scheduled variant blocks
            afterVariantsBlocks.forEach { it() }
            afterVariantsBlocks.clear()

            // Execute the after variant callback if scheduled.
            onAfterVariants()

            // Throw an exception if a scheduled callback was not executed
            if (afterVariantsBlocks.isNotEmpty()) {
                throw IllegalStateException(
                    "After variants blocks cannot be scheduled in the `onAfterVariants` callback."
                )
            }

            // Ensure no scheduled callbacks is skipped.
            onAppVariantBlockScheduler.assertBlockMapEmpty()
            onTestVariantBlockScheduler.assertBlockMapEmpty()
            onLibraryVariantBlockScheduler.assertBlockMapEmpty()
            onVariantBlockScheduler.assertBlockMapEmpty()
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

    protected open fun getWarnings(): Warnings? = null

    protected fun afterVariants(block: () -> (Unit)) = afterVariantsBlocks.add(block)

    @JvmName("onVariant")
    protected fun onVariant(variantName: String, block: (Variant) -> (Unit)) =
        onVariantBlockScheduler.executeOrScheduleOnVariantBlock(variantName, block)

    @JvmName("onApplicationVariant")
    protected fun onVariant(variantName: String, block: (ApplicationVariant) -> (Unit)) =
        onAppVariantBlockScheduler.executeOrScheduleOnVariantBlock(variantName, block)

    @JvmName("onLibraryVariant")
    protected fun onVariant(variantName: String, block: (LibraryVariant) -> (Unit)) =
        onLibraryVariantBlockScheduler.executeOrScheduleOnVariantBlock(variantName, block)

    @JvmName("onTestVariant")
    protected fun onVariant(variantName: String, block: (TestVariant) -> (Unit)) =
        onTestVariantBlockScheduler.executeOrScheduleOnVariantBlock(variantName, block)

    protected fun removeOnVariantCallback(variantName: String) {
        onVariantBlockScheduler.removeOnVariantCallback(variantName)
        onAppVariantBlockScheduler.removeOnVariantCallback(variantName)
        onLibraryVariantBlockScheduler.removeOnVariantCallback(variantName)
        onTestVariantBlockScheduler.removeOnVariantCallback(variantName)
    }

    protected fun agpVersion() = project.agpVersion()

    private fun checkAgpVersion() {

        // According to which callbacks are implemented by the user, this function may be called
        // more than once but we want to check only once.
        if (checkedAgpVersion) return
        checkedAgpVersion = true

        val agpVersion = project.agpVersion()
        if (agpVersion.previewType == "dev") {
            return // Skip version check for androidx-studio-integration branch
        }
        if (agpVersion < minAgpVersionInclusive) {
            throw GradleException(
                """
        This version of the Baseline Profile Gradle Plugin requires the Android Gradle Plugin to be
        at least version $minAgpVersionInclusive. The current version is $agpVersion.
        Please update your project.
            """
                    .trimIndent()
            )
        }
        if (agpVersion >= (testMaxAgpVersion ?: maxAgpVersionExclusive)) {
            logger.warn(
                property = { maxAgpVersion },
                propertyName = "maxAgpVersion",
                message =
                    """
        This version of the Baseline Profile Gradle Plugin was tested with versions below Android
        Gradle Plugin version $maxAgpVersionExclusive and it may not work as intended.
        Current version is $agpVersion.
                """
                        .trimIndent()
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
        project.extensions.findByType(TestAndroidComponentsExtension::class.java)

    private fun applicationAndroidComponentsExtension(): ApplicationAndroidComponentsExtension? =
        project.extensions.findByType(ApplicationAndroidComponentsExtension::class.java)

    private fun libraryAndroidComponentsExtension(): LibraryAndroidComponentsExtension? =
        project.extensions.findByType(LibraryAndroidComponentsExtension::class.java)

    private fun androidComponentsExtension(): AndroidComponentsExtension<*, *, *>? =
        project.extensions.findByType(AndroidComponentsExtension::class.java)

    private fun testedExtension(): TestedExtension? =
        project.extensions.findByType(TestedExtension::class.java)

    private fun testExtension(): com.android.build.gradle.TestExtension? =
        project.extensions.findByType(com.android.build.gradle.TestExtension::class.java)
}

private val gradleSyncProps by lazy {
    listOf(
        "android.injected.build.model.v2",
        "android.injected.build.model.only",
        "android.injected.build.model.only.advanced",
    )
}

internal fun Project.isGradleSyncRunning() =
    gradleSyncProps.any { property ->
        providers.gradleProperty(property).map { it.toBoolean() }.orElse(false).get()
    }

/** Enumerates the supported android plugins. */
internal enum class AgpPluginId(val value: String) {
    ID_ANDROID_APPLICATION_PLUGIN("com.android.application"),
    ID_ANDROID_LIBRARY_PLUGIN("com.android.library"),
    ID_ANDROID_TEST_PLUGIN("com.android.test")
}

/**
 * This class is basically an help to manage executing callbacks on a variant. Because of how agp
 * variants are published, there is no way to directly access it. This class stores a callback and
 * executes it when the variant is published in the agp onVariants callback.
 */
private class OnVariantBlockScheduler<T : Variant>(private val variantTypeName: String) {

    // Stores the current published variants
    val publishedVariants = mutableMapOf<String, T>()

    // Defines a list of block to be executed for a certain variant when it gets published
    val onVariantBlocks = mutableMapOf<String, MutableList<(T) -> (Unit)>>()

    fun executeOrScheduleOnVariantBlock(variantName: String, block: (T) -> (Unit)) {
        if (variantName in publishedVariants) {
            publishedVariants[variantName]?.let { block(it) }
        } else {
            onVariantBlocks.computeIfAbsent(variantName) { mutableListOf() } += block
        }
    }

    fun removeOnVariantCallback(variantName: String) {
        onVariantBlocks.remove(variantName)
    }

    fun onVariant(variant: T) {

        // This error cannot be thrown because of a user configuration but only an error when
        // extending AgpPlugin.
        if (variant.name in publishedVariants)
            throw IllegalStateException(
                """
            A variant was published more than once. This can only happen if the AgpPlugin base
            class is used and an additional onVariants callback is directly registered with the
            base components.
        """
                    .trimIndent()
            )

        // Stores the published variant
        publishedVariants[variant.name] = variant

        // Executes all the callbacks previously scheduled for this variant.
        onVariantBlocks.remove(variant.name)?.apply {
            forEach { b -> b(variant) }
            clear()
        }
    }

    fun assertBlockMapEmpty() {
        if (onVariantBlocks.isEmpty()) return
        val variantNames = "[`${onVariantBlocks.toList().joinToString("`, `") { it.first }}`]"
        throw IllegalStateException(
            "Callbacks for $variantTypeName variants $variantNames were not executed."
        )
    }
}
