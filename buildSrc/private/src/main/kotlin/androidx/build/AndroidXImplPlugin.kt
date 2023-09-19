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
import androidx.build.checkapi.JavaApiTaskConfig
import androidx.build.checkapi.KmpApiTaskConfig
import androidx.build.checkapi.LibraryApiTaskConfig
import androidx.build.checkapi.configureProjectForApiTasks
import androidx.build.gradle.isRoot
import androidx.build.license.configureExternalDependencyLicenseCheck
import androidx.build.resources.configurePublicResourcesStub
import androidx.build.sbom.configureSbomPublishing
import androidx.build.sbom.validateAllArchiveInputsRecognized
import androidx.build.studio.StudioTask
import androidx.build.testConfiguration.ModuleInfoGenerator
import androidx.build.testConfiguration.TestModule
import androidx.build.testConfiguration.addAppApkToTestConfigGeneration
import androidx.build.testConfiguration.configureTestConfigGeneration
import androidx.build.uptodatedness.TaskUpToDateValidator
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.HasAndroidTest
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
import java.io.File
import java.time.Duration
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

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
        val extension = project.extensions.create<AndroidXExtension>(EXTENSION_NAME, project)

        val kmpExtension =
            project.extensions.create<AndroidXMultiplatformExtension>(
                AndroidXMultiplatformExtension.EXTENSION_NAME,
                project
            )

        project.tasks.register(BUILD_ON_SERVER_TASK, DefaultTask::class.java)
        // Perform different actions based on which plugins have been applied to the project.
        // Many of the actions overlap, ex. API tracking.
        project.plugins.all { plugin ->
            when (plugin) {
                is JavaPlugin -> configureWithJavaPlugin(project, extension)
                is LibraryPlugin -> configureWithLibraryPlugin(project, extension)
                is AppPlugin -> configureWithAppPlugin(project, extension)
                is TestPlugin -> configureWithTestPlugin(project, extension)
                is KotlinBasePluginWrapper -> configureWithKotlinPlugin(project, extension, plugin)
            }
        }

        project.configureLint()
        project.configureKtlint()
        project.configureKotlinVersion()

        // Configure all Jar-packing tasks for hermetic builds.
        project.tasks.withType(Zip::class.java).configureEach { it.configureForHermeticBuild() }
        project.tasks.withType(Copy::class.java).configureEach { it.configureForHermeticBuild() }

        // copy host side test results to DIST
        project.tasks.withType(AbstractTestTask::class.java) { task ->
            configureTestTask(project, task)
        }
        project.tasks.withType(Test::class.java) { task -> configureJvmTestTask(project, task) }

        project.configureTaskTimeouts()
        project.configureMavenArtifactUpload(extension, kmpExtension, componentFactory)
        project.configureExternalDependencyLicenseCheck()
        project.configureProjectStructureValidation(extension)
        project.configureProjectVersionValidation(extension)
        project.registerProjectOrArtifact()
        project.addCreateLibraryBuildInfoFileTasks(extension)

        project.configurations.create("samples")
        project.validateMultiplatformPluginHasNotBeenApplied()

        project.tasks.register("printCoordinates", PrintProjectCoordinatesTask::class.java) {
            it.configureWithAndroidXExtension(extension)
        }
        project.configureConstraintsWithinGroup(extension)
        project.validateProjectParser(extension)
        project.validateAllArchiveInputsRecognized()
        project.afterEvaluate {
            if (extension.shouldPublishSbom()) {
                project.configureSbomPublishing()
            }
            if (extension.shouldPublish()) {
                project.validatePublishedMultiplatformHasDefault()
            }
        }
        project.disallowAccidentalAndroidDependenciesInKmpProject(kmpExtension)
        TaskUpToDateValidator.setup(project, registry)
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

    private fun configureTestTask(project: Project, task: AbstractTestTask) {
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
                    }
                task.finalizedBy(zipXmlTask)
            }
        }
    }

    /** Configures the project to use the Kotlin version specified by `androidx.kotlinTarget`. */
    private fun Project.configureKotlinVersion() {
        val kotlinVersionStringProvider = androidXConfiguration.kotlinBomVersion

        // Resolve unspecified Kotlin versions to the target version.
        configurations.all { configuration ->
            configuration.resolutionStrategy { strategy ->
                strategy.eachDependency { details ->
                    if (details.requested.group == "org.jetbrains.kotlin") {
                        if (
                            details.requested.group == "org.jetbrains.kotlin" &&
                                details.requested.version == null
                        ) {
                            details.useVersion(kotlinVersionStringProvider.get())
                        }
                    }
                }
            }
        }

        // Set the Kotlin compiler's API and language version to ensure bytecode is compatible.
        val kotlinVersionProvider =
            kotlinVersionStringProvider.map { version ->
                KotlinVersion.fromVersion(version.substringBeforeLast('.'))
            }
        tasks.configureEach { task ->
            (task as? KotlinCompilationTask<*>)?.apply {
                compilerOptions.apiVersion.set(kotlinVersionProvider)
                compilerOptions.languageVersion.set(kotlinVersionProvider)
            }
        }

        // Specify coreLibrariesVersion for consumption by Kotlin Gradle Plugin.
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
        extension: AndroidXExtension,
        plugin: KotlinBasePluginWrapper
    ) {
        project.configureKtfmt()

        project.afterEvaluate {
            project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
                if (extension.type == LibraryType.COMPILER_PLUGIN) {
                    task.kotlinOptions.jvmTarget = "11"
                } else if (
                    extension.type.compilationTarget == CompilationTarget.HOST &&
                        extension.type != LibraryType.ANNOTATION_PROCESSOR_UTILS
                ) {
                    task.kotlinOptions.jvmTarget = "17"
                } else {
                    task.kotlinOptions.jvmTarget = "1.8"
                }
                val kotlinCompilerArgs =
                    mutableListOf(
                        "-Xskip-metadata-version-check",
                    )
                // TODO (b/259578592): enable -Xjvm-default=all for camera-camera2-pipe projects
                if (!project.name.contains("camera-camera2-pipe")) {
                    kotlinCompilerArgs += "-Xjvm-default=all"
                }
                if (!extension.targetsJavaConsumers) {
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
            }

            val isAndroidProject =
                project.plugins.hasPlugin(LibraryPlugin::class.java) ||
                    project.plugins.hasPlugin(AppPlugin::class.java)
            // Explicit API mode is broken for Android projects
            // https://youtrack.jetbrains.com/issue/KT-37652
            if (extension.shouldEnforceKotlinStrictApiMode() && !isAndroidProject) {
                project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
                    // Workaround for https://youtrack.jetbrains.com/issue/KT-37652
                    if (task.name.endsWith("TestKotlin")) return@configureEach
                    if (task.name.endsWith("TestKotlinJvm")) return@configureEach
                    task.kotlinOptions.freeCompilerArgs += listOf("-Xexplicit-api=strict")
                }
            }
        }
        if (plugin is KotlinMultiplatformPluginWrapper) {
            project.configureKonanDirectory()
            project.extensions.findByType<LibraryExtension>()?.apply {
                configureAndroidLibraryWithMultiplatformPluginOptions()
            }
            project.configureKmpTests()
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
            configureAndroidApplicationOptions(project, androidXExtension)
        }

        project.extensions.getByType<ApplicationAndroidComponentsExtension>().apply {
            onVariants {
                it.configureTests()
                it.artRewritingWorkaround()
            }
        }

        project.buildOnServerDependsOnAssembleRelease()
        project.buildOnServerDependsOnLint()
    }

    private fun configureWithTestPlugin(project: Project, androidXExtension: AndroidXExtension) {
        project.extensions.getByType<TestExtension>().apply {
            configureAndroidBaseOptions(project, androidXExtension)
            project.addAppApkToTestConfigGeneration(androidXExtension)
        }

        project.configureJavaCompilationWarnings(androidXExtension)

        project.addToProjectMap(androidXExtension)
    }

    private fun Project.buildOnServerDependsOnAssembleRelease() {
        project.addToBuildOnServer("assembleRelease")
    }

    private fun Project.buildOnServerDependsOnLint() {
        if (!project.usingMaxDepVersions()) {
            project.agpVariants.all { variant ->
                // in AndroidX, release and debug variants are essentially the same,
                // so we don't run the lintRelease task on the build server
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

    private fun HasAndroidTest.configureTests() {
        configureLicensePackaging()
        excludeVersionFilesFromTestApks()
    }

    @Suppress("UnstableApiUsage") // usage of experimentalProperties
    private fun Variant.artRewritingWorkaround() {
        // b/279234807
        experimentalProperties.put("android.experimental.art-profile-r8-rewriting", false)
    }

    private fun HasAndroidTest.configureLicensePackaging() {
        androidTest?.packaging?.resources?.apply {
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

    /**
     * Excludes files telling which versions of androidx libraries were used in test apks to avoid
     * invalidating the build cache as often
     */
    private fun HasAndroidTest.excludeVersionFilesFromTestApks() {
        androidTest?.packaging?.resources?.apply { excludes.add("/META-INF/androidx*.version") }
    }

    @Suppress("DEPRECATION") // AGP DSL APIs
    private fun configureWithLibraryPlugin(project: Project, androidXExtension: AndroidXExtension) {
        val libraryExtension =
            project.extensions.getByType<LibraryExtension>().apply {
                configureAndroidBaseOptions(project, androidXExtension)
                project.addAppApkToTestConfigGeneration(androidXExtension)
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
            project.tasks
                .register(
                    variant.name + "AarManifestTransformer",
                    AarManifestTransformerTask::class.java
                )
                .let { taskProvider ->
                    variant.artifacts
                        .use(taskProvider)
                        .wiredWithFiles(
                            AarManifestTransformerTask::aarFile,
                            AarManifestTransformerTask::updatedAarFile
                        )
                        .toTransform(SingleArtifact.AAR)
                }
        }

        project.extensions.getByType<com.android.build.api.dsl.LibraryExtension>().apply {
            publishing { singleVariant(DEFAULT_PUBLISH_CONFIG) }
        }

        libraryAndroidComponentsExtension.apply {
            beforeVariants(selector().withBuildType("release")) { variant ->
                variant.enableUnitTest = false
            }
            onVariants {
                it.configureTests()
                it.artRewritingWorkaround()
            }
        }

        project.configurePublicResourcesStub(libraryExtension)
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
                    libraryVariant.packageLibraryProvider.map { zip -> zip.inputs.files }
                )
            }
        }

        // Standard docs, resource API, and Metalava configuration for AndroidX projects.
        project.configureProjectForApiTasks(
            LibraryApiTaskConfig(libraryExtension),
            androidXExtension
        )

        project.addToProjectMap(androidXExtension)

        project.buildOnServerDependsOnAssembleRelease()
        project.buildOnServerDependsOnLint()
    }

    private fun configureWithJavaPlugin(project: Project, extension: AndroidXExtension) {
        project.configureErrorProneForJava()

        // Force Java 1.8 source- and target-compatibility for all Java libraries.
        val javaExtension = project.extensions.getByType<JavaPluginExtension>()
        project.afterEvaluate {
            if (extension.type == LibraryType.COMPILER_PLUGIN) {
                javaExtension.apply {
                    sourceCompatibility = VERSION_11
                    targetCompatibility = VERSION_11
                }
            } else if (
                extension.type.compilationTarget == CompilationTarget.HOST &&
                    extension.type != LibraryType.ANNOTATION_PROCESSOR_UTILS
            ) {
                javaExtension.apply {
                    sourceCompatibility = VERSION_17
                    targetCompatibility = VERSION_17
                }
            } else {
                javaExtension.apply {
                    sourceCompatibility = VERSION_1_8
                    targetCompatibility = VERSION_1_8
                }
            }
            if (!project.plugins.hasPlugin(KotlinBasePluginWrapper::class.java)) {
                project.configureSourceJarForJava()
            }
        }

        project.configureJavaCompilationWarnings(extension)

        project.hideJavadocTask()

        project.configureDependencyVerification(extension) { taskProvider ->
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
        project.configureProjectForApiTasks(apiTaskConfig, extension)

        project.afterEvaluate {
            if (extension.shouldRelease()) {
                project.extra.set("publish", true)
            }
        }

        // Workaround for b/120487939 wherein Gradle's default resolution strategy prefers external
        // modules with lower versions over local projects with higher versions.
        project.configurations.all { configuration ->
            configuration.resolutionStrategy.preferProjectModules()
        }

        project.addToBuildOnServer("jar")

        project.addToProjectMap(extension)
    }

    private fun Project.configureProjectStructureValidation(extension: AndroidXExtension) {
        // AndroidXExtension.mavenGroup is not readable until afterEvaluate.
        afterEvaluate {
            val mavenGroup = extension.mavenGroup
            val isProbablyPublished =
                extension.type == LibraryType.PUBLISHED_LIBRARY ||
                    extension.type == LibraryType.UNSET
            if (mavenGroup != null && isProbablyPublished && extension.shouldPublish()) {
                validateProjectStructure(mavenGroup.group)
                validateProjectMavenName(extension.name.get(), mavenGroup.group)
            }
        }
    }

    private fun Project.configureProjectVersionValidation(extension: AndroidXExtension) {
        // AndroidXExtension.mavenGroup is not readable until afterEvaluate.
        afterEvaluate { extension.validateMavenVersion() }
    }

    private fun BaseExtension.configureAndroidBaseOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        compileOptions.apply {
            sourceCompatibility = VERSION_1_8
            targetCompatibility = VERSION_1_8
        }

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

        testOptions.animationsDisabled = true
        testOptions.unitTests.isReturnDefaultValues = true
        testOptions.unitTests.all { task ->
            // https://github.com/robolectric/robolectric/issues/7456
            task.jvmArgs =
                listOf(
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                    "--add-opens=java.base/java.util=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
            // Robolectric 1.7 increased heap size requirements, see b/207169653.
            task.maxHeapSize = "3g"
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
            project.configurations.all { configuration ->
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

        project.configureErrorProneForAndroid(variants)

        // workaround for b/120487939
        project.configurations.all { configuration ->
            // Gradle seems to crash on androidtest configurations
            // preferring project modules...
            if (!configuration.name.lowercase(Locale.US).contains("androidtest")) {
                configuration.resolutionStrategy.preferProjectModules()
            }
        }

        project.configureTestConfigGeneration(this)
        project.configureFtlRunner()

        // AGP warns if we use project.buildDir (or subdirs) for CMake's generated
        // build files (ninja build files, CMakeCache.txt, etc.). Use a staging directory that
        // lives alongside the project's buildDir.
        @Suppress("DEPRECATION")
        externalNativeBuild.cmake.buildStagingDirectory =
            File(project.buildDir, "../nativeBuildStaging")
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

        project.afterEvaluate {
            if (androidXExtension.shouldRelease()) {
                project.extra.set("publish", true)
            }
            if (project.hasBenchmarkPlugin()) {
                // Inject AOT compilation - see b/287358254 for context, b/288167775 for AGP support

                // NOTE: we assume here that all benchmarks have package name $namespace.test
                val aotCompile = "cmd package compile -m speed -f $namespace.test"

                // only run aotCompile on N+, where it's supported
                val inject = "if [ `getprop ro.build.version.sdk` -ge 24 ]; then $aotCompile; fi"
                val options =
                    "/data/local/tmp/${project.name}-$testBuildType-androidTest.apk && $inject #"
                adbOptions.setInstallOptions(*options.split(" ").toTypedArray())
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

    /** Sets the konan distribution url to the prebuilts directory. */
    private fun Project.configureKonanDirectory() {
        if (ProjectLayoutType.isPlayground(this)) {
            return // playground does not use prebuilts
        }
        overrideKotlinNativeDistributionUrlToLocalDirectory()
        overrideKotlinNativeDependenciesUrlToLocalDirectory()
    }

    private fun Project.overrideKotlinNativeDependenciesUrlToLocalDirectory() {
        val konanPrebuiltsFolder = getKonanPrebuiltsFolder()
        // use relative path so it doesn't affect gradle remote cache.
        val relativeRootPath = konanPrebuiltsFolder.relativeTo(rootProject.projectDir).path
        val relativeProjectPath = konanPrebuiltsFolder.relativeTo(projectDir).path
        tasks.withType(KotlinNativeCompile::class.java).configureEach {
            it.kotlinOptions.freeCompilerArgs +=
                listOf("-Xoverride-konan-properties=dependenciesUrl=file:$relativeRootPath")
        }
        tasks.withType(CInteropProcess::class.java).configureEach {
            it.settings.extraOpts +=
                listOf("-Xoverride-konan-properties", "dependenciesUrl=file:$relativeProjectPath")
        }
    }

    private fun Project.overrideKotlinNativeDistributionUrlToLocalDirectory() {
        val relativePath =
            getKonanPrebuiltsFolder().resolve("nativeCompilerPrebuilts").relativeTo(projectDir).path
        val url = "file:$relativePath"
        extensions.extraProperties["kotlin.native.distribution.baseDownloadUrl"] = url
    }

    private fun Project.configureKmpTests() {
        val kmpExtension =
            checkNotNull(project.extensions.findByType<KotlinMultiplatformExtension>()) {
                """
            Project ${project.path} applies kotlin multiplatform plugin but we cannot find the
            KotlinMultiplatformExtension.
            """
                    .trimIndent()
            }
        kmpExtension.testableTargets.all { kotlinTarget ->
            if (kotlinTarget is KotlinNativeTargetWithSimulatorTests) {
                kotlinTarget.binaries.all {
                    // Use std allocator to avoid the following warning:
                    // w: Mimalloc allocator isn't supported on target <target>. Used standard mode.
                    it.freeCompilerArgs += "-Xallocator=std"
                }
            }
        }
    }

    private fun AppExtension.configureAndroidApplicationOptions(
        project: Project,
        androidXExtension: AndroidXExtension
    ) {
        defaultConfig.apply {
            versionCode = 1
            versionName = "1.0"
        }

        project.addAppApkToTestConfigGeneration(androidXExtension)
        project.addAppApkToFtlRunner()
    }

    private fun Project.configureDependencyVerification(
        extension: AndroidXExtension,
        taskConfigurator: (TaskProvider<VerifyDependencyVersionsTask>) -> Unit
    ) {
        afterEvaluate {
            if (extension.type != LibraryType.UNSET && extension.type != LibraryType.SAMPLES) {
                val verifyDependencyVersionsTask = project.createVerifyDependencyVersionsTask()
                if (verifyDependencyVersionsTask != null) {
                    taskConfigurator(verifyDependencyVersionsTask)
                }
            }
        }
    }

    // If this project wants other project in the same group to have the same version,
    // this function configures those constraints.
    private fun Project.configureConstraintsWithinGroup(extension: AndroidXExtension) {
        if (!project.shouldAddGroupConstraints().get()) {
            return
        }
        project.afterEvaluate {
            // make sure that the project has a group
            val projectGroup = extension.mavenGroup ?: return@afterEvaluate
            // make sure that this group is configured to use a single version
            projectGroup.atomicGroupVersion ?: return@afterEvaluate

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

            val otherProjectsInSameGroup = extension.getOtherProjectsInSameGroup()
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

    companion object {
        const val CREATE_LIBRARY_BUILD_INFO_FILES_TASK = "createLibraryBuildInfoFiles"
        const val GENERATE_TEST_CONFIGURATION_TASK = "GenerateTestConfiguration"
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

private fun Project.addToProjectMap(extension: AndroidXExtension) {
    // TODO(alanv): Move this out of afterEvaluate
    afterEvaluate {
        if (extension.shouldRelease()) {
            val group = extension.mavenGroup?.group
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
private fun Project.configureTaskTimeouts() {
    tasks.configureEach { t ->
        // skip adding a timeout for some tasks that both take a long time and
        // that we can count on the user to monitor
        if (t !is StudioTask) {
            t.timeout.set(Duration.ofMinutes(TASK_TIMEOUT_MINUTES))
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

/** Expected to be called in afterEvaluate when all extensions are available */
internal fun Project.hasAndroidTestSourceCode(): Boolean {
    // com.android.test modules keep test code in main sourceset
    extensions.findByType(TestExtension::class.java)?.let { extension ->
        extension.sourceSets.findByName("main")?.let { sourceSet ->
            if (!sourceSet.java.getSourceFiles().isEmpty) return true
        }
        // check kotlin-android main source set
        extensions
            .findByType(KotlinAndroidProjectExtension::class.java)
            ?.sourceSets
            ?.findByName("main")
            ?.let { if (it.kotlin.files.isNotEmpty()) return true }
        // Note, don't have to check for kotlin-multiplatform as it is not compatible with
        // com.android.test modules
    }

    // check Java androidTest source set
    extensions
        .findByType(TestedExtension::class.java)
        ?.sourceSets
        ?.findByName("androidTest")
        ?.let { sourceSet ->
            // using getSourceFiles() instead of sourceFiles due to b/150800094
            if (!sourceSet.java.getSourceFiles().isEmpty) return true
        }

    // check kotlin-android androidTest source set
    extensions
        .findByType(KotlinAndroidProjectExtension::class.java)
        ?.sourceSets
        ?.findByName("androidTest")
        ?.let { if (it.kotlin.files.isNotEmpty()) return true }

    // check kotlin-multiplatform androidInstrumentedTest target source sets
    multiplatformExtension?.let { extension ->
        val instrumentedTestSourceSets = extension
            .targets
            .filterIsInstance<KotlinAndroidTarget>()
            .mapNotNull {
                target -> target.compilations.findByName("debugAndroidTest")
            }.flatMap { compilation -> compilation.allKotlinSourceSets }
        if (instrumentedTestSourceSets.any { it.kotlin.files.isNotEmpty() }) {
            return true
        }
    }

    return false
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
    kmpExtension: AndroidXMultiplatformExtension
) {
    project.afterEvaluate {
        if (kmpExtension.supportedPlatforms.isNotEmpty()) {
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
fun Project.validateProjectParser(extension: AndroidXExtension) {
    // If configuration fails, we don't want to validate the ProjectParser
    // (otherwise it could report a confusing, unnecessary error)
    project.gradle.taskGraph.whenReady {
        if (!extension.runProjectParser) return@whenReady

        val parsed = project.parse()
        check(extension.type == parsed.libraryType) {
            "ProjectParser incorrectly computed libraryType = ${parsed.libraryType} " +
                "instead of ${extension.type}"
        }
        check(extension.publish == parsed.publish) {
            "ProjectParser incorrectly computed publish = ${parsed.publish} " +
                "instead of ${extension.publish}"
        }
        check(extension.shouldPublish() == parsed.shouldPublish()) {
            "ProjectParser incorrectly computed shouldPublish() = ${parsed.shouldPublish()} " +
                "instead of ${extension.shouldPublish()}"
        }
        check(extension.shouldRelease() == parsed.shouldRelease()) {
            "ProjectParser incorrectly computed shouldRelease() = ${parsed.shouldRelease()} " +
                "instead of ${extension.shouldRelease()}"
        }
        check(extension.projectDirectlySpecifiesMavenVersion == parsed.specifiesVersion) {
            "ProjectParser incorrectly computed specifiesVersion = ${parsed.specifiesVersion}" +
                "instead of ${extension.projectDirectlySpecifiesMavenVersion}"
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

const val PROJECT_OR_ARTIFACT_EXT_NAME = "projectOrArtifact"
