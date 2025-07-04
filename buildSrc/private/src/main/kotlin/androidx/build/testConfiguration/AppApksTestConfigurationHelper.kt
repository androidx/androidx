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

import androidx.build.AndroidXImplPlugin.Companion.SUPPORTED_BUILD_ABIS
import androidx.build.androidXExtension
import androidx.build.asFilenamePrefix
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.getAppApksFilesDirectory
import com.android.build.api.variant.ApkOutputProviders
import com.android.build.api.variant.DeviceSpec
import com.android.build.api.variant.Variant
import java.util.function.Consumer
import kotlin.math.max
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

internal fun Project.registerCopyPrivacySandboxMainAppApksTask(
    variant: Variant,
    outputProviders: ApkOutputProviders,
    excludeTestApk: Provider<RegularFile>,
): Provider<CopyApksFromOutputProviderTask> =
    registerCopyApksFromOutputProviderTask(
        taskName = "CopyPrivacySandboxMainAppApks${variant.name}",
        variant,
        outputProviders,
        excludeTestApk,
        outputFileNamePrefix = "${path.asFilenamePrefix()}-${variant.name}-sandbox-enabled",
        outputApkModelFileName = "PrivacySandboxMainAppApksModel${variant.name}.json",
        deviceSpec = mainSandboxDeviceSpec(variant.minSdk.apiLevel),
    )

internal fun Project.registerCopyPrivacySandboxCompatAppApksTask(
    variant: Variant,
    outputProviders: ApkOutputProviders,
    excludeTestApk: Provider<RegularFile>,
): Provider<CopyApksFromOutputProviderTask> =
    registerCopyApksFromOutputProviderTask(
        taskName = "CopyPrivacySandboxCompatAppApks${variant.name}",
        variant,
        outputProviders,
        excludeTestApk,
        outputFileNamePrefix = "${path.asFilenamePrefix()}-${variant.name}-sandbox-compat",
        outputApkModelFileName = "PrivacySandboxCompatAppApksModel${variant.name}.json",
        deviceSpec = compatSandboxDeviceSpec(variant.minSdk.apiLevel),
    )

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

@Suppress("UnstableApiUsage") // DeviceSpec b/397703661
private fun Project.registerCopyApksFromOutputProviderTask(
    taskName: String,
    variant: Variant,
    outputProviders: ApkOutputProviders,
    excludeTestApk: Provider<RegularFile>,
    outputFileNamePrefix: String,
    outputApkModelFileName: String,
    deviceSpec: DeviceSpec,
): Provider<CopyApksFromOutputProviderTask> {
    val copyApksTask =
        tasks.register(taskName, CopyApksFromOutputProviderTask::class.java) { task ->
            task.excludeTestApk.set(excludeTestApk)

            task.outputFilenamesPrefix.set(outputFileNamePrefix)
            task.outputDirectory.set(
                getAppApksFilesDirectory().map { it.dir("${path.asFilenamePrefix()}-$taskName") }
            )

            task.outputAppApksModel.set(layout.buildDirectory.file(outputApkModelFileName))

            task.androidTestSourceCode.from(getTestSourceSetsForAndroid(variant))
            task.enabled = androidXExtension.deviceTests.enabled
            AffectedModuleDetector.configureTaskGuard(task)
        }

    outputProviders.provideApkOutputToTask(
        copyApksTask,
        CopyApksFromOutputProviderTask::apkOutput,
        deviceSpec,
    )

    return copyApksTask
}

@Suppress("UnstableApiUsage") // DeviceSpec b/397703661
private fun mainSandboxDeviceSpec(minApiLevel: Int): DeviceSpec =
    DeviceSpec.Builder()
        .setApiLevel(max(minApiLevel, PRIVACY_SANDBOX_MIN_API_LEVEL))
        .setSupportsPrivacySandbox(true)
        .setAbis(SUPPORTED_BUILD_ABIS) // To pass filters in defaultConfig.ndk.abiFilters
        .build()

@Suppress("UnstableApiUsage") // DeviceSpec b/397703661
private fun compatSandboxDeviceSpec(minApiLevel: Int): DeviceSpec =
    DeviceSpec.Builder()
        .setApiLevel(minApiLevel)
        .setSupportsPrivacySandbox(false)
        .setAbis(SUPPORTED_BUILD_ABIS) // To pass filters in defaultConfig.ndk.abiFilters
        .build()

internal const val PRIVACY_SANDBOX_MIN_API_LEVEL = 34
