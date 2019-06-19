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

package com.android.tools.build.jetifier.standalone

import com.android.tools.build.jetifier.processor.archive.Archive
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.archive.ArchiveItemVisitor
import com.android.tools.build.jetifier.processor.transform.pom.PomDocument
import java.io.File
import java.nio.file.Paths
import java.security.MessageDigest

/**
 * Utility to rebuild de-jetified zipped maven repo.
 */
class TopOfTreeBuilder {

    companion object {
        const val DIR_PREFIX = "m2repository"
    }

    fun rebuildFrom(inputZip: File, outputZip: File) {
        val archive = Archive.Builder.extract(inputZip, recursive = false)

        // Find poms
        val pomFilter = FileFilter({ it.isPomFile() })
        archive.accept(pomFilter)
        val pomFiles = pomFilter.files

        // Find archives
        val archivesFilter = FileFilter({
                return@FileFilter it.fileName.endsWith(".aar") || it.fileName.endsWith("jar")
        })
        archive.accept(archivesFilter)
        val libFiles = archivesFilter.files

        // Process
        val newFiles = mutableSetOf<ArchiveFile>()
        pomFiles.forEach {
            pomFile -> run {
                val name = pomFile.relativePath.toFile().nameWithoutExtension
                val nameAar = name + ".aar"
                val nameJar = name + ".jar"
                val artifactFile = libFiles.first {
                    it.fileName == nameAar || it.fileName == nameJar
                }
                val nameSources = name + "-sources.jar"
                val sourcesFile = libFiles.first {
                    it.fileName == nameSources
                }
                process(pomFile, artifactFile, sourcesFile, newFiles)
            }
        }

        // Write the result
        val finalArchive = Archive(outputZip.toPath(), newFiles.toList())
        finalArchive.writeSelf()
    }

    private fun process(
        pomFile: ArchiveFile,
        artifactFile: ArchiveFile,
        sourcesFile: ArchiveFile,
        resultSet: MutableSet<ArchiveFile>
    ) {
        val pomDep = PomDocument.loadFrom(pomFile).getAsPomDependency()

        val groupAsPath = pomDep.groupId!!.replace('.', '/')

        val packaging = artifactFile.relativePath.toFile().extension
        val baseFileName = "${pomDep.artifactId}-${pomDep.version}"

        val artifactDir = Paths.get(DIR_PREFIX, groupAsPath, pomDep.artifactId, pomDep.version!!)
        val newLibFilePath = Paths.get(artifactDir.toString(), "$baseFileName.$packaging")
        val newPomFilePath = Paths.get(artifactDir.toString(), "$baseFileName.pom")
        val newSourcesFilePath = Paths.get(artifactDir.toString(), "$baseFileName-sources.jar")

        val newArtifactFile = ArchiveFile(newLibFilePath, artifactFile.data)
        val newPomFile = ArchiveFile(newPomFilePath, pomFile.data)
        val newSourcesFile = ArchiveFile(newSourcesFilePath, sourcesFile.data)

        resultSet.add(newArtifactFile)
        resultSet.add(getHashFileOf(newArtifactFile, "MD5"))
        resultSet.add(getHashFileOf(newArtifactFile, "SHA1"))

        resultSet.add(newPomFile)
        resultSet.add(getHashFileOf(newPomFile, "MD5"))
        resultSet.add(getHashFileOf(newPomFile, "SHA1"))

        resultSet.add(newSourcesFile)
        resultSet.add(getHashFileOf(newSourcesFile, "MD5"))
        resultSet.add(getHashFileOf(newSourcesFile, "SHA1"))
    }

    private fun getHashFileOf(file: ArchiveFile, hashType: String): ArchiveFile {
        val md = MessageDigest.getInstance(hashType)
        val result = md.digest(file.data)
        return ArchiveFile(Paths.get(
            file.relativePath.toString() + "." + hashType.toLowerCase()), result)
    }

    private class FileFilter(private val filter: (ArchiveFile) -> Boolean) : ArchiveItemVisitor {

        val files = mutableSetOf<ArchiveFile>()

        override fun visit(archiveFile: ArchiveFile) {
            if (filter(archiveFile)) {
                files.add(archiveFile)
            }
        }

        override fun visit(archive: Archive) {
            archive.files.forEach { it.accept(this) }
        }
    }
}
