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

import java.io.FileOutputStream
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault

/** Zips all artifacts to publish. */
@DisableCachingByDefault(because = "Zip tasks are not worth caching according to Gradle")
abstract class GMavenZipTask : DefaultTask() {

    /** Whether this build adds automatic constraints between projects in the same group */
    @Internal val shouldAddGroupConstraints = project.shouldAddGroupConstraints()

    /** Repository containing artifacts to include */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val projectRepositoryDir: DirectoryProperty

    /** Zip file to save artifacts to */
    @get:OutputFile abstract val archiveFile: RegularFileProperty

    @TaskAction
    fun createZip() {
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
        val sourceDir = projectRepositoryDir.get().asFile
        ZipOutputStream(FileOutputStream(archiveFile.get().asFile)).use { zipOut ->
            zipOut.putNextEntry(
                // Top-level of the ZIP to align with Maven's expected repository structure
                ZipEntry("m2repository/").also { it.time = CONSTANT_TIME_FOR_ZIP_ENTRIES }
            )
            zipOut.closeEntry()

            sourceDir.walkTopDown().forEach { fileOrDir ->
                if (fileOrDir == sourceDir) return@forEach

                val relativePath = fileOrDir.relativeTo(sourceDir).invariantSeparatorsPath
                val entryName =
                    "m2repository/$relativePath" + if (fileOrDir.isDirectory) "/" else ""

                zipOut.putNextEntry(
                    ZipEntry(entryName).also { it.time = CONSTANT_TIME_FOR_ZIP_ENTRIES }
                )
                if (fileOrDir.isFile) {
                    fileOrDir.inputStream().use { it.copyTo(zipOut) }
                }
                zipOut.closeEntry()
            }
        }
    }
}

/** Merges multiple zip files into one. */
@DisableCachingByDefault(because = "Zip tasks are not worth caching according to Gradle")
abstract class MergeZipsTask : DefaultTask() {
    /** List of zips to include */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val zips: ConfigurableFileCollection

    /** Zip file to save artifacts to */
    @get:OutputFile abstract val archiveFile: RegularFileProperty

    @TaskAction
    fun createZip() {
        val seenEntries = HashSet<String>()
        ZipOutputStream(FileOutputStream(archiveFile.get().asFile)).use { zipOut ->
            zips.files.forEach { file ->
                ZipFile(file).use { zipFile ->
                    val entries = zipFile.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (seenEntries.add(entry.name)) {
                            zipOut.putNextEntry(
                                ZipEntry(entry.name).also {
                                    it.time = CONSTANT_TIME_FOR_ZIP_ENTRIES
                                }
                            )
                            zipFile.getInputStream(entry).use { it.copyTo(zipOut) }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
        }
    }
}

/** Handles creating various release tasks that create zips for the maven upload and local use. */
object Release {
    @Suppress("MemberVisibilityCanBePrivate")
    const val PROJECT_ARCHIVE_ZIP_TASK_NAME = "createProjectZip"
    const val DEFAULT_PUBLISH_CONFIG = "release"
    const val PROJECT_ZIPS_FOLDER = "per-project-zips"

    /**
     * Registers the project to be included in its group's zip file as well as the global zip files.
     */
    fun register(project: Project, androidXExtension: AndroidXExtension) {
        if (!androidXExtension.shouldPublish.get()) {
            project.logger.info(
                "project ${project.name} isn't part of release," +
                    " because its \"publish\" property is explicitly set to Publish.NONE"
            )
            return
        }
        if (!androidXExtension.shouldRelease.get() && !isSnapshotBuild()) {
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

        val projectZipTask = getProjectZipTask(project)

        val publishTask = project.tasks.named("publish")
        projectZipTask.configure { zipTask -> zipTask.dependsOn(publishTask) }

        // Playground projects run with group constraints disabled (androidx.constraints=false).
        // Returning early here ensures that the createProjectZip task (GMavenZipTask) is not
        // included in the task graph, as it would otherwise fail due to the missing constraints.
        if (ProjectLayoutType.isPlayground(project)) return
        project.configurations.register("releaseArtifacts") {
            it.isCanBeResolved = false
            it.isCanBeConsumed = true
            it.isTransitive = false
            it.attributes { attributes ->
                attributes.attribute(
                    CATEGORY_ATTRIBUTE,
                    project.objects.named(Category::class.java, "androidx-release-artifacts"),
                )
            }
        }
        project.artifacts.add("releaseArtifacts", projectZipTask)
    }

    private fun getProjectZipTask(project: Project): TaskProvider<GMavenZipTask> {
        val taskProvider =
            project.tasks.register(PROJECT_ARCHIVE_ZIP_TASK_NAME, GMavenZipTask::class.java) {
                it.archiveFile.set(
                    project.getDistributionDirectory().file(project.getProjectZipPath())
                )
                it.projectRepositoryDir.set(project.getPerProjectRepositoryDirectory())
            }
        return taskProvider
    }
}

private fun Project.projectZipPrefix(): String {
    return "${project.group}-${project.name}"
}

private fun getZipName(fileNamePrefix: String) = "$fileNamePrefix-all"

fun Project.getProjectZipPath(): String {
    return Release.PROJECT_ZIPS_FOLDER +
        "/" +
        // We pass in a "" because that mimics not passing the group to getParams() inside
        // the getProjectZipTask function
        getZipName(projectZipPrefix()) +
        "-${project.version}.zip"
}

/**
 * Strip timestamps from the zip entries to generate consistent output. Set to be ths same as what
 * Gradle uses:
 * https://github.com/gradle/gradle/blob/master/platforms/core-runtime/files/src/main/java/org/gradle/api/internal/file/archive/ZipEntryConstants.java
 */
private val CONSTANT_TIME_FOR_ZIP_ENTRIES =
    GregorianCalendar(1980, Calendar.FEBRUARY, 1, 0, 0, 0).timeInMillis
