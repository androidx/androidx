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
import android.support.tools.jetifier.core.transform.Transformer
import android.support.tools.jetifier.core.transform.bytecode.ByteCodeTransformer
import android.support.tools.jetifier.core.transform.resource.XmlResourcesTransformer
import android.support.tools.jetifier.core.utils.Log
import java.nio.file.Files
import java.nio.file.Path

/**
 * The main entry point to the library. Extracts any given archive recursively and runs all
 * the registered [Transformer]s over the set and creates new archives that will contain the
 * transformed files.
 */
class Processor(config : Config) : ArchiveItemVisitor {

    private val tag : String = "Processor"

    private val transformers = listOf(
            // Register your transformers here
            ByteCodeTransformer(config),
            XmlResourcesTransformer(config)
    )

    /**
     * Transforms the input archive given in [inputPath] using all the registered [Transformer]s
     * and returns a new archive stored in [outputPath].
     *
     * Currently we have the following transformers:
     * - [ByteCodeTransformer] for java native code
     */
    fun transform(inputPath: Path, outputPath: Path) {
        if (!Files.isReadable(inputPath)) {
            Log.e(tag, "Cannot access the input file: '%s'", inputPath)
        }

        if (Files.exists(outputPath)) {
            Log.i(tag, "Deleting old output file")
            Files.delete(outputPath)
        }

        Log.i(tag, "Started new transformation")
        Log.i(tag, "- Input file: %s", inputPath)
        Log.i(tag, "- Output file: %s", outputPath)

        val archive = Archive.Builder.extract(inputPath)
        transform(archive)
        archive.writeGlobal(outputPath)
    }

    private fun transform(archive: Archive) {
        archive.accept(this)
    }

    override fun visit(archive: Archive) {
        archive.files.forEach{ it.accept(this) }
    }

    override fun visit(archiveFile: ArchiveFile) {
        val transformer = transformers.firstOrNull { it.canTransform(archiveFile) }

        if (transformer == null) {
            Log.i(tag, "[Skipped] %s", archiveFile.relativePath)
            return
        }

        Log.i(tag, "[Applied: %s] %s", transformer.javaClass.simpleName, archiveFile.relativePath)
        transformer.runTransform(archiveFile)
    }

}