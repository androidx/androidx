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

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

const val zipComposeReportsTaskName = "zipComposeCompilerReports"
const val zipComposeMetricsTaskName = "zipComposeCompilerMetrics"

/** Plugin to apply common configuration for Compose projects. */
class AndroidXComposeImplPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create<AndroidXComposeExtension>("androidxCompose", project)
        project.plugins.configureEach { plugin ->
            when (plugin) {
                is AppPlugin,
                is LibraryPlugin -> {
                    project.configureAndroidCommonOptions()
                }
                is KotlinBasePluginWrapper -> {
                    configureComposeCompilerPlugin(project, extension)
                }
            }
        }
    }

    companion object {
        private fun Project.configureAndroidCommonOptions() {
            extensions.findByType(AndroidComponentsExtension::class.java)!!.finalizeDsl { android ->
                val isPublished = androidXExtension.shouldPublish()

                android.lint {
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
        val configuration =
            project.configurations.create(COMPILER_PLUGIN_CONFIGURATION) {
                it.isCanBeConsumed = false
            }
        // Add Compose compiler plugin to kotlinPlugin configuration, making sure it works
        // for Playground builds as well
        val compilerPluginVersion = project.getVersionByName("kotlin")
        project.dependencies.add(
            COMPILER_PLUGIN_CONFIGURATION,
            "org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:$compilerPluginVersion"
        )

        if (
            !ProjectLayoutType.isPlayground(project) &&
                // ksp is also a compiler plugin, updating Kotlin for it will likely break the build
                !project.plugins.hasPlugin("com.google.devtools.ksp")
        ) {
            if (compilerPluginVersion.endsWith("-SNAPSHOT")) {
                // use exact project path instead of subprojects.find, it is faster
                val compilerProject = project.rootProject.resolveProject(":compose")
                val compilerMavenDirectory =
                    File(
                        compilerProject.projectDir,
                        "compiler/compose-compiler-snapshot-repository"
                    )
                project.repositories.maven { it.url = compilerMavenDirectory.toURI() }
                project.configurations.configureEach {
                    it.resolutionStrategy.eachDependency { dep ->
                        val requested = dep.requested
                        if (
                            requested.group == "org.jetbrains.kotlin" &&
                                (requested.name == "kotlin-compiler-embeddable" ||
                                    requested.name == "kotlin-compose-compiler-plugin-embeddable")
                        ) {
                            dep.useVersion(compilerPluginVersion)
                        }
                    }
                }
            }
        }

        val kotlinPluginProvider =
            project.provider {
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
            }

        val enableMetrics = project.enableComposeCompilerMetrics()
        val enableReports = project.enableComposeCompilerReports()

        val compileTasks = project.tasks.withType(KotlinCompile::class.java)

        compileTasks.configureEach { compile ->
            compile.inputs.property("composeMetricsEnabled", enableMetrics)
            compile.inputs.property("composeReportsEnabled", enableReports)

            compile.pluginClasspath.from(kotlinPluginProvider.get())

            // todo(b/291587160): enable when Compose compiler 2.0.20 is merged
            // compile.enableFeatureFlag(ComposeFeatureFlag.StrongSkipping)
            // compile.enableFeatureFlag(ComposeFeatureFlag.OptimizeNonSkippingGroups)
            compile.addPluginOption(ComposeCompileOptions.StrongSkipping, "true")
            compile.addPluginOption(ComposeCompileOptions.NonSkippingGroupOptimization, "true")
            if (shouldPublish) {
                compile.addPluginOption(ComposeCompileOptions.SourceOption, "true")
            }
        }

        if (enableMetrics) {
            project.rootProject.tasks.named(zipComposeMetricsTaskName).configure { zipTask ->
                zipTask.dependsOn(compileTasks)
            }

            val metricsIntermediateDir = project.compilerMetricsIntermediatesDir()
            compileTasks.configureEach { compile ->
                compile.addPluginOption(
                    ComposeCompileOptions.MetricsOption,
                    metricsIntermediateDir.path
                )
            }
        }
        if (enableReports) {
            project.rootProject.tasks.named(zipComposeReportsTaskName).configure { zipTask ->
                zipTask.dependsOn(compileTasks)
            }

            val reportsIntermediateDir = project.compilerReportsIntermediatesDir()
            compileTasks.configureEach { compile ->
                compile.addPluginOption(
                    ComposeCompileOptions.ReportsOption,
                    reportsIntermediateDir.path
                )
            }
        }
    }
}

private fun KotlinCompile.addPluginOption(
    composeCompileOptions: ComposeCompileOptions,
    value: String
) =
    pluginOptions.add(
        CompilerPluginConfig().apply {
            addPluginArgument(
                composeCompileOptions.pluginId,
                SubpluginOption(composeCompileOptions.key, value)
            )
        }
    )

private fun KotlinCompile.enableFeatureFlag(featureFlag: ComposeFeatureFlag) {
    addPluginOption(ComposeCompileOptions.FeatureFlagOption, featureFlag.featureName)
}

private fun KotlinCompile.disableFeatureFlag(featureFlag: ComposeFeatureFlag) {
    addPluginOption(ComposeCompileOptions.FeatureFlagOption, "-${featureFlag.featureName}")
}

internal fun Project.zipComposeCompilerMetrics() {
    if (project.enableComposeCompilerMetrics()) {
        val zipComposeMetrics =
            project.tasks.register(zipComposeMetricsTaskName, Zip::class.java) { zipTask ->
                zipTask.from(project.compilerMetricsIntermediatesDir())
                zipTask.destinationDirectory.set(project.composeCompilerDataDir())
                zipTask.archiveBaseName.set("composemetrics")
            }
        project.addToBuildOnServer(zipComposeMetrics)
    }
}

internal fun Project.zipComposeCompilerReports() {
    if (project.enableComposeCompilerReports()) {
        val zipComposeReports =
            project.tasks.register(zipComposeReportsTaskName, Zip::class.java) { zipTask ->
                zipTask.from(project.compilerReportsIntermediatesDir())
                zipTask.destinationDirectory.set(project.composeCompilerDataDir())
                zipTask.archiveBaseName.set("composereports")
            }
        project.addToBuildOnServer(zipComposeReports)
    }
}

private fun Project.compilerMetricsIntermediatesDir(): File {
    return project.rootProject.layout.buildDirectory
        .dir("libraryreports/composemetrics")
        .get()
        .asFile
}

private fun Project.compilerReportsIntermediatesDir(): File {
    return project.rootProject.layout.buildDirectory
        .dir("libraryreports/composereports")
        .get()
        .asFile
}

private fun Project.composeCompilerDataDir(): File {
    return File(getDistributionDirectory(), "compose-compiler-data")
}

private const val ComposePluginId = "androidx.compose.compiler.plugins.kotlin"

private enum class ComposeCompileOptions(val pluginId: String, val key: String) {
    SourceOption(ComposePluginId, "sourceInformation"),
    StrongSkipping(ComposePluginId, "strongSkipping"),
    NonSkippingGroupOptimization(ComposePluginId, "nonSkippingGroupOptimization"),
    MetricsOption(ComposePluginId, "metricsDestination"),
    ReportsOption(ComposePluginId, "reportsDestination"),
    FeatureFlagOption(ComposePluginId, "featureFlag"),
}

private enum class ComposeFeatureFlag(val featureName: String) {
    StrongSkipping("StrongSkipping"),
    OptimizeNonSkippingGroups("OptimizeNonSkippingGroups"),
}
