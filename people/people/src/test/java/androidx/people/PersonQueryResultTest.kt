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

import androidx.people.internal.failedPersonQueryResultOf
import androidx.people.internal.personQueryResultOf
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PersonQueryResultTest {

    private val phone = Criterion.Phone(PHONE)
    private val otherPhone = Criterion.Phone(OTHER_PHONE)
    private val email = Criterion.Email(EMAIL)
    private val lookupKey = Criterion.PersonLookupKey("user_1")

    private val voipAlice = person("user_1", VOIP, "Alice")
    private val voipBob = person("user_2", VOIP, "Bob")
    private val chatAlice = person("user_1", CHAT, "Alice")

    @Test
    fun partition_splitsTheQueryIntoHandledAndUnhandled() {
        val query = personQuery {
            directory(VOIP) {
                addPhones(PHONE, OTHER_PHONE)
                addEmails(EMAIL)
            }
            directory(CHAT) {
                addPhones(PHONE)
                addLookupKeys("user_1")
            }
        }
        val unhandled = personQuery {
            directory(VOIP) { addEmails(EMAIL) }
            directory(CHAT) {
                addPhones(PHONE)
                addLookupKeys("user_1")
            }
        }

        val result =
            personQueryResultOf(
                query,
                matches = setOf(PersonMatch(voipAlice, setOf(phone))),
                unhandledQuery = unhandled,
            )

        assertThat(result.matches).containsExactly(PersonMatch(voipAlice, setOf(phone)))
        assertThat(result.handledQuery)
            .isEqualTo(personQuery(VOIP) { addPhones(PHONE, OTHER_PHONE) })
        assertThat(result.unhandledQuery).isEqualTo(unhandled)
    }

    @Test
    fun partition_allCriteriaHandledWhenUnhandledIsEmpty() {
        val query = personQuery(listOf(VOIP, CHAT)) { addPhones(PHONE) }

        val result =
            personQueryResultOf(
                query,
                matches = setOf(PersonMatch(voipAlice, setOf(phone))),
                unhandledQuery = NO_CRITERIA,
            )

        assertThat(result.handledQuery).isEqualTo(query)
        assertThat(result.unhandledQuery.isEmpty()).isTrue()
    }

    @Test
    fun getResultFor_returnsHandledOrUnhandledScopedToDirectoryAndCriterion() {
        val query = personQuery {
            directory(VOIP) {
                addPhones(PHONE, OTHER_PHONE)
                addEmails(EMAIL)
            }
            directory(CHAT) {
                addPhones(PHONE)
                addEmails(EMAIL)
                addLookupKeys("user_1")
            }
        }
        val unhandled = personQuery(CHAT) { addLookupKeys("user_1") }

        val result =
            personQueryResultOf(
                query,
                matches =
                    setOf(
                        PersonMatch(voipAlice, setOf(phone, email)),
                        PersonMatch(voipBob, setOf(email)),
                        PersonMatch(chatAlice, setOf(phone)),
                    ),
                unhandledQuery = unhandled,
            )

        assertThat(result.getResultFor(VOIP, phone))
            .isEqualTo(MatchResult.Handled(setOf(PersonMatch(voipAlice, setOf(phone, email)))))
        assertThat(result.getResultFor(CHAT, phone))
            .isEqualTo(MatchResult.Handled(setOf(PersonMatch(chatAlice, setOf(phone)))))
        assertThat(result.getResultFor(VOIP, email))
            .isEqualTo(
                MatchResult.Handled(
                    setOf(
                        PersonMatch(voipAlice, setOf(phone, email)),
                        PersonMatch(voipBob, setOf(email)),
                    )
                )
            )
        // Evaluated with no match -> Handled(emptySet()).
        assertThat(result.getResultFor(VOIP, otherPhone))
            .isEqualTo(MatchResult.Handled(emptySet<PersonMatch>()))
        assertThat(result.getResultFor(CHAT, email))
            .isEqualTo(MatchResult.Handled(emptySet<PersonMatch>()))
        // In unhandledQuery -> Unhandled.
        assertThat(result.getResultFor(CHAT, lookupKey)).isEqualTo(MatchResult.Unhandled)
        // Not part of the query at all -> Unhandled.
        assertThat(result.getResultFor(SMS, phone)).isEqualTo(MatchResult.Unhandled)
        assertThat(result.getResultFor(VOIP, lookupKey)).isEqualTo(MatchResult.Unhandled)
    }

    @Test
    fun failedResult_reportsTheWholeQueryAsUnhandled() {
        val query = personQuery(listOf(VOIP, CHAT)) { addPhones(PHONE) }

        val result = failedPersonQueryResultOf(query)

        assertThat(result.matches).isEmpty()
        assertThat(result.handledQuery.isEmpty()).isTrue()
        assertThat(result.unhandledQuery).isEqualTo(query)
        assertThat(result.getResultFor(VOIP, phone)).isEqualTo(MatchResult.Unhandled)
    }

    @Test
    fun equality_isByMatchesAndPartition() {
        val query = personQuery(VOIP) { addPhones(PHONE, OTHER_PHONE) }
        val matches = setOf(PersonMatch(voipAlice, setOf(phone)))
        val a = personQueryResultOf(query, matches, NO_CRITERIA)
        val b = personQueryResultOf(query, matches.toSet(), NO_CRITERIA)

        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
        assertThat(a)
            .isNotEqualTo(
                personQueryResultOf(query, matches, personQuery(VOIP) { addPhones(OTHER_PHONE) })
            )
        assertThat(failedPersonQueryResultOf(query)).isEqualTo(failedPersonQueryResultOf(query))
    }

    private fun person(lookupKey: String, directoryPackage: String, name: String) =
        Person(
            id = PersonId(directoryPackage, lookupKey),
            displayName = name,
            photoUri = null,
            capabilities = emptySet(),
            isKnown = true,
        )

    private companion object {
        const val VOIP = "com.example.voip"
        const val CHAT = "com.example.chat"
        const val SMS = "com.example.sms"
        const val PHONE = "+16505550100"
        const val OTHER_PHONE = "+16505550101"
        const val EMAIL = "alice@example.com"
        val NO_CRITERIA = personQueryOf(emptyMap())
    }
}
