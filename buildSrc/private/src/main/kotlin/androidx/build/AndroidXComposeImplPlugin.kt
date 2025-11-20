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
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig
import org.jetbrains.kotlin.gradle.plugin.KotlinBaseApiPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/** Plugin to apply common configuration for Compose projects. */
class AndroidXComposeImplPlugin : Plugin<Project> {
    override fun apply(project: Project) {
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
                is KotlinBasePluginWrapper,
                is KotlinBaseApiPlugin -> {
                    configureComposeCompilerPlugin(project)
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

                // If the ComposeTestRuleDispatcher lint rule isn't disabled in the module's
                // build.gradle file, it will be added to the error set.
                if (!disable.contains("ComposeTestRuleDispatcher")) {
                    error.add("ComposeTestRuleDispatcher")
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

                // Disable lambda creation in subcompose check in projects where we're less
                // concerned about performance.
                if (
                    androidXExtension.type == SoftwareType.TEST_APPLICATION ||
                        androidXExtension.type == SoftwareType.PUBLISHED_KOTLIN_ONLY_TEST_LIBRARY ||
                        androidXExtension.type == SoftwareType.PUBLISHED_TEST_LIBRARY ||
                        androidXExtension.type == SoftwareType.SAMPLES ||
                        androidXExtension.type == SoftwareType.UNSET
                ) {
                    disable.add("ComposableLambdaInMeasurePolicy")
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

private fun configureComposeCompilerPlugin(project: Project) {
    project.afterEvaluate {
        // Add Compose compiler plugin to kotlinPlugin configuration, making sure it works
        // for Playground builds as well
        val isPlayground = ProjectLayoutType.isPlayground(project)
        val compilerPluginVersion =
            project.getVersionByName(if (isPlayground) "kotlin" else "composeCompilerPlugin")
        // Create configuration that we'll use to load Compose compiler plugin
        val configuration =
            project.configurations.detachedConfiguration(
                project.dependencies.create(
                    "org.jetbrains.kotlin:kotlin-compose-compiler-plugin-embeddable:$compilerPluginVersion"
                )
            )

        if (
            compilerPluginVersion.endsWith("-SNAPSHOT") &&
                !isPlayground &&
                // ksp is also a compiler plugin, updating Kotlin for it will likely break the build
                !project.plugins.hasPlugin("com.google.devtools.ksp")
        ) {
            // use exact project path instead of subprojects.find, it is faster
            val compilerProject = project.rootProject.resolveProject(":compose")
            val compilerMavenDirectory =
                File(compilerProject.projectDir, "compiler/compose-compiler-snapshot-repository")
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

        val kotlinPlugin =
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

        project.tasks.withType(KotlinCompilationTask::class.java).configureEach { compile ->
            compile.applyPlugin(kotlinPlugin)

            compile.addPluginOption(ComposeCompileOptions.SourceOption, "true")
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

private const val ComposePluginId = "androidx.compose.compiler.plugins.kotlin"

private enum class ComposeCompileOptions(val pluginId: String, val key: String) {
    SourceOption(ComposePluginId, "sourceInformation"),
    StrongSkipping(ComposePluginId, "strongSkipping"),
    NonSkippingGroupOptimization(ComposePluginId, "nonSkippingGroupOptimization"),
    FeatureFlagOption(ComposePluginId, "featureFlag"),
}

private enum class ComposeFeatureFlag(val featureName: String) {
    StrongSkipping("StrongSkipping"),
    OptimizeNonSkippingGroups("OptimizeNonSkippingGroups"),
    PausableComposition("PausableComposition"),
}
