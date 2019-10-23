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

package androidx.lifecycle.truth

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout

/**
 * A Truth Subject for making assertions about [LiveData].
 */
class LiveDataSubject private constructor(
    metadata: FailureMetadata,
    private val actual: LiveData<*>
) : Subject(metadata, actual) {

    /**
     * Assertion that the [LiveData] has active observers.
     */
    fun hasActiveObservers() {
        check("activeObservers").that(actual.hasActiveObservers()).isTrue()
    }

    /**
     * Assertion that the [LiveData] has no active observers.
     */
    fun hasNoActiveObservers() {
        check("activeObservers").that(actual.hasActiveObservers()).isFalse()
    }

    companion object {
        @SuppressLint("MemberVisibilityCanBePrivate")
        val factory = Factory<LiveDataSubject, LiveData<*>> {
            metadata, actual -> LiveDataSubject(metadata, actual) }

        @JvmStatic
        fun assertThat(actual: LiveData<*>): LiveDataSubject {
            return assertAbout(factory).that(actual)
        }
    }
}
