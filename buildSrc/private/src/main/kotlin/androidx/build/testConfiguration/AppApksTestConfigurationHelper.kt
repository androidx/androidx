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

package androidx.build.testConfiguration

import androidx.build.androidXExtension
import androidx.build.dependencyTracker.AffectedModuleDetector
import com.android.build.api.variant.Variant
import java.util.function.Consumer
import org.gradle.api.Project
import org.gradle.api.provider.Provider

internal fun Project.registerCopyAppApkFromArtifactsTask(
    variant: Variant,
    configureAction: Consumer<CopyApkFromArtifactsTask>, // For lazy task init
): Provider<CopyApkFromArtifactsTask> =
    tasks.register("CopyAppApk${variant.name}", CopyApkFromArtifactsTask::class.java) { task ->
        task.appLoader.set(variant.artifacts.getBuiltArtifactsLoader())

        configureAction.accept(task)

        task.outputAppApksModel.set(layout.buildDirectory.file("AppApksModel${variant.name}.json"))

        task.androidTestSourceCode.from(getTestSourceSetsForAndroid(variant))
        task.enabled = androidXExtension.deviceTests.enabled
        AffectedModuleDetector.configureTaskGuard(task)
    }
