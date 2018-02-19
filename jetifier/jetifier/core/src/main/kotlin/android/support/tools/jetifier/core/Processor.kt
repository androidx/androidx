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
import android.support.tools.jetifier.core.transform.metainf.MetaInfTransformer
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
class Processor private constructor (
    private val context: TransformationContext,
    private val transformers: List<Transformer>
) : ArchiveItemVisitor {

    companion object {
        private const val TAG = "Processor"

        /**
         * Value of "restrictToPackagePrefixes" config for reversed jetification.
         */
        private const val REVERSE_RESTRICT_TO_PACKAGE = "androidx"

        /**
         * Transformers to be used when refactoring general libraries.
         */
        private fun createTransformers(context: TransformationContext) = listOf(
            // Register your transformers here
            ByteCodeTransformer(context),
            XmlResourcesTransformer(context),
            ProGuardTransformer(context)
        )

        /**
         * Transformers to be used when refactoring the support library itself.
         */
        private fun createSLTransformers(context: TransformationContext) = listOf(
            // Register your transformers here
            ByteCodeTransformer(context),
            XmlResourcesTransformer(context),
            ProGuardTransformer(context),
            MetaInfTransformer(context)
        )

        /**
         * Creates a new instance of the [Processor].
         *
         * @param config Transformation configuration
         * @param reversedMode Whether the processor should run in reversed mode
         * @param rewritingSupportLib Whether we are rewriting the support library itself
         */
        fun createProcessor(
            config: Config,
            reversedMode: Boolean = false,
            rewritingSupportLib: Boolean = false
        ): Processor {
            var newConfig = config

            if (reversedMode) {
                newConfig = Config(
                    restrictToPackagePrefixes = listOf(REVERSE_RESTRICT_TO_PACKAGE),
                    rewriteRules = config.rewriteRules,
                    slRules = config.slRules,
                    pomRewriteRules = emptyList(), // TODO: This will need a new set of rules
                    typesMap = config.typesMap.reverseMapOrDie(),
                    proGuardMap = config.proGuardMap.reverseMapOrDie(),
                    packageMap = config.packageMap.reverse()
                )
            }

            val context = TransformationContext(newConfig, rewritingSupportLib, reversedMode)
            val transformers = if (rewritingSupportLib) {
                createSLTransformers(context)
            } else {
                createTransformers(context)
            }

            return Processor(context, transformers)
        }
    }

    /**
     * Transforms the input libraries given in [inputLibraries] using all the registered
     * [Transformer]s and returns a list of replacement libraries (the newly created libraries are
     * get stored into [outputPath]).
     *
     * Currently we have the following transformers:
     * - [ByteCodeTransformer] for java native code
     * - [XmlResourcesTransformer] for java native code
     * - [ProGuardTransformer] for PorGuard files
     *
     * @param outputPath Path where to save the generated library / libraries.
     * @param outputIsDir Whether the [outputPath] represents a single file or a directory. In case
     * of a single file, only one library can be given as input.
     * @param copyUnmodifiedLibsAlso Whether archives that were not modified should be also copied
     * to the given [outputPath]
     * @return List of files (existing and generated) that should replace the given [inputLibraries]
     */
    fun transform(inputLibraries: Set<File>,
            outputPath: Path,
            outputIsDir: Boolean,
            copyUnmodifiedLibsAlso: Boolean = true
    ): Set<File> {
        // 0) Validate arguments
        if (!outputIsDir && inputLibraries.size > 1) {
            throw IllegalArgumentException("Cannot process more than 1 library (" + inputLibraries +
                    ") when it is requested tha the destination (" + outputPath +
                    ") be made a file")
        }

        // 1) Extract and load all libraries
        val libraries = loadLibraries(inputLibraries)

        // 2) Search for POM files
        val pomFiles = scanPomFiles(libraries)

        // 3) Transform all the libraries
        libraries.forEach { transformLibrary(it) }

        if (context.errorsTotal() > 0) {
            throw IllegalArgumentException("There were ${context.errorsTotal()}" +
                " errors found during the remapping. Check the logs for more details.")
        }

        // TODO: Here we might need to modify the POM files if they point at a library that we have
        // just refactored.

        // 4) Transform the previously discovered POM files
        transformPomFiles(pomFiles)

        // 5) Repackage the libraries back to archive files
        val generatedLibraries = libraries
            .filter { copyUnmodifiedLibsAlso || it.wasChanged }
            .map {
                if (outputIsDir) {
                    it.writeSelfToDir(outputPath)
                } else {
                    it.writeSelfToFile(outputPath)
                }
            }
            .toSet()

        if (copyUnmodifiedLibsAlso) {
            return generatedLibraries
        }

        // 6) Create a set of files that should be removed (because they've been changed).
        val filesToRemove = libraries
            .filter { it.wasChanged }
            .map { it.relativePath.toFile() }
            .toSet()

        return inputLibraries.minus(filesToRemove).plus(generatedLibraries)
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
        val scanner = PomScanner(context.config)

        libraries.forEach { scanner.scanArchiveForPomFile(it) }
        if (scanner.wasErrorFound()) {
            throw IllegalArgumentException("At least one of the libraries depends on an older" +
                " version of support library. Check the logs for more details.")
        }

        return scanner.pomFiles
    }

    private fun transformPomFiles(files: List<PomDocument>) {
        files.forEach {
            it.applyRules(context.config.pomRewriteRules)
            it.saveBackToFileIfNeeded()
        }
    }

    private fun transformLibrary(archive: Archive) {
        Log.i(TAG, "Started new transformation")
        Log.i(TAG, "- Input file: %s", archive.relativePath)

        context.libraryName = archive.fileName
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