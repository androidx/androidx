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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.support.annotation.NonNull;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManagerTest;
import androidx.work.impl.constraints.trackers.ConstraintTracker;
import androidx.work.impl.model.WorkSpec;
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
    private ConstraintTracker<Boolean> mMockTracker = mock(ConstraintTracker.class);
    private ConstraintController.OnConstraintUpdatedCallback mCallback =
            mock(ConstraintController.OnConstraintUpdatedCallback.class);

    @Before
    public void setUp() {
        mTestIdleController = new TestDeviceIdleConstraintController(mMockTracker, mCallback);
    }

    private WorkSpec createTestWorkSpec(Constraints constraints) {
        return getWorkSpec(new OneTimeWorkRequest.Builder(TestWorker.class)
                .setConstraints(constraints)
                .build());
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
        mTestIdleController.replace(Collections.<WorkSpec>emptyList());
        verify(mMockTracker).removeListener(mTestIdleController);
        verifyZeroInteractions(mCallback);
    }

    @Test
    @SmallTest
    public void testReplace_workSpecNoConstraints() {
        WorkSpec workSpecNoConstraints = createNoConstraintWorkSpec();
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecNoConstraints);
        mTestIdleController.replace(workSpecs);
        verify(mMockTracker).removeListener(mTestIdleController);
        verifyZeroInteractions(mCallback);
    }

    @Test
    @SmallTest
    public void testReplace_workSpecWithConstraint_constrained() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<String> expectedWorkIds = Collections.singletonList(workSpecWithConstraint.id);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);

        mTestIdleController.setDeviceActive();
        mTestIdleController.replace(workSpecs);
        verify(mMockTracker).addListener(mTestIdleController);
        verify(mCallback).onConstraintNotMet(eq(expectedWorkIds));
    }

    @Test
    @SmallTest
    public void testReplace_workSpecWithConstraint_unconstrained() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<String> expectedWorkIds = Collections.singletonList(workSpecWithConstraint.id);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);

        mTestIdleController.setDeviceIdle();
        mTestIdleController.replace(workSpecs);
        verify(mMockTracker).addListener(mTestIdleController);
        verify(mCallback).onConstraintMet(eq(expectedWorkIds));
    }

    @Test
    @SmallTest
    public void testReplace_workSpecWithConstraint_constraintNotSet() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<String> expectedWorkIds = Collections.singletonList(workSpecWithConstraint.id);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);

        mTestIdleController.replace(workSpecs);
        verify(mCallback).onConstraintNotMet(expectedWorkIds);
    }

    @Test
    @SmallTest
    public void testReset_alreadyNoMatchingWorkSpecs() {
        mTestIdleController.reset();
        verifyZeroInteractions(mMockTracker);
    }

    @Test
    @SmallTest
    public void testReset_withMatchingWorkSpecs() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);
        mTestIdleController.replace(workSpecs);

        mTestIdleController.reset();
        verify(mMockTracker).removeListener(mTestIdleController);
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
        List<String> expectedWorkIds = Collections.singletonList(workSpecWithConstraint.id);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);
        mTestIdleController.replace(workSpecs);
        verify(mCallback).onConstraintNotMet(expectedWorkIds);

        final boolean deviceIdle = false;
        mTestIdleController.onConstraintChanged(deviceIdle);
        verify(mCallback, times(2)).onConstraintNotMet(expectedWorkIds);
    }

    @Test
    @SmallTest
    public void testOnConstraintChanged_toUnconstrained_withMatchingWorkSpecs() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        List<String> expectedWorkIds = Collections.singletonList(workSpecWithConstraint.id);
        List<WorkSpec> workSpecs = Collections.singletonList(workSpecWithConstraint);
        mTestIdleController.replace(workSpecs);

        final boolean deviceIdle = true;
        mTestIdleController.onConstraintChanged(deviceIdle);
        verify(mCallback).onConstraintMet(expectedWorkIds);
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
    public void testIsWorkSpecConstrained_constraintNotSet() {
        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        mTestIdleController.replace(Collections.singletonList(workSpecWithConstraint));
        assertThat(mTestIdleController.isWorkSpecConstrained(workSpecWithConstraint.id),
                is(false));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_constrained_withMatchingWorkSpecs() {
        mTestIdleController.setDeviceActive();

        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        mTestIdleController.replace(Collections.singletonList(workSpecWithConstraint));
        assertThat(mTestIdleController.isWorkSpecConstrained(workSpecWithConstraint.id),
                is(true));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_constrained_noMatchingWorkSpecs() {
        mTestIdleController.setDeviceActive();

        WorkSpec workSpecNoConstraints = createNoConstraintWorkSpec();
        mTestIdleController.replace(Collections.singletonList(workSpecNoConstraints));
        assertThat(mTestIdleController.isWorkSpecConstrained(workSpecNoConstraints.id),
                is(false));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_unconstrained_withMatchingWorkSpecs() {
        mTestIdleController.setDeviceIdle();

        WorkSpec workSpecWithConstraint = createTestConstraintWorkSpec();
        mTestIdleController.replace(Collections.singletonList(workSpecWithConstraint));
        assertThat(mTestIdleController.isWorkSpecConstrained(workSpecWithConstraint.id),
                is(false));
    }

    @Test
    @SmallTest
    public void testIsWorkSpecConstrained_unconstrained_noMatchingWorkSpecs() {
        mTestIdleController.setDeviceIdle();

        WorkSpec workSpecNoConstraints = createNoConstraintWorkSpec();
        mTestIdleController.replace(Collections.singletonList(workSpecNoConstraints));
        assertThat(mTestIdleController.isWorkSpecConstrained(workSpecNoConstraints.id),
                is(false));
    }

    private static class TestDeviceIdleConstraintController extends ConstraintController<Boolean> {
        TestDeviceIdleConstraintController(ConstraintTracker<Boolean> tracker,
                OnConstraintUpdatedCallback callback) {
            super(tracker, callback);
        }

        @Override
        boolean hasConstraint(@NonNull WorkSpec workSpec) {
            return workSpec.constraints.requiresDeviceIdle();
        }

        @Override
        boolean isConstrained(@NonNull Boolean isDeviceIdle) {
            return !isDeviceIdle;
        }

        void setDeviceActive() {
            onConstraintChanged(false);
        }

        void setDeviceIdle() {
            onConstraintChanged(true);
        }
    }
}
