/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build.binarycompatibilityvalidator

import androidx.build.AndroidXMultiplatformExtension
import androidx.build.Version
import androidx.build.addToBuildOnServer
import androidx.build.addToCheckTask
import androidx.build.checkapi.ApiType
import androidx.build.checkapi.getBcvFileDirectory
import androidx.build.checkapi.getRequiredCompatibilityApiFileFromDir
import androidx.build.checkapi.shouldWriteVersionedApiFile
import androidx.build.getDistributionDirectory
import androidx.build.getLibraryClasspath
import androidx.build.getSupportRootFolder
import androidx.build.isWriteVersionedApiFilesEnabled
import androidx.build.metalava.UpdateApiTask
import androidx.build.multiplatformExtension
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import androidx.build.version
import com.android.utils.appendCapitalized
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.abi.tools.KlibTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

private const val GENERATE_NAME = "generateAbi"
private const val CHECK_NAME = "checkAbi"
private const val CHECK_RELEASE_NAME = "checkAbiRelease"
private const val UPDATE_NAME = "updateAbi"
private const val IGNORE_CHANGES_NAME = "ignoreAbiChanges"

private const val KLIB_DUMPS_DIRECTORY = "klib"
private const val NATIVE_SUFFIX = "native"
internal const val CURRENT_API_FILE_NAME = "current.txt"
private const val IGNORE_FILE_NAME = "current.ignore"
private const val ABI_GROUP_NAME = "abi"
private const val CROSS_COMPILATION_FLAG = "kotlin.native.enableKlibsCrossCompilation"

class BinaryCompatibilityValidation(
    val project: Project,
    private val kotlinMultiplatformExtension: KotlinMultiplatformExtension,
) {
    private val projectVersion: Version = project.version()

    fun setupBinaryCompatibilityValidatorTasks() =
        project.afterEvaluate {
            val androidXMultiplatformExtension =
                project.extensions.getByType(AndroidXMultiplatformExtension::class.java)
            if (!androidXMultiplatformExtension.enableBinaryCompatibilityValidator) {
                return@afterEvaluate
            }
            val checkAll: TaskProvider<Task> = project.tasks.register(CHECK_NAME)
            val updateAll: TaskProvider<Task> = project.tasks.register(UPDATE_NAME)
            configureKlibTasks(project, checkAll, updateAll)
            if (project.multiplatformExtension?.hasUnsupportedTargets() == false) {
                project.addToCheckTask(checkAll)
                project.addToBuildOnServer(checkAll)
                project.tasks.named("updateApi", UpdateApiTask::class.java) {
                    it.dependsOn(updateAll)
                }
            }
        }

    private fun configureKlibTasks(
        project: Project,
        checkAll: TaskProvider<Task>,
        updateAll: TaskProvider<Task>,
    ) {
        if (kotlinMultiplatformExtension.nativeTargets().isEmpty()) {
            return
        }
        val runtimeClasspath: FileCollection =
            project.getLibraryClasspath("kotlinCompilerEmbeddable")
        val abiToolsClasspath: FileCollection = project.getLibraryClasspath("kotlinAbiTools")
        val projectAbiDir = project.getBcvFileDirectory().dir(NATIVE_SUFFIX)
        val currentIgnoreFile = projectAbiDir.file(IGNORE_FILE_NAME)

        val klibDumpDir = project.layout.buildDirectory.dir(KLIB_DUMPS_DIRECTORY)
        val klibDumpFile = klibDumpDir.map { it.file(CURRENT_API_FILE_NAME) }

        val generateAbi =
            project.generateAbiTask(
                klibDumpFile,
                abiToolsClasspath,
                kotlinMultiplatformExtension.hasUnsupportedTargets(),
                kotlinMultiplatformExtension.hasCInterop(),
                project.providers.gradleProperty(CROSS_COMPILATION_FLAG).get() == "true",
            )
        val generatedAndMergedApiFile: Provider<RegularFileProperty> =
            generateAbi.map { it.abiFile }
        val updateKlibAbi =
            project.updateKlibAbiTask(projectAbiDir, generatedAndMergedApiFile, runtimeClasspath)

        val checkKlibAbi =
            project.checkKlibAbiTask(
                projectAbiDir.file(CURRENT_API_FILE_NAME),
                generatedAndMergedApiFile,
                projectAbiDir,
            )
        val checkKlibAbiRelease =
            project.checkKlibAbiReleaseTask(
                generatedAndMergedApiFile,
                projectAbiDir,
                currentIgnoreFile,
                runtimeClasspath,
            )

        updateKlibAbi.configure { update ->
            checkKlibAbiRelease?.let { check -> update.dependsOn(check) }
        }
        updateAll.configure { it.dependsOn(updateKlibAbi) }
        checkAll.configure { checkTask ->
            checkTask.dependsOn(checkKlibAbi)
            checkKlibAbiRelease?.let { releaseCheck -> checkTask.dependsOn(releaseCheck) }
        }
    }

    /* Check that the current ABI definition is up to date. */
    private fun Project.checkKlibAbiTask(
        projectApiFile: RegularFile,
        generatedApiFile: Provider<RegularFileProperty>,
        projectAbiDir: Directory,
    ) =
        project.tasks.register(
            CHECK_NAME.appendCapitalized(NATIVE_SUFFIX),
            CheckAbiEquivalenceTask::class.java,
        ) {
            it.checkedInDump = projectApiFile
            it.builtDump = generatedApiFile
            it.projectAbiDir.set(projectAbiDir)
            val projectDirPath =
                project.projectDir.path.removePrefix(project.getSupportRootFolder().path + "/")

            it.debugOutFile.set(
                project.getDistributionDirectory().map { outDir ->
                    // e.g. out/bcv/foo/bar/bar
                    outDir.dir("bcv").dir(projectDirPath).file("actual_current.txt")
                }
            )
            it.group = ABI_GROUP_NAME
            it.cacheEvenIfNoOutputs()
            it.shouldWriteVersionedAbiFile.set(project.shouldWriteVersionedApiFile())
            it.version.set(projectVersion.toString())
        }

    /* Check that the current ABI definition is compatible with most recently released version */
    private fun Project.checkKlibAbiReleaseTask(
        mergedApiFile: Provider<RegularFileProperty>,
        klibApiDir: Directory,
        ignoreFile: RegularFile,
        runtimeClasspath: FileCollection,
    ) =
        project.getRequiredCompatibilityAbiLocation(NATIVE_SUFFIX)?.let { requiredCompatFile ->
            val previousApiDump = klibApiDir.file(requiredCompatFile.name)
            val referenceVersionProvider = provider { requiredCompatFile.nameWithoutExtension }
            project.tasks.register(IGNORE_CHANGES_NAME, IgnoreAbiChangesTask::class.java) {
                it.currentApiDump.set(mergedApiFile.map { fileProperty -> fileProperty.get() })
                it.previousApiDump.set(previousApiDump)
                it.dependencies.set(
                    kotlinMultiplatformExtension.nativeTargets().map { target ->
                        DependenciesForTarget(
                            KlibTarget.fromKonanTargetName(target.konanTarget.name).targetName,
                            target.compileDependencyFiles(),
                        )
                    }
                )
                it.ignoreFile.set(ignoreFile)
                it.runtimeClasspath.from(runtimeClasspath)
                it.projectVersion = provider { projectVersion.toString() }
                it.referenceVersion = referenceVersionProvider
            }
            project.tasks.register(CHECK_RELEASE_NAME, CheckAbiIsCompatibleTask::class.java) {
                it.dependencies.set(
                    kotlinMultiplatformExtension.nativeTargets().map { target ->
                        DependenciesForTarget(
                            KlibTarget.fromKonanTargetName(target.konanTarget.name).targetName,
                            target.compileDependencyFiles(),
                        )
                    }
                )
                it.currentApiDump.set(mergedApiFile.map { fileProperty -> fileProperty.get() })
                it.previousApiDump.set(previousApiDump)
                it.projectVersion = provider { projectVersion.toString() }
                it.referenceVersion = referenceVersionProvider
                it.ignoreFile.set(ignoreFile)
                it.group = ABI_GROUP_NAME
                it.runtimeClasspath.from(runtimeClasspath)
                it.cacheEvenIfNoOutputs()
            }
        }

    /* Updates the current abi file as well as the versioned abi file if appropriate */
    private fun Project.updateKlibAbiTask(
        klibApiDir: Directory,
        mergedKlibFile: Provider<RegularFileProperty>,
        runtimeClasspath: FileCollection,
    ) =
        project.tasks.register(
            UPDATE_NAME.appendCapitalized(NATIVE_SUFFIX),
            UpdateAbiTask::class.java,
        ) {
            it.outputDir.set(klibApiDir)
            it.inputApiLocation.set(mergedKlibFile.map { fileProperty -> fileProperty.get() })
            it.version.set(projectVersion.toString())
            it.shouldWriteVersionedApiFile.set(project.shouldWriteVersionedApiFile())
            it.group = ABI_GROUP_NAME
            it.runtimeClasspath.from(runtimeClasspath)
        }

    /* Generate ABI dump files in build directory */
    private fun Project.generateAbiTask(
        mergeFile: Provider<RegularFile>,
        runtimeClasspath: FileCollection,
        hasUnsupportedTargets: Boolean,
        hasCInterop: Boolean,
        crossCompilationEnabled: Boolean,
    ) =
        project.tasks.register(GENERATE_NAME, GenerateAbiTask::class.java) {
            // This only affects the external process launched by this task,
            // NOT the core Kotlin compilation tasks in the same build.
            it.runtimeClasspath.from(runtimeClasspath)
            it.abiFile.set(mergeFile)
            it.excludedAnnotatedWith.addAll(nonPublicMarkers)
            it.klibs.set(
                kotlinMultiplatformExtension.nativeTargets().map { target ->
                    val klibTarget =
                        KlibTarget.fromKonanTargetName(target.konanTarget.name)
                            .configureName(target.targetName)
                    objects.newInstance(KlibTargetInfo::class.java).apply {
                        targetName = klibTarget.configurableName
                        canonicalTargetName = klibTarget.targetName
                        klibFiles =
                            target.compilations.getByName(MAIN_COMPILATION_NAME).output.classesDirs
                    }
                }
            )
            it.group = ABI_GROUP_NAME
            it.doFirst {
                runHostCompatibilityChecks(
                    hasUnsupportedTargets,
                    hasCInterop,
                    crossCompilationEnabled,
                )
            }
        }
}

private fun Project.getRequiredCompatibilityAbiLocation(suffix: String) =
    getRequiredCompatibilityApiFileFromDir(
        project.getBcvFileDirectory().dir(suffix).asFile,
        project.version(),
        ApiType.CLASSAPI,
        enforceVersionContinuity = isWriteVersionedApiFilesEnabled(),
    )

private fun KotlinMultiplatformExtension.nativeTargets() =
    targets.withType(KotlinNativeTarget::class.java).matching {
        it.platformType == KotlinPlatformType.native
    }

private fun KotlinMultiplatformExtension.hasCInterop(): Boolean {
    val mainCompilations = nativeTargets().map { it.compilations.getByName(MAIN_COMPILATION_NAME) }
    return mainCompilations.any { it.cinterops.isNotEmpty() }
}

private fun KotlinMultiplatformExtension.hasUnsupportedTargets(): Boolean {
    val hostManager = HostManager()
    return nativeTargets().any { !hostManager.isEnabled(it.konanTarget) }
}

private fun runHostCompatibilityChecks(
    hasUnsupportedTargets: Boolean,
    hasCInterop: Boolean,
    crossCompilationEnabled: Boolean,
) {
    if (!hasUnsupportedTargets) {
        // running on mac, or project has no mac targets. No further checks necessary
        return
    }
    if (hasCInterop) {
        // It's impossible to run these tasks on the current host, because they require cinterop
        // so cross compilation is not an option
        throw GradleException(
            """
            Project uses cinterop and cannot be compiled on the current host (${HostManager.host}).

            ABI checks and updates need to compile all targets to run. Please run these tasks on a Mac machine which can build all targets.
        """
        )
    }
    // Unsupported targets exist, but they can be built by enabling cross compilation just for the
    // ABI tasks
    if (!crossCompilationEnabled)
        throw GradleException(
            """
        Project requires cross compilation to be compiled on the current host (${HostManager.host}).

        Please re-run the tasks with cross compilation enabled using the flag '-Pkotlin.native.enableKlibsCrossCompilation=true'
    """
        )
}

// Not ideal to have a list instead of a pattern to match but this is all the API supports right now
// https://github.com/Kotlin/binary-compatibility-validator/issues/280
private val nonPublicMarkers =
    setOf(
        "androidx.annotation.Experimental",
        "androidx.benchmark.BenchmarkState.Companion.ExperimentalExternalReport",
        "androidx.benchmark.ExperimentalBenchmarkConfigApi",
        "androidx.benchmark.ExperimentalBenchmarkStateApi",
        "androidx.benchmark.ExperimentalBlackHoleApi",
        "androidx.benchmark.macro.ExperimentalMacrobenchmarkApi",
        "androidx.benchmark.macro.ExperimentalMetricApi",
        "androidx.benchmark.perfetto.ExperimentalPerfettoCaptureApi",
        "androidx.benchmark.perfetto.ExperimentalPerfettoTraceProcessorApi",
        "androidx.camera.core.ExperimentalUseCaseApi",
        "androidx.car.app.annotations.ExperimentalCarApi",
        "androidx.compose.animation.ExperimentalAnimationApi",
        "androidx.compose.animation.ExperimentalSharedTransitionApi",
        "androidx.compose.animation.core.ExperimentalAnimatableApi",
        "androidx.compose.animation.core.ExperimentalAnimationSpecApi",
        "androidx.compose.animation.core.ExperimentalTransitionApi",
        "androidx.compose.animation.core.InternalAnimationApi",
        "androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
        "androidx.compose.foundation.gestures.ExperimentalTapGestureDetectorBehaviorApi",
        "androidx.compose.foundation.ExperimentalFoundationApi",
        "androidx.compose.foundation.InternalFoundationApi",
        "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        "androidx.compose.material.ExperimentalMaterialApi",
        "androidx.compose.runtime.ExperimentalComposeApi",
        "androidx.compose.runtime.ExperimentalComposeRuntimeApi",
        "androidx.compose.runtime.InternalComposeApi",
        "androidx.compose.runtime.InternalComposeTracingApi",
        "androidx.compose.ui.ExperimentalComposeUiApi",
        "androidx.compose.ui.ExperimentalIndirectTouchTypeApi",
        "androidx.compose.ui.InternalComposeUiApi",
        "androidx.compose.ui.input.pointer.util.ExperimentalVelocityTrackerApi",
        "androidx.compose.ui.node.InternalCoreApi",
        "androidx.compose.ui.test.ExperimentalTestApi",
        "androidx.compose.ui.test.InternalTestApi",
        "androidx.compose.ui.text.ExperimentalTextApi",
        "androidx.compose.ui.text.InternalTextApi",
        "androidx.compose.ui.unit.ExperimentalUnitApi",
        "androidx.constraintlayout.compose.ExperimentalMotionApi",
        "androidx.core.telecom.util.ExperimentalAppActions",
        "androidx.credentials.ExperimentalDigitalCredentialApi",
        "androidx.glance.ExperimentalGlanceApi",
        "androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi",
        "androidx.health.connect.client.ExperimentalDeduplicationApi",
        "androidx.health.connect.client.feature.ExperimentalFeatureAvailabilityApi",
        "androidx.ink.authoring.ExperimentalLatencyDataApi",
        "androidx.ink.brush.ExperimentalInkCustomBrushApi",
        "androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi",
        "androidx.paging.ExperimentalPagingApi",
        "androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.RegisterSourceOptIn",
        "androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.Ext8OptIn",
        "androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.Ext10OptIn",
        "androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.Ext11OptIn",
        "androidx.privacysandbox.ads.adservices.common.ExperimentalFeatures.Ext12OptIn",
        "androidx.room3.ExperimentalRoomApi",
        "androidx.room3.compiler.processing.ExperimentalProcessingApi",
        "androidx.tv.foundation.ExperimentalTvFoundationApi",
        "androidx.wear.compose.foundation.ExperimentalWearFoundationApi",
        "androidx.wear.compose.material.ExperimentalWearMaterialApi",
        "androidx.window.core.ExperimentalWindowApi",
        "androidx.compose.material3.ExperimentalMaterial3Api",
    )

const val NEW_ISSUE_URL = "https://b.corp.google.com/issues/new?component=1102332"

private fun KotlinNativeTarget.compileDependencyFiles(): FileCollection =
    compilations.getByName(MAIN_COMPILATION_NAME).compileDependencyFiles.filter {
        // stdlib is a klib directory so no extension
        it.extension == "" || it.extension == "klib"
    }
