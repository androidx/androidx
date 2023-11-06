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

package androidx.build.testConfiguration

import com.android.build.api.variant.BuiltArtifactsLoader
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Writes a configuration file in <a
 * href=https://source.android.com/devices/tech/test_infra/tradefed/testing/through-suite/android-test-structure>AndroidTest.xml</a>
 * format that gets zipped alongside the APKs to be tested. This config gets ingested by Tradefed.
 */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class GenerateTestConfigurationTask
@Inject
constructor(private val objects: ObjectFactory) : DefaultTask() {

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appFolder: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appFileCollection: ConfigurableFileCollection

    @get:Internal abstract val appLoader: Property<BuiltArtifactsLoader>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testFolder: DirectoryProperty

    @get:Internal abstract val testLoader: Property<BuiltArtifactsLoader>

    @get:Input abstract val testProjectPath: Property<String>

    @get:Input abstract val minSdk: Property<Int>

    @get:Input abstract val hasBenchmarkPlugin: Property<Boolean>

    @get:Input abstract val testRunner: Property<String>

    @get:Input abstract val presubmit: Property<Boolean>

    @get:Input abstract val additionalApkKeys: ListProperty<String>

    @get:Input abstract val additionalTags: ListProperty<String>

    @get:OutputFile abstract val outputXml: RegularFileProperty

    @get:OutputFile abstract val outputJson: RegularFileProperty

    @get:OutputFile abstract val outputTestApk: RegularFileProperty

    @get:[OutputFile Optional]
    abstract val outputAppApk: RegularFileProperty

    @TaskAction
    fun generateAndroidTestZip() {
        /*
        Testing an Android Application project involves 2 APKS: an application to be instrumented,
        and a test APK. Testing an Android Library project involves only 1 APK, since the library
        is bundled inside the test APK, meaning it is self instrumenting. We add extra data to
        configurations testing Android Application projects, so that both APKs get installed.
         */
        val configBuilder = ConfigBuilder()
        configBuilder.configName = outputXml.asFile.get().name
        if (appLoader.isPresent) {

            // Decides where to load the app apk from, depending on whether appFolder or
            // appFileCollection has been set.
            val appDir =
                if (appFolder.isPresent && appFileCollection.files.isEmpty()) {
                    appFolder.get()
                } else if (!appFolder.isPresent && appFileCollection.files.size == 1) {
                    objects
                        .directoryProperty()
                        .also { it.set(appFileCollection.files.first()) }
                        .get()
                } else {
                    throw IllegalStateException(
                        """
                    App apk not specified or both appFileCollection and appFolder specified.
                """
                            .trimIndent()
                    )
                }

            val appApk =
                appLoader.get().load(appDir)
                    ?: throw RuntimeException("Cannot load required APK for task: $name")
            // We don't need to check hasBenchmarkPlugin because benchmarks shouldn't have test apps
            val appApkBuiltArtifact = appApk.elements.single()
            val destinationApk = outputAppApk.get().asFile
            File(appApkBuiltArtifact.outputFile).copyTo(destinationApk, overwrite = true)
            configBuilder
                .appApkName(destinationApk.name)
                .appApkSha256(sha256(File(appApkBuiltArtifact.outputFile)))
        }
        configBuilder.additionalApkKeys(additionalApkKeys.get())
        val isPresubmit = presubmit.get()
        configBuilder.isPostsubmit(!isPresubmit)
        // This section adds metadata tags that will help filter runners to specific modules.
        if (hasBenchmarkPlugin.get()) {
            configBuilder.isMicrobenchmark(true)

            // tag microbenchmarks as "microbenchmarks" in either build config, so that benchmark
            // test configs will always have something to run, regardless of build (though presubmit
            // builds will still set dry run, and not output metrics)
            configBuilder.tag("microbenchmarks")

            if (isPresubmit) {
                // in presubmit, we treat micro benchmarks as regular correctness tests as
                // they run with dryRunMode to check crashes don't happen, without measurement
                configBuilder.tag("androidx_unit_tests")
            }
        } else if (testProjectPath.get().endsWith("macrobenchmark")) {
            // macro benchmarks do not have a dryRunMode, so we don't run them in presubmit
            configBuilder.isMacrobenchmark(true)
            configBuilder.tag("macrobenchmarks")
        } else {
            configBuilder.tag("androidx_unit_tests")
        }
        additionalTags.get().forEach { configBuilder.tag(it) }
        val testApk =
            testLoader.get().load(testFolder.get())
                ?: throw RuntimeException("Cannot load required APK for task: $name")
        val testApkBuiltArtifact = testApk.elements.single()
        val destinationApk = outputTestApk.get().asFile
        File(testApkBuiltArtifact.outputFile).copyTo(destinationApk, overwrite = true)
        configBuilder
            .testApkName(destinationApk.name)
            .applicationId(testApk.applicationId)
            .minSdk(minSdk.get().toString())
            .testRunner(testRunner.get())
            .testApkSha256(sha256(File(testApkBuiltArtifact.outputFile)))
        createOrFail(outputXml).writeText(configBuilder.buildXml())
        if (!outputJson.asFile.get().name.startsWith("_")) {
            // Prefixing json file names with _ allows us to collocate these files
            // inside of the androidTest.zip to make fetching them less expensive.
            throw GradleException(
                "json output file names are expected to use _ prefix to, " +
                    "currently set to ${outputJson.asFile.get().name}"
            )
        }
        createOrFail(outputJson).writeText(configBuilder.buildJson())
    }
}

internal fun createOrFail(fileProperty: RegularFileProperty): File {
    val resolvedFile: File = fileProperty.asFile.get()
    if (!resolvedFile.exists()) {
        if (!resolvedFile.createNewFile()) {
            throw RuntimeException("Failed to create test configuration file: $resolvedFile")
        }
    }
    return resolvedFile
}
