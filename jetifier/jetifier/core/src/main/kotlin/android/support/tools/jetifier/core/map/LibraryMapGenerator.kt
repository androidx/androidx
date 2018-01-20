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

package android.support.tools.jetifier.core.map

import android.support.tools.jetifier.core.archive.Archive
import android.support.tools.jetifier.core.archive.ArchiveFile
import android.support.tools.jetifier.core.archive.ArchiveItemVisitor
import android.support.tools.jetifier.core.config.Config
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

/**
 * Scans a library java files using [MapGeneratorRemapper] to create [TypesMap].
 */
class LibraryMapGenerator constructor(config: Config) : ArchiveItemVisitor {

    private val remapper = MapGeneratorRemapper(config)

    /**
     * Scans the given [library] to extend the types map meta-data. The final map can be retrieved
     * using [generateMap].
     */
    fun scanLibrary(library: Archive) {
        library.accept(this)
    }

    /**
     * Creates the [TypesMap] based on the meta-data aggregated via previous [scanFile] calls
     */
    fun generateMap(): TypesMap {
        val map = remapper.createTypesMap()
        map.reverseMapOrDie()
        return map
    }

    override fun visit(archive: Archive) {
        archive.files.forEach { it.accept(this) }
    }

    override fun visit(archiveFile: ArchiveFile) {
        if (archiveFile.isClassFile()) {
            scanFile(archiveFile)
        }
    }

    private fun scanFile(file: ArchiveFile) {
        val reader = ClassReader(file.data)
        val writer = ClassWriter(0 /* flags */)

        val visitor = remapper.createClassRemapper(writer)

        reader.accept(visitor, 0 /* flags */)
    }
}