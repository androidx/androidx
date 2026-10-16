/*
 * Copyright (C) 2026 The Android Open Source Project
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

package androidx.people

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import androidx.annotation.CallSuper
import androidx.annotation.RestrictTo

/**
 * Base [ContentProvider] that a People Directory application extends to publish its people and
 * groups to the Android People Directory.
 *
 * This class implements URI matching for the `persons` and `groups` paths, parsing of the query
 * argument bundle into [PersonQuery] and [GroupQuery], and serialization of [Person] and [Group]
 * into the required cursor shape. Subclasses supply only the data, by implementing [queryPersons]
 * and [queryGroups].
 *
 * **For People Directory applications:** declare exactly one subclass in the manifest. The
 * declaration must be exported, and must carry the [PROPERTY_NAME] marker. Declare the marker as
 * both a `<property>` and a `<meta-data>`: `<property>` is the source of truth on API 31 and above,
 * and the `<meta-data>` fallback keeps the declaration discoverable on older API levels where
 * `<property>` is not available.
 *
 * ```xml
 * <provider
 *     android:name=".MyPeopleDirectory"
 *     android:authorities="com.example.app.people"
 *     android:exported="true">
 *     <property
 *         android:name="android.content.PeopleDirectory"
 *         android:value="true" />
 *     <meta-data
 *         android:name="android.content.PeopleDirectory"
 *         android:value="true" />
 * </provider>
 * ```
 *
 * A centralized component reads from this provider using [queryPersons] and [queryGroups] to sync
 * data to its internal database and provide to privileged client applications. Call [notifyChange]
 * or [requestRefresh] whenever the published data changes, so that the internal database is
 * invalidated and clients observing the data are notified.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public abstract class PeopleDirectoryContentProvider : ContentProvider() {

    private val uriMatcher: UriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    @CallSuper
    override fun onCreate(): Boolean {
        // Subclasses typically initialize their data sources here.
        return true
    }

    @CallSuper
    override fun attachInfo(context: Context?, info: ProviderInfo?) {
        super.attachInfo(context, info)
        // android:authorities accepts a semicolon-separated list; register every authority so that
        // URI matching does not silently fail for all but the first one.
        val authorities = info?.authority?.split(';') ?: return
        for (authority in authorities) {
            if (authority.isEmpty()) continue
            uriMatcher.addURI(authority, "persons", MATCH_PERSONS)
            uriMatcher.addURI(authority, "groups", MATCH_GROUPS)
        }
    }

    override fun getType(uri: Uri): String? = TODO("Implemented in the follow-up change")

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?,
    ): Cursor? = TODO("Implemented in the follow-up change")

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        throw UnsupportedOperationException(
            "Selection-based queries are not supported by PeopleDirectoryContentProvider. " +
                "Use query(Uri, Array<String>?, Bundle?, CancellationSignal?) instead."
        )
    }

    /**
     * Returns the people matching [query].
     *
     * Called on a background thread, and may block. Return an empty list when nothing matches.
     *
     * Return each person at most once. Report every input that matched the person in
     * [Person.matchedCriteria], for example both a [Criterion.Phone] and a [Criterion.Email] when
     * the same person matches two inputs. Each reported value must be echoed exactly as it appears
     * in [query]; criteria that do not appear in [query] are dropped. The order of the returned
     * list is not significant and is not preserved for client applications.
     *
     * @param query the query criteria. [PersonQuery.phones], [PersonQuery.emails],
     *   [PersonQuery.lookupKeys], and [PersonQuery.groupLookupKeys] contain the criteria to match.
     * @param cancellationSignal signals that the caller has abandoned the query, or `null`.
     */
    public abstract fun queryPersons(
        query: PersonQuery,
        cancellationSignal: CancellationSignal?,
    ): List<Person>

    /**
     * Returns the groups matching [query].
     *
     * Called on a background thread, and may block. Return an empty list when nothing matches.
     *
     * Return each group at most once, and report the lookup keys that matched it as
     * [Criterion.GroupLookupKey] values in [Group.matchedCriteria], echoed exactly as they appear
     * in [query]. Criteria that do not appear in [query] are dropped. The order of the returned
     * list is not significant and is not preserved for client applications.
     *
     * @param query the query criteria. [GroupQuery.lookupKeys] contains the lookup keys to match.
     * @param cancellationSignal signals that the caller has abandoned the query, or `null`.
     */
    public abstract fun queryGroups(
        query: GroupQuery,
        cancellationSignal: CancellationSignal?,
    ): List<Group>

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        throw UnsupportedOperationException(
            "insert is not supported by PeopleDirectoryContentProvider"
        )
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        throw UnsupportedOperationException(
            "delete is not supported by PeopleDirectoryContentProvider"
        )
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        throw UnsupportedOperationException(
            "update is not supported by PeopleDirectoryContentProvider"
        )
    }

    public companion object {
        /**
         * Name of the manifest property and meta-data tag a `ContentProvider` declares to register
         * itself as a People Directory.
         */
        public const val PROPERTY_NAME: String = "android.content.PeopleDirectory"

        /**
         * Requests that the system People Directory refresh its cached records for this
         * application.
         *
         * Call this after large-scale state changes such as an account synchronization.
         *
         * The first call to this library in a process may take a round trip to Google Play services
         * to find out whether this device supports the People Directory.
         *
         * @param context The application context.
         * @return `true` if the refresh request was accepted by the system, `false` if the system
         *   People Directory is unavailable or declined the request.
         */
        @JvmStatic
        public suspend fun requestRefresh(context: Context): Boolean =
            TODO("Implemented in the follow-up change")

        /**
         * Notifies the system People Directory that records matching [query] have changed.
         *
         * Call this when specific person records change.
         *
         * The first call to this library in a process may take a round trip to Google Play services
         * to find out whether this device supports the People Directory.
         *
         * @param context The application context.
         * @param query The query identifying the modified persons.
         * @return `true` if the notification was accepted by the system, `false` if the system
         *   People Directory is unavailable or declined the request.
         */
        @JvmStatic
        public suspend fun notifyChange(context: Context, query: PersonQuery): Boolean =
            TODO("Implemented in the follow-up change")

        /**
         * Notifies the system People Directory that groups matching [query] have changed.
         *
         * Call this when specific group records change.
         *
         * The first call to this library in a process may take a round trip to Google Play services
         * to find out whether this device supports the People Directory.
         *
         * @param context The application context.
         * @param query The query identifying the modified groups.
         * @return `true` if the notification was accepted by the system, `false` if the system
         *   People Directory is unavailable or declined the request.
         */
        @JvmStatic
        public suspend fun notifyChange(context: Context, query: GroupQuery): Boolean =
            TODO("Implemented in the follow-up change")

        private const val MATCH_PERSONS = 1
        private const val MATCH_GROUPS = 2
    }
}
