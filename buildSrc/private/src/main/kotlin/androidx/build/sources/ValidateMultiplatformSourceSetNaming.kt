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

package androidx.build.sources

import androidx.build.addToBuildOnServer
import androidx.build.addToCheckTask
import androidx.build.multiplatformExtension
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class ValidateMultiplatformSourceSetNaming : DefaultTask() {

    @get:Input abstract val rootDir: Property<String>

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    fun getInputFiles(): Collection<FileCollection> = sourceSetMap.values

    private val sourceSetMap: MutableMap<String, FileCollection> = mutableMapOf()

    @set:Option(
        option = "autoFix",
        description = "Whether to automatically rename files instead of throwing an exception",
    )
    @get:Input
    var autoFix: Boolean = false

    @TaskAction
    fun validate() {
        // Files or entire source sets may duplicated shared across compilations, but it's more
        // expensive to de-dupe them than to check the suffixes for everything multiple times.
        for ((sourceFileSuffix, kotlinSourceSet) in sourceSetMap) {
            for (fileOrDir in kotlinSourceSet) {
                for (file in fileOrDir.walk()) {
                    // Kotlin source files must be uniquely-named across platforms.
                    if (
                        file.isFile &&
                            file.name.endsWith(".kt") &&
                            !file.name.endsWith(".$sourceFileSuffix.kt")
                    ) {
                        val actualPath = file.toRelativeString(File(rootDir.get()))
                        val expectedName = "${file.name.substringBefore('.')}.$sourceFileSuffix.kt"
                        if (autoFix) {
                            val destFile = File(file.parentFile, expectedName)
                            file.renameTo(destFile)
                            logger.info("Applied fix: $actualPath -> $expectedName")
                        } else {
                            throw GradleException(
                                "Source files for non-common platforms must be suffixed with " +
                                    "their target platform. Found '$actualPath' but expected " +
                                    "'$expectedName'."
                            )
                        }
                    }
                }
            }
        }
    }

    fun addTarget(project: Project, target: KotlinTarget) {
        sourceSetMap[target.preferredSourceFileSuffix] =
            project.files(
                target.compilations
                    .filterNot { compilation ->
                        // Don't enforce suffixes for test source sets. Names can be e.g. testOnJvm
                        compilation.name.startsWith("test") || compilation.name.endsWith("Test")
                    }
                    .flatMap { compilation -> compilation.kotlinSourceSets }
                    .map { kotlinSourceSet -> kotlinSourceSet.kotlin.sourceDirectories }
                    .toTypedArray()
            )
    }

    /**
     * List of Kotlin target names which may be used as source file suffixes. Any target whose name
     * does not appear in this list will use its [KotlinPlatformType] name.
     */
    private val allowedTargetNameSuffixes =
        setOf("android", "desktop", "jvm", "commonStubs", "jvmStubs", "linuxx64Stubs", "wasmJs")

    /** The preferred source file suffix for the target's platform type. */
    private val KotlinTarget.preferredSourceFileSuffix: String
        get() =
            if (allowedTargetNameSuffixes.contains(name)) {
                name
            } else {
                platformType.name
            }
}

/**
 * Ensures that multiplatform sources are suffixed with their target platform, ex. `MyClass.jvm.kt`.
 *
 * Must be called in afterEvaluate().
 */
fun Project.registerValidateMultiplatformSourceSetNamingTask() {
    val targets = multiplatformExtension?.targets?.filterNot { target -> target.name == "metadata" }
    if (targets == null || targets.size <= 1) {
        // We only care about multiplatform projects with more than one target platform.
        return
    }

    tasks
        .register(
            "validateMultiplatformSourceSetNaming",
            ValidateMultiplatformSourceSetNaming::class.java
        ) { task ->
            targets
                .filterNot { target -> target.platformType.name == "common" }
                .forEach { target -> task.addTarget(project, target) }
            task.rootDir.set(rootDir.path)
            task.cacheEvenIfNoOutputs()
        }
        .also { validateTask ->
            // Multiplatform projects with no enabled platforms do not actually apply the Kotlin
            // plugin
            // and therefore do not have the check task. They are skipped unless a platform is
            // enabled.
            if (project.tasks.findByName("check") != null) {
                project.addToCheckTask(validateTask)
                project.addToBuildOnServer(validateTask)
            }
        }
}
