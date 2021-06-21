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

package androidx.inspection.gradle

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.File

/**
 * Generates a file into META-INF/ folder that has version of androidx.inspection used
 * during complication. Android Studio checks compatibility of its version with version required
 * by inspector.
 */
abstract class GenerateInspectionPlatformVersionTask : DefaultTask() {
    // ArtCollection can't be exposed as input as it is, so below there is "getCompileInputs"
    // that adds it properly as input.
    @get:Internal
    abstract var compileClasspath: ArtifactCollection

    @PathSensitive(PathSensitivity.NONE)
    @InputFiles
    fun getCompileInputs(): FileCollection = compileClasspath.artifactFiles

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @Input
    fun getVersion(): String {
        val artifacts = compileClasspath.artifacts
        val projectDep = artifacts.any {
            (it.id.componentIdentifier as? ProjectComponentIdentifier)?.projectPath ==
                ":inspection:inspection"
        }

        val prebuiltVersion = artifacts.mapNotNull {
            it.id.componentIdentifier as? ModuleComponentIdentifier
        }.firstOrNull { id ->
            id.group == "androidx.inspection" && id.module == "inspection"
        }?.version

        return if (projectDep) {
            "${project.project(":inspection:inspection").version}"
        } else prebuiltVersion ?: throw GradleException(
            "Inspector must have a dependency on androidx.inspection"
        )
    }

    @TaskAction
    fun exec() {
        val file = File(outputDir.asFile.get(), "META-INF/androidx_inspection.min_version")
        file.parentFile.mkdirs()
        file.writeText(getVersion())
    }
}

@ExperimentalStdlibApi
fun Project.registerGenerateInspectionPlatformVersionTask(
    variant: BaseVariant
): TaskProvider<GenerateInspectionPlatformVersionTask> {
    val name = variant.taskName("generateInspectionPlatformVersion")
    return tasks.register(name, GenerateInspectionPlatformVersionTask::class.java) {
        it.compileClasspath = variant.compileConfiguration.incoming.artifactView {
            it.attributes {
                it.attribute(Attribute.of("artifactType", String::class.java), "android-classes")
            }
        }.artifacts
        it.outputDir.set(taskWorkingDir(variant, "inspectionVersion"))
    }
}
