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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.concurrent.TimeUnit;

import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.workers.ConstraintTrackingWorker;
import androidx.work.worker.TestWorker;

@SmallTest
public class WorkTest extends WorkManagerTest {
    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    private Work.Builder mBuilder;

    @Before
    public void setUp() {
        mBuilder = new Work.Builder(TestWorker.class);
    }

    @Test
    public void testBuild_withInitialDelay() {
        final long expectedInitialDelay = 123L;
        Work work = mBuilder.withInitialDelay(expectedInitialDelay, TimeUnit.MILLISECONDS).build();
        assertThat(getWorkSpec(work).getInitialDelay(), is(expectedInitialDelay));
    }

    @Test
    public void testBuild_setBackoffCriteria_exceedMaxBackoffDuration() {
        final long backoffDuration = BaseWork.MAX_BACKOFF_MILLIS + 123L;
        Work work = mBuilder
                .withBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        backoffDuration,
                        TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(work).getBackoffDelayDuration(), is(BaseWork.MAX_BACKOFF_MILLIS));
    }

    @Test
    public void testBuild_setBackoffCriteria_lessThanMinBackoffDuration() {
        final long backoffDuration = BaseWork.MIN_BACKOFF_MILLIS - 123L;
        Work work = mBuilder
                .withBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        backoffDuration,
                        TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(work).getBackoffDelayDuration(), is(BaseWork.MIN_BACKOFF_MILLIS));
    }

    @Test
    @SdkSuppress(minSdkVersion = 23)
    public void testBuild_backoffAndIdleMode_throwsIllegalArgumentException() {
        mThrown.expect(IllegalArgumentException.class);
        Constraints constraints = new Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .build();
        mBuilder.withBackoffCriteria(BackoffPolicy.EXPONENTIAL, 123L, TimeUnit.MILLISECONDS)
                .withConstraints(constraints)
                .build();
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testBuild_withBatteryNotLowConstraint_expectsConstraintTrackingWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        Work work = mBuilder.withConstraints(constraints).build();
        WorkSpec workSpec = getWorkSpec(work);
        String workerClassName = workSpec.getArguments().getString(
                ConstraintTrackingWorker.ARGUMENT_CLASS_NAME, null);

        assertThat(workSpec.getWorkerClassName(), is(ConstraintTrackingWorker.class.getName()));
        assertThat(workerClassName, is(TestWorker.class.getName()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testBuild_withStorageNotLowConstraint_expectsConstraintTrackingWorker() {
        Constraints constraints = new Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build();

        Work work = mBuilder.withConstraints(constraints).build();
        WorkSpec workSpec = getWorkSpec(work);
        String workerClassName = workSpec.getArguments().getString(
                ConstraintTrackingWorker.ARGUMENT_CLASS_NAME, null);

        assertThat(workSpec.getWorkerClassName(), is(ConstraintTrackingWorker.class.getName()));
        assertThat(workerClassName, is(TestWorker.class.getName()));
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testBuild_withBatteryNotLowConstraintApi26() {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        Work work = mBuilder.withConstraints(constraints).build();
        WorkSpec workSpec = getWorkSpec(work);
        assertThat(workSpec.getWorkerClassName(), is(TestWorker.class.getName()));
    }
}
