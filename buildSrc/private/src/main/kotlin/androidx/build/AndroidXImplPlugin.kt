/*
 * Copyright 2018 The Android Open Source Project
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

import androidx.benchmark.gradle.BenchmarkPlugin
import androidx.build.AndroidXImplPlugin.Companion.TASK_TIMEOUT_MINUTES
import androidx.build.Release.DEFAULT_PUBLISH_CONFIG
import androidx.build.buildInfo.addCreateLibraryBuildInfoFileTasks
import androidx.build.checkapi.AndroidMultiplatformApiTaskConfig
import androidx.build.checkapi.JavaApiTaskConfig
import androidx.build.checkapi.KmpApiTaskConfig
import androidx.build.checkapi.LibraryApiTaskConfig
import androidx.build.checkapi.configureProjectForApiTasks
import androidx.build.docs.CheckTipOfTreeDocsTask.Companion.setUpCheckDocsTask
import androidx.build.gradle.isRoot
import androidx.build.license.configureExternalDependencyLicenseCheck
import androidx.build.resources.CopyPublicResourcesDirTask
import androidx.build.resources.configurePublicResourcesStub
import androidx.build.sbom.configureSbomPublishing
import androidx.build.sbom.validateAllArchiveInputsRecognized
import androidx.build.studio.StudioTask
import androidx.build.testConfiguration.ModuleInfoGenerator
import androidx.build.testConfiguration.TestModule
import androidx.build.testConfiguration.addAppApkToTestConfigGeneration
import androidx.build.testConfiguration.configureTestConfigGeneration
import androidx.build.uptodatedness.TaskUpToDateValidator
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.android.build.api.artifact.Artifacts
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.KotlinMultiplatformAndroidTarget
import com.android.build.api.dsl.KotlinMultiplatformAndroidTestOnDeviceCompilation
import com.android.build.api.dsl.KotlinMultiplatformAndroidTestOnJvmCompilation
import com.android.build.api.dsl.PrivacySandboxSdkExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasDeviceTests
import com.android.build.api.variant.HasUnitTestBuilder
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.Variant
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestExtension
import com.android.build.gradle.TestPlugin
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.KotlinMultiplatformAndroidPlugin
import com.android.build.gradle.api.PrivacySandboxSdkPlugin
import com.android.build.gradle.tasks.factory.AndroidUnitTest
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.JavaVersion.VERSION_11
import org.gradle.api.JavaVersion.VERSION_17
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.Configuration
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.KotlinClosure1
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withModule
import org.gradle.kotlin.dsl.withType
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin
import org.gradle.plugin.devel.tasks.ValidatePlugins
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * A plugin which enables all of the Gradle customizations for AndroidX. This plugin reacts to other
 * plugins being added and adds required and optional functionality.
 */
abstract class AndroidXImplPlugin
@Inject
constructor(private val componentFactory: SoftwareComponentFactory) : Plugin<Project> {
    @get:javax.inject.Inject abstract val registry: BuildEventsListenerRegistry

    override fun apply(project: Project) {
        if (project.isRoot)
            throw Exception("Root project should use AndroidXRootImplPlugin instead")
        val androidXExtension =
            project.extensions.create<AndroidXExtension>(EXTENSION_NAME, project)

        val androidXKmpExtension =
            project.extensions.create<AndroidXMultiplatformExtension>(
                AndroidXMultiplatformExtension.EXTENSION_NAME,
                project
            )

        project.tasks.register(BUILD_ON_SERVER_TASK, DefaultTask::class.java)
        // Perform different actions based on which plugins have been applied to the project.
        // Many of the actions overlap, ex. API tracking.
        project.plugins.all { plugin ->
            @Suppress("UnstableApiUsage") // PrivacySandboxSdkPlugin, KMPAndroidPlugin
            when (plugin) {
                is JavaGradlePluginPlugin -> configureGradlePluginPlugin(project)
                is JavaPlugin -> configureWithJavaPlugin(project, androidXExtension)
                is LibraryPlugin -> configureWithLibraryPlugin(project, androidXExtension)
                is AppPlugin -> configureWithAppPlugin(project, androidXExtension)
                is TestPlugin -> configureWithTestPlugin(project, androidXExtension)
                is KotlinMultiplatformAndroidPlugin ->
                    configureWithKotlinMultiplatformAndroidPlugin(
                        project,
                        androidXKmpExtension.agpKmpExtension,
                        androidXExtension
                    )
                is KotlinBasePluginWrapper -> configureWithKotlinPlugin(
                    project,
                    androidXExtension,
                    plugin,
                    androidXKmpExtension
                )
                is PrivacySandboxSdkPlugin -> configureWithPrivacySandboxSdkPlugin(project)
            }
        }

        project.configureLint()
        project.configureKtlint()
        project.configureKotlinVersion()

        // Avoid conflicts between full Guava and LF-only Guava.
        project.configureGuavaUpgradeHandler()

        // Configure all Jar-packing tasks for hermetic builds.
        project.tasks.withType(Zip::class.java).configureEach { it.configureForHermeticBuild() }
        project.tasks.withType(Copy::class.java).configureEach { it.configureForHermeticBuild() }

        val allHostTests = project.tasks.register("allHostTests").get()
        // copy host side test results to DIST
        project.tasks.withType(AbstractTestTask::class.java) { task ->
            configureTestTask(project, task, allHostTests)
        }
        project.tasks.withType(Test::class.java) { task -> configureJvmTestTask(project, task) }

        project.configureTaskTimeouts()
        project.configureMavenArtifactUpload(
            androidXExtension, androidXKmpExtension, componentFactory) {
            project.addCreateLibraryBuildInfoFileTasks(androidXExtension, androidXKmpExtension)
        }
        project.publishInspectionArtifacts()
        project.configureExternalDependencyLicenseCheck()
        project.configureProjectStructureValidation(androidXExtension)
        project.configureProjectVersionValidation(androidXExtension)
        project.registerProjectOrArtifact()
        project.validateMultiplatformPluginHasNotBeenApplied()

        project.tasks.register("printCoordinates", PrintProjectCoordinatesTask::class.java) {
            it.configureWithAndroidXExtension(androidXExtension)
        }
        project.configureConstraintsWithinGroup(androidXExtension)
        project.validateProjectParser(androidXExtension)
        project.validateAllArchiveInputsRecognized()
        project.afterEvaluate {
            if (androidXExtension.shouldPublishSbom()) {
                project.configureSbomPublishing()
            }
            if (androidXExtension.shouldPublish()) {
                project.validatePublishedMultiplatformHasDefault()
            }
            project.registerValidateMultiplatformSourceSetNamingTask()
        }
        project.disallowAccidentalAndroidDependenciesInKmpProject(androidXKmpExtension)
        TaskUpToDateValidator.setup(project, registry)

        project.workaroundPrebuiltTakingPrecedenceOverProject()
    }

    private fun Project.registerProjectOrArtifact() {
        // Add a method for each sub project where they can declare an optional
        // dependency on a project or its latest snapshot artifact.
        if (!ProjectLayoutType.isPlayground(this)) {
            // In AndroidX build, this is always enforced to the project
            extra.set(
                PROJECT_OR_ARTIFACT_EXT_NAME,
                KotlinClosure1<String, Project>(
                    function = {
                        // this refers to the first parameter of the closure.
                        project.resolveProject(this)
                    }
                )
            )
        } else {
            // In Playground builds, they are converted to the latest SNAPSHOT artifact if the
            // project is not included in that playground.
            extra.set(
                PROJECT_OR_ARTIFACT_EXT_NAME,
                KotlinClosure1<String, Any>(
                    function = {
                        AndroidXPlaygroundRootImplPlugin.projectOrArtifact(rootProject, this)
                    }
                )
            )
        }
    }

    /**
     * Disables timestamps and ensures filesystem-independent archive ordering to maximize
     * cross-machine byte-for-byte reproducibility of artifacts.
     */
    private fun Zip.configureForHermeticBuild() {
        isReproducibleFileOrder = true
        isPreserveFileTimestamps = false
    }

    private fun Copy.configureForHermeticBuild() {
        duplicatesStrategy = DuplicatesStrategy.FAIL
    }

    private fun configureJvmTestTask(project: Project, task: Test) {
        // Robolectric 1.7 increased heap size requirements, see b/207169653.
        task.maxHeapSize = "3g"

        // For non-playground setup use robolectric offline
        if (!ProjectLayoutType.isPlayground(project)) {
            task.systemProperty("robolectric.offline", "true")
            val robolectricDependencies =
                File(
                    project.getPrebuiltsRoot(),
                    "androidx/external/org/robolectric/android-all-instrumented"
                )
            task.systemProperty(
                "robolectric.dependency.dir",
                robolectricDependencies.relativeTo(project.projectDir)
            )
        }
    }

    private fun configureTestTask(
        project: Project,
        task: AbstractTestTask,
        anchorTask: Task,
    ) {
        anchorTask.dependsOn(task)
        val ignoreFailuresProperty =
            project.providers.gradleProperty(TEST_FAILURES_DO_NOT_FAIL_TEST_TASK)
        val ignoreFailures = ignoreFailuresProperty.isPresent
        if (ignoreFailures) {
            task.ignoreFailures = true
        }
        task.inputs.property("ignoreFailures", ignoreFailures)

        val xmlReportDestDir = project.getHostTestResultDirectory()
        val testName = "${project.path}:${task.name}"
        project.rootProject.tasks.named("createModuleInfo").configure {
            it as ModuleInfoGenerator
            it.testModules.add(
                TestModule(
                    name = testName,
                    path =
                        listOf(project.projectDir.toRelativeString(project.getSupportRootFolder()))
                )
            )
        }
        val archiveName = "$testName.zip"
        if (project.isDisplayTestOutput()) {
            // Enable tracing to see results in command line
            task.testLogging.apply {
                events =
                    hashSetOf(
                        TestLogEvent.FAILED,
                        TestLogEvent.PASSED,
                        TestLogEvent.SKIPPED,
                        TestLogEvent.STANDARD_OUT
                    )
                showExceptions = true
                showCauses = true
                showStackTraces = true
                exceptionFormat = TestExceptionFormat.FULL
            }
        } else {
            task.testLogging.apply {
                showExceptions = false
                // Disable all output, including the names of the failing tests, by specifying
                // that the minimum granularity we're interested in is this very high number
                // (which is higher than the current maximum granularity that Gradle offers (3))
                minGranularity = 1000
            }
            val testTaskName = task.name
            val capitalizedTestTaskName =
                testTaskName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
            val xmlReport = task.reports.junitXml
            if (xmlReport.required.get()) {
                val zipXmlTask =
                    project.tasks.register(
                        "zipXmlResultsOf$capitalizedTestTaskName",
                        Zip::class.java
                    ) {
                        it.destinationDirectory.set(xmlReportDestDir)
                        it.archiveFileName.set(archiveName)
                        it.from(project.file(xmlReport.outputLocation))
                        it.include("*.xml")
                    }
                task.finalizedBy(zipXmlTask)
            }
        }
    }

    /** Configures the project to use the Kotlin version specified by `androidx.kotlinTarget`. */
    private fun Project.configureKotlinVersion() {
        val kotlinVersionStringProvider = androidXConfiguration.kotlinBomVersion
        val kotlinTestVersionStringProvider = androidXConfiguration.kotlinTestBomVersion

        // Resolve unspecified Kotlin versions to the target version.
        configurations.all { configuration ->
            val useVersionStringProvider = if (configuration.isTest()) {
                kotlinTestVersionStringProvider
            } else {
                kotlinVersionStringProvider
            }
            configuration.resolutionStrategy { strategy ->
                strategy.eachDependency { details ->
                    if (details.requested.group == "org.jetbrains.kotlin" &&
                        details.requested.version == null) {
                        details.useVersion(useVersionStringProvider.get())
                    }
                }
            }
        }

        fun Provider<String>.toKotlinVersionProvider() = map { version ->
            KotlinVersion.fromVersion(version.substringBeforeLast('.'))
        }

        fun KotlinCompilationTask<*>.isTestCompilation() =
            multiplatformExtension?.targets?.any { target ->
                target.compilations.findByName("test")?.compileKotlinTaskName == name
            } ?: false

        // Set the Kotlin compiler's API and language version to ensure bytecode is compatible.
        val kotlinVersionProvider = kotlinVersionStringProvider.toKotlinVersionProvider()
        val kotlinTestVersionProvider = kotlinTestVersionStringProvider.toKotlinVersionProvider()
        tasks.configureEach { task ->
            if (task is KotlinCompilationTask<*>) {
                // We can't directly determine if a Task is compiling test code, but we can scrape
                // the names of all the compilation units and compare them to Task names.
                val useVersionProvider = if (task.isTestCompilation()) {
                    kotlinTestVersionProvider
                } else {
                    kotlinVersionProvider
                }
                task.compilerOptions.apiVersion.set(useVersionProvider)
                task.compilerOptions.languageVersion.set(useVersionProvider)
            }
        }

        // Specify coreLibrariesVersion for consumption by Kotlin Gradle Plugin. Note that KGP does
        // not explicitly support varying the version between tasks/configurations for a given
        // project, so this is not strictly correct. Picking the non-test (e.g. lower) value seems
        // to work, though.
        afterEvaluate { evaluatedProject ->
            evaluatedProject.kotlinExtensionOrNull?.let { kotlinExtension ->
                kotlinExtension.coreLibrariesVersion = kotlinVersionStringProvider.get()
            }
        }

        // Resolve classpath conflicts caused by kotlin-stdlib-jdk7 and -jdk8 artifacts by amending
        // the kotlin-stdlib artifact metadata to add same-version constraints.
        project.dependencies {
            components { componentMetadata ->
                componentMetadata.withModule<KotlinStdlibDependenciesRule>(
                    "org.jetbrains.kotlin:kotlin-stdlib"
                )
            }
        }
    }

    @CacheableRule
    internal abstract class KotlinStdlibDependenciesRule : ComponentMetadataRule {
        override fun execute(context: ComponentMetadataContext) {
            val module = context.details.id
            val version = module.version
            context.details.allVariants { variantMetadata ->
                variantMetadata.withDependencyConstraints { constraintsMetadata ->
                    val reason = "${module.name} is in atomic group ${module.group}"
                    constraintsMetadata.add("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$version") {
                        it.because(reason)
                    }
                    constraintsMetadata.add("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version") {
                        it.because(reason)
                    }
                }
            }
        }
    }

    private fun configureWithKotlinPlugin(
        project: Project,
        androidXExtension: AndroidXExtension,
        plugin: KotlinBasePluginWrapper,
        androidXMultiplatformExtension: AndroidXMultiplatformExtension
    ) {
        project.configureKtfmt()

        project.afterEvaluate {
            val targetsAndroid =
                project.plugins.hasPlugin(LibraryPlugin::class.java) ||
                    project.plugins.hasPlugin(AppPlugin::class.java) ||
                    project.plugins.hasPlugin(TestPlugin::class.java) ||
                    @Suppress("UnstableApiUsage")
                    project.plugins.hasPlugin(KotlinMultiplatformAndroidPlugin::class.java)
            val defaultJavaTargetVersion =
                getDefaultTargetJavaVersion(androidXExtension.type).toString()
            if (plugin is KotlinMultiplatformPluginWrapper) {
                project.extensions.getByType<KotlinMultiplatformExtension>().apply {
                    targets.withType<KotlinAndroidTarget> {
                        compilations.configureEach {
                            it.kotlinOptions.jvmTarget = defaultJavaTargetVersion
                        }
                    }
                    targets.withType<KotlinJvmTarget> {
                        val defaultJavaTargetVersionForNonAndroidTargets =
                            getDefaultTargetJavaVersion(
                                libraryType = androidXExtension.type,
                                projectName = project.name,
                                targetName = name
                            ).toString()
                        compilations.configureEach {
                            it.compileJavaTaskProvider?.configure { javaCompile ->
                                javaCompile.targetCompatibility =
                                    defaultJavaTargetVersionForNonAndroidTargets
                                javaCompile.sourceCompatibility =
                                    defaultJavaTargetVersionForNonAndroidTargets
                            }
                            it.kotlinOptions {
                                jvmTarget = defaultJavaTargetVersionForNonAndroidTargets
                                // Set jdk-release version for non-Android KMP targets
                                freeCompilerArgs += listOf(
                                    "-Xjdk-release=$defaultJavaTargetVersionForNonAndroidTargets",
                                )
                            }
                        }
                    }
                }
            } else {
                project.tasks.withType(KotlinJvmCompile::class.java).configureEach { task ->
                    task.kotlinOptions.jvmTarget = defaultJavaTargetVersion
                    if (!targetsAndroid) {
                        // Set jdk-release version for non-Android JVM projects
                        task.kotlinOptions.freeCompilerArgs += listOf(
                            "-Xjdk-release=$defaultJavaTargetVersion",
                        )
                    }
                }
            }
            project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
                val kotlinCompilerArgs =
                    mutableListOf(
                        "-Xskip-metadata-version-check",
                    )
                // TODO (b/259578592): enable -Xjvm-default=all for camera-camera2-pipe projects
                if (!project.name.contains("camera-camera2-pipe")) {
                    kotlinCompilerArgs += "-Xjvm-default=all"
                }
                if (androidXExtension.type == LibraryType.PUBLISHED_KOTLIN_ONLY_LIBRARY ||
                    androidXExtension.type == LibraryType.PUBLISHED_KOTLIN_ONLY_TEST_LIBRARY) {
                    // The Kotlin Compiler adds intrinsic assertions which are only relevant
                    // when the code is consumed by Java users. Therefore we can turn this off
                    // when code is being consumed by Kotlin users.

                    // Additional Context:
                    // https://github.com/JetBrains/kotlin/blob/master/compiler/cli/cli-common/src/org/jetbrains/kotlin/cli/common/arguments/K2JVMCompilerArguments.kt#L239
                    // b/280633711
                    kotlinCompilerArgs +=
                        listOf(
                            "-Xno-param-assertions",
                            "-Xno-call-assertions",
                            "-Xno-receiver-assertions"
                        )
                }
                task.kotlinOptions.freeCompilerArgs += kotlinCompilerArgs
                logScriptSources(task, project)
            }

            // Explicit API mode is broken for Android projects
            // https://youtrack.jetbrains.com/issue/KT-37652
            if (androidXExtension.shouldEnforceKotlinStrictApiMode() && !targetsAndroid) {
                project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
                    // Workaround for https://youtrack.jetbrains.com/issue/KT-37652
                    if (task.name.endsWith("TestKotlin")) return@configureEach
                    if (task.name.endsWith("TestKotlinJvm")) return@configureEach
                    task.kotlinOptions.freeCompilerArgs += listOf("-Xexplicit-api=strict")
                }
            }
        }
        if (plugin is KotlinMultiplatformPluginWrapper) {
            KonanPrebuiltsSetup.configureKonanDirectory(project)
            project.afterEvaluate {
                val libraryExtension = project.extensions.findByType<LibraryExtension>()
                if (libraryExtension != null) {
                    libraryExtension.configureAndroidLibraryWithMultiplatformPluginOptions()
                } else if (!androidXMultiplatformExtension.hasAndroidMultiplatform()) {
                    // Kotlin MPP does not apply java plugin anymore, but we still want to configure
                    // all java-related tasks.
                    // We only need to do this when project does not have Android plugin, which already
                    // configures Java tasks.
                    configureWithJavaPlugin(project, androidXExtension)
                }
            }
            project.configureKmp()
            project.configureSourceJarForMultiplatform()

            // Disable any source JAR task(s) added by KotlinMultiplatformPlugin.
            // https://youtrack.jetbrains.com/issue/KT-55881
            project.tasks.withType(Jar::class.java).configureEach { jarTask ->
                if (jarTask.name == "jvmSourcesJar") {
                    // We can't set duplicatesStrategy directly on the Jar task since it will get
                    // overridden when the KotlinMultiplatformPlugin creates child specs, but we
                    // can set it on a per-file basis.
                    jarTask.eachFile { fileCopyDetails ->
                        fileCopyDetails.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    }
                }
            }
        }
    }

    private fun configureWithAppPlugin(project: Project, androidXExtension: AndroidXExtension) {
        project.extensions.getByType<AppExtension>().apply {
            configureAndroidBaseOptions(project, androidXExtension)
            excludeVersionFiles(packagingOptions.resources)
        }

        project.extensions.getByType<ApplicationExtension>().apply {
            configureAndroidApplicationOptions(project, androidXExtension)
        }

        project.extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
            beforeVariants(selector().withBuildType("release")) { variant ->
                // Cast is needed because ApplicationAndroidComponentsExtension implements both
                // HasUnitTestBuilder and VariantBuilder, and VariantBuilder#enableUnitTest is
                // deprecated in favor of HasUnitTestBuilder#enableUnitTest.
                // Remove the cast when we upgrade to AGP 9.0.0
                (variant as HasUnitTestBuilder).enableUnitTest = false
            }
            onVariants {
                it.configureTests()
                it.configureLocalAsbSigning(project.getKeystore())
            }
        }

        project.buildOnServerDependsOnAssembleRelease()
        project.buildOnServerDependsOnLint()
    }

    private fun configureWithTestPlugin(project: Project, androidXExtension: AndroidXExtension) {
        project.extensions.getByType<TestExtension>().apply {
            configureAndroidBaseOptions(project, androidXExtension)
            excludeVersionFiles(packagingOptions.resources)
        }

        project.extensions.getByType<com.android.build.api.dsl.TestExtension>().apply {
            project.configureTestConfigGeneration(this)
            project.addAppApkToTestConfigGeneration(androidXExtension)
        }

        project.configureJavaCompilationWarnings(androidXExtension)

        project.addToProjectMap(androidXExtension)
    }

    private fun configureWithKotlinMultiplatformAndroidPlugin(
        project: Project,
        kotlinMultiplatformAndroidTarget: KotlinMultiplatformAndroidTarget,
        androidXExtension: AndroidXExtension
    ) {
        val kotlinMultiplatformAndroidComponentsExtension = project
            .extensions
            .getByType<KotlinMultiplatformAndroidComponentsExtension>()
        kotlinMultiplatformAndroidTarget.configureAndroidBaseOptions(
            project,
            kotlinMultiplatformAndroidComponentsExtension
        )
        kotlinMultiplatformAndroidTarget.configureAndroidLibraryOptions(
            project,
            androidXExtension
        )

        project.configureProjectForApiTasks(
            AndroidMultiplatformApiTaskConfig,
            androidXExtension
        )

        kotlinMultiplatformAndroidComponentsExtension.onVariant { variant ->
            project.createVariantAarManifestTransformerTask(variant.name, variant.artifacts)
        }

        kotlinMultiplatformAndroidComponentsExtension.onVariant {
            it.configureTests()
        }

        project.configurePublicResourcesStub(project.multiplatformExtension!!)
        project.configureMultiplatformSourcesForAndroid { action ->
            kotlinMultiplatformAndroidComponentsExtension.onVariant {
                action(it.name)
            }
        }
        project.configureVersionFileWriter(
            project.multiplatformExtension!!,
            androidXExtension
        )
        project.configureJavaCompilationWarnings(androidXExtension)

        project.configureDependencyVerification(androidXExtension) { taskProvider ->
            kotlinMultiplatformAndroidTarget.compilations.configureEach {
                taskProvider.configure { task ->
                    task.dependsOn(it.compileTaskProvider)
                }
            }
        }

        val reportLibraryMetrics = project.configureReportLibraryMetricsTask()
        project.addToBuildOnServer(reportLibraryMetrics)

        project.addToProjectMap(androidXExtension)
        project.afterEvaluate {
            project.addToBuildOnServer("assembleAndroidMain")
            project.addToBuildOnServer("lint")
        }
        project.setUpCheckDocsTask(androidXExtension)
    }

    @Suppress("UnstableApiUsage") // usage of PrivacySandboxSdkExtension
    private fun configureWithPrivacySandboxSdkPlugin(project: Project) {
        project.extensions.getByType<PrivacySandboxSdkExtension>().apply {
            configureLocalAsbSigning(experimentalProperties, project.getKeystore())
        }
    }

    private fun configureLocalAsbSigning(
        experimentalProperties: MutableMap<String, Any>,
        keyStore: File
    ) {
        experimentalProperties[ASB_SIGNING_CONFIG_PROPERTY_NAME] = keyStore.absolutePath
    }

    private val ASB_SIGNING_CONFIG_PROPERTY_NAME =
        "android.privacy_sandbox.local_deployment_signing_store_file"

    /**
     * Temporary diagnostics for b/321949384
     */
    private fun logScriptSources(task: KotlinCompile, project: Project) {
        if (getBuildId() == "0")
            return // don't need to log when not running on the build server
        val logFile = File(project.getDistributionDirectory(), "KotlinCompile-scriptSources.log")
        fun writeScriptSources(label: String) {
            val now = LocalDateTime.now()
            @Suppress("INVISIBLE_MEMBER")
            val scriptSources = task.scriptSources.files
            logFile.appendText(
                "${task.path} $label at $now with ${scriptSources.size} scriptSources: " +
                "${scriptSources.joinToString()}\n"
            )
        }
        task.doFirst {
            writeScriptSources("starting")
        }
        task.doLast {
            writeScriptSources("completed")
        }
    }

    /**
     * Excludes files telling which versions of androidx libraries were used in test apks, to avoid
     * invalidating caches as often
     */
    private fun excludeVersionFiles(packaging: com.android.build.api.variant.ResourcesPackaging) {
        packaging.excludes.add("/META-INF/androidx*.version")
    }

    /**
     * Excludes files telling which versions of androidx libraries were used in test apks, to avoid
     * invalidating caches as often
     */
    private fun excludeVersionFiles(packaging: com.android.build.api.dsl.ResourcesPackaging) {
        packaging.excludes.add("/META-INF/androidx*.version")
    }

    private fun Project.buildOnServerDependsOnAssembleRelease() {
        project.addToBuildOnServer("assembleRelease")
    }

    private fun Project.buildOnServerDependsOnLint() {
        if (!project.usingMaxDepVersions()) {
            val androidComponents = extensions.findByType(AndroidComponentsExtension::class.java)
            androidComponents?.onVariants { variant ->
                if (!variant.name.lowercase(Locale.getDefault()).contains("release")) {
                    val taskName =
                        "lint${variant.name.replaceFirstChar {
                            if (it.isLowerCase()) {
                                it.titlecase(Locale.getDefault())
                            } else {
                                it.toString()
                            }
                        }}"
                    project.addToBuildOnServer(taskName)
                }
            }
        }
    }

    @Suppress("UnstableApiUsage") // usage of HasDeviceTests
    private fun HasDeviceTests.configureTests() {
        deviceTests.forEach { deviceTest ->
            deviceTest.packaging.resources.apply {
                excludeVersionFiles(this)

                // Workaround a limitation in AGP that fails to merge these META-INF license files.
                pickFirsts.add("/META-INF/AL2.0")
                // In addition to working around the above issue, we exclude the LGPL2.1 license as
                // we're
                // approved to distribute code via AL2.0 and the only dependencies which pull in LGPL2.1
                // are currently dual-licensed with AL2.0 and LGPL2.1. The affected dependencies are:
                //   - net.java.dev.jna:jna:5.5.0
                excludes.add("/META-INF/LGPL2.1")
            }
        }
    }

    /**
     * Enable long-running method tracing on the UI thread, even if that risks ANR for profiling
     * convenience.
     *
     * See [androidx.build.testConfiguration.INST_ARG_BLOCKLIST], which is used to suppress the arg
     * in CI.
     */
    @Suppress("UnstableApiUsage") // usage of HasDeviceTests
    private fun HasDeviceTests.enableLongMethodTracingInMicrobenchmark(project: Project) {
        if (project.hasBenchmarkPlugin()) {
            deviceTests.forEach { deviceTest ->
                deviceTest.instrumentationRunnerArguments.put(
                    "androidx.benchmark.profiling.skipWhenDurationRisksAnr", "false"
                )
            }
        }
    }

    private fun Variant.aotCompileMicrobenchmarks(project: Project) {
        if (project.hasBenchmarkPlugin()) {
            @Suppress("UnstableApiUsage") // usage of experimentalProperties
            experimentalProperties.put("android.experimental.force-aot-compilation", true)
        }
    }

    @Suppress("UnstableApiUsage") // usage of experimentalProperties
    private fun Variant.configureLocalAsbSigning(keyStore: File) {
        experimentalProperties.put(ASB_SIGNING_CONFIG_PROPERTY_NAME, keyStore.absolutePath)
    }

    @Suppress("DEPRECATION") // AGP DSL APIs
    private fun configureWithLibraryPlugin(project: Project, androidXExtension: AndroidXExtension) {
        val libraryExtension =
            project.extensions.getByType<LibraryExtension>().apply {
                configureAndroidBaseOptions(project, androidXExtension)
                configureAndroidLibraryOptions(project, androidXExtension)

                // Make sure the main Kotlin source set doesn't contain anything under
                // src/main/kotlin.
                val mainKotlinSrcDir =
                    (sourceSets.findByName("main")?.kotlin
                            as com.android.build.gradle.api.AndroidSourceDirectorySet)
                        .srcDirs
                        .filter { it.name == "kotlin" }
                        .getOrNull(0)
                if (mainKotlinSrcDir?.isDirectory == true) {
                    throw GradleException(
                        "Invalid project structure! AndroidX does not support \"kotlin\" as a " +
                            "top-level source directory for libraries, use \"java\" instead: " +
                            mainKotlinSrcDir.path
                    )
                }
            }

        val libraryAndroidComponentsExtension =
            project.extensions.getByType<LibraryAndroidComponentsExtension>()

        // Remove the android:targetSdkVersion element from the manifest used for AARs.
        libraryAndroidComponentsExtension.onVariants { variant ->
            project.createVariantAarManifestTransformerTask(variant.name, variant.artifacts)
        }

        project.extensions.getByType<com.android.build.api.dsl.LibraryExtension>().apply {
            publishing { singleVariant(DEFAULT_PUBLISH_CONFIG) }
            project.configureTestConfigGeneration(this)
            project.addAppApkToTestConfigGeneration(androidXExtension)
        }

        libraryAndroidComponentsExtension.apply {
            beforeVariants(selector().withBuildType("release")) { variant ->
                variant.enableUnitTest = false
            }
            onVariants {
                it.configureTests()
                it.aotCompileMicrobenchmarks(project)
                it.enableLongMethodTracingInMicrobenchmark(project)
            }
        }

        project.configureSourceJarForAndroid(libraryExtension)
        project.configureVersionFileWriter(libraryAndroidComponentsExtension, androidXExtension)
        project.configureJavaCompilationWarnings(androidXExtension)

        project.configureDependencyVerification(androidXExtension) { taskProvider ->
            libraryExtension.defaultPublishVariant { libraryVariant ->
                taskProvider.configure { task ->
                    task.dependsOn(libraryVariant.javaCompileProvider)
                }
            }
        }

        val reportLibraryMetrics = project.configureReportLibraryMetricsTask()
        project.addToBuildOnServer(reportLibraryMetrics)
        libraryExtension.defaultPublishVariant { libraryVariant ->
            reportLibraryMetrics.configure {
                it.jarFiles.from(
                    libraryVariant.packageLibraryProvider.map { zip ->
                        zip.inputs.files
                    }
                )
            }
        }

        val prebuiltLibraries = listOf("libtracing_perfetto.so", "libc++_shared.so")
        val copyPublicResourcesDirTask =
            project.tasks.register(
                "generatePublicResourcesStub",
                CopyPublicResourcesDirTask::class.java
            ) { task ->
                task.buildSrcResDir.set(File(project.getSupportRootFolder(), "buildSrc/res"))
            }
        libraryAndroidComponentsExtension.onVariants { variant ->
            if (variant.buildType == DEFAULT_PUBLISH_CONFIG) {
                // Standard docs, resource API, and Metalava configuration for AndroidX projects.
                project.configureProjectForApiTasks(
                    LibraryApiTaskConfig(variant),
                    androidXExtension
                )
            }
            configurePublicResourcesStub(variant, copyPublicResourcesDirTask)
            val verifyELFRegionAlignmentTaskProvider = project.tasks.register(
                variant.name + "VerifyELFRegionAlignment",
                VerifyELFRegionAlignmentTask::class.java
            ) { task ->
                task.files.from(
                    variant.artifacts.get(SingleArtifact.MERGED_NATIVE_LIBS)
                        .map { dir ->
                            dir.asFileTree.files
                                .filter { it.extension == "so" }
                                .filter { it.path.contains("arm64-v8a") }
                                .filterNot { prebuiltLibraries.contains(it.name) }
                        }
                )
                task.cacheEvenIfNoOutputs()
            }
            project.addToBuildOnServer(verifyELFRegionAlignmentTaskProvider)
        }

        project.setUpCheckDocsTask(androidXExtension)

        project.addToProjectMap(androidXExtension)

        project.buildOnServerDependsOnAssembleRelease()
        project.buildOnServerDependsOnLint()
    }

    private fun configureGradlePluginPlugin(project: Project) {
        project.tasks.withType(ValidatePlugins::class.java).configureEach {
            it.enableStricterValidation.set(true)
            it.failOnWarning.set(true)
        }
        SdkResourceGenerator.generateForHostTest(project)
    }

    private fun getDefaultTargetJavaVersion(
        libraryType: LibraryType,
        projectName: String? = null,
        targetName: String? = null
    ): JavaVersion {
        return when {
            projectName != null && projectName.contains("desktop") -> VERSION_17
            targetName != null && targetName == "desktop" -> VERSION_17
            libraryType == LibraryType.COMPILER_PLUGIN -> VERSION_11
            libraryType.compilationTarget == CompilationTarget.HOST -> VERSION_17
            else -> VERSION_1_8
        }
    }

    private fun configureWithJavaPlugin(project: Project, androidXExtension: AndroidXExtension) {
        project.configureErrorProneForJava()

        // Force Java 1.8 source- and target-compatibility for all Java libraries.
        val javaExtension = project.extensions.getByType<JavaPluginExtension>()
        project.afterEvaluate {
            javaExtension.apply {
                val defaultTargetJavaVersion = getDefaultTargetJavaVersion(androidXExtension.type)
                sourceCompatibility = defaultTargetJavaVersion
                targetCompatibility = defaultTargetJavaVersion
                project.disableJava8TargetObsoleteWarnings()
            }
            if (!project.plugins.hasPlugin(KotlinBasePluginWrapper::class.java)) {
                project.configureSourceJarForJava()
            }
        }

        project.configureJavaCompilationWarnings(androidXExtension)

        project.hideJavadocTask()

        project.configureDependencyVerification(androidXExtension) { taskProvider ->
            taskProvider.configure { task ->
                task.dependsOn(project.tasks.named(JavaPlugin.COMPILE_JAVA_TASK_NAME))
            }
        }

        val apiTaskConfig =
            if (project.multiplatformExtension != null) {
                KmpApiTaskConfig
            } else {
                JavaApiTaskConfig
            }

        project.configureProjectForApiTasks(apiTaskConfig, androidXExtension)
        project.setUpCheckDocsTask(androidXExtension)

        project.afterEvaluate {
            if (androidXExtension.shouldRelease()) {
                project.extra.set("publish", true)
            }
        }

        // Workaround for b/120487939 wherein Gradle's default resolution strategy prefers external
        // modules with lower versions over local projects with higher versions.
        project.configurations.all { configuration ->
            configuration.resolutionStrategy.preferProjectModules()
        }

        if (project.multiplatformExtension == null) {
            project.addToBuildOnServer("jar")
        } else {
            val multiplatformExtension = project.multiplatformExtension!!
            multiplatformExtension.targets.forEach {
                if (it.platformType == KotlinPlatformType.jvm) {
                    val task = project.tasks.named(it.artifactsTaskName, Jar::class.java)
                    project.addToBuildOnServer(task)
                }
            }
        }

        project.addToProjectMap(androidXExtension)
    }

    private fun Project.configureProjectStructureValidation(androidXExtension: AndroidXExtension) {
        // AndroidXExtension.mavenGroup is not readable until afterEvaluate.
        afterEvaluate {
            val mavenGroup = androidXExtension.mavenGroup
            val isProbablyPublished =
                androidXExtension.type == LibraryType.PUBLISHED_LIBRARY ||
                    androidXExtension.type == LibraryType.PUBLISHED_KOTLIN_ONLY_LIBRARY ||
                    androidXExtension.type == LibraryType.UNSET
            if (mavenGroup != null && isProbablyPublished && androidXExtension.shouldPublish()) {
                validateProjectMavenGroup(mavenGroup.group)
                validateProjectMavenName(androidXExtension.name.get(), mavenGroup.group)
                validateProjectStructure(mavenGroup.group)
            }
        }
    }

    private fun Project.configureProjectVersionValidation(androidXExtension: AndroidXExtension) {
        // AndroidXExtension.mavenGroup is not readable until afterEvaluate.
        afterEvaluate { androidXExtension.validateMavenVersion() }
    }

    private fun BaseExtension.configureAndroidBaseOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        compileOptions.apply {
            sourceCompatibility = VERSION_1_8
            targetCompatibility = VERSION_1_8
        }
        project.disableJava8TargetObsoleteWarnings()

        val defaultMinSdkVersion = project.defaultAndroidConfig.minSdk
        val defaultCompileSdkVersion = project.defaultAndroidConfig.compileSdk

        // Suppress output of android:compileSdkVersion and related attributes (b/277836549).
        aaptOptions.additionalParameters += "--no-compile-sdk-metadata"

        // Specify default values. The client may attempt to override these in their build.gradle,
        // so we'll need to perform validation in afterEvaluate().
        compileSdkVersion(defaultCompileSdkVersion)
        buildToolsVersion = project.defaultAndroidConfig.buildToolsVersion
        ndkVersion = project.defaultAndroidConfig.ndkVersion

        defaultConfig.minSdk = defaultMinSdkVersion
        defaultConfig.targetSdk = project.defaultAndroidConfig.targetSdk
        defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        testOptions.animationsDisabled = !project.isMacrobenchmark()
        testOptions.unitTests.isReturnDefaultValues = true
        testOptions.unitTests.all { task ->
            task.configureForRobolectric()
        }

        // Include resources in Robolectric tests as a workaround for b/184641296
        testOptions.unitTests.isIncludeAndroidResources = true

        project.afterEvaluate {
            val minSdkVersion = defaultConfig.minSdk!!
            check(minSdkVersion >= defaultMinSdkVersion) {
                "minSdkVersion $minSdkVersion lower than the default of $defaultMinSdkVersion"
            }
            check(
                compileSdkVersion == defaultCompileSdkVersion || project.isCustomCompileSdkAllowed()
            ) {
                "compileSdkVersion must not be explicitly specified, was \"$compileSdkVersion\""
            }
           project.enforceBanOnVersionRanges()

            if (androidXExtension.type.compilationTarget != CompilationTarget.DEVICE) {
                throw IllegalStateException(
                    "${androidXExtension.type.name} libraries cannot apply the android plugin, as" +
                        " they do not target android devices"
                )
            }
        }

        val debugSigningConfig = signingConfigs.getByName("debug")
        // Use a local debug keystore to avoid build server issues.
        debugSigningConfig.storeFile = project.getKeystore()
        buildTypes.all { buildType ->
            // Sign all the builds (including release) with debug key
            buildType.signingConfig = debugSigningConfig
        }

        project.configureErrorProneForAndroid()

        // workaround for b/120487939
        project.configurations.all { configuration ->
            // Gradle seems to crash on androidtest configurations
            // preferring project modules...
            if (!configuration.name.lowercase(Locale.US).contains("androidtest")) {
                configuration.resolutionStrategy.preferProjectModules()
            }
        }

        project.configureFtlRunner(
            project.extensions.getByType(AndroidComponentsExtension::class.java)
        )

        // AGP warns if we use project.buildDir (or subdirs) for CMake's generated
        // build files (ninja build files, CMakeCache.txt, etc.). Use a staging directory that
        // lives alongside the project's buildDir.
        @Suppress("DEPRECATION")
        externalNativeBuild.cmake.buildStagingDirectory =
            File(project.buildDir, "../nativeBuildStaging")

        // Align the ELF region of native shared libs 16kb boundary
        defaultConfig.externalNativeBuild.cmake.arguments.add(
            "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
        )
    }

    private fun KotlinMultiplatformAndroidTarget.configureAndroidBaseOptions(
        project: Project,
        componentsExtension: KotlinMultiplatformAndroidComponentsExtension
    ) {
        val defaultMinSdkVersion = project.defaultAndroidConfig.minSdk
        val defaultCompileSdkVersion = project.defaultAndroidConfig.compileSdkInt()

        compileSdk = defaultCompileSdkVersion
        buildToolsVersion = project.defaultAndroidConfig.buildToolsVersion

        minSdk = defaultMinSdkVersion

        lint.targetSdk = project.defaultAndroidConfig.targetSdk
        compilations.withType(
            KotlinMultiplatformAndroidTestOnDeviceCompilation::class.java
        ).configureEach {
            it.instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            it.animationsDisabled = true
        }
        compilations.withType(
            KotlinMultiplatformAndroidTestOnJvmCompilation::class.java
        ).configureEach {
            it.isReturnDefaultValues = true
            // Include resources in Robolectric tests as a workaround for b/184641296
            it.isIncludeAndroidResources = true
        }

        project.tasks.withType(AndroidUnitTest::class.java).configureEach { task ->
            task.configureForRobolectric()
        }

        // validate that SDK versions haven't been altered during evaluation
        project.afterEvaluate {
            val minSdkVersion = minSdk!!
            check(minSdkVersion >= defaultMinSdkVersion) {
                "minSdkVersion $minSdkVersion lower than the default of $defaultMinSdkVersion"
            }
            check(
                compileSdk == defaultCompileSdkVersion || project.isCustomCompileSdkAllowed()
            ) {
                "compileSdkVersion must not be explicitly specified, was \"$compileSdk\""
            }
            project.enforceBanOnVersionRanges()
        }

        project.configureTestConfigGeneration(
            this,
            componentsExtension
        )
        project.configureFtlRunner(componentsExtension)
    }

    @Suppress("UnstableApiUsage") // finalizeDsl, minCompileSdkExtension
    private fun LibraryExtension.configureAndroidLibraryOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        // Propagate the compileSdk value into minCompileSdk. Don't propagate compileSdkExtension,
        // since only one library actually depends on the extension APIs and they can explicitly
        // declare that in their build.gradle. Note that when we're using a preview SDK, the value
        // for compileSdk will be null and the resulting AAR metadata won't have a minCompileSdk --
        // this is okay because AGP automatically embeds forceCompileSdkPreview in the AAR metadata
        // and uses it instead of minCompileSdk.
        project.extensions.findByType<LibraryAndroidComponentsExtension>()!!.finalizeDsl {
            it.defaultConfig.aarMetadata.minCompileSdk = it.compileSdk
        }
        project.disableStrictVersionConstraints()
        project.setPublishProperty(androidXExtension)
    }

    private fun KotlinMultiplatformAndroidTarget.configureAndroidLibraryOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        // Propagate the compileSdk value into minCompileSdk.
        aarMetadata.minCompileSdk = compileSdk
        project.disableStrictVersionConstraints()
        project.setPublishProperty(androidXExtension)
    }

    /**
     * Adds a module handler replacement rule that treats full Guava (of any version) as an upgrade
     * to ListenableFuture-only Guava. This prevents irreconcilable versioning conflicts and/or
     * class duplication issues.
     */
    private fun Project.configureGuavaUpgradeHandler() {
        // The full Guava artifact is very large, so they split off a special artifact containing a
        // standalone version of the commonly-used ListenableFuture interface. However, they also
        // structured the artifacts in a way that causes dependency resolution conflicts:
        // - `com.google.guava:listenablefuture:1.0` contains only ListenableFuture
        // - `com.google.guava:listenablefuture:9999.0` contains nothing
        // - `com.google.guava:guava` contains all of Guava, including ListenableFuture
        // If a transitive dependency includes `guava` as implementation-type and we have a direct
        // API-type dependency on `listenablefuture:1.0`, then we'll get `listenablefuture:9999.0`
        // on the compilation classpath -- which does not have the ListenableFuture class. However,
        // if we tell Gradle to upgrade all LF dependencies to Guava then we'll get `guava` as an
        // API-type dependency. See b/274621238 for more details.
        project.dependencies {
            modules { moduleHandler ->
                moduleHandler.module("com.google.guava:listenablefuture") { module ->
                    module.replacedBy("com.google.guava:guava")
                }
            }
        }
    }

    private fun Project.disableStrictVersionConstraints() {
        // Gradle inserts strict version constraints to ensure that dependency versions are
        // identical across main and test source sets. For normal projects, this ensures
        // that test bytecode is binary- and behavior-compatible with the main source set's
        // bytecode. For AndroidX, though, we require backward compatibility and therefore
        // don't need to enforce such constraints.
        project.configurations.all { configuration ->
            if (!configuration.isTest()) return@all

            configuration.dependencyConstraints.configureEach { dependencyConstraint ->
                val strictVersion = dependencyConstraint.versionConstraint.strictVersion
                if (strictVersion != "") {
                    // Migrate strict-type version constraints to required-type to allow upgrades.
                    dependencyConstraint.version { versionConstraint ->
                        versionConstraint.strictly("")
                        versionConstraint.require(strictVersion)
                    }
                }
            }
        }
    }

    private fun Project.setPublishProperty(androidXExtension: AndroidXExtension) {
        afterEvaluate {
            if (androidXExtension.shouldRelease()) {
                project.extra.set("publish", true)
            }
        }
    }

    private fun TestedExtension.configureAndroidLibraryWithMultiplatformPluginOptions() {
        sourceSets.findByName("main")!!.manifest.srcFile("src/androidMain/AndroidManifest.xml")
        sourceSets
            .findByName("androidTest")!!
            .manifest
            .srcFile("src/androidInstrumentedTest/AndroidManifest.xml")
    }

    private fun Project.configureKmp() {
        val kmpExtension =
            checkNotNull(project.extensions.findByType<KotlinMultiplatformExtension>()) {
                """
            Project ${project.path} applies kotlin multiplatform plugin but we cannot find the
            KotlinMultiplatformExtension.
            """
                    .trimIndent()
            }

        // Configure all KMP targets to allow expect/actual classes that are not stable.
        // (see https://youtrack.jetbrains.com/issue/KT-61573)
        kmpExtension.targets.all { kotlinTarget ->
            kotlinTarget.compilations.all {
                it.compilerOptions.options.freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    private fun ApplicationExtension.configureAndroidApplicationOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        defaultConfig.apply {
            versionCode = 1
            versionName = "1.0"
        }

        project.configureTestConfigGeneration(this)
        project.addAppApkToTestConfigGeneration(androidXExtension)
        project.addAppApkToFtlRunner()
    }

    private fun Project.configureDependencyVerification(
        androidXExtension: AndroidXExtension,
        taskConfigurator: (TaskProvider<VerifyDependencyVersionsTask>) -> Unit
    ) {
        afterEvaluate {
            if (androidXExtension.type !in listOf(LibraryType.UNSET, LibraryType.SAMPLES)) {
                val verifyDependencyVersionsTask = project.createVerifyDependencyVersionsTask()
                if (verifyDependencyVersionsTask != null) {
                    taskConfigurator(verifyDependencyVersionsTask)
                }
            }
        }
    }

    // If this project wants other project in the same group to have the same version,
    // this function configures those constraints.
    private fun Project.configureConstraintsWithinGroup(androidXExtension: AndroidXExtension) {
        if (!project.shouldAddGroupConstraints().get()) {
            return
        }
        project.afterEvaluate {
            // make sure that the project has a group
            val projectGroup = androidXExtension.mavenGroup ?: return@afterEvaluate
            // make sure that this group is configured to use a single version
            projectGroup.atomicGroupVersion ?: return@afterEvaluate

            // Under certain circumstances, a project is allowed to override its
            // version see ( isGroupVersionOverrideAllowed ), in which case it's
            // not participating in the versioning policy yet,
            // and we don't assign it any version constraints
            if (androidXExtension.mavenVersion != null) {
                return@afterEvaluate
            }

            // We don't want to emit the same constraint into our .module file more than once,
            // and we don't want to try to apply a constraint to a configuration that doesn't accept
            // them,
            // so we create a configuration to hold the constraints and make each other constraint
            // extend it
            val constraintConfiguration = project.configurations.create("groupConstraints")
            project.configurations.configureEach { configuration ->
                if (configuration != constraintConfiguration)
                    configuration.extendsFrom(constraintConfiguration)
            }

            val otherProjectsInSameGroup = androidXExtension.getOtherProjectsInSameGroup()
            val constraints = project.dependencies.constraints
            val allProjectsExist = buildContainsAllStandardProjects()
            for (otherProject in otherProjectsInSameGroup) {
                val otherGradlePath = otherProject.gradlePath
                if (otherGradlePath == ":compose:ui:ui-android-stubs") {
                    // exemption for library that doesn't truly get published: b/168127161
                    continue
                }
                // We only enable constraints for builds that we intend to be able to publish from.
                //   If a project isn't included in a build we intend to be able to publish from,
                //   the project isn't going to be published.
                // Sometimes this can happen when a project subset is enabled:
                //   The KMP project subset enabled by androidx_multiplatform_mac.sh contains
                //   :benchmark:benchmark-common but not :benchmark:benchmark-benchmark
                //   This is ok because we don't intend to publish that artifact from that build
                val otherProjectShouldExist =
                    allProjectsExist || findProject(otherGradlePath) != null
                if (!otherProjectShouldExist) {
                    continue
                }
                // We only emit constraints referring to projects that will release
                val otherFilepath = File(otherProject.filePath, "build.gradle")
                val parsed = parseBuildFile(otherFilepath)
                if (!parsed.shouldRelease()) {
                    continue
                }
                if (parsed.libraryType == LibraryType.SAMPLES) {
                    // a SAMPLES project knows how to publish, but we don't intend to actually
                    // publish it
                    continue
                }
                // Under certain circumstances, a project is allowed to override its
                // version see ( isGroupVersionOverrideAllowed ), in which case it's
                // not participating in the versioning policy yet and we don't emit
                // version constraints referencing it
                if (parsed.specifiesVersion) {
                    continue
                }
                val dependencyConstraint = project(otherGradlePath)
                constraints.add(constraintConfiguration.name, dependencyConstraint) {
                    it.because("${project.name} is in atomic group ${projectGroup.group}")
                }
            }

            // disallow duplicate constraints
            project.configurations.all { config ->
                // Allow duplicate constraints in test configurations. This is partially a
                // workaround for duplication due to downgrading strict-type dependencies to
                // required-type, but also we don't care if tests have duplicate constraints.
                if (config.isTest()) return@all

                // find all constraints contributed by this Configuration and its ancestors
                val configurationConstraints: MutableSet<String> = mutableSetOf()
                config.hierarchy.forEach { parentConfig ->
                    parentConfig.dependencyConstraints.configureEach { dependencyConstraint ->
                        dependencyConstraint.apply {
                            if (
                                versionConstraint.requiredVersion != "" &&
                                    versionConstraint.requiredVersion != "unspecified"
                            ) {
                                val key =
                                    "${dependencyConstraint.group}:${dependencyConstraint.name}"
                                if (configurationConstraints.contains(key)) {
                                    throw GradleException(
                                        "Constraint on $key was added multiple times in " +
                                            "$config (version = " +
                                            "${versionConstraint.requiredVersion}).\n\n" +
                                            "This is unnecessary and can also trigger " +
                                            "https://github.com/gradle/gradle/issues/24037 in " +
                                            "builds trying to use the resulting artifacts."
                                    )
                                }
                                configurationConstraints.add(key)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Tells whether this build contains the usual set of all projects (`./gradlew projects`)
     * Sometimes developers request to include fewer projects because this may run more quickly
     */
    private fun Project.buildContainsAllStandardProjects(): Boolean {
        if (getProjectSubset() != null) return false
        if (ProjectLayoutType.isPlayground(this)) return false
        return true
    }

    private fun Project.createVariantAarManifestTransformerTask(
        variantName: String,
        artifacts: Artifacts
    ) {
        // Remove the android:targetSdkVersion element from the manifest used for AARs.
        tasks
            .register(
                variantName + "AarManifestTransformer",
                AarManifestTransformerTask::class.java
            )
            .let { taskProvider ->
                artifacts
                    .use(taskProvider)
                    .wiredWithFiles(
                        AarManifestTransformerTask::aarFile,
                        AarManifestTransformerTask::updatedAarFile
                    )
                    .toTransform(SingleArtifact.AAR)
            }
    }

    companion object {
        const val CREATE_LIBRARY_BUILD_INFO_FILES_TASK = "createLibraryBuildInfoFiles"
        const val GENERATE_TEST_CONFIGURATION_TASK = "GenerateTestConfiguration"
        const val FINALIZE_TEST_CONFIGS_WITH_APKS_TASK = "finalizeTestConfigsWithApks"
        const val ZIP_TEST_CONFIGS_WITH_APKS_TASK = "zipTestConfigsWithApks"

        const val TASK_GROUP_API = "API"

        const val EXTENSION_NAME = "androidx"

        /** Fail the build if a non-Studio task runs longer than expected */
        const val TASK_TIMEOUT_MINUTES = 60L
    }
}

private const val PROJECTS_MAP_KEY = "projects"
private const val ACCESSED_PROJECTS_MAP_KEY = "accessedProjectsMap"

/** Returns whether the configuration is used for testing. */
private fun Configuration.isTest(): Boolean = name.lowercase().contains("test")

/**
 * Hides a project's Javadoc tasks from the output of `./gradlew tasks` by setting their group to
 * `null`.
 *
 * AndroidX projects do not use the Javadoc task for docs generation, so we don't want them
 * cluttering up the task overview.
 */
private fun Project.hideJavadocTask() {
    tasks.withType(Javadoc::class.java).configureEach {
        if (it.name == "javadoc") {
            it.group = null
        }
    }
}

private fun Project.addToProjectMap(androidXExtension: AndroidXExtension) {
    // TODO(alanv): Move this out of afterEvaluate
    afterEvaluate {
        if (androidXExtension.shouldRelease()) {
            val group = androidXExtension.mavenGroup?.group
            if (group != null) {
                val module = "$group:$name"

                if (project.rootProject.extra.has(ACCESSED_PROJECTS_MAP_KEY)) {
                    throw GradleException(
                        "Attempted to add $project to project map after " +
                            "the contents of the map were accessed"
                    )
                }
                @Suppress("UNCHECKED_CAST")
                val projectModules =
                    project.rootProject.extra.get(PROJECTS_MAP_KEY)
                        as ConcurrentHashMap<String, String>
                projectModules[module] = path
            }
        }
    }
}

val Project.androidExtension: AndroidComponentsExtension<*, *, *>
    get() =
        extensions.findByType<LibraryAndroidComponentsExtension>()
            ?: extensions.findByType<ApplicationAndroidComponentsExtension>()
            ?: throw IllegalArgumentException("Failed to find any registered Android extension")

val Project.multiplatformExtension
    get() = extensions.findByType(KotlinMultiplatformExtension::class.java)

val Project.kotlinExtensionOrNull: KotlinProjectExtension?
    get() = extensions.findByType()

val Project.androidXExtension: AndroidXExtension
    get() = extensions.getByType()

@Suppress("UNCHECKED_CAST")
fun Project.getProjectsMap(): ConcurrentHashMap<String, String> {
    project.rootProject.extra.set(ACCESSED_PROJECTS_MAP_KEY, true)
    return rootProject.extra.get(PROJECTS_MAP_KEY) as ConcurrentHashMap<String, String>
}

/**
 * Configures all non-Studio tasks in a project (see b/153193718 for background) to time out after
 * [TASK_TIMEOUT_MINUTES].
 */
internal fun Project.configureTaskTimeouts() {
    tasks.configureEach { t ->
        // skip adding a timeout for some tasks that both take a long time and
        // that we can count on the user to monitor
        if (t !is StudioTask) {
            t.timeout.set(Duration.ofMinutes(TASK_TIMEOUT_MINUTES))
        }
    }
}

private fun Project.disableJava8TargetObsoleteWarnings() {
    afterEvaluate {
        project.tasks.withType(JavaCompile::class.java).configureEach { task ->
            // JDK 21 considers Java 8 an obsolete source and target value. Disable this warning.
            task.options.compilerArgs.add("-Xlint:-options")
        }
    }
}

private fun Project.configureJavaCompilationWarnings(androidXExtension: AndroidXExtension) {
    afterEvaluate {
        project.tasks.withType(JavaCompile::class.java).configureEach { task ->
            // If we're running a hypothetical test build confirming that tip-of-tree versions
            // are compatible, then we're not concerned about warnings
            if (!project.usingMaxDepVersions()) {
                task.options.compilerArgs.add("-Xlint:unchecked")
                if (androidXExtension.failOnDeprecationWarnings) {
                    task.options.compilerArgs.add("-Xlint:deprecation")
                }
            }
        }
    }
}

fun Project.hasBenchmarkPlugin(): Boolean {
    return this.plugins.hasPlugin(BenchmarkPlugin::class.java)
}

fun Project.isMacrobenchmark(): Boolean {
    return this.path.endsWith("macrobenchmark")
}

/**
 * Returns a string that is a valid filename and loosely based on the project name The value
 * returned for each project will be distinct
 */
fun String.asFilenamePrefix(): String {
    return this.substring(1).replace(':', '-')
}

/**
 * Sets the specified [task] as a dependency of the top-level `check` task, ensuring that it runs as
 * part of `./gradlew check`.
 */
fun <T : Task> Project.addToCheckTask(task: TaskProvider<T>) {
    project.tasks.named("check").configure { it.dependsOn(task) }
}

fun Project.validateMultiplatformPluginHasNotBeenApplied() {
    if (plugins.hasPlugin(KotlinMultiplatformPluginWrapper::class.java)) {
        throw GradleException(
            "The Kotlin multiplatform plugin should only be applied by the AndroidX plugin."
        )
    }
}

/** Verifies we don't accidentially write "implementation" instead of "commonMainImplementation" */
fun Project.disallowAccidentalAndroidDependenciesInKmpProject(
    androidXKmpExtension: AndroidXMultiplatformExtension
) {
    project.afterEvaluate {
        if (androidXKmpExtension.supportedPlatforms.isNotEmpty()) {
            val androidConfiguration = project.configurations.findByName("implementation")
            if (androidConfiguration != null) {
                if (
                    androidConfiguration.dependencies.isNotEmpty() ||
                        androidConfiguration.dependencyConstraints.isNotEmpty()
                ) {
                    throw GradleException(
                        "The 'implementation' Configuration should not be used in a " +
                            "multiplatform project: this Configuration is declared by the " +
                            "Android plugin rather than the kmp plugin. Did you mean " +
                            "'commonMainImplementation'?"
                    )
                }
            }
        }
    }
}

/** Verifies that ProjectParser computes the correct values for this project */
fun Project.validateProjectParser(androidXExtension: AndroidXExtension) {
    // If configuration fails, we don't want to validate the ProjectParser
    // (otherwise it could report a confusing, unnecessary error)
    project.gradle.taskGraph.whenReady {
        if (!androidXExtension.runProjectParser) return@whenReady

        val parsed = project.parse()
        val errorPrefix = "ProjectParser error parsing ${project.path}."
        check(androidXExtension.type == parsed.libraryType) {
            "$errorPrefix Incorrectly computed libraryType = ${parsed.libraryType} " +
                "instead of ${androidXExtension.type}"
        }
        check(androidXExtension.publish == parsed.publish) {
            "$errorPrefix Incorrectly computed publish = ${parsed.publish} " +
                "instead of ${androidXExtension.publish}"
        }
        check(androidXExtension.shouldPublish() == parsed.shouldPublish()) {
            "$errorPrefix Incorrectly computed shouldPublish() = ${parsed.shouldPublish()} " +
                "instead of ${androidXExtension.shouldPublish()}"
        }
        check(androidXExtension.shouldRelease() == parsed.shouldRelease()) {
            "$errorPrefix Incorrectly computed shouldRelease() = ${parsed.shouldRelease()} " +
                "instead of ${androidXExtension.shouldRelease()}"
        }
        check(androidXExtension.projectDirectlySpecifiesMavenVersion == parsed.specifiesVersion) {
            "$errorPrefix Incorrectly computed specifiesVersion = ${parsed.specifiesVersion} " +
                " instead of ${androidXExtension.projectDirectlySpecifiesMavenVersion}"
        }
    }
}

/** Validates the Maven version against Jetpack guidelines. */
fun AndroidXExtension.validateMavenVersion() {
    val mavenGroup = mavenGroup
    val mavenVersion = mavenVersion
    val forcedVersion = mavenGroup?.atomicGroupVersion
    if (forcedVersion != null && forcedVersion == mavenVersion) {
        throw GradleException(
            """
            Unnecessary override of same-group library version

            Project version is already set to $forcedVersion by same-version group
            ${mavenGroup.group}.

            To fix this error, remove "mavenVersion = ..." from your build.gradle
            configuration.
            """
                .trimIndent()
        )
    }
}

/** Workaround for https://github.com/gradle/gradle/issues/27407 */
fun Project.workaroundPrebuiltTakingPrecedenceOverProject() {
    project.configurations.configureEach { configuration ->
        configuration.resolutionStrategy.preferProjectModules()
    }
}

private fun AndroidConfig.compileSdkInt() = compileSdk.removePrefix("android-").toInt()

private fun Test.configureForRobolectric() {
    // https://github.com/robolectric/robolectric/issues/7456
    jvmArgs =
        listOf(
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
            "--add-opens=java.base/java.util=ALL-UNNAMED",
            "--add-opens=java.base/java.io=ALL-UNNAMED",
        )
    // Robolectric 1.7 increased heap size requirements, see b/207169653.
    maxHeapSize = "3g"
}

private fun Project.enforceBanOnVersionRanges() {
    configurations.all { configuration ->
        configuration.resolutionStrategy.eachDependency { dep ->
            val target = dep.target
            val version = target.version
            // Enforce the ban on declaring dependencies with version ranges.
            // Note: In playground, this ban is exempted to allow unresolvable prebuilts
            // to automatically get bumped to snapshot versions via version range
            // substitution.
            if (
                version != null &&
                Version.isDependencyRange(version) &&
                project.rootProject.rootDir == project.getSupportRootFolder()
            ) {
                throw IllegalArgumentException(
                    "Dependency ${dep.target} declares its version as " +
                        "version range ${dep.target.version} however the use of " +
                        "version ranges is not allowed, please update the " +
                        "dependency to list a fixed version."
                )
            }
        }
    }
}

internal fun Project.hasAndroidMultiplatformPlugin(): Boolean =
    extensions.findByType(AndroidXMultiplatformExtension::class.java)?.hasAndroidMultiplatform()
        ?: false

internal fun String.camelCase() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

const val PROJECT_OR_ARTIFACT_EXT_NAME = "projectOrArtifact"
