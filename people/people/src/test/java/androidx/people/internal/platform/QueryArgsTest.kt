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
import androidx.core.os.BundleCompat
import androidx.people.Criterion
import androidx.people.groupQuery
import androidx.people.personQuery
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class QueryArgsTest {

    @Test
    fun personQuery_roundTrips() {
        val query =
            personQuery("com.example.voip") {
                addPhones("+16505550100")
                addEmails("alice@example.com")
            }

        assertThat(query.toQueryArgs().toPersonQuery()).isEqualTo(query)
    }

    @Test
    fun personQueries_roundTripPerDirectory() {
        val queries =
            listOf(
                personQuery("com.example.voip") { addPhones("+1") },
                personQuery("com.example.chat") {
                    addLookupKeys("p1")
                    addGroupLookupKeys("g1")
                },
            )

        assertThat(queries.toQueryArgs().toPersonQueries()).isEqualTo(queries)
    }

    @Test
    fun groupQuery_roundTrips() {
        val query = groupQuery("com.example.chat") { addLookupKeys("g1", "g2") }

        assertThat(query.toQueryArgs().toGroupQuery()).isEqualTo(query)
    }

    @Test
    fun toPersonQueries_nullBundle_returnsEmpty() {
        assertThat(null.toPersonQueries()).isEmpty()
        assertThat(null.toGroupQueries()).isEmpty()
    }

    @Test
    fun requireInteractionTarget_singleCriterion_returnsIt() {
        val query = personQuery("com.example.voip") { addPhones("+1") }

        assertThat(query.requireInteractionTarget()).isEqualTo(Criterion.Phone("+1"))
    }

    @Test
    fun requireInteractionTarget_multipleCriteria_throws() {
        val query =
            personQuery("com.example.voip") {
                addPhones("+1")
                addEmails("a@example.com")
            }

        assertThrows(IllegalArgumentException::class.java) { query.requireInteractionTarget() }
    }

    @Test
    fun toInteractionQueryArgs_carriesTargetAndDependencyUri() {
        val query = personQuery("com.example.voip") { addLookupKeys("p1") }

        val args = query.toInteractionQueryArgs()

        assertThat(args.toPersonQuery()).isEqualTo(query)
        assertThat(
                BundleCompat.getParcelable(
                    args,
                    PeopleContractCompat.QUERY_ARG_DEPENDENCY_URI,
                    Uri::class.java,
                )
            )
            .isEqualTo(PeopleContractCompat.Persons.CONTENT_URI)
    }
}
