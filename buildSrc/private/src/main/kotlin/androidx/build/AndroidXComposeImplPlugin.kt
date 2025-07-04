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

import com.android.build.api.dsl.Lint
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LintLifecycleExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.KotlinMultiplatformAndroidPlugin
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

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
                    project.extensions
                        .findByType(LintLifecycleExtension::class.java)!!
                        .finalizeDsl { project.configureAndroidCommonOptions(it) }
                }
                is KotlinMultiplatformAndroidPlugin -> {
                    project.extensions
                        .getByType<KotlinMultiplatformAndroidComponentsExtension>()
                        .finalizeDsl { project.configureAndroidCommonOptions(it.lint) }
                }
                is KotlinBasePluginWrapper -> {
                    configureComposeCompilerPlugin(project, extension)
                }
            }
        }
    }

    companion object {
        private fun Project.configureAndroidCommonOptions(lint: Lint) {
            val isPublished = androidXExtension.shouldPublish()

            lint.apply {
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
                        "wear:compose:compose-navigation",
                    )

                // Disable ListIterator if we are not in a matching path, or we are in an
                // unpublished project
                if (ignoreListIteratorFilter.any { path.contains(it) } || !isPublished) {
                    disable.add("ListIterator")
                }

                // b/333784604 Disable ConfigurationScreenWidthHeight for wear libraries, it
                // does not apply to wear
                if (path.startsWith(":wear:")) {
                    disable.add("ConfigurationScreenWidthHeight")
                }

                // These checks are not required for samples projects.
                if (androidXExtension.type == SoftwareType.SAMPLES) {
                    disable.add("ListIterator")
                    disable.add("PrimitiveInCollection")
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
                            "configuration" to "shadow",
                        )
                    ),
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

        // Create configuration that we'll use to load Compose compiler plugin
        val configuration =
            project.configurations.create(COMPILER_PLUGIN_CONFIGURATION) {
                it.isCanBeConsumed = false
            }
        // Add Compose compiler plugin to kotlinPlugin configuration, making sure it works
        // for Playground builds as well
        val isPlayground = ProjectLayoutType.isPlayground(project)
        val compilerPluginVersion =
            project.getVersionByName(if (isPlayground) "kotlin" else "composeCompilerPlugin")
        project.dependencies.add(
            COMPILER_PLUGIN_CONFIGURATION,
            "org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:$compilerPluginVersion",
        )

        if (
            !isPlayground &&
                // ksp is also a compiler plugin, updating Kotlin for it will likely break the build
                !project.plugins.hasPlugin("com.google.devtools.ksp")
        ) {
            if (compilerPluginVersion.endsWith("-SNAPSHOT")) {
                // use exact project path instead of subprojects.find, it is faster
                val compilerProject = project.rootProject.resolveProject(":compose")
                val compilerMavenDirectory =
                    File(
                        compilerProject.projectDir,
                        "compiler/compose-compiler-snapshot-repository",
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
                                ArtifactTypeDefinition.JAR_TYPE,
                            )
                        }
                    }
                    .files
            }

        val enableMetrics = project.enableComposeCompilerMetrics()
        val enableReports = project.enableComposeCompilerReports()

        val compileTasks = project.tasks.withType(KotlinCompilationTask::class.java)

        compileTasks.configureEach { compile ->
            compile.inputs.property("composeMetricsEnabled", enableMetrics)
            compile.inputs.property("composeReportsEnabled", enableReports)

            compile.applyPlugin(kotlinPluginProvider.get())

            compile.enableFeatureFlag(ComposeFeatureFlag.OptimizeNonSkippingGroups)
            compile.enableFeatureFlag(ComposeFeatureFlag.PausableComposition)

            compile.addPluginOption(ComposeCompileOptions.SourceOption, "true")
        }

        if (enableMetrics) {
            project.rootProject.tasks.named(zipComposeMetricsTaskName).configure { zipTask ->
                zipTask.dependsOn(compileTasks)
            }

            val metricsIntermediateDir = project.compilerMetricsIntermediatesDir()
            compileTasks.configureEach { compile ->
                compile.addPluginOption(
                    ComposeCompileOptions.MetricsOption,
                    metricsIntermediateDir.path,
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
                    reportsIntermediateDir.path,
                )
            }
        }
    }
}

private fun KotlinCompilationTask<*>.applyPlugin(plugins: FileCollection) =
    when (this) {
        is AbstractKotlinCompile<*> -> pluginClasspath.from(plugins)
        is AbstractKotlinNativeCompile<*, *> -> compilerPluginClasspath = plugins
        else -> throw IllegalStateException("Unsupported Kotlin compilation task type")
    }

private fun KotlinCompilationTask<*>.addPluginArgument(pluginId: String, option: SubpluginOption) =
    when (this) {
        is AbstractKotlinCompile<*> ->
            pluginOptions.add(CompilerPluginConfig().apply { addPluginArgument(pluginId, option) })
        is AbstractKotlinNativeCompile<*, *> ->
            compilerPluginOptions.addPluginArgument(pluginId, option)
        else -> throw IllegalStateException("Unsupported Kotlin compilation task type")
    }

private fun KotlinCompilationTask<*>.addPluginOption(
    composeCompileOptions: ComposeCompileOptions,
    value: String,
) =
    addPluginArgument(
        pluginId = composeCompileOptions.pluginId,
        option = SubpluginOption(composeCompileOptions.key, value),
    )

private fun KotlinCompilationTask<*>.enableFeatureFlag(featureFlag: ComposeFeatureFlag) {
    addPluginOption(ComposeCompileOptions.FeatureFlagOption, featureFlag.featureName)
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
    PausableComposition("PausableComposition"),
}
