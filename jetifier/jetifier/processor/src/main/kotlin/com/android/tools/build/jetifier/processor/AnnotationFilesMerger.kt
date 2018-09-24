/*
 * Copyright 2018 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor

import com.android.tools.build.jetifier.processor.archive.Archive
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import java.nio.charset.StandardCharsets

/**
 * Provides functionality to merge annotation files during dejetification. These annotations are
 * used by Android Studio's lint. The reason we need this is that it can happen that there is an old
 * annotation file (as not everything was moved to AndroidX yet, e.g. Media) and a new one.
 * After dejetification such files need to be merged into a one.
 */
object AnnotationFilesMerger {

    fun tryMergeFilesInArchive(archive: Archive) {
        archive.files
            .filter { it.fileName == "annotations.xml" && it is ArchiveFile }
            .map { it as ArchiveFile }
            .groupBy { it.relativePath.toString() }
            .forEach {
                if (it.value.size <= 1) {
                    return@forEach
                } else {
                    val files = it.value
                    val mergedFile = mergeAnnotationFiles(files)
                    files.forEach { file -> archive.removeItem(file) }
                    archive.addItem(mergedFile)
                }
            }
    }

    private fun mergeAnnotationFiles(files: Iterable<ArchiveFile>): ArchiveFile {
        val data = files
            .map { it.data.toString(StandardCharsets.UTF_8) }
            .joinToString()
            .replace("</root>(.|\\n)*?<root>[\n\r]*".toRegex(), "")
            .toByteArray(StandardCharsets.UTF_8)

        return ArchiveFile(files.first().relativePath, data)
    }
}