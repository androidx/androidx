package android.support.tools.jetifier.core.transform.pom

import android.support.tools.jetifier.core.archive.Archive
import android.support.tools.jetifier.core.archive.ArchiveFile
import android.support.tools.jetifier.core.archive.ArchiveItemVisitor

/**
 * Helper to scan [Archive]s to find their POM files.
 */
class PomScanner {

    companion object {

        /**
         * Scans the given [archive] for a POM file. Returns null if POM file was not found.
         */
        fun scanArchiveForPomFile(archive: Archive) : PomDocument? {
            val session = PomScannerSession()
            archive.accept(session)
            return session.pomFile
        }

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