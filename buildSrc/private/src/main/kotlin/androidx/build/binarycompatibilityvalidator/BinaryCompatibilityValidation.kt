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
import androidx.build.metalava.UpdateApiTask
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import androidx.build.version
import com.android.utils.appendCapitalized
import java.io.File
import kotlinx.validation.KotlinApiCompareTask
import kotlinx.validation.KotlinKlibAbiBuildTask
import kotlinx.validation.KotlinKlibExtractSupportedTargetsAbiTask
import kotlinx.validation.KotlinKlibMergeAbiTask
import kotlinx.validation.SerializableSignatureVersion
import kotlinx.validation.api.klib.KlibSignatureVersion
import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.konanTargetNameMapping
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager

private const val GENERATE_NAME = "generateAbi"
private const val CHECK_NAME = "checkAbi"
private const val CHECK_RELEASE_NAME = "checkAbiRelease"
private const val UPDATE_NAME = "updateAbi"
private const val EXTRACT_NAME = "extractAbi"
private const val EXTRACT_RELEASE_NAME = "extractAbiRelease"

private const val KLIB_DUMPS_DIRECTORY = "klib"
private const val KLIB_MERGE_DIRECTORY = "merged"
private const val KLIB_EXTRACTED_DIRECTORY = "extracted"
private const val NATIVE_SUFFIX = "native"
internal const val CURRENT_API_FILE_NAME = "current.txt"
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
        val projectVersion: Version = project.version()
        val projectAbiDir =
            project.provider { project.getBcvFileDirectory().resolve(NATIVE_SUFFIX) }
        val buildAbiDir =
            project.provider {
                project.getBuiltBcvFileDirectory().get().asFile.resolve(NATIVE_SUFFIX)
            }

        val projectBuildDir = project.layout.buildDirectory.asFile.get()
        val klibDumpDir = projectBuildDir.resolve(KLIB_DUMPS_DIRECTORY)
        val klibMergeDir = klibDumpDir.resolve(KLIB_MERGE_DIRECTORY)
        val klibMergeFile = klibMergeDir.resolve(CURRENT_API_FILE_NAME)
        val klibExtractedFileDir = klibDumpDir.resolve(KLIB_EXTRACTED_DIRECTORY)

        val generateAbi = project.generateAbiTask(klibMergeFile)
        val updateKlibAbi =
            project.updateKlibAbiTask(
                projectAbiDir,
                generateAbi.map { it.mergedFile },
                projectVersion.toString()
            )

        val extractKlibAbi = project.extractKlibAbiTask(projectAbiDir.get(), klibExtractedFileDir)
        val checkKlibAbi =
            project.checkKlibAbiTask(extractKlibAbi.map { it.outputAbiFile }.get(), klibMergeFile)
        // because extract/merge takes a [File] instead of a provider we set up the dependencies
        // manually
        checkKlibAbi.configure {
            it.dependsOn(extractKlibAbi)
            it.dependsOn(generateAbi)
        }
        val checkKlibAbiRelease = project.checkKlibAbiReleaseTask(generateAbi, klibExtractedFileDir)

        updateKlibAbi.configure { update ->
            checkKlibAbiRelease?.let { check -> update.dependsOn(check) }
        }
        updateAll.configure { it.dependsOn(updateKlibAbi) }
        checkAll.configure { checkTask ->
            checkTask.dependsOn(checkKlibAbi)
            checkKlibAbiRelease?.let { releaseCheck -> checkTask.dependsOn(releaseCheck) }
        }

        // configure the dump tasks for each individual target and set their output as inputs
        // to the merge task
        project.configureKlibTargets(generateAbi, buildAbiDir)
    }

    /* Check that the current ABI definition is up to date. */
    private fun Project.checkKlibAbiTask(projectApiFile: File, generatedApiFile: File) =
        project.tasks
            .register(
                CHECK_NAME.appendCapitalized(NATIVE_SUFFIX),
                KotlinApiCompareTask::class.java
            ) {
                it.projectApiFile = projectApiFile
                it.generatedApiFile = generatedApiFile
                it.group = ABI_GROUP_NAME
            }
            .also { task -> task.configure { it.cacheEvenIfNoOutputs() } }

    /* Check that the current ABI definition is compatible with most recently released version */
    private fun Project.checkKlibAbiReleaseTask(
        generateAbiTask: TaskProvider<KotlinKlibMergeAbiTask>,
        klibExtractDir: File
    ) =
        project.getRequiredCompatibilityAbiLocation(NATIVE_SUFFIX)?.let { requiredCompatFile ->
            val extractReleaseTask =
                project.tasks.register(
                    EXTRACT_RELEASE_NAME,
                    KotlinKlibExtractSupportedTargetsAbiTask::class.java
                ) {
                    it.strictValidation = true
                    it.supportedTargets = project.provider { supportedNativeTargetNames() }
                    it.inputAbiFile = requiredCompatFile
                    it.outputAbiFile = klibExtractDir.resolve(requiredCompatFile.name)
                    (it as DefaultTask).group = ABI_GROUP_NAME
                }
            project.tasks
                .register(CHECK_RELEASE_NAME, CheckAbiIsCompatibleTask::class.java) {
                    it.currentApiDump = generateAbiTask.map { merge -> merge.mergedFile }
                    it.previousApiDump = extractReleaseTask.map { extract -> extract.outputAbiFile }
                    it.projectVersion = provider { projectVersion.toString() }
                    it.referenceVersion =
                        extractReleaseTask.map { extract ->
                            extract.outputAbiFile.nameWithoutExtension
                        }
                    it.group = ABI_GROUP_NAME
                }
                .also { checkRelease ->
                    checkRelease.configure { it.dependsOn(extractReleaseTask) }
                }
        }

    /* Updates the current abi file as well as the versioned abi file if appropriate */
    private fun Project.updateKlibAbiTask(
        klibApiDir: Provider<File>,
        mergedKlibFile: Provider<File>,
        projectVersion: String
    ) =
        project.tasks.register(
            UPDATE_NAME.appendCapitalized(NATIVE_SUFFIX),
            UpdateAbiTask::class.java
        ) {
            it.outputDir = klibApiDir
            it.inputApiLocation = mergedKlibFile
            it.version.set(projectVersion)
            it.shouldWriteVersionedApiFile.set(project.shouldWriteVersionedApiFile())
            it.group = ABI_GROUP_NAME
            it.unsupportedNativeTargetNames.set(unsupportedNativeTargetNames())
        }

    /**
     * Extracts the targets that are supported on the current machine from the current file in the
     * project directory so they can be validated with checkAbi. For example on linux, extract all
     * current non-mac targets from the dump.
     */
    private fun Project.extractKlibAbiTask(klibApiDir: File, extractDir: File) =
        project.tasks.register(EXTRACT_NAME, KotlinKlibExtractSupportedTargetsAbiTask::class.java) {
            it.strictValidation = HostManager.hostIsMac
            it.supportedTargets = project.provider { supportedNativeTargetNames() }
            it.inputAbiFile = klibApiDir.resolve(CURRENT_API_FILE_NAME)
            it.outputAbiFile = extractDir.resolve(CURRENT_API_FILE_NAME)
            (it as DefaultTask).group = ABI_GROUP_NAME
        }

    /* Merge target specific dumps into single file located in [mergeDir] */
    private fun Project.generateAbiTask(mergeFile: File) =
        project.tasks.register(GENERATE_NAME, KotlinKlibMergeAbiTask::class.java) {
            it.dumpFileName = CURRENT_API_FILE_NAME
            it.mergedFile = mergeFile
            (it as DefaultTask).group = ABI_GROUP_NAME
        }

    private fun Project.configureKlibTargets(
        mergeTask: TaskProvider<KotlinKlibMergeAbiTask>,
        abiBuildDir: Provider<File>
    ) {
        kotlinMultiplatformExtension.nativeTargets().configureEach { currentTarget ->
            val mainCompilations =
                currentTarget.compilations.matching {
                    it.name == KotlinCompilation.MAIN_COMPILATION_NAME
                }

            val targetName = currentTarget.targetName
            val isEnabled =
                currentTarget is KotlinNativeTarget &&
                    HostManager().isEnabled(currentTarget.konanTarget)
            if (isEnabled) {
                mainCompilations.configureEach {
                    val abiBuildLocation = abiBuildDir.get().resolve(targetName)
                    val buildTargetAbi: TaskProvider<*> =
                        configureKlibCompilation(it, targetName, abiBuildLocation)
                    mergeTask.configure { task ->
                        task.addInput(targetName, abiBuildLocation)
                        task.dependsOn(buildTargetAbi)
                    }
                }
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
        compilation: KotlinCompilation<KotlinCommonOptions>,
        targetName: String,
        outputFileDir: File
    ): TaskProvider<KotlinKlibAbiBuildTask> {
        val buildTask =
            tasks.register(
                GENERATE_NAME.appendCapitalized(targetName),
                KotlinKlibAbiBuildTask::class.java
            ) {
                it.target = targetName
                it.klibFile = project.files(provider { compilation.output.classesDirs })
                it.compilationDependencies = files(provider { compilation.compileDependencyFiles })
                it.signatureVersion = SerializableSignatureVersion(KlibSignatureVersion.LATEST)
                it.outputApiFile = outputFileDir.resolve(CURRENT_API_FILE_NAME)
                (it as DefaultTask).group = ABI_GROUP_NAME
            }
        return buildTask
    }
}

private fun Project.getRequiredCompatibilityAbiLocation(suffix: String) =
    getRequiredCompatibilityApiFileFromDir(
        project.getBcvFileDirectory().resolve(suffix),
        project.version(),
        ApiType.CLASSAPI
    )

private fun KotlinMultiplatformExtension.nativeTargets() =
    targets.withType(KotlinNativeTarget::class.java).matching {
        it.platformType == KotlinPlatformType.native
    }

private fun KotlinNativeTarget.klibTargetName(): String =
    KlibTarget(targetName, konanTargetNameMapping[konanTarget.name]!!).toString()
