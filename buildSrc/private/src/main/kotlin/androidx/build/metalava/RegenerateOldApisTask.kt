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
import androidx.build.checkapi.getApiFileVersion
import androidx.build.checkapi.getRequiredCompatibilityApiLocation
import androidx.build.checkapi.getVersionedApiLocation
import androidx.build.checkapi.isValidArtifactVersion
import androidx.build.getAndroidJar
import androidx.build.getCheckoutRoot
import androidx.build.java.JavaCompileInputs
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
        val inputs: JavaCompileInputs?
        try {
            inputs = getFiles(runnerProject, mavenId)
        } catch (e: TypedResolveException) {
            runnerProject.logger.info("Ignoring missing artifact $mavenId: $e")
            return
        }

        if (outputApiLocation.publicApiFile.exists()) {
            project.logger.lifecycle("Regenerating $mavenId")
            generateApi(
                project.getMetalavaClasspath(),
                inputs,
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

    private fun getFiles(runnerProject: Project, mavenId: String): JavaCompileInputs {
        val jars = getJars(runnerProject, mavenId)
        val sources = getSources(runnerProject, "$mavenId:sources")

        return JavaCompileInputs(
            sourcePaths = sources,
            // TODO(b/330721660) parse META-INF/kotlin-project-structure-metadata.json for
            // common sources
            commonModuleSourcePaths = project.files(),
            dependencyClasspath = jars,
            bootClasspath = project.getAndroidJar()
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

    private fun getSources(runnerProject: Project, mavenId: String): FileCollection {
        val configuration =
            runnerProject.configurations.detachedConfiguration(
                runnerProject.dependencies.create(mavenId)
            )
        configuration.isTransitive = false

        val sanitizedMavenId = mavenId.replace(":", "-")
        @Suppress("DEPRECATION")
        val unzippedDir = File("${runnerProject.buildDir.path}/sources-unzipped/$sanitizedMavenId")
        runnerProject.copy { copySpec ->
            copySpec.from(runnerProject.zipTree(configuration.singleFile))
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
