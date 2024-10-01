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
import androidx.build.license.CheckExternalDependencyLicensesTask
import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.bundling.ZipEntryCompression
import org.gradle.build.event.BuildEventsListenerRegistry
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

abstract class AndroidXRootImplPlugin : Plugin<Project> {
    @get:Inject abstract val registry: BuildEventsListenerRegistry
    @Suppress("UnstableApiUsage") @get:Inject abstract val buildFeatures: BuildFeatures

    override fun apply(project: Project) {
        if (!project.isRoot) {
            throw Exception("This plugin should only be applied to root project")
        }
        project.configureRootProject()
    }

    private fun Project.configureRootProject() {
        project.validateAllAndroidxArgumentsAreRecognized()
        tasks.register("listAndroidXProperties", ListAndroidXPropertiesTask::class.java)
        configureKtfmtCheckFile()
        tasks.register(CheckExternalDependencyLicensesTask.TASK_NAME)
        maybeRegisterFilterableTask()

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

        val buildOnServerTask = tasks.create(BUILD_ON_SERVER_TASK, BuildOnServerTask::class.java)
        buildOnServerTask.cacheEvenIfNoOutputs()
        buildOnServerTask.distributionDirectory = getDistributionDirectory()
        if (!buildFeatures.isIsolatedProjectsEnabled()) {
            buildOnServerTask.dependsOn(
                tasks.register(
                    CREATE_AGGREGATE_BUILD_INFO_FILES_TASK,
                    CreateAggregateLibraryBuildInfoFileTask::class.java
                )
            )
        }

        VerifyPlaygroundGradleConfigurationTask.createIfNecessary(project)?.let {
            buildOnServerTask.dependsOn(it)
        }

        extra.set("projects", ConcurrentHashMap<String, String>())

        /**
         * Copy PrivacySandbox related APKs into [getTestConfigDirectory] before zipping. Flatten
         * directory hierarchy as both TradeFed and FTL work with flat hierarchy.
         */
        val finalizeConfigsTask =
            project.tasks.register(FINALIZE_TEST_CONFIGS_WITH_APKS_TASK, Copy::class.java) {
                it.from(project.getPrivacySandboxFilesDirectory())
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

        project.tasks.register("listTaskOutputs", ListTaskOutputsTask::class.java) { task ->
            task.setOutput(File(project.getDistributionDirectory(), "task_outputs.txt"))
            task.removePrefix(project.getCheckoutRoot().path)
        }

        project.zipComposeCompilerMetrics()
        project.zipComposeCompilerReports()

        TaskUpToDateValidator.setup(project, registry)

        /**
         * Add dependency analysis plugin and add buildHealth task to buildOnServer when
         * maxDepVersions is not enabled
         */
        if (!project.usingMaxDepVersions()) {
            project.plugins.apply("com.autonomousapps.dependency-analysis")

            // Ignore advice regarding ktx dependencies
            val dependencyAnalysis =
                project.extensions.getByType(
                    com.autonomousapps.DependencyAnalysisExtension::class.java
                )
            dependencyAnalysis.structure { it.ignoreKtx(true) }
        }
        project.configureTasksForKotlinWeb()
    }

    private fun Project.configureTasksForKotlinWeb() {
        val offlineMirrorStorage =
            File(getPrebuiltsRoot(), "androidx/external/wasm/yarn-offline-mirror")
        val createYarnRcFileTask =
            tasks.register("createYarnRcFile", CreateYarnRcFileTask::class.java) {
                it.offlineMirrorStorage.set(offlineMirrorStorage)
                it.yarnrcFile.set(layout.buildDirectory.file("js/.yarnrc"))
            }

        tasks.withType<KotlinNpmInstallTask>().configureEach {
            it.dependsOn(createYarnRcFileTask)
            it.args.addAll(listOf("--ignore-engines", "--verbose"))
            if (project.useYarnOffline()) {
                it.args.add("--offline")
                it.doFirst {
                    println(
                        """
                    Fetching yarn packages from the offline mirror: ${offlineMirrorStorage.path}.
                    Your build will fail if a package is not in the offline mirror. To fix, run:

                    $TERMINAL_RED./gradlew kotlinNpmInstall -Pandroidx.yarnOfflineMode=false &&
                    ./gradlew kotlinUpgradeYarnLock$TERMINAL_RESET

                    this will download the dependencies from the internet and update the lockfile.
                    Don't forget to upload the changes to Gerrit!
                    """
                            .trimIndent()
                            .replace("\n", " ")
                    )
                }
            }
        }
    }
}
