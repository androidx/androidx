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

import androidx.people.Criterion
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class SelectionsTest {

    @Test
    fun personSelection_roundTripsForEveryCriterionType() {
        val criteria =
            listOf(
                Criterion.Phone("+1"),
                Criterion.Email("a@example.com"),
                Criterion.PersonLookupKey("k"),
                Criterion.GroupLookupKey("g"),
            )

        for (criterion in criteria) {
            assertThat(criterion.toPersonSelection().toPersonCriterion()).isEqualTo(criterion)
        }
    }

    @Test
    fun toPersonCriterion_unknownType_returnsNull() {
        assertThat(Selection("selection-type-future", "x").toPersonCriterion()).isNull()
    }

    @Test
    fun toGroupSelection_onlySupportsGroupLookupKey() {
        assertThat(Criterion.GroupLookupKey("g").toGroupSelection())
            .isEqualTo(Selection(PeopleContractCompat.Groups.Query.SELECTION_TYPE_LOOKUP_KEY, "g"))
        assertThrows(IllegalArgumentException::class.java) {
            Criterion.Phone("+1").toGroupSelection()
        }
        assertThrows(IllegalArgumentException::class.java) {
            Criterion.PersonLookupKey("g").toGroupSelection()
        }
    }

    @Test
    fun groupSelection_roundTripsGroupLookupKey() {
        val criterion = Criterion.GroupLookupKey("g")

        assertThat(criterion.toGroupSelection().toGroupCriterion()).isEqualTo(criterion)
    }
}
