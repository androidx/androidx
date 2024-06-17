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

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Writes a configuration file in <a
 * href=https://source.android.com/devices/tech/test_infra/tradefed/testing/through-suite/android-test-structure>AndroidTest.xml</a>
 * format that gets zipped alongside the APKs to be tested.
 *
 * Generates XML for Tradefed test infrastructure and JSON for FTL test infrastructure.
 */
@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class GenerateTestConfigurationTask : DefaultTask() {

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val appApk: RegularFileProperty

    /** File existence check to determine whether to run this task. */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val androidTestSourceCodeCollection: ConfigurableFileCollection

    /**
     * Extracted APKs for PrivacySandbox SDKs dependencies. Produced by AGP.
     *
     * Should be set only for applications with PrivacySandbox SDKs dependencies.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val privacySandboxSdkApks: ConfigurableFileCollection

    /**
     * Extracted splits required for running app with PrivacySandbox SDKs. Produced by AGP.
     *
     * Should be set only for applications with PrivacySandbox SDKs dependencies.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val privacySandboxAppSplits: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val testApk: RegularFileProperty

    @get:Input abstract val applicationId: Property<String>

    @get:Input abstract val minSdk: Property<Int>

    @get:Input abstract val macrobenchmark: Property<Boolean>

    @get:Input abstract val hasBenchmarkPlugin: Property<Boolean>

    @get:Input abstract val testRunner: Property<String>

    @get:Input abstract val presubmit: Property<Boolean>

    @get:Input abstract val additionalApkKeys: ListProperty<String>

    @get:Input abstract val additionalTags: ListProperty<String>

    @get:Input abstract val instrumentationArgs: MapProperty<String, String>

    @get:OutputFile abstract val outputXml: RegularFileProperty

    /**
     * Optional as privacy sandbox not yet supported in JSON configs.
     *
     * TODO (b/347315428): Support privacy sandbox on FTL.
     */
    @get:[OutputFile Optional]
    abstract val outputJson: RegularFileProperty

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
        if (appApk.isPresent) {
            val appApkFile = appApk.get().asFile
            configBuilder.appApkName(appApkFile.name).appApkSha256(sha256(appApkFile))
        }

        val privacySandboxSdkApksFileNames =
            privacySandboxSdkApks.asFileTree.map { f -> f.name }.sorted()
        if (privacySandboxSdkApksFileNames.isNotEmpty()) {
            configBuilder.enablePrivacySandbox(true)
            configBuilder.initialSetupApks(privacySandboxSdkApksFileNames)
        }
        val privacySandboxSplitsFileNames =
            privacySandboxAppSplits.asFileTree.map { f -> f.name }.sorted()
        configBuilder.appSplits(privacySandboxSplitsFileNames)

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
        } else if (macrobenchmark.get()) {
            // macro benchmarks do not have a dryRunMode, so we don't run them in presubmit
            configBuilder.isMacrobenchmark(true)
            configBuilder.tag("macrobenchmarks")
            if (additionalTags.get().contains("wear")) {
                // Wear macrobenchmarks are tagged separately to enable running on wear in CI
                // standard macrobenchmarks don't currently run well on wear (b/189952249)
                configBuilder.tag("wear-macrobenchmarks")
            }
        } else {
            configBuilder.tag("androidx_unit_tests")
        }
        additionalTags.get().forEach { configBuilder.tag(it) }
        instrumentationArgs.get().forEach { (key, value) ->
            configBuilder.instrumentationArgsMap[key] = value
        }
        val testApkFile = testApk.get().asFile
        configBuilder
            .testApkName(testApkFile.name)
            .applicationId(applicationId.get())
            .minSdk(minSdk.get().toString())
            .testRunner(testRunner.get())
            .testApkSha256(sha256(testApkFile))
        createOrFail(outputXml).writeText(configBuilder.buildXml())
        if (outputJson.isPresent) {
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
