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

package androidx.build.clang

import androidx.build.getOperatingSystem
import java.io.ByteArrayOutputStream
import java.io.File
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.process.ExecOperations
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.LinkerArguments
import org.jetbrains.kotlin.konan.target.LinkerOutputKind
import org.jetbrains.kotlin.konan.target.Platform
import org.jetbrains.kotlin.konan.target.PlatformManager

/** The common clang builder for a [ClangBuildService]. */
internal interface ClangPlatformBuilder {
    fun compile(parameters: ClangCompileParameters)

    fun archiveLibrary(parameters: ClangArchiveParameters)

    fun runLinker(parameters: ClangLinkerParameters)
}

/** The implementation of the clang builder using the NDK. */
internal class AndroidPlatformBuilder(
    private val execOperations: ExecOperations,
    private val ndkDirectory: File,
    private val sdkLevel: Int,
) : ClangPlatformBuilder {

    val operatingSystem = getOperatingSystem()

    override fun compile(parameters: ClangCompileParameters) {
        val target = parameters.target.get().asNativeTarget
        val additionalArgs = buildList {
            addAll(parameters.freeArgs.get())
            add("-fPIC") // Always compile Android with 'Position Independent Code'
            add("-DNDEBUG") // Always compile with NDEBUG since only release is built
            add("--compile")
            parameters.includes.files.forEach { includeDirectory ->
                check(includeDirectory.isDirectory) {
                    "Include parameter for clang must be a directory: $includeDirectory"
                }
                add("-I${includeDirectory.canonicalPath}")
            }
            addAll(parameters.sources.regularFilePaths())
        }

        val compiler =
            getNdkClangExecutable(
                ndkDir = ndkDirectory,
                target = target,
                sdkLevel = sdkLevel,
                os = operatingSystem,
                isCxx = parameters.sources.files.any { it.extension in CXX_EXTENSIONS },
            )
        val clangCommand = listOf(compiler.canonicalPath) + additionalArgs

        execOperations.executeSilently { execSpec ->
            execSpec.executable = clangCommand.first()
            execSpec.args(clangCommand.drop(1))
            execSpec.workingDir = parameters.output.get().asFile
        }
    }

    override fun archiveLibrary(parameters: ClangArchiveParameters) {
        val outputFile = parameters.outputFile.get().asFile
        outputFile.delete()
        outputFile.parentFile.mkdirs()

        val llvmArgs = buildList {
            add("rc")
            add(parameters.outputFile.get().asFile.canonicalPath)
            addAll(parameters.objectFiles.regularFilePaths())
        }
        val llvmAr = getNdkLlvmArExecutable(ndkDirectory, operatingSystem)
        val commands = listOf(llvmAr.canonicalPath) + llvmArgs

        execOperations.executeSilently { execSpec ->
            execSpec.executable = commands.first()
            execSpec.args(commands.drop(1))
        }
    }

    override fun runLinker(parameters: ClangLinkerParameters) {
        val outputFile = parameters.outputFile.get().asFile
        outputFile.delete()
        outputFile.parentFile.mkdirs()

        val target = parameters.target.get().asNativeTarget
        val isCxx = parameters.sources.files.any { it.extension in CXX_EXTENSIONS }
        val linkerFlags = buildList {
            addAll(parameters.linkerArgs.get())
            // Statically include standard library when compiling c++
            if (isCxx) {
                add("-static-libstdc++")
            }
            // Specify max-page-size to align ELF regions to 16kb
            // https://developer.android.com/guide/practices/page-sizes
            addAll(listOf("-z", "max-page-size=16384", "-z", "common-page-size=16384"))
        }

        val objectFiles = parameters.objectFiles.regularFilePaths()
        val linkedObjectFiles = parameters.linkedObjects.regularFilePaths()

        val linker =
            getNdkClangExecutable(
                ndkDir = ndkDirectory,
                target = target,
                sdkLevel = sdkLevel,
                os = operatingSystem,
                isCxx = isCxx,
            )
        val args = buildList {
            if (parameters.linkerOutputKind.get() == LinkerOutputKind.DYNAMIC_LIBRARY) {
                add("-shared")
            }
            add("-o")
            add(outputFile.canonicalPath)
            addAll(objectFiles)
            addAll(linkedObjectFiles)
            addAll(linkerFlags)
        }
        execOperations.executeSilently { execSpec ->
            execSpec.executable = linker.canonicalPath
            execSpec.args(args)
        }
    }
}

/** The implementation of the clang builder using the Konan prebuilts. */
internal class KonanPlatformBuilder(
    private val execOperations: ExecOperations,
    private val platformManager: PlatformManager,
) : ClangPlatformBuilder {
    override fun compile(parameters: ClangCompileParameters) {
        val target = parameters.target.get().asNativeTarget
        val additionalArgs = buildList {
            addAll(parameters.freeArgs.get())
            add("-DNDEBUG") // Always compile with NDEBUG since only release is built
            add("--compile")
            parameters.includes.files.forEach { includeDirectory ->
                check(includeDirectory.isDirectory) {
                    "Include parameter for clang must be a directory: $includeDirectory"
                }
                add("-I${includeDirectory.canonicalPath}")
            }
            addAll(parameters.sources.regularFilePaths())
        }

        val platform = getPlatform(target)
        val clangCommand = platform.clang.clangC(*additionalArgs.toTypedArray())

        execOperations.executeSilently { execSpec ->
            execSpec.executable = clangCommand.first()
            execSpec.args(clangCommand.drop(1))
            execSpec.workingDir = parameters.output.get().asFile
        }
    }

    override fun archiveLibrary(parameters: ClangArchiveParameters) {
        val outputFile = parameters.outputFile.get().asFile
        outputFile.delete()
        outputFile.parentFile.mkdirs()

        val target = parameters.target.get().asNativeTarget
        val llvmArgs = buildList {
            add("rc")
            add(parameters.outputFile.get().asFile.canonicalPath)
            addAll(parameters.objectFiles.regularFilePaths())
        }
        val platform = getPlatform(target)
        val commands = platform.clang.llvmAr(*llvmArgs.toTypedArray())

        execOperations.executeSilently { execSpec ->
            execSpec.executable = commands.first()
            execSpec.args(commands.drop(1))
        }
    }

    override fun runLinker(parameters: ClangLinkerParameters) {
        val outputFile = parameters.outputFile.get().asFile
        outputFile.delete()
        outputFile.parentFile.mkdirs()

        val target = parameters.target.get().asNativeTarget
        val linkerFlags = parameters.linkerArgs.get()

        val objectFiles = parameters.objectFiles.regularFilePaths()
        val linkedObjectFiles = parameters.linkedObjects.regularFilePaths()

        val platform = getPlatform(target)
        val linkCommands =
            with(platform.linker) {
                LinkerArguments(
                        TempFiles(),
                        objectFiles = objectFiles,
                        executable = outputFile.canonicalPath,
                        dynamicLibraries = linkedObjectFiles,
                        staticLibraries = emptyList(),
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
                    args.drop(1).filter(getLinkerArgsFilter(target.toKonanTarget())).forEach {
                        execSpec.args(it)
                    }
                }
            }
    }

    private fun getPlatform(target: NativeTarget): Platform {
        val konanTarget = target.toKonanTarget()
        check(platformManager.enabled.contains(konanTarget)) {
            "cannot find enabled target with name $target"
        }
        val platform = platformManager.platform(konanTarget)
        platform.downloadDependencies()
        return platform
    }
}

private fun FileCollection.regularFilePaths(): List<String> {
    return files
        .flatMap { it.walkTopDown().filter { it.isFile }.map { it.canonicalPath } }
        .distinct()
}

/** Execute the command without logs unless it fails. */
private fun <T> ExecOperations.executeSilently(block: (ExecSpec) -> T) {
    val outputStream = ByteArrayOutputStream()
    val errorStream = ByteArrayOutputStream()
    var exe: String? = null
    var arguments: List<String>? = null
    val execResult = exec {
        block(it)
        exe = it.executable
        arguments = it.args
        it.errorOutput = errorStream
        it.standardOutput = outputStream
        it.isIgnoreExitValue = true // we'll check it below
    }
    if (execResult.exitValue != 0) {
        throw GradleException(
            """
            Compilation failed:
            Command: $exe ${arguments?.joinToString(" ")}
            ==== output:
            ${outputStream.toString(Charsets.UTF_8)}
            ==== error:
            ${errorStream.toString(Charsets.UTF_8)}
        """
                .trimIndent()
        )
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

private val CXX_EXTENSIONS = listOf("cpp", "cc", "cxx", "cp", "c++")
