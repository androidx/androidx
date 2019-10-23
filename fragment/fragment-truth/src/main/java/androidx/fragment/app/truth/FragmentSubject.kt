/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.fragment.app.truth

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

/**
 * A Truth Subject for making assertions about [Fragment].
 */
class FragmentSubject private constructor(
    metadata: FailureMetadata,
    private val actual: Fragment
) : Subject(metadata, actual) {

    /**
     * Assertion that the [Fragment] is currently added to an activity.
     */
    fun isAdded() {
        check("isAdded").that(actual.isAdded).isTrue()
    }

    /**
     * Assertion that the [Fragment] is not currently added to an activity.
     */
    fun isNotAdded() {
        check("isAdded").that(actual.isAdded).isFalse()
    }

    companion object {
        @SuppressLint("MemberVisibilityCanBePrivate")
        val factory = Factory<FragmentSubject, Fragment> {
                metadata, actual -> FragmentSubject(metadata, actual) }

        @JvmStatic
        fun assertThat(actual: Fragment): FragmentSubject {
            return assertAbout(factory).that(actual)
        }
    }
}
