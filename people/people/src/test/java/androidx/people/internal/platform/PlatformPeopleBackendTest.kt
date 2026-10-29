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
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.people.Capability
import androidx.people.Criterion
import androidx.people.Group
import androidx.people.GroupId
import androidx.people.GroupMatch
import androidx.people.PersonId
import androidx.people.groupQuery
import androidx.people.groupQueryOf
import androidx.people.internal.directoryGroupQuery
import androidx.people.internal.directoryPersonQuery
import androidx.people.peopleInteraction
import androidx.people.personQuery
import androidx.people.personQueryOf
import androidx.people.testing.FakePlatformPeopleProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PlatformPeopleBackendTest {

    private val context = RuntimeEnvironment.getApplication()
    private val backend = PlatformPeopleBackend(context)

    @Test
    fun isAvailable_isTrue() {
        assertThat(backend.isAvailable).isTrue()
    }

    private fun setupPlatformPeopleProvider(
        block: FakePlatformPeopleProvider.() -> Unit = {}
    ): FakePlatformPeopleProvider = FakePlatformPeopleProvider.install(context, block)

    @Test
    fun createIfSupported_withoutThePlatformProvider_returnsNull() = runTest {
        val result =
            PlatformPeopleBackend.createIfSupported(context, StandardTestDispatcher(testScheduler))

        assertThat(result).isNull()
    }

    @Test
    fun createIfSupported_belowAndroid37_2_returnsNullEvenWithThePlatformProviderInstalled() =
        runTest {
            setupPlatformPeopleProvider()

            val result =
                PlatformPeopleBackend.createIfSupported(
                    context,
                    StandardTestDispatcher(testScheduler),
                )

            assertThat(result).isNull()
        }

    @Test
    fun createInteractionRequest_withoutThePlatformProvider_returnsNull() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val interaction =
            peopleInteraction(PersonId("user_123", "com.example.voip"), Capability.VOICE)

        val result = backend.createInteractionRequest(interaction)

        assertThat(result).isNull()
    }

    @Test
    fun createInteractionRequest_withMultipleCriteria_sendsEveryCriterion() = runTest {
        val provider = setupPlatformPeopleProvider {
            interactionPendingIntent =
                PendingIntent.getActivity(context, 0, Intent(), PendingIntent.FLAG_IMMUTABLE)
        }
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val interaction =
            peopleInteraction(VOIP, Capability.VIDEO) {
                addLookupKeys("user_123")
                addPhones("+16505550100")
                addEmails("alice@example.com")
            }

        val result = backend.createInteractionRequest(interaction)

        assertThat(result).isEqualTo(provider.interactionPendingIntent)
        val extras = provider.lastCallExtras!!
        assertThat(extras.getString(PeopleContractCompat.Intents.EXTRA_PEOPLE_INTERACTION_TYPE))
            .isEqualTo(PeopleContractCompat.Intents.PEOPLE_INTERACTION_TYPE_VIDEO)
        val queryArgs =
            BundleCompat.getParcelable(
                extras,
                PeopleContractCompat.Intents.EXTRA_PEOPLE_QUERY_ARGS,
                Bundle::class.java,
            )
        assertThat(queryArgs.personCriteriaByPackage())
            .containsExactly(VOIP, interaction.targetCriteria)
    }

    @Test
    fun notifyChange_personQuery_withoutThePlatformProvider_returnsFalse() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = directoryPersonQuery { addPhones("+16505550100") }

        val result = backend.notifyChange(query)

        assertThat(result).isFalse()
    }

    @Test
    fun notifyChange_groupQuery_withoutThePlatformProvider_returnsFalse() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = directoryGroupQuery { addLookupKeys("g1") }

        val result = backend.notifyChange(query)

        assertThat(result).isFalse()
    }

    @Test
    fun requestRefresh_withoutThePlatformProvider_returnsFalse() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))

        val result = backend.requestRefresh()

        assertThat(result).isFalse()
    }

    @Test
    fun notifyChange_personQuery_sendsTheQueryForThisPackageAndItsDependencyUri() = runTest {
        val provider = setupPlatformPeopleProvider()
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = directoryPersonQuery { addPhones("+16505550100") }

        val result = backend.notifyChange(query)

        assertThat(result).isTrue()
        val extras = provider.lastRefreshExtras!!
        assertThat(
                BundleCompat.getParcelable(
                    extras,
                    PeopleContractCompat.QUERY_ARG_DEPENDENCY_URI,
                    Uri::class.java,
                )
            )
            .isEqualTo(PeopleContractCompat.Persons.CONTENT_URI)
        assertThat(extras.personCriteriaByPackage())
            .containsExactly(context.packageName, query.criteria)
    }

    @Test
    fun notifyChange_groupQuery_sendsTheQueryForThisPackageAndItsDependencyUri() = runTest {
        val provider = setupPlatformPeopleProvider()
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = directoryGroupQuery { addLookupKeys("g1") }

        val result = backend.notifyChange(query)

        assertThat(result).isTrue()
        val extras = provider.lastRefreshExtras!!
        assertThat(
                BundleCompat.getParcelable(
                    extras,
                    PeopleContractCompat.QUERY_ARG_DEPENDENCY_URI,
                    Uri::class.java,
                )
            )
            .isEqualTo(PeopleContractCompat.Groups.CONTENT_URI)
        assertThat(extras.groupCriteriaByPackage())
            .containsExactly(context.packageName, query.criteria)
    }

    @Test
    fun requestRefresh_sendsNoQuery() = runTest {
        val provider = setupPlatformPeopleProvider()
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))

        val result = backend.requestRefresh()

        assertThat(result).isTrue()
        assertThat(provider.lastRefreshExtras).isNull()
    }

    @Test
    fun queryPersons_withoutThePlatformProvider_reportsEveryCriterionAsUnhandled() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = personQuery {
            directory(VOIP) { addPhones(PHONE.number) }
            directory(CHAT) {
                addEmails(EMAIL.address)
                addLookupKeys("p1")
            }
        }

        val result = backend.queryPersons(query)

        assertThat(result.matches).isEmpty()
        assertThat(result.unhandledQuery).isEqualTo(query)
    }

    @Test
    fun observePersons_withoutThePlatformProvider_reportsEveryCriterionAsUnhandled() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = personQuery(VOIP) { addPhones(PHONE.number) }

        val result = backend.observePersons(query).first()

        assertThat(result.unhandledQuery).isEqualTo(query)
    }

    @Test
    fun queryGroups_withoutThePlatformProvider_reportsEveryCriterionAsUnhandled() = runTest {
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = groupQuery {
            directory(CHAT) { addLookupKeys("g1", "g2") }
            directory(VOIP) { addLookupKeys("v1") }
        }

        val result = backend.queryGroups(query)

        assertThat(result.matches).isEmpty()
        assertThat(result.unhandledQuery).isEqualTo(query)
    }

    @Test
    fun queryPersons_emptyQuery_returnsAnEmptyResultWithoutQueryingTheProvider() = runTest {
        val provider = setupPlatformPeopleProvider {
            addPerson(id = PersonId("p1", VOIP), criteria = setOf(PHONE))
        }
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        // The DSL rejects empty queries, but a result's empty partition can be queried again.
        val empty = personQueryOf(emptyMap())

        val result = backend.queryPersons(empty)

        assertThat(provider.queryCount).isEqualTo(0)
        assertThat(result.matches).isEmpty()
        assertThat(result.unhandledQuery.isEmpty()).isTrue()
        assertThat(result.unmatchedQuery.isEmpty()).isTrue()
    }

    @Test
    fun queryGroups_emptyQuery_returnsAnEmptyResultWithoutQueryingTheProvider() = runTest {
        val provider = setupPlatformPeopleProvider()
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))

        val result = backend.queryGroups(groupQueryOf(emptyMap()))

        assertThat(provider.queryCount).isEqualTo(0)
        assertThat(result.matches).isEmpty()
        assertThat(result.unhandledQuery.isEmpty()).isTrue()
    }

    @Test
    fun queryPersons_sendsEveryDirectoryQuery() = runTest {
        val provider = setupPlatformPeopleProvider()
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = personQuery {
            directory(VOIP) { addPhones("+16505550100") }
            directory(CHAT) { addEmails("alice@example.com") }
        }

        backend.queryPersons(query)

        assertThat(provider.lastQueryArgs.personCriteriaByPackage())
            .containsExactly(VOIP, setOf(PHONE), CHAT, setOf(EMAIL))
    }

    @Test
    fun queryPersons_samePersonMatchedByTwoInputs_mergesIntoOneMatch() = runTest {
        setupPlatformPeopleProvider {
            addPerson(
                id = PersonId("lookup_1", VOIP),
                displayName = "Alice",
                isKnown = true,
                criteria = setOf(PHONE, EMAIL),
            )
        }
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query =
            personQuery(VOIP) {
                addPhones(PHONE.number)
                addEmails(EMAIL.address)
            }

        val result = backend.queryPersons(query)

        val match = result.matches.single()
        assertThat(match.person.id).isEqualTo(PersonId("lookup_1", VOIP))
        assertThat(match.criteria).containsExactly(PHONE, EMAIL)
        assertThat(result.unhandledQuery.isEmpty()).isTrue()
        assertThat(result.matchedQuery).isEqualTo(query)
    }

    @Test
    fun queryPersons_unhandledCriteriaInCursorExtras_reachTheResult() = runTest {
        setupPlatformPeopleProvider {
            addPerson(id = PersonId("lookup_1", VOIP), criteria = setOf(PHONE))
            unhandledPersonCriteria[VOIP] = setOf(EMAIL)
            unhandledPersonCriteria[CHAT] = setOf(PHONE)
        }
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))
        val query = personQuery {
            directory(VOIP) {
                addPhones(PHONE.number)
                addEmails(EMAIL.address)
            }
            directories(CHAT, SMS) { addPhones(PHONE.number) }
        }

        val result = backend.queryPersons(query)

        assertThat(result.matches.map { it.person.id }).containsExactly(PersonId("lookup_1", VOIP))
        assertThat(result.matchesFor(VOIP, PHONE)).hasSize(1)
        assertThat(result.matchedQuery).isEqualTo(personQuery(VOIP) { addPhones(PHONE.number) })
        // SMS returned no answer, so its criteria were evaluated without a match.
        assertThat(result.unmatchedQuery).isEqualTo(personQuery(SMS) { addPhones(PHONE.number) })
        assertThat(result.unhandledQuery)
            .isEqualTo(
                personQuery {
                    directory(VOIP) { addEmails(EMAIL.address) }
                    directory(CHAT) { addPhones(PHONE.number) }
                }
            )
    }

    @Test
    fun observePersons_emitsUnhandledCriteria() = runTest {
        setupPlatformPeopleProvider { unhandledPersonCriteria[VOIP] = setOf(PHONE) }
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))

        val result = backend.observePersons(personQuery(VOIP) { addPhones(PHONE.number) }).first()

        assertThat(result.unhandledQuery).isEqualTo(personQuery(VOIP) { addPhones(PHONE.number) })
    }

    @Test
    fun queryGroups_returnsMatchesAndUnhandledCriteria() = runTest {
        val g1 = Criterion.GroupLookupKey("g1")
        val g2 = Criterion.GroupLookupKey("g2")
        setupPlatformPeopleProvider {
            addGroup(id = GroupId("g1", CHAT), displayName = "Family", criteria = setOf(g1))
            unhandledGroupCriteria[CHAT] = setOf(g2)
        }
        val backend = PlatformPeopleBackend(context, StandardTestDispatcher(testScheduler))

        val result = backend.queryGroups(groupQuery(CHAT) { addLookupKeys("g1", "g2") })

        assertThat(result.matches)
            .containsExactly(GroupMatch(Group(GroupId("g1", CHAT), "Family"), setOf(g1)))
        assertThat(result.matches.map { it.group })
            .containsExactly(Group(GroupId("g1", CHAT), "Family"))
        assertThat(result.unhandledQuery).isEqualTo(groupQuery(CHAT) { addLookupKeys("g2") })
    }

    /** The query a People Directory application notifies about, built with the public DSL. */
    private companion object {
        const val VOIP = "com.example.voip"
        const val CHAT = "com.example.chat"
        const val SMS = "com.example.sms"
        val PHONE = Criterion.Phone("+16505550100")
        val EMAIL = Criterion.Email("alice@example.com")
    }
}
