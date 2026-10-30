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

package androidx.people.client.internal

import android.app.PendingIntent
import androidx.people.DirectoryInfo
import androidx.people.GroupQuery
import androidx.people.GroupQueryResult
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonQuery
import androidx.people.PeopleInteraction
import androidx.people.PersonQuery
import androidx.people.PersonQueryResult
import androidx.people.internal.PeopleBackend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Backend that records what it was asked for and returns the results it was given.
 *
 * Querying persons or groups without a result fails.
 */
class FakePeopleBackend(
    override val isAvailable: Boolean = true,
    private val directories: List<DirectoryInfo> = emptyList(),
    private val personResult: PersonQueryResult? = null,
    private val groupResult: GroupQueryResult? = null,
    private val interactionRequest: PendingIntent? = null,
    var notifyChangeResult: Boolean = true,
    var requestRefreshResult: Boolean = true,
) : PeopleBackend {

    val personQueries = mutableListOf<PersonQuery>()
    val groupQueries = mutableListOf<GroupQuery>()
    val interactions = mutableListOf<PeopleInteraction>()
    val personChangeNotifications = mutableListOf<DirectoryPersonQuery>()
    val groupChangeNotifications = mutableListOf<DirectoryGroupQuery>()
    var refreshRequestCount = 0
        private set

    override suspend fun queryDirectories(): List<DirectoryInfo> = directories

    override fun observeDirectories(): Flow<List<DirectoryInfo>> = flowOf(directories)

    override suspend fun queryPersons(query: PersonQuery): PersonQueryResult {
        personQueries.add(query)
        return checkNotNull(personResult) { "No person result was given" }
    }

    override fun observePersons(query: PersonQuery): Flow<PersonQueryResult> {
        personQueries.add(query)
        return flowOf(checkNotNull(personResult) { "No person result was given" })
    }

    override suspend fun queryGroups(query: GroupQuery): GroupQueryResult {
        groupQueries.add(query)
        return checkNotNull(groupResult) { "No group result was given" }
    }

    override fun observeGroups(query: GroupQuery): Flow<GroupQueryResult> {
        groupQueries.add(query)
        return flowOf(checkNotNull(groupResult) { "No group result was given" })
    }

    override suspend fun createInteractionRequest(interaction: PeopleInteraction): PendingIntent? {
        interactions.add(interaction)
        return interactionRequest
    }

    override suspend fun notifyChange(query: DirectoryPersonQuery): Boolean {
        personChangeNotifications.add(query)
        return notifyChangeResult
    }

    override suspend fun notifyChange(query: DirectoryGroupQuery): Boolean {
        groupChangeNotifications.add(query)
        return notifyChangeResult
    }

    override suspend fun requestRefresh(): Boolean {
        refreshRequestCount++
        return requestRefreshResult
    }
}
