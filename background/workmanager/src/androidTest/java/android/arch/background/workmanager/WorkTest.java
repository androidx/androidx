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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class WorkTest {
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
        Work work = mBuilder.withInitialDelay(expectedInitialDelay).build();
        assertThat(work.getWorkSpec().getInitialDelay(), is(expectedInitialDelay));
    }

    @Test
    public void testBuild_setBackoffCriteria_exceedMaxBackoffDuration() {
        final long backoffDuration = Work.MAX_BACKOFF_MILLIS + 123L;
        Work work = mBuilder
                .withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, backoffDuration)
                .build();
        assertThat(work.getWorkSpec().getBackoffDelayDuration(), is(Work.MAX_BACKOFF_MILLIS));
    }

    @Test
    public void testBuild_setBackoffCriteria_lessThanMinBackoffDuration() {
        final long backoffDuration = Work.MIN_BACKOFF_MILLIS - 123L;
        Work work = mBuilder
                .withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, backoffDuration)
                .build();
        assertThat(work.getWorkSpec().getBackoffDelayDuration(), is(Work.MIN_BACKOFF_MILLIS));
    }

    @Test
    public void testBuild_backoffAndIdleMode_throwsIllegalArgumentException() {
        mThrown.expect(IllegalArgumentException.class);
        Constraints constraints = new Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .build();
        mBuilder.withBackoffCriteria(Work.BACKOFF_POLICY_EXPONENTIAL, 123L)
                .withConstraints(constraints)
                .build();
    }
}
