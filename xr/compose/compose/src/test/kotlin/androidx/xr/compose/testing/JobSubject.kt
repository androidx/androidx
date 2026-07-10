/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.xr.compose.testing

import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth
import kotlinx.coroutines.Job

/**
 * For testing. A Truth Subject extension for [Job]s.
 *
 * This allows you to do fluent Truth assertions like: `assertThat(job).isCancelled()` and
 * `assertThat(job).isActive()` with helpful error messages for testing.
 */
class JobSubject(metadata: FailureMetadata, private val actual: Job?) : Subject(metadata, actual) {
    fun isCancelled() {
        if (actual?.isCancelled != true) {
            failWithActual(simpleFact("expected to be cancelled"))
        }
    }

    fun isActive() {
        if (actual?.isActive != true) {
            failWithActual(simpleFact("expected to be active"))
        }
    }

    fun isCompleted() {
        if (actual?.isCompleted != true) {
            failWithActual(simpleFact("expected to be completed"))
        }
    }

    companion object {
        private val JOB_SUBJECT_FACTORY: Factory<JobSubject, Job?> =
            Factory<JobSubject, Job?> { metadata, actual -> JobSubject(metadata, actual) }

        @JvmStatic
        fun assertThat(actual: Job?): JobSubject {
            return Truth.assertAbout(JOB_SUBJECT_FACTORY).that(actual)
        }
    }
}
