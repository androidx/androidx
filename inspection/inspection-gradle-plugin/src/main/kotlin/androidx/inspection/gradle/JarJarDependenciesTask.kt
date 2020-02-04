/*
 * Copyright 2020 The Android Open Source Project
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

import com.android.build.gradle.api.BaseVariant
import org.anarres.gradle.plugin.jarjar.JarjarTask
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.jar.JarFile

// variant.taskName relies on @ExperimentalStdlibApi api
@ExperimentalStdlibApi
fun Project.registerJarJarDependenciesTask(
    variant: BaseVariant,
    zipTask: TaskProvider<Copy>
): TaskProvider<JarjarTask> {
    return tasks.register(
        variant.taskName("jarJarDependencies"),
        JarjarTask::class.java
    ) {
        it.from(variant.runtimeConfiguration)
        val fileTree = project.fileTree(zipTask.get().destinationDir)
        fileTree.include("**/*.jar")
        it.from(fileTree)
        it.destinationDir = taskWorkingDir(variant, "jarJar")
        it.destinationName = "${project.name}-shadowed.jar"
        it.dependsOn(zipTask)
        val prefix = "deps.${project.name.replace('-', '.')}"
        it.doFirst {
            val task = it as JarjarTask
            variant.runtimeConfiguration.files.extractPackageNames().forEach { packageName ->
                task.classRename("$packageName.**", "$prefix.$packageName.@1")
            }
        }
    }
}

private fun Set<File>.extractPackageNames(): Set<String> = map(::JarFile)
    .map { jar -> jar.use { it.entries().toList() } }.flatten()
    .filter { jarEntry -> jarEntry.name.endsWith(".class") }
    .map { jarEntry -> jarEntry.name.substringBeforeLast("/").replace('/', '.') }
    .toSet()
