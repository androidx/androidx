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

package androidx.people.testing

import android.app.PendingIntent
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import androidx.people.Capability
import androidx.people.Criterion
import androidx.people.DirectoryInfo
import androidx.people.Group
import androidx.people.GroupId
import androidx.people.GroupMatch
import androidx.people.PeopleDirectoryContentProvider.Companion.directoryGroupResult
import androidx.people.PeopleDirectoryContentProvider.Companion.directoryPersonResult
import androidx.people.Person
import androidx.people.PersonId
import androidx.people.PersonMatch
import androidx.people.internal.platform.PeopleContractCompat
import androidx.people.internal.platform.groupQueryArgsOf
import androidx.people.internal.platform.personQueryArgsOf
import androidx.people.internal.platform.toCursor
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Stands in for the platform People Provider (`com.android.people`) during tests.
 *
 * Stores domain records in memory and returns real cursors formatted according to
 * [PeopleContractCompat], while recording incoming refresh signals and call requests. Every stored
 * match is returned regardless of the query, and criteria reported as unhandled are returned in the
 * cursor extras under [PeopleContractCompat.EXTRA_UNHANDLED_QUERIES].
 */
class FakePlatformPeopleProvider : ContentProvider() {

    val persons = mutableListOf<PersonMatch>()
    val groups = mutableListOf<GroupMatch>()
    val directories = mutableListOf<DirectoryInfo>()

    /** Person criteria reported as unhandled, keyed by People Directory package. */
    val unhandledPersonCriteria = mutableMapOf<String, Set<Criterion>>()

    /** Group criteria reported as unhandled, keyed by People Directory package. */
    val unhandledGroupCriteria = mutableMapOf<String, Set<Criterion>>()

    var lastQueryArgs: Bundle? = null

    /** Number of `query` calls received. */
    var queryCount: Int = 0
        private set

    var lastCallExtras: Bundle? = null

    var lastRefreshExtras: Bundle? = null
    var refreshCount: Int = 0
        private set

    var interactionPendingIntent: PendingIntent? = null
    var notificationUri: Uri? = null

    /** Adds [records] to the provider's person store. */
    fun addPersons(vararg records: PersonMatch) {
        persons.addAll(records)
    }

    /** Adds [records] to the provider's person store. */
    fun addPersons(records: Collection<PersonMatch>) {
        persons.addAll(records)
    }

    /** Convenience overload to create and add a single [Person] record. */
    fun addPerson(
        id: PersonId,
        displayName: String? = null,
        photoUri: Uri? = null,
        capabilities: Set<Capability> = emptySet(),
        isKnown: Boolean = false,
        criteria: Set<Criterion> = emptySet(),
    ) {
        persons.add(
            PersonMatch(
                Person(
                    id = id,
                    displayName = displayName,
                    photoUri = photoUri,
                    capabilities = capabilities,
                    isKnown = isKnown,
                ),
                criteria,
            )
        )
    }

    /** Adds [records] to the provider's group store. */
    fun addGroups(vararg records: GroupMatch) {
        groups.addAll(records)
    }

    /** Adds [records] to the provider's group store. */
    fun addGroups(records: Collection<GroupMatch>) {
        groups.addAll(records)
    }

    /** Convenience overload to create and add a single [Group] record. */
    fun addGroup(
        id: GroupId,
        displayName: String? = null,
        criteria: Set<Criterion> = emptySet(),
    ) {
        groups.add(GroupMatch(Group(id = id, displayName = displayName), criteria))
    }

    /** Adds [records] to the provider's directory store. */
    fun addDirectories(vararg records: DirectoryInfo) {
        directories.addAll(records)
    }

    /** Adds [records] to the provider's directory store. */
    fun addDirectories(records: Collection<DirectoryInfo>) {
        directories.addAll(records)
    }

    /** Convenience overload to create and add a single [DirectoryInfo] record. */
    fun addDirectory(packageName: String, authority: String) {
        directories.add(DirectoryInfo(packageName = packageName, authority = authority))
    }

    /** Clears all stored records and recorded interaction state. */
    fun clear() {
        persons.clear()
        groups.clear()
        directories.clear()
        unhandledPersonCriteria.clear()
        unhandledGroupCriteria.clear()
        lastQueryArgs = null
        queryCount = 0
        lastCallExtras = null
        lastRefreshExtras = null
        refreshCount = 0
        interactionPendingIntent = null
        notificationUri = null
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        queryArgs: Bundle?,
        cancellationSignal: CancellationSignal?,
    ): Cursor? {
        lastQueryArgs = queryArgs
        queryCount++
        val extras = Bundle()
        val cursor =
            when (uri) {
                PeopleContractCompat.Persons.CONTENT_URI -> {
                    if (unhandledPersonCriteria.isNotEmpty()) {
                        extras.putBundle(
                            PeopleContractCompat.EXTRA_UNHANDLED_QUERIES,
                            personQueryArgsOf(unhandledPersonCriteria),
                        )
                    }
                    // The directory package only names the unhandled criteria, written above.
                    directoryPersonResult { addMatches(persons) }
                        .toCursor("", projection, persons.flatMapTo(mutableSetOf()) { it.criteria })
                }
                PeopleContractCompat.Groups.CONTENT_URI -> {
                    if (unhandledGroupCriteria.isNotEmpty()) {
                        extras.putBundle(
                            PeopleContractCompat.EXTRA_UNHANDLED_QUERIES,
                            groupQueryArgsOf(unhandledGroupCriteria),
                        )
                    }
                    directoryGroupResult { addMatches(groups) }
                        .toCursor("", projection, groups.flatMapTo(mutableSetOf()) { it.criteria })
                }
                PeopleContractCompat.Directory.CONTENT_URI -> directories.toCursor(projection)
                else -> null
            } ?: return null

        notificationUri?.let { notifUri ->
            extras.putString(PeopleContractCompat.EXTRA_NOTIFICATION_URI, notifUri.toString())
        }
        if (!extras.isEmpty) cursor.extras = extras
        return cursor
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun refresh(
        uri: Uri,
        extras: Bundle?,
        cancellationSignal: CancellationSignal?,
    ): Boolean {
        lastRefreshExtras = extras
        refreshCount++
        return true
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        lastCallExtras = extras
        return when (method) {
            PeopleContractCompat.Intents.METHOD_CREATE_INTERACTION_INTENT ->
                interactionPendingIntent?.let { intent ->
                    Bundle().apply {
                        putParcelable(
                            PeopleContractCompat.Intents.EXTRA_PEOPLE_INTERACTION_PENDING_INTENT,
                            intent,
                        )
                    }
                }
            else -> null
        }
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

    private fun List<DirectoryInfo>.toCursor(projection: Array<out String>? = null): Cursor {
        val columns =
            projection
                ?: arrayOf(
                    PeopleContractCompat.Directory.PACKAGE_NAME,
                    PeopleContractCompat.Directory.DIRECTORY_AUTHORITY,
                )
        val cursor = MatrixCursor(columns, size)
        for (directory in this) {
            cursor.addRow(
                columns.map { column ->
                    when (column) {
                        PeopleContractCompat.Directory.PACKAGE_NAME -> directory.packageName
                        PeopleContractCompat.Directory.DIRECTORY_AUTHORITY -> directory.authority
                        else -> null
                    }
                }
            )
        }
        return cursor
    }

    companion object {
        /**
         * Installs this provider into Robolectric's package manager and content resolver for
         * [PeopleContractCompat.AUTHORITY], applies [block] on it, and returns the instance.
         */
        fun install(
            context: Context = RuntimeEnvironment.getApplication(),
            block: FakePlatformPeopleProvider.() -> Unit = {},
        ): FakePlatformPeopleProvider {
            val providerInfo =
                ProviderInfo().apply {
                    authority = PeopleContractCompat.AUTHORITY
                    packageName = context.packageName
                    name = FakePlatformPeopleProvider::class.java.name
                }
            shadowOf(context.packageManager).addOrUpdateProvider(providerInfo)
            return Robolectric.buildContentProvider(FakePlatformPeopleProvider::class.java)
                .create(providerInfo)
                .get()
                .apply(block)
        }
    }
}

/**
 * A JUnit [TestRule] that installs a [FakePlatformPeopleProvider] before each test and clears it
 * after each test.
 */
class PlatformPeopleProviderRule(
    private val context: Context = RuntimeEnvironment.getApplication()
) : TestWatcher() {

    lateinit var provider: FakePlatformPeopleProvider
        private set

    override fun starting(description: Description) {
        provider = FakePlatformPeopleProvider.install(context)
    }

    override fun finished(description: Description) {
        if (::provider.isInitialized) {
            provider.clear()
        }
    }
}
