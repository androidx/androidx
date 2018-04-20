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
package androidx.work.impl.constraints;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.impl.constraints.controllers.ConstraintController;
import androidx.work.impl.model.WorkSpec;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class WorkConstraintsTrackerTest {
    private static final List<String> TEST_WORKSPEC_IDS = new ArrayList<>();
    static {
        TEST_WORKSPEC_IDS.add("A");
        TEST_WORKSPEC_IDS.add("B");
        TEST_WORKSPEC_IDS.add("C");
    }

    private WorkConstraintsCallback mCallback = new WorkConstraintsCallback() {
        @Override
        public void onAllConstraintsMet(@NonNull List<String> workSpecIds) {
            mUnconstrainedWorkSpecIds = workSpecIds;
        }

        @Override
        public void onAllConstraintsNotMet(@NonNull List<String> workSpecIds) {
            mConstrainedWorkSpecIds = workSpecIds;
        }
    };

    private ConstraintController mMockController = mock(ConstraintController.class);
    private List<String> mUnconstrainedWorkSpecIds;
    private List<String> mConstrainedWorkSpecIds;
    private WorkConstraintsTracker mWorkConstraintsTracker;

    @Before
    public void setUp() {
        ConstraintController[] controllers = new ConstraintController[] {mMockController};
        mWorkConstraintsTracker = new WorkConstraintsTracker(mCallback, controllers);
    }

    @Test
    public void testReplace() {
        List<WorkSpec> emptyList = Collections.emptyList();
        mWorkConstraintsTracker.replace(emptyList);
        verify(mMockController).replace(emptyList);
    }

    @Test
    public void testReset() {
        mWorkConstraintsTracker.reset();
        verify(mMockController).reset();
    }

    @Test
    public void testOnConstraintMet_controllerInvoked() {
        mWorkConstraintsTracker.onConstraintMet(TEST_WORKSPEC_IDS);
        for (String id : TEST_WORKSPEC_IDS) {
            verify(mMockController).isWorkSpecConstrained(id);
        }
    }

    @Test
    public void testOnConstraintMet_allConstraintsMet() {
        when(mMockController.isWorkSpecConstrained(any(String.class))).thenReturn(false);
        mWorkConstraintsTracker.onConstraintMet(TEST_WORKSPEC_IDS);
        assertThat(mUnconstrainedWorkSpecIds, is(TEST_WORKSPEC_IDS));
    }

    @Test
    public void testOnConstraintMet_allConstraintsMet_subList() {
        when(mMockController.isWorkSpecConstrained(TEST_WORKSPEC_IDS.get(0))).thenReturn(true);
        when(mMockController.isWorkSpecConstrained(TEST_WORKSPEC_IDS.get(1))).thenReturn(false);
        when(mMockController.isWorkSpecConstrained(TEST_WORKSPEC_IDS.get(2))).thenReturn(false);
        mWorkConstraintsTracker.onConstraintMet(TEST_WORKSPEC_IDS);
        assertThat(mUnconstrainedWorkSpecIds,
                containsInAnyOrder(TEST_WORKSPEC_IDS.get(1), TEST_WORKSPEC_IDS.get(2)));
    }

    @Test
    public void testOnConstraintMet_allConstraintsNotMet() {
        when(mMockController.isWorkSpecConstrained(any(String.class))).thenReturn(true);
        mWorkConstraintsTracker.onConstraintMet(TEST_WORKSPEC_IDS);
        assertThat(mUnconstrainedWorkSpecIds, is(empty()));
    }

    @Test
    public void testOnConstraintNotMet() {
        mWorkConstraintsTracker.onConstraintNotMet(TEST_WORKSPEC_IDS);
        assertThat(mConstrainedWorkSpecIds, is(TEST_WORKSPEC_IDS));
    }
}
