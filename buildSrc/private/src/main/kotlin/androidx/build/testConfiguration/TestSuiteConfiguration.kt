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
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package androidx.build.testConfiguration

import androidx.build.AndroidXExtension
import androidx.build.AndroidXImplPlugin.Companion.FINALIZE_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.DeprecatedKotlinMultiplatformAndroidTarget
import androidx.build.androidXExtension
import androidx.build.asFilenamePrefix
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.getFileInTestConfigDirectory
import androidx.build.getPrivacySandboxFilesDirectory
import androidx.build.hasBenchmarkPlugin
import androidx.build.isMacrobenchmark
import androidx.build.isPresubmitBuild
import androidx.build.multiplatformExtension
import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.TestExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.TestAndroidComponentsExtension
import com.android.build.api.variant.TestVariant
import com.android.build.api.variant.Variant
import kotlin.math.max
import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.attributes.Usage
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

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
            xmlName = "${path.asFilenamePrefix()}$variantName.xml",
            jsonName = null, // Privacy sandbox not yet supported in JSON configs
            copyTestApksTask.flatMap { it.outputApplicationId },
            copyTestApksTask.flatMap { it.outputAppApk },
            copyTestApksTask.flatMap { it.outputTestApk },
            copyTestApksTask.flatMap { it.outputPrivacySandboxSdkApks },
            copyTestApksTask.flatMap { it.outputPrivacySandboxAppSplits },
            minSdk = max(minSdk, PRIVACY_SANDBOX_MIN_API_LEVEL),
            testRunner,
            instrumentationRunnerArgs,
            variant,
            projectIsolationEnabled,
        )

        registerGenerateTestConfigurationTask(
            "${GENERATE_PRIVACY_SANDBOX_COMPAT_TEST_CONFIGURATION_TASK}${variantName}",
            xmlName = "${path.asFilenamePrefix()}${variantName}Compat.xml",
            jsonName = null, // Privacy sandbox not yet supported in JSON configs
            copyTestApksTask.flatMap { it.outputApplicationId },
            copyTestApksTask.flatMap { it.outputAppApk },
            copyTestApksTask.flatMap { it.outputTestApk },
            privacySandboxApks = null,
            copyTestApksTask.flatMap { it.outputPrivacySandboxCompatAppSplits },
            minSdk,
            testRunner,
            instrumentationRunnerArgs,
            variant,
            projectIsolationEnabled,
        )
    } else {
        registerGenerateTestConfigurationTask(
            "${GENERATE_TEST_CONFIGURATION_TASK}$variantName",
            xmlName = "${path.asFilenamePrefix()}$variantName.xml",
            jsonName = "_${path.asFilenamePrefix()}$variantName.json",
            copyTestApksTask.flatMap { it.outputApplicationId },
            copyTestApksTask.flatMap { it.outputAppApk },
            copyTestApksTask.flatMap { it.outputTestApk },
            privacySandboxApks = null,
            privacySandboxSplits = null,
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
    variant: Variant?
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
    xmlName: String,
    jsonName: String?,
    applicationIdFile: Provider<RegularFile>,
    appApk: Provider<RegularFile>,
    testApk: Provider<RegularFile>,
    privacySandboxApks: Provider<Directory>?,
    privacySandboxSplits: Provider<Directory>?,
    minSdk: Int,
    testRunner: Provider<String>,
    instrumentationRunnerArgs: Provider<Map<String, String>>,
    variant: Variant?,
    projectIsolationEnabled: Boolean,
) {
    val generateTestConfigurationTask =
        tasks.register(taskName, GenerateTestConfigurationTask::class.java) { task ->
            task.applicationId.set(project.providers.fileContents(applicationIdFile).asText)
            task.appApk.set(appApk)
            task.testApk.set(testApk)

            privacySandboxApks?.let { task.privacySandboxSdkApks.from(it) }
            privacySandboxSplits?.let { task.privacySandboxAppSplits.from(it) }

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
        instrumentationProjectPath: String?
    ): Provider<RegularFile> {
        var filename = appProjectPath.asFilenamePrefix()
        if (instrumentationProjectPath != null) {
            filename += "_for_${instrumentationProjectPath.asFilenamePrefix()}"
        }
        filename += "-${variant.name}.apk"
        return getFileInTestConfigDirectory(filename)
    }

    fun addPrivacySandboxApksFor(variant: Variant, task: CopyTestApksTask) {
        // TODO (b/309610890): Replace for dependency on AGP artifact.
        val extractedPrivacySandboxSdkApksDir =
            layout.buildDirectory.dir("intermediates/extracted_apks_from_privacy_sandbox_sdks")
        task.privacySandboxSdkApks.from(
            files(extractedPrivacySandboxSdkApksDir) {
                it.builtBy("buildPrivacySandboxSdkApksForDebug")
            }
        )
        // TODO (b/309610890): Replace for dependency on AGP artifact.
        val usesSdkSplitDir =
            layout.buildDirectory.dir("intermediates/uses_sdk_library_split_for_local_deployment")
        task.privacySandboxUsesSdkSplit.from(
            files(usesSdkSplitDir) {
                it.builtBy("generateDebugAdditionalSplitForPrivacySandboxDeployment")
            }
        )
        // TODO (b/309610890): Replace for dependency on AGP artifact.
        val extractedPrivacySandboxCompatSplitsDir =
            layout.buildDirectory.dir("intermediates/extracted_sdk_apks")
        task.privacySandboxSdkCompatSplits.from(
            files(extractedPrivacySandboxCompatSplitsDir) {
                it.builtBy("extractApksFromSdkSplitsForDebug")
            }
        )
        task.filenamePrefixForPrivacySandboxFiles.set("${path.asFilenamePrefix()}-${variant.name}")
        task.outputPrivacySandboxSdkApks.set(
            getPrivacySandboxFilesDirectory().map {
                it.dir("${path.asFilenamePrefix()}-${variant.name}-sdks")
            }
        )
        task.outputPrivacySandboxAppSplits.set(
            getPrivacySandboxFilesDirectory().map {
                it.dir("${path.asFilenamePrefix()}-${variant.name}-app-splits")
            }
        )
        task.outputPrivacySandboxCompatAppSplits.set(
            getPrivacySandboxFilesDirectory().map {
                it.dir("${path.asFilenamePrefix()}-${variant.name}-compat-app-splits")
            }
        )
    }

    // For application modules, the instrumentation apk is generated in the module itself
    extensions.findByType(ApplicationAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().withBuildType("debug")) { variant ->
            tasks.named(
                "${COPY_TEST_APKS_TASK}${variant.name}AndroidTest",
                CopyTestApksTask::class.java
            ) { task ->
                task.appFolder.set(variant.artifacts.get(SingleArtifact.APK))
                task.appLoader.set(variant.artifacts.getBuiltArtifactsLoader())

                // The target project is the same being evaluated
                task.outputAppApk.set(outputAppApkFile(variant, path, null))

                if (isPrivacySandboxEnabled()) {
                    addPrivacySandboxApksFor(variant, task)
                }
            }
        }
    }

    // Migrate away when b/280680434 is fixed.
    // For tests modules, the instrumentation apk is pulled from the <variant>TestedApks
    // configuration. Note that also the associated test configuration task name is different
    // from the application one.
    extensions.findByType(TestAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().all()) { variant ->
            tasks.named("${COPY_TEST_APKS_TASK}${variant.name}", CopyTestApksTask::class.java) {
                task ->
                task.appLoader.set(variant.artifacts.getBuiltArtifactsLoader())

                // The target app path is defined in the targetProjectPath field in the android
                // extension of the test module
                val targetProjectPath =
                    project.extensions.getByType(TestExtension::class.java).targetProjectPath
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
            @Suppress("UnstableApiUsage") // Incubating dependencyFactory APIs
            val configuration =
                configurations.create("${variant.name}TestedApks") { config ->
                    config.isCanBeResolved = true
                    config.isCanBeConsumed = false
                    config.attributes {
                        it.attribute(
                            BuildTypeAttr.ATTRIBUTE,
                            objects.named(targetAppProjectVariant)
                        )
                        it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                    }
                    config.dependencies.add(project.dependencyFactory.create(targetAppProject))
                }

            tasks.named(
                "${COPY_TEST_APKS_TASK}${variant.name}AndroidTest",
                CopyTestApksTask::class.java
            ) { task ->
                task.appLoader.set(variant.artifacts.getBuiltArtifactsLoader())

                // The target app path is defined in the androidx extension
                task.outputAppApk.set(outputAppApkFile(variant, targetAppProject.path, path))

                task.appFileCollection.from(
                    configuration.incoming
                        .artifactView { view ->
                            view.attributes { it.attribute(ARTIFACT_TYPE_ATTRIBUTE, "apk") }
                        }
                        .files
                )
            }
        }
    }
}

private fun getOrCreateMediaTestConfigTask(
    project: Project
): TaskProvider<GenerateMediaTestConfigurationTask> {
    val parentProject = project.parent!!
    if (
        !parentProject.tasks
            .withType(GenerateMediaTestConfigurationTask::class.java)
            .names
            .contains("support-media-test${GENERATE_TEST_CONFIGURATION_TASK}")
    ) {
        val task =
            parentProject.tasks.register(
                "support-media-test${GENERATE_TEST_CONFIGURATION_TASK}",
                GenerateMediaTestConfigurationTask::class.java
            ) { task ->
                AffectedModuleDetector.configureTaskGuard(task)
            }
        project.rootProject.tasks.findByName(FINALIZE_TEST_CONFIGS_WITH_APKS_TASK)!!.dependsOn(task)
        return task
    } else {
        return parentProject.tasks
            .withType(GenerateMediaTestConfigurationTask::class.java)
            .named("support-media-test${GENERATE_TEST_CONFIGURATION_TASK}")
    }
}

private fun Project.createOrUpdateMediaTestConfigurationGenerationTask(
    variantName: String,
    artifacts: Artifacts,
    minSdk: Int,
    testRunner: Provider<String>,
    projectIsolationEnabled: Boolean,
) {
    val mediaTask = getOrCreateMediaTestConfigTask(this)

    fun getJsonName(clientToT: Boolean, serviceToT: Boolean, clientTests: Boolean): String {
        return "_mediaClient${
            if (clientToT) "ToT" else "Previous"
        }Service${
            if (serviceToT) "ToT" else "Previous"
        }${
            if (clientTests) "Client" else "Service"
        }Tests$variantName.json"
    }

    fun Project.addTestModule(
        clientToT: Boolean,
        serviceToT: Boolean,
        projectIsolationEnabled: Boolean
    ) {
        // We don't test the combination of previous versions of service and client as that is not
        // useful data. We always want at least one tip of tree project.
        if (!clientToT && !serviceToT) return

        var testName =
            getJsonName(clientToT = clientToT, serviceToT = serviceToT, clientTests = true)
        addToModuleInfo(testName, projectIsolationEnabled)
        extensions.getByType<AndroidXExtension>().testModuleNames.add(testName)

        testName = getJsonName(clientToT = clientToT, serviceToT = serviceToT, clientTests = false)
        addToModuleInfo(testName, projectIsolationEnabled)
        extensions.getByType<AndroidXExtension>().testModuleNames.add(testName)
    }
    val isClient = this.name.contains("client")
    val isPrevious = this.name.contains("previous")

    if (isClient) {
        addTestModule(clientToT = !isPrevious, serviceToT = false, projectIsolationEnabled)
        addTestModule(clientToT = !isPrevious, serviceToT = true, projectIsolationEnabled)
    } else {
        addTestModule(clientToT = true, serviceToT = !isPrevious, projectIsolationEnabled)
        addTestModule(clientToT = false, serviceToT = !isPrevious, projectIsolationEnabled)
    }

    mediaTask.configure {
        if (isClient) {
            if (isPrevious) {
                it.clientPreviousFolder.set(artifacts.get(SingleArtifact.APK))
                it.clientPreviousLoader.set(artifacts.getBuiltArtifactsLoader())
            } else {
                it.clientToTFolder.set(artifacts.get(SingleArtifact.APK))
                it.clientToTLoader.set(artifacts.getBuiltArtifactsLoader())
            }
        } else {
            if (isPrevious) {
                it.servicePreviousFolder.set(artifacts.get(SingleArtifact.APK))
                it.servicePreviousLoader.set(artifacts.getBuiltArtifactsLoader())
            } else {
                it.serviceToTFolder.set(artifacts.get(SingleArtifact.APK))
                it.serviceToTLoader.set(artifacts.getBuiltArtifactsLoader())
            }
        }
        it.jsonClientPreviousServiceToTClientTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = false, serviceToT = true, clientTests = true)
            )
        )
        it.jsonClientPreviousServiceToTServiceTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = false, serviceToT = true, clientTests = false)
            )
        )
        it.jsonClientToTServicePreviousClientTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = true, serviceToT = false, clientTests = true)
            )
        )
        it.jsonClientToTServicePreviousServiceTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = true, serviceToT = false, clientTests = false)
            )
        )
        it.jsonClientToTServiceToTClientTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = true, serviceToT = true, clientTests = true)
            )
        )
        it.jsonClientToTServiceToTServiceTests.set(
            getFileInTestConfigDirectory(
                getJsonName(clientToT = true, serviceToT = true, clientTests = false)
            )
        )
        it.totClientApk.set(getFileInTestConfigDirectory("mediaClientToT$variantName.apk"))
        it.previousClientApk.set(
            getFileInTestConfigDirectory("mediaClientPrevious$variantName.apk")
        )
        it.totServiceApk.set(getFileInTestConfigDirectory("mediaServiceToT$variantName.apk"))
        it.previousServiceApk.set(
            getFileInTestConfigDirectory("mediaServicePrevious$variantName.apk")
        )
        it.minSdk.set(minSdk)
        it.testRunner.set(testRunner)
        it.presubmit.set(isPresubmitBuild())
        AffectedModuleDetector.configureTaskGuard(it)
    }
}

@Suppress("UnstableApiUsage") // HasDeviceTests is @Incubating b/372495504
fun Project.configureTestConfigGeneration(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    projectIsolationEnabled: Boolean,
) {
    extensions.getByType(AndroidComponentsExtension::class.java).apply {
        onVariants { variant ->
            when {
                variant is HasDeviceTests -> {
                    variant.deviceTests.forEach { (_, deviceTest) ->
                        when {
                            path.contains("media:version-compat-tests:") -> {
                                createOrUpdateMediaTestConfigurationGenerationTask(
                                    deviceTest.name,
                                    deviceTest.artifacts,
                                    // replace minSdk after b/328495232 is fixed
                                    commonExtension.defaultConfig.minSdk!!,
                                    deviceTest.instrumentationRunner,
                                    projectIsolationEnabled,
                                )
                            }
                            else -> {
                                createTestConfigurationGenerationTask(
                                    deviceTest.name,
                                    deviceTest.artifacts,
                                    // replace minSdk after b/328495232 is fixed
                                    commonExtension.defaultConfig.minSdk!!,
                                    deviceTest.instrumentationRunner,
                                    deviceTest.instrumentationRunnerArguments,
                                    variant,
                                    projectIsolationEnabled,
                                )
                            }
                        }
                    }
                }
                project.plugins.hasPlugin("com.android.test") -> {
                    createTestConfigurationGenerationTask(
                        variant.name,
                        variant.artifacts,
                        // replace minSdk after b/328495232 is fixed
                        commonExtension.defaultConfig.minSdk!!,
                        provider { commonExtension.defaultConfig.testInstrumentationRunner!! },
                        provider {
                            commonExtension.defaultConfig.testInstrumentationRunnerArguments
                        },
                        variant,
                        projectIsolationEnabled,
                    )
                }
            }
        }
    }
}

@Suppress("UnstableApiUsage") // HasDeviceTests is @Incubating b/372495504
fun Project.configureTestConfigGeneration(
    kotlinMultiplatformAndroidTarget: DeprecatedKotlinMultiplatformAndroidTarget,
    componentsExtension: KotlinMultiplatformAndroidComponentsExtension,
    projectIsolationEnabled: Boolean,
) {
    componentsExtension.onVariant { variant ->
        variant.deviceTests.forEach { (_, deviceTest) ->
            createTestConfigurationGenerationTask(
                deviceTest.name,
                deviceTest.artifacts,
                // replace minSdk after b/328495232 is fixed
                kotlinMultiplatformAndroidTarget.minSdk!!,
                deviceTest.instrumentationRunner,
                deviceTest.instrumentationRunnerArguments,
                null,
                projectIsolationEnabled,
            )
        }
    }
}

private fun Project.getTestSourceSetsForAndroid(variant: Variant?): List<FileCollection> {
    val testSourceFileCollections = mutableListOf<FileCollection>()
    when (variant) {
        is TestVariant -> {
            // com.android.test modules keep test code in main sourceset
            variant.sources.java?.all?.let { sourceSet ->
                testSourceFileCollections.add(files(sourceSet))
            }
            // Add kotlin-android main source set
            extensions
                .findByType(KotlinAndroidProjectExtension::class.java)
                ?.sourceSets
                ?.find { it.name == "main" }
                ?.let { testSourceFileCollections.add(it.kotlin.sourceDirectories) }
            // Note, don't have to add kotlin-multiplatform as it is not compatible with
            // com.android.test modules
        }
        is com.android.build.api.variant.HasAndroidTest -> {
            variant.androidTest?.sources?.java?.all?.let {
                testSourceFileCollections.add(files(it))
            }
        }
    }

    // Add kotlin-android androidTest source set
    extensions
        .findByType(KotlinAndroidProjectExtension::class.java)
        ?.sourceSets
        ?.find { it.name == "androidTest" }
        ?.let { testSourceFileCollections.add(it.kotlin.sourceDirectories) }

    // Add kotlin-multiplatform androidInstrumentedTest target source sets
    multiplatformExtension
        ?.targets
        ?.filterIsInstance<KotlinAndroidTarget>()
        ?.mapNotNull { it.compilations.find { it.name == "releaseAndroidTest" } }
        ?.flatMap { it.allKotlinSourceSets }
        ?.mapTo(testSourceFileCollections) { it.kotlin.sourceDirectories }
    return testSourceFileCollections
}

private fun Project.isPrivacySandboxEnabled(): Boolean =
    extensions.findByType(ApplicationExtension::class.java)?.privacySandbox?.enable ?: false

private const val COPY_TEST_APKS_TASK = "CopyTestApks"
private const val GENERATE_PRIVACY_SANDBOX_MAIN_TEST_CONFIGURATION_TASK =
    "GeneratePrivacySandboxMainTestConfiguration"
private const val GENERATE_PRIVACY_SANDBOX_COMPAT_TEST_CONFIGURATION_TASK =
    "GeneratePrivacySandboxCompatTestConfiguration"
private const val GENERATE_TEST_CONFIGURATION_TASK = "GenerateTestConfiguration"
private const val PRIVACY_SANDBOX_MIN_API_LEVEL = 34
