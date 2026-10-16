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

package androidx.people.internal.platform

import android.app.PendingIntent
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.people.DirectoryInfo
import androidx.people.Group
import androidx.people.GroupQuery
import androidx.people.PeopleInteraction
import androidx.people.Person
import androidx.people.PersonQuery
import androidx.people.internal.PeopleBackend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backend for the platform People Provider, the `com.android.people` provider available on Android
 * 37.2 and above. The only code in this library that talks to it.
 *
 * @param coroutineDispatcher the dispatcher that blocking provider calls run on.
 *
 * TODO(b/562550480): Once the Android 37.2 SDK is in prebuilts, annotate with
 *   `@RequiresApi(Build.VERSION_CODES_FULL.<37.2>)` and replace each call that speaks the platform
 *   People Provider's wire protocol with the corresponding `android.provider.PeopleContract` call.
 */
internal class PlatformPeopleBackend(
    private val context: Context,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PeopleBackend {

    override val isAvailable: Boolean = true

    override suspend fun queryDirectories(): List<DirectoryInfo> =
        withContext(coroutineDispatcher) { readDirectories() }

    override fun observeDirectories(): Flow<List<DirectoryInfo>> =
        observe(
            uri = { PeopleContractCompat.CONTENT_NOTIFICATION_URI },
            read = { QueryResult(readDirectories(), null) },
        )

    override suspend fun queryPersons(queries: List<PersonQuery>): List<Person> {
        val queryArgs = queries.toQueryArgs()
        return withContext(coroutineDispatcher) { readPersons(queryArgs).records }
    }

    override fun observePersons(queries: List<PersonQuery>): Flow<List<Person>> {
        val queryArgs = queries.toQueryArgs()
        return observe(read = { readPersons(queryArgs) })
    }

    override suspend fun queryGroups(queries: List<GroupQuery>): List<Group> {
        val queryArgs = queries.toQueryArgs()
        return withContext(coroutineDispatcher) { readGroups(queryArgs).records }
    }

    override fun observeGroups(queries: List<GroupQuery>): Flow<List<Group>> {
        val queryArgs = queries.toQueryArgs()
        return observe(read = { readGroups(queryArgs) })
    }

    override suspend fun createInteractionRequest(interaction: PeopleInteraction): PendingIntent? {
        val extras =
            Bundle().apply {
                putString(
                    PeopleContractCompat.Intents.EXTRA_PEOPLE_INTERACTION_TYPE,
                    interaction.capability.toInteractionType(),
                )
                putParcelable(
                    PeopleContractCompat.Intents.EXTRA_PEOPLE_QUERY_ARGS,
                    interaction.targetQuery.toInteractionQueryArgs(),
                )
            }
        val result =
            withContext(coroutineDispatcher) {
                callProvider {
                    context.contentResolver.call(
                        PeopleContractCompat.AUTHORITY_URI,
                        PeopleContractCompat.Intents.METHOD_CREATE_INTERACTION_INTENT,
                        null,
                        extras,
                    )
                }
            }
        return result?.let {
            BundleCompat.getParcelable(
                it,
                PeopleContractCompat.Intents.EXTRA_PEOPLE_INTERACTION_PENDING_INTENT,
                PendingIntent::class.java,
            )
        }
    }

    override suspend fun notifyChange(query: PersonQuery): Boolean =
        notifyChange(PeopleContractCompat.Persons.CONTENT_URI, query.toQueryArgs())

    override suspend fun notifyChange(query: GroupQuery): Boolean =
        notifyChange(PeopleContractCompat.Groups.CONTENT_URI, query.toQueryArgs())

    override suspend fun requestRefresh(): Boolean = refresh(extras = null)

    /** Asks the platform People Provider to refresh the records that depend on [dependencyUri]. */
    private suspend fun notifyChange(dependencyUri: Uri, queryArgs: Bundle): Boolean =
        refresh(
            Bundle().apply {
                // Caller-supplied arguments are applied first so that the dependency URI always
                // wins.
                putAll(queryArgs)
                putParcelable(PeopleContractCompat.QUERY_ARG_DEPENDENCY_URI, dependencyUri)
            }
        )

    private suspend fun refresh(extras: Bundle?): Boolean =
        withContext(coroutineDispatcher) {
            callProvider {
                context.contentResolver.refresh(PeopleContractCompat.AUTHORITY_URI, extras, null)
            } ?: false
        }

    /**
     * Emits [read] now and again on every change to the URI the first read advertised, falling back
     * to [uri] when it advertises none.
     */
    private fun <T> observe(
        uri: () -> Uri = { PeopleContractCompat.CONTENT_NOTIFICATION_URI },
        read: () -> QueryResult<T>,
    ): Flow<List<T>> = callbackFlow {
        val resolver = context.contentResolver

        val initial = read()
        trySend(initial.records)

        val signals = Channel<Unit>(Channel.CONFLATED)
        launch {
            for (signal in signals) {
                trySend(read().records)
            }
        }

        // A null handler dispatches onChange on a binder thread, so the event is signaled onto
        // this flow's own context instead of blocking the binder thread.
        val observer =
            object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    signals.trySend(Unit)
                }
            }

        resolver.registerContentObserver(initial.notificationUri ?: uri(), true, observer)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }
        .flowOn(coroutineDispatcher)
        .conflate()
        .distinctUntilChanged()

    private fun readDirectories(): List<DirectoryInfo> =
        queryProvider(PeopleContractCompat.Directory.CONTENT_URI, null)
            ?.use { it.toDirectories() }
            .orEmpty()

    private fun readPersons(queryArgs: Bundle): QueryResult<Person> =
        queryProvider(PeopleContractCompat.Persons.CONTENT_URI, queryArgs)?.use {
            QueryResult(it.toPersons(), it.notificationUri())
        } ?: QueryResult(emptyList(), null)

    private fun readGroups(queryArgs: Bundle): QueryResult<Group> =
        queryProvider(PeopleContractCompat.Groups.CONTENT_URI, queryArgs)?.use {
            QueryResult(it.toGroups(), it.notificationUri())
        } ?: QueryResult(emptyList(), null)

    private fun queryProvider(uri: Uri, queryArgs: Bundle?): Cursor? = callProvider {
        context.contentResolver.query(uri, null, queryArgs, null)
    }

    /**
     * Runs [block] against the platform People Provider, treating only its absence as no result.
     *
     * `ContentResolver` reports an unknown authority with [IllegalArgumentException], which is how
     * a missing platform People Provider surfaces. Any other failure, including [SecurityException]
     * for a missing `READ_CONTACTS` grant, propagates to the caller.
     */
    private inline fun <T> callProvider(block: () -> T): T? =
        try {
            block()
        } catch (e: IllegalArgumentException) {
            null
        }

    private fun Cursor.notificationUri(): Uri? =
        extras?.getString(PeopleContractCompat.EXTRA_NOTIFICATION_URI)?.toUri()

    /** Records parsed from a cursor, plus the query-specific notification URI it advertised. */
    private class QueryResult<T>(val records: List<T>, val notificationUri: Uri?)

    companion object {
        /**
         * Returns the platform backend if the platform People Provider serves this device: Android
         * 37.2 or higher with `com.android.people` installed. Returns `null` otherwise, including
         * on devices on Android 37.2 or higher without the provider.
         *
         * Main-safe. The version is checked first so that the package manager is only queried on
         * devices that can have the platform People Provider at all.
         */
        suspend fun createIfSupported(
            context: Context,
            coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
        ): PlatformPeopleBackend? {
            if (!isAtLeastPeopleContract()) return null
            val installed =
                withContext(coroutineDispatcher) {
                    context.packageManager.resolveContentProvider(
                        PeopleContractCompat.AUTHORITY,
                        0,
                    ) != null
                }
            return if (installed) PlatformPeopleBackend(context, coroutineDispatcher) else null
        }

        /**
         * Whether the platform `PeopleContract` APIs are available, mirroring
         * `androidx.core.os.BuildCompat.isAtLeastB_1`.
         *
         * TODO(b/562550480): Replace with `Build.VERSION.SDK_INT_FULL >=
         *   Build.VERSION_CODES_FULL.<37.2>` once the Android 37.2 SDK is in prebuilts.
         */
        private fun isAtLeastPeopleContract(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN &&
                Build.VERSION.SDK_INT_FULL >= SDK_INT_FULL_CINNAMON_BUN_2

        // TODO(b/562550480): Replace with Build.VERSION_CODES_FULL.CINNAMON_BUN_2 once available.
        /** `Build.VERSION.SDK_INT_FULL` of Android 37.2: `major * 100_000 + minor`. */
        private const val SDK_INT_FULL_CINNAMON_BUN_2 =
            Build.VERSION_CODES.CINNAMON_BUN * 100_000 + 2
    }
}
