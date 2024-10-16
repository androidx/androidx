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

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * Combines all given [objectFiles] into a directory with a well defined directory structure.
 *
 * The Android targets will be placed into a directory structure that matches the jniLibs structure
 * of Android Gradle Plugin, e.g.:
 * ```
 * <outputDir>
 *     armeabi-v7a/libfoo.so
 *     arm64-v8a/libfoo.so
 *     x86/libfoo.so
 *     x86_64/libfoo.so
 * ```
 *
 * Desktop targets will be placed on a structure that is based on the OS and architecture. e.g.:
 * ```
 * <outputDir>
 *     linux_arm64/libfoo.so
 *     linux_x64/libfoo.so
 *     osx_arm64/libfoo.dylib
 *     osx_x64/libfoo.dylib
 *     windows_x64/foo.dll
 * ```
 */
@DisableCachingByDefault(because = "not worth caching,just copies inputs into a another directory")
abstract class CombineObjectFilesTask : DefaultTask() {
    @get:Nested abstract val objectFiles: ListProperty<Provider<ObjectFile>>

    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun combineLibraries() {
        // TODO: (b/304281116) figure out how we'll have a single source of truth between the logic
        //  here and the runtime logic.
        val outputDir = outputDirectory.get().asFile
        outputDir.deleteRecursively()
        outputDir.mkdirs()
        val resolvedObjectFiles = objectFiles.get().map { it.get() }
        check(resolvedObjectFiles.isNotEmpty()) {
            "Running CombineSharedLibrariesTask without any inputs, this is likely an error"
        }
        resolvedObjectFiles.forEach { objectFile ->
            val konanTarget = objectFile.konanTarget.get().asKonanTarget
            val targetFile = targetFileFor(outputDir, konanTarget, objectFile)
            targetFile.parentFile?.mkdirs()
            objectFile.file.get().asFile.copyTo(target = targetFile, overwrite = true)
        }
    }

    companion object {
        private val familyDirectoryPrefixes =
            mapOf(
                Family.LINUX to "linux",
                Family.MINGW to "windows",
                Family.OSX to "osx",
            )

        private val architectureSuffixes =
            mapOf(
                Architecture.ARM32 to "arm32",
                Architecture.ARM64 to "arm64",
                Architecture.X64 to "x64",
                Architecture.X86 to "x86"
            )

        private fun targetFileFor(
            outputDir: File,
            konanTarget: KonanTarget,
            objectFile: ObjectFile
        ) = outputDir.resolve(directoryName(konanTarget)).resolve(objectFile.file.get().asFile.name)

        private fun directoryName(konanTarget: KonanTarget): String {
            if (konanTarget.family == Family.ANDROID) {
                // use android's own native library directory convention
                // https://developer.android.com/ndk/guides/abis#sa
                return when (konanTarget.architecture) {
                    Architecture.X86 -> "x86"
                    Architecture.X64 -> "x86_64"
                    Architecture.ARM32 -> "armeabi-v7a"
                    Architecture.ARM64 -> "arm64-v8a"
                    else -> error("add this architecture for android ${konanTarget.architecture}")
                }
            }
            val familyPrefix =
                familyDirectoryPrefixes[konanTarget.family]
                    ?: error("Unsupported family ${konanTarget.family} for $konanTarget")
            val architectureSuffix =
                architectureSuffixes[konanTarget.architecture]
                    ?: error(
                        "Unsupported architecture ${konanTarget.architecture} for $konanTarget"
                    )
            return "natives/${familyPrefix}_$architectureSuffix"
        }
    }
}

/**
 * Configures the [CombineObjectFilesTask] with the outputs of the [multiTargetNativeCompilation]
 * based on the given target [filter].
 */
fun TaskProvider<CombineObjectFilesTask>.configureFrom(
    multiTargetNativeCompilation: MultiTargetNativeCompilation,
    filter: (KonanTarget) -> Boolean
) {
    configure { task ->
        task.objectFiles.addAll(
            multiTargetNativeCompilation.targetsProvider(filter).map { nativeTargetCompilations ->
                nativeTargetCompilations.map { nativeTargetCompilation ->
                    nativeTargetCompilation.sharedLibTask.map { sharedLibraryTask ->
                        ObjectFile(
                            konanTarget = sharedLibraryTask.clangParameters.konanTarget,
                            file = sharedLibraryTask.clangParameters.outputFile
                        )
                    }
                }
            }
        )
    }
}

/** Represents an object file (.o, .so) associated with its [konanTarget]. */
class ObjectFile(
    @get:Input val konanTarget: Provider<SerializableKonanTarget>,
    @get:InputFile @get:PathSensitive(PathSensitivity.NAME_ONLY) val file: RegularFileProperty
)
