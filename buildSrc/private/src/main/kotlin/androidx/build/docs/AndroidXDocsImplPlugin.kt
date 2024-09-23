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

import androidx.build.PROJECT_STRUCTURE_METADATA_FILENAME
import androidx.build.configureTaskTimeouts
import androidx.build.dackka.DackkaTask
import androidx.build.dackka.GenerateMetadataTask
import androidx.build.defaultAndroidConfig
import androidx.build.getAndroidJar
import androidx.build.getBuildId
import androidx.build.getCheckoutRoot
import androidx.build.getDistributionDirectory
import androidx.build.getKeystore
import androidx.build.getLibraryByName
import androidx.build.getSupportRootFolder
import androidx.build.metalava.versionMetadataUsage
import androidx.build.multiplatformUsage
import androidx.build.versionCatalog
import androidx.build.workaroundPrebuiltTakingPrecedenceOverProject
import com.android.build.api.attributes.BuildTypeAttr
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.LibraryPlugin
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
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.all
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.work.DisableCachingByDefault

/**
 * Plugin that allows to build documentation for a given set of prebuilt and tip of tree projects.
 */
abstract class AndroidXDocsImplPlugin : Plugin<Project> {
    lateinit var docsType: String
    lateinit var docsSourcesConfiguration: Configuration
    lateinit var multiplatformDocsSourcesConfiguration: Configuration
    lateinit var samplesSourcesConfiguration: Configuration
    lateinit var versionMetadataConfiguration: Configuration
    lateinit var dependencyClasspath: FileCollection

    @get:Inject abstract val archiveOperations: ArchiveOperations

    override fun apply(project: Project) {
        docsType = project.name.removePrefix("docs-")
        project.plugins.configureEach { plugin ->
            when (plugin) {
                is LibraryPlugin -> {
                    val libraryExtension = project.extensions.getByType<LibraryExtension>()
                    libraryExtension.compileSdk =
                        project.defaultAndroidConfig.latestStableCompileSdk
                    libraryExtension.buildToolsVersion =
                        project.defaultAndroidConfig.buildToolsVersion

                    // Use a local debug keystore to avoid build server issues.
                    val debugSigningConfig = libraryExtension.signingConfigs.getByName("debug")
                    debugSigningConfig.storeFile = project.getKeystore()
                    libraryExtension.buildTypes.configureEach { buildType ->
                        // Sign all the builds (including release) with debug key
                        buildType.signingConfig = debugSigningConfig
                    }
                }
            }
        }
        disableUnneededTasks(project)
        createConfigurations(project)
        val buildOnServer =
            project.tasks.register<DocsBuildOnServer>("buildOnServer") {
                buildId = getBuildId()
                docsType = this@AndroidXDocsImplPlugin.docsType
                distributionDirectory = project.getDistributionDirectory()
            }

        val unzippedDeprecatedSamplesSources =
            project.layout.buildDirectory.dir("unzippedDeprecatedSampleSources")
        val deprecatedUnzipSamplesTask =
            configureUnzipTask(
                project,
                "unzipSampleSourcesDeprecated",
                unzippedDeprecatedSamplesSources,
                samplesSourcesConfiguration
            )
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
                docsSourcesConfiguration
            )
        val configureMultiplatformSourcesTask =
            configureMultiplatformInputsTasks(
                project,
                unzippedMultiplatformSourcesDirectory,
                unzippedKmpSamplesSourcesDirectory,
                multiplatformDocsSourcesConfiguration,
                mergedProjectMetadata
            )

        configureDackka(
            project = project,
            unzippedJvmSourcesDirectory = unzippedJvmSourcesDirectory,
            unzippedMultiplatformSourcesDirectory = unzippedMultiplatformSourcesDirectory,
            unzipJvmSourcesTask = unzipJvmSourcesTask,
            configureMultiplatformSourcesTask = configureMultiplatformSourcesTask,
            unzippedDeprecatedSamplesSources = unzippedDeprecatedSamplesSources,
            unzipDeprecatedSamplesTask = deprecatedUnzipSamplesTask,
            unzippedJvmSamplesSources = unzippedJvmSamplesSourcesDirectory,
            unzipJvmSamplesTask = unzipJvmSamplesTask,
            unzippedKmpSamplesSources = unzippedKmpSamplesSourcesDirectory,
            dependencyClasspath = dependencyClasspath,
            buildOnServer = buildOnServer,
            docsConfiguration = docsSourcesConfiguration,
            multiplatformDocsConfiguration = multiplatformDocsSourcesConfiguration,
            mergedProjectMetadata = mergedProjectMetadata
        )

        project.configureTaskTimeouts()
        project.workaroundPrebuiltTakingPrecedenceOverProject()
    }

    /**
     * Creates and configures a task that will build a list of all sources for projects in
     * [docsConfiguration] configuration, resolve them and put them to [destinationDirectory].
     */
    private fun configureUnzipTask(
        project: Project,
        taskName: String,
        destinationDirectory: Provider<Directory>,
        docsConfiguration: Configuration
    ): TaskProvider<Sync> {
        return project.tasks.register(taskName, Sync::class.java) { task ->
            val sources = docsConfiguration.incoming.artifactView {}.files
            // Store archiveOperations into a local variable to prevent access to the plugin
            // during the task execution, as that breaks configuration caching.
            val localVar = archiveOperations
            task.from(
                sources.elements.map { jars ->
                    jars.map { jar ->
                        localVar.zipTree(jar).matching {
                            // Filter out files that documentation tools cannot process.
                            it.exclude("**/*.MF")
                            it.exclude("**/*.aidl")
                            it.exclude("**/META-INF/**")
                            it.exclude("**/OWNERS")
                            it.exclude("**/package.html")
                            it.exclude("**/*.md")
                        }
                    }
                }
            )
            task.into(destinationDirectory)
            // TODO(123020809) remove this filter once it is no longer necessary to prevent Dokka
            //  from failing
            val regex = Regex("@attr ref ([^*]*)styleable#([^_*]*)_([^*]*)$")
            task.filter { line -> regex.replace(line, "{@link $1attr#$3}") }
        }
    }

    /**
     * Creates and configures a task that builds a list of select sources from jars and places them
     * in [sourcesDestinationDirectory], partitioning samples into [samplesDestinationDirectory].
     *
     * This is a modified version of [configureUnzipTask], customized for Dackka usage.
     */
    private fun configureUnzipJvmSourcesTasks(
        project: Project,
        sourcesDestinationDirectory: Provider<Directory>,
        samplesDestinationDirectory: Provider<Directory>,
        docsConfiguration: Configuration
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
                    .map {
                        it.map { jar ->
                            localVar.zipTree(jar).matching { it.exclude("**/META-INF/MANIFEST.MF") }
                        }
                    }
            )
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
        mergedProjectMetadata: Provider<RegularFile>
    ): TaskProvider<MergeMultiplatformMetadataTask> {
        val tempMultiplatformMetadataDirectory =
            project.layout.buildDirectory.dir("tmp/multiplatformMetadataFiles")
        // unzip the sources into source folder and metadata files into folders per project
        val unzipMultiplatformSources =
            project.tasks.register(
                "unzipMultiplatformSources",
                UnzipMultiplatformSourcesTask::class.java
            ) {
                it.inputJars.set(
                    multiplatformDocsSourcesConfiguration.incoming.artifactView {}.files
                )
                it.metadataOutput.set(tempMultiplatformMetadataDirectory)
                it.sourceOutput.set(unzippedMultiplatformSourcesDirectory)
                it.samplesOutput.set(unzippedMultiplatformSamplesDirectory)
            }
        // merge all the metadata files from the individual project dirs
        return project.tasks.register(
            "mergeMultiplatformMetadata",
            MergeMultiplatformMetadataTask::class.java
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
        val samplesConfiguration =
            project.configurations.create("samples") {
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
                    project.objects.named<Usage>(Usage.JAVA_RUNTIME)
                )
                it.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named<Category>(Category.DOCUMENTATION)
                )
                it.attribute(
                    DocsType.DOCS_TYPE_ATTRIBUTE,
                    project.objects.named<DocsType>(DocsType.SOURCES)
                )
                it.attribute(
                    LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                    project.objects.named<LibraryElements>(LibraryElements.JAR)
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
                        project.objects.named<Category>(Category.DOCUMENTATION)
                    )
                    it.attribute(
                        DocsType.DOCS_TYPE_ATTRIBUTE,
                        project.objects.named<DocsType>(DocsType.SOURCES)
                    )
                    it.attribute(
                        LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        project.objects.named<LibraryElements>(LibraryElements.JAR)
                    )
                }
                configuration.extendsFrom(multiplatformDocsConfiguration)
            }
        samplesSourcesConfiguration =
            project.configurations.create("samples-sources") {
                it.setResolveSources()
                it.extendsFrom(samplesConfiguration)
            }

        versionMetadataConfiguration =
            project.configurations.create("library-version-metadata") {
                it.isTransitive = false
                it.isCanBeConsumed = false

                it.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.versionMetadataUsage)
                it.attributes.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named<Category>(Category.DOCUMENTATION)
                )
                it.attributes.attribute(
                    Bundling.BUNDLING_ATTRIBUTE,
                    project.objects.named<Bundling>(Bundling.EXTERNAL)
                )

                it.extendsFrom(docsConfiguration, multiplatformDocsConfiguration)
            }

        fun Configuration.setResolveClasspathForUsage(usage: String) {
            isCanBeConsumed = false
            attributes {
                it.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named<Usage>(usage))
                it.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    project.objects.named<Category>(Category.LIBRARY)
                )
                it.attribute(
                    BuildTypeAttr.ATTRIBUTE,
                    project.objects.named<BuildTypeAttr>("release")
                )
            }
            extendsFrom(
                docsConfiguration,
                samplesConfiguration,
                stubsConfiguration,
                docsWithoutApiSinceConfiguration
            )
        }

        // Build a compile & runtime classpaths for needed for documenting the libraries
        // from the configurations above.
        val docsCompileClasspath =
            project.configurations.create("docs-compile-classpath") {
                it.setResolveClasspathForUsage(Usage.JAVA_API)
            }
        val docsRuntimeClasspath =
            project.configurations.create("docs-runtime-classpath") {
                it.setResolveClasspathForUsage(Usage.JAVA_RUNTIME)
            }
        val kotlinDefaultCatalogVersion = androidx.build.KotlinTarget.DEFAULT.catalogVersion
        val kotlinLatest = project.versionCatalog.findVersion(kotlinDefaultCatalogVersion).get()
        listOf(docsCompileClasspath, docsRuntimeClasspath).forEach { config ->
            config.resolutionStrategy {
                it.eachDependency { details ->
                    if (details.requested.group == "org.jetbrains.kotlin") {
                        details.useVersion(kotlinLatest.requiredVersion)
                    }
                }
            }
        }
        dependencyClasspath =
            docsCompileClasspath.incoming
                .artifactView {
                    it.attributes.attribute(
                        Attribute.of("artifactType", String::class.java),
                        "android-classes"
                    )
                }
                .files +
                docsRuntimeClasspath.incoming
                    .artifactView {
                        it.attributes.attribute(
                            Attribute.of("artifactType", String::class.java),
                            "android-classes"
                        )
                    }
                    .files
    }

    private fun configureDackka(
        project: Project,
        unzippedJvmSourcesDirectory: Provider<Directory>,
        unzippedMultiplatformSourcesDirectory: Provider<Directory>,
        unzipJvmSourcesTask: TaskProvider<Sync>,
        configureMultiplatformSourcesTask: TaskProvider<MergeMultiplatformMetadataTask>,
        unzippedDeprecatedSamplesSources: Provider<Directory>,
        unzipDeprecatedSamplesTask: TaskProvider<Sync>,
        unzippedJvmSamplesSources: Provider<Directory>,
        unzipJvmSamplesTask: TaskProvider<Sync>,
        unzippedKmpSamplesSources: Provider<Directory>,
        dependencyClasspath: FileCollection,
        buildOnServer: TaskProvider<*>,
        docsConfiguration: Configuration,
        multiplatformDocsConfiguration: Configuration,
        mergedProjectMetadata: Provider<RegularFile>
    ) {
        val generatedDocsDir = project.layout.buildDirectory.dir("docs")

        val dackkaConfiguration =
            project.configurations.create("dackka") {
                it.dependencies.add(project.dependencies.create(project.getLibraryByName("dackka")))
                it.isCanBeConsumed = false
            }

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
                    File(project.getDistributionDirectory(), "dackkaArgs-${project.name}.json")
                )
                task.apply {
                    // Remove once there is property version of Copy#destinationDir
                    // Use samplesDir.set(unzipSamplesTask.flatMap { it.destinationDirectory })
                    // https://github.com/gradle/gradle/issues/25824
                    dependsOn(unzipJvmSourcesTask)
                    dependsOn(unzipJvmSamplesTask)
                    dependsOn(unzipDeprecatedSamplesTask)
                    dependsOn(configureMultiplatformSourcesTask)

                    description =
                        "Generates reference documentation using a Google devsite Dokka" +
                            " plugin. Places docs in $generatedDocsDir"
                    group = JavaBasePlugin.DOCUMENTATION_GROUP

                    dackkaClasspath.from(project.files(dackkaConfiguration))
                    destinationDir.set(generatedDocsDir)
                    frameworkSamplesDir.set(File(project.getSupportRootFolder(), "samples"))
                    samplesDeprecatedDir.set(unzippedDeprecatedSamplesSources)
                    samplesJvmDir.set(unzippedJvmSamplesSources)
                    samplesKmpDir.set(unzippedKmpSamplesSources)
                    jvmSourcesDir.set(unzippedJvmSourcesDirectory)
                    multiplatformSourcesDir.set(unzippedMultiplatformSourcesDirectory)
                    projectListsDirectory.set(
                        File(project.getSupportRootFolder(), "docs-public/package-lists")
                    )
                    dependenciesClasspath.from(
                        dependencyClasspath +
                            project.getAndroidJar() +
                            project.getExtraCommonDependencies()
                    )
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
                    versionMetadataFiles.from(
                        versionMetadataConfiguration.incoming.artifactView {}.files
                    )
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
                    val buildId = getBuildId()
                    archiveBaseName.set(baseName)
                    archiveVersion.set(buildId)
                    destinationDirectory.set(project.getDistributionDirectory())
                    group = JavaBasePlugin.DOCUMENTATION_GROUP

                    val filePath = "${project.getDistributionDirectory().canonicalPath}/"
                    val fileName = "$baseName-$buildId.zip"
                    val destinationFile = filePath + fileName
                    description =
                        "Zips Java and Kotlin documentation (generated via Dackka in the" +
                            " style of d.android.com) into $destinationFile"
                }
            }
        buildOnServer.configure { it.dependsOn(zipTask) }
    }

    /**
     * Replace all tests etc with empty task, so we don't run anything it is more effective then
     * task.enabled = false, because we avoid executing deps as well
     */
    private fun disableUnneededTasks(project: Project) {
        var reentrance = false
        project.tasks.whenTaskAdded { task ->
            if (
                task is Test ||
                    task.name.startsWith("assemble") ||
                    task.name == "lint" ||
                    task.name == "lintDebug" ||
                    task.name == "lintAnalyzeDebug" ||
                    task.name == "transformDexArchiveWithExternalLibsDexMergerForPublicDebug" ||
                    task.name == "transformResourcesWithMergeJavaResForPublicDebug" ||
                    task.name == "checkPublicDebugDuplicateClasses"
            ) {
                if (!reentrance) {
                    reentrance = true
                    project.tasks.named(task.name) {
                        it.actions = emptyList()
                        it.dependsOn(emptyList<Task>())
                    }
                    reentrance = false
                }
            }
        }
    }
}

@DisableCachingByDefault(because = "Doesn't benefit from caching")
open class DocsBuildOnServer : DefaultTask() {
    @Internal lateinit var docsType: String
    @Internal lateinit var buildId: String
    @Internal lateinit var distributionDirectory: File

    @[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    fun getRequiredFiles(): List<File> {
        return listOf(
            File(distributionDirectory, "docs-$docsType-$buildId.zip"),
        )
    }

    @TaskAction
    fun checkAllBuildOutputs() {
        val missingFiles = mutableListOf<String>()
        getRequiredFiles().forEach { file ->
            if (!file.exists()) {
                missingFiles.add(file.path)
            }
        }

        if (missingFiles.isNotEmpty()) {
            val missingFileString = missingFiles.reduce { acc, s -> "$acc, $s" }
            throw FileNotFoundException("buildOnServer required output missing: $missingFileString")
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
        "androidx.work.impl.*"
    )

// Set of packages to exclude from Java refdoc generation
private val hiddenPackagesJava =
    setOf(
        "androidx.*compose.*",
        "androidx.*glance.*",
        "androidx\\.tv\\..*",
    )

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
        "androidx.room.Ignore"
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
private val hiddenAnnotationsKotlin: List<String> = listOf("kotlin.ExtensionFunctionType")

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
data class ProjectStructureMetadata(var sourceSets: List<SourceSetMetadata>)

data class SourceSetMetadata(
    val name: String,
    val analysisPlatform: String,
    var dependencies: List<String>
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

private fun Project.getPrebuiltsExternalPath() =
    File(project.getCheckoutRoot(), "prebuilts/androidx/external/")

private val PLATFORMS =
    listOf("linuxx64", "macosarm64", "macosx64", "iosx64", "iossimulatorarm64", "iosarm64")

private fun Project.getExtraCommonDependencies(): FileCollection =
    files(
        arrayOf(
            File(
                getPrebuiltsExternalPath(),
                "org/jetbrains/kotlinx/kotlinx-coroutines-core/1.6.4/" +
                    "kotlinx-coroutines-core-1.6.4.jar"
            ),
            File(
                getPrebuiltsExternalPath(),
                "org/jetbrains/kotlinx/atomicfu/0.17.0/atomicfu-0.17.0.jar"
            ),
            File(getPrebuiltsExternalPath(), "com/squareup/okio/okio-jvm/3.1.0/okio-jvm-3.1.0.jar")
        ) +
            PLATFORMS.map {
                File(
                    getPrebuiltsExternalPath(),
                    "com/squareup/okio/okio-$it/3.1.0/okio-$it-3.1.0.klib"
                )
            }
    )
