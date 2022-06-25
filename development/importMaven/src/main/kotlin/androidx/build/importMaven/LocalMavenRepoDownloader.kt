/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build.importMaven

import okio.FileSystem
import okio.Path
import org.apache.logging.log4j.kotlin.logger
import org.jetbrains.kotlin.com.google.common.annotations.VisibleForTesting
import java.security.MessageDigest
import java.util.Locale

/**
 * A [DownloadObserver] that will save all files into the given repository folders.
 */
class LocalMavenRepoDownloader(
    val fileSystem: FileSystem,
    /**
     * Path to the internal repo (e.g. prebuilts/androidx/internal)
     */
    val internalFolder: Path,
    /**
     * Path to the external repo (e.g. prebuilts/androidx/external)
     */
    val externalFolder: Path
) : DownloadObserver {
    private val logger = logger("LocalMavenRepoDownloader")
    private val licenseDownloader = LicenseDownloader(enableGithubApi = false)
    private val writtenFiles = mutableSetOf<Path>()

    /**
     * Returns the list of files we've downloaded.
     */
    fun getDownloadedFiles() = writtenFiles.sorted().distinct()

    override fun onDownload(path: String, bytes: ByteArray) {
        if (path.substringAfterLast('.') in checksumExtensions) {
            // we sign files locally, don't download them
            logger.trace {
                "Skipping $path because we'll sign it locally"
            }
            return
        }
        val internal = isInternalArtifact(path)
        val folder = if (internal) internalFolder else externalFolder
        logger.trace {
            "Downloading $path. internal? $internal"
        }
        folder.resolve(path).let { file ->
            val targetFolder = file.parent ?: error("invalid parent for $file")
            if (file.name.endsWith(".pom")) {
                // Keep original MD5 and SHA1 hashes
                copyWithDigest(targetFolder, fileName = file.name, bytes = bytes)
                if (internal) {
                    val transformed = transformInternalPomFile(bytes)
                    copyWithoutDigest(targetFolder, fileName = file.name, bytes = transformed)
                } else {
                    // download licenses only for external poms
                    licenseDownloader.fetchLicenseFromPom(bytes)?.let { licenseBytes ->
                        copyWithoutDigest(targetFolder, "LICENSE", licenseBytes)
                    }
                }
            } else {
                copyWithDigest(
                    targetFolder = targetFolder,
                    fileName = file.name,
                    bytes = bytes
                )
            }
        }
    }

    /**
     * Creates the file in the given [targetFolder]/[fileName] pair.
     */
    private fun copyWithoutDigest(
        targetFolder: Path,
        fileName: String,
        bytes: ByteArray
    ) {
        fileSystem.writeBytes(targetFolder / fileName, bytes)
    }

    /**
     *  Creates the file in the given [targetFolder]/[fileName] pair and also creates the md5 and
     *  sha1 checksums.
     */
    private fun copyWithDigest(
        targetFolder: Path,
        fileName: String,
        bytes: ByteArray
    ) {
        copyWithoutDigest(targetFolder, fileName, bytes)
        if (fileName.substringAfterLast('.') !in checksumExtensions) {
            digest(bytes, fileName, "MD5").let { (name, contents) ->
                fileSystem.writeBytes(targetFolder / name, contents)
            }
            digest(bytes, fileName, "SHA1").let { (name, contents) ->
                fileSystem.writeBytes(targetFolder / name, contents)
            }
        }
    }

    private fun FileSystem.writeBytes(
        file: Path,
        contents: ByteArray
    ) {
        writtenFiles.add(file.normalized())
        file.parent?.let(fileSystem::createDirectories)
        write(
            file = file,
            mustCreate = false
        ) {
            write(contents)
        }
        logger.info {
            "Saved $file (${contents.size} bytes)"
        }
    }

    /**
     * Certain prebuilts might have improper downloads.
     * This method traverses all folders into which we've fetched an artifact and deletes files
     * that are not fetched by us.
     *
     * Note that, sometimes we might pull a pom file but not download its artifacts (if there is a
     * newer version). So before cleaning any file, we make sure we fetched one of
     * [EXTENSIONS_FOR_CLENAUP].
     *
     * This should be used only if the local repositories are disabled in resolution. Otherwise, it
     * might delete files that were resolved from the local repository.
     */
    fun cleanupLocalRepositories() {
        val folders = writtenFiles.filter {
            val isDirectory = fileSystem.metadata(it).isDirectory
            !isDirectory && it.name.substringAfterLast(".") in EXTENSIONS_FOR_CLENAUP
        }.mapNotNull {
            it.parent
        }.distinct()
        logger.info {
            "Cleaning up local repository. Folders to clean: ${folders.size}"
        }

        // traverse all folders and make sure they are in the written files list
        folders.forEachIndexed { index, folder ->
            logger.trace {
                "Cleaning up $folder ($index of ${folders.size})"
            }
            fileSystem.list(folder).forEach { candidateToDelete ->
                if (!writtenFiles.contains(candidateToDelete.normalized())) {
                    logger.trace {
                        "Deleting $candidateToDelete since it is not re-downloaded"
                    }
                    fileSystem.delete(candidateToDelete)
                } else {
                    logger.trace {
                        "Keeping $candidateToDelete"
                    }
                }
            }
        }
    }

    /**
     * Transforms POM files so we automatically comment out nodes with <type>aar</type>.
     *
     * We are doing this for all internal libraries to account for -Pandroidx.useMaxDepVersions
     * which swaps out the dependencies of all androidx libraries with their respective ToT
     * versions. For more information look at b/127495641.
     *
     * Instead of parsing the dom and re-writing it, we use a simple string replace to keep the file
     * contents intact.
     */
    private fun transformInternalPomFile(bytes: ByteArray): ByteArray {
        // using a simple line match rather than xml parsing because we want to keep file same as
        // much as possible
        return bytes.toString(Charsets.UTF_8).lineSequence().map {
            it.replace("<type>aar</type>", "<!--<type>aar</type>-->")
        }.joinToString("\n").toByteArray(Charsets.UTF_8)
    }

    companion object {
        val checksumExtensions = listOf("md5", "sha1")

        /**
         * If we downloaded an artifact with one of these extensions, we can cleanup that folder
         * for files that are not re-downloaded.
         */
        private val EXTENSIONS_FOR_CLENAUP = listOf(
            "jar",
            "aar",
            "klib"
        )
        private val INTERNAL_ARTIFACT_PREFIXES = listOf(
            "android/arch",
            "com/android/support",
            "androidx"
        )

        // Need to exclude androidx.databinding
        private val FORCE_EXTERNAL_PREFIXES = setOf(
            "androidx/databinding"
        )

        /**
         * Checks if an artifact is *internal*.
         */
        fun isInternalArtifact(path: String): Boolean {
            if (FORCE_EXTERNAL_PREFIXES.any {
                    path.startsWith(it)
                }) {
                return false
            }
            return INTERNAL_ARTIFACT_PREFIXES.any {
                path.startsWith(it)
            }
        }

        /**
         * Creates digest for the given contents.
         *
         * @param contents file contents
         * @param fileName original file name
         * @param algorithm Algorithm to use
         *
         * @return a pair if <new file name> : <digest bytes>
         */
        @VisibleForTesting
        internal fun digest(
            contents: ByteArray,
            fileName: String,
            algorithm: String
        ): Pair<String, ByteArray> {
            val messageDigest = MessageDigest.getInstance(algorithm)
            val digestBytes = messageDigest.digest(contents)
            val builder = StringBuilder()
            for (byte in digestBytes) {
                builder.append(String.format("%02x", byte))
            }
            val signatureFileName = "$fileName.${algorithm.lowercase(Locale.US)}"
            val resultBytes = builder.toString().toByteArray(Charsets.UTF_8)
            return signatureFileName to resultBytes
        }
    }
}