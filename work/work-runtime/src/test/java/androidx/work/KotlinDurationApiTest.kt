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

package androidx.work

import android.content.Context
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the `kotlin.time.Duration` overloads on [WorkRequest.Builder],
 * [PeriodicWorkRequest.Builder] and [Constraints.Builder] produce the same underlying millisecond
 * values as the pre-existing `(Long, TimeUnit)` overloads.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [24])
class KotlinDurationApiTest {

    // --- WorkRequest.Builder -----------------------------------------------------------------

    @Test
    fun setInitialDelay_kotlinDuration() {
        val request =
            OneTimeWorkRequest.Builder(DurationTestWorker::class.java)
                .setInitialDelay(10.minutes)
                .build()
        assertThat(request.workSpec.initialDelay).isEqualTo(TimeUnit.MINUTES.toMillis(10))
    }

    @Test
    fun setInitialDelay_kotlinDuration_matchesTimeUnitOverload() {
        val viaDuration =
            OneTimeWorkRequest.Builder(DurationTestWorker::class.java)
                .setInitialDelay(1500.milliseconds)
                .build()
        val viaTimeUnit =
            OneTimeWorkRequest.Builder(DurationTestWorker::class.java)
                .setInitialDelay(1500, TimeUnit.MILLISECONDS)
                .build()
        assertThat(viaDuration.workSpec.initialDelay).isEqualTo(viaTimeUnit.workSpec.initialDelay)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setInitialDelay_infiniteDuration_throwsIllegalArgumentException() {
        OneTimeWorkRequest.Builder(DurationTestWorker::class.java)
            .setInitialDelay(Duration.INFINITE)
            .build()
    }

    @Test
    fun setInitialDelay_subMillisecondDuration_truncatesToZero() {
        val request =
            OneTimeWorkRequest.Builder(DurationTestWorker::class.java)
                .setInitialDelay(500.microseconds)
                .build()
        assertThat(request.workSpec.initialDelay).isEqualTo(0L)
    }

    @Test
    fun setBackoffCriteria_kotlinDuration() {
        val request =
            OneTimeWorkRequest.Builder(DurationTestWorker::class.java)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 45.seconds)
                .build()
        assertThat(request.workSpec.backoffPolicy).isEqualTo(BackoffPolicy.LINEAR)
        assertThat(request.workSpec.backoffDelayDuration).isEqualTo(TimeUnit.SECONDS.toMillis(45))
    }

    @Test
    fun setBackoffCriteria_kotlinDuration_clampsToMin() {
        val request =
            OneTimeWorkRequest.Builder(DurationTestWorker::class.java)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1.milliseconds)
                .build()
        assertThat(request.workSpec.backoffDelayDuration).isEqualTo(WorkRequest.MIN_BACKOFF_MILLIS)
    }

    @Test
    fun setBackoffCriteria_kotlinDuration_clampsToMax() {
        val request =
            OneTimeWorkRequest.Builder(DurationTestWorker::class.java)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30.days)
                .build()
        assertThat(request.workSpec.backoffDelayDuration).isEqualTo(WorkRequest.MAX_BACKOFF_MILLIS)
    }

    @Test
    fun keepResultsForAtLeast_kotlinDuration() {
        val request =
            OneTimeWorkRequest.Builder(DurationTestWorker::class.java)
                .keepResultsForAtLeast(2.days)
                .build()
        assertThat(request.workSpec.minimumRetentionDuration).isEqualTo(TimeUnit.DAYS.toMillis(2))
    }

    // --- PeriodicWorkRequest.Builder ---------------------------------------------------------

    @Test
    fun periodicBuilder_class_kotlinDuration() {
        val request = PeriodicWorkRequest.Builder(DurationTestWorker::class.java, 1.hours).build()
        assertThat(request.workSpec.intervalDuration).isEqualTo(TimeUnit.HOURS.toMillis(1))
        assertThat(request.workSpec.flexDuration).isEqualTo(TimeUnit.HOURS.toMillis(1))
    }

    @Test
    fun periodicBuilder_kclass_kotlinDuration() {
        val request = PeriodicWorkRequest.Builder(DurationTestWorker::class, 1.hours).build()
        assertThat(request.workSpec.intervalDuration).isEqualTo(TimeUnit.HOURS.toMillis(1))
    }

    @Test
    fun periodicBuilder_class_kotlinDuration_withFlex() {
        val request =
            PeriodicWorkRequest.Builder(DurationTestWorker::class.java, 1.hours, 15.minutes).build()
        assertThat(request.workSpec.intervalDuration).isEqualTo(TimeUnit.HOURS.toMillis(1))
        assertThat(request.workSpec.flexDuration).isEqualTo(TimeUnit.MINUTES.toMillis(15))
    }

    @Test
    fun periodicBuilder_kclass_kotlinDuration_withFlex() {
        val request =
            PeriodicWorkRequest.Builder(DurationTestWorker::class, 1.hours, 15.minutes).build()
        assertThat(request.workSpec.intervalDuration).isEqualTo(TimeUnit.HOURS.toMillis(1))
        assertThat(request.workSpec.flexDuration).isEqualTo(TimeUnit.MINUTES.toMillis(15))
    }

    @Test
    fun periodicBuilder_kotlinDuration_clampsToMinInterval() {
        val request = PeriodicWorkRequest.Builder(DurationTestWorker::class.java, 1.minutes).build()
        assertThat(request.workSpec.intervalDuration)
            .isEqualTo(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
    }

    @Test
    fun periodicWorkRequestBuilder_reified_kotlinDuration() {
        val request = PeriodicWorkRequestBuilder<DurationTestWorker>(2.hours).build()
        assertThat(request.workSpec.workerClassName).isEqualTo(DurationTestWorker::class.java.name)
        assertThat(request.workSpec.intervalDuration).isEqualTo(TimeUnit.HOURS.toMillis(2))
    }

    @Test
    fun periodicWorkRequestBuilder_reified_kotlinDuration_withFlex() {
        val request = PeriodicWorkRequestBuilder<DurationTestWorker>(2.hours, 30.minutes).build()
        assertThat(request.workSpec.intervalDuration).isEqualTo(TimeUnit.HOURS.toMillis(2))
        assertThat(request.workSpec.flexDuration).isEqualTo(TimeUnit.MINUTES.toMillis(30))
    }

    @Test(expected = IllegalArgumentException::class)
    fun periodicBuilder_class_infiniteRepeatInterval_throwsIllegalArgumentException() {
        PeriodicWorkRequest.Builder(DurationTestWorker::class.java, Duration.INFINITE).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun periodicBuilder_kclass_infiniteRepeatInterval_throwsIllegalArgumentException() {
        PeriodicWorkRequest.Builder(DurationTestWorker::class, Duration.INFINITE).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun periodicBuilder_class_withFlex_infiniteRepeatInterval_throwsIllegalArgumentException() {
        PeriodicWorkRequest.Builder(DurationTestWorker::class.java, Duration.INFINITE, 15.minutes)
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun periodicBuilder_class_withFlex_infiniteFlexInterval_throwsIllegalArgumentException() {
        PeriodicWorkRequest.Builder(DurationTestWorker::class.java, 1.hours, Duration.INFINITE)
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun periodicBuilder_kclass_withFlex_infiniteRepeatInterval_throwsIllegalArgumentException() {
        PeriodicWorkRequest.Builder(DurationTestWorker::class, Duration.INFINITE, 15.minutes)
            .build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun periodicBuilder_kclass_withFlex_infiniteFlexInterval_throwsIllegalArgumentException() {
        PeriodicWorkRequest.Builder(DurationTestWorker::class, 1.hours, Duration.INFINITE).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun periodicWorkRequestBuilder_reified_infiniteRepeatInterval_throwsIllegalArgumentException() {
        PeriodicWorkRequestBuilder<DurationTestWorker>(Duration.INFINITE).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun periodicWorkRequestBuilder_reified_withFlex_infiniteRepeatInterval_throws() {
        PeriodicWorkRequestBuilder<DurationTestWorker>(Duration.INFINITE, 15.minutes).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun periodicWorkRequestBuilder_reified_withFlex_infiniteFlexInterval_throws() {
        PeriodicWorkRequestBuilder<DurationTestWorker>(1.hours, Duration.INFINITE).build()
    }

    // --- Constraints.Builder -----------------------------------------------------------------

    @Test
    fun constraints_triggerContentDelays_kotlinDuration() {
        val constraints =
            Constraints.Builder()
                .setTriggerContentUpdateDelay(5.seconds)
                .setTriggerContentMaxDelay(1.minutes)
                .build()
        assertThat(constraints.contentTriggerUpdateDelayMillis)
            .isEqualTo(TimeUnit.SECONDS.toMillis(5))
        assertThat(constraints.contentTriggerMaxDelayMillis).isEqualTo(TimeUnit.MINUTES.toMillis(1))
    }

    @Test
    fun constraints_triggerContentDelays_kotlinDuration_matchesTimeUnitOverload() {
        val viaDuration =
            Constraints.Builder()
                .setTriggerContentUpdateDelay(5.seconds)
                .setTriggerContentMaxDelay(1.minutes)
                .build()
        val viaTimeUnit =
            Constraints.Builder()
                .setTriggerContentUpdateDelay(5, TimeUnit.SECONDS)
                .setTriggerContentMaxDelay(1, TimeUnit.MINUTES)
                .build()
        assertThat(viaDuration).isEqualTo(viaTimeUnit)
    }

    class DurationTestWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result = Result.success()
    }
}
