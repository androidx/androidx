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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CriterionTest {

    @Test
    fun equals_sameTypeAndValue_isEqual() {
        assertThat(Criterion.Phone("+1")).isEqualTo(Criterion.Phone("+1"))
        assertThat(Criterion.Email("a@b")).isEqualTo(Criterion.Email("a@b"))
        assertThat(Criterion.PersonLookupKey("k")).isEqualTo(Criterion.PersonLookupKey("k"))
        assertThat(Criterion.GroupLookupKey("g")).isEqualTo(Criterion.GroupLookupKey("g"))
    }

    @Test
    fun hashCode_sameTypeAndValue_isSame() {
        assertThat(Criterion.Phone("+1").hashCode()).isEqualTo(Criterion.Phone("+1").hashCode())
        assertThat(Criterion.Email("a@b").hashCode()).isEqualTo(Criterion.Email("a@b").hashCode())
        assertThat(Criterion.PersonLookupKey("k").hashCode())
            .isEqualTo(Criterion.PersonLookupKey("k").hashCode())
        assertThat(Criterion.GroupLookupKey("g").hashCode())
            .isEqualTo(Criterion.GroupLookupKey("g").hashCode())
    }

    @Test
    fun equals_differentValue_isNotEqual() {
        assertThat(Criterion.Phone("+1")).isNotEqualTo(Criterion.Phone("+2"))
    }

    @Test
    fun equals_differentTypeSameValue_isNotEqual() {
        assertThat(Criterion.Phone("x")).isNotEqualTo(Criterion.Email("x"))
        assertThat(Criterion.PersonLookupKey("x")).isNotEqualTo(Criterion.GroupLookupKey("x"))
    }

    @Test
    fun setMembership_usesValueEquality() {
        val criteria = setOf(Criterion.Phone("+1"), Criterion.Email("a@b"))

        assertThat(Criterion.Phone("+1") in criteria).isTrue()
        assertThat(Criterion.Email("+1") in criteria).isFalse()
    }

    @Test
    fun toString_includesTypeAndValue() {
        assertThat(Criterion.Phone("+1").toString()).isEqualTo("Criterion.Phone(number=+1)")
        assertThat(Criterion.Email("a@b").toString()).isEqualTo("Criterion.Email(address=a@b)")
        assertThat(Criterion.PersonLookupKey("k").toString())
            .isEqualTo("Criterion.PersonLookupKey(key=k)")
        assertThat(Criterion.GroupLookupKey("g").toString())
            .isEqualTo("Criterion.GroupLookupKey(key=g)")
    }
}
