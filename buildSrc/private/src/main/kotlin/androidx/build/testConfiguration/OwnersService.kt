/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.build.testConfiguration

import androidx.build.getDistributionDirectory
import androidx.build.getSupportRootFolder
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip

@CacheableTask
abstract class ModuleInfoGenerator : DefaultTask() {
    @get:OutputFile abstract val outputFile: RegularFileProperty

    @get:Internal val testModules: MutableList<TestModule> = mutableListOf()

    @Input
    fun getSerialized(): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val data = testModules.associateBy { it.name }
        return gson.toJson(data)
    }

    @TaskAction
    fun writeModuleInfo() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(getSerialized())
    }
}

/**
 * Register two tasks needed to generate information for Android test owners service. One task zips
 * all the OWNERS files in frameworks/support, and second task creates a module-info.json that links
 * test modules to paths.
 */
internal fun Project.registerOwnersServiceTasks() {
    val ownersDirectory = layout.buildDirectory.dir("owners")
    val copyOwners =
        tasks.register("copyOwnersFiles", Sync::class.java) { task ->
            task.into(ownersDirectory)
            task.from(layout.projectDirectory)
            task.include("**/OWNERS")
            task.exclude("buildSrc/.gradle/**")
            task.exclude(".gradle/**")
            task.exclude("build/reports/**")
            task.exclude("kotlin-js-store/**")
            task.includeEmptyDirs = false
        }
    val lintOwners =
        tasks.register("lintOwnersFiles", LintOwnersFiles::class.java) { task ->
            task.ownersDirectory.set(ownersDirectory)
            task.dependsOn(copyOwners)
            task.cacheEvenIfNoOutputs()
        }
    tasks.register("zipOwnersFiles", Zip::class.java) { task ->
        task.archiveFileName.set("owners.zip")
        task.destinationDirectory.set(getDistributionDirectory())
        task.from(ownersDirectory)
        task.dependsOn(copyOwners, lintOwners)
    }

    tasks.register(CREATE_MODULE_INFO, ModuleInfoGenerator::class.java) {
        it.outputFile.set(getDistributionDirectory().file("module-info.json"))
    }
}

internal fun Project.addToModuleInfo(testName: String, projectIsolationEnabled: Boolean) {
    if (!projectIsolationEnabled) {
        rootProject.tasks.named(CREATE_MODULE_INFO).configure {
            it as ModuleInfoGenerator
            it.testModules.add(
                TestModule(
                    name = testName,
                    path = listOf(projectDir.toRelativeString(getSupportRootFolder())),
                )
            )
        }
    }
}

data class TestModule(val name: String, val path: List<String>)

private const val CREATE_MODULE_INFO = "createModuleInfo"

@CacheableTask
abstract class LintOwnersFiles : DefaultTask() {
    @get:[InputFiles PathSensitive(PathSensitivity.NONE)]
    abstract val ownersDirectory: DirectoryProperty

    @TaskAction
    fun validate() {
        ownersDirectory.asFileTree.forEach {
            val relativePath = it.relativeTo(ownersDirectory.get().asFile).toString()
            if (relativePath.startsWith("buildSrc/")) return@forEach
            if (relativePath.startsWith("busytown/")) return@forEach
            if (relativePath.startsWith("development/")) return@forEach
            if (relativePath.startsWith("samples/")) return@forEach
            if (relativePath.startsWith(".agents/")) return@forEach
            if (relativePath.startsWith(".github/")) return@forEach
            if (relativePath == "OWNERS") return@forEach
            val firstLine = it.useLines { lines -> lines.firstOrNull() }
            if (firstLine?.startsWith("# Bug component: ") != true) {
                throw Exception("$relativePath is missing `# Bug component: 000000`")
            }
        }
    }
}
