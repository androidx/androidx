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

package androidx.build.sbom

import androidx.build.AndroidXPlaygroundRootImplPlugin
import androidx.build.BundleInsideHelper
import androidx.build.GMavenZipTask
import androidx.build.ProjectLayoutType
import androidx.build.addToBuildOnServer
import androidx.build.getDistributionDirectory
import androidx.build.getPrebuiltsRoot
import androidx.build.getSupportRootFolder
import androidx.build.gitclient.getHeadShaProvider
import androidx.inspection.gradle.EXPORT_INSPECTOR_DEPENDENCIES
import androidx.inspection.gradle.IMPORT_INSPECTOR_DEPENDENCIES
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.File
import java.net.URI
import java.util.UUID
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.spdx.sbom.gradle.SpdxSbomExtension
import org.spdx.sbom.gradle.SpdxSbomTask
import org.spdx.sbom.gradle.extensions.DefaultSpdxSbomTaskExtension
import org.spdx.sbom.gradle.project.ProjectInfo
import org.spdx.sbom.gradle.project.ScmInfo

/**
 * Tells whether the contents of the Configuration with the given name should be listed in our sbom
 *
 * That is, this tells whether the corresponding Configuration contains dependencies that get
 * embedded into our build artifact
 */
private fun Project.shouldSbomIncludeConfigurationName(configurationName: String): Boolean {
    return when (configurationName) {
        BundleInsideHelper.CONFIGURATION_NAME -> true
        "shadowed" -> true
        // compileClasspath is included by the Shadow plugin by default but projects that
        // declare a "shadowed" configuration exclude the "compileClasspath" configuration from
        // the shadowJar task
        "compileClasspath" -> appliesShadowPlugin() && configurations.findByName("shadowed") == null
        EXPORT_INSPECTOR_DEPENDENCIES -> true
        IMPORT_INSPECTOR_DEPENDENCIES -> true
        // https://github.com/spdx/spdx-gradle-plugin/issues/12
        sbomEmptyConfiguration -> true
        else -> false
    }
}

// An empty Configuration for the sbom plugin to ensure it has at least one Configuration
private const val sbomEmptyConfiguration = "sbomEmpty"

// some tasks that don't embed configurations having external dependencies
private val excludeTaskNames =
    setOf(
        "distZip",
        "shadowDistZip",
        "annotationsZip",
        "protoLiteJar",
        "bundleDebugLocalLintAar",
        "bundleReleaseLocalLintAar",
        "bundleDebugAar",
        "bundleReleaseAar",
        "bundleAndroidMainAar",
        "bundleAndroidMainLocalLintAar",
        "repackageAndroidMainAar",
        "repackageAarWithResourceApiAndroidMain"
    )

/**
 * Lists the Configurations that we should declare we're embedding into the output of this task
 *
 * The immediate inputs to the task are not generally mentioned here: external entities aren't
 * interested in knowing that our .aar file contains a classes.jar
 *
 * The external dependencies that embed into our artifacts are what we mention here: external
 * entities might be interested in knowing if, for example, we embed protobuf-javalite into our
 * artifact
 *
 * The purpose of this function is to detect new archive tasks and remind developers to update
 * shouldSbomIncludeConfigurationName
 */
private fun Project.listSbomConfigurationNamesForArchive(task: AbstractArchiveTask): List<String> {
    if (task is Jar && task !is ShadowJar) {
        // Jar tasks don't generally embed other dependencies in them
        return listOf()
    }
    if (task is GMavenZipTask) {
        // A GMavenZipTask just zips one or more artifacts we've already built
        return listOf()
    }

    val projectPath = path
    val taskName = task.name

    // some tasks that embed other configurations
    if (taskName == BundleInsideHelper.REPACKAGE_TASK_NAME) {
        return listOf(BundleInsideHelper.CONFIGURATION_NAME)
    }
    if (
        projectPath.contains("inspection") &&
            (taskName == "assembleInspectorJarRelease" ||
                taskName == "inspectionShadowDependenciesRelease")
    ) {
        return listOf(EXPORT_INSPECTOR_DEPENDENCIES)
    }

    if (excludeTaskNames.contains(taskName)) return listOf()
    if (projectPath == ":compose:lint:internal-lint-checks")
        return listOf() // we don't publish these lint checks
    if (projectPath.contains("integration-tests"))
        return listOf() // we don't publish integration tests
    if (taskName.startsWith("zip") && taskName.contains("ResultsOf") && taskName.contains("Test"))
        return listOf() // we don't publish test results

    // ShadowJar tasks have a `configurations` property that lists the configurations that
    // are inputs to the task, but they don't also list file inputs
    // If a project only has one shadowJar task (named "shadowJar"), for now we assume
    // that it doesn't include any external files that aren't already declared in
    // its configurations.
    // If a project has multiple shadowJar tasks, we ask the developer to provide
    // this metadata somehow by failing below
    if (taskName == "shadowJar" || taskName == "shadowLibraryJar") {
        // If the task is a ShadowJar task, we can just ask it which configurations it intends to
        // embed
        // We separately validate that this list is correct in
        val shadowTask = task as? ShadowJar
        if (shadowTask != null) {
            val configurations =
                configurations.filter { conf -> shadowTask.configurations.contains(conf) }
            return configurations.map { conf -> conf.name }
        }
    }

    if (taskName == "stubAar") {
        return listOf()
    }

    throw GradleException(
        "Not sure which external dependencies are included in $projectPath:$taskName of type " +
            "${task::class.java} (this is used for publishing sboms). Please update " +
            "Sbom.kt's listSbomConfigurationNamesForArchive and " +
            "shouldSbomIncludeConfigurationName"
    )
}

/** Validates that the inputs of the given archive task are recognized */
private fun Project.validateArchiveInputsRecognized(task: AbstractArchiveTask) {
    val configurationNames = listSbomConfigurationNamesForArchive(task)
    for (configurationName in configurationNames) {
        if (!shouldSbomIncludeConfigurationName(configurationName)) {
            throw GradleException(
                "Task listSbomConfigurationNamesForArchive(\"${task.name}\") = " +
                    "$configurationNames but " +
                    "shouldSbomIncludeConfigurationName(\"$configurationName\") = false. " +
                    "You probably should update shouldSbomIncludeConfigurationName to match"
            )
        }
    }
}

/** Validates that the inputs of each archive task are recognized */
fun Project.validateAllArchiveInputsRecognized() {
    tasks.withType(Zip::class.java).configureEach { task -> validateArchiveInputsRecognized(task) }
    tasks.withType(ShadowJar::class.java).configureEach { task ->
        validateArchiveInputsRecognized(task)
    }
}

/** Enables the publishing of an sbom that lists our embedded dependencies */
fun Project.configureSbomPublishing() {
    val uuid = coordinatesToUUID().toString()
    val projectName = name
    val projectVersion = version.toString()

    configurations.create(sbomEmptyConfiguration) { emptyConfiguration ->
        emptyConfiguration.isCanBeConsumed = false
    }
    apply(plugin = "org.spdx.sbom")
    val repos = getRepoPublicUrls()
    val headShaProvider = getHeadShaProvider(this)
    val supportRootDir = getSupportRootFolder()

    val allowPublicRepos = System.getenv("ALLOW_PUBLIC_REPOS") != null
    val sbomPublishDir = getSbomPublishDir()

    val sbomBuiltFile = layout.buildDirectory.file("spdx/release.spdx.json").get().asFile

    val publishTask =
        tasks.register("exportSboms", Copy::class.java) { publishTask ->
            publishTask.destinationDir = sbomPublishDir
            val sbomBuildDir = sbomBuiltFile.parentFile
            publishTask.from(sbomBuildDir)
            publishTask.rename(sbomBuiltFile.name, "$projectName-$projectVersion.spdx.json")

            publishTask.doFirst {
                if (!sbomBuiltFile.exists()) {
                    throw GradleException("sbom file does not exist: $sbomBuiltFile")
                }
            }
        }

    tasks.withType(SpdxSbomTask::class.java).configureEach { task ->
        val sbomProjectDir = projectDir

        task.taskExtension.set(
            object : DefaultSpdxSbomTaskExtension() {
                override fun mapRepoUri(repoUri: URI?, artifact: ModuleVersionIdentifier): URI {
                    val uriString = repoUri.toString()
                    for (repo in repos) {
                        val ourRepoUrl = repo.key
                        val publicRepoUrl = repo.value
                        if (uriString.startsWith(ourRepoUrl)) {
                            return URI.create(publicRepoUrl)
                        }
                        if (allowPublicRepos) {
                            if (uriString.startsWith(publicRepoUrl)) {
                                return URI.create(publicRepoUrl)
                            }
                        }
                    }
                    throw GradleException(
                        "Cannot determine public repo url for repo $uriString artifact $artifact"
                    )
                }

                override fun mapScmForProject(
                    original: ScmInfo,
                    projectInfo: ProjectInfo
                ): ScmInfo {
                    val url = getGitRemoteUrl(projectInfo.projectDirectory, supportRootDir)
                    return ScmInfo.from("git", url, headShaProvider.get())
                }

                override fun shouldCreatePackageForProject(projectInfo: ProjectInfo): Boolean {
                    // sbom should include the project it describes
                    if (sbomProjectDir.equals(projectInfo.projectDirectory)) return true
                    // sbom doesn't need to list our projects as dependencies;
                    // they're implementation details
                    // Example: glance:glance-appwidget uses glance:glance-appwidget-proto
                    if (pathContains(supportRootDir, projectInfo.projectDirectory)) return false
                    // sbom should list remaining project dependencies
                    return true
                }
            }
        )
    }

    val sbomExtension = extensions.getByType<SpdxSbomExtension>()
    val sbomConfigurations = mutableListOf<String>()

    afterEvaluate {
        configurations.configureEach { configuration ->
            if (shouldSbomIncludeConfigurationName(configuration.name)) {
                sbomConfigurations.add(configuration.name)
            }
        }

        sbomExtension.targets.create("release") { target ->
            val googleOrganization = "Organization: Google LLC"
            val document = target.document
            document.namespace.set("https://spdx.google.com/$uuid")
            document.creator.set(googleOrganization)
            document.packageSupplier.set(googleOrganization)

            target.configurations.set(sbomConfigurations)
        }
        addToBuildOnServer(tasks.named("spdxSbomForRelease"))
        publishTask.configure { task -> task.dependsOn("spdxSbomForRelease") }
    }
}

// Returns a UUID whose contents are based on the project's coordinates (group:artifact:version)
private fun Project.coordinatesToUUID(): UUID {
    val coordinates = "$group:$name:$version"
    val bytes = coordinates.toByteArray()
    return UUID.nameUUIDFromBytes(bytes)
}

private fun pathContains(ancestor: File, child: File): Boolean {
    val childNormalized = child.getCanonicalPath() + File.separator
    val ancestorNormalized = ancestor.getCanonicalPath() + File.separator
    return childNormalized.startsWith(ancestorNormalized)
}

private fun getGitRemoteUrl(dir: File, supportRootDir: File): String {
    if (pathContains(supportRootDir, dir)) {
        return "android.googlesource.com/platform/frameworks/support"
    }

    val notoFontsDir = File("$supportRootDir/../../external/noto-fonts")
    if (pathContains(notoFontsDir, dir)) {
        return "android.googlesource.com/platform/external/noto-fonts"
    }

    val icingDir = File("$supportRootDir/../../external/icing")
    if (pathContains(icingDir, dir)) {
        return "android.googlesource.com/platform/external/icing"
    }
    throw GradleException("Could not identify git remote url for project at $dir")
}

private fun Project.getSbomPublishDir(): File {
    val groupPath = group.toString().replace(".", "/")
    return File(getDistributionDirectory(), "sboms/$groupPath/$name/$version")
}

private const val MAVEN_CENTRAL_REPO_URL = "https://repo.maven.apache.org/maven2"
private const val GMAVEN_REPO_URL = "https://dl.google.com/android/maven2"

/** Returns a mapping from local repo url to public repo url */
private fun Project.getRepoPublicUrls(): Map<String, String> {
    return if (ProjectLayoutType.isPlayground(this)) {
        mapOf(
            MAVEN_CENTRAL_REPO_URL to MAVEN_CENTRAL_REPO_URL,
            AndroidXPlaygroundRootImplPlugin.INTERNAL_PREBUILTS_REPO_URL to GMAVEN_REPO_URL
        )
    } else {
        mapOf(
            "file:${getPrebuiltsRoot()}/androidx/external" to MAVEN_CENTRAL_REPO_URL,
            "file:${getPrebuiltsRoot()}/androidx/internal" to GMAVEN_REPO_URL
        )
    }
}

private fun Project.appliesShadowPlugin() = pluginManager.hasPlugin("com.gradleup.shadow")
