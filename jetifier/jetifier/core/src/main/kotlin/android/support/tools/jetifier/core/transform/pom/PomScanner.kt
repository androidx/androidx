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

package android.support.tools.jetifier.core.transform.pom

import android.support.tools.jetifier.core.archive.Archive
import android.support.tools.jetifier.core.archive.ArchiveFile
import android.support.tools.jetifier.core.archive.ArchiveItemVisitor
import android.support.tools.jetifier.core.config.Config
import android.support.tools.jetifier.core.utils.Log

/**
 * Helper to scan [Archive]s to find their POM files.
 */
class PomScanner(private val config: Config) {

    companion object {
        private const val TAG = "PomScanner"
    }

    private val pomFilesInternal = mutableListOf<PomDocument>()

    private var validationFailuresCount = 0

    val pomFiles : List<PomDocument> = pomFilesInternal

    fun wasErrorFound() = validationFailuresCount > 0

    /**
     * Scans the given [archive] for a POM file
     *
     * @return null if POM file was not found
     */
    fun scanArchiveForPomFile(archive: Archive) : PomDocument? {
        val session = PomScannerSession()
        archive.accept(session)

        if (session.pomFile == null) {
            return null
        }
        val pomFile = session.pomFile!!

        pomFile.logDocumentDetails()

        if (!pomFile.validate(config.pomRewriteRules)) {
            Log.e(TAG, "Version mismatch!")
            validationFailuresCount++
        }

        pomFilesInternal.add(session.pomFile!!)

        return session.pomFile
    }


    private class PomScannerSession : ArchiveItemVisitor {

        var pomFile : PomDocument? = null

        override fun visit(archive: Archive) {
            for (archiveItem in archive.files) {
                if (pomFile != null) {
                    break
                }
                archiveItem.accept(this)
            }
        }

        override fun visit(archiveFile: ArchiveFile) {
            if (archiveFile.isPomFile()) {
                pomFile = PomDocument.loadFrom(archiveFile)
            }
        }
    }
}