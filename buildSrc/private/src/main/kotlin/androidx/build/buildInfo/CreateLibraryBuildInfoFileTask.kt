/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.buildInfo

import androidx.build.AndroidXExtension
import androidx.build.AndroidXMultiplatformExtension
import androidx.build.LibraryGroup
import androidx.build.docs.CheckTipOfTreeDocsTask.Companion.requiresDocs
import androidx.build.getBuildInfoDirectory
import androidx.build.getProjectZipPath
import androidx.build.getSupportRootFolder
import androidx.build.gitclient.getHeadShaProvider
import androidx.build.jetpad.LibraryBuildInfoFile
import com.google.common.annotations.VisibleForTesting
import com.google.gson.GsonBuilder
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyConstraint
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependencyConstraint
import org.gradle.api.internal.artifacts.ivyservice.projectmodule.ProjectComponentPublication
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.internal.publication.MavenPublicationInternal
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.configure
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

/**
 * This task generates a library build information file containing the artifactId, groupId, and
 * version of public androidx dependencies and release checklist of the library for consumption by
 * the Jetpack Release Service (JetPad).
 *
 * Example: If this task is configured
 * - for a project with group name "myGroup"
 * - on a variant with artifactId "myArtifact",
 * - and root project outDir is "out"
 * - and environment variable DIST_DIR is not set
 *
 * then the build info file will be written to
 * "out/dist/build-info/myGroup_myArtifact_build_info.txt"
 */
@DisableCachingByDefault(because = "uses git sha as input")
abstract class CreateLibraryBuildInfoFileTask : DefaultTask() {
    init {
        group = "Help"
        description = "Generates a file containing library build information serialized to json"
    }

    @get:OutputFile abstract val outputFile: Property<File>

    @get:Input abstract val artifactId: Property<String>

    @get:Input abstract val groupId: Property<String>

    @get:Input abstract val version: Property<String>

    @get:Optional @get:Input abstract val kotlinVersion: Property<String>

    @get:Input abstract val projectDir: Property<String>

    @get:Input abstract val commit: Property<String>

    @get:Input abstract val groupIdRequiresSameVersion: Property<Boolean>

    @get:Input abstract val projectZipPath: Property<String>

    @get:[Input Optional]
    abstract val dependencyList: ListProperty<LibraryBuildInfoFile.Dependency>

    @get:[Input Optional]
    abstract val dependencyConstraintList: ListProperty<LibraryBuildInfoFile.Dependency>

    /** the local project directory without the full framework/support root directory path */
    @get:Input abstract val projectSpecificDirectory: Property<String>

    /** Whether the project should be included in docs-public/build.gradle. */
    @get:Input abstract val shouldPublishDocs: Property<Boolean>

    /** Whether the artifact is from a KMP project. */
    @get:Input abstract val kmp: Property<Boolean>

    private fun writeJsonToFile(info: LibraryBuildInfoFile) {
        val resolvedOutputFile: File = outputFile.get()
        val outputDir = resolvedOutputFile.parentFile
        if (!outputDir.exists()) {
            if (!outputDir.mkdirs()) {
                throw RuntimeException("Failed to create " + "output directory: $outputDir")
            }
        }
        if (!resolvedOutputFile.exists()) {
            if (!resolvedOutputFile.createNewFile()) {
                throw RuntimeException("Failed to create output dependency dump file: $outputFile")
            }
        }

        // Create json object from the artifact instance
        val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
        val serializedInfo: String = gson.toJson(info)
        resolvedOutputFile.writeText(serializedInfo)
    }

    private fun resolveAndCollectDependencies(): LibraryBuildInfoFile {
        val libraryBuildInfoFile = LibraryBuildInfoFile()
        libraryBuildInfoFile.artifactId = artifactId.get()
        libraryBuildInfoFile.groupId = groupId.get()
        libraryBuildInfoFile.version = version.get()
        libraryBuildInfoFile.path = projectDir.get()
        libraryBuildInfoFile.sha = commit.get()
        libraryBuildInfoFile.groupIdRequiresSameVersion = groupIdRequiresSameVersion.get()
        libraryBuildInfoFile.projectZipPath = projectZipPath.get()
        libraryBuildInfoFile.kotlinVersion = kotlinVersion.orNull
        libraryBuildInfoFile.checks = ArrayList()
        libraryBuildInfoFile.dependencies =
            if (dependencyList.isPresent) ArrayList(dependencyList.get()) else ArrayList()
        libraryBuildInfoFile.dependencyConstraints =
            if (dependencyConstraintList.isPresent) ArrayList(dependencyConstraintList.get())
            else ArrayList()
        libraryBuildInfoFile.shouldPublishDocs = shouldPublishDocs.get()
        libraryBuildInfoFile.isKmp = kmp.get()
        return libraryBuildInfoFile
    }

    /**
     * Task: createLibraryBuildInfoFile Iterates through each configuration of the project and
     * builds the set of all dependencies. Then adds each dependency to the Artifact class as a
     * project or prebuilt dependency. Finally, writes these dependencies to a json file as a json
     * object.
     */
    @TaskAction
    fun createLibraryBuildInfoFile() {
        val resolvedArtifact = resolveAndCollectDependencies()
        writeJsonToFile(resolvedArtifact)
    }

    companion object {
        const val TASK_NAME = "createLibraryBuildInfoFiles"

        fun setup(
            project: Project,
            mavenGroup: LibraryGroup?,
            variant: VariantPublishPlan,
            shaProvider: Provider<String>,
            shouldPublishDocs: Boolean,
            isKmp: Boolean,
        ): TaskProvider<CreateLibraryBuildInfoFileTask> {
            return project.tasks.register(
                TASK_NAME + variant.taskSuffix,
                CreateLibraryBuildInfoFileTask::class.java
            ) { task ->
                val group = project.group.toString()
                val artifactId = variant.artifactId
                task.outputFile.set(
                    File(project.getBuildInfoDirectory(), "${group}_${artifactId}_build_info.txt")
                )
                task.artifactId.set(artifactId)
                task.groupId.set(group)
                task.version.set(project.version.toString())
                task.kotlinVersion.set(project.getKotlinPluginVersion())
                task.projectDir.set(
                    project.projectDir.absolutePath.removePrefix(
                        project.getSupportRootFolder().absolutePath
                    )
                )
                task.commit.set(shaProvider)
                task.groupIdRequiresSameVersion.set(mavenGroup?.requireSameVersion ?: false)
                task.projectZipPath.set(project.getProjectZipPath())

                // Note:
                // `project.projectDir.toString().removePrefix(project.rootDir.toString())`
                // does not work because the project rootDir is not guaranteed to be a
                // substring of the projectDir
                task.projectSpecificDirectory.set(
                    project.projectDir.absolutePath.removePrefix(
                        project.getSupportRootFolder().absolutePath
                    )
                )

                // lazily compute the task dependency list based on the variant dependencies.
                task.dependencyList.set(variant.dependencies.map { it.asBuildInfoDependencies() })
                task.dependencyConstraintList.set(
                    variant.dependencyConstraints.map { it.asBuildInfoDependencies() }
                )
                task.shouldPublishDocs.set(shouldPublishDocs)
                task.kmp.set(isKmp)
            }
        }

        fun List<Dependency>.asBuildInfoDependencies() =
            filter { it.group.isAndroidXDependency() }
                .map {
                    LibraryBuildInfoFile.Dependency().apply {
                        this.artifactId = it.name.toString()
                        this.groupId = it.group.toString()
                        this.version = it.version.toString()
                        this.isTipOfTree =
                            it is ProjectDependency || it is BuildInfoVariantDependency
                    }
                }
                .toHashSet()
                .sortedWith(compareBy({ it.groupId }, { it.artifactId }, { it.version }))

        @JvmName("dependencyConstraintsasBuildInfoDependencies")
        fun List<DependencyConstraint>.asBuildInfoDependencies() =
            filter { it.group.isAndroidXDependency() }
                .map {
                    LibraryBuildInfoFile.Dependency().apply {
                        this.artifactId = it.name.toString()
                        this.groupId = it.group.toString()
                        this.version = it.version.toString()
                        this.isTipOfTree = it is DefaultProjectDependencyConstraint
                    }
                }
                .toHashSet()
                .sortedWith(compareBy({ it.groupId }, { it.artifactId }, { it.version }))

        private fun String?.isAndroidXDependency() =
            this != null &&
                startsWith("androidx.") &&
                !startsWith("androidx.test") &&
                !startsWith("androidx.databinding")
    }
}

// Tasks that create a json files of a project's variant's dependencies
fun Project.addCreateLibraryBuildInfoFileTasks(
    androidXExtension: AndroidXExtension,
    androidXKmpExtension: AndroidXMultiplatformExtension,
) {
    androidXExtension.ifReleasing {
        configure<PublishingExtension> {
            // Unfortunately, dependency information is only available through internal API
            // (See https://github.com/gradle/gradle/issues/21345).
            publications.withType(MavenPublicationInternal::class.java).configureEach { mavenPub ->
                // java-gradle-plugin creates marker publications that are aliases of the
                // main publication.  We do not track these aliases.
                if (!mavenPub.isAlias) {
                    createTaskForComponent(
                        pub = mavenPub,
                        libraryGroup = androidXExtension.mavenGroup,
                        artifactId = mavenPub.artifactId,
                        shouldPublishDocs = androidXExtension.requiresDocs(),
                        isKmp = androidXKmpExtension.supportedPlatforms.isNotEmpty(),
                    )
                }
            }
        }
    }
}

private fun Project.createTaskForComponent(
    pub: ProjectComponentPublication,
    libraryGroup: LibraryGroup?,
    artifactId: String,
    shouldPublishDocs: Boolean,
    isKmp: Boolean,
) {
    val task =
        createBuildInfoTask(
            pub,
            libraryGroup,
            artifactId,
            getHeadShaProvider(project),
            shouldPublishDocs,
            isKmp
        )
    rootProject.tasks.named(CreateLibraryBuildInfoFileTask.TASK_NAME).configure {
        it.dependsOn(task)
    }
    addTaskToAggregateBuildInfoFileTask(task)
}

private fun Project.createBuildInfoTask(
    pub: ProjectComponentPublication,
    libraryGroup: LibraryGroup?,
    artifactId: String,
    shaProvider: Provider<String>,
    shouldPublishDocs: Boolean,
    isKmp: Boolean,
): TaskProvider<CreateLibraryBuildInfoFileTask> {
    val kmpTaskSuffix = computeTaskSuffix(name, artifactId)
    return CreateLibraryBuildInfoFileTask.setup(
        project = project,
        mavenGroup = libraryGroup,
        variant =
            VariantPublishPlan(
                artifactId = artifactId,
                taskSuffix = kmpTaskSuffix,
                dependencies =
                    pub.component.map { component ->
                        val usageDependencies =
                            component.usages.orEmpty().flatMap { it.dependencies }
                        usageDependencies + dependenciesOnKmpVariants(component)
                    },
                dependencyConstraints =
                    pub.component.map { component ->
                        component.usages.orEmpty().flatMap { it.dependencyConstraints }
                    }
            ),
        shaProvider = shaProvider,
        // There's a build_info file for each KMP platform, but only the artifact without a platform
        // suffix is listed in docs-public/build.gradle.
        shouldPublishDocs = shouldPublishDocs && kmpTaskSuffix == "",
        isKmp = isKmp,
    )
}

private fun dependenciesOnKmpVariants(component: SoftwareComponentInternal) =
    (component as? ComponentWithVariants)?.variants.orEmpty().mapNotNull {
        (it as? ComponentWithCoordinates)?.coordinates?.asDependency()
    }

private fun ModuleVersionIdentifier.asDependency() =
    BuildInfoVariantDependency(group, name, version)

class BuildInfoVariantDependency(group: String, name: String, version: String) :
    DefaultExternalModuleDependency(group, name, version)

// For examples, see CreateLibraryBuildInfoFileTaskTest
@VisibleForTesting
fun computeTaskSuffix(projectName: String, artifactId: String) =
    artifactId.substringAfter(projectName).split("-").joinToString("") { word ->
        word.replaceFirstChar { it.uppercase() }
    }
