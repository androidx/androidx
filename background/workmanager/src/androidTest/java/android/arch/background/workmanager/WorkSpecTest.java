/*
 * Copyright 2017 The Android Open Source Project
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

package android.arch.background.workmanager;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import android.arch.background.workmanager.worker.InfiniteTestWorker;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WorkSpecTest {
    private static final long DEFAULT_INITIAL_DELAY_TIME_MS = 5000L;
    private static final long DEFAULT_BACKOFF_DELAY_TIME_MS = 5000L;
    private static final long DEFAULT_PERIOD_START_TIME = 10000L;
    private static final long DEFAULT_FLEX_TIME_MS = PeriodicWork.MIN_PERIODIC_FLEX_MILLIS + 5000L;
    private static final long DEFAULT_INTERVAL_TIME_MS =
            PeriodicWork.MIN_PERIODIC_INTERVAL_MILLIS + 5000L;

    @Test
    @SmallTest
    public void testCalculateNextRunTime_firstRunAttempt_oneOff() {
        Work work = new Work.Builder(InfiniteTestWorker.class)
                .withInitialDelay(DEFAULT_INITIAL_DELAY_TIME_MS)
                .withPeriodStartTime(DEFAULT_PERIOD_START_TIME)
                .build();
        long actualDelay = work.getWorkSpec().calculateNextRunTime();
        assertThat(actualDelay, is(DEFAULT_PERIOD_START_TIME + DEFAULT_INITIAL_DELAY_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_firstRunAttempt_periodic() {
        PeriodicWork periodicWork = new PeriodicWork.Builder(
                InfiniteTestWorker.class, DEFAULT_INTERVAL_TIME_MS, DEFAULT_FLEX_TIME_MS)
                .withPeriodStartTime(DEFAULT_PERIOD_START_TIME)
                .build();
        assertThat(periodicWork.getWorkSpec().calculateNextRunTime(),
                is(DEFAULT_PERIOD_START_TIME + DEFAULT_INTERVAL_TIME_MS - DEFAULT_FLEX_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_rerunAttempt_exponential() {
        Work work1 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(1)
                .withPeriodStartTime(DEFAULT_PERIOD_START_TIME)
                .build();
        Work work2 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(2)
                .withPeriodStartTime(DEFAULT_PERIOD_START_TIME)
                .build();
        Work work3 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(3)
                .withPeriodStartTime(DEFAULT_PERIOD_START_TIME)
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
        Work work1 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_LINEAR, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(1)
                .withPeriodStartTime(DEFAULT_PERIOD_START_TIME)
                .build();
        Work work2 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_LINEAR, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(2)
                .withPeriodStartTime(DEFAULT_PERIOD_START_TIME)
                .build();
        Work work3 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_LINEAR, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(3)
                .withPeriodStartTime(DEFAULT_PERIOD_START_TIME)
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
        Work work = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_LINEAR, Work.MAX_BACKOFF_MILLIS + 1)
                .withInitialRunAttemptCount(1)
                .withPeriodStartTime(DEFAULT_PERIOD_START_TIME)
                .build();
        assertThat(work.getWorkSpec().calculateNextRunTime(),
                is(DEFAULT_PERIOD_START_TIME + Work.MAX_BACKOFF_MILLIS));
    }

    @Test
    @SmallTest
    public void testCalculateNextRunTime_rerunAttempt_exponential_upperBound() {
        Work work = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, Work.MAX_BACKOFF_MILLIS + 1)
                .withInitialRunAttemptCount(1)
                .withPeriodStartTime(DEFAULT_PERIOD_START_TIME)
                .build();
        assertThat(work.getWorkSpec().calculateNextRunTime(),
                is(DEFAULT_PERIOD_START_TIME + Work.MAX_BACKOFF_MILLIS));
    }
}
