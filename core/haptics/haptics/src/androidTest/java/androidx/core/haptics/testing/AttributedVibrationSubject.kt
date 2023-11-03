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

import androidx.core.haptics.AttributesWrapper
import androidx.core.haptics.AudioAttributesWrapper
import androidx.core.haptics.PatternVibrationWrapper
import androidx.core.haptics.VibrationAttributesWrapper
import androidx.core.haptics.VibrationEffectWrapper
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout

/**
 * Truth extension for a single [AttributedVibration].
 */
internal class AttributedVibrationSubject private constructor(
    metadata: FailureMetadata?,
    private val actual: AttributedVibration,
) : Subject(metadata, actual) {

    companion object {
        private val SUBJECT_FACTORY: Factory<AttributedVibrationSubject?, AttributedVibration> =
            Factory { failureMetadata, subject ->
                AttributedVibrationSubject(
                    failureMetadata,
                    subject
                )
            }

        internal fun assertThat(vibration: AttributedVibration): AttributedVibrationSubject =
            requireNotNull(assertAbout(SUBJECT_FACTORY).that(vibration))
    }

    /** Checks the vibration was requested with a [PatternVibrationWrapper] or fails. */
    fun hasPatternVibration(): Unit =
        check("vibration()").that(actual.vibration)
            .isInstanceOf(PatternVibrationWrapper::class.java)

    /** Returns a [Subject] for the requested [android.os.VibrationEffect]. */
    fun hasVibrationEffectThat(): Subject {
        check("vibration()").that(actual.vibration is VibrationEffectWrapper)
        val vibrationEffect = (actual.vibration as VibrationEffectWrapper).vibrationEffect
        return check("vibration()").that(vibrationEffect)
    }

    /** Returns a [Subject] for the requested [android.media.AudioAttributes] or fails. */
    fun hasAudioAttributesThat(): Subject {
        check("attributes()").that(actual.attrs is AudioAttributesWrapper)
        val audioAttributes = (actual.attrs as AudioAttributesWrapper).audioAttributes
        return check("attributes()").that(audioAttributes)
    }

    /** Returns a [Subject] for the requested [android.os.VibrationAttributes] or fails. */
    fun hasVibrationAttributesThat(): Subject {
        check("attributes()").that(actual.attrs is VibrationAttributesWrapper)
        val vibrationAttributes = (actual.attrs as VibrationAttributesWrapper).vibrationAttributes
        return check("attributes()").that(vibrationAttributes)
    }

    /** Checks the vibration was requested without a [AttributesWrapper] or fails. */
    fun hasNoAttributes(): Unit = check("attributes()").that(actual.attrs).isNull()
}
