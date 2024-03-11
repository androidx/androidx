/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.kruth

import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

/** Tests for {@link ExpectFailure} used as JUnit {@link Rule}. */
class ExpectFailureRuleTest {
    @get:Rule
    val expectFailure: ExpectFailure = ExpectFailure()

    @get:Rule
    @Suppress("DEPRECATION")
    val thrown: ExpectedException = ExpectedException.none()

    @Test
    fun expectFail_captureFailureAsExpected() {
        expectFailure.whenTesting().withMessage("abc").fail()
        assertThat(expectFailure.getFailure()).hasMessageThat().isEqualTo("abc")
    }

    @Test
    fun expectFail_passesIfUnused() {
        assertThat(4).isEqualTo(4)
    }

    @Test
    fun expectFail_failsAfterTest() {
        expectFailure.whenTesting().that(4).isEqualTo(4)
        thrown.expectMessage("ExpectFailure.whenTesting() invoked, but no failure was caught.")
    }

    @Test
    fun expectFail_throwInSubject_shouldPropagateOriginalException() {
        thrown.expectMessage("Throwing deliberately")
        expectFailure.whenTesting().that(throwingMethod()).isEqualTo(2)
    }

    @Test
    fun expectFail_throwAfterSubject_shouldPropagateOriginalException() {
        expectFailure.whenTesting().that(2).isEqualTo(2)
        thrown.expectMessage("Throwing deliberately")
        throwingMethod()
    }
}

private fun throwingMethod() {
    throw RuntimeException("Throwing deliberately")
}
