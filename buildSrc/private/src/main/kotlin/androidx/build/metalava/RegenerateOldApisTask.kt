/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.build.metalava

import androidx.build.Version
import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.SourceSetInputs
import androidx.build.checkapi.getApiFileVersion
import androidx.build.checkapi.getRequiredCompatibilityApiLocation
import androidx.build.checkapi.getVersionedApiLocation
import androidx.build.checkapi.isValidArtifactVersion
import androidx.build.getAndroidJar
import androidx.build.getCheckoutRoot
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.ivyservice.TypedResolveException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

/** Generate API signature text files using previously built .jar/.aar artifacts. */
@CacheableTask
abstract class RegenerateOldApisTask
@Inject
constructor(private val workerExecutor: WorkerExecutor) : DefaultTask() {

    @Input var generateRestrictToLibraryGroupAPIs = true

    @get:Input abstract val kotlinSourceLevel: Property<KotlinVersion>

    @get:Input
    @set:Option(
        option = "compat-version",
        description = "Regenerate just the signature file needed for compatibility checks"
    )
    var compatVersion: Boolean = false

    @TaskAction
    fun exec() {
        val groupId = project.group.toString()
        val artifactId = project.name
        val internalPrebuiltsDir = File(project.getCheckoutRoot(), "prebuilts/androidx/internal")
        val projectPrebuiltsDir =
            File(internalPrebuiltsDir, groupId.replace(".", "/") + "/" + artifactId)
        if (compatVersion) {
            regenerateCompatVersion(groupId, artifactId, projectPrebuiltsDir)
        } else {
            regenerateAllVersions(groupId, artifactId, projectPrebuiltsDir)
        }
    }

    /**
     * Attempts to regenerate the API file for all previous versions by listing the prebuilt
     * versions that exist and regenerating each one which already has an existing signature file.
     */
    private fun regenerateAllVersions(
        groupId: String,
        artifactId: String,
        projectPrebuiltsDir: File,
    ) {
        val artifactVersions = listVersions(projectPrebuiltsDir)

        var prevApiFileVersion = getApiFileVersion(project.version as Version)
        for (artifactVersion in artifactVersions.reversed()) {
            val apiFileVersion = getApiFileVersion(artifactVersion)
            // If two artifacts correspond to the same API file, don't regenerate the
            // same api file again
            if (apiFileVersion != prevApiFileVersion) {
                val location = project.getVersionedApiLocation(apiFileVersion)
                regenerate(project.rootProject, groupId, artifactId, artifactVersion, location)
                prevApiFileVersion = apiFileVersion
            }
        }
    }

    /**
     * Regenerates just the signature file used for compatibility checks against the current
     * version. If prebuilts for that version don't exist (since prebuilts for betas are sometimes
     * deleted), attempts to use prebuilts for the corresponding stable version, which should have
     * the same API surface.
     */
    private fun regenerateCompatVersion(
        groupId: String,
        artifactId: String,
        projectPrebuiltsDir: File,
    ) {
        val location =
            project.getRequiredCompatibilityApiLocation()
                ?: run {
                    logger.warn("No required compat location for $groupId:$artifactId")
                    return
                }
        val compatVersion = location.version()!!

        if (!tryRegenerate(projectPrebuiltsDir, groupId, artifactId, compatVersion, location)) {
            val stable = compatVersion.copy(extra = null)
            logger.warn("No prebuilts for version $compatVersion, trying with $stable")
            if (!tryRegenerate(projectPrebuiltsDir, groupId, artifactId, stable, location)) {
                logger.error("Could not regenerate $compatVersion")
            }
        }
    }

    /**
     * If prebuilts exists for the [version], runs [regenerate] and returns true, otherwise returns
     * false.
     */
    private fun tryRegenerate(
        projectPrebuiltsDir: File,
        groupId: String,
        artifactId: String,
        version: Version,
        location: ApiLocation,
    ): Boolean {
        if (File(projectPrebuiltsDir, version.toString()).exists()) {
            regenerate(project.rootProject, groupId, artifactId, version, location)
            return true
        }
        return false
    }

    // Returns all (valid) artifact versions that appear to exist in <dir>
    private fun listVersions(dir: File): List<Version> {
        val pathNames: Array<String> = dir.list() ?: arrayOf()
        val files = pathNames.map { name -> File(dir, name) }
        val subdirs = files.filter { child -> child.isDirectory() }
        val versions = subdirs.map { child -> Version(child.name) }
        val validVersions = versions.filter { v -> isValidArtifactVersion(v) }
        return validVersions.sorted()
    }

    private fun regenerate(
        runnerProject: Project,
        groupId: String,
        artifactId: String,
        version: Version,
        outputApiLocation: ApiLocation,
    ) {
        val mavenId = "$groupId:$artifactId:$version"
        val (compiledSources, sourceSets) =
            try {
                getFiles(runnerProject, mavenId)
            } catch (e: TypedResolveException) {
                runnerProject.logger.info("Ignoring missing artifact $mavenId: $e")
                return
            }

        if (outputApiLocation.publicApiFile.exists()) {
            project.logger.lifecycle("Regenerating $mavenId")
            val projectXml = File(temporaryDir, "$mavenId-project.xml")
            ProjectXml.create(
                sourceSets,
                project.getAndroidJar().files,
                compiledSources,
                projectXml
            )
            generateApi(
                project.getMetalavaClasspath(),
                projectXml,
                sourceSets.flatMap { it.sourcePaths.files },
                outputApiLocation,
                ApiLintMode.Skip,
                generateRestrictToLibraryGroupAPIs,
                emptyList(),
                false,
                kotlinSourceLevel.get(),
                workerExecutor
            )
        } else {
            logger.warn("No API file for $mavenId")
        }
    }

    /**
     * For the given [mavenId], returns a pair with the source jar as the first element, and
     * [SourceSetInputs] representing the unzipped sources as the second element.
     */
    private fun getFiles(
        runnerProject: Project,
        mavenId: String
    ): Pair<File, List<SourceSetInputs>> {
        val jars = getJars(runnerProject, mavenId)
        val sourcesMavenId = "$mavenId:sources"
        val compiledSources = getCompiledSources(runnerProject, sourcesMavenId)
        val sources = getSources(runnerProject, sourcesMavenId, compiledSources)

        // TODO(b/330721660) parse META-INF/kotlin-project-structure-metadata.json for KMP projects
        // Represent the project as a single source set.
        return compiledSources to
            listOf(
                SourceSetInputs(
                    // Since there's just one source set, the name is arbitrary.
                    sourceSetName = "main",
                    // There are no other source sets to depend on.
                    dependsOnSourceSets = emptyList(),
                    sourcePaths = sources,
                    dependencyClasspath = jars,
                )
            )
    }

    private fun getJars(runnerProject: Project, mavenId: String): FileCollection {
        val configuration =
            runnerProject.configurations.detachedConfiguration(
                runnerProject.dependencies.create(mavenId)
            )
        val resolvedConfiguration = configuration.resolvedConfiguration.resolvedArtifacts
        val dependencyFiles = resolvedConfiguration.map { artifact -> artifact.file }

        val jars = dependencyFiles.filter { file -> file.name.endsWith(".jar") }
        val aars = dependencyFiles.filter { file -> file.name.endsWith(".aar") }
        val classesJars =
            aars.map { aar ->
                val tree = project.zipTree(aar)
                val classesJar =
                    tree
                        .matching { filter: PatternFilterable -> filter.include("classes.jar") }
                        .single()
                classesJar
            }
        val embeddedLibs = getEmbeddedLibs(runnerProject, mavenId)
        val undeclaredJarDeps = getUndeclaredJarDeps(runnerProject, mavenId)
        return runnerProject.files(jars + classesJars + embeddedLibs + undeclaredJarDeps)
    }

    private fun getUndeclaredJarDeps(runnerProject: Project, mavenId: String): FileCollection {
        if (mavenId.startsWith("androidx.wear:wear:")) {
            return runnerProject.files("wear/wear_stubs/com.google.android.wearable-stubs.jar")
        }
        return runnerProject.files()
    }

    /** Returns the source jar for the [mavenId]. */
    private fun getCompiledSources(runnerProject: Project, mavenId: String): File {
        val configuration =
            runnerProject.configurations.detachedConfiguration(
                runnerProject.dependencies.create(mavenId)
            )
        configuration.isTransitive = false
        return configuration.singleFile
    }

    /** Returns a file collection containing the unzipped sources from [compiledSources]. */
    private fun getSources(
        runnerProject: Project,
        mavenId: String,
        compiledSources: File
    ): FileCollection {
        val sanitizedMavenId = mavenId.replace(":", "-")
        @Suppress("DEPRECATION")
        val unzippedDir = File("${runnerProject.buildDir.path}/sources-unzipped/$sanitizedMavenId")
        runnerProject.copy { copySpec ->
            copySpec.from(runnerProject.zipTree(compiledSources))
            copySpec.into(unzippedDir)
        }
        return project.files(unzippedDir)
    }

    private fun getEmbeddedLibs(runnerProject: Project, mavenId: String): Collection<File> {
        val configuration =
            runnerProject.configurations.detachedConfiguration(
                runnerProject.dependencies.create(mavenId)
            )
        configuration.isTransitive = false

        val sanitizedMavenId = mavenId.replace(":", "-")
        @Suppress("DEPRECATION")
        val unzippedDir = File("${runnerProject.buildDir.path}/aars-unzipped/$sanitizedMavenId")
        runnerProject.copy { copySpec ->
            copySpec.from(runnerProject.zipTree(configuration.singleFile))
            copySpec.into(unzippedDir)
        }
        val libsDir = File(unzippedDir, "libs")
        if (libsDir.exists()) {
            return libsDir.listFiles()?.toList() ?: listOf()
        }

        return listOf()
    }
}
