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
import com.google.gson.GsonBuilder
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor

@CacheableTask
abstract class DackkaTask
@Inject
constructor(private val workerExecutor: WorkerExecutor, private val objects: ObjectFactory) :
    DefaultTask() {

    @get:OutputFile abstract val argsJsonFile: RegularFileProperty

    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val projectStructureMetadataFile: RegularFileProperty

    // Classpath containing Dackka
    @get:Classpath abstract val dackkaClasspath: ConfigurableFileCollection

    // Classpath containing dependencies of libraries needed to resolve types in docs
    @get:[InputFiles Classpath]
    abstract val dependenciesClasspath: ConfigurableFileCollection

    // Directory containing the code samples from framework
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val frameworkSamplesDir: DirectoryProperty

    // Directory containing the code samples for non-KMP libraries
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val samplesJvmDir: DirectoryProperty

    // Directory containing the code samples for KMP libraries
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val samplesKmpDir: DirectoryProperty

    // Directory containing the JVM source code for Dackka to process
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val jvmSourcesDir: DirectoryProperty

    // Directory containing the multiplatform source code for Dackka to process
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val multiplatformSourcesDir: DirectoryProperty

    // Directory containing the package-lists
    @get:[InputFiles PathSensitive(PathSensitivity.RELATIVE)]
    abstract val projectListsDirectory: DirectoryProperty

    // Location of generated reference docs
    @get:OutputDirectory abstract val destinationDir: DirectoryProperty

    // Set of packages to exclude for refdoc generation for all languages
    @get:Input abstract val excludedPackages: SetProperty<String>

    // Set of packages to exclude for Java refdoc generation
    @get:Input abstract val excludedPackagesForJava: SetProperty<String>

    // Set of packages to exclude for Kotlin refdoc generation
    @get:Input abstract val excludedPackagesForKotlin: SetProperty<String>

    @get:Input abstract val annotationsNotToDisplay: ListProperty<String>

    @get:Input abstract val annotationsNotToDisplayJava: ListProperty<String>

    @get:Input abstract val annotationsNotToDisplayKotlin: ListProperty<String>

    @get:Input abstract val hidingAnnotations: ListProperty<String>

    @get:Input abstract val nullabilityAnnotations: ListProperty<String>

    // Version metadata for apiSince, only marked as @InputFiles if includeVersionMetadata is true
    @get:Internal abstract val versionMetadataFiles: ConfigurableFileCollection

    @InputFiles
    @PathSensitive(PathSensitivity.NONE)
    fun getOptionalVersionMetadataFiles(): ConfigurableFileCollection {
        return if (includeVersionMetadata) {
            versionMetadataFiles
        } else {
            objects.fileCollection()
        }
    }

    // Maps to the system variable LIBRARY_METADATA_FILE containing artifactID and other metadata
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val libraryMetadataFile: RegularFileProperty

    // The base URLs to create source links for classes, functions, and properties, respectively, as
    // format strings with placeholders for the file path and qualified class name, function name,
    // or property name.
    @get:Input abstract val baseSourceLink: Property<String>
    @get:Input abstract val baseFunctionSourceLink: Property<String>
    @get:Input abstract val basePropertySourceLink: Property<String>

    /**
     * Option for whether to include apiSince metadata in the docs. Defaults to including metadata.
     * Run with `--no-version-metadata` to avoid running `generateApi` before `docs`.
     */
    @get:Input
    @set:Option(
        option = "version-metadata",
        description = "Include added-in/deprecated-in API version metadata",
    )
    var includeVersionMetadata: Boolean = true

    private fun sourceSets(): List<DokkaInputModels.SourceSet> {
        fun getSampleSourceFileCollection(): FileCollection {
            // Filter out non-existent directories as Dackka crashes if you pass it in b/332262321
            val dirs =
                listOf(samplesJvmDir, samplesKmpDir, frameworkSamplesDir).mapNotNull {
                    if (it.get().asFile.exists()) it else null
                }
            return objects.fileCollection().from(dirs)
        }
        val externalDocs =
            externalLinks.map { (name, url) ->
                DokkaInputModels.GlobalDocsLink(
                    url = url,
                    packageListUrl =
                        "file://${
                            projectListsDirectory.get().asFile.absolutePath
                        }/$name/package-list",
                )
            }
        val gson = GsonBuilder().create()
        val multiplatformSourceSets =
            projectStructureMetadataFile
                .get()
                .asFile
                .takeIf { it.exists() }
                ?.let { metadataFile ->
                    val metadata =
                        gson.fromJson(metadataFile.readText(), ProjectStructureMetadata::class.java)
                    // Sort to ensure that child sourceSets come after their parents, b/404784813
                    metadata.sourceSets
                        .sortedWith(compareBy({ it.dependencies.size }, { it.name }))
                        .mapNotNull { sourceSet ->
                            val sourceDir =
                                multiplatformSourcesDir.get().asFile.resolve(sourceSet.name)
                            if (!sourceDir.exists()) return@mapNotNull null
                            val analysisPlatform =
                                DokkaAnalysisPlatform.valueOf(
                                    sourceSet.analysisPlatform.uppercase()
                                )
                            DokkaInputModels.SourceSet(
                                id = sourceSetIdForSourceSet(sourceSet.name),
                                displayName = sourceSet.name,
                                analysisPlatform = analysisPlatform.jsonName,
                                sourceRoots = objects.fileCollection().from(sourceDir),
                                // TODO(b/181224204): KMP samples aren't supported, dackka assumes
                                // all
                                // samples are in common
                                samples =
                                    if (analysisPlatform == DokkaAnalysisPlatform.COMMON) {
                                        getSampleSourceFileCollection()
                                    } else {
                                        objects.fileCollection()
                                    },
                                includes = objects.fileCollection().from(includesFiles(sourceDir)),
                                classpath = dependenciesClasspath,
                                externalDocumentationLinks = externalDocs,
                                dependentSourceSets =
                                    sourceSet.dependencies.map { sourceSetIdForSourceSet(it) },
                                noJdkLink = !analysisPlatform.androidOrJvm(),
                                noAndroidSdkLink =
                                    analysisPlatform != DokkaAnalysisPlatform.ANDROID,
                                noStdlibLink = false,
                                // Dackka source link configuration doesn't use the Dokka version
                                sourceLinks = emptyList(),
                            )
                        }
                } ?: emptyList()
        return listOf(
            DokkaInputModels.SourceSet(
                id = sourceSetIdForSourceSet("main"),
                displayName = "main",
                analysisPlatform = "jvm",
                sourceRoots = objects.fileCollection().from(jvmSourcesDir),
                samples = getSampleSourceFileCollection(),
                includes = objects.fileCollection().from(includesFiles(jvmSourcesDir.get().asFile)),
                classpath = dependenciesClasspath,
                externalDocumentationLinks = externalDocs,
                dependentSourceSets = emptyList(),
                noJdkLink = false,
                noAndroidSdkLink = false,
                noStdlibLink = false,
                // Dackka source link configuration doesn't use the Dokka version
                sourceLinks = emptyList(),
            )
        ) + multiplatformSourceSets
    }

    // Documentation for Dackka command line usage and arguments can be found at
    // https://kotlin.github.io/dokka/1.6.0/user_guide/cli/usage/
    // Documentation for the DevsitePlugin arguments can be found at
    // https://cs.android.com/androidx/platform/tools/dokka-devsite-plugin/+/master:src/main/java/com/google/devsite/DevsiteConfiguration.kt
    private fun computeArguments(): File {
        val gson = DokkaUtils.createGson()
        val linksConfiguration = ""
        val jsonMap =
            mapOf(
                "outputDir" to destinationDir.get().asFile.path,
                "globalLinks" to linksConfiguration,
                "sourceSets" to sourceSets(),
                "offlineMode" to "true",
                "noJdkLink" to "true",
                "pluginsConfiguration" to
                    listOf(
                        mapOf(
                            "fqPluginName" to "com.google.devsite.DevsitePlugin",
                            "serializationFormat" to "JSON",
                            // values is a JSON string
                            "values" to
                                gson.toJson(
                                    mapOf(
                                        "projectPath" to "androidx",
                                        "javaDocsPath" to "",
                                        "kotlinDocsPath" to "kotlin",
                                        "excludedPackages" to excludedPackages.get(),
                                        "excludedPackagesForJava" to excludedPackagesForJava.get(),
                                        "excludedPackagesForKotlin" to
                                            excludedPackagesForKotlin.get(),
                                        "libraryMetadataFilename" to
                                            libraryMetadataFile.get().toString(),
                                        "baseSourceLink" to baseSourceLink.get(),
                                        "baseFunctionSourceLink" to baseFunctionSourceLink.get(),
                                        "basePropertySourceLink" to basePropertySourceLink.get(),
                                        "annotationsNotToDisplay" to annotationsNotToDisplay.get(),
                                        "annotationsNotToDisplayJava" to
                                            annotationsNotToDisplayJava.get(),
                                        "annotationsNotToDisplayKotlin" to
                                            annotationsNotToDisplayKotlin.get(),
                                        "hidingAnnotations" to hidingAnnotations.get(),
                                        "versionMetadataFilenames" to getVersionMetadataFiles(),
                                        "validNullabilityAnnotations" to
                                            nullabilityAnnotations.get(),
                                    )
                                ),
                        )
                    ),
            )

        val json = gson.toJson(jsonMap)
        return argsJsonFile.get().asFile.apply { writeText(json) }
    }

    /**
     * If version metadata shouldn't be included in the docs, returns an empty list. Otherwise,
     * returns the list of version metadata files after checking if they're all JSON. If version
     * metadata does not exist for a project, it's possible that a configuration which isn't an
     * exact match of the version metadata attributes to be selected as version metadata.
     */
    private fun getVersionMetadataFiles(): List<File> {
        val (json, nonJson) =
            getOptionalVersionMetadataFiles().files.partition { it.extension == "json" }
        if (nonJson.isNotEmpty()) {
            logger.error(
                "The following were resolved as version metadata files but are not JSON files. " +
                    "If these projects do not have API tracking enabled (e.g. compiler plugin, " +
                    "annotation processor, proto), they should not be included in the docs. " +
                    "Remove the projects from `docs-public/build.gradle` and/or " +
                    "`docs-tip-of-tree/build.gradle`.\n" +
                    nonJson.joinToString("\n")
            )
        }
        return json
    }

    @TaskAction
    fun generate() {
        runDackkaWithArgs(
            classpath = dackkaClasspath,
            argsFile = computeArguments(),
            workerExecutor = workerExecutor,
        )
    }

    companion object {
        private val externalLinks =
            mapOf(
                "coroutinesCore" to "https://kotlinlang.org/api/kotlinx.coroutines/",
                "android" to "https://developer.android.com/reference",
                "guava" to "https://guava.dev/releases/18.0/api/docs/",
                "kotlin" to "https://kotlinlang.org/api/core/kotlin-stdlib/",
                "junit" to "https://junit.org/junit4/javadoc/4.12/",
                "okio" to "https://square.github.io/okio/3.x/okio/",
                "protobuf" to "https://protobuf.dev/reference/java/api-docs/",
                "kotlinpoet" to "https://square.github.io/kotlinpoet/1.x/kotlinpoet/",
                "skiko" to "https://jetbrains.github.io/skiko/",
                "reactivex" to "https://reactivex.io/RxJava/2.x/javadoc/",
                "reactivex-rxjava3" to "http://reactivex.io/RxJava/3.x/javadoc/",
                "grpc" to "https://grpc.github.io/grpc-java/javadoc/",
                // From developer.android.com/reference/com/google/android/play/core/package-list
                "play" to "https://developer.android.com/reference/",
                // From developer.android.com/reference/com/google/android/material/package-list
                "material" to "https://developer.android.com/reference",
                "okhttp3" to "https://square.github.io/okhttp/5.x/",
                "truth" to "https://truth.dev/api/0.41/",
                // From developer.android.com/reference/android/support/wearable/package-list
                "wearable" to "https://developer.android.com/reference/",
                // Filtered to just java.awt and javax packages (base java packages are included in
                // the android package-list)
                "javase8" to "https://docs.oracle.com/javase/8/docs/api/",
                "javaee7" to "https://docs.oracle.com/javaee%2F7%2Fapi%2F%2F",
                "findbugs" to "https://www.javadoc.io/doc/com.google.code.findbugs/jsr305/latest/",
                // All package-lists below were created manually
                "mlkit" to "https://developers.google.com/android/reference/",
                "dagger" to "https://dagger.dev/api/latest/",
                "reactivestreams" to
                    "https://www.reactive-streams.org/reactive-streams-1.0.4-javadoc/",
                "jetbrains-annotations" to
                    "https://javadoc.io/doc/org.jetbrains/annotations/latest/",
                "auto-value" to
                    "https://www.javadoc.io/doc/com.google.auto.value/auto-value/latest/",
                "robolectric" to "https://robolectric.org/javadoc/4.11/",
                "interactive-media" to
                    "https://developers.google.com/interactive-media-ads/docs/sdks/android/" +
                        "client-side/api/reference/com/google/ads/interactivemedia/v3",
                "errorprone" to "https://errorprone.info/api/latest/",
                "gms" to "https://developers.google.com/android/reference",
                "checkerframework" to "https://checkerframework.org/api/",
                "chromium" to
                    "https://developer.android.com/develop/connectivity/cronet/reference/",
                "jspecify" to "https://jspecify.dev/docs/api/",
            )
    }
}

interface DackkaParams : WorkParameters {
    val args: ListProperty<String>
    val classpath: SetProperty<File>
}

fun runDackkaWithArgs(classpath: FileCollection, argsFile: File, workerExecutor: WorkerExecutor) {
    val workQueue = workerExecutor.noIsolation()
    workQueue.submit(DackkaWorkAction::class.java) { parameters ->
        parameters.args.set(listOf(argsFile.path, "-loggingLevel", "WARN"))
        parameters.classpath.set(classpath)
    }
}

abstract class DackkaWorkAction @Inject constructor(private val execOperations: ExecOperations) :
    WorkAction<DackkaParams> {
    override fun execute() {
        execOperations.javaexec {
            it.mainClass.set("org.jetbrains.dokka.MainKt")
            it.args = parameters.args.get()
            it.classpath(parameters.classpath.get())
        }
    }
}

private fun includesFiles(sourceRoot: File): List<File> {
    return sourceRoot.walkTopDown().filter { it.name.endsWith("documentation.md") }.toList()
}

private fun sourceSetIdForSourceSet(name: String): DokkaInputModels.SourceSetId {
    return DokkaInputModels.SourceSetId(scopeId = "androidx", sourceSetName = name)
}
