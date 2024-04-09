/*
 * Copyright 2023 The Android Open Source Project
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

import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.BuiltArtifactsLoader
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.getByType
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Expected to rerun every time")
abstract class FtlRunner : DefaultTask() {
    init {
        group = "Verification"
        description = "Runs devices tests in Firebase Test Lab filtered by --className"
    }

    @get:Inject abstract val execOperations: ExecOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testFolder: DirectoryProperty

    @get:Internal abstract val testLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    abstract val appFolder: DirectoryProperty

    @get:Internal abstract val appLoader: Property<BuiltArtifactsLoader>

    @get:Input abstract val apkPackageName: Property<String>

    @get:Optional
    @get:Input
    @get:Option(option = "className", description = "Fully qualified class name of a class to run")
    abstract val className: Property<String>

    @get:Optional
    @get:Input
    @get:Option(option = "packageName", description = "Package name test classes to run")
    abstract val packageName: Property<String>

    @get:Optional
    @get:Input
    @get:Option(option = "pullScreenshots", description = "true if screenshots should be pulled")
    abstract val pullScreenshots: Property<String>

    @get:Optional
    @get:Input
    @get:Option(option = "testTimeout", description = "timeout to pass to FTL test runner")
    abstract val testTimeout: Property<String>

    @get:Optional
    @get:Input
    @get:Option(
        option = "instrumentationArgs",
        description = "instrumentation arguments to pass to FTL test runner"
    )
    abstract val instrumentationArgs: Property<String>

    @get:Input abstract val device: ListProperty<String>

    @TaskAction
    fun execThings() {
        if (!System.getenv().containsKey("GOOGLE_APPLICATION_CREDENTIALS")) {
            throw Exception(
                "Running tests in FTL requires credentials, you have not set up " +
                    "GOOGLE_APPLICATION_CREDENTIALS, follow go/androidx-dev#remote-build-cache"
            )
        }
        val testApk =
            testLoader.get().load(testFolder.get())
                ?: throw RuntimeException("Cannot load required APK for task: $name")
        val testApkPath = testApk.elements.single().outputFile
        val appApkPath =
            if (appLoader.isPresent) {
                val appApk =
                    appLoader.get().load(appFolder.get())
                        ?: throw RuntimeException("Cannot load required APK for task: $name")
                appApk.elements.single().outputFile
            } else {
                "gs://androidx-ftl-test-results/github-ci-action/placeholderApp/" +
                    "d345c82828c355acc1432535153cf1dcf456e559c26f735346bf5f38859e0512.apk"
            }
        try {
            execOperations.exec { it.commandLine("gcloud", "--version") }
        } catch (exception: Exception) {
            throw Exception(
                "Missing gcloud, please follow go/androidx-dev#remote-build-cache to set it up"
            )
        }
        val hasFilters = className.isPresent || packageName.isPresent
        val filters =
            listOfNotNull(
                    if (className.isPresent) "class ${className.get()}" else null,
                    if (packageName.isPresent) "package ${packageName.get()}" else null,
                )
                .joinToString(separator = ",")

        val shouldPull = pullScreenshots.isPresent && pullScreenshots.get() == "true"

        execOperations.exec {
            it.commandLine(
                listOfNotNull(
                    "gcloud",
                    "--project",
                    "androidx-dev-prod",
                    "firebase",
                    "test",
                    "android",
                    "run",
                    "--type",
                    "instrumentation",
                    "--no-performance-metrics",
                    "--no-auto-google-login",
                    "--app",
                    appApkPath,
                    "--test",
                    testApkPath,
                    if (hasFilters) "--test-targets" else null,
                    if (hasFilters) filters else null,
                    if (shouldPull) "--directories-to-pull" else null,
                    if (shouldPull) {
                        "/sdcard/Android/data/${apkPackageName.get()}/cache/androidx_screenshots"
                    } else null,
                    if (testTimeout.isPresent) "--timeout" else null,
                    if (testTimeout.isPresent) testTimeout.get() else null,
                    if (instrumentationArgs.isPresent) "--environment-variables" else null,
                    if (instrumentationArgs.isPresent) instrumentationArgs.get() else null,
                ) +
                    getDeviceArguments(device.get())
            )
        }
    }

    private fun getDeviceArguments(devices: List<String>): List<String> {
        val deviceArguments = mutableListOf<String>()
        devices.forEach { device ->
            deviceArguments.addAll(
                listOf(
                    "--device",
                    "model=$device,locale=en_US,orientation=portrait"
                )
            )
        }
        return deviceArguments
    }
}

private const val NEXUS_6P = "Nexus6P,version=27"
private const val A10 = "a10,version=29"
private const val PETTYL = "pettyl,version=27"
private const val HWCOR = "HWCOR,version=27"
private const val Q2Q = "q2q,version=31"

private val devicesToRunOn =
    listOf(
        "ftlmediumphoneapi34" to listOf("MediumPhone.arm,version=34"),
        "ftlpixel2api33" to listOf("Pixel2.arm,version=33"),
        "ftlpixel2api30" to listOf("Pixel2.arm,version=30"),
        "ftlpixel2api28" to listOf("Pixel2.arm,version=28"),
        "ftlpixel2api26" to listOf("Pixel2.arm,version=26"),
        "ftlnexus4api21" to listOf("Nexus4,version=21"),
        "ftlCoreTelecomDeviceSet" to listOf(NEXUS_6P, A10, PETTYL, HWCOR, Q2Q),
    )

internal fun Project.registerRunner(
    name: String,
    artifacts: Artifacts,
    namespace: Provider<String>
) {
    devicesToRunOn.forEach { (taskPrefix, model) ->
        tasks.register("$taskPrefix$name", FtlRunner::class.java) { task ->
            task.device.set(model)
            task.apkPackageName.set(namespace)
            task.testFolder.set(artifacts.get(SingleArtifact.APK))
            task.testLoader.set(artifacts.getBuiltArtifactsLoader())
        }
    }
}

fun Project.configureFtlRunner(androidComponentsExtension: AndroidComponentsExtension<*, *, *>) {
    androidComponentsExtension.apply {
        onVariants { variant ->
            @Suppress("UnstableApiUsage") // usage of HasDeviceTests
            when {
                variant is HasDeviceTests -> {
                    variant.deviceTests.forEach { deviceTest ->
                        registerRunner(deviceTest.name, deviceTest.artifacts, deviceTest.namespace)
                    }
                }
                project.plugins.hasPlugin("com.android.test") -> {
                    registerRunner(variant.name, variant.artifacts, variant.namespace)
                }
            }
        }
    }
}

fun Project.configureFtlRunner(componentsExtension: KotlinMultiplatformAndroidComponentsExtension) {
    componentsExtension.onVariant { variant ->
        @Suppress("UnstableApiUsage") // usage of HasDeviceTests
        variant.deviceTests.forEach { deviceTest ->
            registerRunner(deviceTest.name, deviceTest.artifacts, deviceTest.namespace)
        }
    }
}

fun Project.addAppApkToFtlRunner() {
    extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
        onVariants(selector().withBuildType("debug")) { appVariant ->
            devicesToRunOn.forEach { (taskPrefix, _) ->
                tasks.named("$taskPrefix${appVariant.name}AndroidTest") { configTask ->
                    configTask as FtlRunner
                    configTask.appFolder.set(appVariant.artifacts.get(SingleArtifact.APK))
                    configTask.appLoader.set(appVariant.artifacts.getBuiltArtifactsLoader())
                }
            }
        }
    }
}
