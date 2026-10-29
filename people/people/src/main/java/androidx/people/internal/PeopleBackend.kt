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

package androidx.people.internal

import android.app.PendingIntent
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.people.DirectoryInfo
import androidx.people.GroupQuery
import androidx.people.GroupQueryResult
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonQuery
import androidx.people.PeopleInteraction
import androidx.people.PersonQuery
import androidx.people.PersonQueryResult
import kotlinx.coroutines.flow.Flow

/**
 * Every operation this library performs against a People Provider, for both client applications and
 * People Directory applications.
 *
 * TODO(b/562550480): Split into a publisher-side backend in `:people` (notify and refresh, backed
 *   by GMS `PeopleDirectoryCompatClient`) and a client-side backend in `:people-client` (reads and
 *   interactions, backed by GMS `PeopleProviderCompatClient`).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PeopleBackend {

    /**
     * Whether a People Provider serves this device; `false` only for [UnavailablePeopleBackend].
     */
    public val isAvailable: Boolean

    // For client applications.

    /** Returns the People Directory applications registered on the device. */
    public suspend fun queryDirectories(): List<DirectoryInfo>

    /** Observes the People Directory applications registered on the device. */
    public fun observeDirectories(): Flow<List<DirectoryInfo>>

    /** Returns the people matching [query]. */
    public suspend fun queryPersons(query: PersonQuery): PersonQueryResult

    /** Observes the people matching [query]. */
    public fun observePersons(query: PersonQuery): Flow<PersonQueryResult>

    /** Returns the groups matching [query]. */
    public suspend fun queryGroups(query: GroupQuery): GroupQueryResult

    /** Observes the groups matching [query]. */
    public fun observeGroups(query: GroupQuery): Flow<GroupQueryResult>

    /** Creates a [PendingIntent] that starts [interaction], or returns `null` if it is declined. */
    public suspend fun createInteractionRequest(interaction: PeopleInteraction): PendingIntent?

    // For People Directory applications. Each call is about the calling application's own records.

    /**
     * Notifies the People Provider that the calling application's people matching [query] have
     * changed.
     */
    public suspend fun notifyChange(query: DirectoryPersonQuery): Boolean

    /**
     * Notifies the People Provider that the calling application's groups matching [query] have
     * changed.
     */
    public suspend fun notifyChange(query: DirectoryGroupQuery): Boolean

    /** Requests that the People Provider refresh every record the calling application publishes. */
    public suspend fun requestRefresh(): Boolean
}

/**
 * Creates the backend for a People Provider that `androidx.people:people` cannot reference
 * directly.
 *
 * Implemented by `androidx.people:people-play-services`, which [PeopleBackendResolver] loads by
 * name so that applications which do not include that artifact do not pay for it.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface PeopleBackendFactory {

    /**
     * Returns a backend if this People Provider serves the calling application, or `null` if it
     * definitively does not.
     *
     * Throws if the answer could not be determined, for example because the connection to Google
     * Play services failed, so that [PeopleBackendResolver] asks again on the next call.
     */
    public suspend fun create(context: Context): PeopleBackend?
}
