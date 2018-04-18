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

package androidx.recyclerview.selection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class OperationMonitorTest {

    private OperationMonitor mMonitor;
    private TestListener mListener;

    @Before
    public void setUp() {
        mMonitor = new OperationMonitor();
        mListener = new TestListener(mMonitor);
    }

    @Test
    public void testNotStarted() {
        assertFalse(mMonitor.isStarted());
    }

    @Test
    public void testStarted() {
        mMonitor.start();
        assertTrue(mMonitor.isStarted());
    }

    @Test
    public void testStopped() {
        mMonitor.start();
        mMonitor.stop();
        assertFalse(mMonitor.isStarted());
    }

    @Test
    public void testStartedCallsListener() {
        mMonitor.addListener(mListener);
        mMonitor.start();
        mListener.assertLastState(true);
        mMonitor.stop();
        mListener.assertLastState(false);
    }

    @Test
    public void testRemoveListener() {
        mMonitor.addListener(mListener);
        mMonitor.removeListener(mListener);
        mMonitor.start();
        mListener.assertLastState(false);
    }

    private static final class TestListener implements OperationMonitor.OnChangeListener {

        private boolean mLastState;
        private OperationMonitor mMonitor;

        TestListener(OperationMonitor monitor) {
            mMonitor = monitor;
        }

        @Override
        public void onChanged() {
            mLastState = mMonitor.isStarted();
        }

        void assertLastState(boolean expected) {
            assertEquals(expected, mLastState);
        }
    }
}
