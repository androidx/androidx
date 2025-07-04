/*
 * Copyright 2025 The Android Open Source Project
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

import android.view.MotionEvent;

import androidx.recyclerview.selection.testing.TestEvents;
import androidx.recyclerview.selection.testing.TestItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class PointerDragEventInterceptorTest implements OnDragInitiatedListener {
    private boolean mDragInitiated;

    private TestItemDetailsLookup mDetailsLookup;
    private RecyclerView mRecyclerView;
    private PointerDragEventInterceptor mPointerDragEventInterceptor;

    @Before
    public void setUp() {
        mDetailsLookup = new TestItemDetailsLookup();
        mRecyclerView = new RecyclerView(ApplicationProvider.getApplicationContext());
        mPointerDragEventInterceptor = new PointerDragEventInterceptor(mDetailsLookup, this, null);
    }

    @Override
    public boolean onDragInitiated(@NonNull MotionEvent e) {
        mDragInitiated = true;
        return true;
    }

    private void doTest(boolean inItemDragRegion, int deltaY, boolean expectDragInitiated) {
        mDragInitiated = false;
        mDetailsLookup.reset();
        mDetailsLookup.initAt(123456).setInItemDragRegion(inItemDragRegion);

        mPointerDragEventInterceptor.onInterceptTouchEvent(
                mRecyclerView,
                TestEvents.builder().down().mouse().primary().location(1, 2).build());

        mPointerDragEventInterceptor.onInterceptTouchEvent(
                mRecyclerView,
                TestEvents.builder().move().mouse().primary().location(1, 2 + deltaY).build());

        assertEquals(mDragInitiated, expectDragInitiated);
    }

    @Test
    public void testNotInItemDragRegion() {
        doTest(false, 999, false);
    }

    @Test
    public void testMoveButSameXY() {
        doTest(true, 0, false);
    }

    @Test
    public void testMoveFarAway() {
        doTest(true, 999, true);
    }
}
