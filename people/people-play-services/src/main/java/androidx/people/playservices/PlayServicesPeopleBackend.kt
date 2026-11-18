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

package androidx.people.playservices

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.util.Log
import androidx.people.DirectoryInfo
import androidx.people.GroupQuery
import androidx.people.GroupQueryResult
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonQuery
import androidx.people.PeopleInteraction
import androidx.people.PersonQuery
import androidx.people.PersonQueryResult
import androidx.people.internal.PeopleBackend
import androidx.people.internal.failedGroupQueryResultOf
import androidx.people.internal.failedPersonQueryResultOf
import androidx.people.internal.groupQueryResultOf
import androidx.people.internal.personQueryResultOf
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.people.peopleprovider.GetDirectoriesRequest
import com.google.android.gms.people.peopleprovider.NotifyDirectoryChangeRequest
import com.google.android.gms.people.peopleprovider.PeopleDirectoryCompatClient
import com.google.android.gms.people.peopleprovider.PeopleProviderCompatClient
import com.google.android.gms.people.peopleprovider.PeopleProviderCompatClient.OnDataChangedListener
import com.google.android.gms.people.peopleprovider.QueryGroupsRequest
import com.google.android.gms.people.peopleprovider.QueryPersonsRequest
import com.google.android.gms.people.peopleprovider.RequestDirectoryRefreshRequest
import com.google.android.gms.tasks.Task
import java.io.Closeable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Backend for the Google Play services backport of the People Provider, on devices below Android
 * 37.2. The only code in this library that talks to Google Play services after
 * [PlayServicesPeopleBackendFactory] has confirmed support.
 *
 * A Google Play services failure, reported as [ApiException] or, by an installed version that does
 * not support a call, as [UnsupportedOperationException], is rethrown as [IllegalStateException]
 * with the original as its cause, so that callers do not need Google Play services classes to
 * handle it. [SecurityException] propagates as it does from the platform People Provider.
 * [notifyChange] and [requestRefresh] instead return `false` on any of these failures, as
 * `PeopleDirectoryContentProvider` documents.
 *
 * Google Play services enforces `READ_CONTACTS`, as the platform People Provider does, and a
 * missing grant propagates to the caller, so the permission lint is suppressed.
 *
 * @param providerClient the Google Play services client for client applications.
 * @param directoryClient the Google Play services client for People Directory applications.
 * @param packageName the calling application's package name, which change notifications are about.
 */
@SuppressLint("MissingPermission")
internal class PlayServicesPeopleBackend(
    private val providerClient: PeopleProviderCompatClient,
    private val directoryClient: PeopleDirectoryCompatClient,
    private val packageName: String,
) : PeopleBackend {

    override val isAvailable: Boolean = true

    override suspend fun queryDirectories(): List<DirectoryInfo> = call {
        providerClient.getDirectories(GetDirectoriesRequest.builder().build())
    }
        .directories
        .map { it.toDirectoryInfo() }

    /**
     * Emits the registered People Directory applications once.
     *
     * Google Play services reports changes to people and groups but not to the set of People
     * Directory applications, so a later change is seen when the flow is collected again.
     */
    override fun observeDirectories(): Flow<List<DirectoryInfo>> = flow { emit(queryDirectories()) }

    override suspend fun queryPersons(query: PersonQuery): PersonQueryResult {
        // Nothing to ask: for an empty query, every part of the result is empty.
        val request = query.toQueryPersonsRequest() ?: return failedPersonQueryResultOf(query)
        return readPersons(query, request)
    }

    override fun observePersons(query: PersonQuery): Flow<PersonQueryResult> {
        val request =
            query.toQueryPersonsRequest() ?: return flowOf(failedPersonQueryResultOf(query))
        return observe(
            register = { listener -> providerClient.observePersons(request, listener) },
            read = { readPersons(query, request) },
        )
    }

    override suspend fun queryGroups(query: GroupQuery): GroupQueryResult {
        val request = query.toQueryGroupsRequest() ?: return failedGroupQueryResultOf(query)
        return readGroups(query, request)
    }

    override fun observeGroups(query: GroupQuery): Flow<GroupQueryResult> {
        val request = query.toQueryGroupsRequest() ?: return flowOf(failedGroupQueryResultOf(query))
        return observe(
            register = { listener -> providerClient.observeGroups(request, listener) },
            read = { readGroups(query, request) },
        )
    }

    override suspend fun createInteractionRequest(interaction: PeopleInteraction): PendingIntent? {
        // Converted before the call, so that an invalid target fails the same way on every device.
        val request = interaction.toCreateInteractionPendingIntentRequest()
        return call { providerClient.createInteractionPendingIntent(request) }.pendingIntent
    }

    override suspend fun notifyChange(query: DirectoryPersonQuery): Boolean =
        notifyChange(personsChangedRequestOf(packageName, query.criteria))

    override suspend fun notifyChange(query: DirectoryGroupQuery): Boolean =
        notifyChange(groupsChangedRequestOf(packageName, query.criteria))

    /**
     * Sends [request] and returns whether Google Play services accepted it, or `false` if it
     * failed, as `PeopleDirectoryContentProvider` documents for an unavailable or declining People
     * Directory. Cancellation still propagates.
     */
    internal suspend fun notifyChange(request: NotifyDirectoryChangeRequest): Boolean =
        try {
            directoryClient.notifyDirectoryChange(request).await().isAccepted
        } catch (e: ApiException) {
            Log.w(TAG, "Google Play services People Provider failed to notify a change", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Google Play services People Provider failed to notify a change", e)
            false
        } catch (e: UnsupportedOperationException) {
            Log.w(
                TAG,
                "The installed version of Google Play services does not support notifying a change",
                e,
            )
            false
        }

    /**
     * Requests a refresh and returns whether Google Play services accepted it, or `false` if it
     * failed, as [notifyChange] does.
     */
    override suspend fun requestRefresh(): Boolean =
        try {
            directoryClient
                .requestDirectoryRefresh(RequestDirectoryRefreshRequest())
                .await()
                .isAccepted
        } catch (e: ApiException) {
            Log.w(TAG, "Google Play services People Provider failed to request a refresh", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Google Play services People Provider failed to request a refresh", e)
            false
        } catch (e: UnsupportedOperationException) {
            Log.w(
                TAG,
                "The installed version of Google Play services does not support requesting a " +
                    "refresh",
                e,
            )
            false
        }

    /**
     * Reads the result of [query], sent as [request], always releasing the returned buffer. The
     * criteria of People Directory applications that Google Play services could not reach are
     * unhandled.
     */
    private suspend fun readPersons(
        query: PersonQuery,
        request: QueryPersonsRequest,
    ): PersonQueryResult = callClosing {
        providerClient.queryPersons(request)
    }
        .use { response ->
            personQueryResultOf(
                query,
                matches = response.toPersonsList().toPersonMatches(),
                // TODO (b/581944630): update once GMS supports returning unhandled queries.
                unhandledQuery = query.forDirectories(response.unreachablePackages),
            )
        }

    /** Reads the result of [query], sent as [request], as [readPersons] does for people. */
    private suspend fun readGroups(
        query: GroupQuery,
        request: QueryGroupsRequest,
    ): GroupQueryResult = callClosing {
        providerClient.queryGroups(request)
    }
        .use { response ->
            groupQueryResultOf(
                query,
                matches = response.toGroupsList().toGroupMatches(),
                // TODO (b/581944630): update once GMS supports returning unhandled queries.
                unhandledQuery = query.forDirectories(response.unreachablePackages),
            )
        }

    /**
     * Emits [read] once, then again after each change that Google Play services reports through the
     * listener passed to [register].
     *
     * The listener is registered before the first read, so that a change between the two is not
     * missed. Each collection registers its own listener, since Google Play services keys
     * registrations by listener, and unregisters it when collection stops.
     */
    private fun <T> observe(
        register: (OnDataChangedListener) -> Task<Void>,
        read: suspend () -> T,
    ): Flow<T> = channelFlow {
        val changes = Channel<Unit>(Channel.CONFLATED)
        val listener = OnDataChangedListener { changes.trySend(Unit) }
        try {
            // Registered inside the try, so that a registration which completes after the
            // collector is cancelled is still undone. Unregistering a listener that was never
            // registered is harmless.
            call { register(listener) }
            send(read())
            for (change in changes) send(read())
        } finally {
            stopObserving(listener)
        }
    }
        .conflate()
        .distinctUntilChanged()

    /** Unregisters [listener] without waiting, since the collector is already gone. */
    private fun stopObserving(listener: OnDataChangedListener) {
        providerClient.stopObserving(listener).addOnFailureListener { e ->
            Log.w(TAG, "Failed to stop observing Google Play services People Provider", e)
        }
    }

    private companion object {
        const val TAG = "PlayServicesPeople"
    }
}

/**
 * Awaits the [Task] that [block] starts, rethrowing an [ApiException], or the
 * [UnsupportedOperationException] with which an installed Google Play services version reports a
 * call it does not support, as [IllegalStateException] with the original as its cause.
 */
private suspend inline fun <T> call(block: () -> Task<T>): T =
    try {
        block().await()
    } catch (e: ApiException) {
        throw IllegalStateException(
            "Google Play services People Provider call failed with status ${e.statusCode}",
            e,
        )
    } catch (e: UnsupportedOperationException) {
        // Includes UnsupportedApiCallException.
        throw IllegalStateException(
            "The installed version of Google Play services does not support this People " +
                "Provider call",
            e,
        )
    }

/**
 * Awaits the [Task] that [block] starts, as [call] does, and closes its result if it arrives after
 * the caller is cancelled, since the caller then never receives it to close.
 */
private suspend inline fun <T : Closeable> callClosing(block: () -> Task<T>): T {
    val task = block()
    try {
        return call { task }
    } catch (e: CancellationException) {
        // Runs at once if the task already succeeded, and otherwise when it does.
        task.addOnSuccessListener(Runnable::run) { it.close() }
        throw e
    }
}
