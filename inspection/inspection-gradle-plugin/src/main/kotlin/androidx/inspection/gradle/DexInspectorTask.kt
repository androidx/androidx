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

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import org.anarres.gradle.plugin.jarjar.JarjarTask
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.File

abstract class DexInspectorTask : DefaultTask() {
    @get:InputFile
    abstract val dxExecutable: RegularFileProperty

    @get:InputFiles
    abstract val jars: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun exec() {
        val output = outputFile.get().asFile
        output.parentFile.mkdirs()
        project.exec {
            it.executable = dxExecutable.get().asFile.absolutePath
            val flatten = jars.map { file -> file.absolutePath }
            it.args = listOf("--dex", "--output", output.absolutePath) + flatten
        }
    }

    fun setDx(sdkDir: File, toolsVersion: String) {
        dxExecutable.set(File(sdkDir, "build-tools/$toolsVersion/dx"))
    }
}

// variant.taskName relies on @ExperimentalStdlibApi api
@ExperimentalStdlibApi
fun Project.registerUnzipTask(variant: LibraryVariant): TaskProvider<Copy> {
    return tasks.register(variant.taskName("unpackInspectorAAR"), Copy::class.java) {
        it.from(zipTree(variant.packageLibraryProvider!!.get().archiveFile))
        it.into(taskWorkingDir(variant, "unpackedInspectorAAR"))
        it.dependsOn(variant.assembleProvider)
    }
}

// variant.taskName relies on @ExperimentalStdlibApi api
@ExperimentalStdlibApi
fun Project.registerDexInspectorTask(
    variant: BaseVariant,
    extension: BaseExtension,
    jarJar: TaskProvider<JarjarTask>
) {
    tasks.register(variant.taskName("dexInspector"), DexInspectorTask::class.java) {
        it.setDx(extension.sdkDirectory, extension.buildToolsVersion)
        it.jars.from(jarJar.get().destinationPath)
        val out = File(taskWorkingDir(variant, "dexedInspector"), "${project.name}.jar")
        it.outputFile.set(out)
        it.dependsOn(jarJar)
    }
}
