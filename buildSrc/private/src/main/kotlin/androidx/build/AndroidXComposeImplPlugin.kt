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
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.ClasspathNormalizer
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

const val composeSourceOption =
    "plugin:androidx.compose.compiler.plugins.kotlin:sourceInformation=true"
const val composeMetricsOption =
    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination"
const val composeReportsOption =
    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination"
const val enableMetricsArg = "androidx.enableComposeCompilerMetrics"
const val enableReportsArg = "androidx.enableComposeCompilerReports"

/** Plugin to apply common configuration for Compose projects. */
class AndroidXComposeImplPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create<AndroidXComposeExtension>("androidxCompose", project)
        project.plugins.all { plugin ->
            when (plugin) {
                is LibraryPlugin -> {
                    val library =
                        project.extensions.findByType(LibraryExtension::class.java)
                            ?: throw Exception("Failed to find Android extension")

                    project.configureAndroidCommonOptions(library)
                }
                is AppPlugin -> {
                    val app =
                        project.extensions.findByType(AppExtension::class.java)
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
        private fun Project.configureAndroidCommonOptions(testedExtension: TestedExtension) {
            testedExtension.defaultConfig.minSdk = 21

            extensions.findByType(AndroidComponentsExtension::class.java)!!.finalizeDsl {
                val isPublished = androidXExtension.shouldPublish()

                it.lint {
                    // These lint checks are normally a warning (or lower), but we ignore (in
                    // AndroidX)
                    // warnings in Lint, so we make it an error here so it will fail the build.
                    // Note that this causes 'UnknownIssueId' lint warnings in the build log when
                    // Lint tries to apply this rule to modules that do not have this lint check, so
                    // we disable that check too
                    disable.add("UnknownIssueId")
                    error.addAll(ComposeLintWarningIdsToTreatAsErrors)

                    // Paths we want to disable ListIteratorChecks for
                    val ignoreListIteratorFilter =
                        listOf(
                            // These are not runtime libraries and so Iterator allocation is not
                            // relevant.
                            "compose:ui:ui-test",
                            "compose:ui:ui-tooling",
                            "compose:ui:ui-inspection",
                            // Navigation libraries are not in performance critical paths, so we can
                            // ignore them.
                            "navigation:navigation-compose",
                            "wear:compose:compose-navigation"
                        )

                    // Disable ListIterator if we are not in a matching path, or we are in an
                    // unpublished project
                    if (ignoreListIteratorFilter.any { path.contains(it) } || !isPublished) {
                        disable.add("ListIterator")
                    }
                }
            }

            if (!allowMissingLintProject()) {
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
        }

        /**
         * General configuration for MPP projects. In the future, these workarounds should either be
         * generified and added to AndroidXPlugin, or removed as/when the underlying issues have
         * been resolved.
         */
        private fun Project.configureForMultiplatform() {
            // This is to allow K/N not matching the kotlinVersion
            (this.rootProject.property("ext") as ExtraPropertiesExtension).set(
                "kotlin.native.version",
                KOTLIN_NATIVE_VERSION
            )

            val multiplatformExtension =
                checkNotNull(multiplatformExtension) {
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
                it.languageSettings.apply { optIn("kotlin.ExperimentalMultiplatform") }
            }

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

private fun configureComposeCompilerPlugin(project: Project, extension: AndroidXComposeExtension) {
    project.afterEvaluate {
        // If a project has opted-out of Compose compiler plugin, don't add it
        if (!extension.composeCompilerPluginEnabled) return@afterEvaluate

        val androidXExtension =
            project.extensions.findByType(AndroidXExtension::class.java)
                ?: throw Exception("You have applied AndroidXComposePlugin without AndroidXPlugin")
        val shouldPublish = androidXExtension.shouldPublish()

        // Create configuration that we'll use to load Compose compiler plugin
        val configuration = project.configurations.create(COMPILER_PLUGIN_CONFIGURATION)
        // Add Compose compiler plugin to kotlinPlugin configuration, making sure it works
        // for Playground builds as well
        project.dependencies.add(
            COMPILER_PLUGIN_CONFIGURATION,
            if (ProjectLayoutType.isPlayground(project)) {
                AndroidXPlaygroundRootImplPlugin.projectOrArtifact(
                    project.rootProject,
                    ":compose:compiler:compiler"
                )
            } else {
                project.rootProject.resolveProject(":compose:compiler:compiler")
            }
        )
        val kotlinPlugin =
            configuration.incoming
                .artifactView { view ->
                    view.attributes { attributes ->
                        attributes.attribute(
                            Attribute.of("artifactType", String::class.java),
                            ArtifactTypeDefinition.JAR_TYPE
                        )
                    }
                }
                .files

        val enableMetricsProvider = project.providers.gradleProperty(enableMetricsArg)
        val enableReportsProvider = project.providers.gradleProperty(enableReportsArg)

        val libraryMetricsDirectory = project.rootProject.getLibraryMetricsDirectory()
        val libraryReportsDirectory = project.rootProject.getLibraryReportsDirectory()
        project.tasks.withType(KotlinCompile::class.java).configureEach { compile ->
            // Append inputs to KotlinCompile so tasks get invalidated if any of these values change
            compile.inputs
                .files({ kotlinPlugin })
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
                        listOf("-P", "$composeMetricsOption=${metricsDest.absolutePath}")
                }
                if ((enableReportsProvider.orNull == "true")) {
                    val reportsDest = File(libraryReportsDirectory, "compose")
                    compile.kotlinOptions.freeCompilerArgs +=
                        listOf("-P", "$composeReportsOption=${reportsDest.absolutePath}")
                }
                if (shouldPublish) {
                    compile.kotlinOptions.freeCompilerArgs += listOf("-P", composeSourceOption)
                }
            }
        }
    }
}
