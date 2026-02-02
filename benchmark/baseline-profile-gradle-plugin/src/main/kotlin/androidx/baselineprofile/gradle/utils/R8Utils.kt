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
import com.android.build.api.variant.Variant
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

internal class R8Utils(private val project: Project) {

    companion object {
        private const val PROPERTY_R8_REWRITE_BASELINE_PROFILE_RULES =
            "android.experimental.art-profile-r8-rewriting"
        private const val PROPERTY_R8_DEX_LAYOUT_OPTIMIZATION =
            "android.experimental.r8.dex-startup-optimization"
    }

    @Suppress("UnstableApiUsage")
    fun setRulesRewriteForVariantEnabled(variant: Variant, value: Boolean) {

        // Checks the AGP min version to support this.
        if (project.agpVersion() < AndroidPluginVersion(8, 1, 0).alpha(8)) {
            if (!project.isGradleSyncRunning()) {
                throw IllegalStateException(
                    """
                Unable to set baseline profile rules rewrite property in module `${project.path}`
                due to minimum AGP version requirement not met. This functionality requires at
                least AGP version 8.1.0. Please check your module build.gradle file and ensure
                the property `baselineProfileRulesRewrite` is not set.
                """
                        .trimIndent()
                )
            }
            return
        }

        // TODO: Note that currently there needs to be at least a baseline profile,
        //  even if empty. For this reason we always add a src set that points to
        //  an empty file. This can removed after b/271158087 is fixed.
        if (value) GenerateDummyBaselineProfileTask.setupForVariant(project, variant)

        // Sets the experimental property
        variant.experimentalProperties.put(PROPERTY_R8_REWRITE_BASELINE_PROFILE_RULES, value)
    }

    @Suppress("UnstableApiUsage")
    fun setDexLayoutOptimizationEnabled(variant: Variant, value: Boolean) {

        // Checks the AGP min version to support this.
        if (project.agpVersion() < AndroidPluginVersion(8, 1, 0).alpha(11)) {
            if (!project.isGradleSyncRunning()) {
                throw IllegalStateException(
                    """
                Unable to set dex layout optimization property in module `${project.path}` due to
                minimum AGP version requirement not met. This functionality requires at least AGP
                version 8.1.0. Please check your module build.gradle file and ensure the property
                `dexLayoutOptimization` is not set.
                """
                        .trimIndent()
                )
            }
        }

        // Sets the experimental property
        variant.experimentalProperties.put(PROPERTY_R8_DEX_LAYOUT_OPTIMIZATION, value)
    }
}

@DisableCachingByDefault(because = "Not worth caching.")
abstract class GenerateDummyBaselineProfileTask : DefaultTask() {

    companion object {
        internal fun setupForVariant(project: Project, variant: Variant) {
            val taskProvider =
                project.tasks.maybeRegister<GenerateDummyBaselineProfileTask>(
                    "generate",
                    variant.name,
                    "profileForR8RuleRewrite"
                ) {
                    it.outputDir.set(
                        project.layout.buildDirectory.dir(
                            "$INTERMEDIATES_BASE_FOLDER/${variant.name}/empty/"
                        )
                    )
                    it.variantName.set(variant.name)
                }
            @Suppress("UnstableApiUsage")
            variant.sources.baselineProfiles?.addGeneratedSourceDirectory(
                taskProvider,
                GenerateDummyBaselineProfileTask::outputDir
            )
        }
    }

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @get:Input abstract val variantName: Property<String>

    @TaskAction
    fun exec() {
        outputDir.file("empty-baseline-prof.txt").get().asFile.writeText("Lignore/This;")
    }
}
