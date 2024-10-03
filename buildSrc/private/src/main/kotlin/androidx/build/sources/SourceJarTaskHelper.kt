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

package androidx.build.sources

import androidx.build.LazyInputsCopyTask
import androidx.build.capitalize
import androidx.build.dackka.DokkaAnalysisPlatform
import androidx.build.dackka.docsPlatform
import androidx.build.hasAndroidMultiplatformPlugin
import androidx.build.multiplatformExtension
import androidx.build.registerAsComponentForPublishing
import com.android.build.api.dsl.KotlinMultiplatformAndroidTarget
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.LibraryVariant
import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.attributes.Usage
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget

/** Sets up a source jar task for an Android library project. */
fun Project.configureSourceJarForAndroid(
    libraryVariant: LibraryVariant,
    samplesProjects: MutableCollection<Project>
) {
    val sourceJar =
        tasks.register("sourceJar${libraryVariant.name.capitalize()}", Jar::class.java) { task ->
            task.archiveClassifier.set("sources")
            task.from(libraryVariant.sources.java!!.all)
            task.exclude { it.file.path.contains("generated") }
            // Do not allow source files with duplicate names, information would be lost
            // otherwise.
            task.duplicatesStrategy = DuplicatesStrategy.FAIL
        }
    registerSourcesVariant(sourceJar)
    registerSamplesLibraries(samplesProjects)

    // b/272214715
    configurations.whenObjectAdded {
        if (it.name == "debugSourcesElements" || it.name == "releaseSourcesElements") {
            it.artifacts.whenObjectAdded { _ ->
                it.attributes.attribute(
                    DocsType.DOCS_TYPE_ATTRIBUTE,
                    project.objects.named(DocsType::class.java, "fake-sources")
                )
            }
        }
    }

    val disableNames =
        setOf(
            "releaseSourcesJar",
        )
    disableUnusedSourceJarTasks(disableNames)
}

fun Project.configureMultiplatformSourcesForAndroid(
    variantName: String,
    target: KotlinMultiplatformAndroidTarget,
    samplesProjects: MutableCollection<Project>
) {
    val sourceJar =
        tasks.register("sourceJar${variantName.capitalize()}", Jar::class.java) { task ->
            task.archiveClassifier.set("sources")
            target.mainCompilation().allKotlinSourceSets.forEach { sourceSet ->
                task.from(sourceSet.kotlin.srcDirs) { copySpec -> copySpec.into(sourceSet.name) }
            }
            task.duplicatesStrategy = DuplicatesStrategy.FAIL
        }
    registerSourcesVariant(sourceJar)
    registerSamplesLibraries(samplesProjects)
}

/** Sets up a source jar task for a Java library project. */
fun Project.configureSourceJarForJava(samplesProjects: MutableCollection<Project>) {
    val sourceJar =
        tasks.register("sourceJar", Jar::class.java) { task ->
            task.archiveClassifier.set("sources")

            // Do not allow source files with duplicate names, information would be lost otherwise.
            // Different sourceSets in KMP should use different platform infixes, see b/203764756
            task.duplicatesStrategy = DuplicatesStrategy.FAIL

            extensions.findByType(JavaPluginExtension::class.java)?.let { javaExtension ->
                // Since KotlinPlugin applies JavaPlugin, it's possible for JavaPlugin to exist, but
                // not to have "main".  Eventually, we should stop expecting to grab sourceSets by
                // name
                // (b/235828421)
                javaExtension.sourceSets.findByName("main")?.let {
                    task.from(it.allSource.sourceDirectories)
                }
            }

            extensions.findByType(KotlinMultiplatformExtension::class.java)?.let { kmpExtension ->
                for (sourceSetName in listOf("commonMain", "jvmMain")) {
                    kmpExtension.sourceSets.findByName(sourceSetName)?.let { sourceSet ->
                        task.from(sourceSet.kotlin.sourceDirectories)
                    }
                }
            }
        }
    registerSourcesVariant(sourceJar)
    registerSamplesLibraries(samplesProjects)

    val disableNames =
        setOf(
            "kotlinSourcesJar",
        )
    disableUnusedSourceJarTasks(disableNames)
}

fun Project.configureSourceJarForMultiplatform() {
    val kmpExtension =
        multiplatformExtension
            ?: throw GradleException(
                "Unable to find multiplatform extension while configuring multiplatform source JAR"
            )
    val metadataFile = layout.buildDirectory.file(PROJECT_STRUCTURE_METADATA_FILEPATH)
    val multiplatformMetadataTask =
        tasks.register("createMultiplatformMetadata", CreateMultiplatformMetadata::class.java) {
            it.metadataFile.set(metadataFile)
            it.sourceSetMetadata = project.provider { createSourceSetMetadata(kmpExtension) }
        }
    val sourceJar =
        tasks.register("multiplatformSourceJar", Jar::class.java) { task ->
            task.dependsOn(multiplatformMetadataTask)
            task.archiveClassifier.set("multiplatform-sources")

            // Do not allow source files with duplicate names, information would be lost otherwise.
            // Different sourceSets in KMP should use different platform infixes, see b/203764756
            task.duplicatesStrategy = DuplicatesStrategy.FAIL
            kmpExtension.targets
                // Filter out sources from stub targets as they are not intended to be documented
                .filterNot { it.name in setOfStubTargets }
                .flatMap { it.mainCompilation().allKotlinSourceSets }
                .toSet()
                .forEach { sourceSet ->
                    task.from(sourceSet.kotlin.srcDirs) { copySpec ->
                        copySpec.into(sourceSet.name)
                    }
                }
            task.metaInf.from(metadataFile)
        }
    registerMultiplatformSourcesVariant(sourceJar)

    val disableNames =
        setOf(
            "kotlinSourcesJar",
        )
    disableUnusedSourceJarTasks(disableNames)
}

fun Project.disableUnusedSourceJarTasks(disableNames: Set<String>) {
    project.tasks.configureEach { task ->
        if (disableNames.contains(task.name)) {
            task.enabled = false
        }
    }
}

internal val Project.multiplatformUsage
    get() = objects.named<Usage>("androidx-multiplatform-docs")

private fun Project.registerMultiplatformSourcesVariant(sourceJar: TaskProvider<Jar>) =
    registerSourcesVariant(kmpSourcesConfigurationName, sourceJar, multiplatformUsage)

private fun Project.registerSourcesVariant(sourceJar: TaskProvider<Jar>) =
    registerSourcesVariant(sourcesConfigurationName, sourceJar, objects.named(Usage.JAVA_RUNTIME))

private fun Project.registerSourcesVariant(
    configurationName: String,
    sourceJar: TaskProvider<Jar>,
    usage: Usage,
) =
    configurations.create(configurationName) { gradleVariant ->
        gradleVariant.isVisible = false
        gradleVariant.isCanBeResolved = false
        gradleVariant.attributes.attribute(Usage.USAGE_ATTRIBUTE, usage)
        gradleVariant.attributes.attribute(
            Category.CATEGORY_ATTRIBUTE,
            objects.named<Category>(Category.DOCUMENTATION)
        )
        gradleVariant.attributes.attribute(
            Bundling.BUNDLING_ATTRIBUTE,
            objects.named<Bundling>(Bundling.EXTERNAL)
        )
        gradleVariant.attributes.attribute(
            DocsType.DOCS_TYPE_ATTRIBUTE,
            objects.named<DocsType>(DocsType.SOURCES)
        )
        gradleVariant.outgoing.artifact(sourceJar)
        registerAsComponentForPublishing(gradleVariant)
    }

/**
 * Finds the main compilation for a source set, usually called 'main' but for android we need to
 * search for 'release' instead.
 */
private fun KotlinTarget.mainCompilation() =
    compilations.findByName(MAIN_COMPILATION_NAME) ?: compilations.getByName("release")

/**
 * Writes a metadata file to the given [metadataFile] location for all multiplatform Kotlin source
 * sets including their dependencies and analysisPlatform. This is consumed when we are reading
 * source JARs so that we can pass the correct inputs to Dackka.
 */
@CacheableTask
abstract class CreateMultiplatformMetadata : DefaultTask() {
    @Input lateinit var sourceSetMetadata: Provider<Map<String, Any>>

    @get:OutputFile abstract val metadataFile: RegularFileProperty

    @TaskAction
    fun execute() {
        metadataFile.get().asFile.apply {
            parentFile.mkdirs()
            createNewFile()
            val gson = GsonBuilder().setPrettyPrinting().create()
            writeText(gson.toJson(sourceSetMetadata.get()))
        }
    }
}

fun createSourceSetMetadata(kmpExtension: KotlinMultiplatformExtension): Map<String, Any> {
    val commonMain = kmpExtension.sourceSets.getByName("commonMain")
    val sourceSetsByName =
        mutableMapOf(
            "commonMain" to
                mapOf(
                    "name" to commonMain.name,
                    "dependencies" to commonMain.dependsOn.map { it.name }.sorted(),
                    "analysisPlatform" to DokkaAnalysisPlatform.COMMON.jsonName
                )
        )
    kmpExtension.targets.forEach { target ->
        // Skip adding entries for stub targets are they are not intended to be documented
        if (target.name in setOfStubTargets) return@forEach
        target.mainCompilation().allKotlinSourceSets.forEach {
            sourceSetsByName.getOrPut(it.name) {
                mapOf(
                    "name" to it.name,
                    "dependencies" to it.dependsOn.map { it.name }.sorted(),
                    "analysisPlatform" to target.docsPlatform().jsonName
                )
            }
        }
    }
    return mapOf("sourceSets" to sourceSetsByName.keys.sorted().map { sourceSetsByName[it] })
}

private fun Project.registerSamplesLibraries(samplesProjects: MutableCollection<Project>) =
    samplesProjects.forEach {
        dependencies.add("samples", it)
        // this publishing variant is used in non-KMP projects and non-KMP source jars of KMP
        // projects
        val publishingVariants = mutableListOf<String>()
        val hasAndroidMultiplatformPlugin = hasAndroidMultiplatformPlugin()
        publishingVariants.add(sourcesConfigurationName)
        project.multiplatformExtension?.let { ext ->
            val hasAndroidJvmTarget =
                ext.targets.any { target -> target.platformType == KotlinPlatformType.androidJvm }
            publishingVariants += kmpSourcesConfigurationName // used for KMP source jars
            // used for --android source jars of KMP projects
            if (hasAndroidMultiplatformPlugin) {
                publishingVariants += "$androidMultiplatformSourcesConfigurationName-published"
            } else if (hasAndroidJvmTarget) {
                publishingVariants += "release${sourcesConfigurationName.capitalize()}"
            }
        }
        updateCopySampleSourceJarsTaskWithVariant(publishingVariants)
    }

/**
 * Updates the published variants with the output of [LazyInputsCopyTask]. This function must be
 * called in the stack of [LibraryAndroidComponentsExtension.onVariants] as at that stage,
 * [AndroidXExtension.samplesProjects] would be populated.
 */
private fun Project.updateCopySampleSourceJarsTaskWithVariant(publishingVariants: List<String>) {
    val copySampleJarTask = tasks.named("copySampleSourceJars", LazyInputsCopyTask::class.java)
    val configuredVariants = mutableListOf<String>()
    configurations.configureEach { config ->
        if (config.name in publishingVariants) {
            // Register the sample source jar as an outgoing artifact of the publishing variant
            config.outgoing.artifact(copySampleJarTask.flatMap { it.destinationJar }) {
                // The only place where this classifier is load-bearing is when we filter sample
                // source jars out in our AndroidXDocsImplPlugin.configureUnzipJvmSourcesTasks
                it.classifier = "samples-sources"
            }
            configuredVariants.add(config.name)
        }
    }
    // Check that all the variants are configured because we only configure when the name matches
    // and could fail silently if we never see a matching configuration
    gradle.taskGraph.whenReady {
        if (!configuredVariants.containsAll(publishingVariants)) {
            val unconfiguredVariants =
                (publishingVariants.toSet() - configuredVariants.toSet()).joinToString(", ")
            throw GradleException(
                "Sample source jar tasks were not configured for $unconfiguredVariants"
            )
        }
    }
}

/**
 * Set of targets are there to serve as stubs, but are not expected to be consumed by library
 * consumers.
 */
private val setOfStubTargets = setOf("commonStubs", "jvmStubs", "linuxx64Stubs")

internal const val PROJECT_STRUCTURE_METADATA_FILENAME = "kotlin-project-structure-metadata.json"

private const val PROJECT_STRUCTURE_METADATA_FILEPATH =
    "project_structure_metadata/$PROJECT_STRUCTURE_METADATA_FILENAME"

internal const val sourcesConfigurationName = "sourcesElements"
private const val androidMultiplatformSourcesConfigurationName = "androidSourcesElements"
private const val kmpSourcesConfigurationName = "androidxSourcesElements"
