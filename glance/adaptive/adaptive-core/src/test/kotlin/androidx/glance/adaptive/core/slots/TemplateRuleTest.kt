/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.glance.adaptive.core.slots

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TemplateRuleTest {

    private data class SamplePayload(val hasPrimary: Boolean, val hasTracking: Boolean)

    private val rule =
        TemplateRule<SamplePayload>(
            "SampleTemplate",
            Requirement("a primary value") { it.hasPrimary },
            Requirement("a tracking visual") { it.hasTracking },
        )

    @Test
    fun check_passesWhenAllRequirementsAreMet() {
        rule.check(SamplePayload(hasPrimary = true, hasTracking = true))
    }

    @Test
    fun check_listsOnlyUnmetRequirementsWhenSomeAreMet() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                rule.check(SamplePayload(hasPrimary = true, hasTracking = false))
            }
        assertThat(error).hasMessageThat().isEqualTo("SampleTemplate needs a tracking visual.")
    }

    @Test
    fun check_listsAllUnmetRequirementsTogether() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                rule.check(SamplePayload(hasPrimary = false, hasTracking = false))
            }
        assertThat(error)
            .hasMessageThat()
            .isEqualTo("SampleTemplate needs a primary value; a tracking visual.")
    }
}
