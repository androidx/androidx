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

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.work.impl.model.WorkSpec;
import androidx.work.worker.InfiniteTestWorker;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WorkSpecTest extends WorkManagerTest {
    private static final long DEFAULT_INITIAL_DELAY_TIME_MS = 5000L;
    private static final long DEFAULT_BACKOFF_DELAY_TIME_MS =
            WorkRequest.MIN_BACKOFF_MILLIS + 2000L;
    private static final long DEFAULT_LAST_ENQUEUE_TIME_MS = 10000L;
    private static final long DEFAULT_FLEX_TIME_MS =
            PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS + 5000L;
    private static final long DEFAULT_INTERVAL_TIME_MS =
            PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS + 5000L;
    private static final long DEFAULT_OVERRIDE_TIME_MS =
            DEFAULT_LAST_ENQUEUE_TIME_MS + PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS + 6000L;
    private static final long DEFAULT_OVERRIDE_TIME_SOONER_MS =
            DEFAULT_LAST_ENQUEUE_TIME_MS + DEFAULT_INTERVAL_TIME_MS - 2000L;

    @Test
    @SmallTest
    public void testCalculateNextRunTime_firstRunAttempt_oneOff() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialDelay(DEFAULT_INITIAL_DELAY_TIME_MS, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();
        long actualDelay = work.getWorkSpec().calculateNextRunTime();
        assertThat(actualDelay, is(DEFAULT_LAST_ENQUEUE_TIME_MS + DEFAULT_INITIAL_DELAY_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_firstRun_periodic_withFlexApplicable() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS,
                MILLISECONDS,
                DEFAULT_FLEX_TIME_MS,
                MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime, is(DEFAULT_LAST_ENQUEUE_TIME_MS
                + DEFAULT_INTERVAL_TIME_MS - DEFAULT_FLEX_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_firstRun_periodic_withFlexNotApplicable() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS,
                MILLISECONDS,
                DEFAULT_INTERVAL_TIME_MS,
                MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime,
                is(DEFAULT_LAST_ENQUEUE_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_nextRun_periodic_withFlexApplicable() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS,
                DEFAULT_FLEX_TIME_MS, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        workSpec.setPeriodCount(1);
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime,
                is(DEFAULT_LAST_ENQUEUE_TIME_MS + DEFAULT_INTERVAL_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_nextRun_periodic_withFlexNotApplicable() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS,
                MILLISECONDS,
                DEFAULT_INTERVAL_TIME_MS,
                MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        workSpec.setPeriodCount(1);
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime, is(DEFAULT_LAST_ENQUEUE_TIME_MS + DEFAULT_INTERVAL_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_nextRun_periodic_withInitialDelay() {
        long delay = 10000L;
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS)
                .setInitialDelay(delay, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime, is(DEFAULT_LAST_ENQUEUE_TIME_MS + delay));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_nextRun_periodic_withInitialDelay_flex_firstRun() {
        long delay = 10000L;
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS,
                DEFAULT_FLEX_TIME_MS, MILLISECONDS)
                .setInitialDelay(delay, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime, is(DEFAULT_LAST_ENQUEUE_TIME_MS
                + delay + DEFAULT_INTERVAL_TIME_MS - DEFAULT_FLEX_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_rerunAttempt_exponential() {
        OneTimeWorkRequest work1 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        MILLISECONDS)
                .setInitialRunAttemptCount(1)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        MILLISECONDS)
                .setInitialRunAttemptCount(2)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();
        OneTimeWorkRequest work3 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        MILLISECONDS)
                .setInitialRunAttemptCount(3)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
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
                        MILLISECONDS)
                .setInitialRunAttemptCount(1)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();
        OneTimeWorkRequest work2 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        MILLISECONDS)
                .setInitialRunAttemptCount(2)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();
        OneTimeWorkRequest work3 = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        MILLISECONDS)
                .setInitialRunAttemptCount(3)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
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
                        MILLISECONDS)
                .setInitialRunAttemptCount(1)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();
        assertThat(work.getWorkSpec().calculateNextRunTime(),
                is(DEFAULT_LAST_ENQUEUE_TIME_MS + WorkRequest.MAX_BACKOFF_MILLIS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_rerunAttempt_exponential_upperBound() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setBackoffCriteria(
                        BackoffPolicy.EXPONENTIAL,
                        WorkRequest.MAX_BACKOFF_MILLIS + 1,
                        MILLISECONDS)
                .setInitialRunAttemptCount(1)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();

        assertThat(work.getWorkSpec().calculateNextRunTime(),
                is(DEFAULT_LAST_ENQUEUE_TIME_MS + WorkRequest.MAX_BACKOFF_MILLIS));
    }

    @Test
    @SmallTest
    public void testNextScheduleTimeOverride_doesntChangeLastEnqueueTime() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS,
                DEFAULT_FLEX_TIME_MS, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .setNextScheduleTimeOverride(DEFAULT_OVERRIDE_TIME_SOONER_MS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        workSpec.setPeriodCount(0);
        long lastEnqueueTime = workSpec.lastEnqueueTime;
        assertThat(lastEnqueueTime,
                is(DEFAULT_LAST_ENQUEUE_TIME_MS));
    }

    @Test
    @SmallTest
    public void testNextScheduleTimeOverride_doesntRespectFlex() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS,
                12345, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .setNextScheduleTimeOverride(DEFAULT_OVERRIDE_TIME_SOONER_MS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        workSpec.setPeriodCount(0);

        assertThat(workSpec.calculateNextRunTime(),
                // Flex doesn't adjust the schedule; we run at the override time
                is(DEFAULT_OVERRIDE_TIME_SOONER_MS));
    }

    @Test
    @SmallTest
    public void testNextScheduleTimeOverride_firstRun() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS,
                DEFAULT_FLEX_TIME_MS, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .setNextScheduleTimeOverride(DEFAULT_OVERRIDE_TIME_SOONER_MS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        workSpec.setPeriodCount(0);
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime,
                is(DEFAULT_OVERRIDE_TIME_SOONER_MS));
    }

    @Test
    @SmallTest
    public void testNextScheduleTimeOverride_firstRun_allowsImmediateExecution() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS,
                DEFAULT_FLEX_TIME_MS, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .setNextScheduleTimeOverride(DEFAULT_LAST_ENQUEUE_TIME_MS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        workSpec.setPeriodCount(0); // Only for the first run may minimum period be ignored.
        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime,
                is(DEFAULT_LAST_ENQUEUE_TIME_MS));
    }

    @Test
    @SmallTest
    public void testNextScheduleTimeOverride_firstRun_allowsScheduleInPast() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS,
                DEFAULT_FLEX_TIME_MS, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .setNextScheduleTimeOverride(DEFAULT_LAST_ENQUEUE_TIME_MS - 2000L)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        workSpec.setPeriodCount(0); // Only for the first run may minimum period be ignored.
        long nextRunTime = workSpec.calculateNextRunTime();

        // The override may be in the past. This would result in immediate execution.
        assertThat(nextRunTime,
                is(DEFAULT_LAST_ENQUEUE_TIME_MS - 2000L));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_nextRun_minimumDelay_met() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS,
                DEFAULT_FLEX_TIME_MS, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .setNextScheduleTimeOverride(DEFAULT_OVERRIDE_TIME_SOONER_MS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        workSpec.setPeriodCount(1);

        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime,
                // Rescheduling for a sooner-than-period time is ok, as long as it's later than the
                // global minimum period.
                is(DEFAULT_OVERRIDE_TIME_SOONER_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_nextRun_minimumDelay_clamped() {
        PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class,
                DEFAULT_INTERVAL_TIME_MS, MILLISECONDS,
                DEFAULT_FLEX_TIME_MS, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .setNextScheduleTimeOverride(DEFAULT_LAST_ENQUEUE_TIME_MS)
                .build();

        WorkSpec workSpec = periodicWork.getWorkSpec();
        workSpec.setPeriodCount(1);

        long nextRunTime = workSpec.calculateNextRunTime();
        assertThat(nextRunTime,
                // For subsequent runs, the minimum period interval is enforced.
                is(DEFAULT_LAST_ENQUEUE_TIME_MS
                        + PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_oneTimeWork_nextScheduleTimeOverride_doesNothing() {
        OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(InfiniteTestWorker.class)
                .setInitialDelay(DEFAULT_INITIAL_DELAY_TIME_MS, MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();
        // Directly set somehow. Still shouldn't work.
        work.getWorkSpec().setNextScheduleTimeOverride(DEFAULT_OVERRIDE_TIME_MS);

        assertThat(work.getWorkSpec().calculateNextRunTime(),
                is(DEFAULT_LAST_ENQUEUE_TIME_MS + DEFAULT_INITIAL_DELAY_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_backOffSet_overrideTakesPriority() {
        PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(
                InfiniteTestWorker.class, DEFAULT_INTERVAL_TIME_MS, MILLISECONDS)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        DEFAULT_BACKOFF_DELAY_TIME_MS,
                        MILLISECONDS)
                .setLastEnqueueTime(DEFAULT_LAST_ENQUEUE_TIME_MS, MILLISECONDS)
                .build();
        work.getWorkSpec().setPeriodCount(2);
        work.getWorkSpec().runAttemptCount = 1;
        // During the run of a worker, if override is set, then the Worker returns Result.retry().
        // The override must take priority, but the runAttemptCount must be maintained, so if
        // clearOverride() is later called, the original backoff would be respected.
        work.getWorkSpec().setNextScheduleTimeOverride(DEFAULT_OVERRIDE_TIME_MS);

        // The override should be respected, while the runCount value is preserved but ignored.
        assertThat(work.getWorkSpec().calculateNextRunTime(),
                is(DEFAULT_OVERRIDE_TIME_MS));
        assertThat(work.getWorkSpec().runAttemptCount, is(1));
    }
}
