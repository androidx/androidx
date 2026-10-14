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

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class PersonTest {

    @Test
    fun properties_whenProvided_exposeGivenValues() {
        val photoUri = Uri.parse("https://example.com/avatar.jpg")

        val person =
            Person(
                id = PersonId("key_123", "com.example.voip"),
                displayName = "Alice Smith",
                photoUri = photoUri,
                capabilities = setOf(Capability.VOICE, Capability.VIDEO),
                isKnown = true,
                matchedCriteria =
                    setOf(Criterion.Phone("+16505550100"), Criterion.Email("alice@example.com")),
            )

        assertThat(person.displayName).isEqualTo("Alice Smith")
        assertThat(person.photoUri).isEqualTo(photoUri)
        assertThat(person.capabilities).containsExactly(Capability.VOICE, Capability.VIDEO)
        assertThat(person.isKnown).isTrue()
        assertThat(person.matchedCriteria)
            .containsExactly(Criterion.Phone("+16505550100"), Criterion.Email("alice@example.com"))
    }

    @Test
    fun properties_whenOptionalPropertiesEmpty_exposeNullOrEmpty() {
        val person =
            Person(
                id = PersonId("key_123", "com.example.voip"),
                displayName = null,
                photoUri = null,
                capabilities = emptySet(),
                isKnown = false,
                matchedCriteria = emptySet(),
            )

        assertThat(person.displayName).isNull()
        assertThat(person.photoUri).isNull()
        assertThat(person.capabilities).isEmpty()
        assertThat(person.isKnown).isFalse()
        assertThat(person.matchedCriteria).isEmpty()
    }

    @Test
    fun capabilityConvenienceProperties_reflectUnderlyingCapabilities() {
        val person =
            Person(
                id = PersonId("key_123", "com.example.voip"),
                displayName = null,
                photoUri = null,
                capabilities = setOf(Capability.VOICE, Capability.VIDEO),
                isKnown = false,
                matchedCriteria = emptySet(),
            )

        assertThat(person.isVoiceSupported).isTrue()
        assertThat(person.isVideoSupported).isTrue()
        assertThat(person.isMessageSupported).isFalse()
    }

    @Test
    fun equals_comparesMatchedCriteria() {
        val id = PersonId("key_123", "com.example.voip")
        fun person(criteria: Set<Criterion>) =
            Person(
                id = id,
                displayName = null,
                photoUri = null,
                capabilities = emptySet(),
                isKnown = false,
                matchedCriteria = criteria,
            )

        assertThat(person(setOf(Criterion.Phone("1"))))
            .isEqualTo(person(setOf(Criterion.Phone("1"))))
        assertThat(person(setOf(Criterion.Phone("1"))).hashCode())
            .isEqualTo(person(setOf(Criterion.Phone("1"))).hashCode())
        assertThat(person(setOf(Criterion.Phone("1")))).isNotEqualTo(person(emptySet()))
    }
}
