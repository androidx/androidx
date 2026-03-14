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

package androidx.build.docs

import androidx.build.KonanPrebuiltsSetup
import androidx.build.configureTaskTimeouts
import androidx.build.dackka.DackkaTask
import androidx.build.dackka.GenerateMetadataTask
import androidx.build.defaultAndroidConfig
import androidx.build.getAndroidJar
import androidx.build.getDistributionDirectory
import androidx.build.getLibraryClasspath
import androidx.build.getSupportRootFolder
import androidx.build.isIsolatedProjectsEnabled
import androidx.build.metalava.versionMetadataUsage
import androidx.build.multiplatformExtension
import androidx.build.sources.PROJECT_STRUCTURE_METADATA_FILENAME
import androidx.build.sources.multiplatformUsage
import androidx.build.versionCatalog
import androidx.build.workaroundAndroidXDependencyResolutions
import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.google.gson.GsonBuilder
import java.io.File
import java.io.FileNotFoundException
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.configuration.BuildFeatures
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.all
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetAttribute
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.toAttribute

/**
 * Plugin that allows to build documentation for a given set of prebuilt and tip of tree projects.
 */
abstract class AndroidXDocsImplPlugin : Plugin<Project> {
    lateinit var docsSourcesConfiguration: Configuration
    lateinit var multiplatformDocsSourcesConfiguration: Configuration
    lateinit var versionMetadataConfiguration: Configuration
    // Classpath for non-KMP projects
    lateinit var nonKmpDependencyClasspath: Provider<FileCollection>
    // Mapping from KMP target name to classpath for that target
    lateinit var kmpDependencyClasspathMap: MapProperty<String, FileCollection>

    @get:Inject abstract val archiveOperations: ArchiveOperations
    @get:Inject abstract val buildFeatures: BuildFeatures

    override fun apply(project: Project) {
        val docsType = project.name.removePrefix("docs-")
        // Configure this as a KMP project.
        KonanPrebuiltsSetup.configureKonanDirectory(project)
        configureTargets(project, docsType)

        disableUnneededTasks(project)
        createConfigurations(project)
        val buildOnServer =
            project.tasks.register<DocsBuildOnServer>("buildOnServer") {
                requiredFile.set(project.getDistributionDirectory().file("docs-$docsType.zip"))
            }

        val unzippedKmpSamplesSourcesDirectory =
            project.layout.buildDirectory.dir("unzippedMultiplatformSampleSources")
        val unzippedJvmSamplesSourcesDirectory =
            project.layout.buildDirectory.dir("unzippedJvmSampleSources")
        val unzippedJvmSourcesDirectory = project.layout.buildDirectory.dir("unzippedJvmSources")
        val unzippedMultiplatformSourcesDirectory =
            project.layout.buildDirectory.dir("unzippedMultiplatformSources")
        val mergedProjectMetadata =
            project.layout.buildDirectory.file(
                "project_metadata/$PROJECT_STRUCTURE_METADATA_FILENAME"
            )
        val (unzipJvmSourcesTask, unzipJvmSamplesTask) =
            configureUnzipJvmSourcesTasks(
                project,
                unzippedJvmSourcesDirectory,
                unzippedJvmSamplesSourcesDirectory,
                docsSourcesConfiguration,
            )
        val configureMultiplatformSourcesTask =
            configureMultiplatformInputsTasks(
                project,
                unzippedMultiplatformSourcesDirectory,
                unzippedKmpSamplesSourcesDirectory,
                multiplatformDocsSourcesConfiguration,
                mergedProjectMetadata,
            )

        configureDackka(
            project = project,
            unzippedJvmSourcesDirectory = unzippedJvmSourcesDirectory,
            unzippedMultiplatformSourcesDirectory = unzippedMultiplatformSourcesDirectory,
            unzipJvmSourcesTask = unzipJvmSourcesTask,
            configureMultiplatformSourcesTask = configureMultiplatformSourcesTask,
            unzippedJvmSamplesSources = unzippedJvmSamplesSourcesDirectory,
            unzipJvmSamplesTask = unzipJvmSamplesTask,
            unzippedKmpSamplesSources = unzippedKmpSamplesSourcesDirectory,
            nonKmpDependencyClasspath = nonKmpDependencyClasspath,
            kmpDependencyClasspathMap = kmpDependencyClasspathMap,
            buildOnServer = buildOnServer,
            docsConfiguration = docsSourcesConfiguration,
            multiplatformDocsConfiguration = multiplatformDocsSourcesConfiguration,
            mergedProjectMetadata = mergedProjectMetadata,
            docsType = docsType,
        )

        project.configureTaskTimeouts()
        project.workaroundAndroidXDependencyResolutions()
    }

    /**
     * Creates and configures a task that builds a list of select sources from jars and places them
     * in [sourcesDestinationDirectory], partitioning samples into [samplesDestinationDirectory].
     */
    private fun configureUnzipJvmSourcesTasks(
        project: Project,
        sourcesDestinationDirectory: Provider<Directory>,
        samplesDestinationDirectory: Provider<Directory>,
        docsConfiguration: Configuration,
    ): Pair<TaskProvider<Sync>, TaskProvider<Sync>> {
        val pairProvider =
            docsConfiguration.incoming
                .artifactView {}
                .files
                .elements
                .map {
                    it.map { it.asFile }.toSortedSet().partition { "samples" !in it.toString() }
                }
        return project.tasks.register("unzipJvmSources", Sync::class.java) { task ->
            // Store archiveOperations into a local variable to prevent access to the plugin
            // during the task execution, as that breaks configuration caching.
            val localVar = archiveOperations
            task.into(sourcesDestinationDirectory)
            task.from(
                pairProvider
                    .map { it.first }
                    .map { jars ->
                        jars.map { jar ->
                            localVar.zipTree(jar).matching { it.exclude("**/META-INF/MANIFEST.MF") }
                        }
                    }
            )
            task.rewriteSamplesTags()
            // Files with the same path in different source jars of the same library will lead to
            // some classes/methods not appearing in the docs.
            task.duplicatesStrategy = DuplicatesStrategy.WARN
        } to
            project.tasks.register("unzipSampleSources", Sync::class.java) { task ->
                // Store archiveOperations into a local variable to prevent access to the plugin
                // during the task execution, as that breaks configuration caching.
                val localVar = archiveOperations
                task.into(samplesDestinationDirectory)
                task.from(
                    pairProvider
                        .map { it.second }
                        .map {
                            it.map { jar ->
                                localVar.zipTree(jar).matching {
                                    it.exclude("**/META-INF/MANIFEST.MF")
                                }
                            }
                        }
                )
                // We expect this to happen when multiple libraries use the same sample, e.g.
                // paging.
                task.duplicatesStrategy = DuplicatesStrategy.INCLUDE
            }
    }

    /**
     * Creates multiple tasks to unzip multiplatform sources and merge their metadata to be used as
     * input for Dackka. Returns a single umbrella task which depends on the others.
     */
    private fun configureMultiplatformInputsTasks(
        project: Project,
        unzippedMultiplatformSourcesDirectory: Provider<Directory>,
        unzippedMultiplatformSamplesDirectory: Provider<Directory>,
        multiplatformDocsSourcesConfiguration: Configuration,
        mergedProjectMetadata: Provider<RegularFile>,
    ): TaskProvider<MergeMultiplatformMetadataTask> {
        val tempMultiplatformMetadataDirectory =
            project.layout.buildDirectory.dir("tmp/multiplatformMetadataFiles")
        // unzip the sources into source folder and metadata files into folders per project
        val unzipMultiplatformSources =
            project.tasks.register(
                "unzipMultiplatformSources",
                UnzipMultiplatformSourcesTask::class.java,
            ) {
                it.inputJars.set(multiplatformDocsSourcesConfiguration.incoming.files)
                it.metadataOutput.set(tempMultiplatformMetadataDirectory)
                it.sourceOutput.set(unzippedMultiplatformSourcesDirectory)
                it.samplesOutput.set(unzippedMultiplatformSamplesDirectory)
            }
        // merge all the metadata files from the individual project dirs
        return project.tasks.register(
            "mergeMultiplatformMetadata",
            MergeMultiplatformMetadataTask::class.java,
        ) {
            it.mergedProjectMetadata.set(mergedProjectMetadata)
            it.inputDirectory.set(unzipMultiplatformSources.flatMap { it.metadataOutput })
        }
    }

    /**
     * The following configurations are created to build a list of projects that need to be
     * documented and should be used from build.gradle of docs projects for the following:
     * - docs(project(":foo:foo") or docs("androidx.foo:foo:1.0.0") for docs sources
     * - samples(project(":foo:foo-samples") or samples("androidx.foo:foo-samples:1.0.0") for
     *   samples sources
     * - stubs(project(":foo:foo-stubs")) - stubs needed for a documented library
     */
    private fun createConfigurations(project: Project) {
        project.dependencies.components.all<SourcesVariantRule>()
        val docsConfiguration =
            project.configurations.create("docs") {
                it.isCanBeResolved = false
                it.isCanBeConsumed = false
            }
        // This exists for libraries that are deprecated or not hosted in the AndroidX repo
        val docsWithoutApiSinceConfiguration =
            project.configurations.create("docsWithoutApiSince") {
                it.isCanBeResolved = false
                it.isCanBeConsumed = false
            }
        val multiplatformDocsConfiguration =
            project.configurations.create("kmpDocs") {
                it.isCanBeResolved = false
                it.isCanBeConsumed = false
            }
        // b/491196586: a KMP project without a jvm/android target will not have version metadata
        val multiplatformDocsWithoutApiSinceConfiguration =
            project.configurations.create("kmpDocsWithoutApiSince") {
                it.isCanBeResolved = false
                it.isCanBeConsumed = false
            }
        val stubsConfiguration =
            project.configurations.create("stubs") {
                it.isCanBeResolved = false
                it.isCanBeConsumed = false
            }

        fun Configuration.setResolveSources() {
            isTransitive = false
            isCanBeConsumed = false
            attributes {
                it.attribute(
                    Usage.USAGE_ATTRIBUTE,
                    project.objects.named<Usage>(Usage.JAVA_RUNTIME),
                )
                it.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named<Category>(Category.DOCUMENTATION),
                )
                it.attribute(
                    DocsType.DOCS_TYPE_ATTRIBUTE,
                    project.objects.named<DocsType>(DocsType.SOURCES),
                )
                it.attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    project.objects.named<LibraryElements>(LibraryElements.JAR),
                )
            }
        }
        docsSourcesConfiguration =
            project.configurations.create("docs-sources") {
                it.setResolveSources()
                it.extendsFrom(docsConfiguration, docsWithoutApiSinceConfiguration)
            }
        multiplatformDocsSourcesConfiguration =
            project.configurations.create("multiplatform-docs-sources") { configuration ->
                configuration.isTransitive = false
                configuration.isCanBeConsumed = false
                configuration.attributes {
                    it.attribute(Usage.USAGE_ATTRIBUTE, project.multiplatformUsage)
                    it.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        project.objects.named<Category>(Category.DOCUMENTATION),
                    )
                    it.attribute(
                        DocsType.DOCS_TYPE_ATTRIBUTE,
                        project.objects.named<DocsType>(DocsType.SOURCES),
                    )
                    it.attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        project.objects.named<LibraryElements>(LibraryElements.JAR),
                    )
                }
                configuration.extendsFrom(
                    multiplatformDocsConfiguration,
                    multiplatformDocsWithoutApiSinceConfiguration,
                )
            }

        versionMetadataConfiguration =
            project.configurations.create("library-version-metadata") {
                it.isTransitive = false
                it.isCanBeConsumed = false

                it.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.versionMetadataUsage)
                it.attributes.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named<Category>(Category.DOCUMENTATION),
                )
                it.attributes.attribute(
                    Bundling.BUNDLING_ATTRIBUTE,
                    project.objects.named<Bundling>(Bundling.EXTERNAL),
                )

                it.extendsFrom(docsConfiguration, multiplatformDocsConfiguration)
            }

        val kotlinDefaultCatalogVersion = androidx.build.KotlinTarget.LATEST.catalogVersion
        val kotlinLatest = project.versionCatalog.findVersion(kotlinDefaultCatalogVersion).get()

        val kmpExtension = project.extensions.getByType<KotlinMultiplatformExtension>()

        // Use the android target to resolve the non-KMP classpath, so that for any KMP dependencies
        // of non-KMP projects with both android and jvmstubs artifacts the android variant is used.
        nonKmpDependencyClasspath =
            createClasspathConfigurationsForTarget(
                project = project,
                extendsFromConfigurations =
                    arrayOf(
                        docsConfiguration,
                        stubsConfiguration,
                        docsWithoutApiSinceConfiguration,
                    ),
                target = kmpExtension.androidLibraryTarget(),
                kotlinVersionConstraint = kotlinLatest,
                isKmp = false,
            )

        // Create mapping from target name to classpath for that target.
        kmpDependencyClasspathMap = project.objects.mapProperty<String, FileCollection>()
        kmpExtension.targets.configureEach { target ->
            val classpath =
                createClasspathConfigurationsForTarget(
                    project = project,
                    extendsFromConfigurations =
                        arrayOf(
                            multiplatformDocsConfiguration,
                            multiplatformDocsWithoutApiSinceConfiguration,
                            stubsConfiguration,
                        ),
                    target = target,
                    kotlinVersionConstraint = kotlinLatest,
                    isKmp = true,
                )
            // Add the classpath for the target to the mapping.
            kmpDependencyClasspathMap.put(target.name + "Main", classpath)
            // It is an error to configure separate jvm and desktop targets, so treat the jvm target
            // as both jvm and desktop.
            if (target.name == "jvm") {
                kmpDependencyClasspathMap.put("desktopMain", classpath)
            }
        }
    }

    /**
     * Configures the classpath for the given [target] extending from all configurations in
     * [extendsFromConfigurations], with both the API and runtime dependencies.
     */
    private fun createClasspathConfigurationsForTarget(
        project: Project,
        extendsFromConfigurations: Array<Configuration>,
        target: KotlinTarget,
        kotlinVersionConstraint: VersionConstraint,
        isKmp: Boolean,
    ): Provider<FileCollection> {
        // Find both the API and runtime dependencies. Technically only the API dependencies
        // should be required for docs, but projects don't always use the correct configuration.
        val targetApiClasspath =
            createClasspathConfigurationForTarget(
                project = project,
                extendsFromConfigurations = extendsFromConfigurations,
                target = target,
                usageDescription = "api",
                javaUsage = Usage.JAVA_API,
                kotlinUsage = KotlinUsages.KOTLIN_API,
                kotlinVersionConstraint = kotlinVersionConstraint,
                isKmp = isKmp,
            )
        val targetRuntimeClasspath =
            createClasspathConfigurationForTarget(
                project = project,
                extendsFromConfigurations = extendsFromConfigurations,
                target = target,
                usageDescription = "runtime",
                javaUsage = Usage.JAVA_RUNTIME,
                kotlinUsage = KotlinUsages.KOTLIN_RUNTIME,
                kotlinVersionConstraint = kotlinVersionConstraint,
                isKmp = isKmp,
            )
        return targetApiClasspath.zip(targetRuntimeClasspath) { api, runtime -> api + runtime }
    }

    /**
     * Configures the classpath for the given [target] extending from all configurations in
     * [extendsFromConfigurations].
     *
     * The [usageDescription] is used in the configuration name. If [target] is JVM or android, the
     * [javaUsage] is used as the usage attribute, otherwise [kotlinUsage] is used instead.
     */
    private fun createClasspathConfigurationForTarget(
        project: Project,
        extendsFromConfigurations: Array<Configuration>,
        target: KotlinTarget,
        usageDescription: String,
        javaUsage: String,
        kotlinUsage: String,
        kotlinVersionConstraint: VersionConstraint,
        isKmp: Boolean,
    ): Provider<FileCollection> {
        // Skip the common target, which is associated with the metadata compilation.
        if (target.platformType == KotlinPlatformType.common)
            return project.provider { project.files() }
        val isJvm =
            target.platformType == KotlinPlatformType.androidJvm ||
                target.platformType == KotlinPlatformType.jvm

        val kmpString = if (isKmp) "kmp" else "non-kmp"
        val configurationName = "docs-compile-classpath-${target.name}-$kmpString-$usageDescription"
        return project.configurations
            .register(configurationName) { config ->
                config.extendsFrom(*extendsFromConfigurations)
                config.isCanBeConsumed = false
                config.attributes {
                    it.attribute(KotlinPlatformType.attribute, target.platformType)
                    // Use the appropriate usage based on whether this is a jvm target.
                    val usage =
                        if (isJvm) {
                            javaUsage
                        } else {
                            kotlinUsage
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
                    // Add additional attributes based on the target.
                    target.attributes.keySet().forEach { key ->
                        if (key.type == String::class.java) {
                            val attributeValue = target.attributes.getAttribute(key)
                            @Suppress("UNCHECKED_CAST")
                            it.attribute(key as Attribute<String>, attributeValue as String)
                        }
                    }
                    // For wasm targets add an extra required attribute which isn't part of the
                    // target attribute set already.
                    if (target.platformType == KotlinPlatformType.wasm) {
                        (target as? KotlinWasmTargetDsl)?.wasmTargetType?.let { wasmTargetType ->
                            it.attribute(
                                KotlinWasmTargetAttribute.wasmTargetAttribute,
                                wasmTargetType.toAttribute(),
                            )
                        }
                    }
                }
                config.resolutionStrategy {
                    it.eachDependency { details ->
                        if (details.requested.group == "org.jetbrains.kotlin") {
                            details.useVersion(kotlinVersionConstraint.requiredVersion)
                        }
                    }
                }
            }
            .map { configuration ->
                classpathArtifactsFromConfiguration(configuration, isJvm = isJvm, isKmp = isKmp)
            }
    }

    /**
     * Creates a file collection with jar and klib dependencies resolved from the [configuration].
     *
     * When [isJvm] is true, this transforms aar dependencies into jars which dackka can process.
     *
     * When [isKmp] is true, classpath resolution is lenient because not every KMP dependency exists
     * for every target.
     */
    private fun classpathArtifactsFromConfiguration(
        configuration: Configuration,
        isJvm: Boolean,
        isKmp: Boolean,
    ): FileCollection {
        fun getArtifacts(androidArtifactType: String? = null): FileCollection {
            return configuration.incoming
                .artifactView {
                    // Set the configuration to lenient because not every KMP project will have all
                    // targets configured.
                    if (isKmp) {
                        it.isLenient = true
                    }
                    // Set the aar transformation as needed.
                    androidArtifactType?.let { androidArtifactType ->
                        it.attributes.attribute(
                            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
                            androidArtifactType,
                        )
                    }
                }
                .files
        }

        return if (isJvm) {
            // Dackka can't handle the aar dependencies, so this gets the jar from any aars (it is
            // important that this does not use the transformed android-classes jar, because that
            // jar does not contain kotlin module metadata) and the resource jar.
            getArtifacts("jar") + getArtifacts("r-class-jar")
        } else {
            getArtifacts()
        }
    }

    private fun configureDackka(
        project: Project,
        unzippedJvmSourcesDirectory: Provider<Directory>,
        unzippedMultiplatformSourcesDirectory: Provider<Directory>,
        unzipJvmSourcesTask: TaskProvider<Sync>,
        configureMultiplatformSourcesTask: TaskProvider<MergeMultiplatformMetadataTask>,
        unzippedJvmSamplesSources: Provider<Directory>,
        unzipJvmSamplesTask: TaskProvider<Sync>,
        unzippedKmpSamplesSources: Provider<Directory>,
        nonKmpDependencyClasspath: Provider<FileCollection>,
        kmpDependencyClasspathMap: Provider<Map<String, FileCollection>>,
        buildOnServer: TaskProvider<*>,
        docsConfiguration: Configuration,
        multiplatformDocsConfiguration: Configuration,
        mergedProjectMetadata: Provider<RegularFile>,
        docsType: String,
    ) {
        val generatedDocsDir = project.layout.buildDirectory.dir("docs")
        val generateMetadataTask =
            project.tasks.register("generateMetadata", GenerateMetadataTask::class.java) { task ->
                val artifacts = docsConfiguration.incoming.artifacts.resolvedArtifacts
                task.getArtifactIds().set(artifacts.map { result -> result.map { it.id } })
                task.getArtifactFiles().set(artifacts.map { result -> result.map { it.file } })
                val multiplatformArtifacts =
                    multiplatformDocsConfiguration.incoming.artifacts.resolvedArtifacts
                task
                    .getMultiplatformArtifactIds()
                    .set(multiplatformArtifacts.map { result -> result.map { it.id } })
                task
                    .getMultiplatformArtifactFiles()
                    .set(multiplatformArtifacts.map { result -> result.map { it.file } })
                task.destinationFile.set(getMetadataRegularFile(project))
            }

        val metricsFile = project.layout.buildDirectory.file("build-metrics.json")
        val projectName = project.name

        val dackkaTask =
            project.tasks.register("docs", DackkaTask::class.java) { task ->
                var taskStartTime: LocalDateTime? = null
                task.argsJsonFile.set(
                    project.getDistributionDirectory().file("dackkaArgs-${project.name}.json")
                )
                task.apply {
                    // Remove once there is property version of Copy#destinationDir
                    // Use samplesDir.set(unzipSamplesTask.flatMap { it.destinationDirectory })
                    // https://github.com/gradle/gradle/issues/25824
                    dependsOn(unzipJvmSourcesTask)
                    dependsOn(unzipJvmSamplesTask)
                    dependsOn(configureMultiplatformSourcesTask)

                    description =
                        "Generates reference documentation using a Google devsite Dokka" +
                            " plugin. Places docs in ${generatedDocsDir.get()}"
                    group = JavaBasePlugin.DOCUMENTATION_GROUP

                    dackkaClasspath.from(project.getLibraryClasspath("dackka"))
                    destinationDir.set(generatedDocsDir)
                    frameworkSamplesDir.set(File(project.getSupportRootFolder(), "samples"))
                    samplesJvmDir.set(unzippedJvmSamplesSources)
                    samplesKmpDir.set(unzippedKmpSamplesSources)
                    jvmSourcesDir.set(unzippedJvmSourcesDirectory)
                    multiplatformSourcesDir.set(unzippedMultiplatformSourcesDirectory)
                    projectListsDirectory.set(
                        File(project.getSupportRootFolder(), "docs-public/package-lists")
                    )
                    androidJars.setFrom(
                        project.getAndroidJar(
                            project.defaultAndroidConfig.latestStableCompileSdk,
                            project.defaultAndroidConfig.latestCompileSdkExtension,
                        )
                    )
                    nonKmpDependenciesClasspath.from(nonKmpDependencyClasspath)
                    kmpDependenciesClasspathMap.set(kmpDependencyClasspathMap)
                    excludedPackages.set(hiddenPackages.toSet())
                    excludedPackagesForJava.set(hiddenPackagesJava)
                    excludedPackagesForKotlin.set(emptySet())
                    libraryMetadataFile.set(generateMetadataTask.flatMap { it.destinationFile })
                    projectStructureMetadataFile.set(mergedProjectMetadata)
                    // See go/dackka-source-link for details on these links.
                    baseSourceLink.set("https://cs.android.com/search?q=file:%s+class:%s")
                    baseFunctionSourceLink.set(
                        "https://cs.android.com/search?q=file:%s+function:%s"
                    )
                    basePropertySourceLink.set("https://cs.android.com/search?q=file:%s+symbol:%s")
                    annotationsNotToDisplay.set(hiddenAnnotations)
                    annotationsNotToDisplayJava.set(hiddenAnnotationsJava)
                    annotationsNotToDisplayKotlin.set(hiddenAnnotationsKotlin)
                    hidingAnnotations.set(annotationsToHideApis)
                    nullabilityAnnotations.set(validNullabilityAnnotations)
                    versionMetadataFiles.from(versionMetadataConfiguration.incoming.files)
                    task.doFirst { taskStartTime = LocalDateTime.now() }
                    task.doLast {
                        val cpus =
                            try {
                                ProcessBuilder("lscpu")
                                    .start()
                                    .apply { waitFor(100L, TimeUnit.MILLISECONDS) }
                                    .inputStream
                                    .bufferedReader()
                                    .readLines()
                                    .filter { it.startsWith("CPU(s):") }
                                    .singleOrNull()
                                    ?.split(" ")
                                    ?.last()
                                    ?.toInt()
                            } catch (e: java.io.IOException) {
                                null
                            } // not running on linux
                        if (cpus != 64) { // Keep stddev of build metrics low b/334867245
                            println("$cpus cpus, so not storing build metrics.")
                            return@doLast
                        }
                        println("$cpus cpus, so storing build metrics.")
                        val taskEndTime = LocalDateTime.now()
                        val duration = Duration.between(taskStartTime, taskEndTime).toMillis()
                        metricsFile
                            .get()
                            .asFile
                            .writeText("{ \"${projectName}_docs_execution_duration\": $duration }")
                    }
                }
            }

        val zipTask =
            project.tasks.register("zipDocs", Zip::class.java) { task ->
                task.apply {
                    from(dackkaTask.flatMap { it.destinationDir })

                    val baseName = "docs-$docsType"
                    archiveBaseName.set(baseName)
                    destinationDirectory.set(project.getDistributionDirectory())
                    group = JavaBasePlugin.DOCUMENTATION_GROUP
                }
            }
        buildOnServer.configure { it.dependsOn(zipTask) }
    }

    /**
     * Replace all unneeded tasks with an empty task and disables them, to avoid executing deps as
     * well
     */
    private fun disableUnneededTasks(project: Project) {
        var reentrance = false
        project.tasks.configureEach { task ->
            if (
                task.name.startsWith("assemble") ||
                    task.name == "jsTest" ||
                    task.name == "lint" ||
                    task.name == "lintDebug" ||
                    task.name == "lintAnalyzeDebug" ||
                    task.name == "transformDexArchiveWithExternalLibsDexMergerForPublicDebug" ||
                    task.name == "transformResourcesWithMergeJavaResForPublicDebug" ||
                    task.name == "checkPublicDebugDuplicateClasses" ||
                    task.name == "wasmJsTestTestProductionExecutableCompileSync" ||
                    task.name == "wasmJsTestTestDevelopmentExecutableCompileSync"
            ) {
                if (!reentrance) {
                    reentrance = true
                    task.actions = emptyList()
                    task.dependsOn(emptyList<Task>())
                    task.enabled = false
                    reentrance = false
                }
            }
        }
    }

    private fun KotlinMultiplatformExtension.androidLibraryTarget():
        KotlinMultiplatformAndroidLibraryTarget {
        return extensions.getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
    }

    /** Configures all possible targets, so that all necessary classpaths will be generated. */
    @OptIn(ExperimentalWasmDsl::class)
    private fun configureTargets(project: Project, docsType: String) {
        val multiplatformExtension = project.multiplatformExtension!!

        val androidLibraryTarget = multiplatformExtension.androidLibraryTarget()
        androidLibraryTarget.compileSdk {
            version = release(project.defaultAndroidConfig.latestStableCompileSdk)
        }
        androidLibraryTarget.buildToolsVersion = project.defaultAndroidConfig.buildToolsVersion
        androidLibraryTarget.namespace = "androidx.docs.$docsType"

        multiplatformExtension.jvm()

        multiplatformExtension.androidNativeX64()
        multiplatformExtension.androidNativeX86()
        multiplatformExtension.androidNativeArm32()
        multiplatformExtension.androidNativeArm64()

        multiplatformExtension.mingwX64()
        multiplatformExtension.macosArm64()

        multiplatformExtension.iosArm64()
        multiplatformExtension.iosSimulatorArm64()

        multiplatformExtension.watchosArm32()
        multiplatformExtension.watchosArm64()
        multiplatformExtension.watchosDeviceArm64()
        multiplatformExtension.watchosSimulatorArm64()

        multiplatformExtension.tvosArm64()
        multiplatformExtension.tvosSimulatorArm64()

        multiplatformExtension.linuxArm64()
        multiplatformExtension.linuxX64()

        if (!buildFeatures.isIsolatedProjectsEnabled()) { // KT-80311
            multiplatformExtension.js { browser() }
            multiplatformExtension.wasmJs { browser() }
        }
    }
}

@DisableCachingByDefault(because = "Doesn't benefit from caching")
abstract class DocsBuildOnServer : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val requiredFile: RegularFileProperty

    @TaskAction
    fun checkAllBuildOutputs() {
        val file = requiredFile.get().asFile
        if (!file.exists()) {
            throw FileNotFoundException("buildOnServer required output missing: ${file.path}")
        }
    }
}

/**
 * Adapter rule to handles prebuilt dependencies that do not use Gradle Metadata (only pom). We
 * create a new variant sources that we can later use in the same way we do for tip of tree projects
 * and prebuilts with Gradle Metadata.
 */
abstract class SourcesVariantRule : ComponentMetadataRule {
    @get:Inject abstract val objects: ObjectFactory

    override fun execute(context: ComponentMetadataContext) {
        context.details.maybeAddVariant("sources", "runtime") {
            it.attributes {
                it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                it.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.DOCUMENTATION))
                it.attribute(DocsType.DOCS_TYPE_ATTRIBUTE, objects.named(DocsType.SOURCES))
            }
            it.withFiles {
                it.removeAllFiles()
                it.addFile("${context.details.id.name}-${context.details.id.version}-sources.jar")
            }
        }
    }
}

/**
 * Location of the library metadata JSON file that's used by Dackka, represented as a [RegularFile]
 */
private fun getMetadataRegularFile(project: Project): Provider<RegularFile> =
    project.layout.buildDirectory.file("AndroidXLibraryMetadata.json")

// List of packages to exclude from both Java and Kotlin refdoc generation
private val hiddenPackages =
    listOf(
        "androidx.camera.camera2.impl",
        "androidx.camera.camera2.internal.*",
        "androidx.camera.core.impl.*",
        "androidx.camera.core.internal.*",
        "androidx.core.internal",
        "androidx.preference.internal",
        "androidx.wear.internal.widget.drawer",
        "androidx.webkit.internal",
        "androidx.work.impl.*",
    )

// Set of packages to exclude from Java refdoc generation
private val hiddenPackagesJava =
    setOf("androidx.*compose.*", "androidx.*glance.*", "androidx\\.tv\\..*")

// List of annotations which should not be displayed in the docs
private val hiddenAnnotations: List<String> =
    listOf(
        // This information is compose runtime implementation details; not useful for most, those
        // who
        // would want it should look at source
        "androidx.compose.runtime.Stable",
        "androidx.compose.runtime.Immutable",
        "androidx.compose.runtime.ReadOnlyComposable",
        // This opt-in requirement is non-propagating so developers don't need to know about it
        // https://kotlinlang.org/docs/opt-in-requirements.html#non-propagating-opt-in
        "androidx.annotation.OptIn",
        "kotlin.OptIn",
        // This annotation is used mostly in paging, and was removed at the request of the paging
        // team
        "androidx.annotation.CheckResult",
        // This annotation is generated upstream. Dokka uses it for signature serialization. It
        // doesn't
        // seem useful for developers
        "kotlin.ParameterName",
        // This annotations is not useful for developers but right now is @ShowAnnotation?
        "kotlin.js.JsName",
        // This annotation is intended to target the compiler and is general not useful for devs.
        "java.lang.Override",
        // This annotation is used by the room processor and isn't useful for developers
        "androidx.room3.Ignore",
        // This is an internal annotation only used by the kotlin compiler.
        "kotlin.ExtensionFunctionType",
    )

val validNullabilityAnnotations =
    listOf(
        "org.jspecify.annotations.NonNull",
        "org.jspecify.annotations.Nullable",
        "androidx.annotation.Nullable",
        "android.annotation.Nullable",
        "androidx.annotation.NonNull",
        "android.annotation.NonNull",
        // Required by media3
        "org.checkerframework.checker.nullness.qual.Nullable",
    )

// Annotations which should not be displayed in the Kotlin docs, in addition to hiddenAnnotations
private val hiddenAnnotationsKotlin: List<String> = emptyList()

// Annotations which should not be displayed in the Java docs, in addition to hiddenAnnotations
private val hiddenAnnotationsJava: List<String> = emptyList()

// Annotations which mean the elements they are applied to should be hidden from the docs
private val annotationsToHideApis: List<String> =
    listOf(
        "androidx.annotation.RestrictTo",
        // Appears in androidx.test sources
        "dagger.internal.DaggerGenerated",
    )

/** Data class that matches JSON structure of kotlin source set metadata */
data class ProjectStructureMetadata(var sourceSets: List<SourceSetMetadata>) {
    /** Computes the source sets which are dependent on [name] (including [name]. */
    fun sourceSetsDependentOn(name: String): List<String> {
        return sourceSets
            .filter { otherSourceSet ->
                name == otherSourceSet.name || name in otherSourceSet.dependencies
            }
            .map { it.name }
    }
}

data class SourceSetMetadata(
    val name: String,
    val analysisPlatform: String,
    var dependencies: List<String>,
)

@CacheableTask
abstract class UnzipMultiplatformSourcesTask() : DefaultTask() {

    @get:Classpath abstract val inputJars: Property<FileCollection>

    @get:OutputDirectory abstract val metadataOutput: DirectoryProperty

    @get:OutputDirectory abstract val sourceOutput: DirectoryProperty

    @get:OutputDirectory abstract val samplesOutput: DirectoryProperty

    @get:Inject abstract val fileSystemOperations: FileSystemOperations

    @get:Inject abstract val archiveOperations: ArchiveOperations

    @TaskAction
    fun execute() {
        listOf(sourceOutput, samplesOutput).map { it.get().asFile.deleteRecursively() }
        val (sources, samples) =
            inputJars
                .get()
                .associate { it.name to archiveOperations.zipTree(it) }
                .toSortedMap()
                // Now that we publish sample jars, they can get confused with normal source
                // jars. We want to handle sample jars separately, so filter by the name.
                .partition { name -> "samples" !in name }

        fileSystemOperations.sync {
            it.duplicatesStrategy = DuplicatesStrategy.FAIL
            it.from(sources.values)
            it.into(sourceOutput)
            it.exclude("META-INF/*")
            // TODO(b/418945918): Remove when the files below are deduped:
            // benchmark/benchmark-traceprocessor/src/androidMain/kotlin/perfetto/protos/package-info.java
            // tracing/tracing-wire/src/androidMain/kotlin/perfetto/protos/package-info.java
            var seenPath = false
            it.eachFile { file ->
                val relPath = file.relativePath.pathString
                if (relPath == "androidMain/perfetto/protos/package-info.java") {
                    if (seenPath) {
                        file.exclude()
                    }
                    seenPath = true
                }
            }
            it.rewriteSamplesTags()
        }

        fileSystemOperations.sync {
            // Some libraries share samples, e.g. paging. This can be an issue if and only if the
            // consumer libraries have pinned samples version or are not in an atomic group.
            // We don't have anything matching this case now, but should enforce better. b/334825580
            it.duplicatesStrategy = DuplicatesStrategy.INCLUDE
            it.from(samples.values)
            it.into(samplesOutput)
            it.exclude("META-INF/*")
        }
        sources.forEach { (name, fileTree) ->
            fileSystemOperations.sync {
                it.from(fileTree)
                it.into(metadataOutput.file(name))
                it.include("META-INF/*")
            }
        }
    }
}

/**
 * To work around a parser issue with `@sample` where when the tag is used in the middle of a kdoc
 * any links after the sample do not resolve (see b/427708573), rewrite `@sample` tags to
 * `@author #@sample`. The `@author` tag is not supported by dackka, so as a workaround for the
 * samples issue it replaces any author tags with samples.
 */
internal fun CopySpec.rewriteSamplesTags() {
    filter { line -> line.replace(" * @sample ", " * @author #@sample ") }
}

private fun <K, V> Map<K, V>.partition(condition: (K) -> Boolean): Pair<Map<K, V>, Map<K, V>> =
    this.toList().partition { (k, _) -> condition(k) }.let { it.first.toMap() to it.second.toMap() }

/** Merges multiplatform metadata files created by [CreateMultiplatformMetadata] */
@CacheableTask
abstract class MergeMultiplatformMetadataTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDirectory: DirectoryProperty
    @get:OutputFile abstract val mergedProjectMetadata: RegularFileProperty

    @TaskAction
    fun execute() {
        val mergedMetadata = ProjectStructureMetadata(sourceSets = listOf())
        inputDirectory
            .get()
            .asFile
            .walkTopDown()
            .filter { file -> file.name == PROJECT_STRUCTURE_METADATA_FILENAME }
            .forEach { metaFile ->
                val gson = GsonBuilder().create()
                val metadata =
                    gson.fromJson(metaFile.readText(), ProjectStructureMetadata::class.java)
                mergedMetadata.merge(metadata)
            }
        val gson = GsonBuilder().setPrettyPrinting().create()
        // Sort sourceSets to ensure that child sourceSets come after their parents, b/404784813
        // Also ensure deterministic order--mergedMetadata.merge() uses .toSet() to deduplicate.
        mergedMetadata.sourceSets =
            mergedMetadata.sourceSets.sortedWith(compareBy({ it.dependencies.size }, { it.name }))
        val json = gson.toJson(mergedMetadata)
        mergedProjectMetadata.get().asFile.apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(json)
        }
    }

    private fun ProjectStructureMetadata.merge(metadata: ProjectStructureMetadata) {
        val originalSourceSets = this.sourceSets
        metadata.sourceSets.forEach { newSourceSet ->
            val existingSourceSet = originalSourceSets.find { it.name == newSourceSet.name }
            if (existingSourceSet != null) {
                existingSourceSet.dependencies =
                    (newSourceSet.dependencies + existingSourceSet.dependencies).toSet().toList()
            } else {
                sourceSets += listOf(newSourceSet)
            }
        }
    }
}
