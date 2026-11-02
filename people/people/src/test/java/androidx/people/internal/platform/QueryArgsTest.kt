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

import android.net.Uri
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.people.Capability
import androidx.people.Criterion
import androidx.people.GroupQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryGroupQuery
import androidx.people.PeopleDirectoryContentProvider.DirectoryPersonQuery
import androidx.people.PersonId
import androidx.people.PersonQuery
import androidx.people.groupQuery
import androidx.people.peopleInteraction
import androidx.people.personQuery
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class QueryArgsTest {

    @Test
    fun personQueryArgs_roundTripEveryCriterionType() {
        val query =
            personQuery(VOIP) {
                addPhones("+16505550100")
                addEmails("alice@example.com")
                addLookupKeys("p1")
                addGroupLookupKeys("g1")
            }

        val parsed = queriesFor(query).toPersonQueryArgs().personCriteriaByPackage()

        assertThat(parsed)
            .containsExactly(
                VOIP,
                setOf(
                    Criterion.Phone("+16505550100"),
                    Criterion.Email("alice@example.com"),
                    Criterion.PersonLookupKey("p1"),
                    Criterion.GroupLookupKey("g1"),
                ),
            )
    }

    @Test
    fun personQueryArgs_roundTripPerDirectory() {
        val query = personQuery {
            directory(VOIP) { addPhones("+1") }
            directory(CHAT) {
                addLookupKeys("p1")
                addGroupLookupKeys("g1")
            }
        }

        val parsed = queriesFor(query).toPersonQueryArgs().personCriteriaByPackage()

        assertThat(parsed)
            .containsExactly(
                VOIP,
                setOf(Criterion.Phone("+1")),
                CHAT,
                setOf(Criterion.PersonLookupKey("p1"), Criterion.GroupLookupKey("g1")),
            )
    }

    @Test
    fun groupQueryArgs_roundTrip() {
        val query = groupQuery(CHAT) { addLookupKeys("g1", "g2") }

        val parsed = queriesFor(query).toGroupQueryArgs().groupCriteriaByPackage()

        assertThat(parsed)
            .containsExactly(
                CHAT,
                setOf(Criterion.GroupLookupKey("g1"), Criterion.GroupLookupKey("g2")),
            )
    }

    @Test
    fun personQueryArgsOf_matchesTheQueryBasedSerialization() {
        val query = personQuery(VOIP) { addPhones("+1") }

        assertThat(
                personQueryArgsOf(mapOf(VOIP to setOf(Criterion.Phone("+1"))))
                    .personCriteriaByPackage()
            )
            .isEqualTo(queriesFor(query).toPersonQueryArgs().personCriteriaByPackage())
    }

    @Test
    fun groupCriteriaByPackage_ignoresPersonOnlySelectionTypes() {
        val args = queriesFor(personQuery(CHAT) { addPhones("+1") }).toPersonQueryArgs()

        assertThat(args.groupCriteriaByPackage()).isEmpty()
    }

    @Test
    fun criteriaByPackage_nullBundle_returnsEmpty() {
        assertThat(null.personCriteriaByPackage()).isEmpty()
        assertThat(null.groupCriteriaByPackage()).isEmpty()
    }

    @Test
    fun criteriaByPackage_ignoresSubQueriesWithoutPackage() {
        val args =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, VOIP)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("+1"),
                            )
                        },
                        Bundle().apply {
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("+2"),
                            )
                        },
                    ),
                )
            }

        assertThat(args.personCriteriaByPackage())
            .containsExactly(VOIP, setOf(Criterion.Phone("+1")))
    }

    @Test
    fun personCriteriaFor_returnsOnlyTheDirectoryCriteria() {
        val args =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, VOIP)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("+1"),
                            )
                        },
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, CHAT)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_EMAIL,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("a@example.com"),
                            )
                        },
                    ),
                )
            }

        assertThat(args.personCriteriaFor(VOIP)).containsExactly(Criterion.Phone("+1"))
        assertThat(args.personCriteriaFor(CHAT)).containsExactly(Criterion.Email("a@example.com"))
    }

    @Test
    fun personCriteriaFor_byDefault_includesSubQueriesWithoutPackage() {
        val args =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, VOIP)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_LOOKUP_KEY,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("p1"),
                            )
                        },
                        Bundle().apply {
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("+1"),
                            )
                        },
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, CHAT)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_EMAIL,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("a@example.com"),
                            )
                        },
                    ),
                )
            }

        assertThat(args.personCriteriaFor(VOIP))
            .containsExactly(Criterion.PersonLookupKey("p1"), Criterion.Phone("+1"))
    }

    @Test
    fun personCriteriaFor_nullImplicitPackage_ignoresSubQueriesWithoutPackage() {
        val args =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, VOIP)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_LOOKUP_KEY,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("p1"),
                            )
                        },
                        Bundle().apply {
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("+1"),
                            )
                        },
                    ),
                )
            }

        assertThat(args.personCriteriaFor(VOIP, implicitPackage = null))
            .containsExactly(Criterion.PersonLookupKey("p1"))
    }

    @Test
    fun personCriteriaFor_otherImplicitPackage_addressesSubQueriesWithoutPackageToIt() {
        val args =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, VOIP)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_LOOKUP_KEY,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("p1"),
                            )
                        },
                        Bundle().apply {
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("+1"),
                            )
                        },
                    ),
                )
            }

        assertThat(args.personCriteriaFor(VOIP, implicitPackage = CHAT))
            .containsExactly(Criterion.PersonLookupKey("p1"))
        assertThat(args.personCriteriaFor(CHAT, implicitPackage = CHAT))
            .containsExactly(Criterion.Phone("+1"))
    }

    @Test
    fun personCriteriaFor_noCriteriaForTheDirectory_returnsNull() {
        val chatPhoneArgs =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, CHAT)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("+1"),
                            )
                        }
                    ),
                )
            }
        val unpackagedPhoneArgs =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("+1"),
                            )
                        }
                    ),
                )
            }

        assertThat(chatPhoneArgs.personCriteriaFor(VOIP)).isNull()
        assertThat(unpackagedPhoneArgs.personCriteriaFor(VOIP, implicitPackage = null)).isNull()
        assertThat(null.personCriteriaFor(VOIP)).isNull()
    }

    @Test
    fun groupCriteriaFor_byDefault_includesSubQueriesWithoutPackage() {
        val args =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, CHAT)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Groups.Query.SELECTION_TYPE_LOOKUP_KEY,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("g1"),
                            )
                        },
                        Bundle().apply {
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Groups.Query.SELECTION_TYPE_LOOKUP_KEY,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("g2"),
                            )
                        },
                    ),
                )
            }

        assertThat(args.groupCriteriaFor(CHAT))
            .containsExactly(Criterion.GroupLookupKey("g1"), Criterion.GroupLookupKey("g2"))
    }

    @Test
    fun groupCriteriaFor_nullImplicitPackage_ignoresSubQueriesWithoutPackage() {
        val args =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, CHAT)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Groups.Query.SELECTION_TYPE_LOOKUP_KEY,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("g1"),
                            )
                        },
                        Bundle().apply {
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Groups.Query.SELECTION_TYPE_LOOKUP_KEY,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("g2"),
                            )
                        },
                    ),
                )
            }

        assertThat(args.groupCriteriaFor(CHAT, implicitPackage = null))
            .containsExactly(Criterion.GroupLookupKey("g1"))
    }

    @Test
    fun groupCriteriaFor_noCriteriaForTheDirectory_returnsNull() {
        val voipGroupArgs =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(PeopleContractCompat.QUERY_ARG_PACKAGE, VOIP)
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Groups.Query.SELECTION_TYPE_LOOKUP_KEY,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("g1"),
                            )
                        }
                    ),
                )
            }
        val unpackagedPhoneArgs =
            Bundle().apply {
                putParcelableArrayList(
                    PeopleContractCompat.QUERY_ARG_QUERIES,
                    arrayListOf(
                        Bundle().apply {
                            putString(
                                PeopleContractCompat.QUERY_ARG_SELECTION_TYPE,
                                PeopleContractCompat.Persons.Query.SELECTION_TYPE_PHONE,
                            )
                            putStringArray(
                                PeopleContractCompat.QUERY_ARG_SELECTION_VALUE,
                                arrayOf("+1"),
                            )
                        }
                    ),
                )
            }

        assertThat(voipGroupArgs.groupCriteriaFor(CHAT)).isNull()
        assertThat(unpackagedPhoneArgs.groupCriteriaFor(CHAT)).isNull()
        assertThat(null.groupCriteriaFor(CHAT)).isNull()
    }

    @Test
    fun toInteractionQueryArgs_carriesTargetAndDependencyUri() {
        val interaction = peopleInteraction(PersonId("p1", VOIP), Capability.VOICE)

        val args = interaction.toInteractionQueryArgs()

        assertThat(args.personCriteriaByPackage())
            .containsExactly(VOIP, setOf(Criterion.PersonLookupKey("p1")))
        assertThat(
                BundleCompat.getParcelable(
                    args,
                    PeopleContractCompat.QUERY_ARG_DEPENDENCY_URI,
                    Uri::class.java,
                )
            )
            .isEqualTo(PeopleContractCompat.Persons.CONTENT_URI)
    }

    /** The per-directory queries a client's [query] sends, built with the public DSL. */
    private fun queriesFor(query: PersonQuery): Map<String, DirectoryPersonQuery> =
        query.directoryQueries

    private fun queriesFor(query: GroupQuery): Map<String, DirectoryGroupQuery> =
        query.directoryQueries

    private companion object {
        const val VOIP = "com.example.voip"
        const val CHAT = "com.example.chat"
    }
}
