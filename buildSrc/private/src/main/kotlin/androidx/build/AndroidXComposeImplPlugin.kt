/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build

import androidx.build.dependencies.KOTLIN_NATIVE_VERSION
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.internal.lint.AndroidLintAnalysisTask
import com.android.build.gradle.internal.lint.AndroidLintTask
import com.android.build.gradle.internal.lint.LintModelWriterTask
import com.android.build.gradle.internal.lint.VariantInputs
import java.io.File
import kotlin.reflect.KFunction
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.tooling.core.withClosure

const val composeSourceOption =
    "plugin:androidx.compose.compiler.plugins.kotlin:sourceInformation=true"
const val composeMetricsOption =
    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination"
const val composeReportsOption =
    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination"
const val enableMetricsArg = "androidx.enableComposeCompilerMetrics"
const val enableReportsArg = "androidx.enableComposeCompilerReports"

/**
 * Plugin to apply common configuration for Compose projects.
 */
class AndroidXComposeImplPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val f: KFunction<Unit> = Companion::applyAndConfigureKotlinPlugin
        project.extensions.add("applyAndConfigureKotlinPlugin", f)
        val extension = project.extensions.create<AndroidXComposeExtension>(
            "androidxCompose",
            project
        )
        project.plugins.all { plugin ->
            when (plugin) {
                is LibraryPlugin -> {
                    val library = project.extensions.findByType(LibraryExtension::class.java)
                        ?: throw Exception("Failed to find Android extension")

                    project.configureAndroidCommonOptions(library)
                }
                is AppPlugin -> {
                    val app = project.extensions.findByType(AppExtension::class.java)
                        ?: throw Exception("Failed to find Android extension")

                    project.configureAndroidCommonOptions(app)
                }
                is KotlinBasePluginWrapper -> {
                    configureComposeCompilerPlugin(project, extension)

                    if (plugin is KotlinMultiplatformPluginWrapper) {
                        project.configureForMultiplatform()
                    }
                }
            }
        }
    }

    companion object {

        /**
         * @param isMultiplatformEnabled whether this module has a corresponding
         * multiplatform configuration, or whether it is Android only
         */
        fun applyAndConfigureKotlinPlugin(
            project: Project,
            isMultiplatformEnabled: Boolean
        ) {
            if (isMultiplatformEnabled) {
                project.apply(plugin = "kotlin-multiplatform")

                project.extensions.create(
                    AndroidXComposeMultiplatformExtension::class.java,
                    "androidXComposeMultiplatform",
                    AndroidXComposeMultiplatformExtensionImpl::class.java
                )
            } else {
                project.apply(plugin = "org.jetbrains.kotlin.android")
            }

            project.configureManifests()
            if (isMultiplatformEnabled) {
                project.configureForMultiplatform()
            } else {
                project.configureForKotlinMultiplatformSourceStructure()
            }

            project.tasks.withType(KotlinCompile::class.java).configureEach { compile ->
                // Needed to enable `expect` and `actual` keywords
                compile.kotlinOptions.freeCompilerArgs += "-Xmulti-platform"
            }
        }

        private fun Project.androidxExtension(): AndroidXExtension? {
            return extensions.findByType(AndroidXExtension::class.java)
        }

        private fun Project.configureAndroidCommonOptions(testedExtension: TestedExtension) {
            testedExtension.defaultConfig.minSdk = 21

            @Suppress("UnstableApiUsage")
            extensions.findByType(AndroidComponentsExtension::class.java)!!.finalizeDsl {
                val isPublished = androidxExtension()?.shouldPublish() ?: false

                it.lint {
                    // Too many Kotlin features require synthetic accessors - we want to rely on R8 to
                    // remove these accessors
                    disable.add("SyntheticAccessor")
                    // These lint checks are normally a warning (or lower), but we ignore (in AndroidX)
                    // warnings in Lint, so we make it an error here so it will fail the build.
                    // Note that this causes 'UnknownIssueId' lint warnings in the build log when
                    // Lint tries to apply this rule to modules that do not have this lint check, so
                    // we disable that check too
                    disable.add("UnknownIssueId")
                    error.add("ComposableNaming")
                    error.add("ComposableLambdaParameterNaming")
                    error.add("ComposableLambdaParameterPosition")
                    error.add("CompositionLocalNaming")
                    error.add("ComposableModifierFactory")
                    error.add("InvalidColorHexValue")
                    error.add("MissingColorAlphaChannel")
                    error.add("ModifierFactoryReturnType")
                    error.add("ModifierFactoryExtensionFunction")
                    error.add("ModifierParameter")
                    error.add("MutableCollectionMutableState")
                    error.add("UnnecessaryComposedModifier")
                    error.add("FrequentlyChangedStateReadInComposition")
                    error.add("ReturnFromAwaitPointerEventScope")
                    error.add("UseOfNonLambdaOffsetOverload")

                    // Paths we want to enable ListIterator checks for - for higher level
                    // libraries it won't have a noticeable performance impact, and we don't want
                    // developers reading high level library code to worry about this.
                    val listIteratorPaths = listOf(
                        "compose:foundation",
                        "compose:runtime",
                        "compose:ui",
                        "text"
                    )

                    // Paths we want to disable ListIteratorChecks for - these are not runtime
                    // libraries and so Iterator allocation is not relevant.
                    val ignoreListIteratorFilter = listOf(
                        "compose:ui:ui-test",
                        "compose:ui:ui-tooling",
                        "compose:ui:ui-inspection",
                    )

                    // Disable ListIterator if we are not in a matching path, or we are in an
                    // unpublished project
                    if (
                        listIteratorPaths.none { path.contains(it) } ||
                        ignoreListIteratorFilter.any { path.contains(it) } ||
                        !isPublished
                    ) {
                        disable.add("ListIterator")
                    }
                }
            }

            // TODO: figure out how to apply this to multiplatform modules
            dependencies.add(
                "lintChecks",
                project.dependencies.project(
                    mapOf(
                        "path" to ":compose:lint:internal-lint-checks",
                        // TODO(b/206617878) remove this shadow configuration
                        "configuration" to "shadow"
                    )
                )
            )
        }

        private fun Project.configureManifests() {
            val libraryExtension = project.extensions.findByType<LibraryExtension>() ?: return
            libraryExtension.apply {
                sourceSets.findByName("main")!!.manifest
                    .srcFile("src/androidMain/AndroidManifest.xml")
                sourceSets.findByName("androidTest")!!.manifest
                    .srcFile("src/androidAndroidTest/AndroidManifest.xml")
            }
        }

        /**
         * General configuration for MPP projects. In the future, these workarounds should either be
         * generified and added to AndroidXPlugin, or removed as/when the underlying issues have been
         * resolved.
         */
        private fun Project.configureForKotlinMultiplatformSourceStructure() {
            val libraryExtension = project.extensions.findByType<LibraryExtension>() ?: return

            // TODO: b/148416113: AGP doesn't know about Kotlin-MPP's sourcesets yet, so add
            // them to its source directories (this fixes lint, and code completion in
            // Android Studio on versions >= 4.0canary8)
            libraryExtension.apply {
                sourceSets.findByName("main")?.apply {
                    java.srcDirs(
                        "src/commonMain/kotlin", "src/jvmMain/kotlin",
                        "src/androidMain/kotlin"
                    )
                    res.srcDirs(
                        "src/commonMain/resources",
                        "src/androidMain/res"
                    )
                    assets.srcDirs("src/androidMain/assets")

                    // Keep Kotlin files in java source sets so the source set is not empty when
                    // running unit tests which would prevent the tests from running in CI.
                    java.includes.add("**/*.kt")
                }
                sourceSets.findByName("test")?.apply {
                    java.srcDirs(
                        "src/commonTest/kotlin", "src/jvmTest/kotlin"
                    )
                    res.srcDirs("src/commonTest/res", "src/jvmTest/res")

                    // Keep Kotlin files in java source sets so the source set is not empty when
                    // running unit tests which would prevent the tests from running in CI.
                    java.includes.add("**/*.kt")
                }
                sourceSets.findByName("androidTest")?.apply {
                    java.srcDirs("src/androidAndroidTest/kotlin")
                    res.srcDirs("src/androidAndroidTest/res")
                    assets.srcDirs("src/androidAndroidTest/assets")

                    // Keep Kotlin files in java source sets so the source set is not empty when
                    // running unit tests which would prevent the tests from running in CI.
                    java.includes.add("**/*.kt")
                }
            }
        }

        /**
         * General configuration for MPP projects. In the future, these workarounds should either be
         * generified and added to AndroidXPlugin, or removed as/when the underlying issues have been
         * resolved.
         */
        private fun Project.configureForMultiplatform() {
            // This is to allow K/N not matching the kotlinVersion
            (this.rootProject.property("ext") as ExtraPropertiesExtension)
                .set("kotlin.native.version", KOTLIN_NATIVE_VERSION)

            val multiplatformExtension = checkNotNull(multiplatformExtension) {
                "Unable to configureForMultiplatform() when " +
                    "multiplatformExtension is null (multiplatform plugin not enabled?)"
            }

            /*
            The following configures source sets - note:

            1. The common unit test source set, commonTest, is included by default in both android
            unit and instrumented tests. This causes unnecessary duplication, so we explicitly do
            _not_ use commonTest, instead choosing to just use the unit test variant.
            TODO: Consider using commonTest for unit tests if a usable feature is added for
            https://youtrack.jetbrains.com/issue/KT-34662.

            2. The default (android) unit test source set is named 'androidTest', which conflicts / is
            confusing as this shares the same name / expected directory as AGP's 'androidTest', which
            represents _instrumented_ tests.
            TODO: Consider changing unitTest to androidLocalTest and androidAndroidTest to
            androidDeviceTest when https://github.com/JetBrains/kotlin/pull/2829 rolls in.
            */
            multiplatformExtension.sourceSets.all {
                // Allow all experimental APIs, since MPP projects are themselves experimental
                it.languageSettings.apply {
                    optIn("kotlin.Experimental")
                    optIn("kotlin.ExperimentalMultiplatform")
                }
            }

            configureLintForMultiplatformLibrary(multiplatformExtension)

            afterEvaluate {
                if (multiplatformExtension.targets.findByName("jvm") != null) {
                    tasks.named("jvmTestClasses").also(::addToBuildOnServer)
                }
                if (multiplatformExtension.targets.findByName("desktop") != null) {
                    tasks.named("desktopTestClasses").also(::addToBuildOnServer)
                }
            }
        }
    }
}

private const val COMPILER_PLUGIN_CONFIGURATION = "kotlinPlugin"

private fun configureComposeCompilerPlugin(
    project: Project,
    extension: AndroidXComposeExtension
) {
    project.afterEvaluate {
        // If a project has opted-out of Compose compiler plugin, don't add it
        if (!extension.composeCompilerPluginEnabled) return@afterEvaluate

        val androidXExtension = project.extensions.findByType(AndroidXExtension::class.java)
            ?: throw Exception("You have applied AndroidXComposePlugin without AndroidXPlugin")
        val shouldPublish = androidXExtension.shouldPublish()

        // Create configuration that we'll use to load Compose compiler plugin
        val configuration = project.configurations.create(COMPILER_PLUGIN_CONFIGURATION)
        // Add Compose compiler plugin to kotlinPlugin configuration, making sure it works
        // for Playground builds as well
        project.dependencies.add(
            COMPILER_PLUGIN_CONFIGURATION,
            if (StudioType.isPlayground(project)) {
                AndroidXPlaygroundRootImplPlugin.projectOrArtifact(
                    project.rootProject,
                    ":compose:compiler:compiler"
                )
            } else {
                project.rootProject.findProject(":compose:compiler:compiler")!!
            }
        )
        val kotlinPlugin = configuration.incoming.artifactView { view ->
            view.attributes { attributes ->
                attributes.attribute(
                    Attribute.of("artifactType", String::class.java),
                    ArtifactTypeDefinition.JAR_TYPE
                )
            }
        }.files

        val enableMetricsProvider = project.providers.gradleProperty(enableMetricsArg)
        val enableReportsProvider = project.providers.gradleProperty(enableReportsArg)

        val libraryMetricsDirectory = project.rootProject.getLibraryMetricsDirectory()
        val libraryReportsDirectory = project.rootProject.getLibraryReportsDirectory()
        project.tasks.withType(KotlinCompile::class.java).configureEach { compile ->
            // TODO(b/157230235): remove when this is enabled by default
            compile.kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"

            // Append inputs to KotlinCompile so tasks get invalidated if any of these values change
            compile.inputs.files({ kotlinPlugin })
                .withPropertyName("composeCompilerExtension")
                .withNormalizer(ClasspathNormalizer::class.java)
            compile.inputs.property("composeMetricsEnabled", enableMetricsProvider).optional(true)
            compile.inputs.property("composeReportsEnabled", enableReportsProvider).optional(true)

            // Gradle hack ahead, we use of absolute paths, but is OK here because we do it in
            // doFirst which happens after Gradle task input snapshotting. AGP does the same.
            compile.doFirst {
                compile.kotlinOptions.freeCompilerArgs += "-Xplugin=${kotlinPlugin.first()}"

                if (enableMetricsProvider.orNull == "true") {
                    val metricsDest = File(libraryMetricsDirectory, "compose")
                    compile.kotlinOptions.freeCompilerArgs +=
                        listOf(
                            "-P",
                            "$composeMetricsOption=${metricsDest.absolutePath}"
                        )
                }
                if ((enableReportsProvider.orNull == "true")) {
                    val reportsDest = File(libraryReportsDirectory, "compose")
                    compile.kotlinOptions.freeCompilerArgs +=
                        listOf(
                            "-P",
                            "$composeReportsOption=${reportsDest.absolutePath}"
                        )
                }
                if (shouldPublish) {
                    compile.kotlinOptions.freeCompilerArgs +=
                        listOf("-P", composeSourceOption)
                }
            }
        }
    }
}

/**
 * Adds missing MPP sourcesets (such as commonMain) to the Lint tasks
 *
 * TODO: b/195329463
 * Lint is not aware of MPP, and MPP doesn't configure Lint. There is no built-in
 * API to adjust the default Lint task's sources, so we use this hack to manually
 * add sources for MPP source sets. In the future with the new Kotlin Project Model
 * (https://youtrack.jetbrains.com/issue/KT-42572) and an AGP / MPP integration
 * plugin this will no longer be needed.
 */
private fun Project.configureLintForMultiplatformLibrary(
    multiplatformExtension: KotlinMultiplatformExtension
) {
    afterEvaluate {
        // This workaround only works for libraries (apps would require changes to a different
        // task). Given that we currently do not have any MPP app projects, this should never
        // happen.
        project.extensions.findByType<LibraryExtension>()
            ?: return@afterEvaluate
        val androidMain = multiplatformExtension.sourceSets.findByName("androidMain")
            ?: return@afterEvaluate
        // Get all the sourcesets androidMain transitively / directly depends on
        val dependencies = androidMain.withClosure(KotlinSourceSet::dependsOn)

        /**
         * Helper function to add the missing sourcesets to this [VariantInputs]
         */
        fun VariantInputs.addSourceSets() {
            // Each variant has a source provider for the variant (such as debug) and the 'main'
            // variant. The actual files that Lint will run on is both of these providers
            // combined - so we can just add the dependencies to the first we see.
            val sourceProvider = sourceProviders.get().firstOrNull() ?: return
            dependencies.forEach { sourceSet ->
                sourceProvider.javaDirectories.withChangesAllowed {
                    from(sourceSet.kotlin.sourceDirectories)
                }
            }
        }

        // Lint for libraries is split into two tasks - analysis, and reporting. We need to
        // add the new sources to both, so all parts of the pipeline are aware.
        project.tasks.withType<AndroidLintAnalysisTask>().configureEach {
            it.variantInputs.addSourceSets()
        }

        project.tasks.withType<AndroidLintTask>().configureEach {
            it.variantInputs.addSourceSets()
        }

        // Also configure the model writing task, so that we don't run into mismatches between
        // analyzed sources in one module and a downstream module
        project.tasks.withType<LintModelWriterTask>().configureEach {
            it.variantInputs.addSourceSets()
        }
    }
}

/**
 * Lint uses [ConfigurableFileCollection.disallowChanges] during initialization, which prevents
 * modifying the file collection separately (there is no time to configure it before AGP has
 * initialized and disallowed changes). This uses reflection to temporarily allow changes, and
 * apply [block].
 */
private fun ConfigurableFileCollection.withChangesAllowed(
    block: ConfigurableFileCollection.() -> Unit
) {
    val disallowChanges = this::class.java.getDeclaredField("disallowChanges")
    disallowChanges.isAccessible = true
    disallowChanges.set(this, false)
    block()
    disallowChanges.set(this, true)
}

/**
 * General purpose implementation of a transitive closure
 * - Recursion free
 * - Predictable amount of allocations
 * - Handles loops and self references gracefully
 * @param edges: Producer function from one node to all its children. This implementation can handle loops and self references gracefully.
 * @return Note: No guarantees given about the order ot this [Set]
 */
public inline fun <reified T> transitiveClosure(seed: T, edges: T.() -> Iterable<T>): Set<T> {
    // Fast path when initial edges are empty
    val initialEdges = seed.edges()
    if (initialEdges is Collection && initialEdges.isEmpty()) return emptySet()

    val queue = deque<T>(initialEdges.count() * 2)
    val results = mutableSetOf<T>()
    queue.addAll(initialEdges)
    while (queue.isNotEmpty()) {
        // ArrayDeque implementation will optimize this call to 'removeFirst'
        val resolved = queue.removeAt(0)
        if (resolved != seed && results.add(resolved)) {
            queue.addAll(resolved.edges())
        }
    }

    return results.toSet()
}

@OptIn(ExperimentalStdlibApi::class)
@PublishedApi
internal inline fun <reified T> deque(initialSize: Int): MutableList<T> {
    return if (KotlinVersion.CURRENT.isAtLeast(1, 4)) ArrayDeque(initialSize)
    else ArrayList(initialSize)
}
