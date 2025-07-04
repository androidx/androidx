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
        val sourceSetElements =
            sourceSets.map { sourceSet ->
                createSourceSetElement(
                    sourceSet.sourceSetName,
                    sourceSet.dependsOnSourceSets,
                    sourceFiles(sourceSet.sourcePaths),
                    (sourceSet.dependencyClasspath + bootClasspath),
                    compiledSourceJar,
                )
            }
        val projectElement = createProjectElement(sourceSetElements)
        writeXml(projectElement, outputFile.writer())
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
    ): Element {
        val moduleElement = DocumentHelper.createElement("module")
        moduleElement.addAttribute("name", sourceSetName)
        if (sourceSetName == "androidMain") {
            moduleElement.addAttribute("android", "true")
        }
        // Currently, we only process JVM compilations in metalava, so it works to set just the JVM
        // platform for all modules (a common module should list all platforms it is used by).
        // TODO(b/407737495): use all platforms for non-JVM targets, don't hardcode the java version
        moduleElement.addAttribute("kotlinPlatforms", "JVM [1.8]")

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
