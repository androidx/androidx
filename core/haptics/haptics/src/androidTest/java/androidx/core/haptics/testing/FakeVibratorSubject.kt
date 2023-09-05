/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.haptics.testing

import androidx.core.haptics.VibrationWrapper
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Ordered
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout

/**
 * Truth extension for [FakeVibrator].
 */
internal class FakeVibratorSubject private constructor(
    metadata: FailureMetadata?,
    private val actual: FakeVibrator,
) : Subject(metadata, actual) {

    companion object {
        private val SUBJECT_FACTORY: Factory<FakeVibratorSubject?, FakeVibrator> =
            Factory { failureMetadata, subject -> FakeVibratorSubject(failureMetadata, subject) }

        internal fun assertThat(vibrator: FakeVibrator): FakeVibratorSubject =
            requireNotNull(assertAbout(SUBJECT_FACTORY).that(vibrator))
    }

    /**
     * Checks the subject was requested to vibrate with exactly the provided parameters.
     *
     * To also test that the requests appear in the given order, make a call to inOrder() on the
     * object returned by this method.
     */
    fun vibratedExactly(vararg expected: VibrationWrapper): Ordered =
        check("vibrations()").that(actual.vibrations()).containsExactly(*expected)

    /**
     * Checks the subject has never requested to vibrate.
     */
    fun neverVibrated(): Unit =
        check("vibrations()").that(actual.vibrations()).isEmpty()
}
