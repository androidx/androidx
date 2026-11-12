/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.video.internal.utils

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

/**
 * A fake [ContentProvider] that backs a single media entry with a file on disk, similar to how
 * `MediaStore` stores the file path of an entry in its `DATA` column.
 *
 * [insert] into [COLLECTION_URI] always returns [ENTRY_URI].
 */
class FakeMediaContentProvider : ContentProvider() {

    /** The value of the `DATA` column of the entry, or `null` to return an empty cursor. */
    var dataPath: String? = null

    /** Whether [query] returns a `null` cursor. */
    var returnNullCursor: Boolean = false

    /**
     * Handles [openFile]. By default, the file at [dataPath] is opened, which throws a
     * [FileNotFoundException] if its parent folder doesn't exist.
     */
    var openFileHandler: (Uri, String) -> ParcelFileDescriptor? = { _, mode ->
        val path = dataPath ?: throw FileNotFoundException("No entry")
        ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.parseMode(mode))
    }

    /** The number of times [openFile] has been called. */
    var openFileCount: Int = 0
        private set

    /** The exception thrown by [delete], or `null` to delete successfully. */
    var deleteException: RuntimeException? = null

    /** The uris passed to [delete]. */
    val deletedUris: MutableList<Uri> = mutableListOf()

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (returnNullCursor) {
            return null
        }
        val columns = projection ?: arrayOf(DATA_COLUMN)
        return MatrixCursor(columns).apply {
            dataPath?.let { path -> addRow(Array<Any?>(columns.size) { path }) }
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        openFileCount++
        return openFileHandler(uri, mode)
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = ENTRY_URI

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        deletedUris.add(uri)
        deleteException?.let { throw it }
        return 1
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY: String = "androidx.camera.video.test.fakemedia"
        const val DATA_COLUMN: String = "_data"
        val COLLECTION_URI: Uri = Uri.parse("content://$AUTHORITY/video")
        val ENTRY_URI: Uri = Uri.parse("content://$AUTHORITY/video/1")
    }
}
