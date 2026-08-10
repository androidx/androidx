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

import androidx.build.androidExtension
import androidx.build.multiplatformAndroidExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.SourceDirectories
import com.android.build.api.variant.Sources
import com.android.utils.appendCapitalized
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.Family

/**
 * Helper class to bundle outputs of [MultiTargetNativeCompilation] with a JVM or Android project.
 */
class NativeLibraryBundler(private val project: Project) {
    /**
     * Adds the shared library outputs from [nativeCompilation] to the resources of the [jvmTarget].
     *
     * @see CombineObjectFilesTask for details.
     */
    fun addNativeLibrariesToResources(
        jvmTarget: KotlinJvmTarget,
        nativeCompilation: MultiTargetNativeCompilation,
        compilationName: String = KotlinCompilation.MAIN_COMPILATION_NAME,
    ) {
        val combineTask =
            project.tasks.register(
                "createCombinedResourceArchiveFor"
                    .appendCapitalized(
                        jvmTarget.name,
                        nativeCompilation.archiveName,
                        compilationName,
                    ),
                CombineObjectFilesTask::class.java,
            ) {
                it.outputDirectory.set(
                    project.layout.buildDirectory.dir(
                        "combinedNativeLibraries/${jvmTarget.name}/" +
                            "${nativeCompilation.archiveName}/$compilationName"
                    )
                )
            }
        val jniFamilies = listOf(Family.OSX, Family.MINGW, Family.LINUX)
        combineTask.configureFrom(nativeCompilation) { target -> target.family in jniFamilies }
        jvmTarget.compilations[compilationName]
            .defaultSourceSet
            .resources
            .srcDir(combineTask.map { it.outputDirectory })
    }

    /**
     * Adds the shared library outputs from [nativeCompilation] to a given variant src set of the
     * [androidTarget], expressed with the [provideSourceDirectories].
     *
     * @see CombineObjectFilesTask for details.
     */
    fun addNativeLibrariesToAndroidVariantSources(
        androidTarget: KotlinMultiplatformAndroidLibraryTarget,
        nativeCompilation: MultiTargetNativeCompilation,
        forTest: Boolean,
        provideSourceDirectories: Sources.() -> (SourceDirectories.Layered?),
    ) {
        addNativeLibrariesToAndroidSources(
            androidComponents = project.multiplatformAndroidExtension,
            nativeCompilation = nativeCompilation,
            forTest = forTest,
            provideSourceDirectories = provideSourceDirectories,
            additionalTaskSuffix = androidTarget.name,
        )
    }

    /**
     * Adds the shared library outputs from [nativeCompilation] to a given variant src set of the
     * project, expressed with the [provideSourceDirectories].
     */
    fun addNativeLibrariesToAndroidSources(
        nativeCompilation: MultiTargetNativeCompilation,
        forTest: Boolean,
        provideSourceDirectories: Sources.() -> (SourceDirectories.Layered?),
    ) {
        addNativeLibrariesToAndroidSources(
            androidComponents = project.androidExtension,
            nativeCompilation = nativeCompilation,
            forTest = forTest,
            provideSourceDirectories = provideSourceDirectories,
            additionalTaskSuffix = "",
        )
    }

    private fun addNativeLibrariesToAndroidSources(
        androidComponents: AndroidComponentsExtension<*, *, *>,
        nativeCompilation: MultiTargetNativeCompilation,
        forTest: Boolean,
        provideSourceDirectories: Sources.() -> (SourceDirectories.Layered?),
        additionalTaskSuffix: String,
    ) {
        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            fun setup(name: String, sources: SourceDirectories.Layered?) {
                checkNotNull(sources) {
                    "Cannot find jni libs sources for variant: $variant (forTest=$forTest)"
                }
                val taskSuffixes = buildList {
                    add(nativeCompilation.archiveName)
                    add("For")
                    add(name)
                    if (additionalTaskSuffix.isNotEmpty()) {
                        add(additionalTaskSuffix)
                    }
                }
                val combineTask =
                    project.tasks.register(
                        "createJniLibsDirectoryFor".appendCapitalized(*taskSuffixes.toTypedArray()),
                        CombineObjectFilesTask::class.java,
                    )
                combineTask.configureFrom(nativeCompilation) { it.isAndroid }

                sources.addGeneratedSourceDirectory(
                    taskProvider = combineTask,
                    wiredWith = CombineObjectFilesTask::outputDirectory,
                )
            }

            if (forTest) {
                check(variant is HasDeviceTests) { "Variant $variant does not have a test target" }
                variant.deviceTests.forEach { (_, deviceTest) ->
                    setup(deviceTest.name, provideSourceDirectories(deviceTest.sources))
                }
            } else {
                setup(variant.name, provideSourceDirectories(variant.sources))
            }
        }
    }
}
