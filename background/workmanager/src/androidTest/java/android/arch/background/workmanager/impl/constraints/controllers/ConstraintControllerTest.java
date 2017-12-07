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

package android.arch.background.workmanager.impl.constraints.controllers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import android.arch.background.workmanager.TestLifecycleOwner;
import android.arch.background.workmanager.impl.constraints.trackers.ConstraintTracker;
import android.arch.background.workmanager.impl.model.WorkSpec;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ConstraintControllerTest {

    private TestConstraintController mController;
    private LiveData<List<WorkSpec>> mMockLiveData = mock(LiveData.class);
    private TestLifecycleOwner mLifecycleOwner = new TestLifecycleOwner();
    private ConstraintTracker<Boolean> mMockTracker = mock(ConstraintTracker.class);
    private ConstraintController.OnConstraintUpdatedCallback mCallback = mock(
            ConstraintController.OnConstraintUpdatedCallback.class);

    @Before
    public void setUp() {
        mController = new TestConstraintController(
                mMockLiveData, mLifecycleOwner, mMockTracker, mCallback);
    }

    @Test
    @SmallTest
    public void testLiveData_workSpecs() {
        List<WorkSpec> workSpecs = Collections.emptyList();
        mController.onChanged(workSpecs);
        verify(mMockTracker).removeListener(mController);

        workSpecs = Collections.singletonList(new WorkSpec("TEST"));
        mController.onChanged(workSpecs);
        verify(mMockTracker).addListener(mController);

        workSpecs = Arrays.asList(new WorkSpec("TEST1"), new WorkSpec("TEST2"));
        mController.onChanged(workSpecs);
        verify(mMockTracker, times(2)).addListener(mController);

        workSpecs = Collections.emptyList();
        mController.onChanged(workSpecs);
        verify(mMockTracker, times(2)).removeListener(mController);
    }

    @Test
    @SmallTest
    public void testCallback_constraintSet_noWorkSpecs() {
        mController.onConstraintChanged(true);
        verifyNoMoreInteractions(mCallback);
    }

    @Test
    @SmallTest
    public void testCallback_workSpecsUpdated_constraintNotSet() {
        List<WorkSpec> workSpecs = Collections.singletonList(new WorkSpec("TEST"));
        mController.onChanged(workSpecs);
        verify(mCallback).onConstraintNotMet(workSpecs);
    }

    @Test
    @SmallTest
    public void testCallback_isConstrained() {
        mController.mIsConstrained = true;
        List<WorkSpec> workSpecs = Collections.singletonList(new WorkSpec("TEST"));
        mController.onChanged(workSpecs);
        verify(mCallback).onConstraintNotMet(workSpecs);
        mController.onConstraintChanged(true);
        verify(mCallback, times(2)).onConstraintNotMet(workSpecs);
    }

    @Test
    @SmallTest
    public void testCallback_isNotConstrained() {
        mController.mIsConstrained = false;
        List<WorkSpec> workSpecs = Collections.singletonList(new WorkSpec("TEST"));
        mController.onChanged(workSpecs);
        mController.onConstraintChanged(true);
        verify(mCallback).onConstraintMet(workSpecs);
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_workSpecsUpdated_constraintNotSet() {
        WorkSpec workSpec = new WorkSpec("TEST");
        List<WorkSpec> workSpecs = Collections.singletonList(workSpec);
        mController.onChanged(workSpecs);
        assertThat(mController.isWorkSpecConstrained(workSpec), is(true));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_constraintSet_noWorkSpecs() {
        WorkSpec workSpec = new WorkSpec("TEST");
        mController.onConstraintChanged(true);
        assertThat(mController.isWorkSpecConstrained(workSpec), is(true));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_isConstrained() {
        WorkSpec workSpec = new WorkSpec("TEST");
        mController.mIsConstrained = true;
        List<WorkSpec> workSpecs = Collections.singletonList(workSpec);
        mController.onChanged(workSpecs);
        mController.onConstraintChanged(true);
        assertThat(mController.isWorkSpecConstrained(workSpec), is(true));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_isNotConstrained() {
        WorkSpec workSpec = new WorkSpec("TEST");
        mController.mIsConstrained = false;
        List<WorkSpec> workSpecs = Collections.singletonList(workSpec);
        mController.onChanged(workSpecs);
        mController.onConstraintChanged(true);
        assertThat(mController.isWorkSpecConstrained(workSpec), is(false));
    }

    private static class TestConstraintController extends ConstraintController<Boolean> {
        private boolean mIsConstrained = false;

        TestConstraintController(LiveData<List<WorkSpec>> constraintLiveData,
                LifecycleOwner lifecycleOwner,
                ConstraintTracker<Boolean> tracker,
                OnConstraintUpdatedCallback onConstraintUpdatedCallback) {
            super(constraintLiveData, lifecycleOwner, tracker, onConstraintUpdatedCallback);
        }

        @Override
        boolean isConstrained(@NonNull Boolean currentValue) {
            return mIsConstrained;
        }
    }
}
