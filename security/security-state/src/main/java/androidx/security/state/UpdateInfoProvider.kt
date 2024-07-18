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

package androidx.security.state

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.google.gson.Gson

/**
 * A content provider for managing and serving update information for system components. This class
 * interfaces with a [SecurityPatchState] to fetch update data stored in JSON format and serves it
 * via a content URI. It only supports querying data; insert, delete, and update operations are not
 * supported.
 *
 * This provider is typically used by OTA or other update client to expose update information to
 * other applications or components within the system that need access to the latest security
 * updates data. The client calls registerUpdate() and unregisterUpdate() to add or remove update
 * information to a local store, from which the content provider serves the data to the
 * applications. To setup the content provider add following snippet to the client's manifest,
 * replacing com.example with correct namespace:
 * <pre>
 * <permission
 * android:name="com.example.updateinfoprovider.WRITE_UPDATES_INFO"
 * android:label="@string/write_permission_label"
 * android:description="@string/write_permission_description"
 * android:protectionLevel="signature" />
 *
 * <provider
 * android:name=".UpdateInfoProvider"
 * android:authorities="com.example.updateinfoprovider"
 * android:exported="true"
 * android:writePermission="com.example.updateinfoprovider.WRITE_UPDATES_INFO"/>
 * </pre>
 *
 * @param context The [Context] of the calling application.
 * @param authority The authority for this content provider, used to construct the base URI.
 * @param customSecurityState An optional instance of [SecurityPatchState] to use. If not provided,
 *   a new instance is created.
 */
public open class UpdateInfoProvider(
    private val context: Context,
    private val authority: String,
    private val customSecurityState: SecurityPatchState? = null
) : ContentProvider() {

    private val contentUri: Uri = Uri.parse("content://$authority/updateinfo")

    private val updateInfoPrefs = "UpdateInfoPrefs"

    private lateinit var securityState: SecurityPatchState

    /**
     * Initializes the content provider. This method sets up the [SecurityPatchState] used to
     * retrieve update information. If a custom security state is provided during instantiation, it
     * will be used; otherwise, a new one is initialized.
     *
     * @return true if the provider was successfully created, false otherwise.
     */
    override fun onCreate(): Boolean {
        securityState = customSecurityState ?: SecurityPatchState(context)
        return true
    }

    /**
     * Handles queries for the update information. This method only responds to queries directed at
     * the specific content URI corresponding to update data. It retrieves data from
     * [SecurityPatchState], which is then returned as a [Cursor].
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
        sortOrder: String?
    ): Cursor {
        // Verify that the caller has requested a correct URI for this provider
        if (uri == contentUri) {
            val cursor = MatrixCursor(arrayOf("json"))
            val jsonUpdates = getAllUpdatesAsJson()
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
        selectionArgs: Array<out String>?
    ): Int {
        throw UnsupportedOperationException("Update operation is not supported.")
    }

    /**
     * Registers information about an available update for the specified component.
     *
     * @param updateInfo Update information structure.
     */
    public fun registerUpdate(updateInfo: UpdateInfo) {
        cleanupUpdateInfo()

        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit()
        val key = getKeyForUpdateInfo(updateInfo)
        val json = Gson().toJson(updateInfo)
        editor?.putString(key, json)
        editor?.apply()
    }

    /**
     * Unregisters information about an available update for the specified component.
     *
     * @param updateInfo Update information structure.
     */
    public fun unregisterUpdate(updateInfo: UpdateInfo) {
        cleanupUpdateInfo()

        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit()
        val key = getKeyForUpdateInfo(updateInfo)
        editor?.remove(key)
        editor?.apply()
    }

    private fun getKeyForUpdateInfo(updateInfo: UpdateInfo): String {
        // Create a unique key for each update info.
        return "${updateInfo.component}-${updateInfo.uri}"
    }

    /**
     * Retrieves a list of all updates currently registered in the system's shared preferences. This
     * method is primarily used for managing and tracking updates that have been registered but not
     * yet applied or acknowledged by the system.
     *
     * @return A list of [UpdateInfo] objects, each representing a registered update.
     */
    private fun getAllUpdates(): List<UpdateInfo> {
        val allUpdates = mutableListOf<UpdateInfo>()
        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val allEntries = sharedPreferences?.all ?: return listOf()
        for ((_, value) in allEntries) {
            val json = value as? String
            if (json != null) {
                val updateInfo: UpdateInfo = Gson().fromJson(json, UpdateInfo::class.java)
                allUpdates.add(updateInfo)
            }
        }
        return allUpdates
    }

    /**
     * Cleans up outdated or applied updates from the shared preferences. This method checks each
     * registered update against the current device security patch levels and removes any updates
     * that are no longer relevant (i.e., the update's patch level is less than or equal to the
     * current device patch level).
     */
    private fun cleanupUpdateInfo() {
        val allUpdates = getAllUpdates()
        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val editor = sharedPreferences?.edit() ?: return

        allUpdates.forEach { updateInfo ->
            val component = updateInfo.component
            val currentSpl: SecurityPatchState.SecurityPatchLevel
            try {
                currentSpl = securityState.getDeviceSecurityPatchLevel(component)
            } catch (e: IllegalArgumentException) {
                // Ignore unknown components.
                return@forEach
            }
            val updateSpl =
                securityState.getComponentSecurityPatchLevel(
                    component,
                    updateInfo.securityPatchLevel
                )

            if (updateSpl <= currentSpl) {
                val key = getKeyForUpdateInfo(updateInfo)
                editor.remove(key)
            }
        }

        editor.apply()
    }

    /**
     * Retrieves all registered updates in JSON format from the system's shared preferences. This
     * can be useful for exporting or debugging update information.
     *
     * @return A list of strings, each representing an update in JSON format.
     */
    private fun getAllUpdatesAsJson(): List<String> {
        val allUpdates = mutableListOf<String>()
        val sharedPreferences = context.getSharedPreferences(updateInfoPrefs, Context.MODE_PRIVATE)
        val allEntries = sharedPreferences?.all ?: return emptyList()
        for ((_, value) in allEntries) {
            val json = value as? String
            if (json != null) {
                allUpdates.add(json)
            }
        }
        return allUpdates
    }
}
