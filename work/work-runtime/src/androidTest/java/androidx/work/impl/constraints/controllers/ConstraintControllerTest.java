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

package androidx.work.impl.constraints.controllers;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManagerTest;
import androidx.work.impl.constraints.trackers.ConstraintTracker;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.taskexecutor.InstantWorkTaskExecutor;
import androidx.work.worker.TestWorker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 23)
public class ConstraintControllerTest extends WorkManagerTest {
    private TestDeviceIdleConstraintController mTestIdleController;

    @SuppressWarnings("unchecked")
    private final FakeConstraintTracker mTracker = new FakeConstraintTracker();

    private ConstraintController.OnConstraintUpdatedCallback mCallback =
            mock(ConstraintController.OnConstraintUpdatedCallback.class);

    @Before
    public void setUp() {
        mTestIdleController = new TestDeviceIdleConstraintController(mTracker);
        mTestIdleController.setCallback(mCallback);
    }

    private WorkSpec createTestWorkSpec(Constraints constraints) {
        return new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(constraints)
                .build().getWorkSpec();
    }

    private WorkSpec createTestConstraintWorkSpec() {
        Constraints mConstraintsWithTestConstraint = new Constraints.Builder()
                .setRequiresDeviceIdle(true)
                .build();

        return createTestWorkSpec(mConstraintsWithTestConstraint);
    }

    private WorkSpec createNoConstraintWorkSpec() {
        return createTestWorkSpec(Constraints.NONE);
    }

    @Test
    @SmallTest
    public void testReplace_empty() {
        mTestIdleController.replace(Collections.emptyList());
        assertThat(mTracker.mTracking, is(false));
        verifyZeroInteractions(mCallback);
    }

    @Test
    @SmallTest
    public void testReplace_workSpecNoConstraints() {
        WorkSpec workSpecNoConstraints = createNoConstraintWorkSpec();
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecNoConstraints);
        mTestIdleController.replace(workSpecs);
        assertThat(mTracker.mTracking, is(false));
        verifyZeroInteractions(mCallback);
    }

    @Test
    @SmallTest
    public void testReplace_workSpecWithConstraint_constrained() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);

        mTracker.setDeviceActive();
        mTestIdleController.replace(workSpecs);
        assertThat(mTracker.mTracking, is(true));
        verify(mCallback, atLeastOnce()).onConstraintNotMet(eq(workSpecs));
    }

    @Test
    @SmallTest
    public void testReplace_workSpecWithConstraint_unconstrained() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);

        mTracker.setDeviceIdle();
        mTestIdleController.replace(workSpecs);
        assertThat(mTracker.mTracking, is(true));
        // called twice: replace calls updateCallback explicitly and
        // tracker.addListener results in updateCallback too
        // probably should be fixed eventually
        verify(mCallback, atLeastOnce()).onConstraintMet(eq(workSpecs));
    }

    @Test
    @SmallTest
    public void testReplace_workSpecWithConstraint_constraintNotSet() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);

        mTestIdleController.replace(workSpecs);
        // called twice: replace calls updateCallback explictily and
        // tracker.addListener results in updateCallback too
        // probably should be fixed eventually
        verify(mCallback, atLeastOnce()).onConstraintNotMet(workSpecs);
    }

    @Test
    @SmallTest
    public void testReset_alreadyNoMatchingWorkSpecs() {
        mTestIdleController.reset();
        assertThat(mTracker.mTracking, is(false));
    }

    @Test
    @SmallTest
    public void testReset_withMatchingWorkSpecs() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);
        mTestIdleController.replace(workSpecs);

        mTestIdleController.reset();
        assertThat(mTracker.mTracking, is(false));
    }

    @Test
    @SmallTest
    public void testOnConstraintChanged_noMatchingWorkSpecs() {
        mTestIdleController.onConstraintChanged(true);
        verifyZeroInteractions(mCallback);
    }

    @Test
    @SmallTest
    public void testOnConstraintChanged_toConstrained_withMatchingWorkSpecs() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);
        mTestIdleController.replace(workSpecs);
        verify(mCallback, times(2)).onConstraintNotMet(workSpecs);

        final boolean deviceIdle = false;
        mTestIdleController.onConstraintChanged(deviceIdle);
        verify(mCallback, times(3)).onConstraintNotMet(workSpecs);
    }

    @Test
    @SmallTest
    public void testOnConstraintChanged_toUnconstrained_withMatchingWorkSpecs() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);
        mTestIdleController.replace(workSpecs);

        final boolean deviceIdle = true;
        mTestIdleController.onConstraintChanged(deviceIdle);
        verify(mCallback).onConstraintMet(workSpecs);
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_noMatchingWorkSpecs() {
        WorkSpec workSpecNoConstraints = createNoConstraintWorkSpec();
        mTestIdleController.replace(Collections.singletonList(workSpecNoConstraints));
        assertThat(mTestIdleController.isWorkSpecConstrained(workSpecNoConstraints.id),
                is(false));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_constrained_withMatchingWorkSpecs() {
        mTracker.setDeviceActive();

        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        mTestIdleController.replace(Collections.singletonList(workSpecWithConstraint));
        assertThat(mTestIdleController.isWorkSpecConstrained(workSpecWithConstraint.id),
                is(true));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_constrained_noMatchingWorkSpecs() {
        mTracker.setDeviceActive();

        WorkSpec workSpecNoConstraints = createNoConstraintWorkSpec();
        mTestIdleController.replace(Collections.singletonList(workSpecNoConstraints));
        assertThat(mTestIdleController.isWorkSpecConstrained(workSpecNoConstraints.id),
                is(false));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_unconstrained_withMatchingWorkSpecs() {
        mTracker.setDeviceIdle();

        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        mTestIdleController.replace(Collections.singletonList(workSpecWithConstraint));
        assertThat(mTestIdleController.isWorkSpecConstrained(workSpecWithConstraint.id),
                is(false));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_unconstrained_noMatchingWorkSpecs() {
        mTracker.setDeviceIdle();

        WorkSpec workSpecNoConstraints = createNoConstraintWorkSpec();
        mTestIdleController.replace(Collections.singletonList(workSpecNoConstraints));
        assertThat(mTestIdleController.isWorkSpecConstrained(workSpecNoConstraints.id),
                is(false));
    }

    private static class TestDeviceIdleConstraintController extends ConstraintController<Boolean> {
        TestDeviceIdleConstraintController(ConstraintTracker<Boolean> tracker) {
            super(tracker);
        }

        @Override
        public boolean hasConstraint(@NonNull WorkSpec workSpec) {
            return workSpec.constraints.requiresDeviceIdle();
        }

        @Override
        public boolean isConstrained(@NonNull Boolean isDeviceIdle) {
            return !isDeviceIdle;
        }
    }

    private static class FakeConstraintTracker extends ConstraintTracker<Boolean> {
        public boolean mTracking;
        private boolean mInitialState;

        protected FakeConstraintTracker() {
            super(ApplicationProvider.getApplicationContext(), new InstantWorkTaskExecutor());
        }

        @Override
        public Boolean getInitialState() {
            return mInitialState;
        }

        @Override
        public void startTracking() {
            mTracking = true;
        }

        @Override
        public void stopTracking() {
            mTracking = false;
        }

        void setDeviceActive() {
            mInitialState = false;
        }

        void setDeviceIdle() {
            mInitialState = true;
        }
    }
}
