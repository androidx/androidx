/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.build.dackka

import androidx.build.docs.ProjectStructureMetadata
import androidx.build.dokka.kmpDocs.DokkaAnalysisPlatform
import androidx.build.dokka.kmpDocs.DokkaInputModels
import androidx.build.dokka.kmpDocs.DokkaUtils
import com.google.gson.GsonBuilder
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

@CacheableTask
abstract class DackkaTask @Inject constructor(
    private val workerExecutor: WorkerExecutor,
    private val objects: ObjectFactory
) : DefaultTask() {

    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var projectStructureMetadataFile: File

    // Classpath containing Dackka
    @get:Classpath
    abstract val dackkaClasspath: ConfigurableFileCollection

    // Classpath containing dependencies of libraries needed to resolve types in docs
    @get:[InputFiles Classpath]
    lateinit var dependenciesClasspath: FileCollection

    // Directory containing the code samples from framework
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var frameworkSamplesDir: File

    // Directory containing the code samples
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var samplesDir: File

    // Directory containing the JVM source code for Dackka to process
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var jvmSourcesDir: File

    // Directory containing the multiplatform source code for Dackka to process
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var multiplatformSourcesDir: File

    // Directory containing the docs project and package-lists
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    lateinit var docsProjectDir: File

    // Location of generated reference docs
    @get:OutputDirectory
    lateinit var destinationDir: File

    // Set of packages to exclude for refdoc generation for all languages
    @Input
    lateinit var excludedPackages: Set<String>

    // Set of packages to exclude for Java refdoc generation
    @Input
    lateinit var excludedPackagesForJava: Set<String>

    // Set of packages to exclude for Kotlin refdoc generation
    @Input
    lateinit var excludedPackagesForKotlin: Set<String>

    // Maps to the system variable LIBRARY_METADATA_FILE containing artifactID and other metadata
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val libraryMetadataFile: RegularFileProperty

    // The base URL to create source links for classes, as a format string with placeholders for the
    // file path and qualified class name.
    @Input
    lateinit var baseSourceLink: String

    private fun sourceSets(): List<DokkaInputModels.SourceSet> {
        val externalDocs = externalLinks.map { (name, url) ->
            DokkaInputModels.GlobalDocsLink(
                url = url,
                packageListUrl =
                    "file://${docsProjectDir.toPath()}/package-lists/$name/package-list"
            )
        }
        val sourceLinks = listOf(
            DokkaInputModels.SrcLink(
                // This is part of dokka source links but isn't needed by dackka
                File("/"),
                baseSourceLink
            )
        )
        val gson = GsonBuilder().create()
        val multiplatformSourceSets = projectStructureMetadataFile
            .takeIf { it.exists() }
            ?.let { metadataFile ->
                val metadata = gson.fromJson(
                    metadataFile.readText(),
                    ProjectStructureMetadata::class.java
                )
                metadata.sourceSets.map { sourceSet ->
                    val analysisPlatform = DokkaAnalysisPlatform.valueOf(
                        sourceSet.analysisPlatform.uppercase()
                    )
                    val sourceDir = multiplatformSourcesDir.resolve(sourceSet.name)
                    DokkaInputModels.SourceSet(
                        id = sourceSetIdForSourceSet(sourceSet.name),
                        displayName = sourceSet.name,
                        analysisPlatform = analysisPlatform.jsonName,
                        sourceRoots = objects.fileCollection().from(sourceDir),
                        samples = objects.fileCollection(),
                        includes = objects.fileCollection().from(includesFiles(sourceDir)),
                        classpath = dependenciesClasspath,
                        externalDocumentationLinks = externalDocs,
                        dependentSourceSets = sourceSet.dependencies.map {
                            sourceSetIdForSourceSet(it)
                        },
                        noJdkLink = !analysisPlatform.androidOrJvm(),
                        noAndroidSdkLink = analysisPlatform != DokkaAnalysisPlatform.ANDROID,
                        noStdlibLink = false,
                        sourceLinks = sourceLinks
                    )
                }
        } ?: emptyList()
        return listOf(
            DokkaInputModels.SourceSet(
                id = sourceSetIdForSourceSet("main"),
                displayName = "main",
                analysisPlatform = "jvm",
                sourceRoots = objects.fileCollection().from(jvmSourcesDir),
                samples = objects.fileCollection().from(samplesDir, frameworkSamplesDir),
                includes = objects.fileCollection().from(includesFiles(jvmSourcesDir)),
                classpath = dependenciesClasspath,
                externalDocumentationLinks = externalDocs,
                dependentSourceSets = emptyList(),
                noJdkLink = false,
                noAndroidSdkLink = false,
                noStdlibLink = false,
                sourceLinks = sourceLinks
            )
        ) + multiplatformSourceSets
    }

    // Documentation for Dackka command line usage and arguments can be found at
    // https://kotlin.github.io/dokka/1.6.0/user_guide/cli/usage/
    private fun computeArguments(): File {
        val linksConfiguration = ""
        val jsonMap = mapOf(
            "moduleName" to "",
            "outputDir" to destinationDir.path,
            "globalLinks" to linksConfiguration,
            "sourceSets" to sourceSets(),
            "offlineMode" to "true",
            "noJdkLink" to "true"
            )
        val gson = DokkaUtils.createGson()
        val json = gson.toJson(jsonMap)
        val outputFile = File.createTempFile("dackkaArgs", ".json")
        outputFile.deleteOnExit()
        outputFile.writeText(json)
        return outputFile
    }

    @TaskAction
    fun generate() {
        runDackkaWithArgs(
            classpath = dackkaClasspath,
            argsFile = computeArguments(),
            workerExecutor = workerExecutor,
            excludedPackages = excludedPackages,
            excludedPackagesForJava = excludedPackagesForJava,
            excludedPackagesForKotlin = excludedPackagesForKotlin,
            libraryMetadataFile = libraryMetadataFile,
        )
    }

    companion object {
        private val externalLinks = mapOf(
            "coroutinesCore"
                to "https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core",
            "android" to "https://developer.android.com/reference",
            "guava" to "https://guava.dev/releases/18.0/api/docs/",
            "kotlin" to "https://kotlinlang.org/api/latest/jvm/stdlib/",
            "junit" to "https://junit.org/junit4/javadoc/4.12/",
            "okio" to "https://square.github.io/okio/3.x/okio/"
        )
    }
}

interface DackkaParams : WorkParameters {
    val args: ListProperty<String>
    val classpath: SetProperty<File>
    val excludedPackages: ListProperty<String>
    val excludedPackagesForJava: ListProperty<String>
    val excludedPackagesForKotlin: ListProperty<String>
    var libraryMetadataFile: Provider<RegularFile>
}

fun runDackkaWithArgs(
    classpath: FileCollection,
    argsFile: File,
    workerExecutor: WorkerExecutor,
    excludedPackages: Set<String>,
    excludedPackagesForJava: Set<String>,
    excludedPackagesForKotlin: Set<String>,
    libraryMetadataFile: Provider<RegularFile>,
) {
    val workQueue = workerExecutor.noIsolation()
    workQueue.submit(DackkaWorkAction::class.java) { parameters ->
        parameters.args.set(listOf(argsFile.path, "-loggingLevel", "WARN"))
        parameters.classpath.set(classpath)
        parameters.excludedPackages.set(excludedPackages)
        parameters.excludedPackagesForJava.set(excludedPackagesForJava)
        parameters.excludedPackagesForKotlin.set(excludedPackagesForKotlin)
        parameters.libraryMetadataFile = libraryMetadataFile
    }
}

abstract class DackkaWorkAction @Inject constructor(
    private val execOperations: ExecOperations
) : WorkAction<DackkaParams> {
    override fun execute() {
        execOperations.javaexec {
            it.mainClass.set("org.jetbrains.dokka.MainKt")
            it.args = parameters.args.get()
            it.classpath(parameters.classpath.get())

            // b/183989795 tracks moving these away from an environment variables
            it.environment("DEVSITE_TENANT", "androidx")
            it.environment("LIBRARY_METADATA_FILE", parameters.libraryMetadataFile.get().toString())

            if (parameters.excludedPackages.get().isNotEmpty())
                it.environment(
                    "DACKKA_EXCLUDED_PACKAGES",
                    parameters.excludedPackages.get().joinToString(",")
                )

            if (parameters.excludedPackagesForJava.get().isNotEmpty())
                it.environment(
                    "DACKKA_EXCLUDED_PACKAGES_JAVA",
                    parameters.excludedPackagesForJava.get().joinToString(",")
                )

            if (parameters.excludedPackagesForKotlin.get().isNotEmpty())
                it.environment(
                    "DACKKA_EXCLUDED_PACKAGES_KOTLIN",
                    parameters.excludedPackagesForKotlin.get().joinToString(",")
                )
        }
    }
}

private fun includesFiles(sourceRoot: File): List<File> {
    return sourceRoot.walkTopDown().filter {
        it.name.endsWith("documentation.md")
    }.toList()
}

private fun sourceSetIdForSourceSet(name: String): DokkaInputModels.SourceSetId {
    return DokkaInputModels.SourceSetId(scopeId = "androidx", sourceSetName = name)
}
