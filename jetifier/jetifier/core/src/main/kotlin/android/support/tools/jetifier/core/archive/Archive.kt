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

package android.support.tools.jetifier.core.archive

import android.support.tools.jetifier.core.utils.Log
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Represents an archive (zip, jar, aar ...)
 */
class Archive(
        override val relativePath: String,
        val files: List<ArchiveItem>)
    : ArchiveItem {

    companion object {
        /** Defines file extensions that are recognized as archives */
        val ARCHIVE_EXTENSIONS = listOf(".jar", ".zip", ".aar")

        const val TAG = "Archive"
    }

    override fun accept(visitor: ArchiveItemVisitor) {
        visitor.visit(this)
    }

    @Throws(IOException::class)
    fun writeGlobal(outputPath: Path) {
        Log.i(TAG, "Writing archive: %s", outputPath.toUri())

        Files.createFile(outputPath)
        val stream = BufferedOutputStream(FileOutputStream(outputPath.toFile()))
        writeSelfTo(stream)
        stream.close()
    }

    @Throws(IOException::class)
    override fun writeSelfTo(outputStream: OutputStream) {
        val out = ZipOutputStream(outputStream)

        for (file in files) {
            Log.d(TAG, "Writing file: %s", file.relativePath)

            val entry = ZipEntry(file.relativePath)
            out.putNextEntry(entry)
            file.writeSelfTo(out)
            out.closeEntry()
        }
        out.finish()
    }


    object Builder {

        @Throws(IOException::class)
        fun extract(mArchive: Path): Archive {
            Log.i(TAG, "Extracting: %s", mArchive.toUri())

            val inputStream = FileInputStream(mArchive.toFile())
            inputStream.use {
                val archive = extractArchive(it, "/")
                return archive
            }
        }

        @Throws(IOException::class)
        private fun extractArchive(inputStream: InputStream, relativePath: String): Archive {
            val zipIn = ZipInputStream(inputStream)
            val files = mutableListOf<ArchiveItem>()

            var entry: ZipEntry? = zipIn.nextEntry
            // iterates over entries in the zip file
            while (entry != null) {
                if (!entry.isDirectory) {
                    if (isArchive(entry)) {
                        Log.i(TAG, "Extracting nested: %s", entry.name)
                        files.add(extractArchive(zipIn, entry.name))
                    } else {
                        files.add(extractFile(zipIn, entry.name))
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            // Cannot close the zip stream at this moment as that would close also any parent zip
            // streams in case we are processing a nested archive.

            return Archive(relativePath, files.toList())
        }

        @Throws(IOException::class)
        private fun extractFile(zipIn: ZipInputStream, relativePath: String): ArchiveFile {
            Log.d(TAG, "Extracting archive: %s", relativePath)

            val data = zipIn.readBytes()
            return ArchiveFile(relativePath, data)
        }

        private fun isArchive(zipEntry: ZipEntry) : Boolean {
            return ARCHIVE_EXTENSIONS.any { zipEntry.name.endsWith(it) }
        }

    }
}