/*
 * Copyright 2017 The Android Open Source Project
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

package com.android.tools.build.jetifier.processor.transform.pom

import com.android.tools.build.jetifier.core.utils.Log
import com.android.tools.build.jetifier.processor.archive.Archive
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.archive.ArchiveItemVisitor
import com.android.tools.build.jetifier.processor.transform.TransformationContext

/**
 * Helper to scan [Archive]s to find their POM files.
 */
class PomScanner(private val context: TransformationContext) {

    companion object {
        private const val TAG = "PomScanner"
    }

    private val pomFilesInternal = mutableListOf<PomDocument>()

    private var validationFailuresCount = 0

    val pomFiles: List<PomDocument> = pomFilesInternal

    fun wasErrorFound() = validationFailuresCount > 0

    /**
     * Scans the given [archive] for a POM file
     *
     * @return null if POM file was not found
     */
    fun scanArchiveForPomFile(archive: Archive) {
        val session = PomScannerSession()
        archive.accept(session)

        session.pomFiles.forEach {
            it.logDocumentDetails()

            if (!context.ignorePomVersionCheck && !it.validate(context.config.pomRewriteRules)) {
                Log.e(TAG, "Version mismatch!")
                validationFailuresCount++
            }

            pomFilesInternal.add(it)
        }
    }

    private class PomScannerSession : ArchiveItemVisitor {

        val pomFiles = mutableSetOf<PomDocument>()

        override fun visit(archive: Archive) {
            for (archiveItem in archive.files) {
                archiveItem.accept(this)
            }
        }

        override fun visit(archiveFile: ArchiveFile) {
            if (archiveFile.isPomFile()) {
                pomFiles.add(PomDocument.loadFrom(archiveFile))
            }
        }
    }
}