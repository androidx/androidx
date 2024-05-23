/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import java.io.File
import java.io.PrintWriter
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Task that allows to write a version to a given output file. */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class VersionFileWriterTask : DefaultTask() {
    @get:Input abstract val version: Property<String>
    @get:Input abstract val relativePath: Property<String>
    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    /** The main method for actually writing out the file. */
    @TaskAction
    fun run() {
        val outputFile = File(outputDir.get().asFile, relativePath.get())
        outputFile.parentFile.mkdirs()
        val writer = PrintWriter(outputFile)
        writer.println(version.get())
        writer.close()
    }

    internal companion object {
        const val TASK_NAME = "writeVersionFile"
    }
}

/**
 * Sets up Android Library project to have a task that generates a version file.
 *
 * @receiver an Android Library project.
 */
fun Project.configureVersionFileWriter(
    libraryAndroidComponentsExtension: LibraryAndroidComponentsExtension,
    androidXExtension: AndroidXExtension
) {
    val writeVersionFile =
        tasks.register(VersionFileWriterTask.TASK_NAME, VersionFileWriterTask::class.java)

    afterEvaluate { configureVersionFile(writeVersionFile, androidXExtension) }
    libraryAndroidComponentsExtension.onVariants {
        it.sources.resources!!.addGeneratedSourceDirectory(
            writeVersionFile,
            VersionFileWriterTask::outputDir
        )
    }
}

fun Project.configureVersionFileWriter(
    kmpExtension: KotlinMultiplatformExtension,
    androidXExtension: AndroidXExtension
) {
    val writeVersionFile =
        tasks.register(VersionFileWriterTask.TASK_NAME, VersionFileWriterTask::class.java)
    writeVersionFile.configure {
        it.outputDir.set(layout.buildDirectory.dir("generatedVersionFile"))
    }
    val sourceSet = kmpExtension.sourceSets.getByName("androidMain")
    val resources = sourceSet.resources
    val includes = resources.includes
    resources.srcDir(writeVersionFile.map { it.outputDir })
    if (includes.isNotEmpty()) {
        includes.add("META-INF/*.version")
        resources.setIncludes(includes)
    }
    afterEvaluate { configureVersionFile(writeVersionFile, androidXExtension) }
}

private fun Project.configureVersionFile(
    writeVersionFile: TaskProvider<VersionFileWriterTask>,
    androidXExtension: AndroidXExtension
) {
    writeVersionFile.configure {
        val group = project.getGroup() as String
        val artifactId = project.getName() as String
        val version =
            if (androidXExtension.shouldPublish()) {
                version().toString()
            } else {
                "0.0.0"
            }

        it.version.set(version)
        it.relativePath.set(String.format("META-INF/%s_%s.version", group, artifactId))

        // We only add version file if is a library that is publishing.
        it.enabled = androidXExtension.shouldPublish()
    }
}
