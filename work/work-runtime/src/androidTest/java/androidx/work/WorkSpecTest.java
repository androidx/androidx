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
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.work.impl.model.WorkSpec;
import androidx.work.worker.InfiniteTestWorker;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class WorkSpecTest extends WorkManagerTest {
    private static final long DEFAULT_INITIAL_DELAY_TIME_MS = 5000L;
    private static final long DEFAULT_BACKOFF_DELAY_TIME_MS = 5000L;
    private static final long DEFAULT_PERIOD_START_TIME = 10000L;
    private static final long DEFAULT_FLEX_TIME_MS =
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS + 5000L;
    private static final long DEFAULT_INTERVAL_TIME_MS =
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS + 5000L;

    @Test
    @SmallTest
    public void testCalculateNextRunTime_firstRunAttempt_oneOff() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialDelay(DEFAULT_INITIAL_DELAY_TIME_MS, TimeUnit.MILLISECONDS)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();
        long actualDelay = work.getWorkSpec().calculateNextRunTime();
        assertThat(actualDelay, is(DEFAULT_PERIOD_START_TIME + DEFAULT_INITIAL_DELAY_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_firstRun_periodic_withFlexApplicable() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS,
                TimeUnit.MILLISECONDS,
                DEFAULT_FLEX_TIME_MS,
                TimeUnit.MILLISECONDS)
                .build();

        long now = System.currentTimeMillis();
        WorkSpec workSpec = periodicWork.getWorkSpec();
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime, greaterThan(now));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_firstRun_periodic_withFlexNotApplicable() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS,
                TimeUnit.MILLISECONDS,
                DEFAULT_INTERVAL_TIME_MS,
                TimeUnit.MILLISECONDS)
                .build();

        long now = System.currentTimeMillis();
        WorkSpec workSpec = periodicWork.getWorkSpec();
        long nextRunTime = workSpec.calculateNextRunTime();
        assertCloseValues(nextRunTime, now);
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_nextRun_periodic_withFlexApplicable() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, TimeUnit.MILLISECONDS,
                DEFAULT_FLEX_TIME_MS, TimeUnit.MILLISECONDS)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime,
                is(DEFAULT_PERIOD_START_TIME + DEFAULT_INTERVAL_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_nextRun_periodic_withFlexNotApplicable() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS,
                TimeUnit.MILLISECONDS,
                DEFAULT_INTERVAL_TIME_MS,
                TimeUnit.MILLISECONDS)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime, is(DEFAULT_PERIOD_START_TIME + DEFAULT_INTERVAL_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_nextRun_periodic_withInitialDelay() {
        long delay = 10000L;
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, TimeUnit.MILLISECONDS,
                DEFAULT_INTERVAL_TIME_MS, TimeUnit.MILLISECONDS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        long now = System.currentTimeMillis();
        long nextRunTime = workSpec.calculateNextRunTime();
        long delta = nextRunTime - now;
        assertCloseValues(delta, delay);
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_nextRun_periodic_withInitialDelay_flex_firstRun() {
        long delay = 10000L;
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, TimeUnit.MILLISECONDS,
                DEFAULT_FLEX_TIME_MS, TimeUnit.MILLISECONDS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        long now = System.currentTimeMillis();
        long nextRunTime = workSpec.calculateNextRunTime();
        long delta = nextRunTime - now;
        assertCloseValues(delta, delay + DEFAULT_INTERVAL_TIME_MS - DEFAULT_FLEX_TIME_MS);
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_rerunAttempt_exponential() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        TimeUnit.MILLISECONDS)
                .setInitialRunAttemptCount(1)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        TimeUnit.MILLISECONDS)
                .setInitialRunAttemptCount(2)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest work3 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        TimeUnit.MILLISECONDS)
                .setInitialRunAttemptCount(3)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();

        long nextRunTime1 = work1.getWorkSpec().calculateNextRunTime();
        long nextRunTime2 = work2.getWorkSpec().calculateNextRunTime();
        long nextRunTime3 = work3.getWorkSpec().calculateNextRunTime();

        long nextRunTimeDeltaBetweenWork2AndWork1 = nextRunTime2 - nextRunTime1;
        long nextRunTimeDeltaBetweenWork3AndWork2 = nextRunTime3 - nextRunTime2;

        assertThat(nextRunTimeDeltaBetweenWork3AndWork2,
                is(greaterThan(nextRunTimeDeltaBetweenWork2AndWork1)));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_rerunAttempt_linear() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        TimeUnit.MILLISECONDS)
                .setInitialRunAttemptCount(1)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        TimeUnit.MILLISECONDS)
                .setInitialRunAttemptCount(2)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();
        OneTimeWorkRequest work3 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        TimeUnit.MILLISECONDS)
                .setInitialRunAttemptCount(3)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();

        long nextRunTime1 = work1.getWorkSpec().calculateNextRunTime();
        long nextRunTime2 = work2.getWorkSpec().calculateNextRunTime();
        long nextRunTime3 = work3.getWorkSpec().calculateNextRunTime();

        long nextRunTimeDeltaBetweenWork2AndWork1 = nextRunTime2 - nextRunTime1;
        long nextRunTimeDeltaBetweenWork3AndWork2 = nextRunTime3 - nextRunTime2;

        assertThat(nextRunTimeDeltaBetweenWork2AndWork1, is(nextRunTimeDeltaBetweenWork3AndWork2));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_rerunAttempt_linear_upperBound() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        WorkRequest.MAX_BACKOFF_MILLIS + 1,
                        TimeUnit.MILLISECONDS)
                .setInitialRunAttemptCount(1)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();
        assertThat(work.getWorkSpec().calculateNextRunTime(),
                is(DEFAULT_PERIOD_START_TIME + WorkRequest.MAX_BACKOFF_MILLIS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_rerunAttempt_exponential_upperBound() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MAX_BACKOFF_MILLIS + 1,
                        TimeUnit.MILLISECONDS)
                .setInitialRunAttemptCount(1)
                .setPeriodStartTime(DEFAULT_PERIOD_START_TIME, TimeUnit.MILLISECONDS)
                .build();

        assertThat(work.getWorkSpec().calculateNextRunTime(),
                is(DEFAULT_PERIOD_START_TIME + WorkRequest.MAX_BACKOFF_MILLIS));
    }

    private void assertCloseValues(long value, long target) {
        double min = Math.min(value, target);
        double max = Math.max(value, target);
        double ratio = min / max;
        assertThat(ratio, greaterThanOrEqualTo(0.999d));
    }
}
