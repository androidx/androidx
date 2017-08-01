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


package android.arch.persistence.room;


import static android.arch.persistence.room.InvalidationTracker.ObservedTableTracker.ADD;
import static android.arch.persistence.room.InvalidationTracker.ObservedTableTracker.NO_OP;
import static android.arch.persistence.room.InvalidationTracker.ObservedTableTracker.REMOVE;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;

@RunWith(JUnit4.class)
public class ObservedTableTrackerTest {
    private static final int TABLE_COUNT = 5;
    private InvalidationTracker.ObservedTableTracker mTracker;

    @Before
    public void setup() {
        mTracker = new InvalidationTracker.ObservedTableTracker(TABLE_COUNT);
    }

    @Test
    public void basicAdd() {
        mTracker.onAdded(2, 3);
        assertThat(mTracker.getTablesToSync(), is(createResponse(2, ADD, 3, ADD)));
    }

    @Test
    public void basicRemove() {
        initState(2, 3);
        mTracker.onRemoved(3);
        assertThat(mTracker.getTablesToSync(), is(createResponse(3, REMOVE)));
    }

    @Test
    public void noChange() {
        initState(1, 3);
        mTracker.onAdded(3);
        assertThat(mTracker.getTablesToSync(), is(nullValue()));
    }

    @Test
    public void returnNullUntilSync() {
        initState(1, 3);
        mTracker.onAdded(4);
        assertThat(mTracker.getTablesToSync(), is(createResponse(4, ADD)));
        mTracker.onAdded(0);
        assertThat(mTracker.getTablesToSync(), is(nullValue()));
        mTracker.onSyncCompleted();
        assertThat(mTracker.getTablesToSync(), is(createResponse(0, ADD)));
    }

    @Test
    public void multipleAdditionsDeletions() {
        initState(2, 4);
        mTracker.onAdded(2);
        assertThat(mTracker.getTablesToSync(), is(nullValue()));
        mTracker.onAdded(2, 4);
        assertThat(mTracker.getTablesToSync(), is(nullValue()));
        mTracker.onRemoved(2);
        assertThat(mTracker.getTablesToSync(), is(nullValue()));
        mTracker.onRemoved(2, 4);
        assertThat(mTracker.getTablesToSync(), is(nullValue()));
        mTracker.onAdded(1, 3);
        mTracker.onRemoved(2, 4);
        assertThat(mTracker.getTablesToSync(), is(
                createResponse(1, ADD, 2, REMOVE, 3, ADD, 4, REMOVE)));
    }

    private void initState(int... tableIds) {
        mTracker.onAdded(tableIds);
        mTracker.getTablesToSync();
        mTracker.onSyncCompleted();
    }

    private static int[] createResponse(int... tuples) {
        int[] result = new int[TABLE_COUNT];
        Arrays.fill(result, NO_OP);
        for (int i = 0; i < tuples.length; i += 2) {
            result[tuples[i]] = tuples[i + 1];
        }
        return result;
    }
}
