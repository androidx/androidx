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

package androidx.build.metalava

import androidx.build.checkapi.SourceSetInputs
import com.google.common.annotations.VisibleForTesting
import java.io.File
import java.io.Writer
import org.dom4j.DocumentHelper
import org.dom4j.Element
import org.dom4j.io.OutputFormat
import org.dom4j.io.XMLWriter
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal object ProjectXml {
    /**
     * Generates an XML file representing the structure of a KMP project, to be used by metalava.
     *
     * For more information see go/metalavatask-kmp-spec.
     */
    fun create(
        sourceSets: List<SourceSetInputs>,
        bootClasspath: Collection<File>,
        compiledSourceJar: File,
        outputFile: File,
    ) {
        // Compute the files for each source set initially so they can be checked multiple times
        // without recomputing.
        val sourceSetFiles =
            sourceSets.associate { sourceSet ->
                sourceSet.sourceSetName to sourceFiles(sourceSet.sourcePaths)
            }
        val filteredSourceSets = filterSourceSets(sourceSets, sourceSetFiles)
        val sourceSetElements =
            filteredSourceSets.map { sourceSet ->
                val sourceSetDependencies = sourceSet.dependencyClasspath.files
                // Include Android jars only for JVM and Android source sets (they are needed for
                // JVM because they provide the java standard libraries).
                val allDependencies =
                    if (
                        KotlinPlatformType.jvm in sourceSet.kotlinPlatforms ||
                            KotlinPlatformType.androidJvm in sourceSet.kotlinPlatforms
                    ) {
                        sourceSetDependencies + bootClasspath
                    } else {
                        sourceSetDependencies
                    }
                createSourceSetElement(
                    sourceSet.sourceSetName,
                    sourceSet.dependsOnSourceSets,
                    sourceSetFiles[sourceSet.sourceSetName]!!,
                    allDependencies,
                    compiledSourceJar,
                    sourceSet.kotlinPlatforms,
                )
            }
        val projectElement = createProjectElement(sourceSetElements)
        writeXml(projectElement, outputFile.writer())
    }

    /**
     * Returns a filtered list of source sets, removing those that have no source files and are not
     * depended on by any other source sets.
     */
    @VisibleForTesting
    fun filterSourceSets(
        sourceSets: List<SourceSetInputs>,
        sourceSetFiles: Map<String, List<File>>,
    ): List<SourceSetInputs> {
        val filtered =
            sourceSets.filter { sourceSet ->
                // Include any source sets with source files.
                sourceSetFiles[sourceSet.sourceSetName]!!.isNotEmpty() ||
                    // Include any source sets that are depended on by another source set.
                    sourceSets.any { otherSourceSet ->
                        sourceSet.sourceSetName in otherSourceSet.dependsOnSourceSets
                    } ||
                    // Include androidMain, even if it has no source files, to prevent errors that
                    // come from excluding the primary source set for the android compilation.
                    sourceSet.sourceSetName == "androidMain"
            }
        // If any source sets were filtered, do another pass as there may be source sets which were
        // previously depended on by filtered source sets which now can also be filtered.
        return if (filtered.size == sourceSets.size) {
            filtered
        } else {
            filterSourceSets(filtered, sourceSetFiles)
        }
    }

    /** Writes the [element] as XML to the [writer] and closes the stream. */
    @VisibleForTesting
    fun writeXml(element: Element, writer: Writer) {
        val document = DocumentHelper.createDocument(element)
        XMLWriter(writer, OutputFormat(/* indent= */ "  ", /* newlines= */ true)).apply {
            write(document)
            close()
        }
    }

    /** Constructs the XML [Element] for the project. */
    @VisibleForTesting
    fun createProjectElement(sourceSets: List<Element>): Element {
        val projectElement = DocumentHelper.createElement("project")

        // Setting "." for the root dir is equivalent to using the project directory path.
        val rootDirElement = DocumentHelper.createElement("root")
        rootDirElement.addAttribute("dir", ".")
        projectElement.add(rootDirElement)

        for (sourceSet in sourceSets) {
            projectElement.add(sourceSet)
        }

        return projectElement
    }

    /** Constructs the XML [Element] representing one source set. */
    @VisibleForTesting
    fun createSourceSetElement(
        sourceSetName: String,
        dependsOnSourceSets: Collection<String>,
        sourceFiles: Collection<File>,
        allDependencies: Collection<File>,
        compiledSourceJar: File,
        kotlinPlatforms: Set<KotlinPlatformType>,
    ): Element {
        val moduleElement = DocumentHelper.createElement("module")
        moduleElement.addAttribute("name", sourceSetName)
        if (sourceSetName == "androidMain") {
            moduleElement.addAttribute("android", "true")
        }
        // Create the /-separated string listing all Kotlin platform types that this source set can
        // be part of. The serializations are from the commented-out Kotlin compiler classes. The
        // compiler is a compile only dependency for this project, so to generate the strings
        // instead of hardcoding them it would need to be a runtime dependency as well.
        val kotlinPlatformStrings =
            kotlinPlatforms
                .mapNotNull {
                    when (it) {
                        // JvmPlatforms.defaultJvmPlatform
                        KotlinPlatformType.jvm,
                        KotlinPlatformType.androidJvm -> "JVM [1.8]"
                        // NativePlatforms.unspecifiedNativePlatform
                        KotlinPlatformType.native -> "Native []/Native [general]"
                        // JsPlatforms.defaultJsPlatform
                        KotlinPlatformType.js -> "JS []"
                        // WasmPlatforms.unspecifiedWasmPlatform
                        KotlinPlatformType.wasm -> "Wasm [general]"
                        else -> null
                    }
                }
                .toSet()
        moduleElement.addAttribute("kotlinPlatforms", kotlinPlatformStrings.joinToString("/"))

        for (dependsOn in dependsOnSourceSets) {
            val depElement = DocumentHelper.createElement("dep")
            depElement.addAttribute("module", dependsOn)
            depElement.addAttribute("kind", "dependsOn")
            moduleElement.add(depElement)
        }

        for (sourceFile in sourceFiles) {
            val srcElement = DocumentHelper.createElement("src")
            srcElement.addAttribute("file", sourceFile.absolutePath)
            moduleElement.add(srcElement)
        }

        for (dependency in allDependencies) {
            val (elementType, fileType) =
                when (dependency.extension) {
                    "jar" -> "classpath" to "jar"
                    "klib" -> "klib" to "file"
                    "aar" -> "classpath" to "aar"
                    "" -> "classpath" to "dir"
                    else -> continue
                }

            val dependencyElement = DocumentHelper.createElement(elementType)
            dependencyElement.addAttribute(fileType, dependency.absolutePath)
            moduleElement.add(dependencyElement)
        }

        // Adding the compiled sources of this project fixes issues where annotations on some
        // elements aren't registered by metalava (e.g. in :ink:ink-rendering).
        val jarElement = DocumentHelper.createElement("src")
        jarElement.addAttribute("jar", compiledSourceJar.absolutePath)
        moduleElement.add(jarElement)

        return moduleElement
    }

    /** Lists all of the files from [sources]. */
    private fun sourceFiles(sources: FileCollection): List<File> {
        return sources.files.flatMap { gatherFiles(it) }
    }

    /**
     * If [file] is a normal file, returns a list containing [file].
     *
     * If [file] is a directory, returns a list of all normal files recursively contained in the
     * directory.
     *
     * Otherwise, returns an empty list.
     */
    private fun gatherFiles(file: File): List<File> {
        return if (file.isFile) {
            listOf(file)
        } else if (file.isDirectory) {
            file.listFiles()?.flatMap { gatherFiles(it) } ?: emptyList()
        } else {
            emptyList()
        }
    }
}
