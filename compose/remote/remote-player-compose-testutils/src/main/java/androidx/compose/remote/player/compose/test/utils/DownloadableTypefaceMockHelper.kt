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

package androidx.compose.remote.player.compose.test.utils

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.test.mock.MockContentResolver
import java.io.File
import java.io.InputStream

public fun createMockContextWithFont(
    baseContext: Context,
    fontInputStream: InputStream,
    fontName: String = "karla_regular.ttf",
    authority: String = "com.google.android.gms.fonts",
): Context {
    val key = fontName.substringBefore(".").lowercase()
    return createMockContextWithFonts(
        baseContext = baseContext,
        fontStreams = mapOf(key to fontInputStream),
        defaultFontKey = key,
        authority = authority,
    )
}

public fun createMockContextWithFonts(
    baseContext: Context,
    fontStreams: Map<String, InputStream>,
    defaultFontKey: String = fontStreams.keys.first(),
    authority: String = "com.google.android.gms.fonts",
): Context {
    val fontsByName = mutableMapOf<String, File>()
    val fontFiles = mutableListOf<File>()

    for ((name, stream) in fontStreams) {
        val fileName = if (name.endsWith(".ttf")) name else "$name.ttf"
        val fontFile = File(baseContext.cacheDir, fileName)
        stream.use { input -> fontFile.outputStream().use { output -> input.copyTo(output) } }
        fontFiles.add(fontFile)
        fontsByName[name.lowercase()] = fontFile
    }

    val defaultFile = fontsByName[defaultFontKey.lowercase()] ?: fontFiles.first()
    val mockResolver = MockContentResolver(baseContext)
    val mockProvider = MockMultiFontProvider(fontsByName, fontFiles, defaultFile)
    val info = ProviderInfo().apply { this.authority = authority }
    mockProvider.attachInfo(baseContext, info)
    mockResolver.addProvider(authority, mockProvider)
    return MockContext(baseContext, mockResolver)
}

private class MockContext(base: Context, private val mockResolver: ContentResolver) :
    ContextWrapper(base) {
    override fun getContentResolver(): ContentResolver {
        return mockResolver
    }

    override fun getApplicationContext(): Context {
        return this
    }
}

private class MockMultiFontProvider(
    private val fontsByName: Map<String, File>,
    private val fontFiles: List<File>,
    private val defaultFile: File,
) : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val queryParam = selectionArgs?.firstOrNull() ?: uri.getQueryParameter("q") ?: ""
        val matchedFile =
            fontsByName.entries
                .firstOrNull { (key, _) -> queryParam.contains(key, ignoreCase = true) }
                ?.value ?: defaultFile

        val id = (fontFiles.indexOf(matchedFile).coerceAtLeast(0) + 1).toLong()

        val cursor =
            MatrixCursor(
                arrayOf(
                    "_id",
                    "file_id",
                    "font_ttc_index",
                    "font_variation_settings",
                    "font_weight",
                    "font_italic",
                    "result_code",
                )
            )
        cursor.addRow(arrayOf(id, id, 0, null, 400, 0, 0))
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val index = uri.lastPathSegment?.toIntOrNull()
        val fontFile = index?.let { fontFiles.getOrNull(it - 1) } ?: defaultFile
        return ParcelFileDescriptor.open(fontFile, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
