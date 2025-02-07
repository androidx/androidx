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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package androidx.build.binarycompatibilityvalidator

import androidx.build.AndroidXMultiplatformExtension
import androidx.build.Version
import androidx.build.addToBuildOnServer
import androidx.build.addToCheckTask
import androidx.build.checkapi.ApiType
import androidx.build.checkapi.getBcvFileDirectory
import androidx.build.checkapi.getBuiltBcvFileDirectory
import androidx.build.checkapi.getRequiredCompatibilityApiFileFromDir
import androidx.build.checkapi.shouldWriteVersionedApiFile
import androidx.build.getLibraryByName
import androidx.build.metalava.UpdateApiTask
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import androidx.build.version
import com.android.utils.appendCapitalized
import kotlinx.validation.KlibDumpMetadata
import kotlinx.validation.KotlinKlibAbiBuildTask
import kotlinx.validation.KotlinKlibExtractAbiTask
import kotlinx.validation.KotlinKlibMergeAbiTask
import kotlinx.validation.api.klib.KlibSignatureVersion
import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.konanTargetNameMapping
import kotlinx.validation.toKlibTarget
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

private const val GENERATE_NAME = "generateAbi"
private const val CHECK_NAME = "checkAbi"
private const val CHECK_RELEASE_NAME = "checkAbiRelease"
private const val UPDATE_NAME = "updateAbi"
private const val EXTRACT_NAME = "extractAbi"
private const val EXTRACT_RELEASE_NAME = "extractAbiRelease"
private const val IGNORE_CHANGES_NAME = "ignoreAbiChanges"

private const val KLIB_DUMPS_DIRECTORY = "klib"
private const val KLIB_MERGE_DIRECTORY = "merged"
private const val KLIB_EXTRACTED_DIRECTORY = "extracted"
private const val NATIVE_SUFFIX = "native"
internal const val CURRENT_API_FILE_NAME = "current.txt"
private const val IGNORE_FILE_NAME = "current.ignore"
private const val ABI_GROUP_NAME = "abi"

class BinaryCompatibilityValidation(
    val project: Project,
    private val kotlinMultiplatformExtension: KotlinMultiplatformExtension
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
            project.tasks.named("check").configure { it.dependsOn(checkAll) }
            project.addToCheckTask(checkAll)
            project.addToBuildOnServer(checkAll)
            if (HostManager.hostIsMac) {
                project.tasks.named("updateApi", UpdateApiTask::class.java) {
                    it.dependsOn(updateAll)
                }
            }
        }

    private fun configureKlibTasks(
        project: Project,
        checkAll: TaskProvider<Task>,
        updateAll: TaskProvider<Task>
    ) {
        if (kotlinMultiplatformExtension.nativeTargets().isEmpty()) {
            return
        }
        val runtimeClasspath: ConfigurableFileCollection =
            project.files(project.prepareKlibValidationClasspath())
        val projectVersion: Version = project.version()
        val projectAbiDir = project.getBcvFileDirectory().dir(NATIVE_SUFFIX)
        val currentIgnoreFile = projectAbiDir.file(IGNORE_FILE_NAME)
        val buildAbiDir = project.getBuiltBcvFileDirectory().map { it.dir(NATIVE_SUFFIX) }

        val klibDumpDir = project.layout.buildDirectory.dir(KLIB_DUMPS_DIRECTORY)
        val klibMergeFile =
            klibDumpDir.map { it.dir(KLIB_MERGE_DIRECTORY) }.map { it.file(CURRENT_API_FILE_NAME) }
        val klibExtractedFileDir = klibDumpDir.map { it.dir(KLIB_EXTRACTED_DIRECTORY) }

        val generateAbi = project.generateAbiTask(klibMergeFile, runtimeClasspath)
        val generatedAndMergedApiFile: Provider<RegularFileProperty> =
            generateAbi.map { it.mergedApiFile }
        val updateKlibAbi =
            project.updateKlibAbiTask(
                projectAbiDir,
                generatedAndMergedApiFile,
                projectVersion.toString(),
                runtimeClasspath
            )

        val extractKlibAbi =
            project.extractKlibAbiTask(projectAbiDir, klibExtractedFileDir, runtimeClasspath)
        val extractedProjectFile = extractKlibAbi.map { it.outputAbiFile }
        val checkKlibAbi = project.checkKlibAbiTask(extractedProjectFile, generatedAndMergedApiFile)
        val checkKlibAbiRelease =
            project.checkKlibAbiReleaseTask(
                generatedAndMergedApiFile,
                projectAbiDir,
                klibExtractedFileDir,
                currentIgnoreFile,
                runtimeClasspath
            )

        updateKlibAbi.configure { update ->
            checkKlibAbiRelease?.let { check -> update.dependsOn(check) }
        }
        updateAll.configure { it.dependsOn(updateKlibAbi) }
        checkAll.configure { checkTask ->
            checkTask.dependsOn(checkKlibAbi)
            checkKlibAbiRelease?.let { releaseCheck -> checkTask.dependsOn(releaseCheck) }
        }

        // add each target as an input to the merge task
        project.configureKlibTargets(generateAbi, buildAbiDir, runtimeClasspath)
    }

    /* Check that the current ABI definition is up to date. */
    private fun Project.checkKlibAbiTask(
        projectApiFile: Provider<RegularFileProperty>,
        generatedApiFile: Provider<RegularFileProperty>
    ) =
        project.tasks.register(
            CHECK_NAME.appendCapitalized(NATIVE_SUFFIX),
            CheckAbiEquivalenceTask::class.java
        ) {
            it.checkedInDump = projectApiFile
            it.builtDump = generatedApiFile
            it.group = ABI_GROUP_NAME
            it.cacheEvenIfNoOutputs()
        }

    /* Check that the current ABI definition is compatible with most recently released version */
    private fun Project.checkKlibAbiReleaseTask(
        mergedApiFile: Provider<RegularFileProperty>,
        klibApiDir: Directory,
        klibExtractDir: Provider<Directory>,
        ignoreFile: RegularFile,
        runtimeClasspath: ConfigurableFileCollection
    ) =
        project.getRequiredCompatibilityAbiLocation(NATIVE_SUFFIX)?.let { requiredCompatFile ->
            val extractReleaseTask =
                project.tasks.register(EXTRACT_RELEASE_NAME, KotlinKlibExtractAbiTask::class.java) {
                    it.strictValidation.set(HostManager.hostIsMac)
                    it.targetsToRemove.set(
                        project.provider {
                            unsupportedNativeTargetNames().map { targetName ->
                                KlibTarget(targetName)
                            }
                        }
                    )
                    it.inputAbiFile.set(klibApiDir.file(requiredCompatFile.name))
                    it.outputAbiFile.set(klibExtractDir.map { it.file(requiredCompatFile.name) })
                    it.runtimeClasspath.from(runtimeClasspath)
                    (it as DefaultTask).group = ABI_GROUP_NAME
                }
            project.tasks.register(IGNORE_CHANGES_NAME, IgnoreAbiChangesTask::class.java) {
                it.currentApiDump.set(mergedApiFile.map { fileProperty -> fileProperty.get() })
                it.previousApiDump.set(
                    extractReleaseTask.map { extract -> extract.outputAbiFile.get() }
                )
                it.ignoreFile.set(ignoreFile)
                it.runtimeClasspath.from(runtimeClasspath)
            }
            project.tasks.register(CHECK_RELEASE_NAME, CheckAbiIsCompatibleTask::class.java) {
                it.currentApiDump.set(mergedApiFile.map { fileProperty -> fileProperty.get() })
                it.previousApiDump.set(
                    extractReleaseTask.map { extract -> extract.outputAbiFile.get() }
                )
                it.projectVersion = provider { projectVersion.toString() }
                it.referenceVersion =
                    extractReleaseTask.map { extract ->
                        extract.outputAbiFile.get().asFile.nameWithoutExtension
                    }
                it.ignoreFile.set(ignoreFile)
                it.group = ABI_GROUP_NAME
                it.dependsOn(extractReleaseTask)
                it.runtimeClasspath.from(runtimeClasspath)
                it.cacheEvenIfNoOutputs()
            }
        }

    /* Updates the current abi file as well as the versioned abi file if appropriate */
    private fun Project.updateKlibAbiTask(
        klibApiDir: Directory,
        mergedKlibFile: Provider<RegularFileProperty>,
        projectVersion: String,
        runtimeClasspath: ConfigurableFileCollection
    ) =
        project.tasks.register(
            UPDATE_NAME.appendCapitalized(NATIVE_SUFFIX),
            UpdateAbiTask::class.java
        ) {
            it.outputDir.set(klibApiDir)
            it.inputApiLocation.set(mergedKlibFile.map { fileProperty -> fileProperty.get() })
            it.version.set(projectVersion)
            it.shouldWriteVersionedApiFile.set(project.shouldWriteVersionedApiFile())
            it.group = ABI_GROUP_NAME
            it.unsupportedNativeTargetNames.set(unsupportedNativeTargetNames())
            it.runtimeClasspath.from(runtimeClasspath)
        }

    /**
     * Extracts the targets that are supported on the current machine from the current file in the
     * project directory so they can be validated with checkAbi. For example on linux, extract all
     * current non-mac targets from the dump.
     */
    private fun Project.extractKlibAbiTask(
        klibApiDir: Directory,
        extractDir: Provider<Directory>,
        runtimeClasspath: ConfigurableFileCollection
    ) =
        project.tasks.register(EXTRACT_NAME, KotlinKlibExtractAbiTask::class.java) {
            it.strictValidation.set(HostManager.hostIsMac)
            it.targetsToRemove.set(
                project.provider {
                    unsupportedNativeTargetNames().map { targetName -> KlibTarget(targetName) }
                }
            )
            it.inputAbiFile.set(klibApiDir.file(CURRENT_API_FILE_NAME))
            it.outputAbiFile.set(extractDir.map { it.file(CURRENT_API_FILE_NAME) })
            it.runtimeClasspath.from(runtimeClasspath)
            (it as DefaultTask).group = ABI_GROUP_NAME
        }

    /* Merge target specific dumps into single file located in [mergeDir] */
    private fun Project.generateAbiTask(
        mergeFile: Provider<RegularFile>,
        runtimeClasspath: ConfigurableFileCollection
    ) =
        project.tasks.register(GENERATE_NAME, KotlinKlibMergeAbiTask::class.java) {
            it.mergedApiFile.set(mergeFile)
            it.runtimeClasspath.from(runtimeClasspath)
            (it as DefaultTask).group = ABI_GROUP_NAME
        }

    private fun Project.configureKlibTargets(
        mergeTask: TaskProvider<KotlinKlibMergeAbiTask>,
        abiBuildDir: Provider<Directory>,
        runtimeClasspath: ConfigurableFileCollection
    ) {
        val generatedDumps = objects.setProperty(KlibDumpMetadata::class.java)
        mergeTask.configure { it.dumps.addAll(generatedDumps) }
        kotlinMultiplatformExtension.nativeTargets().configureEach { currentTarget ->
            val mainCompilation =
                currentTarget.compilations.findByName(MAIN_COMPILATION_NAME) ?: return@configureEach

            val target = currentTarget.toKlibTarget()

            val isEnabled =
                currentTarget is KotlinNativeTarget &&
                    HostManager().isEnabled(currentTarget.konanTarget)
            if (isEnabled) {
                val buildTargetAbi =
                    configureKlibCompilation(
                        mainCompilation,
                        target,
                        abiBuildDir.map { it.dir(target.targetName) },
                        runtimeClasspath,
                    )
                generatedDumps.add(
                    KlibDumpMetadata(
                        target,
                        objects.fileProperty().also {
                            it.set(buildTargetAbi.flatMap { it.outputAbiFile })
                        }
                    )
                )
            }
        }
    }

    private fun supportedNativeTargetNames(): Set<String> {
        val hostManager = HostManager()
        return kotlinMultiplatformExtension
            .nativeTargets()
            .filter { hostManager.isEnabled(it.konanTarget) }
            .map { it.klibTargetName() }
            .toSet()
    }

    private fun allNativeTargetNames(): Set<String> =
        kotlinMultiplatformExtension.nativeTargets().map { it.klibTargetName() }.toSet()

    private fun unsupportedNativeTargetNames(): Set<String> =
        allNativeTargetNames() - supportedNativeTargetNames()

    private fun Project.configureKlibCompilation(
        compilation: KotlinCompilation<*>,
        target: KlibTarget,
        outputFileDir: Provider<Directory>,
        runtimeClasspath: ConfigurableFileCollection
    ): TaskProvider<KotlinKlibAbiBuildTask> {
        val buildTask =
            tasks.register(
                GENERATE_NAME.appendCapitalized(target.targetName),
                KotlinKlibAbiBuildTask::class.java
            ) {
                it.nonPublicMarkers.addAll(nonPublicMarkers)
                it.target.set(target)
                it.klibFile.from(compilation.output.classesDirs)
                it.signatureVersion.set(KlibSignatureVersion.LATEST)
                it.outputAbiFile.set(outputFileDir.map { it.file(CURRENT_API_FILE_NAME) })
                it.runtimeClasspath.from(runtimeClasspath)
                (it as DefaultTask).group = ABI_GROUP_NAME
            }
        return buildTask
    }
}

private fun Project.getRequiredCompatibilityAbiLocation(suffix: String) =
    getRequiredCompatibilityApiFileFromDir(
        project.getBcvFileDirectory().dir(suffix).asFile,
        project.version(),
        ApiType.CLASSAPI
    )

private fun KotlinMultiplatformExtension.nativeTargets() =
    targets.withType(KotlinNativeTarget::class.java).matching {
        it.platformType == KotlinPlatformType.native
    }

private fun KotlinNativeTarget.klibTargetName(): String =
    KlibTarget(targetName, konanTargetNameMapping[konanTarget.name]!!).toString()

private fun Project.prepareKlibValidationClasspath(): Configuration {
    return project.configurations.detachedConfiguration(
        project.dependencies.create(getLibraryByName("kotlinCompilerEmbeddable"))
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
        "androidx.privacysandbox.ui.core.ExperimentalFeatures.DelegatingAdapterApi",
        "androidx.room.ExperimentalRoomApi",
        "androidx.room.compiler.processing.ExperimentalProcessingApi",
        "androidx.tv.foundation.ExperimentalTvFoundationApi",
        "androidx.wear.compose.foundation.ExperimentalWearFoundationApi",
        "androidx.wear.compose.material.ExperimentalWearMaterialApi",
        "androidx.window.core.ExperimentalWindowApi",
    )

const val NEW_ISSUE_URL = "https://b.corp.google.com/issues/new?component=1102332"
