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

package androidx.inspection.gradle

import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

/** Task for verifying "inspector.jar" is present in all inspector-supported libraries */
@CacheableTask
abstract class VerifyInspectorJarPresentTask : DefaultTask() {
    init {
        group = "Verification"
        description = "Verify inspector.jar is present in the inspector-supported library"
    }

    @get:[InputFile Classpath]
    abstract var inputAarFile: File

    @TaskAction
    fun verifyInspectorJarIsPresent() {
        check(inspectorJarPresent(inputAarFile)) { "$inputAarFile does not contain inspector.jar" }
    }
}

internal fun inspectorJarPresent(inputFile: File): Boolean {
    val inputStream = FileInputStream(inputFile)
    val aarFileInputStream = ZipInputStream(inputStream)
    var entry: ZipEntry? = aarFileInputStream.nextEntry
    while (entry != null) {
        if (entry.name == "inspector.jar") {
            return true
        }
        entry = aarFileInputStream.nextEntry
    }
    return false
}

private fun Project.getOutDirectory(): File {
    return extensions.extraProperties.get("outDir") as File
}

internal fun Project.createVerifyInspectorJarPresentTask(
    artifactId: String
): TaskProvider<VerifyInspectorJarPresentTask>? {
    val groupId = group.toString().replace('.', '/')
    val version = version
    val aarFileName = "$artifactId-$version.aar"
    val aarFile = file("${getOutDirectory()}/repository/$groupId/$artifactId/$version/$aarFileName")
    val taskProvider =
        tasks.register("verifyInspectorJarIsPresent", VerifyInspectorJarPresentTask::class.java) {
            it.dependsOn("publish")
            it.inputAarFile = aarFile
            it.cacheEvenIfNoOutputs()
        }
    tasks.named("buildOnServer").configure { it.dependsOn(taskProvider) }
    return taskProvider
}

// Tells Gradle to skip running this task, even if this task declares no output files
fun Task.cacheEvenIfNoOutputs() {
    this.outputs.file(this.getDummyOutput())
}

/**
 * Returns an unused output path that we can pass to Gradle to prevent Gradle from thinking that we
 * forgot to declare outputs of this task, and instead to skip this task if its inputs are unchanged
 */
private fun Task.getDummyOutput(): Provider<RegularFile> {
    return project.layout.buildDirectory.file("dummyOutput/" + this.name.replace(":", "-"))
}
