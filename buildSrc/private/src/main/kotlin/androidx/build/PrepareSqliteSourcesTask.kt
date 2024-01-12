/*
 * Copyright 2024 The Android Open Source Project
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

import com.android.utils.appendCapitalized
import java.io.File
import java.net.URL
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault

/**
 * Finds the sqlite sources and puts them into [destinationDirectory].
 *
 * This task runs differently between AOSP and Github (Playground).
 *
 * On AOSP, the sources are in an external prebuilts repository and simply get copied into the given
 * [destinationDirectory].
 * On Github, they are downloaded from SQLite servers.
 *
 * To ensure each version is consistent, we use the [sqliteVersion] parameter and check the SQLite
 * source code for them.
 */
@DisableCachingByDefault(
    because = "This is a simple copy/validation task that is not worth caching"
)
abstract class PrepareSqliteSourcesTask @Inject constructor(
    private val fileSystemOperations: FileSystemOperations
) : DefaultTask() {
    @get:Input
    abstract val sqliteVersion: Property<String>

    @get:OutputDirectory
    abstract val destinationDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sources: DirectoryProperty

    init {
        description = "Create a directory containing Sqlite sources."
        group = "build"
    }

    @TaskAction
    fun prepareSources() {
        val originalSqliteSources = sources.asFile.get()
        validateSqliteVersion(originalSqliteSources)
        val targetSourceDirectory = destinationDirectory.asFile.get().also {
            it.deleteRecursively()
            it.mkdirs()
        }
        fileSystemOperations.copy { copySpec ->
            copySpec.from(originalSqliteSources)
            copySpec.into(targetSourceDirectory)
            copySpec.include("sqlite3.c")
            copySpec.include("sqlite3.h")
        }
    }

    /**
     * Finds the sqlite version definition in the source file and ensures it is the same
     * version as [sqliteVersion] to ensure they never go out of sync.
     */
    private fun validateSqliteVersion(sourceDir: File) {
        val headerFile = sourceDir.resolve("sqlite3.h")
        check(headerFile.isFile && headerFile.canRead()) {
            "Cannot find header file at location: $headerFile"
        }
        // find the line that defines the sqlite version (see docs for the constant).
        val versionLine = headerFile.useLines {
            it.firstOrNull { it.contains(VERSION_PREFIX) }
        } ?: error("Cannot find the version line in sqlite.")
        val strippedVersion = versionLine.substringAfter(VERSION_PREFIX)
            .trim() // trim whitespace
            .trim('"') // trim ""
        check(strippedVersion == sqliteVersion.get()) {
            """
                Expected ${sqliteVersion.get()}, found $strippedVersion. Please update the
                sqliteVersion parameter if this was intentional.
            """.trimIndent()
        }
    }

    /**
     * Downloads the sqlite amalgamation for the given version.
     * See: https://sqlite.org/amalgamation.html and https://www.sqlite.org/download.html for
     * details.
     */
    @CacheableTask
    internal abstract class DownloadSQLiteAmalgamationTask : DefaultTask() {
        @get:Input
        abstract val releaseVersion: Property<String>

        @get:Input
        abstract val releaseYear: Property<Int>

        @get:OutputFile
        abstract val downloadTargetFile: RegularFileProperty

        @TaskAction
        fun doIt() {
            val downloadTargetFile = downloadTargetFile.asFile.get()
            downloadTargetFile.delete()
            downloadTargetFile.parentFile.mkdirs()
            val downloadUrl = buildDownloadUrl(
                releaseYear = releaseYear.get(),
                releaseVersion = releaseVersion.get()
            )
            URL(downloadUrl).openStream().use { inputStream ->
                downloadTargetFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }

        /**
         * Computes the download URL from the sqlite version and release year inputs.
         */
        private fun buildDownloadUrl(
            releaseYear: Int,
            releaseVersion: String
        ): String {
            // see https://www.sqlite.org/download.html
            // The version is encoded so that filenames sort in order of increasing version number
            // when viewed using "ls".
            // For version 3.X.Y the filename encoding is 3XXYY00.
            // For branch version 3.X.Y.Z, the encoding is 3XXYYZZ.
            val sections = releaseVersion.split('.')
            check(sections.size >= 3) {
                "invalid sqlite version $releaseVersion"
            }
            val major = sections[0].toInt()
            val minor = sections[1].toInt()
            val patch = sections[2].toInt()
            val branch = if (sections.size >= 4) sections[3].toInt() else 0
            val fileName = String.format("%d%02d%02d%02d.zip", major, minor, patch, branch)
            return listOf(
                "https://www.sqlite.org",
                releaseYear.toString(),
                "sqlite-amalgamation-${fileName}"
            ).joinToString("/")
        }
    }

    companion object {
        // defined in https://github.com/sqlite/sqlite/blob/master/src/sqlite.h.in#L149
        private const val VERSION_PREFIX = "#define SQLITE_VERSION"

        /**
         * Utility method to create an instance of [PrepareSqliteSourcesTask] that is compatible
         * with both AOSP and GitHub builds.
         */
        @JvmStatic
        fun register(
            project: Project,
            name: String,
            configure: Action<Configuration>
        ): TaskProvider<out PrepareSqliteSourcesTask> {
            val configuration = Configuration(
                sqliteVersion = project.objects.property(String::class.java),
                sqliteReleaseYear = project.objects.property(Int::class.java),
                destinationDirectory = project.objects.directoryProperty(),
                sqlitePrebuiltsDirectory = project.objects.directoryProperty()
            )
            configure.execute(configuration)
            val distDirectory = project.objects.directoryProperty()

            if (ProjectLayoutType.isPlayground(project)) {
                val downloadTaskProvider = project.tasks.register(
                    name.appendCapitalized("DownloadAmalgamation"),
                    DownloadSQLiteAmalgamationTask::class.java
                ) {
                    it.releaseVersion.set(configuration.sqliteVersion)
                    it.releaseYear.set(configuration.sqliteReleaseYear)
                    it.downloadTargetFile.set(
                        project.layout.buildDirectory.file("sqlite3/download/amalgamation.zip")
                    )
                }
                val unzipTaskProvider =
                    project.tasks.register(
                        name.appendCapitalized("UnzipAmalgamation"),
                        Copy::class.java
                    ) {
                        it.from(
                            project.zipTree(downloadTaskProvider.map { it.downloadTargetFile })
                        )
                        it.into(
                            project.layout.buildDirectory
                                .dir("sqlite3/download/unzipped")
                        )
                        it.eachFile {
                            // get rid of the amalgamation folder in output dir
                            it.path = it.path.replaceFirst(
                                "sqlite-amalgamation-[\\d]+/".toRegex(),
                                ""
                            )
                        }
                    }
                distDirectory.set(
                    project.objects.directoryProperty().fileProvider(
                        unzipTaskProvider.map { it.destinationDir }
                    )
                )
            }
            val prepareSourcesTaskProvider = project.tasks.register(
                name,
                PrepareSqliteSourcesTask::class.java
            ) {
                it.sources.set(distDirectory)
                it.sqliteVersion.set(configuration.sqliteVersion)
                it.destinationDirectory.set(
                    project.layout.buildDirectory.dir("sqlite/selected-sources")
                )
            }
            return prepareSourcesTaskProvider
        }
    }

    data class Configuration(
        val sqliteVersion: Property<String>,
        val sqliteReleaseYear: Property<Int>,
        val destinationDirectory: DirectoryProperty,
        val sqlitePrebuiltsDirectory: DirectoryProperty
    )
}