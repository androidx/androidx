/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core

import android.support.tools.jetifier.core.archive.Archive
import android.support.tools.jetifier.core.archive.ArchiveFile
import android.support.tools.jetifier.core.archive.ArchiveItemVisitor
import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.transform.TransformationContext
import android.support.tools.jetifier.core.transform.Transformer
import android.support.tools.jetifier.core.transform.bytecode.ByteCodeTransformer
import android.support.tools.jetifier.core.transform.pom.PomDocument
import android.support.tools.jetifier.core.transform.pom.PomScanner
import android.support.tools.jetifier.core.transform.proguard.ProGuardTransformer
import android.support.tools.jetifier.core.transform.resource.XmlResourcesTransformer
import android.support.tools.jetifier.core.utils.Log
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path

/**
 * The main entry point to the library. Extracts any given archive recursively and runs all
 * the registered [Transformer]s over the set and creates new archives that will contain the
 * transformed files.
 */
class Processor private constructor (private val config: Config) : ArchiveItemVisitor {

    companion object {
        private const val TAG = "Processor"

        /**
         * Value of "restrictToPackagePrefixes" config for reversed jetification.
         */
        private const val REVERSE_RESTRICT_TO_PACKAGE = "androidx/support"

        /**
         * Creates a new instance of the [Processor].
         */
        fun createProcessor(config: Config): Processor {
            return Processor(config)
        }

        /**
         * Create a new instance of the [Processor] to run in reversed mode.
         */
        fun createReversedProcessor(config: Config): Processor {
            val reversedConfig = Config(
                restrictToPackagePrefixes = listOf(REVERSE_RESTRICT_TO_PACKAGE),
                rewriteRules = emptyList(), // We don't need those
                pomRewriteRules = emptyList(), // TODO: This will need a new set of rules
                typesMap = config.typesMap.reverseMapOrDie(),
                proGuardMap = config.proGuardMap.reverseMapOrDie()
            )
            return Processor(reversedConfig)
        }
    }

    private val context = TransformationContext(config)

    private val transformers = listOf(
            // Register your transformers here
            ByteCodeTransformer(context),
            XmlResourcesTransformer(context),
            ProGuardTransformer(context)
    )

    /**
     * Transforms the input libraries given in [inputLibraries] using all the registered
     * [Transformer]s and returns new libraries stored in [outputPath].
     *
     * Currently we have the following transformers:
     * - [ByteCodeTransformer] for java native code
     * - [XmlResourcesTransformer] for java native code
     * - [ProGuardTransformer] for PorGuard files
     */
    fun transform(inputLibraries: Set<File>, outputPath: Path): TransformationResult {
        // 1) Extract and load all libraries
        val libraries = loadLibraries(inputLibraries)

        // 2) Search for POM files
        val pomFiles = scanPomFiles(libraries)

        // 3) Transform all the libraries
        libraries.forEach { transformLibrary(it) }

        if (context.wasErrorFound()) {
            throw IllegalArgumentException("There were ${context.mappingNotFoundFailuresCount}" +
                " errors found during the remapping. Check the logs for more details.")
        }

        // TODO: Here we might need to modify the POM files if they point at a library that we have
        // just refactored.

        // 4) Transform the previously discovered POM files
        transformPomFiles(pomFiles)

        // 5) Repackage the libraries back to archives
        val outputLibraries = libraries.map { it.writeSelfToDir(outputPath) }.toSet()

        // TODO: Filter out only the libraries that have been really changed
        return TransformationResult(
            filesToRemove = inputLibraries,
            filesToAdd = outputLibraries)
    }

    private fun loadLibraries(inputLibraries: Iterable<File>): List<Archive> {
        val libraries = mutableListOf<Archive>()
        for (library in inputLibraries) {
            if (!library.canRead()) {
                throw FileNotFoundException("Cannot open a library at '$library'")
            }

            libraries.add(Archive.Builder.extract(library))
        }
        return libraries.toList()
    }

    private fun scanPomFiles(libraries: List<Archive>): List<PomDocument> {
        val scanner = PomScanner(config)

        libraries.forEach { scanner.scanArchiveForPomFile(it) }
        if (scanner.wasErrorFound()) {
            throw IllegalArgumentException("At least one of the libraries depends on an older" +
                " version of support library. Check the logs for more details.")
        }

        return scanner.pomFiles
    }

    private fun transformPomFiles(files: List<PomDocument>) {
        files.forEach {
            it.applyRules(config.pomRewriteRules)
            it.saveBackToFileIfNeeded()
        }
    }

    private fun transformLibrary(archive: Archive) {
        Log.i(TAG, "Started new transformation")
        Log.i(TAG, "- Input file: %s", archive.relativePath)

        archive.accept(this)
    }

    override fun visit(archive: Archive) {
        archive.files.forEach { it.accept(this) }
    }

    override fun visit(archiveFile: ArchiveFile) {
        val transformer = transformers.firstOrNull { it.canTransform(archiveFile) }

        if (transformer == null) {
            Log.i(TAG, "[Skipped] %s", archiveFile.relativePath)
            return
        }

        Log.i(TAG, "[Applied: %s] %s", transformer.javaClass.simpleName, archiveFile.relativePath)
        transformer.runTransform(archiveFile)
    }
}