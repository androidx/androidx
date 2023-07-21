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
import androidx.stableaidl.tasks.StableAidlPackageApi
import androidx.stableaidl.tasks.UpdateStableAidlApiTask
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.SourceDirectories
import com.android.build.api.variant.Variant
import com.android.utils.usLocaleCapitalize
import java.io.File
import org.gradle.api.Action
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ConfigurationVariant
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider

// Gradle task group used to identify Stable AIDL tasks.
private const val TASK_GROUP_API = "API"

// Artifact type value used by AGP to identify a regular AIDL artifact.
private const val ARTIFACT_TYPE_AIDL = "android-aidl"

// Artifact type value used by Stable AIDL plugin to identify a Stable AIDL artifact.
private const val ARTIFACT_TYPE_STABLE_AIDL = "androidx-stable-aidl"

class ArtifactType {
    companion object {
        private val KEY = Attribute.of("artifactType", String::class.java)

        /**
         * Artifact type for filtering on AIDL artifacts.
         */
        internal val AIDL: Action<AttributeContainer> = Action { container ->
            // Value inlined from AGP's internal `ArtifactType.AIDL` constant.
            container.attribute(KEY, ARTIFACT_TYPE_AIDL)
        }

        /**
         * Artifact type for filtering on Stable AIDL artifacts.
         */
        internal val STABLE_AIDL: Action<AttributeContainer> = Action { container ->
            container.attribute(KEY, ARTIFACT_TYPE_STABLE_AIDL)
        }
    }
}

@Suppress("UnstableApiUsage") // SourceDirectories.Flat
fun registerCompileAidlApi(
    project: Project,
    variant: Variant,
    aidlExecutable: Provider<RegularFile>,
    aidlFramework: Provider<RegularFile>,
    sourceDir: SourceDirectories.Flat,
    packagedDir: Provider<Directory>,
    importsDir: SourceDirectories.Flat,
    depImports: List<FileCollection>,
    outputDir: Provider<Directory>
): TaskProvider<StableAidlCompile> = project.tasks.register(
    computeTaskName("compile", variant, "AidlApi"),
    StableAidlCompile::class.java
) { task ->
    task.group = TASK_GROUP_API
    task.description = "Compiles AIDL source code"
    task.variantName = variant.name
    task.aidlExecutable.set(aidlExecutable)
    task.aidlFrameworkProvider.set(aidlFramework)
    task.sourceDirs.set(sourceDir.all)
    task.sourceOutputDir.set(outputDir)
    task.packagedDir.set(packagedDir)
    task.importDirs.set(importsDir.all)
    depImports.forEach { task.dependencyImportDirs.addAll(it.elements) }
    task.extraArgs.set(
        listOf(
            "--structured"
        )
    )
}.also { taskProvider ->
    variant.sources.java?.addGeneratedSourceDirectory(
        taskProvider,
        StableAidlCompile::sourceOutputDir
    )

    // The API elements config is used by the compile classpath.
    val targetConfig = "${variant.name}ApiElements"

    // Register packaged output for use by Stable AIDL in other projects.
    project.artifacts.add(targetConfig, packagedDir) { artifact ->
        artifact.type = ARTIFACT_TYPE_STABLE_AIDL
        artifact.builtBy(taskProvider)
    }

    // Register packaged output for use by AGP's AIDL in other projects.
    project.configurations.findByName(targetConfig)?.outgoing?.variants { variants ->
        variants.allNamed(ARTIFACT_TYPE_AIDL) { variant ->
            variant.artifact(packagedDir) { artifact ->
                artifact.type = ARTIFACT_TYPE_AIDL
                artifact.builtBy(taskProvider)
            }
        }
    }
}

/**
 * Executes an action on the object with the given name, or any object with the given name that is
 * subsequently added.
 */
private fun DomainObjectCollection<ConfigurationVariant>.allNamed(
    name: String,
    action: Action<ConfigurationVariant>
) = all { variant ->
    if (variant.name == name) {
        action.execute(variant)
    }
}

fun registerPackageAidlApi(
    project: Project,
    variant: Variant,
    compileAidlApiTask: TaskProvider<StableAidlCompile>
): TaskProvider<StableAidlPackageApi> = project.tasks.register(
    computeTaskName("package", variant, "AidlApi"),
    StableAidlPackageApi::class.java
) { task ->
    task.packagedDir.set(compileAidlApiTask.flatMap { it.packagedDir })
}.also { taskProvider ->
    variant.artifacts.use(taskProvider)
        .wiredWithFiles(
            StableAidlPackageApi::aarFile,
            StableAidlPackageApi::updatedAarFile,
        )
        .toTransform(SingleArtifact.AAR)
}

fun registerGenerateAidlApi(
    project: Project,
    variant: Variant,
    aidlExecutable: Provider<RegularFile>,
    aidlFramework: Provider<RegularFile>,
    sourceDir: SourceDirectories.Flat,
    importsDir: SourceDirectories.Flat,
    depImports: List<FileCollection>,
    builtApiDir: Provider<Directory>,
    compileAidlApiTask: Provider<StableAidlCompile>
): TaskProvider<StableAidlCompile> = project.tasks.register(
    computeTaskName("generate", variant, "AidlApi"),
    StableAidlCompile::class.java
) { task ->
    task.group = TASK_GROUP_API
    task.description = "Generates API files from AIDL source code"
    task.variantName = variant.name
    task.aidlExecutable.set(aidlExecutable)
    task.aidlFrameworkProvider.set(aidlFramework)
    task.sourceDirs.set(sourceDir.all)
    task.sourceOutputDir.set(builtApiDir)
    task.importDirs.set(importsDir.all)
    depImports.forEach { task.dependencyImportDirs.addAll(it.elements) }
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
fun registerCheckApiAidlRelease(
    project: Project,
    variant: Variant,
    aidlExecutable: Provider<RegularFile>,
    aidlFramework: Provider<RegularFile>,
    importsDir: SourceDirectories.Flat,
    depImports: List<FileCollection>,
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
    task.aidlExecutable.set(aidlExecutable)
    task.aidlFrameworkProvider.set(aidlFramework)
    task.importDirs.set(importsDir.all)
    depImports.forEach { task.dependencyImportDirs.addAll(it.elements) }
    task.checkApiMode.set(StableAidlCheckApi.MODE_COMPATIBLE)
    task.expectedApiDir.set(lastReleasedApiDir)
    task.actualApiDir.set(generateAidlTask.flatMap { it.sourceOutputDir })
    task.failOnMissingExpected.set(false)
    task.cacheEvenIfNoOutputs()
}

// Policy: All changes to API surfaces for which compatibility is enforced must be
// explicitly confirmed by running the updateApi task. To enforce this, the implementation
fun registerCheckAidlApi(
    project: Project,
    variant: Variant,
    aidlExecutable: Provider<RegularFile>,
    aidlFramework: Provider<RegularFile>,
    importsDir: SourceDirectories.Flat,
    depImports: List<FileCollection>,
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
    task.aidlExecutable.set(aidlExecutable)
    task.aidlFrameworkProvider.set(aidlFramework)
    task.importDirs.set(importsDir.all)
    depImports.forEach { task.dependencyImportDirs.addAll(it.elements) }
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
