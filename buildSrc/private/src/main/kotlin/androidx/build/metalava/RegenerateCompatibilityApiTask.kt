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

import androidx.build.SoftwareType
import androidx.build.androidXExtension
import androidx.build.checkapi.ApiBaselinesLocation
import androidx.build.checkapi.ApiLocation
import androidx.build.checkapi.SourceSetInputs
import androidx.build.checkapi.setResolveReleaseApi
import androidx.build.dackka.DokkaUtils
import androidx.build.docs.ProjectStructureMetadata
import androidx.build.sources.SourceJarAttributeConfiguration.setResolveSources
import com.android.build.api.attributes.BuildTypeAttr
import java.io.File
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.named
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * Regenerates the signature files used for compatibility checks using the current version of
 * metalava, intended to be run after a metalava prebuilts update.
 *
 * There may be issues reported during generation, but the new signature files should be created.
 *
 * Run as `ALLOW_PUBLIC_REPOS=true ./gradlew regenerateCompatibilityApi --continue` so that the task
 * will work when the compatibility version is no longer in the internal prebuilts repo.
 */
@CacheableTask
internal abstract class RegenerateCompatibilityApiTask
@Inject
constructor(workerExecutor: WorkerExecutor, private val objectFactory: ObjectFactory) :
    SourceMetalavaTask(workerExecutor) {

    @get:Input abstract val generateRestrictToLibraryGroupAPIs: Property<Boolean>

    // Represented as output through getTaskOutputs
    @get:Internal abstract val outputApiLocation: Property<ApiLocation>

    @OutputFiles
    fun getTaskOutputs(): List<File> {
        val api = outputApiLocation.get()
        return listOf(api.publicApiFile, api.restrictedApiFile)
    }

    @TaskAction
    fun regenerate() {
        // Some projects have a repackaged jar, but metalava should use the source packages
        val compiledSources = compiledSources.files.single { it.name != "repackaged.jar" }
        val sourceSets =
            if (multiplatform.get()) {
                getMultiplatformSourceSets()
            } else {
                getSingleSourceSet()
            }
        val projectXml = File(temporaryDir, "project.xml")
        ProjectXml.create(sourceSets, bootClasspath.files, compiledSources, projectXml)

        generateApi(
            metalavaClasspath = metalavaClasspath,
            projectXml = projectXml,
            sourcePaths = sourceSets.flatMap { it.sourcePaths.files },
            compiledSources = compiledSources,
            apiLocation = outputApiLocation.get(),
            apiLintMode = ApiLintMode.Skip,
            includeRestrictToLibraryGroupApis = generateRestrictToLibraryGroupAPIs.get(),
            // Don't generate an API version history file
            apiLevelsArgs = emptyList(),
            kotlinSourceLevel = kotlinSourceLevel.get(),
            workerExecutor = workerExecutor,
            // Even if this is a KMP project, don't run multiplatform checks on it
            multiplatform = false,
            pathToManifest = null,
            hasJvmOrAndroidTarget = hasJvmOrAndroidTarget.get(),
            configFile = configFile.get().asFile,
        )
    }

    /**
     * Creates the [SourceSetInputs] needed for a KMP project. This just includes the source sets
     * relevant for the android or jvm compilation, since the signature file only contains APIs from
     * those source sets.
     */
    private fun getMultiplatformSourceSets(): List<SourceSetInputs> {
        val sourceDir = sourcePaths.single()
        // A KMP source jar has a file describing the relationships between source sets
        val metadataFile = File(sourceDir, "META-INF/kotlin-project-structure-metadata.json")
        val gson = DokkaUtils.createGson()
        val metadata = gson.fromJson(metadataFile.readText(), ProjectStructureMetadata::class.java)

        // API file generation happens for either the android or jvm source set of the project
        val mainSourceSet =
            metadata.sourceSets.singleOrNull { it.name == "androidMain" }
                ?: metadata.sourceSets.singleOrNull { it.name == "jvmMain" }
                ?: error("No androidMain or jvmMain source set found")

        // Only include source sets needed to compile the main source set
        val allSourceSets =
            mainSourceSet.dependencies.map { dependency ->
                metadata.sourceSets.single { it.name == dependency }
            } + mainSourceSet

        // Based on the main source set, use android/jvm as the platform for all source sets
        val platformTypes =
            setOf(
                when (mainSourceSet.name) {
                    "androidMain" -> KotlinPlatformType.androidJvm
                    else -> KotlinPlatformType.jvm
                }
            )

        return allSourceSets.map { sourceSet ->
            SourceSetInputs(
                sourceSetName = sourceSet.name,
                dependsOnSourceSets = sourceSet.dependencies,
                // Find the subdirectory for this source set
                sourcePaths = objectFactory.fileCollection().from(File(sourceDir, sourceSet.name)),
                // Use the same classpath for all source sets
                dependencyClasspath = dependencyClasspath,
                kotlinPlatforms = platformTypes,
            )
        }
    }

    /** Returns a list with one [SourceSetInputs] for a non-KMP project. */
    private fun getSingleSourceSet(): List<SourceSetInputs> {
        return listOf(
            SourceSetInputs(
                // Since there's just one source set, the name is arbitrary.
                sourceSetName = "main",
                // There are no other source sets to depend on.
                dependsOnSourceSets = emptyList(),
                sourcePaths = sourcePaths,
                dependencyClasspath = dependencyClasspath,
                kotlinPlatforms = setOf(KotlinPlatformType.androidJvm),
            )
        )
    }

    companion object {
        /**
         * Creates a [RegenerateCompatibilityApiTask] for the [project] if there is a signature file
         * used for compatibility checks based on a previous version.
         */
        fun configureTask(
            project: Project,
            apiLocation: ApiLocation,
            kotlinSourceLevel: Provider<KotlinVersion>,
            generateRestrictToLibraryGroupAPIs: Boolean,
            metalavaClasspath: FileCollection,
            hasAndroidTarget: Boolean,
            hasJvmOrAndroidTarget: Boolean,
            multiplatform: Boolean,
            targetsJavaConsumers: Provider<Boolean>,
            bootClasspath: FileCollection,
        ): TaskProvider<RegenerateCompatibilityApiTask>? {
            val groupId = project.group.toString()
            val artifactId = project.name
            // This task is only configured in an `afterEvaluate` block when the `type` has already
            // been eagerly accessed, so the `type` will be available.
            val type = project.androidXExtension.type.get()
            // Skip non-jvm/android projects, which don't have regular signature files, and projects
            // that don't release even though they run API tasks.
            if (
                !hasJvmOrAndroidTarget ||
                    type == SoftwareType.SNAPSHOT_ONLY_LIBRARY_WITH_API_TASKS ||
                    type == SoftwareType.SNAPSHOT_ONLY_TEST_LIBRARY_WITH_API_TASKS
            ) {
                return null
            }
            // Find the version used for compatibility checks. If it is the same as the current
            // version there's no need to regenerate.
            val compatVersion = apiLocation.version()!!
            if (compatVersion == project.version) {
                return null
            }

            val mavenId = "$groupId:$artifactId:$compatVersion"
            val dependency = project.dependencies.create(mavenId)

            // Find the jar file for the previous version.
            val compiledSources = project.configurations.detachedConfiguration(dependency)
            compiledSources.setResolveReleaseApi(project)

            // Find classpath dependencies for the previous version. Use `api`, `implementation`,
            // and `compileOnly` dependencies.
            val classpath =
                project.configurations
                    .detachedConfiguration(dependency)
                    .setResolveClasspath(
                        project,
                        Usage.JAVA_API,
                        hasAndroidTarget,
                    ) +
                    project.configurations
                        .detachedConfiguration(dependency)
                        .setResolveClasspath(
                            project,
                            Usage.JAVA_RUNTIME,
                            hasAndroidTarget,
                        ) +
                    project.files(getCompileOnlyDependencies(project, hasAndroidTarget))

            // Find and unzip the source jar for the previous version.
            val sourcePaths = getSourcePaths(project, dependency)

            return project.tasks.register(
                "regenerateCompatibilityApi",
                RegenerateCompatibilityApiTask::class.java,
            ) { task ->
                task.group = "API"
                task.description =
                    "Regenerates historic API .txt files using the " +
                        "corresponding prebuilt and the latest Metalava"
                task.kotlinSourceLevel.set(kotlinSourceLevel)
                task.generateRestrictToLibraryGroupAPIs.set(generateRestrictToLibraryGroupAPIs)
                task.outputApiLocation.set(apiLocation)
                task.compiledSources.from(project.files(compiledSources))
                task.dependencyClasspath.from(classpath)
                task.sourcePaths.from(sourcePaths)
                task.bootClasspath.from(bootClasspath)
                task.metalavaClasspath.from(metalavaClasspath)
                task.baselines.set(ApiBaselinesLocation.fromApiLocation(apiLocation))
                task.hasJvmOrAndroidTarget.set(hasJvmOrAndroidTarget)
                task.multiplatform.set(multiplatform)
                task.targetsJavaConsumers.set(targetsJavaConsumers)
            }
        }

        /** Returns a [FileCollection] with the source files to use for API generation. */
        private fun getSourcePaths(project: Project, dependency: Dependency): FileCollection {
            // Find the source jar from the previously released version
            val sourceJars = project.configurations.detachedConfiguration(dependency)
            sourceJars.setResolveSources(project)
            // Filter out samples source jars, which aren't used for API generation
            val sourceJar =
                sourceJars.incoming
                    .artifactView {}
                    .files
                    .elements
                    .map { jars ->
                        jars.single { jar -> !jar.asFile.name.contains("-samples") }.asFile
                    }
            // Unzip the source jar
            val unzippedSourcesTask =
                project.tasks.register(
                    "unzipRegenerateCompatibilityApiSources",
                    Sync::class.java,
                ) { task ->
                    task.into(
                        project.layout.buildDirectory.dir("regenerateCompatibilityApiSources")
                    )
                    task.from(project.zipTree(sourceJar))
                    // Exclude the generated R.java file because it is not included in the original
                    // API sources.
                    task.exclude { it.file.name == "R.java" }
                    task.duplicatesStrategy = DuplicatesStrategy.FAIL
                }
            return project.files(unzippedSourcesTask)
        }

        /**
         * Sets the [Configuration] to resolve classpath dependencies, returning a collection of jar
         * files.
         */
        private fun Configuration.setResolveClasspath(
            project: Project,
            usage: String,
            hasAndroidTarget: Boolean,
        ): FileCollection {
            isCanBeConsumed = false
            isTransitive = true
            isCanBeResolved = true
            addClasspathAttributes(project, usage, hasAndroidTarget)

            // For aar dependencies, load both the `jar` and `android-classes` artifacts, because
            // `android-classes` contains resources but `jar` contains kotlin module metadata.
            return incoming
                .artifactView {
                    it.attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
                }
                .files +
                incoming
                    .artifactView {
                        it.attributes.attribute(
                            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                            "android-classes",
                        )
                    }
                    .files
        }

        /**
         * Returns the `compileOnly` dependencies of the [project].
         *
         * Note that this returns the dependencies for the current version, which may be different
         * from the dependencies of the previously released version. However, `compileOnly`
         * dependencies aren't included in the metadata for published artifacts, so it isn't
         * possible to get the exact `compileOnly` dependencies for the previous release.
         */
        private fun getCompileOnlyDependencies(
            project: Project,
            hasAndroidTarget: Boolean,
        ): Provider<FileCollection> {
            return project.configurations
                .register("regenerateCompileOnlyDependencies") { configuration ->
                    project.configurations.findByName("compileOnly")?.let {
                        configuration.extendsFrom(it)
                    }
                    configuration.isCanBeResolved = true
                    configuration.isCanBeConsumed = false
                    configuration.isTransitive = false
                    configuration.addClasspathAttributes(project, Usage.JAVA_API, hasAndroidTarget)
                }
                .map { configuration ->
                    configuration.incoming
                        .artifactView {
                            it.attributes.attribute(
                                ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                                "android-classes",
                            )
                        }
                        .files
                }
        }

        /** Adds attributes to the [Configuration] to resolve classpath dependencies. */
        private fun Configuration.addClasspathAttributes(
            project: Project,
            usage: String,
            hasAndroidTarget: Boolean,
        ) {
            attributes {
                if (hasAndroidTarget) {
                    // Make sure to pick the Android artifacts, not the JVM artifacts
                    it.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
                    it.attribute(
                        TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
                        project.objects.named<TargetJvmEnvironment>(TargetJvmEnvironment.ANDROID),
                    )
                }
                it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named<Usage>(usage))
                it.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named<Category>(Category.LIBRARY),
                )
                it.attribute(
                    BuildTypeAttr.ATTRIBUTE,
                    project.objects.named<BuildTypeAttr>("release"),
                )
            }
        }
    }
}
