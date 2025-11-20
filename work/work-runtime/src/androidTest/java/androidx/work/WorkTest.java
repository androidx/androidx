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

import static androidx.work.NetworkType.METERED;
import static androidx.work.NetworkType.NOT_REQUIRED;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import android.os.Build;

import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.work.impl.model.WorkSpec;
import androidx.work.worker.TestWorker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.UUID;
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
    public void testBuild_GetsUniqueIdsOnBuild() {
        UUID firstId = mBuilder.build().getId();
        UUID secondId = mBuilder.build().getId();
        assertThat(firstId, is(not(secondId)));
    }

    @Test
    public void testBuild_GeneratesSimilarWorkSpecsOnBuild() {
        WorkSpec firstWorkSpec = mBuilder.build().getWorkSpec();
        WorkSpec secondWorkSpec = mBuilder.build().getWorkSpec();

        // Because of the previous test (testBuild_GetsUniqueIdsOnBuild), we know the id's are
        // different, so just set them to be the same for now so we can use an equality check.
        assertThat(new WorkSpec(secondWorkSpec.id, firstWorkSpec), is(equalTo(secondWorkSpec)));
    }

    @Test
    public void testBuild_withInitialDelay() {
        final long expectedInitialDelay = 123L;
        OneTimeWorkRequest work = mBuilder
                .setInitialDelay(expectedInitialDelay, TimeUnit.MILLISECONDS)
                .build();
        assertThat(work.getWorkSpec().initialDelay, is(expectedInitialDelay));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testBuild_withInitialDelayUsingDurationParameter() {
        OneTimeWorkRequest work = mBuilder
                .setInitialDelay(Duration.ofHours(2).plusMinutes(3))
                .build();
        assertThat(
                work.getWorkSpec().initialDelay,
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
        assertThat(work.getWorkSpec().backoffDelayDuration, is(WorkRequest.MAX_BACKOFF_MILLIS));
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
        assertThat(work.getWorkSpec().backoffDelayDuration, is(WorkRequest.MIN_BACKOFF_MILLIS));
    }

    @Test
    public void testBuild_backoffAndIdleMode_throwsIllegalArgumentException() {
        mThrown.expect(IllegalArgumentException.class);
        Constraints constraints = new Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .build();
        mBuilder.setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 123L, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build();
    }

    @Test
    public void testBuild_initialDelayMaxLong_throwsIllegalArgumentException() {
        mThrown.expect(IllegalArgumentException.class);
        new OneTimeWorkRequest.Builder(
                TestWorker.class)
                .setInitialDelay(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
                .build();
    }

    @Test
    public void testBuild_initialDelayTooLarge_throwsIllegalArgumentException() {
        mThrown.expect(IllegalArgumentException.class);
        long now = System.currentTimeMillis();
        new OneTimeWorkRequest.Builder(
                TestWorker.class)
                .setInitialDelay(Long.MAX_VALUE - now, TimeUnit.MILLISECONDS)
                .build();
    }

    @Test
    public void testBuild_expedited_noConstraints() {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }

        OneTimeWorkRequest request = mBuilder
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build();
        WorkSpec workSpec = request.getWorkSpec();
        Constraints constraints = workSpec.constraints;
        assertThat(constraints.getRequiredNetworkType(), is(NOT_REQUIRED));
    }

    @Test
    public void testBuild_expedited_networkConstraints() {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }

        OneTimeWorkRequest request = mBuilder
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.METERED)
                        .build()
                )
                .build();
        WorkSpec workSpec = request.getWorkSpec();
        Constraints constraints = workSpec.constraints;
        assertThat(constraints.getRequiredNetworkType(), is(METERED));
    }

    @Test
    public void testBuild_expedited_networkStorageConstraints() {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }

        OneTimeWorkRequest request = mBuilder
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.METERED)
                        .setRequiresStorageNotLow(true)
                        .build()
                )
                .build();
        WorkSpec workSpec = request.getWorkSpec();
        Constraints constraints = workSpec.constraints;
        assertThat(constraints.getRequiredNetworkType(), is(METERED));
    }

    @Test
    public void testBuild_expedited_withUnspportedConstraints() {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }

        mThrown.expect(IllegalArgumentException.class);
        OneTimeWorkRequest request = mBuilder
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.METERED)
                        .setRequiresStorageNotLow(true)
                        .setRequiresCharging(true)
                        .build()
                )
                .build();
        WorkSpec workSpec = request.getWorkSpec();
        Constraints constraints = workSpec.constraints;
        assertThat(constraints.getRequiredNetworkType(), is(METERED));
    }

    @Test
    public void testBuild_expedited_withUnspportedConstraints2() {
        if (Build.VERSION.SDK_INT < 31) {
            return;
        }

        mThrown.expect(IllegalArgumentException.class);
        OneTimeWorkRequest request = mBuilder
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.METERED)
                        .setRequiresStorageNotLow(true)
                        .setRequiresDeviceIdle(true)
                        .build()
                )
                .build();
        WorkSpec workSpec = request.getWorkSpec();
        Constraints constraints = workSpec.constraints;
        assertThat(constraints.getRequiredNetworkType(), is(METERED));
    }
}
