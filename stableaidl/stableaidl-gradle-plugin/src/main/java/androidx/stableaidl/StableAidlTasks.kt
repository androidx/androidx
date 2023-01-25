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

package androidx.stableaidl

import androidx.stableaidl.tasks.StableAidlCheckApi
import androidx.stableaidl.tasks.StableAidlCompile
import androidx.stableaidl.tasks.UpdateStableAidlApiTask
import com.android.build.api.variant.SourceDirectories
import com.android.build.api.variant.Variant
import com.android.build.gradle.BaseExtension
import com.android.utils.usLocaleCapitalize
import java.io.File
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

private const val TASK_GROUP_API = "API"

@Suppress("UnstableApiUsage") // SourceDirectories.Flat
fun registerCompileAidlApi(
    project: Project,
    baseExtension: BaseExtension,
    variant: Variant,
    sourceDir: SourceDirectories.Flat,
    importsDir: SourceDirectories.Flat,
    outputDir: Provider<Directory>
): TaskProvider<StableAidlCompile> = project.tasks.register(
    computeTaskName("compile", variant, "AidlApi"),
    StableAidlCompile::class.java
) { task ->
    task.group = TASK_GROUP_API
    task.description = "Compiles AIDL source code"
    task.variantName = variant.name
    task.configureBuildToolsFrom(baseExtension)
    task.configurePackageDirFrom(project, variant)
    task.sourceDirs.set(sourceDir.all)
    task.sourceOutputDir.set(outputDir)
    task.importDirs.set(importsDir.all)
    task.extraArgs.set(
        listOf(
            "--structured"
        )
    )
}

@Suppress("UnstableApiUsage") // SourceDirectories.Flat
fun registerGenerateAidlApi(
    project: Project,
    baseExtension: BaseExtension,
    variant: Variant,
    sourceDir: SourceDirectories.Flat,
    importsDir: SourceDirectories.Flat,
    builtApiDir: Provider<Directory>,
    compileAidlApiTask: Provider<StableAidlCompile>
): TaskProvider<StableAidlCompile> = project.tasks.register(
    computeTaskName("generate", variant, "AidlApi"),
    StableAidlCompile::class.java
) { task ->
    task.group = TASK_GROUP_API
    task.description = "Generates API files from AIDL source code"
    task.variantName = variant.name
    task.configureBuildToolsFrom(baseExtension)
    task.configurePackageDirFrom(project, variant)
    task.sourceDirs.set(sourceDir.all)
    task.sourceOutputDir.set(builtApiDir)
    task.importDirs.set(importsDir.all)
    task.extraArgs.set(
        listOf(
            "--structured",
            "--dumpapi"
        )
    )
    task.dependsOn(compileAidlApiTask)
}

// Policy: If the artifact has previously been released, e.g. has a beta or later API file
// checked in, then we must verify "release compatibility" against the work-in-progress
// API file.
@Suppress("UnstableApiUsage") // SourceDirectories.Flat
fun registerCheckApiAidlRelease(
    project: Project,
    baseExtension: BaseExtension,
    variant: Variant,
    importsDir: SourceDirectories.Flat,
    lastReleasedApiDir: Directory,
    generateAidlTask: Provider<StableAidlCompile>
): TaskProvider<StableAidlCheckApi> = project.tasks.register(
    computeTaskName("check", variant, "AidlApiRelease"),
    StableAidlCheckApi::class.java
) { task ->
    task.group = TASK_GROUP_API
    task.description = "Checks the AIDL source code API surface against the " +
        "stabilized AIDL API files"
    task.variantName = variant.name
    task.configureBuildToolsFrom(baseExtension)
    task.importDirs.set(importsDir.all)
    task.checkApiMode.set(StableAidlCheckApi.MODE_COMPATIBLE)
    task.expectedApiDir.set(lastReleasedApiDir)
    task.actualApiDir.set(generateAidlTask.flatMap { it.sourceOutputDir })
    task.failOnMissingExpected.set(false)
    task.cacheEvenIfNoOutputs()
}

// Policy: All changes to API surfaces for which compatibility is enforced must be
// explicitly confirmed by running the updateApi task. To enforce this, the implementation
// checks the "work-in-progress" built API file against the checked in current API file.
@Suppress("UnstableApiUsage") // SourceDirectories.Flat
fun registerCheckAidlApi(
    project: Project,
    baseExtension: BaseExtension,
    variant: Variant,
    importsDir: SourceDirectories.Flat,
    lastCheckedInApiFile: Directory,
    generateAidlTask: Provider<StableAidlCompile>,
    checkAidlApiReleaseTask: Provider<StableAidlCheckApi>
): TaskProvider<StableAidlCheckApi> = project.tasks.register(
    computeTaskName("check", variant, "AidlApi"),
    StableAidlCheckApi::class.java
) { task ->
    task.group = TASK_GROUP_API
    task.description = "Checks the AIDL source code API surface against the checked-in " +
        "AIDL API files"
    task.variantName = variant.name
    task.configureBuildToolsFrom(baseExtension)
    task.importDirs.set(importsDir.all)
    task.checkApiMode.set(StableAidlCheckApi.MODE_EQUAL)
    task.expectedApiDir.set(lastCheckedInApiFile)
    task.actualApiDir.set(generateAidlTask.flatMap { it.sourceOutputDir })
    task.failOnMissingExpected.set(true)
    task.cacheEvenIfNoOutputs()
    task.dependsOn(checkAidlApiReleaseTask)
}

fun registerUpdateAidlApi(
    project: Project,
    variant: Variant,
    lastCheckedInApiFile: Directory,
    generateAidlTask: Provider<StableAidlCompile>,
): TaskProvider<UpdateStableAidlApiTask> = project.tasks.register(
    computeTaskName("update", variant, "AidlApi"),
    UpdateStableAidlApiTask::class.java
) { task ->
    task.group = TASK_GROUP_API
    task.description = "Updates the checked-in AIDL API files to AIDL match source code " +
        "API surface"
    task.apiLocation.set(generateAidlTask.flatMap { it.sourceOutputDir })
    task.outputApiLocations.set(listOf(lastCheckedInApiFile.asFile))
    task.forceUpdate.set(project.providers.gradleProperty("force").isPresent)
}

/**
 * Tells Gradle to skip running this task, even if this task declares no output files.
 */
private fun Task.cacheEvenIfNoOutputs() {
    this.outputs.file(this.getPlaceholderOutput())
}

/**
 * Returns an unused output path that we can pass to Gradle to prevent Gradle from thinking that we
 * forgot to declare outputs of this task, and instead to skip this task if its inputs are
 * unchanged.
 */
private fun Task.getPlaceholderOutput(): File {
    return File(this.project.buildDir, "placeholderOutput/" + this.name.replace(":", "-"))
}

private fun computeTaskName(prefix: String, variant: Variant, suffix: String) =
    "$prefix${variant.name.usLocaleCapitalize()}$suffix"
