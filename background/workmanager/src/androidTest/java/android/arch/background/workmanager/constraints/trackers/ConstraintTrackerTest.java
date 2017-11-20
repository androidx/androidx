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
package android.arch.background.workmanager.constraints.trackers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import android.arch.background.workmanager.constraints.listeners.ConstraintListener;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class ConstraintTrackerTest {

    private TestConstraintTracker mTracker;

    @Before
    public void setUp() {
        mTracker = new TestConstraintTracker(InstrumentationRegistry.getTargetContext());
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
    public void testAddListener_firstListener_setsInitialState() {
        ConstraintListener constraintListener = mock(ConstraintListener.class);
        mTracker.addListener(constraintListener);
        assertThat(mTracker.mStateInitialized, is(true));
    }

    @Test
    public void testAddListener_manyListeners_startsTrackingOnlyAfterFirstAdded() {
        ConstraintListener constraintListener1 = mock(ConstraintListener.class);
        ConstraintListener constraintListener2 = mock(ConstraintListener.class);

        mTracker.addListener(constraintListener1);
        assertThat(mTracker.mIsTracking, is(true));
        assertThat(mTracker.mStartTrackingCount, is(1));

        mTracker.addListener(constraintListener2);
        assertThat(mTracker.mIsTracking, is(true));
        assertThat(mTracker.mStartTrackingCount, is(1));
    }

    @Test
    public void testAddListener_sameListener_doesNotStartTrackingTwice() {
        ConstraintListener constraintListener = mock(ConstraintListener.class);
        for (int i = 0; i < 2; ++i) {
            mTracker.addListener(constraintListener);
            assertThat(mTracker.mListeners.size(), is(1));
            assertThat(mTracker.mListeners, contains(constraintListener));
        }
    }

    @Test
    public void testAddListener_manyListeners_notifiesAllListeners() {
        ConstraintListener[] constraintListeners = new ConstraintListener[] {
                mock(ConstraintListener.class),
                mock(ConstraintListener.class),
                mock(ConstraintListener.class)
        };

        for (ConstraintListener listener : constraintListeners) {
            assertThat(wasListenerNotified(listener), is(false));
        }

        for (ConstraintListener listener : constraintListeners) {
            mTracker.addListener(listener);
            assertThat(wasListenerNotified(listener), is(true));
        }
    }

    private boolean wasListenerNotified(@NonNull ConstraintListener listener) {
        return mTracker.mNotifiedListeners.contains(listener);
    }

    @Test
    public void testRemoveListener_lastListener_stopsTracking() {
        ConstraintListener constraintListener = mock(ConstraintListener.class);
        mTracker.addListener(constraintListener);

        mTracker.removeListener(constraintListener);
        assertThat(mTracker.mIsTracking, is(false));
        assertThat(mTracker.mStopTrackingCount, is(1));
    }

    @Test
    public void testRemoveListener_notLastListener_doesNotStopTracking() {
        ConstraintListener constraintListener1 = mock(ConstraintListener.class);
        ConstraintListener constraintListener2 = mock(ConstraintListener.class);

        mTracker.addListener(constraintListener1);
        mTracker.addListener(constraintListener2);

        mTracker.removeListener(constraintListener1);
        assertThat(mTracker.mIsTracking, is(true));
        assertThat(mTracker.mStopTrackingCount, is(0));
    }

    private static class TestConstraintTracker extends ConstraintTracker<ConstraintListener> {

        final Set<ConstraintListener> mNotifiedListeners = new HashSet<>();

        boolean mStateInitialized;
        boolean mIsTracking;
        int mStartTrackingCount;
        int mStopTrackingCount;

        TestConstraintTracker(Context context) {
            super(context);
        }

        @Override
        public void initState() {
            mStateInitialized = true;
        }

        @Override
        public void notifyListener(@NonNull ConstraintListener listener) {
            mNotifiedListeners.add(listener);
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
