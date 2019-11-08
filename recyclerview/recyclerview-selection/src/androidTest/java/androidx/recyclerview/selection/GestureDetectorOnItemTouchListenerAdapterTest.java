/*
 * Copyright 2019 The Android Open Source Project
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.recyclerview.selection.testing.TestEvents;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(AndroidJUnit4.class)
@SmallTest
public final class GestureDetectorOnItemTouchListenerAdapterTest {

    private GestureDetectorOnItemTouchListenerAdapter mAdapter;
    private GestureDetector mDetector;
    private TestOnGestureListener mListener;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mListener = new TestOnGestureListener();
        mDetector = new GestureDetector(ApplicationProvider.getApplicationContext(), mListener);
        mAdapter = new GestureDetectorOnItemTouchListenerAdapter(mDetector);
    }

    @Test
    public void testReflectsGestureDetectorReturnValue() {
        assertFalse(mDetector.onTouchEvent(TestEvents.Mouse.SECONDARY_CLICK));
        assertFalse(mAdapter.onInterceptTouchEvent(null, TestEvents.Mouse.SECONDARY_CLICK));

        mListener.mReturnValue = true;
        assertTrue(mDetector.onTouchEvent(TestEvents.Mouse.SECONDARY_CLICK));
        assertTrue(mAdapter.onInterceptTouchEvent(null, TestEvents.Mouse.SECONDARY_CLICK));
    }

    private static class TestOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        boolean mReturnValue;

        @Override
        public boolean onDown(MotionEvent e) {
            return mReturnValue;
        }
    }
}
