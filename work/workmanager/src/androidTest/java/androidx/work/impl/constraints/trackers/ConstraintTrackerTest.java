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
package androidx.work.impl.constraints.trackers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.work.impl.constraints.ConstraintListener;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ConstraintTrackerTest {

    private TestConstraintTracker mTracker;
    private Context mMockContext;

    @Before
    public void setUp() {
        mMockContext = mock(Context.class);
        when(mMockContext.getApplicationContext()).thenReturn(mMockContext);
        mTracker = new TestConstraintTracker(mMockContext);
    }

    @After
    public void tearDown() {
        try {
            mTracker.stopTracking();
        } catch (IllegalArgumentException e) {
            // Ignore any exceptions if the receiver isn't registered.
        }
    }

    @Test
    public void testAddListener_manyListeners_doesNotCallGetInitialStateAndStartTrackingTwice() {
        ConstraintListener<Boolean> constraintListener1 = mock(ConstraintListener.class);
        ConstraintListener<Boolean> constraintListener2 = mock(ConstraintListener.class);

        assertThat(mTracker.mIsTracking, is(false));
        assertThat(mTracker.mStartTrackingCount, is(0));
        assertThat(mTracker.mGetInitialStateCount, is(0));

        mTracker.addListener(constraintListener1);
        assertThat(mTracker.mIsTracking, is(true));
        assertThat(mTracker.mStartTrackingCount, is(1));
        assertThat(mTracker.mGetInitialStateCount, is(1));

        mTracker.addListener(constraintListener2);
        assertThat(mTracker.mIsTracking, is(true));
        assertThat(mTracker.mStartTrackingCount, is(1));
        assertThat(mTracker.mGetInitialStateCount, is(1));
    }

    @Test
    public void testAddListener_sameListener_doesNotCallGetInitialStateAndStartTrackingTwice() {
        ConstraintListener<Boolean> constraintListener = mock(ConstraintListener.class);

        assertThat(mTracker.mIsTracking, is(false));
        assertThat(mTracker.mStartTrackingCount, is(0));
        assertThat(mTracker.mGetInitialStateCount, is(0));

        mTracker.addListener(constraintListener);
        assertThat(mTracker.mIsTracking, is(true));
        assertThat(mTracker.mStartTrackingCount, is(1));
        assertThat(mTracker.mGetInitialStateCount, is(1));

        mTracker.addListener(constraintListener);
        assertThat(mTracker.mIsTracking, is(true));
        assertThat(mTracker.mStartTrackingCount, is(1));
        assertThat(mTracker.mGetInitialStateCount, is(1));
    }

    @Test
    public void testAddListener_notifiesAddedListener() {
        ConstraintListener<Boolean> constraintListener1 = mock(ConstraintListener.class);
        ConstraintListener<Boolean> constraintListener2 = mock(ConstraintListener.class);

        mTracker.mInitialState = true;
        mTracker.addListener(constraintListener1);
        verify(constraintListener1).onConstraintChanged(true);

        mTracker.addListener(constraintListener2);
        verify(constraintListener2).onConstraintChanged(true);
        verifyNoMoreInteractions(constraintListener1);
    }

    @Test
    public void testAddListener_constraintNotSet() {
        ConstraintListener<Boolean> constraintListener1 = mock(ConstraintListener.class);
        ConstraintListener<Boolean> constraintListener2 = mock(ConstraintListener.class);

        mTracker.addListener(constraintListener1);
        mTracker.addListener(constraintListener2);
        verify(constraintListener1).onConstraintChanged(null);
        verify(constraintListener2).onConstraintChanged(null);
    }

    @Test
    public void testSetState_newState_notifiesAllListeners() {
        ConstraintListener<Boolean> constraintListener1 = mock(ConstraintListener.class);
        ConstraintListener<Boolean> constraintListener2 = mock(ConstraintListener.class);

        mTracker.addListener(constraintListener1);
        mTracker.addListener(constraintListener2);
        verify(constraintListener1).onConstraintChanged(null);
        verify(constraintListener2).onConstraintChanged(null);

        mTracker.setState(true);
        verify(constraintListener1).onConstraintChanged(true);
        verify(constraintListener2).onConstraintChanged(true);
    }

    @Test
    public void testSetState_sameState_doesNotNotify() {
        ConstraintListener<Boolean> constraintListener1 = mock(ConstraintListener.class);
        ConstraintListener<Boolean> constraintListener2 = mock(ConstraintListener.class);

        mTracker.addListener(constraintListener1);
        mTracker.addListener(constraintListener2);
        verify(constraintListener1).onConstraintChanged(null);
        verify(constraintListener2).onConstraintChanged(null);

        mTracker.setState(false);
        verify(constraintListener1).onConstraintChanged(false);
        verify(constraintListener2).onConstraintChanged(false);

        mTracker.setState(false);
        verifyNoMoreInteractions(constraintListener1);
        verifyNoMoreInteractions(constraintListener2);
    }

    @Test
    public void testRemoveListener() {
        ConstraintListener<Boolean> constraintListener1 = mock(ConstraintListener.class);
        ConstraintListener<Boolean> constraintListener2 = mock(ConstraintListener.class);

        mTracker.addListener(constraintListener1);
        mTracker.addListener(constraintListener2);

        mTracker.removeListener(constraintListener1);
        assertThat(mTracker.mIsTracking, is(true));
        assertThat(mTracker.mStopTrackingCount, is(0));

        mTracker.removeListener(constraintListener2);
        assertThat(mTracker.mIsTracking, is(false));
        assertThat(mTracker.mStopTrackingCount, is(1));
    }

    private static class TestConstraintTracker extends ConstraintTracker<Boolean> {
        boolean mIsTracking;
        int mGetInitialStateCount;
        int mStartTrackingCount;
        int mStopTrackingCount;
        Boolean mInitialState = null;

        TestConstraintTracker(Context context) {
            super(context);
        }

        @Override
        public Boolean getInitialState() {
            mGetInitialStateCount++;
            return mInitialState;
        }

        @Override
        public void startTracking() {
            mIsTracking = true;
            mStartTrackingCount++;
        }

        @Override
        public void stopTracking() {
            mIsTracking = false;
            mStopTrackingCount++;
        }
    }
}
