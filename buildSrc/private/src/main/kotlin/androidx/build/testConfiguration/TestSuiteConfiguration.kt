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
import androidx.build.AndroidXImplPlugin
import androidx.build.AndroidXImplPlugin.Companion.ZIP_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.asFilenamePrefix
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.getSupportRootFolder
import androidx.build.getTestConfigDirectory
import androidx.build.hasAndroidTestSourceCode
import androidx.build.hasBenchmarkPlugin
import androidx.build.isPresubmitBuild
import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.TestAndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.TestExtension
import com.android.build.gradle.internal.attributes.VariantAttr
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType
import java.io.File
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

/**
 * Creates and configures the test config generation task for a project. Configuration includes
 * populating the task with relevant data from the first 4 params, and setting whether the task
 * is enabled.
 */
fun Project.createTestConfigurationGenerationTask(
    variantName: String,
    artifacts: Artifacts,
    minSdk: Int,
    testRunner: String,
) {
    val xmlName = "${path.asFilenamePrefix()}$variantName.xml"
    val jsonName = "_${path.asFilenamePrefix()}$variantName.json"
    rootProject.tasks.named("createModuleInfo").configure {
        it as ModuleInfoGenerator
        it.testModules.add(
            TestModule(
                name = xmlName,
                path = listOf(projectDir.toRelativeString(getSupportRootFolder()))
            )
        )
    }
    val generateTestConfigurationTask = tasks.register(
        "${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}$variantName",
        GenerateTestConfigurationTask::class.java
    ) { task ->
        val androidXExtension = extensions.getByType<AndroidXExtension>()

        task.testFolder.set(artifacts.get(SingleArtifact.APK))
        task.testLoader.set(artifacts.getBuiltArtifactsLoader())
        task.outputTestApk.set(
            File(getTestConfigDirectory(), "${path.asFilenamePrefix()}-$variantName.apk")
        )
        task.additionalApkKeys.set(androidXExtension.additionalDeviceTestApkKeys)
        task.additionalTags.set(androidXExtension.additionalDeviceTestTags)
        task.outputXml.fileValue(File(getTestConfigDirectory(), xmlName))
        task.outputJson.fileValue(File(getTestConfigDirectory(), jsonName))
        task.presubmit.set(isPresubmitBuild())
        // Disable work tests on < API 18: b/178127496
        if (path.startsWith(":work:")) {
            task.minSdk.set(maxOf(18, minSdk))
        } else {
            task.minSdk.set(minSdk)
        }
        val hasBenchmarkPlugin = hasBenchmarkPlugin()
        task.hasBenchmarkPlugin.set(hasBenchmarkPlugin)
        task.testRunner.set(testRunner)
        task.testProjectPath.set(path)
        AffectedModuleDetector.configureTaskGuard(task)
    }
    // Disable xml generation for projects that have no test sources
    // or explicitly don't want to run device tests
    afterEvaluate {
        val androidXExtension = extensions.getByType<AndroidXExtension>()
        generateTestConfigurationTask.configure {
            it.enabled = androidXExtension.deviceTests.enabled && hasAndroidTestSourceCode()
        }
    }
    rootProject.tasks.findByName(ZIP_TEST_CONFIGS_WITH_APKS_TASK)!!
        .dependsOn(generateTestConfigurationTask)
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
    ): File {
        var filename = appProjectPath.asFilenamePrefix()
        if (instrumentationProjectPath != null) {
            filename += "_for_${instrumentationProjectPath.asFilenamePrefix()}"
        }
        filename += "-${variant.name}.apk"
        return File(getTestConfigDirectory(), filename)
    }

    // For application modules, the instrumentation apk is generated in the module itself
    extensions.findByType(ApplicationAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().withBuildType("debug")) { variant ->
            tasks.named(
                "${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}${variant.name}AndroidTest",
                GenerateTestConfigurationTask::class.java
            ) { task ->
                task.appFolder.set(variant.artifacts.get(SingleArtifact.APK))
                task.appLoader.set(variant.artifacts.getBuiltArtifactsLoader())

                // The target project is the same being evaluated
                task.outputAppApk.set(outputAppApkFile(variant, path, null))
            }
        }
    }

    // For tests modules, the instrumentation apk is pulled from the <variant>TestedApks
    // configuration. Note that also the associated test configuration task name is different
    // from the application one.
    extensions.findByType(TestAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().all()) { variant ->
            tasks.named(
                "${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}${variant.name}",
                GenerateTestConfigurationTask::class.java
            ) { task ->
                task.appLoader.set(
                    variant.artifacts.getBuiltArtifactsLoader()
                )

                // The target app path is defined in the targetProjectPath field in the android
                // extension of the test module
                val targetProjectPath = project
                    .extensions
                    .getByType(TestExtension::class.java)
                    .targetProjectPath
                    ?: throw IllegalStateException("""
                        Module `$path` does not have a targetProjectPath defined.
                    """.trimIndent())
                task.outputAppApk.set(
                    outputAppApkFile(variant, targetProjectPath, path)
                )

                task.appFileCollection.from(
                    configurations
                        .named("${variant.name}TestedApks")
                        .get()
                        .incoming
                        .artifactView {
                            it.attributes { container ->
                                container.attribute(
                                    AndroidArtifacts.ARTIFACT_TYPE,
                                    ArtifactType.APK.type
                                )
                            }
                        }
                        .files
                )
            }
        }
    }

    // For library modules we only look at the build type debug. The target app project can be
    // specified through the androidX extension, through: targetAppProjectForInstrumentationTest
    // and targetAppProjectVariantForInstrumentationTest.
    extensions.findByType(LibraryAndroidComponentsExtension::class.java)?.apply {
        onVariants(selector().withBuildType("debug")) { variant ->

            val targetAppProject =
                androidXExtension.deviceTests.targetAppProject ?: return@onVariants
            val targetAppProjectVariant =
                androidXExtension.deviceTests.targetAppVariant

            // Recreate the same configuration existing for test modules to pull the artifact
            // from the application module specified in the deviceTests extension.
            @Suppress("UnstableApiUsage") // Incubating dependencyFactory APIs
            val configuration = configurations.create("${variant.name}TestedApks") { config ->
                config.isCanBeResolved = true
                config.isCanBeConsumed = false
                config.attributes {
                    it.attribute(VariantAttr.ATTRIBUTE, objects.named(targetAppProjectVariant))
                    it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                }
                config
                    .dependencies
                    .add(project.dependencyFactory.create(targetAppProject))
            }

            tasks.named(
                "${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}${variant.name}AndroidTest",
                GenerateTestConfigurationTask::class.java
            ) { task ->
                task.appLoader.set(variant.artifacts.getBuiltArtifactsLoader())

                // The target app path is defined in the androidx extension
                task.outputAppApk.set(
                    outputAppApkFile(variant, targetAppProject.path, path)
                )

                task.appFileCollection.from(
                    configuration.incoming.artifactView { view ->
                        view.attributes {
                            it.attribute(AndroidArtifacts.ARTIFACT_TYPE, ArtifactType.APK.type)
                        }
                    }.files
                )
            }
        }
    }
}

private fun getOrCreateMediaTestConfigTask(project: Project, isMedia2: Boolean):
    TaskProvider<GenerateMediaTestConfigurationTask> {
        val mediaPrefix = getMediaConfigTaskPrefix(isMedia2)
        val parentProject = project.parent!!
        if (!parentProject.tasks.withType(GenerateMediaTestConfigurationTask::class.java)
            .names.contains(
                    "support-$mediaPrefix-test${
                    AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK
                    }"
                )
        ) {
            val task = parentProject.tasks.register(
                "support-$mediaPrefix-test${AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK}",
                GenerateMediaTestConfigurationTask::class.java
            ) { task ->
                AffectedModuleDetector.configureTaskGuard(task)
            }
            project.rootProject.tasks.findByName(ZIP_TEST_CONFIGS_WITH_APKS_TASK)!!
                .dependsOn(task)
            return task
        } else {
            return parentProject.tasks.withType(GenerateMediaTestConfigurationTask::class.java)
                .named(
                    "support-$mediaPrefix-test${
                    AndroidXImplPlugin.GENERATE_TEST_CONFIGURATION_TASK
                    }"
                )
        }
    }

private fun getMediaConfigTaskPrefix(isMedia2: Boolean): String {
    return if (isMedia2) "media2" else "media"
}

fun Project.createOrUpdateMediaTestConfigurationGenerationTask(
    variantName: String,
    artifacts: Artifacts,
    minSdk: Int,
    testRunner: String,
    isMedia2: Boolean
) {
    val mediaPrefix = getMediaConfigTaskPrefix(isMedia2)
    val mediaTask = getOrCreateMediaTestConfigTask(this, isMedia2)
    mediaTask.configure {
        it as GenerateMediaTestConfigurationTask
        if (this.name.contains("client")) {
            if (this.name.contains("previous")) {
                it.clientPreviousFolder.set(artifacts.get(SingleArtifact.APK))
                it.clientPreviousLoader.set(artifacts.getBuiltArtifactsLoader())
            } else {
                it.clientToTFolder.set(artifacts.get(SingleArtifact.APK))
                it.clientToTLoader.set(artifacts.getBuiltArtifactsLoader())
            }
        } else {
            if (this.name.contains("previous")) {
                it.servicePreviousFolder.set(artifacts.get(SingleArtifact.APK))
                it.servicePreviousLoader.set(artifacts.getBuiltArtifactsLoader())
            } else {
                it.serviceToTFolder.set(artifacts.get(SingleArtifact.APK))
                it.serviceToTLoader.set(artifacts.getBuiltArtifactsLoader())
            }
        }
        it.jsonClientPreviousServiceToTClientTests.fileValue(
            File(
                this.getTestConfigDirectory(),
                "_${mediaPrefix}ClientPreviousServiceToTClientTests$variantName.json"
            )
        )
        it.jsonClientPreviousServiceToTServiceTests.fileValue(
            File(
                this.getTestConfigDirectory(),
                "_${mediaPrefix}ClientPreviousServiceToTServiceTests$variantName.json"
            )
        )
        it.jsonClientToTServicePreviousClientTests.fileValue(
            File(
                this.getTestConfigDirectory(),
                "_${mediaPrefix}ClientToTServicePreviousClientTests$variantName.json"
            )
        )
        it.jsonClientToTServicePreviousServiceTests.fileValue(
            File(
                this.getTestConfigDirectory(),
                "_${mediaPrefix}ClientToTServicePreviousServiceTests$variantName.json"
            )
        )
        it.jsonClientToTServiceToTClientTests.fileValue(
            File(
                this.getTestConfigDirectory(),
                "_${mediaPrefix}ClientToTServiceToTClientTests$variantName.json"
            )
        )
        it.jsonClientToTServiceToTServiceTests.fileValue(
            File(
                this.getTestConfigDirectory(),
                "_${mediaPrefix}ClientToTServiceToTServiceTests$variantName.json"
            )
        )
        it.totClientApk.fileValue(
            File(getTestConfigDirectory(), "${mediaPrefix}ClientToT$variantName.apk")
        )
        it.previousClientApk.fileValue(
            File(getTestConfigDirectory(), "${mediaPrefix}ClientPrevious$variantName.apk")
        )
        it.totServiceApk.fileValue(
            File(getTestConfigDirectory(), "${mediaPrefix}ServiceToT$variantName.apk")
        )
        it.previousServiceApk.fileValue(
            File(getTestConfigDirectory(), "${mediaPrefix}ServicePrevious$variantName.apk")
        )
        it.minSdk.set(minSdk)
        it.testRunner.set(testRunner)
        it.presubmit.set(isPresubmitBuild())
        AffectedModuleDetector.configureTaskGuard(it)
    }
}

fun Project.configureTestConfigGeneration(baseExtension: BaseExtension) {
    extensions.getByType(AndroidComponentsExtension::class.java).apply {
        onVariants { variant ->
            var name: String? = null
            var artifacts: Artifacts? = null
            when {
                variant is HasAndroidTest -> {
                    name = variant.androidTest?.name
                    artifacts = variant.androidTest?.artifacts
                }
                project.plugins.hasPlugin("com.android.test") -> {
                    name = variant.name
                    artifacts = variant.artifacts
                }
            }
            if (name == null || artifacts == null) {
                return@onVariants
            }
            when {
                path.contains("media2:media2-session:version-compat-tests:") -> {
                    createOrUpdateMediaTestConfigurationGenerationTask(
                        name,
                        artifacts,
                        baseExtension.defaultConfig.minSdk!!,
                        baseExtension.defaultConfig.testInstrumentationRunner!!,
                        isMedia2 = true
                    )
                }
                path.contains("media:version-compat-tests:") -> {
                    createOrUpdateMediaTestConfigurationGenerationTask(
                        name,
                        artifacts,
                        baseExtension.defaultConfig.minSdk!!,
                        baseExtension.defaultConfig.testInstrumentationRunner!!,
                        isMedia2 = false
                    )
                }
                else -> {
                    createTestConfigurationGenerationTask(
                        name,
                        artifacts,
                        baseExtension.defaultConfig.minSdk!!,
                        baseExtension.defaultConfig.testInstrumentationRunner!!
                    )
                }
            }
        }
    }
}
