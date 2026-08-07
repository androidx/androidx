/*
 * Copyright 2026 The Android Open Source Project
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

import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.LinkerOutputKind

/**
 * [AndroidXNativeExtension] is an extension that provides Android native compilation via the
 * [ClangBuildService] and using the NDK.
 *
 * This is preferred over AGP's NDK integration because it is Gradle cache friendly.
 */
abstract class AndroidXNativeExtension @Inject constructor(val project: Project) {
    private val clang = AndroidXClang(project)
    private val nativeLibraryBundler = NativeLibraryBundler(project)

    @JvmOverloads
    fun createNativeCompilation(
        archiveName: String,
        outputKind: LinkerOutputKind = LinkerOutputKind.DYNAMIC_LIBRARY,
        configure: Action<MultiTargetNativeCompilation>,
    ): MultiTargetNativeCompilation {
        return clang.createNativeCompilation(
            archiveName = archiveName,
            outputKind = outputKind,
            configure = { compilation ->
                compilation.configureTargets(NATIVE_TARGETS)
                configure.execute(compilation)
            },
        )
    }

    @JvmOverloads
    fun addNativeLibrariesToJniLibs(
        nativeCompilation: MultiTargetNativeCompilation,
        forTest: Boolean = false,
    ) {
        nativeLibraryBundler.addNativeLibrariesToAndroidSources(
            nativeCompilation = nativeCompilation,
            forTest = forTest,
            provideSourceDirectories = { jniLibs },
        )
    }

    companion object {
        val NATIVE_TARGETS =
            listOf(
                NativeTarget.ANDROID_ARM32,
                NativeTarget.ANDROID_ARM64,
                NativeTarget.ANDROID_X86,
                NativeTarget.ANDROID_X64,
            )
    }
}
