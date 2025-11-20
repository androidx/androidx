/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.security.state.provider

import android.content.ComponentName
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * A content provider that serves update information for system components.
 *
 * This class retrieves [UpdateInfo] stored in JSON format and serves it via a content URI. It only
 * supports the [query] operation; [insert], [delete], and [update] operations are not permitted.
 *
 * Typically, OTA or other update clients utilize this provider to expose update information to
 * other applications or components within the system that need access to the latest security
 * updates data. The client calls [UpdateInfoManager.registerUpdate] and
 * [UpdateInfoManager.unregisterUpdate] to add or remove update information to a local store, from
 * which the content provider serves the data to the applications.
 *
 * To setup the content provider add the following snippet to the client's manifest:
 * ```
 * <provider
 * android:name="androidx.security.state.provider.UpdateInfoProvider"
 * android:authorities="${applicationId}.updateinfoprovider"
 * android:exported="true" />
 * ```
 */
public class UpdateInfoProvider : ContentProvider() {

    private var context: Context? = null
    private lateinit var authority: String
    private lateinit var contentUri: Uri

    /**
     * Initializes the content provider by constructing [contentUri] using the authority listed in
     * the manifest.
     *
     * @return true if the provider was successfully created, false otherwise.
     */
    override fun onCreate(): Boolean {
        context =
            getContext() ?: throw IllegalStateException("Cannot find context from the provider.")
        authority = getAuthority(context!!)
        contentUri = Uri.parse("content://$authority/updateinfo")
        return true
    }

    /**
     * Handles queries for the update information.
     *
     * This method only responds to queries directed at the specific content URI corresponding to
     * update data. It returns a [Cursor] containing [UpdateInfo] represented in JSON format.
     *
     * @param uri The URI to query. This must match the expected content URI for update data.
     * @param projection The list of columns to put into the cursor. If null, all columns are
     *   included.
     * @param selection The selection criteria to apply.
     * @param selectionArgs Arguments for the selection criteria.
     * @param sortOrder The order in which rows are sorted in the returned Cursor.
     * @return A [Cursor] object containing the update data.
     * @throws IllegalArgumentException if the provided URI does not match the expected URI for
     *   update data.
     */
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        // Verify that the caller has requested a correct URI for this provider
        if (uri == contentUri) {
            val updateInfoManager = UpdateInfoManager(context!!)
            val jsonUpdates = updateInfoManager.getAllUpdatesAsJson()
            val cursor = MatrixCursor(arrayOf("json"))
            jsonUpdates.forEach { cursor.addRow(arrayOf(it)) }
            return cursor
        } else {
            throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    /**
     * Returns the MIME type of the data at the given URI. This method only handles the content URI
     * for update data.
     *
     * @param uri The URI to query for its MIME type.
     * @return The MIME type of the data at the specified URI, or null if the URI is not handled by
     *   this provider.
     */
    override fun getType(uri: Uri): String? {
        return "vnd.android.cursor.dir/vnd.$authority.updateinfo"
    }

    /**
     * Unsupported operation. This method will throw an exception if called.
     *
     * @param uri The URI to query.
     * @param values The new values to insert.
     * @return nothing as this operation is not supported.
     * @throws UnsupportedOperationException always as this operation is not supported.
     */
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException("Insert operation is not supported.")
    }

    /**
     * Unsupported operation. This method will throw an exception if called.
     *
     * @param uri The URI to delete from.
     * @param selection The selection criteria to apply.
     * @param selectionArgs Arguments for the selection criteria.
     * @return nothing as this operation is not supported.
     * @throws UnsupportedOperationException always as this operation is not supported.
     */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException("Delete operation is not supported.")
    }

    /**
     * Unsupported operation. This method will throw an exception if called.
     *
     * @param uri The URI to update.
     * @param values The new values to apply.
     * @param selection The selection criteria to apply.
     * @param selectionArgs Arguments for the selection criteria.
     * @return nothing as this operation is not supported.
     * @throws UnsupportedOperationException always as this operation is not supported.
     */
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        throw UnsupportedOperationException("Update operation is not supported.")
    }

    /**
     * Returns [android.content.pm.ProviderInfo.authority], the authority of the provider defined in
     * the manifest.
     *
     * For example, "com.example.updateinfoprovider" would be returned for the following provider:
     * ```
     * <provider
     * android:name="androidx.security.state.provider.UpdateInfoProvider"
     * android:authorities="com.example.updateinfoprovider" />
     * ```
     */
    private fun getAuthority(context: Context): String {
        return context.packageManager
            .getProviderInfo(
                ComponentName(context, UpdateInfoProvider::class.java),
                PackageManager.GET_META_DATA,
            )
            .authority
    }
}
