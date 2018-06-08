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

import androidx.work.worker.TestWorker;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class PeriodicWorkTest extends WorkManagerTest {

    @Rule
    public ExpectedException mThrown = ExpectedException.none();

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 23)
    public void testBuild_backoffAndIdleMode_throwsIllegalArgumentException() {
        mThrown.expect(IllegalArgumentException.class);
        new PeriodicWorkRequest.Builder(
                TestWorker.class,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 20000, TimeUnit.MILLISECONDS)
                .setConstraints(new Constraints.Builder()
                        .setRequiresDeviceIdle(true)
                        .build())
                .build();
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_onlyIntervalDuration_inRange() {
        long testInterval = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS + 123L;
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).intervalDuration, is(testInterval));
        assertThat(getWorkSpec(periodicWork).flexDuration, is(testInterval));
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_onlyIntervalDuration_outOfRange() {
        long testInterval = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS - 123L;
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).intervalDuration,
                is(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS));
        assertThat(getWorkSpec(periodicWork).flexDuration,
                is(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS));
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_intervalAndFlexDurations_inRange() {
        long testInterval = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS + 123L;
        long testFlex = PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS + 123L;
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).intervalDuration, is(testInterval));
        assertThat(getWorkSpec(periodicWork).flexDuration, is(testFlex));
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_intervalAndFlexDurations_outOfRange() {
        long testInterval = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS - 123L;
        long testFlex = PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS - 123L;
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).intervalDuration,
                is(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS));
        assertThat(getWorkSpec(periodicWork).flexDuration,
                is(PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS));
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_intervalInRange_flexOutOfRange() {
        long testInterval = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS + 123L;
        long testFlex = PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS - 123L;
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).intervalDuration, is(testInterval));
        assertThat(getWorkSpec(periodicWork).flexDuration,
                is(PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS));
    }

    @Test
    @SmallTest
    public void testBuild_setPeriodic_intervalOutOfRange_flexInRange() {
        long testInterval = PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS - 123L;
        long testFlex = PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS + 123L;
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                testInterval,
                TimeUnit.MILLISECONDS,
                testFlex,
                TimeUnit.MILLISECONDS)
                .build();
        assertThat(getWorkSpec(periodicWork).intervalDuration,
                is(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS));
        assertThat(getWorkSpec(periodicWork).flexDuration, is(testFlex));
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 26)
    public void testBuild_setPeriodic_withDurationParameters() {
        Duration repeatInterval = Duration.ofDays(2).plusHours(3);
        Duration flexInterval = Duration.ofHours(1).plusMinutes(2);
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                TestWorker.class,
                repeatInterval,
                flexInterval)
                .build();
        assertThat(
                getWorkSpec(periodicWork).intervalDuration,
                is(TimeUnit.HOURS.toMillis((2 * 24) + 3)));
        assertThat(
                getWorkSpec(periodicWork).flexDuration,
                is(TimeUnit.MINUTES.toMillis((1 * 60) + 2)));
    }
}
