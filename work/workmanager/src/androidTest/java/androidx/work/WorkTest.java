/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.work;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;

import androidx.work.worker.TestWorker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@SmallTest
public class WorkTest extends WorkManagerTest {
    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    private OneTimeWorkRequest.Builder mBuilder;

    @Before
    public void setUp() {
        mBuilder = new OneTimeWorkRequest.Builder(TestWorker.class);
    }

    @Test
    public void testBuild_withInitialDelay() {
        final long expectedInitialDelay = 123L;
        OneTimeWorkRequest work = mBuilder
                .setInitialDelay(expectedInitialDelay, TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(work).initialDelay, is(expectedInitialDelay));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testBuild_withInitialDelayUsingDurationParameter() {
        OneTimeWorkRequest work = mBuilder
                .setInitialDelay(Duration.ofHours(2).plusMinutes(3))
                .build();
        assertThat(
                getWorkSpec(work).initialDelay,
                is(TimeUnit.MINUTES.toMillis((2 * 60) + 3)));
    }

    @Test
    public void testBuild_setBackoffCriteria_exceedMaxBackoffDuration() {
        final long backoffDuration = WorkRequest.MAX_BACKOFF_MILLIS + 123L;
        OneTimeWorkRequest work = mBuilder
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        backoffDuration,
                        TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(work).backoffDelayDuration, is(WorkRequest.MAX_BACKOFF_MILLIS));
    }

    @Test
    public void testBuild_setBackoffCriteria_lessThanMinBackoffDuration() {
        final long backoffDuration = WorkRequest.MIN_BACKOFF_MILLIS - 123L;
        OneTimeWorkRequest work = mBuilder
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        backoffDuration,
                        TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(work).backoffDelayDuration, is(WorkRequest.MIN_BACKOFF_MILLIS));
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    public void testBuild_backoffAndIdleMode_throwsIllegalArgumentException() {
        mThrown.expect(IllegalArgumentException.class);
        Constraints constraints = new Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .build();
        mBuilder.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 123L, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build();
    }
}
