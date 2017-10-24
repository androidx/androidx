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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.background.workmanager.worker.TestWorker;
import android.support.test.filters.SmallTest;

import org.junit.Test;

@SmallTest
public class PeriodicWorkTest {

    private static final long TEST_INTERVAL_DURATION =
            PeriodicWork.MIN_PERIODIC_INTERVAL_DURATION + 123L;
    private static final long TEST_FLEX_DURATION = PeriodicWork.MIN_PERIODIC_FLEX_DURATION + 321L;

    @Test
    public void testBuild_setPeriodic_onlyIntervalDuration() {
        PeriodicWork periodicWork =
                new PeriodicWork.Builder(TestWorker.class, TEST_INTERVAL_DURATION).build();
        assertThat(periodicWork.getWorkSpec().getIntervalDuration(), is(TEST_INTERVAL_DURATION));
    }

    @Test
    public void testBuild_setPeriodic_intervalAndFlexDurations() {
        PeriodicWork periodicWork =
                new PeriodicWork.Builder(
                        TestWorker.class,
                        TEST_INTERVAL_DURATION,
                        TEST_FLEX_DURATION)
                        .build();
        assertThat(periodicWork.getWorkSpec().getIntervalDuration(), is(TEST_INTERVAL_DURATION));
        assertThat(periodicWork.getWorkSpec().getFlexDuration(), is(TEST_FLEX_DURATION));
    }
}
