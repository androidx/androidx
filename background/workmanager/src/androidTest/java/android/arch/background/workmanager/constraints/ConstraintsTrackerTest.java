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
package android.arch.background.workmanager.constraints;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.arch.background.workmanager.constraints.controllers.ConstraintController;
import android.arch.background.workmanager.model.WorkSpec;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ConstraintsTrackerTest {
    private static final List<WorkSpec> TEST_WORKSPECS = Arrays.asList(
            new WorkSpec("A"), new WorkSpec("B"), new WorkSpec("C")
    );

    private ConstraintsMetCallback mCallback = new ConstraintsMetCallback() {
        @Override
        public void onAllConstraintsMet(List<WorkSpec> workSpecs) {
            mUnconstrainedWorkSpecs = workSpecs;
        }

        @Override
        public void onAllConstraintsNotMet(List<WorkSpec> workSpecs) {
            mConstrainedWorkSpecs = workSpecs;
        }
    };

    private ConstraintController mMockController = mock(ConstraintController.class);
    private List<WorkSpec> mUnconstrainedWorkSpecs;
    private List<WorkSpec> mConstrainedWorkSpecs;
    private ConstraintsTracker mConstraintsTracker;

    @Before
    public void setUp() {
        ConstraintController[] controllers = new ConstraintController[] {mMockController};
        mConstraintsTracker = new ConstraintsTracker(mCallback, controllers);
    }

    @Test
    public void testOnConstraintMet_controllerInvoked() {
        mConstraintsTracker.onConstraintMet(TEST_WORKSPECS);
        verify(mMockController).isWorkSpecConstrained(TEST_WORKSPECS.get(0));
        verify(mMockController).isWorkSpecConstrained(TEST_WORKSPECS.get(1));
        verify(mMockController).isWorkSpecConstrained(TEST_WORKSPECS.get(2));
    }

    @Test
    public void testOnConstraintMet_allConstraintsMet() {
        when(mMockController.isWorkSpecConstrained(any(WorkSpec.class))).thenReturn(false);
        mConstraintsTracker.onConstraintMet(TEST_WORKSPECS);
        assertThat(mUnconstrainedWorkSpecs, is(TEST_WORKSPECS));
    }

    @Test
    public void testOnConstraintMet_allConstraintsMet_subList() {
        when(mMockController.isWorkSpecConstrained(TEST_WORKSPECS.get(0))).thenReturn(true);
        when(mMockController.isWorkSpecConstrained(TEST_WORKSPECS.get(1))).thenReturn(false);
        when(mMockController.isWorkSpecConstrained(TEST_WORKSPECS.get(2))).thenReturn(false);
        mConstraintsTracker.onConstraintMet(TEST_WORKSPECS);
        assertThat(mUnconstrainedWorkSpecs,
                containsInAnyOrder(TEST_WORKSPECS.get(1), TEST_WORKSPECS.get(2)));
    }

    @Test
    public void testOnConstraintMet_allConstraintsNotMet() {
        when(mMockController.isWorkSpecConstrained(any(WorkSpec.class))).thenReturn(true);
        mConstraintsTracker.onConstraintMet(TEST_WORKSPECS);
        assertThat(mUnconstrainedWorkSpecs, is(empty()));
    }

    @Test
    public void testOnConstraintNotMet() {
        mConstraintsTracker.onConstraintNotMet(TEST_WORKSPECS);
        assertThat(mConstrainedWorkSpecs, is(TEST_WORKSPECS));
    }
}
