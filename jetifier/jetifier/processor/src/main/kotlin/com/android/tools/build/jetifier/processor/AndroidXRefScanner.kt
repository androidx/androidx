/*
 * Copyright 2019 The Android Open Source Project
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
import com.android.tools.build.jetifier.processor.archive.ArchiveItemVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper

/**
 * Scans java bytecode for any references to androidX.
 */
class AndroidXRefScanner(private val library: Archive) : ArchiveItemVisitor {

    /** Whether any androidX references were discovered. Check after calling [scan]. */
    var androidXDetected = false

    fun scan(): AndroidXRefScanner {
        library.accept(this)
        return this
    }

    override fun visit(archive: Archive) {
        archive.files.forEach {
            if (androidXDetected) {
                return@forEach
            }

            it.accept(this)
        }
    }

    override fun visit(archiveFile: ArchiveFile) {
        if (!archiveFile.isClassFile()) {
            return
        }

        val reader = ClassReader(archiveFile.data)
        val writer = ClassWriter(0 /* flags */)

        val androidXTrackingRemapper = AndroidXTrackingRemapper()
        val classRemapper = ClassRemapper(writer, androidXTrackingRemapper)

        reader.accept(classRemapper, 0 /* flags */)

        androidXDetected = androidXDetected || androidXTrackingRemapper.androidXDetected
    }

    class AndroidXTrackingRemapper : Remapper() {

        var androidXDetected = false

        override fun map(typeName: String): String {
            if (typeName.startsWith("androidx/")) {
                androidXDetected = true
            }
            return typeName
        }
    }
}
