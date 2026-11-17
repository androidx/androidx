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

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.people.Capability
import androidx.people.Criterion
import androidx.people.DirectoryInfo
import androidx.people.GroupId
import androidx.people.GroupMatch
import androidx.people.PersonId
import androidx.people.PersonMatch
import androidx.people.PersonQueryResult
import androidx.people.group
import androidx.people.groupQuery
import androidx.people.peopleInteraction
import androidx.people.person
import androidx.people.personQuery
import androidx.people.playservices.FakePeopleProviderCompatClient.Companion.apiException
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.UnsupportedApiCallException
import com.google.android.gms.people.peopleprovider.CreateInteractionPendingIntentRequest
import com.google.android.gms.people.peopleprovider.NotifyDirectoryChangeRequest
import com.google.android.gms.people.peopleprovider.PeopleContractCompat.Capability.TEXT_REACHABILITY
import com.google.android.gms.people.peopleprovider.PeopleContractCompat.Capability.VIDEO_REACHABILITY
import com.google.android.gms.people.peopleprovider.PeopleContractCompat.Capability.VOICE_REACHABILITY
import com.google.android.gms.people.peopleprovider.PeopleContractCompat.Groups
import com.google.android.gms.people.peopleprovider.PeopleContractCompat.Persons
import com.google.android.gms.people.peopleprovider.PeopleProviderDirectory
import com.google.android.gms.people.peopleprovider.PeopleProviderGroup
import com.google.android.gms.people.peopleprovider.PeopleProviderPerson
import com.google.android.gms.people.peopleprovider.PeopleProviderSubQuery
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PlayServicesPeopleBackendTest {

    private val providerClient = FakePeopleProviderCompatClient()
    private val directoryClient = FakePeopleDirectoryCompatClient()
    private val backend = PlayServicesPeopleBackend(providerClient, directoryClient, VOIP)

    @Test
    fun isAvailable_isTrue() {
        assertThat(backend.isAvailable).isTrue()
    }

    // Persons

    @Test
    fun queryPersons_sendsOneSubQueryPerSelectionTypeRestrictedToTheDirectory() = runTest {
        val query =
            personQuery(VOIP) {
                addPhones(PHONE_1, PHONE_2)
                addEmails(EMAIL)
            }

        backend.queryPersons(query)

        assertThat(providerClient.personRequests.single().subQueries)
            .containsExactly(
                subQuery(VOIP, Persons.Query.SELECTION_TYPE_PHONE, PHONE_1, PHONE_2),
                subQuery(VOIP, Persons.Query.SELECTION_TYPE_EMAIL, EMAIL),
            )
            .inOrder()
    }

    @Test
    fun queryPersons_withSeveralDirectories_sendsTheSubQueriesOfEach() = runTest {
        val query = personQuery {
            directory(VOIP) { addLookupKeys("user_1") }
            directory(CHAT) { addGroupLookupKeys("g1") }
        }

        backend.queryPersons(query)

        assertThat(providerClient.personRequests.single().subQueries)
            .containsExactly(
                subQuery(VOIP, Persons.Query.SELECTION_TYPE_LOOKUP_KEY, "user_1"),
                subQuery(CHAT, Persons.Query.SELECTION_TYPE_GROUP_LOOKUP_KEY, "g1"),
            )
            .inOrder()
    }

    @Test
    fun queryPersons_withAnEmptyQuery_returnsAnEmptyResultWithoutCallingGooglePlayServices() =
        runTest {
            val query = personQuery(VOIP) { addPhones(PHONE_1) }.forDirectories(emptyList())

            val result = backend.queryPersons(query)

            assertThat(result.matches).isEmpty()
            assertThat(result.unhandledQuery.isEmpty()).isTrue()
            assertThat(providerClient.personRequests).isEmpty()
        }

    @Test
    fun queryPersons_convertsTheRecord() = runTest {
        providerClient.persons =
            listOf(
                gmsPerson(
                    lookupKey = "user_1",
                    displayName = "Alice",
                    photoUri = Uri.parse("https://example.com/alice.png"),
                    capabilities = VOICE_REACHABILITY or TEXT_REACHABILITY,
                    isKnown = true,
                    matchedType = Persons.Query.SELECTION_TYPE_PHONE,
                    matchedValue = PHONE_1,
                )
            )

        val result = backend.queryPersons(personQuery(VOIP) { addPhones(PHONE_1) })

        assertThat(result.matches)
            .containsExactly(
                PersonMatch(
                    person(PersonId(VOIP, "user_1")) {
                        displayName = "Alice"
                        photoUri = Uri.parse("https://example.com/alice.png")
                        capabilities = setOf(Capability.VOICE, Capability.MESSAGE)
                        isKnown = true
                    },
                    setOf(Criterion.Phone(PHONE_1)),
                )
            )
        assertThat(result.matchedQuery).isEqualTo(personQuery(VOIP) { addPhones(PHONE_1) })
    }

    @Test
    fun queryPersons_mergesRecordsOfTheSamePersonAndKeepsTheirOrder() = runTest {
        providerClient.persons =
            listOf(
                gmsPerson("user_1", matchedType = Persons.Query.SELECTION_TYPE_PHONE, PHONE_1),
                gmsPerson("user_2", matchedType = Persons.Query.SELECTION_TYPE_PHONE, PHONE_2),
                gmsPerson("user_1", matchedType = Persons.Query.SELECTION_TYPE_EMAIL, EMAIL),
            )

        val query =
            personQuery(VOIP) {
                addPhones(PHONE_1, PHONE_2)
                addEmails(EMAIL)
            }

        val matches = backend.queryPersons(query).matches.toList()

        assertThat(matches.map { it.person.id })
            .containsExactly(PersonId(VOIP, "user_1"), PersonId(VOIP, "user_2"))
            .inOrder()
        assertThat(matches[0].criteria)
            .containsExactly(Criterion.Phone(PHONE_1), Criterion.Email(EMAIL))
    }

    @Test
    fun queryPersons_reportsTheCriteriaOfUnreachableDirectoriesAsUnhandled() = runTest {
        providerClient.persons =
            listOf(gmsPerson("user_1", Persons.Query.SELECTION_TYPE_PHONE, PHONE_1))
        providerClient.unreachablePackages = listOf(CHAT)
        val query = personQuery(listOf(VOIP, CHAT)) { addPhones(PHONE_1, PHONE_2) }

        val result = backend.queryPersons(query)

        assertThat(result.matchedQuery).isEqualTo(personQuery(VOIP) { addPhones(PHONE_1) })
        assertThat(result.unmatchedQuery).isEqualTo(personQuery(VOIP) { addPhones(PHONE_2) })
        assertThat(result.unhandledQuery)
            .isEqualTo(personQuery(CHAT) { addPhones(PHONE_1, PHONE_2) })
    }

    @Test
    fun queryPersons_releasesTheBuffer() = runTest {
        providerClient.persons = listOf(gmsPerson("user_1"))

        backend.queryPersons(personQuery(VOIP) { addPhones(PHONE_1) })

        @Suppress("DEPRECATION") assertThat(providerClient.personBuffers.single().isClosed).isTrue()
    }

    @Test
    fun queryPersons_whenGooglePlayServicesFails_throwsIllegalStateExceptionWithTheCause() =
        runTest {
            val failure = apiException(CommonStatusCodes.INTERNAL_ERROR)
            providerClient.failure = failure

            val result = runCatching {
                backend.queryPersons(personQuery(VOIP) { addPhones(PHONE_1) })
            }

            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            assertThat(result.exceptionOrNull()).hasCauseThat().isSameInstanceAs(failure)
        }

    @Test
    fun queryPersons_whenGooglePlayServicesDoesNotSupportTheCall_throwsIllegalStateException() =
        runTest {
            val failure = UnsupportedApiCallException(Feature("people_provider_compat_api", 1L))
            providerClient.failure = failure

            val result = runCatching {
                backend.queryPersons(personQuery(VOIP) { addPhones(PHONE_1) })
            }

            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
            assertThat(result.exceptionOrNull()).hasCauseThat().isSameInstanceAs(failure)
        }

    @Test
    fun queryPersons_whenGooglePlayServicesDeniesAccess_propagatesTheSecurityException() = runTest {
        val failure = SecurityException("READ_CONTACTS not granted")
        providerClient.failure = failure

        val result = runCatching {
            backend.queryPersons(personQuery(VOIP) { addPhones(PHONE_1) })
        }

        assertThat(result.exceptionOrNull()).isSameInstanceAs(failure)
    }

    @OptIn(ExperimentalCoroutinesApi::class) // runCurrent
    @Test
    fun queryPersons_whenCancelledBeforeTheResultArrives_releasesTheBuffer() = runTest {
        providerClient.persons = listOf(gmsPerson("user_1"))
        providerClient.holdCalls = true

        val call = launch { backend.queryPersons(personQuery(VOIP) { addPhones(PHONE_1) }) }
        runCurrent()
        call.cancel()
        runCurrent()
        providerClient.completeHeldCalls()

        @Suppress("DEPRECATION") assertThat(providerClient.personBuffers.single().isClosed).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class) // runCurrent
    @Test
    fun observePersons_emitsTheCurrentResultsThenRequeriesOnEachChange() = runTest {
        providerClient.persons = listOf(gmsPerson("user_1"))
        val query = personQuery(VOIP) { addPhones(PHONE_1) }
        val emissions = mutableListOf<PersonQueryResult>()

        val collection = launch { backend.observePersons(query).take(2).toList(emissions) }
        runCurrent()
        providerClient.persons = listOf(gmsPerson("user_2"))
        providerClient.fireDataChanged()
        collection.join()

        assertThat(emissions.map { result -> result.matches.map { it.person.id.lookupKey } })
            .containsExactly(listOf("user_1"), listOf("user_2"))
            .inOrder()
    }

    @Test
    fun observePersons_registersBeforeTheFirstReadAndUnregistersWhenCollectionStops() = runTest {
        val query = personQuery(VOIP) { addPhones(PHONE_1) }

        backend.observePersons(query).first()

        assertThat(providerClient.stoppedListeners).hasSize(1)
        assertThat(providerClient.listeners).isEmpty()
        assertThat(providerClient.personRequests).hasSize(1)
    }

    @OptIn(ExperimentalCoroutinesApi::class) // runCurrent
    @Test
    fun observePersons_doesNotReemitUnchangedResults() = runTest {
        providerClient.persons = listOf(gmsPerson("user_1"))
        val query = personQuery(VOIP) { addPhones(PHONE_1) }
        val emissions = mutableListOf<PersonQueryResult>()

        val collection = launch { backend.observePersons(query).toList(emissions) }
        runCurrent()
        providerClient.fireDataChanged()
        runCurrent()
        collection.cancel()

        assertThat(emissions).hasSize(1)
        assertThat(providerClient.personRequests).hasSize(2)
    }

    @Test
    fun observePersons_whenRegistrationFails_throws() = runTest {
        val failure = apiException(CommonStatusCodes.INTERNAL_ERROR)
        providerClient.failure = failure
        val query = personQuery(VOIP) { addPhones(PHONE_1) }

        val result = runCatching { backend.observePersons(query).first() }

        // Coroutines stack trace recovery may copy the exception, so look through the cause chain.
        val exception = result.exceptionOrNull()
        assertThat(exception).isInstanceOf(IllegalStateException::class.java)
        assertThat(generateSequence(exception) { it.cause }.toList()).contains(failure)
        assertThat(providerClient.stoppedListeners).hasSize(1)
    }

    @OptIn(ExperimentalCoroutinesApi::class) // runCurrent
    @Test
    fun observePersons_whenCancelledDuringRegistration_unregistersTheListener() = runTest {
        providerClient.holdCalls = true
        val query = personQuery(VOIP) { addPhones(PHONE_1) }

        val collection = launch { backend.observePersons(query).collect {} }
        runCurrent()
        collection.cancel()
        runCurrent()
        providerClient.completeHeldCalls()

        assertThat(providerClient.stoppedListeners).hasSize(1)
        assertThat(providerClient.listeners).isEmpty()
    }

    // Groups

    @Test
    fun queryGroups_sendsTheLookupKeySubQueryAndConvertsTheRecords() = runTest {
        providerClient.groups =
            listOf(
                gmsGroup("g1", "Family", Groups.Query.SELECTION_TYPE_LOOKUP_KEY, "g1"),
                gmsGroup("g1", "Family", Groups.Query.SELECTION_TYPE_LOOKUP_KEY, "g1-alias"),
            )

        val result = backend.queryGroups(groupQuery(CHAT) { addLookupKeys("g1", "g1-alias") })

        assertThat(providerClient.groupRequests.single().subQueries)
            .containsExactly(
                subQuery(CHAT, Groups.Query.SELECTION_TYPE_LOOKUP_KEY, "g1", "g1-alias")
            )
        assertThat(result.matches)
            .containsExactly(
                GroupMatch(
                    group(GroupId(CHAT, "g1")) { displayName = "Family" },
                    setOf(Criterion.GroupLookupKey("g1"), Criterion.GroupLookupKey("g1-alias")),
                )
            )
        @Suppress("DEPRECATION") assertThat(providerClient.groupBuffers.single().isClosed).isTrue()
    }

    @Test
    fun observeGroups_emitsAndUnregistersWhenCollectionStops() = runTest {
        providerClient.groups =
            listOf(gmsGroup("g1", "Family", Groups.Query.SELECTION_TYPE_LOOKUP_KEY, "g1"))

        val result = backend.observeGroups(groupQuery(CHAT) { addLookupKeys("g1") }).first()

        assertThat(result.matches.single().group.id).isEqualTo(GroupId(CHAT, "g1"))
        assertThat(providerClient.stoppedListeners).hasSize(1)
    }

    // Directories

    @Test
    fun queryDirectories_convertsTheDirectories() = runTest {
        providerClient.directories =
            listOf(
                PeopleProviderDirectory.builder()
                    .setPackageName(VOIP)
                    .setAuthority("$VOIP.people")
                    .build()
            )

        val result = backend.queryDirectories()

        assertThat(result).containsExactly(DirectoryInfo(VOIP, "$VOIP.people"))
    }

    @Test
    fun observeDirectories_emitsTheCurrentDirectories() = runTest {
        providerClient.directories =
            listOf(
                PeopleProviderDirectory.builder()
                    .setPackageName(VOIP)
                    .setAuthority("$VOIP.people")
                    .build()
            )

        val result = backend.observeDirectories().first()

        assertThat(result).containsExactly(DirectoryInfo(VOIP, "$VOIP.people"))
    }

    // Interactions

    @Test
    fun createInteractionRequest_withPersonId_sendsTheLookupKeyAndReturnsThePendingIntent() =
        runTest {
            val pendingIntent = pendingIntent()
            providerClient.interactionPendingIntent = pendingIntent

            val result =
                backend.createInteractionRequest(
                    peopleInteraction(PersonId(VOIP, "user_1"), Capability.VIDEO)
                )

            assertThat(result).isSameInstanceAs(pendingIntent)
            val request = providerClient.interactionRequests.single()
            assertThat(request.targetPackage).isEqualTo(VOIP)
            assertThat(request.interactionType)
                .isEqualTo(CreateInteractionPendingIntentRequest.PEOPLE_INTERACTION_TYPE_VIDEO)
            assertThat(request.selectionType).isEqualTo(Persons.Query.SELECTION_TYPE_LOOKUP_KEY)
            assertThat(request.selectionValues).containsExactly("user_1")
        }

    @Test
    fun createInteractionRequest_withPhoneTarget_sendsThePhone() = runTest {
        backend.createInteractionRequest(
            peopleInteraction(VOIP, Capability.VOICE) { addPhones(PHONE_1) }
        )

        val request = providerClient.interactionRequests.single()
        assertThat(request.interactionType)
            .isEqualTo(CreateInteractionPendingIntentRequest.PEOPLE_INTERACTION_TYPE_VOICE)
        assertThat(request.selectionType).isEqualTo(Persons.Query.SELECTION_TYPE_PHONE)
        assertThat(request.selectionValues).containsExactly(PHONE_1)
    }

    @Test
    fun createInteractionRequest_withSeveralValuesOfOneType_sendsThemAll() = runTest {
        backend.createInteractionRequest(
            peopleInteraction(VOIP, Capability.VOICE) { addPhones(PHONE_1, PHONE_2) }
        )

        val request = providerClient.interactionRequests.single()
        assertThat(request.selectionType).isEqualTo(Persons.Query.SELECTION_TYPE_PHONE)
        assertThat(request.selectionValues).containsExactly(PHONE_1, PHONE_2).inOrder()
    }

    @Test
    fun createInteractionRequest_withSeveralTypes_throwsWithoutCallingGooglePlayServices() =
        runTest {
            val interaction =
                peopleInteraction(VOIP, Capability.VOICE) {
                    addPhones(PHONE_1)
                    addEmails(EMAIL)
                }

            val result = runCatching { backend.createInteractionRequest(interaction) }

            assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(providerClient.interactionRequests).isEmpty()
        }

    // People Directory applications

    @Test
    fun personsChangedRequestOf_sendsThePersonsSubQueriesOfTheDirectory() {
        val request = personsChangedRequestOf(VOIP, setOf(Criterion.Phone(PHONE_1)))

        assertThat(request.queryType).isEqualTo(NotifyDirectoryChangeRequest.QUERY_TYPE_PERSONS)
        assertThat(request.modifiedQueries)
            .containsExactly(subQuery(VOIP, Persons.Query.SELECTION_TYPE_PHONE, PHONE_1))
    }

    @Test
    fun groupsChangedRequestOf_sendsTheGroupsSubQueriesOfTheDirectory() {
        val request = groupsChangedRequestOf(VOIP, setOf(Criterion.GroupLookupKey("g1")))

        assertThat(request.queryType).isEqualTo(NotifyDirectoryChangeRequest.QUERY_TYPE_GROUPS)
        assertThat(request.modifiedQueries)
            .containsExactly(subQuery(VOIP, Groups.Query.SELECTION_TYPE_LOOKUP_KEY, "g1"))
    }

    @Test
    fun notifyChange_sendsTheRequestAndReturnsThatGooglePlayServicesAccepted() = runTest {
        val request = personsChangedRequestOf(VOIP, setOf(Criterion.Phone(PHONE_1)))

        val result = backend.notifyChange(request)

        assertThat(result).isTrue()
        assertThat(directoryClient.notifyRequests).containsExactly(request)
    }

    @Test
    fun notifyChange_whenGooglePlayServicesDeclines_returnsFalse() = runTest {
        val backend =
            PlayServicesPeopleBackend(
                providerClient,
                FakePeopleDirectoryCompatClient(notifyAccepted = false),
                VOIP,
            )

        val result =
            backend.notifyChange(personsChangedRequestOf(VOIP, setOf(Criterion.Phone(PHONE_1))))

        assertThat(result).isFalse()
    }

    @Test
    fun requestRefresh_returnsWhetherGooglePlayServicesAccepted() = runTest {
        val result = backend.requestRefresh()

        assertThat(result).isTrue()
        assertThat(directoryClient.refreshRequestCount).isEqualTo(1)
    }

    @Test
    fun notifyChange_whenGooglePlayServicesFails_returnsFalse() = runTest {
        for (failure in DIRECTORY_FAILURES) {
            val backend =
                PlayServicesPeopleBackend(
                    providerClient,
                    FakePeopleDirectoryCompatClient(failure = failure),
                    VOIP,
                )

            val result =
                backend.notifyChange(personsChangedRequestOf(VOIP, setOf(Criterion.Phone(PHONE_1))))

            assertWithMessage("for $failure").that(result).isFalse()
        }
    }

    @Test
    fun requestRefresh_whenGooglePlayServicesFails_returnsFalse() = runTest {
        for (failure in DIRECTORY_FAILURES) {
            val backend =
                PlayServicesPeopleBackend(
                    providerClient,
                    FakePeopleDirectoryCompatClient(failure = failure),
                    VOIP,
                )

            val result = backend.requestRefresh()

            assertWithMessage("for $failure").that(result).isFalse()
        }
    }

    private companion object {
        /**
         * Failures that notifyChange and requestRefresh report as `false`, as
         * PeopleDirectoryContentProvider documents.
         */
        val DIRECTORY_FAILURES =
            listOf(
                apiException(CommonStatusCodes.INTERNAL_ERROR),
                SecurityException("Not a People Directory"),
                UnsupportedApiCallException(Feature("people_provider_compat_api", 1L)),
                UnsupportedOperationException("Disabled"),
            )
        const val VOIP = "com.example.voip"
        const val CHAT = "com.example.chat"
        const val PHONE_1 = "+16505550100"
        const val PHONE_2 = "+16505550101"
        const val EMAIL = "alice@example.com"

        fun subQuery(pkg: String, type: String, vararg values: String): PeopleProviderSubQuery =
            PeopleProviderSubQuery.builder()
                .setTargetPackages(listOf(pkg))
                .setSelectionType(type)
                .setSelectionValues(values.toList())
                .build()

        fun gmsPerson(
            lookupKey: String,
            matchedType: String = Persons.Query.SELECTION_TYPE_PHONE,
            matchedValue: String = PHONE_1,
            displayName: String? = null,
            photoUri: Uri? = null,
            capabilities: Int = VIDEO_REACHABILITY,
            isKnown: Boolean = false,
            packageName: String = VOIP,
        ): PeopleProviderPerson =
            PeopleProviderPerson.builder()
                .setLookupKey(lookupKey)
                .setDisplayName(displayName)
                .setPhotoUri(photoUri)
                .setPackageName(packageName)
                .setCapabilities(capabilities)
                .setIsKnown(isKnown)
                .setMatchedSelectionType(matchedType)
                .setMatchedSelectionValue(matchedValue)
                .build()

        fun gmsGroup(
            lookupKey: String,
            displayName: String? = null,
            matchedType: String = Groups.Query.SELECTION_TYPE_LOOKUP_KEY,
            matchedValue: String = lookupKey,
        ): PeopleProviderGroup =
            PeopleProviderGroup.builder()
                .setLookupKey(lookupKey)
                .setDisplayName(displayName)
                .setPackageName(CHAT)
                .setMatchedSelectionType(matchedType)
                .setMatchedSelectionValue(matchedValue)
                .build()

        fun pendingIntent(): PendingIntent =
            PendingIntent.getActivity(
                RuntimeEnvironment.getApplication(),
                0,
                Intent(),
                PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
