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

import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import java.io.File
import java.io.FileNotFoundException
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.work.DisableCachingByDefault

/** Simple description for an artifact that is released from this project. */
data class Artifact(
    @get:Input val mavenGroup: String,
    @get:Input val projectName: String,
    @get:Input val version: String
) {
    override fun toString() = "$mavenGroup:$projectName:$version"
}

/** Zip task that zips all artifacts from given candidates. */
@DisableCachingByDefault(because = "Zip tasks are not worth caching according to Gradle")
// See
// https://github.com/gradle/gradle/commit/7e5c5bc9b2c23d872e1c45c855f07ca223f6c270#diff-ce55b0f0cdcf2174eb47d333d348ff6fbd9dbe5cd8c3beeeaf633ea23b74ed9eR38
open class GMavenZipTask : Zip() {

    init {
        // multiple artifacts in the same group might have the same maven-metadata.xml
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    /** Set to true to include maven-metadata.xml */
    @get:Input var includeMetadata: Boolean = false

    /** Repository containing artifacts to include */
    @get:Internal lateinit var androidxRepoOut: File

    fun addCandidate(artifact: Artifact) {
        val groupSubdir = artifact.mavenGroup.replace('.', '/')
        val projectSubdir = File("$groupSubdir/${artifact.projectName}")
        val artifactSubdir = File("$projectSubdir/${artifact.version}")
        // We specifically pass the subdirectory and specific files into 'from' so that Gradle
        // knows that other directories aren't related:
        // 1. changes in other directories shouldn't cause this task to become out of date
        // 2. contents of other directories shouldn't be cached:
        //    https://github.com/gradle/gradle/issues/24368
        from("$androidxRepoOut/$artifactSubdir") { spec ->
            spec.into("m2repository/$artifactSubdir")
        }
        if (includeMetadata) {
            val suffixes = setOf("", ".md5", ".sha1", ".sha256", ".sha512")
            for (suffix in suffixes) {
                val filename = "maven-metadata.xml$suffix"
                from("$androidxRepoOut/$projectSubdir/$filename") { spec ->
                    spec.into("m2repository/$projectSubdir")
                }
            }
        }
    }

    /** Config action that configures the task when necessary. */
    class ConfigAction(private val params: Params) : Action<GMavenZipTask> {
        data class Params(
            /** Maven group for the task. "" if multiple groups or only one project */
            val mavenGroup: String,
            /** Set to true to include maven-metadata.xml */
            var includeMetadata: Boolean,
            /** The root of the repository where built libraries can be found */
            val androidxRepoOut: File,
            /** The out folder where the zip will be created */
            val distDir: File,
            /** Prefix of file name to create */
            val fileNamePrefix: String,
            /** The build number specified by the server */
            val buildNumber: String
        )

        override fun execute(task: GMavenZipTask) {
            params.apply {
                task.description =
                    """
                    Creates a maven repository that includes just the libraries compiled in
                    this project.
                    Group: ${if (mavenGroup != "") mavenGroup else "All"}
                    """
                        .trimIndent()
                task.androidxRepoOut = androidxRepoOut
                task.destinationDirectory.set(distDir)
                task.includeMetadata = params.includeMetadata
                task.archiveBaseName.set(getZipName(fileNamePrefix, mavenGroup))
            }
        }
    }
}

/** Handles creating various release tasks that create zips for the maven upload and local use. */
object Release {
    @Suppress("MemberVisibilityCanBePrivate")
    const val PROJECT_ARCHIVE_ZIP_TASK_NAME = "createProjectZip"
    private const val FULL_ARCHIVE_TASK_NAME = "createArchive"
    private const val ALL_ARCHIVES_TASK_NAME = "createAllArchives"
    const val DEFAULT_PUBLISH_CONFIG = "release"
    const val PROJECT_ZIPS_FOLDER = "per-project-zips"
    private const val GLOBAL_ZIP_PREFIX = "top-of-tree-m2repository"

    // lazily created config action params so that we don't keep re-creating them
    private var configActionParams: GMavenZipTask.ConfigAction.Params? = null

    /**
     * Registers the project to be included in its group's zip file as well as the global zip files.
     */
    fun register(project: Project, androidXExtension: AndroidXExtension) {
        if (!androidXExtension.shouldPublish()) {
            project.logger.info(
                "project ${project.name} isn't part of release," +
                    " because its \"publish\" property is explicitly set to Publish.NONE"
            )
            return
        }
        if (!androidXExtension.isPublishConfigured()) {
            project.logger.info(
                "project ${project.name} isn't part of release, because" +
                    " it does not set the \"publish\" property."
            )
            return
        }
        if (!androidXExtension.shouldRelease() && !isSnapshotBuild()) {
            project.logger.info(
                "project ${project.name} isn't part of release, because its" +
                    " \"publish\" property is SNAPSHOT_ONLY, but it is not a snapshot build"
            )
            return
        }
        if (!androidXExtension.versionIsSet) {
            throw IllegalArgumentException(
                "Cannot register a project to release if it does not have a mavenVersion set up"
            )
        }
        val version = project.version

        val projectZipTask = getProjectZipTask(project)
        val zipTasks = listOf(projectZipTask, getGlobalFullZipTask(project))

        val artifacts = androidXExtension.publishedArtifacts
        val publishTask = project.tasks.named("publish")
        zipTasks.forEach {
            it.configure { zipTask ->
                artifacts.forEach { artifact -> zipTask.addCandidate(artifact) }

                // Add additional artifacts needed for Gradle Plugins
                if (androidXExtension.type == LibraryType.GRADLE_PLUGIN) {
                    project.extensions
                        .getByType(GradlePluginDevelopmentExtension::class.java)
                        .plugins
                        .forEach { plugin ->
                            zipTask.addCandidate(
                                Artifact(
                                    mavenGroup = plugin.id,
                                    projectName = "${plugin.id}.gradle.plugin",
                                    version = version.toString()
                                )
                            )
                        }
                }

                zipTask.dependsOn(publishTask)
            }
        }

        val verifyInputs = getVerifyProjectZipInputsTask(project)
        verifyInputs.configure { verifyTask ->
            verifyTask.dependsOn(publishTask)
            artifacts.forEach { artifact -> verifyTask.addCandidate(artifact) }
        }
        val verifyOutputs = getVerifyProjectZipOutputsTask(project)
        verifyOutputs.configure { verifyTask ->
            verifyTask.dependsOn(projectZipTask)
            artifacts.forEach { artifact -> verifyTask.addCandidate(artifact) }
        }
        projectZipTask.configure { zipTask ->
            zipTask.dependsOn(verifyInputs)
            zipTask.finalizedBy(verifyOutputs)
            val verifyOutputsTask = verifyOutputs.get()
            verifyOutputsTask.addFile(zipTask.archiveFile.get().asFile)
        }
    }

    /**
     * Create config action parameters for the project and group. If group is `null`, parameters are
     * created for the global tasks.
     */
    private fun getParams(
        project: Project,
        distDir: File,
        fileNamePrefix: String = "",
        group: String? = null
    ): GMavenZipTask.ConfigAction.Params {
        // Make base params or reuse if already created
        val params =
            configActionParams
                ?: GMavenZipTask.ConfigAction.Params(
                        mavenGroup = "",
                        includeMetadata = false,
                        androidxRepoOut = project.getRepositoryDirectory(),
                        distDir = distDir,
                        fileNamePrefix = fileNamePrefix,
                        buildNumber = getBuildId()
                    )
                    .also { configActionParams = it }
        distDir.mkdirs()

        // Copy base params and apply any specific differences
        return params.copy(
            mavenGroup = group ?: "",
            distDir = distDir,
            fileNamePrefix = fileNamePrefix
        )
    }

    /** Registers an archive task as a dependency of the anchor task */
    private fun Project.addToAnchorTask(task: TaskProvider<GMavenZipTask>) {
        val archiveAnchorTask: TaskProvider<VerifyLicenseAndVersionFilesTask> =
            project.rootProject.maybeRegister(
                name = ALL_ARCHIVES_TASK_NAME,
                onConfigure = { archiveTask: VerifyLicenseAndVersionFilesTask ->
                    archiveTask.group = "Distribution"
                    archiveTask.description = "Builds all archives for publishing"
                    archiveTask.repositoryDirectory.set(
                        project.rootProject.getRepositoryDirectory()
                    )
                },
                onRegister = {}
            )
        archiveAnchorTask.configure { it.dependsOn(task) }
    }

    /**
     * Creates and returns the task that includes all projects regardless of their release status.
     */
    private fun getGlobalFullZipTask(project: Project): TaskProvider<GMavenZipTask> {
        return project.rootProject.maybeRegister(
            name = FULL_ARCHIVE_TASK_NAME,
            onConfigure = {
                GMavenZipTask.ConfigAction(
                        getParams(
                                project,
                                project.getDistributionDirectory(),
                                fileNamePrefix = GLOBAL_ZIP_PREFIX
                            )
                            .copy(includeMetadata = true)
                    )
                    .execute(it)
            },
            onRegister = { taskProvider: TaskProvider<GMavenZipTask> ->
                project.addToAnchorTask(taskProvider)
            }
        )
    }

    private fun getProjectZipTask(project: Project): TaskProvider<GMavenZipTask> {
        val taskProvider =
            project.tasks.register(PROJECT_ARCHIVE_ZIP_TASK_NAME, GMavenZipTask::class.java) {
                task: GMavenZipTask ->
                GMavenZipTask.ConfigAction(
                        getParams(
                                project = project,
                                distDir =
                                    File(project.getDistributionDirectory(), PROJECT_ZIPS_FOLDER),
                                fileNamePrefix = project.projectZipPrefix()
                            )
                            .copy(includeMetadata = true)
                    )
                    .execute(task)
            }
        project.addToAnchorTask(taskProvider)
        return taskProvider
    }

    private fun getVerifyProjectZipInputsTask(project: Project): TaskProvider<VerifyGMavenZipTask> {
        return project.tasks.register(
            "verifyInputs$PROJECT_ARCHIVE_ZIP_TASK_NAME",
            VerifyGMavenZipTask::class.java
        )
    }

    private fun getVerifyProjectZipOutputsTask(
        project: Project
    ): TaskProvider<VerifyGMavenZipTask> {
        return project.tasks.register(
            "verifyOutputs$PROJECT_ARCHIVE_ZIP_TASK_NAME",
            VerifyGMavenZipTask::class.java
        )
    }
}

// b/273294710
@DisableCachingByDefault(
    because = "This task only checks the existence of files and isn't worth caching"
)
open class VerifyGMavenZipTask : DefaultTask() {
    @Input val filesToVerify = mutableListOf<File>()

    /** Whether this build adds automatic constraints between projects in the same group */
    @get:Input val shouldAddGroupConstraints: Provider<Boolean>

    init {
        cacheEvenIfNoOutputs()
        shouldAddGroupConstraints = project.shouldAddGroupConstraints()
    }

    fun addFile(file: File) {
        filesToVerify.add(file)
    }

    fun addCandidate(artifact: Artifact) {
        val groupSubdir = artifact.mavenGroup.replace('.', '/')
        val projectSubdir = File("$groupSubdir/${artifact.projectName}")
        val androidxRepoOut = project.getRepositoryDirectory()
        val fromDir = project.file("$androidxRepoOut/$projectSubdir")
        addFile(File(fromDir, artifact.version))
    }

    @TaskAction
    fun execute() {
        verifySettings()
        verifyFiles()
    }

    private fun verifySettings() {
        if (!shouldAddGroupConstraints.get() && !isSnapshotBuild()) {
            throw GradleException(
                """
                Cannot publish artifacts without setting -P$ADD_GROUP_CONSTRAINTS=true

                This property is required when building artifacts to publish

                (but this property can reduce remote cache usage so it is disabled by default)

                See AndroidXGradleProperties.kt for more information about this property
                """
                    .trimIndent()
            )
        }
    }

    private fun verifyFiles() {
        val missingFiles = mutableListOf<String>()
        val emptyDirs = mutableListOf<String>()
        filesToVerify.forEach { file ->
            if (!file.exists()) {
                missingFiles.add(file.path)
            } else {
                if (file.isDirectory) {
                    if (file.listFiles().isEmpty()) {
                        emptyDirs.add(file.path)
                    }
                }
            }
        }

        if (missingFiles.isNotEmpty() || emptyDirs.isNotEmpty()) {
            val checkedFilesString = filesToVerify.toString()
            val missingFileString = missingFiles.toString()
            val emptyDirsString = emptyDirs.toString()
            throw FileNotFoundException(
                "GMavenZip ${missingFiles.size} missing files: $missingFileString, " +
                    "${emptyDirs.size} empty dirs: $emptyDirsString. " +
                    "Checked files: $checkedFilesString"
            )
        }
    }
}

val AndroidXExtension.publishedArtifacts: List<Artifact>
    get() {
        val groupString = mavenGroup?.group!!
        val versionString = project.version.toString()
        val artifacts =
            mutableListOf(
                Artifact(
                    mavenGroup = groupString,
                    projectName = project.name,
                    version = versionString
                )
            )

        // Add platform-specific artifacts, if necessary.
        artifacts +=
            publishPlatforms.map { suffix ->
                Artifact(
                    mavenGroup = groupString,
                    projectName = "${project.name}-$suffix",
                    version = versionString
                )
            }

        return artifacts
    }

private val AndroidXExtension.publishPlatforms: List<String>
    get() {
        val potentialTargets =
            project.multiplatformExtension
                ?.targets
                ?.asMap
                ?.filterValues { it.publishable }
                ?.keys
                ?.map {
                    it.lowercase()
                        // Remove when https://youtrack.jetbrains.com/issue/KT-70072 is fixed.
                        // MultiplatformExtension.targets includes `wasmjs` in its list, however,
                        // the publication folder for this target is named `wasm-js`. Not having
                        // this replace causes the verifyInputscreateProjectZip task to fail
                        // as it is looking for a file named wasmjs
                        .replace("wasmjs", "wasm-js")
                } ?: emptySet()
        val declaredTargets = potentialTargets.filter { it != "metadata" }
        return declaredTargets.toList()
    }

private fun Project.projectZipPrefix(): String {
    return "${project.group}-${project.name}"
}

private fun getZipName(fileNamePrefix: String, mavenGroup: String): String {
    val fileSuffix =
        if (mavenGroup == "") {
            "all"
        } else {
            mavenGroup.split(".").joinToString("-")
        } + "-${getBuildId()}"
    return "$fileNamePrefix-$fileSuffix"
}

fun Project.getProjectZipPath(): String {
    return Release.PROJECT_ZIPS_FOLDER +
        "/" +
        // We pass in a "" because that mimics not passing the group to getParams() inside
        // the getProjectZipTask function
        getZipName(projectZipPrefix(), "") +
        "-${project.version}.zip"
}
