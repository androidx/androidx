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

package androidx.build.clang

import androidx.build.KonanPrebuiltsSetup
import androidx.build.ProjectLayoutType
import androidx.build.clang.KonanBuildService.Companion.obtain
import androidx.build.getKonanPrebuiltsFolder
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.utils.NativeCompilerDownloader
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.LinkerArguments
import org.jetbrains.kotlin.konan.target.Platform
import org.jetbrains.kotlin.konan.target.PlatformManager

/**
 * A Gradle BuildService that provides access to Konan Compiler (clang, linker, ar etc) to build
 * native sources for multiple targets.
 *
 * You can obtain the instance via [obtain].
 *
 * @see ClangArchiveTask
 * @see ClangCompileTask
 * @see ClangLinkerTask
 */
abstract class KonanBuildService @Inject constructor(private val execOperations: ExecOperations) :
    BuildService<KonanBuildService.Parameters> {
    private val dist by lazy {
        // double check that we don't initialize konan distribution without prebuilts in AOSP
        check(
            parameters.projectLayoutType.get() == ProjectLayoutType.PLAYGROUND ||
                parameters.prebuilts.isPresent
        ) {
            """
                Prebuilts directory for Konan must be provided when the project is not a playground
                project.
            """
                .trimIndent()
        }
        KonanPrebuiltsSetup.createKonanDistribution(
            prebuiltsDirectory = parameters.prebuilts.orNull?.asFile,
            konanHome = parameters.konanHome.get().asFile,
        )
    }

    private val platformManager by lazy { PlatformManager(distribution = dist) }

    /** @see ClangCompileTask */
    fun compile(parameters: ClangCompileParameters) {
        val outputDir = parameters.output.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()

        val platform = getPlatform(parameters.konanTarget)
        val additionalArgs = buildList {
            addAll(parameters.freeArgs.get())
            add("--compile")
            parameters.includes.files.forEach { includeDirectory ->
                check(includeDirectory.isDirectory) {
                    "Include parameter for clang must be a directory: $includeDirectory"
                }
                add("-I${includeDirectory.canonicalPath}")
            }
            addAll(parameters.sources.regularFilePaths())
        }

        val clangCommand = platform.clang.clangC(*additionalArgs.toTypedArray())
        execOperations.executeSilently { execSpec ->
            execSpec.executable = clangCommand.first()
            execSpec.args(clangCommand.drop(1))
            execSpec.workingDir = parameters.output.get().asFile
        }
    }

    /** @see ClangArchiveTask */
    fun archiveLibrary(parameters: ClangArchiveParameters) {
        val outputFile = parameters.outputFile.get().asFile
        outputFile.delete()
        outputFile.parentFile.mkdirs()

        val platform = getPlatform(parameters.konanTarget)
        val llvmArgs = buildList {
            add("rc")
            add(parameters.outputFile.get().asFile.canonicalPath)
            addAll(parameters.objectFiles.regularFilePaths())
        }
        val commands = platform.clang.llvmAr(*llvmArgs.toTypedArray())
        execOperations.executeSilently { execSpec ->
            execSpec.executable = commands.first()
            execSpec.args(commands.drop(1))
        }
    }

    /** @see ClangLinkerTask */
    fun runLinker(parameters: ClangLinkerParameters) {
        val outputFile = parameters.outputFile.get().asFile
        outputFile.delete()
        outputFile.parentFile.mkdirs()

        val platform = getPlatform(parameters.konanTarget)

        // Specify max-page-size to align ELF regions to 16kb and use LLVM linker
        // See https://youtrack.jetbrains.com/issue/KT-71728
        val linkerFlags =
            parameters.linkerArgs.get() +
                if (parameters.konanTarget.get().asKonanTarget.family == Family.ANDROID) {
                    listOf("-fuse-ld=lld", "-z", "max-page-size=16384")
                } else {
                    emptyList()
                }

        val objectFiles = parameters.objectFiles.regularFilePaths()
        val linkedObjectFiles = parameters.linkedObjects.regularFilePaths()
        val linkCommands =
            with(platform.linker) {
                LinkerArguments(
                        TempFiles(),
                        objectFiles = objectFiles,
                        executable = outputFile.canonicalPath,
                        libraries = linkedObjectFiles,
                        linkerArgs = linkerFlags,
                        optimize = true,
                        debug = false,
                        kind = parameters.linkerOutputKind.get(),
                        outputDsymBundle = "unused",
                        sanitizer = null,
                    )
                    .finalLinkCommands()
            }
        linkCommands
            .map { it.argsWithExecutable }
            .forEach { args ->
                execOperations.executeSilently { execSpec ->
                    execSpec.executable = args.first()
                    args
                        .drop(1)
                        .filter(getLinkerArgsFilter(parameters.konanTarget.get().asKonanTarget))
                        .forEach { execSpec.args(it) }
                }
            }
    }

    private fun getLinkerArgsFilter(target: KonanTarget): (String) -> Boolean = { flag ->
        // We use the linker that konan uses to be as similar as possible but that linker also has
        // extra things we might not want or need, In the future, we can consider not using the
        // `platform.linker` but then we would need to parse the konan.properties file to get the
        // relevant necessary parameters like sysroot, etc.
        // https://github.com/JetBrains/kotlin/blob/master/kotlin-native/konan/konan.properties
        when {
            // Remove konan demangling, which we don't need and is not available in the default
            // distribution.
            flag == "--defsym" || flag.contains("Konan_cxa_demangle") -> false
            // b/414635735 - Remove flag to explicitly link with the shared version of GCC runtime
            // library as that is not widely available in all Linux distribution and we prefer
            // linking to the static version (via -lgcc). Found in 'linkerGccFlags' in
            // the konan.properties.
            target.family == Family.LINUX && flag == "-lgcc_s" -> false
            else -> true
        }
    }

    private fun FileCollection.regularFilePaths(): List<String> {
        return files
            .flatMap { it.walkTopDown().filter { it.isFile }.map { it.canonicalPath } }
            .distinct()
    }

    private fun getPlatform(serializableKonanTarget: Property<SerializableKonanTarget>): Platform {
        val konanTarget = serializableKonanTarget.get().asKonanTarget
        check(platformManager.enabled.contains(konanTarget)) {
            "cannot find enabled target with name ${serializableKonanTarget.get()}"
        }
        val platform = platformManager.platform(konanTarget)
        platform.downloadDependencies()
        return platform
    }

    /** Execute the command without logs unless it fails. */
    private fun <T> ExecOperations.executeSilently(block: (ExecSpec) -> T) {
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        val execResult = exec {
            block(it)
            it.errorOutput = errorStream
            it.standardOutput = outputStream
            it.isIgnoreExitValue = true // we'll check it below
        }
        if (execResult.exitValue != 0) {
            throw GradleException(
                """
                Compilation failed:
                ==== output:
                ${outputStream.toString(Charsets.UTF_8)}
                ==== error:
                ${errorStream.toString(Charsets.UTF_8)}
            """
                    .trimIndent()
            )
        }
    }

    interface Parameters : BuildServiceParameters {
        /** KONAN_HOME parameter for initializing konan */
        val konanHome: DirectoryProperty

        /** Location if konan prebuilts. Can be null if this is a playground project */
        @get:Optional val prebuilts: DirectoryProperty

        /**
         * The type of the project (Playground vs AOSP main). This value is used to ensure we
         * initialize Konan distribution properly.
         */
        val projectLayoutType: Property<ProjectLayoutType>
    }

    companion object {
        internal const val KEY = "konanBuildService"

        fun obtain(project: Project): Provider<KonanBuildService> {
            return project.gradle.sharedServices.registerIfAbsent(
                KEY,
                KonanBuildService::class.java,
            ) {
                check(project.plugins.hasPlugin(KotlinMultiplatformPluginWrapper::class.java)) {
                    "KonanBuildService can only be used in projects that applied the KMP plugin"
                }
                check(KonanPrebuiltsSetup.isConfigured(project)) {
                    "Konan prebuilt directories are not configured for project \"${project.path}\""
                }
                val nativeCompilerDownloader = NativeCompilerDownloader(project)
                nativeCompilerDownloader.downloadIfNeeded()

                it.parameters.konanHome.set(nativeCompilerDownloader.compilerDirectory)
                it.parameters.projectLayoutType.set(ProjectLayoutType.from(project))
                if (!ProjectLayoutType.isPlayground(project)) {
                    it.parameters.prebuilts.set(project.getKonanPrebuiltsFolder())
                }
            }
        }
    }
}
