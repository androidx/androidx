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

package androidx.baselineprofile.gradle.consumer.task

import androidx.baselineprofile.gradle.utils.BaselineProfilePluginLogger
import androidx.baselineprofile.gradle.utils.TASK_NAME_SUFFIX
import androidx.baselineprofile.gradle.utils.Warnings
import androidx.baselineprofile.gradle.utils.maybeRegister
import com.android.build.gradle.internal.tasks.BuildAnalyzer
import com.android.buildanalyzer.common.TaskCategory
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault

private const val GENERATE_TASK_NAME = "generate"

/** This task does nothing and it's only to start the generation process. */
@DisableCachingByDefault(because = "Not worth caching.")
@BuildAnalyzer(primaryTaskCategory = TaskCategory.OPTIMIZATION)
internal abstract class GenerateBaselineProfileTask : DefaultTask() {

    companion object {
        fun maybeCreate(
            project: Project,
            variantName: String,
            lastTaskProvider: TaskProvider<*>? = null,
        ) =
            project.tasks.maybeRegister<GenerateBaselineProfileTask>(
                GENERATE_TASK_NAME,
                variantName,
                TASK_NAME_SUFFIX
            ) {
                if (lastTaskProvider != null) it.dependsOn(lastTaskProvider)
            }
    }

    init {
        group = "Baseline Profile"
        description = "Generates a baseline profile for the specified variants or dimensions."
    }
}

/**
 * This task does nothing and it's only to start the generation process. This task differs from
 * [GenerateBaselineProfileTask] and it's used ONLY to print the warning about
 * `generateBaselineProfile` generating only for `release` with AGP 8.0.
 */
@DisableCachingByDefault(because = "Not worth caching.")
@BuildAnalyzer(primaryTaskCategory = TaskCategory.OPTIMIZATION)
internal abstract class MainGenerateBaselineProfileTaskForAgp80Only : DefaultTask() {

    companion object {
        fun maybeCreate(
            project: Project,
            variantName: String,
            lastTaskProvider: TaskProvider<*>? = null,
            warnings: Warnings
        ) =
            project.tasks.maybeRegister<MainGenerateBaselineProfileTaskForAgp80Only>(
                GENERATE_TASK_NAME,
                variantName,
                TASK_NAME_SUFFIX
            ) {
                if (lastTaskProvider != null) it.dependsOn(lastTaskProvider)
                it.printWarningMultipleBuildTypesWithAgp80.set(warnings.multipleBuildTypesWithAgp80)
            }
    }

    init {
        group = "Baseline Profile"
        description = "Generates a baseline profile for the `release` variant."
    }

    @get:Input abstract val printWarningMultipleBuildTypesWithAgp80: Property<Boolean>

    private val logger by lazy { BaselineProfilePluginLogger(this.getLogger()) }

    @TaskAction
    fun exec() {
        this.logger.warn(
            property = { printWarningMultipleBuildTypesWithAgp80.get() },
            propertyName = "multipleBuildTypesWithAgp80",
            message =
                """
        The task `generateBaselineProfile` does not support generating baseline profiles for 
        multiple build types with AGP 8.0.

        Only baseline profile for variants of build type `release` will be generated.
        With AGP 8.0, this command behaves like `generateReleaseBaselineProfile`.

        If you intend to generate profiles for multiple build types using AGP 8.0 you'll 
        need to run separate gradle commands for each build type.
        Example: `generateReleaseBaselineProfile` and `generateAnotherReleaseBaselineProfile`.

        Details on https://issuetracker.google.com/issue?id=270433400.
            """
                    .trimIndent()
        )
    }
}
