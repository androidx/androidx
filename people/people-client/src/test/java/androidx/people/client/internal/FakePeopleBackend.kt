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
import androidx.people.Group
import androidx.people.GroupQuery
import androidx.people.PeopleInteraction
import androidx.people.Person
import androidx.people.PersonQuery
import androidx.people.internal.PeopleBackend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Backend that records what it was asked for and replays canned results. */
class FakePeopleBackend(
    override val isAvailable: Boolean = true,
    private val directories: List<DirectoryInfo> = emptyList(),
    private val persons: List<Person> = emptyList(),
    private val groups: List<Group> = emptyList(),
    private val interactionRequest: PendingIntent? = null,
    var notifyChangeResult: Boolean = true,
    var requestRefreshResult: Boolean = true,
) : PeopleBackend {

    val personQueries = mutableListOf<List<PersonQuery>>()
    val groupQueries = mutableListOf<List<GroupQuery>>()
    val interactions = mutableListOf<PeopleInteraction>()
    val personChangeNotifications = mutableListOf<PersonQuery>()
    val groupChangeNotifications = mutableListOf<GroupQuery>()
    var refreshRequestCount = 0
        private set

    override suspend fun queryDirectories(): List<DirectoryInfo> = directories

    override fun observeDirectories(): Flow<List<DirectoryInfo>> = flowOf(directories)

    override suspend fun queryPersons(queries: List<PersonQuery>): List<Person> {
        personQueries.add(queries)
        return persons
    }

    override fun observePersons(queries: List<PersonQuery>): Flow<List<Person>> {
        personQueries.add(queries)
        return flowOf(persons)
    }

    override suspend fun queryGroups(queries: List<GroupQuery>): List<Group> {
        groupQueries.add(queries)
        return groups
    }

    override fun observeGroups(queries: List<GroupQuery>): Flow<List<Group>> {
        groupQueries.add(queries)
        return flowOf(groups)
    }

    override suspend fun createInteractionRequest(interaction: PeopleInteraction): PendingIntent? {
        interactions.add(interaction)
        return interactionRequest
    }

    override suspend fun notifyChange(query: PersonQuery): Boolean {
        personChangeNotifications.add(query)
        return notifyChangeResult
    }

    override suspend fun notifyChange(query: GroupQuery): Boolean {
        groupChangeNotifications.add(query)
        return notifyChangeResult
    }

    override suspend fun requestRefresh(): Boolean {
        refreshRequestCount++
        return requestRefreshResult
    }
}
