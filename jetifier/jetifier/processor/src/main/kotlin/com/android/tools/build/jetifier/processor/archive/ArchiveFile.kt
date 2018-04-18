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

package com.android.tools.build.jetifier.processor.archive

import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path

/**
 * Represents a file in the archive that is not an archive.
 */
class ArchiveFile(relativePath: Path, data: ByteArray) : ArchiveItem {

    override var relativePath = relativePath
        private set

    override var fileName: String = relativePath.fileName.toString()
        private set

    override var wasChanged: Boolean = false
        private set

    var data: ByteArray = data
        private set

    override fun accept(visitor: ArchiveItemVisitor) {
        visitor.visit(this)
    }

    @Throws(IOException::class)
    override fun writeSelfTo(outputStream: OutputStream) {
        outputStream.write(data)
    }

    fun updateRelativePath(newRelativePath: Path) {
        if (relativePath != newRelativePath) {
            wasChanged = true
        }

        relativePath = newRelativePath
        fileName = relativePath.fileName.toString()
    }

    /**
     * Sets new data while also marking this file as changed. This will result into the parent
     * archive also being considered as changed thus marking it as dependent on the Support library.
     */
    fun setNewData(newData: ByteArray) {
        data = newData
        wasChanged = true
    }

    /**
     * Sets a potentially new data without triggering a change. Useful in cases the change is not
     * significant for the refactoring because it occurred due to some optimization or
     * formatting change.
     *
     * If there was at least one genuine change in any file of the parent archive this won't prevent
     * this file from being updated. However this will prevent the change to propagate to
     * the parent archive which would otherwise mark it as dependent on the Support Library.
     */
    fun setNewDataSilently(newData: ByteArray) {
        data = newData
    }
}