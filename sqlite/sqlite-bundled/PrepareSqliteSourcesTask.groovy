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

import androidx.build.ProjectLayoutType
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.PathSensitivity

import javax.inject.Inject

/**
 * Finds the sqlite sources and puts them into the `destinationDirectory`.
 *
 * This task is setup differently between AOSP and Github (Playground), hence use the helper
 * `registerPrepareSqliteSourcesTask` method to create an instance of it.
 *
 * On AOSP, the sources are in an external prebuilts repository and simply get copied into the given
 * [destinationDirectory].
 * On Github, they are downloaded from SQLite servers and copied into the `destinationDirectory`
 * from there.
 *
 * To ensure each version is consistent, we use the `sqliteVersion` parameter and check the Sqlite
 * source code for them.
 */
abstract class PrepareSqliteSourcesTask extends DefaultTask {
    // defined in https://github.com/sqlite/sqlite/blob/master/src/sqlite.h.in#L149
    private static String VERSION_PREFIX = "#define SQLITE_VERSION"
    private FileSystemOperations fileSystemOperations

    /**
     * The Sqlite version to prepare
     */
    @Input
    abstract Property<String> getSqliteVersion()

    /**
     * The target directory where the Sqlite source will be put
     */
    @OutputDirectory
    abstract DirectoryProperty getDestinationDirectory()

    /**
     * The source directory which includes the original Sqlite amalgamation distribution
     */
    @InputDirectory
    @PathSensitive(PathSensitivity.NONE)
    abstract DirectoryProperty getSources()

    @Inject
    PrepareSqliteSourcesTask(FileSystemOperations fileSystemOperations) {
        this.fileSystemOperations = fileSystemOperations
        description = "Create a directory containing Sqlite sources."
        group = "build"
    }

    @TaskAction
    void prepareSources() {
        File originalSqliteSources = sources.asFile.get()
        validateSqliteVersion(originalSqliteSources)
        File targetSourceDirectory = destinationDirectory.asFile.get()
        targetSourceDirectory.deleteDir()
        targetSourceDirectory.mkdirs()

        fileSystemOperations.copy { CopySpec copySpec ->
            copySpec.from(originalSqliteSources)
            copySpec.into(targetSourceDirectory)
            copySpec.include("sqlite3.c", "sqlite3.h", "sqlite3ext.h")
        }
    }

    /**
     * Finds the sqlite version definition in the source file and ensures it is the same
     * version as [sqliteVersion] to ensure they never go out of sync.
     */
    private void validateSqliteVersion(File sourceDir) {
        File headerFile = new File(sourceDir, "sqlite3.h")
        if (!headerFile.isFile() || !headerFile.canRead()) {
            throw new IllegalStateException("Cannot find header file at location: ${headerFile}")
        }
        String versionLine = headerFile.text.split('\n').find { it.contains(VERSION_PREFIX) }
        if (versionLine == null) {
            throw new IllegalStateException("Cannot find the version line in sqlite.")
        }
        String strippedVersion = versionLine.takeAfter(VERSION_PREFIX).trim()
                .takeBetween("\"", "\"")
        if (strippedVersion != sqliteVersion.get()) {
            throw new IllegalStateException("""
                Expected ${sqliteVersion.get()}, found $strippedVersion. Please update the
                sqliteVersion parameter if this was intentional.
            """.trim())
        }
    }
}

/**
 * Downloads the sqlite amalgamation for the given version.
 * See: https://sqlite.org/amalgamation.html and https://www.sqlite.org/download.html for
 * details.
 */
@CacheableTask
abstract class DownloadSQLiteAmalgamationTask extends DefaultTask {
    /**
     * The Sqlite version to download
     */
    @Input
    abstract Property<String> getReleaseVersion()

    /**
     * The year which Sqlite version was released. It is necessary because the download
     * URL includes the year.
     */
    @Input
    abstract Property<Integer> getReleaseYear()

    /**
     * Target file where the downloaded amalgamation zip file will be written.
     */
    @OutputFile
    abstract RegularFileProperty getDownloadTargetFile()

    DownloadSQLiteAmalgamationTask() {
        description = "Downloads the Sqlite amalgamation build from sqlite servers"
        group = "build"
    }

    @TaskAction
    void download() {
        File downloadTarget = downloadTargetFile.asFile.get()
        downloadTarget.delete()
        downloadTarget.parentFile.mkdirs()
        String downloadUrl = buildDownloadUrl(releaseYear.get(), releaseVersion.get())
        downloadTarget.withOutputStream { outputStream ->
            new URL(downloadUrl).withInputStream { inputStream ->
                inputStream.transferTo(outputStream)
            }
        }
    }

    /**
     * Computes the download URL from the sqlite version and release year inputs.
     */
    private static String buildDownloadUrl(int releaseYear, String releaseVersion) {
        // see https://www.sqlite.org/download.html
        // The version is encoded so that filenames sort in order of increasing version number
        // when viewed using "ls".
        // For version 3.X.Y the filename encoding is 3XXYY00.
        // For branch version 3.X.Y.Z, the encoding is 3XXYYZZ.
        def sections = releaseVersion.split('\\.')
        if (sections.size() < 3) {
            throw new IllegalArgumentException("Invalid sqlite version $releaseVersion")
        }
        int major = sections[0].toInteger()
        int minor = sections[1].toInteger()
        int patch = sections[2].toInteger()
        int branch = sections.size() >= 4 ? sections[3].toInteger() : 0
        String fileName = String.format("%d%02d%02d%02d.zip", major, minor, patch, branch)
        return "https://www.sqlite.org/${releaseYear}/sqlite-amalgamation-${fileName}"
    }
}

/**
 * Configuration object for preparing relevant sqlite sources.
 */
abstract class Configuration {
    /**
     * The Sqlite version to be prepared.
     */
    abstract Property<String> getSqliteVersion()

    /**
     * The release year of the requested Sqlite version.
     * It is necessary because the download URL for sqlite amalgamation includes the
     * release year.
     */
    abstract Property<Integer> getSqliteReleaseYear()

    /**
     * The location to put prepared sqlite sources.
     */
    abstract DirectoryProperty getDestinationDirectory()

    /**
     * Set when sqlite is downloaded from prebuilts rather than from Sqlite servers (used in AOSP).
     */
    abstract DirectoryProperty getSqlitePrebuiltsDirectory()
}

/**
 * Utility method to create an instance of [PrepareSqliteSourcesTask] that is compatible
 * with both AOSP and GitHub builds.
 * This is exported into the build script via ext properties.
 */
TaskProvider<PrepareSqliteSourcesTask> registerPrepareSqliteSourcesTask(
        Project project,
        String name,
        Action<Configuration> configure
) {
    def configuration = project.objects.newInstance(Configuration.class)
    configure.execute(configuration)

    def distDirectory = project.objects.directoryProperty()
    if (ProjectLayoutType.isPlayground(project)) {
        def downloadTaskProvider = project.tasks.register(
                name.capitalize() + "DownloadAmalgamation",
                DownloadSQLiteAmalgamationTask
        ) {
            it.releaseVersion.set(configuration.sqliteVersion)
            it.releaseYear.set(configuration.sqliteReleaseYear)
            it.downloadTargetFile.set(
                    project.layout.buildDirectory.file("sqlite3/download/amalgamation.zip")
            )
        }

        def unzipTaskProvider = project.tasks.register(
                name.capitalize() + "UnzipAmalgamation",
                Copy
        ) {
            it.from(
                    project.zipTree(downloadTaskProvider.map { it.downloadTargetFile })
            )
            it.into(
                    project.layout.buildDirectory.dir("sqlite3/download/unzipped")
            )
            it.eachFile {
                it.path = it.path.replaceFirst(/sqlite-amalgamation-\d+\//, '')
            }
        }
        distDirectory.set(
                project.objects.directoryProperty().fileProvider(
                        unzipTaskProvider.map { it.destinationDir }
                )
        )
    } else {
        distDirectory.set(configuration.sqlitePrebuiltsDirectory)
    }

    def prepareSourcesTaskProvider = project.tasks.register(
            name,
            PrepareSqliteSourcesTask
    ) {
        it.sources.set(distDirectory)
        it.sqliteVersion.set(configuration.sqliteVersion)
        it.destinationDirectory.set(
                project.layout.buildDirectory.dir("sqlite/selected-sources")
        )
    }
    return prepareSourcesTaskProvider
}

// export a function to register the task
ext.registerPrepareSqliteSourcesTask = this.&registerPrepareSqliteSourcesTask
