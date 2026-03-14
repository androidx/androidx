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

package androidx.build

import androidx.build.AndroidXImplPlugin.Companion.FINALIZE_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.AndroidXImplPlugin.Companion.ZIP_TEST_CONFIGS_WITH_APKS_TASK
import androidx.build.buildInfo.CreateAggregateLibraryBuildInfoFileTask
import androidx.build.buildInfo.CreateAggregateLibraryBuildInfoFileTask.Companion.CREATE_AGGREGATE_BUILD_INFO_FILES_TASK
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.gradle.isRoot
import androidx.build.intellij.IntelliJTask.Companion.registerIntelliJTask
import androidx.build.license.ValidateLicensesExistTask
import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import androidx.build.playground.ValidateIntegrationPatches
import androidx.build.playground.VerifyPlaygroundGradleConfigurationTask
import androidx.build.studio.StudioTask.Companion.registerStudioTask
import androidx.build.testConfiguration.registerOwnersServiceTasks
import androidx.build.uptodatedness.TaskUpToDateValidator
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.file.Directory
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinToolingSetupTask

abstract class AndroidXRootImplPlugin : Plugin<Project> {
    @get:Inject abstract val registry: BuildEventsListenerRegistry
    @get:Inject abstract val buildFeatures: BuildFeatures

    override fun apply(project: Project) {
        if (!project.isRoot) {
            throw Exception("This plugin should only be applied to root project")
        }
        project.configureRootProject()
    }

    private fun Project.configureRootProject() {
        project.validateAllAndroidxArgumentsAreRecognized()
        tasks.register("listAndroidXProperties", ListAndroidXPropertiesTask::class.java)
        tasks.register("createProject", ProjectCreatorTask::class.java)
        configureKtfmtCheckFile()
        maybeRegisterFilterableTask()
        registerListAffectedProjectsTask()

        // If we're running inside Studio, validate the Android Gradle Plugin version.
        val expectedAgpVersion = System.getenv("EXPECTED_AGP_VERSION")
        if (providers.gradleProperty("android.injected.invoked.from.ide").isPresent) {
            if (expectedAgpVersion != ANDROID_GRADLE_PLUGIN_VERSION) {
                throw GradleException(
                    """
                    Please close and restart Android Studio.

                    Expected AGP version \"$expectedAgpVersion\" does not match actual AGP version
                    \"$ANDROID_GRADLE_PLUGIN_VERSION\". This happens when AGP is updated while
                    Studio is running and can be fixed by restarting Studio.
                    """
                        .trimIndent()
                )
            }
        }

        val verifyPlayground = VerifyPlaygroundGradleConfigurationTask.createIfNecessary(project)

        val aggregateBuildInfo =
            if (!buildFeatures.isIsolatedProjectsEnabled()) {
                tasks.register(
                    CREATE_AGGREGATE_BUILD_INFO_FILES_TASK,
                    CreateAggregateLibraryBuildInfoFileTask::class.java,
                )
            } else null

        val artifactCollection = configureArtifactConfigurations()
        val distDir = getDistributionDirectory()

        val createArchive =
            tasks.register("createArchive", MergeZipsTask::class.java) { task ->
                task.archiveFile.set(distDir.file("top-of-tree-m2repository-all.zip"))
                task.zips.from(artifactCollection.releaseArtifacts)
            }

        tasks.register("createAllArchives", VerifyLicenseAndVersionFilesTask::class.java) { task ->
            task.group = "Distribution"
            task.description = "Builds all archives for publishing"
            task.repositoryZips.from(createArchive.map { it.zips })
            task.tmpDir.set(layout.buildDirectory.dir("androidx-verify"))
        }

        val attestationManifest =
            tasks.register(ATTESTATION_TASK_NAME, AttestationManifestTask::class.java) { task ->
                task.manifestFile.set(distDir.file("attestation_manifest.json"))
                task.zipMap.set(computeArtifactMap(artifactCollection.releaseIncoming, distDir))
                task.sbomMap.set(computeArtifactMap(artifactCollection.sbomIncoming, distDir))
            }

        tasks.register(BUILD_ON_SERVER_TASK, BuildOnServerTask::class.java) { task ->
            task.cacheEvenIfNoOutputs()
            task.aggregateBuildInfoFile.set(distDir.file(AGGREGATE_BUILD_INFO_FILE_NAME))
            task.dependsOn(attestationManifest)
            verifyPlayground?.let { task.dependsOn(it) }
            aggregateBuildInfo?.let { task.dependsOn(it) }
        }

        extra.set("projects", ConcurrentHashMap<String, String>())

        /**
         * Copy App APKs (from ApkOutputProviders) into [getTestConfigDirectory] before zipping.
         * Flatten directory hierarchy as both TradeFed and FTL work with flat hierarchy.
         */
        val finalizeConfigsTask =
            project.tasks.register(FINALIZE_TEST_CONFIGS_WITH_APKS_TASK, Copy::class.java) {
                it.from(project.getAppApksFilesDirectory())
                it.into(project.getTestConfigDirectory())
                it.eachFile { f -> f.relativePath = RelativePath(true, f.name) }
                it.includeEmptyDirs = false
            }

        // NOTE: this task is used by the Github CI as well. If you make any changes here,
        // please update the .github/workflows files as well, if necessary.
        project.tasks.register(ZIP_TEST_CONFIGS_WITH_APKS_TASK, Zip::class.java) {
            // Flatten PrivacySandbox APKs in separate task to preserve file order in resulting ZIP.
            it.dependsOn(finalizeConfigsTask)
            it.destinationDirectory.set(project.getDistributionDirectory())
            it.archiveFileName.set("androidTest.zip")
            it.from(project.getTestConfigDirectory())
            // We're mostly zipping a bunch of .apk files that are already compressed
            it.entryCompression = ZipEntryCompression.STORED
            // Archive is greater than 4Gb :O
            it.isZip64 = true
            it.isReproducibleFileOrder = true
        }

        AffectedModuleDetector.configure(gradle, this)

        if (!buildFeatures.isIsolatedProjectsEnabled()) {
            registerOwnersServiceTasks()
        }
        registerStudioTask()
        registerIntelliJTask()

        project.tasks.register("listTaskOutputs", ListTaskOutputsTask::class.java) { task ->
            task.outputFile.set(project.getDistributionDirectory().file("task_outputs.txt"))
            task.removePrefix(project.getCheckoutRoot().path)
        }

        TaskUpToDateValidator.setup(project, registry)

        /**
         * Add dependency analysis plugin and add buildHealth task to buildOnServer when
         * maxDepVersions is not enabled
         */
        if (!project.usingMaxDepVersions().get()) {
            project.plugins.apply("com.autonomousapps.dependency-analysis")

            // Ignore advice regarding ktx dependencies
            val dependencyAnalysis =
                project.extensions.getByType(
                    com.autonomousapps.DependencyAnalysisExtension::class.java
                )
            dependencyAnalysis.structure { it.ignoreKtx(true) }
        }
        project.configureTasksForKotlinWeb()

        tasks.register("checkExternalLicenses", ValidateLicensesExistTask::class.java) {
            it.prebuiltsDirectory.set(File(getPrebuiltsRoot(), "androidx/external"))
            it.baseline.set(layout.projectDirectory.file("license-baseline.txt"))
            it.cacheEvenIfNoOutputs()
        }

        ValidateIntegrationPatches.createTask(project)

        fetchDevelocityKeysIfNeeded()
    }

    private fun Project.configureTasksForKotlinWeb() {
        val offlineMirrorStorage =
            if (ProjectLayoutType.isPlayground(this)) {
                project.file(
                    layout.buildDirectory.dir("javascript-for-playground").map {
                        it.asFile.also { file -> file.mkdirs() }
                    }
                )
            } else {
                File(getPrebuiltsRoot(), "androidx/javascript-for-kotlin")
            }

        val createToolingYarnRcTask =
            registerYarnConfigTask(
                taskName = "createKotlinToolingYarnRcFile",
                offlineMirror = offlineMirrorStorage,
                cacheDir = layout.buildDirectory.dir("kotlinToolingYarnCache"),
                destFile =
                    project.objects
                        .fileProperty()
                        .fileValue(
                            File(project.getOutDirectory(), ".kotlin/kotlin-npm-tooling/.yarnrc")
                        ),
            )

        val createYarnRcTask =
            registerYarnConfigTask(
                taskName = "createYarnRcFile",
                offlineMirror = offlineMirrorStorage,
                cacheDir = layout.buildDirectory.dir("yarnCache"),
                destFile = layout.buildDirectory.file(".yarnrc"),
            )

        val createWasmYarnRcTask =
            registerYarnConfigTask(
                taskName = "createWasmYarnRcFile",
                offlineMirror = offlineMirrorStorage,
                cacheDir = layout.buildDirectory.dir("wasmYarnCache"),
                destFile = layout.buildDirectory.file("wasm/.yarnrc"),
            )

        configureNode()
        configureKotlinToolingTasks(offlineMirrorStorage, createToolingYarnRcTask)
        configureNpmInstallTasks(offlineMirrorStorage, createYarnRcTask, createWasmYarnRcTask)
    }

    private fun Project.registerYarnConfigTask(
        taskName: String,
        offlineMirror: File,
        cacheDir: Provider<Directory>,
        destFile: Provider<RegularFile>,
    ): TaskProvider<CreateYarnRcFileTask> =
        tasks.register(taskName, CreateYarnRcFileTask::class.java) { task ->
            task.offlineMirrorStorage.set(offlineMirror)
            task.cacheStorage.set(cacheDir)
            task.yarnrcFile.set(destFile)
        }

    private fun Project.configureKotlinToolingTasks(
        offlineMirror: File,
        configTask: TaskProvider<CreateYarnRcFileTask>,
    ) =
        tasks.withType<KotlinToolingSetupTask>().configureEach { task ->
            task.dependsOn(tasks.withType<KotlinNpmInstallTask>(), configTask)
            task.args.addAll(COMMON_YARN_ARGS)

            if (project.useYarnOffline()) {
                task.args.add("--offline")
                task.doFirst { println(getToolingOfflineErrorMsg(offlineMirror.path)) }
            }
        }

    private fun Project.configureNpmInstallTasks(
        offlineMirror: File,
        npmConfig: TaskProvider<CreateYarnRcFileTask>,
        wasmConfig: TaskProvider<CreateYarnRcFileTask>,
    ) =
        tasks.withType<KotlinNpmInstallTask>().configureEach { task ->
            when (task.name) {
                "kotlinNpmInstall" -> task.dependsOn(npmConfig)
                "kotlinWasmNpmInstall" -> task.dependsOn(wasmConfig)
            }

            task.args.addAll(COMMON_YARN_ARGS)

            if (project.useYarnOffline()) {
                task.args.add("--offline")
                task.additionalFiles.plus(offlineMirror)
                task.doFirst { println(getOfflineErrorMsg(offlineMirror.path)) }
            }
        }
}

private val COMMON_YARN_ARGS = listOf("--ignore-engines", "--verbose")

private fun getToolingOfflineErrorMsg(path: String) =
    """
    Fetching yarn packages from the offline mirror: $path.
    Your build will fail if a package is not in the offline mirror. To fix, run:

    $TERMINAL_RED./gradlew kotlinWasmToolingSetup -Pandroidx.yarnOfflineMode=false$TERMINAL_RESET

    This will download the dependencies from the internet.
    Don't forget to upload the changes to Gerrit!
"""
        .trimIndent()
        .replace("\n", " ")

private fun getOfflineErrorMsg(path: String) =
    """
    Fetching yarn packages from the offline mirror: $path.
    Your build will fail if a package is not in the offline mirror. To fix, run:

    $TERMINAL_RED./gradlew kotlinNpmInstall kotlinWasmNpmInstall -Pandroidx.yarnOfflineMode=false && ./gradlew kotlinUpgradeYarnLock kotlinWasmUpgradeYarnLock$TERMINAL_RESET

    This will download the dependencies from the internet and update the lockfile.
    Don't forget to upload the changes to Gerrit!
"""
        .trimIndent()
        .replace("\n", " ")

private fun Project.configureArtifactConfigurations(): RootArtifactCollection {
    val releaseProvider =
        registerArtifactConfiguration("releaseArtifacts", "androidx-release-artifacts")
    val sbomProvider = registerArtifactConfiguration("sbomArtifacts", "androidx-sbom-artifacts")

    subprojects { sub ->
        dependencies.add(releaseProvider.name, dependencies.create(sub))
        dependencies.add(sbomProvider.name, dependencies.create(sub))
    }

    val releaseView =
        releaseProvider.map { conf -> conf.incoming.artifactView { it.lenient(true) } }

    val sbomView = sbomProvider.map { conf -> conf.incoming.artifactView { it.lenient(true) } }
    val releaseFiles = objects.fileCollection().from(releaseView.map { it.files })

    return RootArtifactCollection(
        releaseArtifacts = releaseFiles,
        releaseIncoming = releaseView,
        sbomIncoming = sbomView,
    )
}

private fun Project.registerArtifactConfiguration(
    name: String,
    categoryName: String,
): NamedDomainObjectProvider<Configuration> =
    configurations.register(name) { conf ->
        conf.isCanBeResolved = true
        conf.isCanBeConsumed = false
        conf.isTransitive = false
        conf.attributes { attrs ->
            attrs.attribute(
                Category.CATEGORY_ATTRIBUTE,
                objects.named(Category::class.java, categoryName),
            )
        }
    }

/* Holds the lazy providers for artifact views and file collections. */
private class RootArtifactCollection(
    val releaseArtifacts: FileCollection,
    val releaseIncoming: Provider<ArtifactView>,
    val sbomIncoming: Provider<ArtifactView>,
)

/* Computes a map of projectPath to the artifact's path relative to the distDir. */
private fun computeArtifactMap(
    viewProvider: Provider<ArtifactView>,
    distDir: Provider<Directory>,
): Provider<Map<String, String>> {
    return viewProvider
        .flatMap { view -> view.artifacts.resolvedArtifacts }
        .zip(distDir) { artifacts, distDirectory ->
            artifacts
                .mapNotNull { artifact ->
                    val id = artifact.id.componentIdentifier
                    if (id is ProjectComponentIdentifier) {
                        val relativePath = artifact.file.relativeTo(distDirectory.asFile).path
                        id.projectPath to relativePath
                    } else {
                        null
                    }
                }
                .toMap()
        }
}

internal const val AGGREGATE_BUILD_INFO_FILE_NAME = "androidx_aggregate_build_info.txt"
