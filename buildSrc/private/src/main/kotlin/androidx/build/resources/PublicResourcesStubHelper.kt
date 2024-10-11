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
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package androidx.build.resources

import androidx.build.DeprecatedKotlinMultiplatformAndroidTarget
import androidx.build.getSupportRootFolder
import com.android.build.api.variant.LibraryVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

fun configurePublicResourcesStub(
    libraryVariant: LibraryVariant,
    copyPublicResourcesDirTask: TaskProvider<CopyPublicResourcesDirTask>
) =
    libraryVariant.sources.res.also {
        it?.addGeneratedSourceDirectory(
            copyPublicResourcesDirTask,
            CopyPublicResourcesDirTask::outputFolder
        )
    }

fun Project.configurePublicResourcesStub(kmpExtension: KotlinMultiplatformExtension) {
    val targetRes = project.layout.buildDirectory.dir("generated/res/public-stub")

    val generatePublicResourcesStub =
        tasks.register("generatePublicResourcesStub", Copy::class.java) { task ->
            task.from(File(project.getSupportRootFolder(), "buildSrc/res"))
            task.into(targetRes)
        }
    val sourceSet =
        kmpExtension.targets
            .withType(DeprecatedKotlinMultiplatformAndroidTarget::class.java)
            .single()
            .compilations
            .getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            .defaultSourceSet
    sourceSet.resources.srcDir(
        generatePublicResourcesStub.flatMap { project.provider { it.destinationDir } }
    )
}
