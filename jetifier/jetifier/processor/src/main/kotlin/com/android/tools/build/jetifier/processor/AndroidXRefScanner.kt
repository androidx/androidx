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

import com.android.tools.build.jetifier.core.config.Config
import com.android.tools.build.jetifier.core.type.JavaType
import com.android.tools.build.jetifier.processor.archive.Archive
import com.android.tools.build.jetifier.processor.archive.ArchiveFile
import com.android.tools.build.jetifier.processor.archive.ArchiveItemVisitor
import com.android.tools.build.jetifier.processor.transform.bytecode.InvalidByteCodeException
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper

/**
 * Scans java bytecode for any references to androidX.
 */
class AndroidXRefScanner(
    private val library: Archive,
    private val config: Config
) : ArchiveItemVisitor {

    /** Whether any androidX references were discovered. Check after calling [scan]. */
    val androidXDetected
        get() = androidXRefExample != null
    /** Whether any android support references were discovered. Check after calling [scan]. */
    val androidSupportDetected
        get() = androidSupportRefExample != null

    /**
     * Example of androidX reference that was discovered. This is null if no reference was found.
     * Check after calling [scan].
     */
    var androidXRefExample: String? = null
    /**
     * Example of android support reference that was discovered. This is null if no reference was
     * found. Check after calling [scan].
     */
    var androidSupportRefExample: String? = null

    fun scan(): AndroidXRefScanner {
        library.accept(this)
        return this
    }

    override fun visit(archive: Archive) {
        archive.files.forEach {
            if (androidXDetected && androidSupportDetected) {
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

        val androidXTrackingRemapper = AndroidXTrackingRemapper(config)
        val classRemapper = ClassRemapper(writer, androidXTrackingRemapper)

        try {
            reader.accept(classRemapper, 0 /* flags */)
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw InvalidByteCodeException(
                "Error processing '${archiveFile.relativePath}' bytecode.", e)
        }

        if (androidXTrackingRemapper.androidXRefExample != null) {
            androidXRefExample = androidXTrackingRemapper.androidXRefExample
        }
        if (androidXTrackingRemapper.androidSupportRefExample != null) {
            androidSupportRefExample = androidXTrackingRemapper.androidSupportRefExample
        }
    }

    class AndroidXTrackingRemapper(private val config: Config) : Remapper() {

        var androidXRefExample: String? = null
        var androidSupportRefExample: String? = null

        override fun map(typeName: String): String {
            if (typeName.startsWith("androidx/")) {
                androidXRefExample = typeName
            } else if (config.isEligibleForRewrite(JavaType(typeName))) {
                androidSupportRefExample = typeName
            }

            return typeName
        }
    }
}
