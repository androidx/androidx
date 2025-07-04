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

import androidx.build.AndroidXExtension
import androidx.build.AndroidXImplPlugin.Companion.FINALIZE_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.androidXExtension
import androidx.build.asFilenamePrefix
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.getFileInTestConfigDirectory
import androidx.build.hasBenchmarkPlugin
import androidx.build.isMacrobenchmark
import androidx.build.isPresubmitBuild
import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApkOutputProviders
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.TestAndroidComponentsExtension
import com.android.build.api.variant.Variant
import java.util.function.Consumer
import kotlin.math.max
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.attributes.Usage
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

/**
 * Creates and configures the test config generation task for a project. Configuration includes
 * populating the task with relevant data from the first 4 params, and setting whether the task is
 * enabled.
 */
private fun Project.createTestConfigurationGenerationTask(
    variantName: String,
    artifacts: Artifacts,
    minSdk: Int,
    testRunner: Provider<String>,
    instrumentationRunnerArgs: Provider<Map<String, String>>,
    variant: Variant?,
    projectIsolationEnabled: Boolean,
) {
    val copyTestApksTask = registerCopyTestApksTask(variantName, artifacts, variant)

    if (isPrivacySandboxEnabled()) {
        /*
        Privacy Sandbox SDKs could be installed starting from PRIVACY_SANDBOX_MIN_API_LEVEL.
        Separate compat config generated for lower api levels.
        */
        registerGenerateTestConfigurationTask(
            "${GENERATE_PRIVACY_SANDBOX_MAIN_TEST_CONFIGURATION_TASK}$variantName",
            TestConfigType.PRIVACY_SANDBOX_MAIN,
            xmlName = "${path.asFilenamePrefix()}$variantName.xml",
            jsonName = null, // Privacy sandbox not yet supported in JSON configs
            copyTestApksTask.flatMap { it.outputApplicationId },
            copyTestApksTask.flatMap { it.outputTestApk },
            minSdk = max(minSdk, PRIVACY_SANDBOX_MIN_API_LEVEL),
            testRunner,
            instrumentationRunnerArgs,
            variant,
            projectIsolationEnabled,
        )

        registerGenerateTestConfigurationTask(
            "${GENERATE_PRIVACY_SANDBOX_COMPAT_TEST_CONFIGURATION_TASK}${variantName}",
            TestConfigType.PRIVACY_SANDBOX_COMPAT,
            xmlName = "${path.asFilenamePrefix()}${variantName}Compat.xml",
            jsonName = null, // Privacy sandbox not yet supported in JSON configs
            copyTestApksTask.flatMap { it.outputApplicationId },
            copyTestApksTask.flatMap { it.outputTestApk },
            minSdk,
            testRunner,
            instrumentationRunnerArgs,
            variant,
            projectIsolationEnabled,
        )
    } else {
        registerGenerateTestConfigurationTask(
            "${GENERATE_TEST_CONFIGURATION_TASK}$variantName",
            TestConfigType.DEFAULT,
            xmlName = "${path.asFilenamePrefix()}$variantName.xml",
            jsonName = "_${path.asFilenamePrefix()}$variantName.json",
            copyTestApksTask.flatMap { it.outputApplicationId },
            copyTestApksTask.flatMap { it.outputTestApk },
            minSdk,
            testRunner,
            instrumentationRunnerArgs,
            variant,
            projectIsolationEnabled,
        )
    }
}

private fun Project.registerCopyTestApksTask(
    variantName: String,
    artifacts: Artifacts,
    variant: Variant?,
): TaskProvider<CopyTestApksTask> {
    return tasks.register("${COPY_TEST_APKS_TASK}$variantName", CopyTestApksTask::class.java) { task
        ->
        task.testFolder.set(artifacts.get(SingleArtifact.APK))
        task.testLoader.set(artifacts.getBuiltArtifactsLoader())

        task.outputApplicationId.set(layout.buildDirectory.file("$variantName-appId.txt"))
        task.outputTestApk.set(
            getFileInTestConfigDirectory("${path.asFilenamePrefix()}-$variantName.apk")
        )

        // Skip task if getTestSourceSetsForAndroid is empty, even if
        //  androidXExtension.deviceTests.enabled is set to true
        task.androidTestSourceCode.from(getTestSourceSetsForAndroid(variant))
        val androidXExtension = extensions.getByType<AndroidXExtension>()
        task.enabled = androidXExtension.deviceTests.enabled
        AffectedModuleDetector.configureTaskGuard(task)
    }
}

private fun Project.registerGenerateTestConfigurationTask(
    taskName: String,
    configType: TestConfigType,
    xmlName: String,
    jsonName: String?,
    applicationIdFile: Provider<RegularFile>,
    testApk: Provider<RegularFile>,
    minSdk: Int,
    testRunner: Provider<String>,
    instrumentationRunnerArgs: Provider<Map<String, String>>,
    variant: Variant?,
    projectIsolationEnabled: Boolean,
) {
    val generateTestConfigurationTask =
        tasks.register(taskName, GenerateTestConfigurationTask::class.java) { task ->
            task.testConfigType.set(configType)

            task.applicationId.set(project.providers.fileContents(applicationIdFile).asText)
            task.testApk.set(testApk)

            val androidXExtension = extensions.getByType<AndroidXExtension>()
            task.additionalApkKeys.set(androidXExtension.additionalDeviceTestApkKeys)
            task.additionalTags.set(androidXExtension.additionalDeviceTestTags)
            task.outputXml.set(getFileInTestConfigDirectory(xmlName))
            jsonName?.let { task.outputJson.set(getFileInTestConfigDirectory(it)) }
            task.presubmit.set(isPresubmitBuild())
            task.instrumentationArgs.putAll(instrumentationRunnerArgs)
            task.minSdk.set(minSdk)
            task.hasBenchmarkPlugin.set(hasBenchmarkPlugin())
            task.macrobenchmark.set(isMacrobenchmark())
            task.testRunner.set(testRunner)
            // Skip task if getTestSourceSetsForAndroid is empty, even if
            //  androidXExtension.deviceTests.enabled is set to true
            task.androidTestSourceCodeCollection.from(getTestSourceSetsForAndroid(variant))
            task.enabled = androidXExtension.deviceTests.enabled
            AffectedModuleDetector.configureTaskGuard(task)
        }
    if (!projectIsolationEnabled) {
        rootProject.tasks
            .findByName(FINALIZE_TEST_CONFIGS_WITH_APKS_TASK)!!
            .dependsOn(generateTestConfigurationTask)
        addToModuleInfo(testName = xmlName, projectIsolationEnabled)
    }
    androidXExtension.testModuleNames.add(xmlName)
}

/**
 * Further configures the test config generation task for a project. This only gets called when
 * there is a test app in addition to the instrumentation app, and the only thing it configures is
 * the location of the testapp.
 */
fun Project.addAppApkToTestConfigGeneration(androidXExtension: AndroidXExtension) {

    fun outputAppApkFile(
        variant: Variant,
        appProjectPath: String,
        instrumentationProjectPath: String?,
    ): Provider<RegularFile> {
        var filename = appProjectPath.asFilenamePrefix()
        if (instrumentationProjectPath != null) {
            filename += "_for_${instrumentationProjectPath.asFilenamePrefix()}"
        }
        filename += "-${variant.name}.apk"
        return getFileInTestConfigDirectory(filename)
    }

    // For application modules, the instrumentation apk is generated in the module itself
    extensions.findByType(ApplicationAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().withBuildType("debug")) { variant ->
            if (isPrivacySandboxEnabled()) {
                addAppApksToPrivacySandboxTestConfigsGeneration(
                    testVariantName = "${variant.name}AndroidTest",
                    variant,
                    variant.outputProviders,
                )
            } else {
                // TODO(b/347956800): Migrate to ApkOutputProviders after testing on PrivacySandbox
                addAppApkFromArtifactsToTestConfigGeneration(
                    testVariantName = "${variant.name}AndroidTest",
                    variant,
                    configureAction = { task ->
                        task.appFolder.set(variant.artifacts.get(SingleArtifact.APK))

                        // The target project is the same being evaluated
                        task.outputAppApk.set(outputAppApkFile(variant, path, null))
                    },
                )
            }
        }
    }

    // Migrate away when b/280680434 is fixed.
    // For tests modules, the instrumentation apk is pulled from the <variant>TestedApks
    // configuration. Note that also the associated test configuration task name is different
    // from the application one.
    extensions.findByType(TestAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().all()) { variant ->
            if (isPrivacySandboxEnabled()) {
                addAppApksToPrivacySandboxTestConfigsGeneration(
                    testVariantName = variant.name,
                    variant,
                    variant.outputProviders,
                )
            } else {
                // TODO(b/347956800): Migrate to ApkOutputProviders after b/378675038
                addAppApkFromArtifactsToTestConfigGeneration(
                    testVariantName = variant.name,
                    variant,
                    configureAction = { task ->
                        // The target app path is defined in the targetProjectPath field in the
                        // android extension of the test module
                        val targetProjectPath =
                            project.extensions
                                .getByType(TestExtension::class.java)
                                .targetProjectPath
                                ?: throw IllegalStateException(
                                    """
                                Module `$path` does not have a targetProjectPath defined.
                            """
                                        .trimIndent()
                                )
                        task.outputAppApk.set(outputAppApkFile(variant, targetProjectPath, path))

                        task.appFileCollection.from(
                            configurations
                                .named("${variant.name}TestedApks")
                                .get()
                                .incoming
                                .artifactView {
                                    it.attributes { container ->
                                        container.attribute(ARTIFACT_TYPE_ATTRIBUTE, "apk")
                                    }
                                }
                                .files
                        )
                    },
                )
            }
        }
    }

    // For library modules we only look at the build type release. The target app project can be
    // specified through the androidX extension, through: targetAppProjectForInstrumentationTest
    // and targetAppProjectVariantForInstrumentationTest.
    extensions.findByType(LibraryAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().withBuildType("release")) { variant ->
            val targetAppProject =
                androidXExtension.deviceTests.targetAppProject ?: return@onVariants
            val targetAppProjectVariant = androidXExtension.deviceTests.targetAppVariant

            // Recreate the same configuration existing for test modules to pull the artifact
            // from the application module specified in the deviceTests extension.
            @Suppress(
                "UnstableApiUsage"
            ) // Incubating dependencyFactory APIs https://github.com/gradle/gradle/issues/33923
            val configuration =
                configurations.create("${variant.name}TestedApks") { config ->
                    config.isCanBeResolved = true
                    config.isCanBeConsumed = false
                    config.attributes {
                        it.attribute(
                            BuildTypeAttr.ATTRIBUTE,
                            objects.named(targetAppProjectVariant),
                        )
                        it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                    }
                    config.dependencies.add(project.dependencyFactory.create(targetAppProject))
                }

            addAppApkFromArtifactsToTestConfigGeneration(
                testVariantName = "${variant.name}AndroidTest",
                variant,
                configureAction = { task ->
                    // The target app path is defined in the androidx extension
                    task.outputAppApk.set(outputAppApkFile(variant, targetAppProject.path, path))

                    task.appFileCollection.from(
                        configuration.incoming
                            .artifactView { view ->
                                view.attributes { it.attribute(ARTIFACT_TYPE_ATTRIBUTE, "apk") }
                            }
                            .files
                    )
                },
            )
        }
    }
}

private fun Project.addAppApkFromArtifactsToTestConfigGeneration(
    testVariantName: String,
    variant: Variant,
    configureAction: Consumer<CopyApkFromArtifactsTask>,
) {
    val copyApkTask = registerCopyAppApkFromArtifactsTask(variant, configureAction)
    tasks.named(
        "${GENERATE_TEST_CONFIGURATION_TASK}$testVariantName",
        GenerateTestConfigurationTask::class.java,
    ) { t ->
        t.appApksModel.set(copyApkTask.flatMap(CopyApkFromArtifactsTask::outputAppApksModel))
    }
}

// TODO(b/347956800): Call from createTestConfigurationGenerationTask() after b/378674313
// TODO(b/347956800): Use tasks providers directly instead of tasks.named after b/378674313
@Suppress("UnstableApiUsage") // ApkOutputProviders b/397701480
private fun Project.addAppApksToPrivacySandboxTestConfigsGeneration(
    testVariantName: String,
    variant: Variant,
    outputProviders: ApkOutputProviders,
) {
    val copyTestApksTask =
        tasks.named("${COPY_TEST_APKS_TASK}${testVariantName}", CopyTestApksTask::class.java)
    val excludeTestApk = copyTestApksTask.flatMap(CopyTestApksTask::outputTestApk)

    val copyMainApksTask =
        registerCopyPrivacySandboxMainAppApksTask(variant, outputProviders, excludeTestApk)
    tasks.named(
        "${GENERATE_PRIVACY_SANDBOX_MAIN_TEST_CONFIGURATION_TASK}${testVariantName}",
        GenerateTestConfigurationTask::class.java,
    ) { t ->
        t.appApksModel.set(
            copyMainApksTask.flatMap(CopyApksFromOutputProviderTask::outputAppApksModel)
        )
    }

    val copyCompatApksTask =
        registerCopyPrivacySandboxCompatAppApksTask(variant, outputProviders, excludeTestApk)
    tasks.named(
        "${GENERATE_PRIVACY_SANDBOX_COMPAT_TEST_CONFIGURATION_TASK}${testVariantName}",
        GenerateTestConfigurationTask::class.java,
    ) { t ->
        t.appApksModel.set(
            copyCompatApksTask.flatMap(CopyApksFromOutputProviderTask::outputAppApksModel)
        )
    }
}

fun Project.configureTestConfigGeneration(projectIsolationEnabled: Boolean) {
    extensions.getByType(AndroidComponentsExtension::class.java).apply {
        onVariants { variant ->
            when {
                variant is HasDeviceTests -> {
                    variant.deviceTests.forEach { (_, deviceTest) ->
                        createTestConfigurationGenerationTask(
                            deviceTest.name,
                            deviceTest.artifacts,
                            @Suppress("UnstableApiUsage") // b/409617582
                            deviceTest.minSdk.apiLevel,
                            deviceTest.instrumentationRunner,
                            deviceTest.instrumentationRunnerArguments,
                            variant,
                            projectIsolationEnabled,
                        )
                    }
                }
                project.plugins.hasPlugin("com.android.test") -> {
                    val testExtension = project.extensions.getByType<TestExtension>()
                    createTestConfigurationGenerationTask(
                        variant.name,
                        variant.artifacts,
                        variant.minSdk.apiLevel,
                        provider { testExtension.defaultConfig.testInstrumentationRunner!! },
                        provider { testExtension.defaultConfig.testInstrumentationRunnerArguments },
                        variant,
                        projectIsolationEnabled,
                    )
                }
            }
        }
    }
}

private fun Project.isPrivacySandboxEnabled(): Boolean =
    extensions.findByType(ApplicationExtension::class.java)?.privacySandbox?.enable
        ?: extensions.findByType(TestExtension::class.java)?.privacySandbox?.enable
        ?: false

private const val COPY_TEST_APKS_TASK = "CopyTestApks"
private const val GENERATE_PRIVACY_SANDBOX_MAIN_TEST_CONFIGURATION_TASK =
    "GeneratePrivacySandboxMainTestConfiguration"
private const val GENERATE_PRIVACY_SANDBOX_COMPAT_TEST_CONFIGURATION_TASK =
    "GeneratePrivacySandboxCompatTestConfiguration"
private const val GENERATE_TEST_CONFIGURATION_TASK = "GenerateTestConfiguration"
