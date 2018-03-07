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
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.workers.ConstraintTrackingWorker;
import androidx.work.worker.TestWorker;

@RunWith(AndroidJUnit4.class)
public class PeriodicWorkTest extends WorkManagerTest {

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testBuild_backoffAndIdleMode_throwsIllegalArgumentException() {
        mThrown.expect(IllegalArgumentException.class);
        new PeriodicWork.Builder(
                TestWorker.class,
                PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS)
                .withBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20000, TimeUnit.MILLISECONDS)
                .withConstraints(new Constraints.Builder()
                        .setRequiresDeviceIdle(true)
                        .build())
                .build();
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_onlyIntervalDuration_inRange() {
        long testInterval = PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS + 123L;
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).getIntervalDuration(), is(testInterval));
        assertThat(getWorkSpec(periodicWork).getFlexDuration(), is(testInterval));
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_onlyIntervalDuration_outOfRange() {
        long testInterval = PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS - 123L;
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).getIntervalDuration(),
                is(PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS));
        assertThat(getWorkSpec(periodicWork).getFlexDuration(),
                is(PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS));
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_intervalAndFlexDurations_inRange() {
        long testInterval = PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS + 123L;
        long testFlex = PeriodicWork.MIN_PERIODIC_FLEX_MILLIS + 123L;
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).getIntervalDuration(), is(testInterval));
        assertThat(getWorkSpec(periodicWork).getFlexDuration(), is(testFlex));
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_intervalAndFlexDurations_outOfRange() {
        long testInterval = PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS - 123L;
        long testFlex = PeriodicWork.MIN_PERIODIC_FLEX_MILLIS - 123L;
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).getIntervalDuration(),
                is(PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS));
        assertThat(getWorkSpec(periodicWork).getFlexDuration(),
                is(PeriodicWork.MIN_PERIODIC_FLEX_MILLIS));
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_intervalInRange_flexOutOfRange() {
        long testInterval = PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS + 123L;
        long testFlex = PeriodicWork.MIN_PERIODIC_FLEX_MILLIS - 123L;
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).getIntervalDuration(), is(testInterval));
        assertThat(getWorkSpec(periodicWork).getFlexDuration(),
                is(PeriodicWork.MIN_PERIODIC_FLEX_MILLIS));
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_intervalOutOfRange_flexInRange() {
        long testInterval = PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS - 123L;
        long testFlex = PeriodicWork.MIN_PERIODIC_FLEX_MILLIS + 123L;
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).getIntervalDuration(),
                is(PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS));
        assertThat(getWorkSpec(periodicWork).getFlexDuration(), is(testFlex));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testBuild_withBatteryNotLowConstraint_expectsConstraintTrackingWorker() {
        long testInterval = PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS + 123L;
        long testFlex = PeriodicWork.MIN_PERIODIC_FLEX_MILLIS + 123L;
        PeriodicWork.Builder builder = new PeriodicWork.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS);

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWork work = builder.withConstraints(constraints).build();
        WorkSpec workSpec = getWorkSpec(work);
        String workerClassName = workSpec.getArguments().getString(
                ConstraintTrackingWorker.ARGUMENT_CLASS_NAME, null);

        assertThat(workSpec.getWorkerClassName(), is(ConstraintTrackingWorker.class.getName()));
        assertThat(workerClassName, is(TestWorker.class.getName()));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23, maxSdkVersion = 25)
    public void testBuild_withStorageNotLowConstraint_expectsConstraintTrackingWorker() {
        long testInterval = PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS + 123L;
        long testFlex = PeriodicWork.MIN_PERIODIC_FLEX_MILLIS + 123L;
        PeriodicWork.Builder builder = new PeriodicWork.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS);

        Constraints constraints = new Constraints.Builder()
                .setRequiresStorageNotLow(true)
                .build();

        PeriodicWork work = builder.withConstraints(constraints).build();
        WorkSpec workSpec = getWorkSpec(work);
        String workerClassName = workSpec.getArguments().getString(
                ConstraintTrackingWorker.ARGUMENT_CLASS_NAME, null);

        assertThat(workSpec.getWorkerClassName(), is(ConstraintTrackingWorker.class.getName()));
        assertThat(workerClassName, is(TestWorker.class.getName()));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    public void testBuild_withBatteryNotLowConstraintApi26() {
        long testInterval = PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS + 123L;
        long testFlex = PeriodicWork.MIN_PERIODIC_FLEX_MILLIS + 123L;
        PeriodicWork.Builder builder = new PeriodicWork.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS);

        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWork work = builder.withConstraints(constraints).build();
        WorkSpec workSpec = getWorkSpec(work);
        assertThat(workSpec.getWorkerClassName(), is(TestWorker.class.getName()));
    }
}
