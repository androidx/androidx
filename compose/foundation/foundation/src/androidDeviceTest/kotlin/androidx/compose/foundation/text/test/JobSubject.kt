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

package androidx.compose.foundation.text.test

import com.google.common.truth.Fact.simpleFact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Truth.assertAbout
import kotlinx.coroutines.Job

internal fun assertThatJob(job: Job?): JobSubject =
    assertAbout(JobSubject.SUBJECT_FACTORY).that(job)!!

internal class JobSubject
private constructor(failureMetadata: FailureMetadata?, private val subject: Job?) :
    Subject(failureMetadata, subject) {
    companion object {
        val SUBJECT_FACTORY: Factory<JobSubject?, Job?> = Factory { failureMetadata, subject ->
            JobSubject(failureMetadata, subject)
        }
    }

    private val nullCheckedSubject: Job
        get() {
            if (subject == null) failWithoutActual(simpleFact("is null"))
            return subject!!
        }

    fun isActive() {
        check("isActive").that(nullCheckedSubject.isActive).isTrue()
    }

    /** Checks that the job is completed regardless of whether it was cancelled. */
    fun isCompleted() {
        check("isCompleted").that(nullCheckedSubject.isCompleted).isTrue()
    }

    /** Checks that the job is completed and not cancelled. */
    fun isCompletedSuccessfully() {
        check("isCompleted").that(nullCheckedSubject.isCompleted).isTrue()
        check("isCancelled").that(nullCheckedSubject.isCancelled).isFalse()
    }

    fun isCancelled() {
        check("isCancelled").that(nullCheckedSubject.isCancelled).isTrue()
    }
}
