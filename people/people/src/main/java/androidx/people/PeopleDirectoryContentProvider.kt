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
import androidx.people.internal.PeopleBackendResolver
import androidx.people.internal.directoryGroupQuery
import androidx.people.internal.directoryPersonQuery
import androidx.people.internal.platform.PeopleContractCompat
import androidx.people.internal.platform.groupCriteriaByPackage
import androidx.people.internal.platform.personCriteriaByPackage
import androidx.people.internal.platform.toCursor

/**
 * Base [ContentProvider] that a People Directory application extends to publish its people and
 * groups to the Android People Directory.
 *
 * This class implements URI matching for the `persons` and `groups` paths, parsing of the query
 * argument bundle into [DirectoryPersonQuery] and [DirectoryGroupQuery], and serialization of
 * [DirectoryPersonResult] and [DirectoryGroupResult] into the required cursor shape. Subclasses
 * supply only the data, by implementing [queryPersons] and [queryGroups].
 *
 * Inside a subclass body, the nested types and the companion builders can be used without
 * qualification, for example `DirectoryPersonQuery` and `directoryPersonResult { }`. In the class
 * header, such as primary constructor parameters, Kotlin requires the qualified name, for example
 * `PeopleDirectoryContentProvider.DirectoryPersonResult`.
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
 * data to its internal database and provide to privileged client applications. Call
 * [notifyPersonsChanged], [notifyGroupsChanged], or [requestRefresh] whenever the published data
 * changes, so that the internal database is invalidated and clients observing the data are
 * notified.
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

    override fun getType(uri: Uri): String? {
        return when (uriMatcher.match(uri)) {
            MATCH_PERSONS -> PeopleContractCompat.Persons.CONTENT_TYPE
            MATCH_GROUPS -> PeopleContractCompat.Groups.CONTENT_TYPE
            else -> null
        }
    }

    /**
     * Answers the sub-query of [queryArgs] addressed to this application through [queryPersons] or
     * [queryGroups], and returns the result in the People Directory cursor format.
     *
     * @throws IllegalArgumentException if [queryArgs] contain no criteria addressed to this
     *   application.
     */
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?,
    ): Cursor? {
        val directoryPackage =
            requireNotNull(context) { "The provider is not attached" }.packageName
        return when (uriMatcher.match(uri)) {
            MATCH_PERSONS -> {
                // Answer only the sub-query addressed to this application.
                val criteria =
                    requireNotNull(queryArgs.personCriteriaByPackage()[directoryPackage]) {
                        "The query arguments contain no person criteria for $directoryPackage"
                    }
                val query = DirectoryPersonQuery(criteria)
                queryPersons(query, cancellationSignal)
                    .toCursor(directoryPackage, projection, query.criteria)
            }
            MATCH_GROUPS -> {
                val criteria =
                    requireNotNull(queryArgs.groupCriteriaByPackage()[directoryPackage]) {
                        "The query arguments contain no group criteria for $directoryPackage"
                    }
                val query = DirectoryGroupQuery(criteria)
                queryGroups(query, cancellationSignal)
                    .toCursor(directoryPackage, projection, query.criteria)
            }
            else -> null
        }
    }

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
     * Returns this directory's answer to [query].
     *
     * Called on a background thread, and may block. Build the result with [directoryPersonResult]:
     * - Report each person that matches once, with [DirectoryPersonResultScope.addMatch], naming
     *   every criterion that matched it, for example both a [Criterion.Phone] and a
     *   [Criterion.Email] when the same person matches two inputs.
     * - Report each criterion that was not evaluated, for example because it is not cached yet or
     *   its type is not supported, with [DirectoryPersonResultScope.addUnhandled]. A criterion is
     *   either matched or unhandled, not both.
     *
     * A criterion that is neither matched nor reported as unhandled is treated as evaluated with no
     * match. Each reported criterion must be echoed exactly as it appears in [query]; criteria that
     * do not appear in [query] are dropped. The order of matches is not significant and is not
     * preserved for client applications.
     *
     * @param query the criteria addressed to this directory.
     * @param cancellationSignal signals that the caller has abandoned the query, or `null`.
     */
    public abstract fun queryPersons(
        query: DirectoryPersonQuery,
        cancellationSignal: CancellationSignal?,
    ): DirectoryPersonResult

    /**
     * Returns this directory's answer to [query].
     *
     * Called on a background thread, and may block. Build the result with [directoryGroupResult],
     * following the same rules as [queryPersons]. Groups are matched by their lookup key, reported
     * as [Criterion.GroupLookupKey].
     *
     * @param query the criteria addressed to this directory.
     * @param cancellationSignal signals that the caller has abandoned the query, or `null`.
     */
    public abstract fun queryGroups(
        query: DirectoryGroupQuery,
        cancellationSignal: CancellationSignal?,
    ): DirectoryGroupResult

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

    /**
     * The person criteria a client query addresses to this People Directory application.
     *
     * A client's person query is made of one [DirectoryPersonQuery] per People Directory
     * application. Criteria are combined with OR: a person is a match if it matches any criterion.
     */
    public class DirectoryPersonQuery internal constructor(criteria: Set<Criterion>) {
        /** The criteria to evaluate, in the order the client supplied them. Never empty. */
        public val criteria: Set<Criterion> = criteria.toSet()

        init {
            require(this.criteria.isNotEmpty()) { "A person query needs at least one criterion." }
        }

        override fun equals(other: Any?): Boolean =
            this === other || (other is DirectoryPersonQuery && criteria == other.criteria)

        override fun hashCode(): Int = criteria.hashCode()

        override fun toString(): String = "DirectoryPersonQuery(criteria=$criteria)"
    }

    /**
     * This People Directory application's answer to a [DirectoryPersonQuery].
     *
     * Build one with [directoryPersonResult]. A client's person query result is made of one
     * [DirectoryPersonResult] per People Directory application.
     */
    public class DirectoryPersonResult
    internal constructor(
        /** The matching people, with the criteria each one matched. The order is unspecified. */
        public val matches: Set<PersonMatch>,
        /**
         * The criteria this directory did not evaluate, for example because they were skipped,
         * timed out, or are not supported.
         *
         * This is not an error. A criterion that was evaluated and matched nothing is in neither
         * [matches] nor [unhandledCriteria].
         */
        public val unhandledCriteria: Set<Criterion>,
    ) {
        override fun equals(other: Any?): Boolean =
            this === other ||
                (other is DirectoryPersonResult &&
                    matches == other.matches &&
                    unhandledCriteria == other.unhandledCriteria)

        override fun hashCode(): Int = 31 * matches.hashCode() + unhandledCriteria.hashCode()

        override fun toString(): String =
            "DirectoryPersonResult(matches=$matches, unhandledCriteria=$unhandledCriteria)"
    }

    /** Receiver scope for building a [DirectoryPersonResult] with [directoryPersonResult]. */
    @PeopleQueryDsl
    public class DirectoryPersonResultScope internal constructor() {
        private val matches = mutableSetOf<PersonMatch>()
        private val unhandled = mutableSetOf<Criterion>()

        /** Reports that [person] matched [criterion] and every criterion in [moreCriteria]. */
        public fun addMatch(person: Person, criterion: Criterion, vararg moreCriteria: Criterion) {
            addMatch(person, listOf(criterion) + moreCriteria)
        }

        /**
         * Reports that [person] matched every criterion in [criteria].
         *
         * @throws IllegalArgumentException if [criteria] is empty.
         */
        public fun addMatch(person: Person, criteria: Iterable<Criterion>) {
            matches += PersonMatch(person, criteria.toSet())
        }

        /** Reports every match in [matches]. */
        public fun addMatches(vararg matches: PersonMatch) {
            this.matches += matches
        }

        /** Reports every match in [matches]. */
        public fun addMatches(matches: Iterable<PersonMatch>) {
            this.matches += matches
        }

        /** Reports that this directory did not evaluate [criteria]. */
        public fun addUnhandled(vararg criteria: Criterion) {
            unhandled += criteria
        }

        /** Reports that this directory did not evaluate [criteria]. */
        public fun addUnhandled(criteria: Iterable<Criterion>) {
            unhandled += criteria
        }

        internal fun build(): DirectoryPersonResult = DirectoryPersonResult(matches, unhandled)
    }

    /**
     * The group criteria a client query addresses to this People Directory application.
     *
     * A client's group query is made of one [DirectoryGroupQuery] per People Directory application.
     */
    public class DirectoryGroupQuery internal constructor(criteria: Set<Criterion>) {
        /**
         * The criteria to evaluate, in the order the client supplied them. Never empty, and
         * currently always [Criterion.GroupLookupKey] values.
         */
        public val criteria: Set<Criterion> = criteria.toSet()

        init {
            require(this.criteria.isNotEmpty()) { "A group query needs at least one criterion." }
        }

        override fun equals(other: Any?): Boolean =
            this === other || (other is DirectoryGroupQuery && criteria == other.criteria)

        override fun hashCode(): Int = criteria.hashCode()

        override fun toString(): String = "DirectoryGroupQuery(criteria=$criteria)"
    }

    /**
     * This People Directory application's answer to a [DirectoryGroupQuery].
     *
     * Build one with [directoryGroupResult].
     */
    public class DirectoryGroupResult
    internal constructor(
        /** The matching groups, with the criteria each one matched. The order is unspecified. */
        public val matches: Set<GroupMatch>,
        /**
         * The criteria this directory did not evaluate. See
         * [DirectoryPersonResult.unhandledCriteria].
         */
        public val unhandledCriteria: Set<Criterion>,
    ) {
        override fun equals(other: Any?): Boolean =
            this === other ||
                (other is DirectoryGroupResult &&
                    matches == other.matches &&
                    unhandledCriteria == other.unhandledCriteria)

        override fun hashCode(): Int = 31 * matches.hashCode() + unhandledCriteria.hashCode()

        override fun toString(): String =
            "DirectoryGroupResult(matches=$matches, unhandledCriteria=$unhandledCriteria)"
    }

    /** Receiver scope for building a [DirectoryGroupResult] with [directoryGroupResult]. */
    @PeopleQueryDsl
    public class DirectoryGroupResultScope internal constructor() {
        private val matches = mutableSetOf<GroupMatch>()
        private val unhandled = mutableSetOf<Criterion>()

        /** Reports that [group] matched [criterion] and every criterion in [moreCriteria]. */
        public fun addMatch(group: Group, criterion: Criterion, vararg moreCriteria: Criterion) {
            addMatch(group, listOf(criterion) + moreCriteria)
        }

        /**
         * Reports that [group] matched every criterion in [criteria].
         *
         * @throws IllegalArgumentException if [criteria] is empty.
         */
        public fun addMatch(group: Group, criteria: Iterable<Criterion>) {
            matches += GroupMatch(group, criteria.toSet())
        }

        /** Reports every match in [matches]. */
        public fun addMatches(vararg matches: GroupMatch) {
            this.matches += matches
        }

        /** Reports every match in [matches]. */
        public fun addMatches(matches: Iterable<GroupMatch>) {
            this.matches += matches
        }

        /** Reports that this directory did not evaluate [criteria]. */
        public fun addUnhandled(vararg criteria: Criterion) {
            unhandled += criteria
        }

        /** Reports that this directory did not evaluate [criteria]. */
        public fun addUnhandled(criteria: Iterable<Criterion>) {
            unhandled += criteria
        }

        internal fun build(): DirectoryGroupResult = DirectoryGroupResult(matches, unhandled)
    }

    public companion object {
        /**
         * Name of the manifest property and meta-data tag a `ContentProvider` declares to register
         * itself as a People Directory.
         */
        public const val PROPERTY_NAME: String = "android.content.PeopleDirectory"

        /**
         * Builds the result of [queryPersons], following the rules described there.
         *
         * ```
         * override fun queryPersons(query: DirectoryPersonQuery, cancellationSignal: CancellationSignal?) =
         *     directoryPersonResult {
         *         for (criterion in query.criteria) {
         *             when (criterion) {
         *                 is Criterion.Phone -> cache[criterion.number]
         *                     ?.let { addMatch(it.toPerson(), criterion) }
         *                     ?: addUnhandled(criterion) // not cached yet
         *                 else -> addUnhandled(criterion) // not supported
         *             }
         *         }
         *     }
         * ```
         */
        @JvmStatic
        public fun directoryPersonResult(
            block: DirectoryPersonResultScope.() -> Unit
        ): DirectoryPersonResult = DirectoryPersonResultScope().apply(block).build()

        /**
         * Builds the result of [queryGroups], following the same rules as [directoryPersonResult].
         */
        @JvmStatic
        public fun directoryGroupResult(
            block: DirectoryGroupResultScope.() -> Unit
        ): DirectoryGroupResult = DirectoryGroupResultScope().apply(block).build()

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
            PeopleBackendResolver.resolve(context).requestRefresh()

        /**
         * Notifies the system People Directory that the people of this application matching the
         * criteria in [block] have changed.
         *
         * ```
         * PeopleDirectoryContentProvider.notifyPersonsChanged(context) { addLookupKeys(changedId) }
         * ```
         *
         * The first call to this library in a process may take a round trip to Google Play services
         * to find out whether this device supports the People Directory.
         *
         * @param context The application context.
         * @param block adds the criteria identifying the modified people.
         * @return `true` if the notification was accepted by the system, `false` if the system
         *   People Directory is unavailable or declined the request.
         * @throws IllegalArgumentException if [block] adds no criterion.
         */
        @JvmStatic
        public suspend fun notifyPersonsChanged(
            context: Context,
            block: PersonCriteriaScope.() -> Unit,
        ): Boolean {
            val query = directoryPersonQuery(block)
            return PeopleBackendResolver.resolve(context).notifyChange(query)
        }

        /**
         * Notifies the system People Directory that the groups of this application matching the
         * criteria in [block] have changed.
         *
         * The first call to this library in a process may take a round trip to Google Play services
         * to find out whether this device supports the People Directory.
         *
         * @param context The application context.
         * @param block adds the criteria identifying the modified groups.
         * @return `true` if the notification was accepted by the system, `false` if the system
         *   People Directory is unavailable or declined the request.
         * @throws IllegalArgumentException if [block] adds no criterion.
         */
        @JvmStatic
        public suspend fun notifyGroupsChanged(
            context: Context,
            block: GroupCriteriaScope.() -> Unit,
        ): Boolean {
            val query = directoryGroupQuery(block)
            return PeopleBackendResolver.resolve(context).notifyChange(query)
        }

        private const val MATCH_PERSONS = 1
        private const val MATCH_GROUPS = 2
    }
}
