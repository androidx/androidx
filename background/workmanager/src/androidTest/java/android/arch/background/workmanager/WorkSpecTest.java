/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import android.arch.background.workmanager.worker.InfiniteTestWorker;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class WorkSpecTest {
    private static final long DEFAULT_INITIAL_DELAY_TIME_MS = 5000L;
    private static final long DEFAULT_BACKOFF_DELAY_TIME_MS = 5000L;

    @Test
    @SmallTest
    public void testCalculateDelay_firstRunAttempt() {
        Work work = new Work.Builder(InfiniteTestWorker.class)
                .withInitialDelay(DEFAULT_INITIAL_DELAY_TIME_MS)
                .build();
        long actualDelay = work.getWorkSpec().calculateDelay();
        assertThat(actualDelay, is(DEFAULT_INITIAL_DELAY_TIME_MS));
    }

    @Test
    @SmallTest
    public void testCalculateDelay_rerunAttempt_exponential() {
        Work work1 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(1)
                .build();
        Work work2 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(2)
                .build();
        Work work3 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(3)
                .build();

        long delay1 = work1.getWorkSpec().calculateDelay();
        long delay2 = work2.getWorkSpec().calculateDelay();
        long delay3 = work3.getWorkSpec().calculateDelay();

        long delayDeltaBetweenWork2AndWork1 = delay2 - delay1;
        long delayDeltaBetweenWork3AndWork2 = delay3 - delay2;

        assertThat(delayDeltaBetweenWork3AndWork2, is(greaterThan(delayDeltaBetweenWork2AndWork1)));
    }

    @Test
    @SmallTest
    public void testCalculateDelay_rerunAttempt_linear() {
        Work work1 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_LINEAR, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(1)
                .build();
        Work work2 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_LINEAR, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(2)
                .build();
        Work work3 = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_LINEAR, DEFAULT_BACKOFF_DELAY_TIME_MS)
                .withInitialRunAttemptCount(3)
                .build();

        long delay1 = work1.getWorkSpec().calculateDelay();
        long delay2 = work2.getWorkSpec().calculateDelay();
        long delay3 = work3.getWorkSpec().calculateDelay();

        long delayDeltaBetweenWork2AndWork1 = delay2 - delay1;
        long delayDeltaBetweenWork3AndWork2 = delay3 - delay2;

        assertThat(delayDeltaBetweenWork2AndWork1, is(delayDeltaBetweenWork3AndWork2));
    }

    @Test
    @SmallTest
    public void testCalculateDelay_rerunAttempt_linear_upperBound() {
        Work work = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_LINEAR, Work.MAX_BACKOFF_MILLIS + 1)
                .withInitialRunAttemptCount(1)
                .build();
        assertThat(work.getWorkSpec().calculateDelay(), is(Work.MAX_BACKOFF_MILLIS));
    }

    @Test
    @SmallTest
    public void testCalculateDelay_rerunAttempt_exponential_upperBound() {
        Work work = new Work.Builder(InfiniteTestWorker.class)
                .withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, Work.MAX_BACKOFF_MILLIS + 1)
                .withInitialRunAttemptCount(1)
                .build();
        assertThat(work.getWorkSpec().calculateDelay(), is(Work.MAX_BACKOFF_MILLIS));
    }
}
