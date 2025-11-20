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

package androidx.work.impl.utils;

import static androidx.work.impl.utils.IdGeneratorKt.INITIAL_ID;
import static androidx.work.impl.utils.IdGeneratorKt.NEXT_JOB_SCHEDULER_ID_KEY;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static java.lang.Integer.MAX_VALUE;

import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.work.DatabaseTest;
import androidx.work.impl.model.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class IdGeneratorTest extends DatabaseTest {

    private IdGenerator mIdGenerator;

    @Before
    public void setUp() {
        mIdGenerator = new IdGenerator(mDatabase);
    }

    @Test
    public void testNextId_returnsInitialIdWhenNoStoredNextId() {
        int nextId = mIdGenerator.nextJobSchedulerIdWithRange(INITIAL_ID, MAX_VALUE);
        assertThat(nextId, is(INITIAL_ID));
    }

    @Test
    public void testNextId_returnsStoredNextId() {
        int expectedId = 100;
        storeNextId(expectedId);
        int nextId = mIdGenerator.nextJobSchedulerIdWithRange(INITIAL_ID, MAX_VALUE);
        assertThat(nextId, is(expectedId));
    }

    @Test
    public void testNextId_returnsInitialIdAfterReturningMaxInteger() {
        int expectedId = MAX_VALUE;
        storeNextId(expectedId);
        int nextId = mIdGenerator.nextJobSchedulerIdWithRange(INITIAL_ID, MAX_VALUE);
        assertThat(nextId, is(MAX_VALUE));
        nextId = mIdGenerator.nextJobSchedulerIdWithRange(INITIAL_ID, MAX_VALUE);
        assertThat(nextId, is(INITIAL_ID));
    }

    @Test
    public void testNextId_belowMinRange() {
        storeNextId(2);
        assertThat(mIdGenerator.nextJobSchedulerIdWithRange(10, 100), is(10));
    }

    @Test
    public void testNextId_aboveMaxRange() {
        storeNextId(100);
        assertThat(mIdGenerator.nextJobSchedulerIdWithRange(10, 100), is(100));
    }

    @Test
    public void testNextId_aboveMaxRange2() {
        storeNextId(110);
        assertThat(mIdGenerator.nextJobSchedulerIdWithRange(10, 100), is(10));
    }

    @Test
    public void testNextId_withinRange() {
        storeNextId(20);
        assertThat(mIdGenerator.nextJobSchedulerIdWithRange(10, 100), is(20));
    }

    /**
     * Mocks setting a stored value in {@link SharedPreferences} for the next ID.
     *
     * @param nextId The next ID to store in {@link SharedPreferences}.
     */
    private void storeNextId(int nextId) {
        mDatabase.preferenceDao()
                .insertPreference(new Preference(NEXT_JOB_SCHEDULER_ID_KEY, Long.valueOf(nextId)));
    }
}
