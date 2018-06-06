/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.recyclerview.widget;


import static androidx.recyclerview.widget.RecyclerView.EdgeEffectFactory;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.EdgeEffect;

import androidx.annotation.NonNull;
import androidx.core.view.InputDeviceCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests custom edge effect are properly applied when scrolling.
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
public class CustomEdgeEffectTest extends BaseRecyclerViewInstrumentationTest {

    private static final int NUM_ITEMS = 10;

    private LinearLayoutManager mLayoutManager;
    private RecyclerView mRecyclerView;

    @Before
    public void setup() throws Throwable {
        mLayoutManager = new LinearLayoutManager(getActivity());
        mLayoutManager.ensureLayoutState();

        mRecyclerView = new RecyclerView(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(new TestAdapter(NUM_ITEMS) {

            @Override
            public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                    int viewType) {
                TestViewHolder holder = super.onCreateViewHolder(parent, viewType);
                holder.itemView.setMinimumHeight(mRecyclerView.getMeasuredHeight() * 2 / NUM_ITEMS);
                return holder;
            }
        });
        setRecyclerView(mRecyclerView);
        getInstrumentation().waitForIdleSync();
        assertThat("Test sanity", mRecyclerView.getChildCount() > 0, is(true));
    }

    @Test
    public void testEdgeEffectDirections() throws Throwable {
        TestEdgeEffectFactory factory = new TestEdgeEffectFactory();
        mRecyclerView.setEdgeEffectFactory(factory);
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);
        scrollViewBy(3);
        assertNull(factory.mBottom);
        assertNotNull(factory.mTop);
        assertTrue(factory.mTop.mPullDistance > 0);

        scrollToPosition(NUM_ITEMS - 1);
        waitForIdleScroll(mRecyclerView);
        scrollViewBy(-3);

        assertNotNull(factory.mBottom);
        assertTrue(factory.mBottom.mPullDistance > 0);
    }

    @Test
    public void testEdgeEffectReplaced() throws Throwable {
        TestEdgeEffectFactory factory1 = new TestEdgeEffectFactory();
        mRecyclerView.setEdgeEffectFactory(factory1);
        scrollToPosition(0);
        waitForIdleScroll(mRecyclerView);

        scrollViewBy(3);
        assertNotNull(factory1.mTop);
        float oldPullDistance = factory1.mTop.mPullDistance;

        waitForIdleScroll(mRecyclerView);
        TestEdgeEffectFactory factory2 = new TestEdgeEffectFactory();
        mRecyclerView.setEdgeEffectFactory(factory2);
        scrollViewBy(30);
        assertNotNull(factory2.mTop);

        assertTrue(factory2.mTop.mPullDistance > oldPullDistance);
        assertEquals(oldPullDistance, factory1.mTop.mPullDistance, 0.1f);
    }

    private void scrollViewBy(final int value) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TouchUtils.scrollView(MotionEvent.AXIS_VSCROLL, value,
                        InputDeviceCompat.SOURCE_CLASS_POINTER, mRecyclerView);
            }
        });
    }

    private class TestEdgeEffectFactory extends EdgeEffectFactory {

        TestEdgeEffect mTop, mBottom;

        @NonNull
        @Override
        protected EdgeEffect createEdgeEffect(RecyclerView view, int direction) {
            TestEdgeEffect effect = new TestEdgeEffect(view.getContext());
            if (direction == EdgeEffectFactory.DIRECTION_TOP) {
                mTop = effect;
            } else if (direction == EdgeEffectFactory.DIRECTION_BOTTOM) {
                mBottom = effect;
            }
            return effect;
        }
    }

    private class TestEdgeEffect extends EdgeEffect {

        private float mPullDistance;

        TestEdgeEffect(Context context) {
            super(context);
        }

        @Override
        public void onPull(float deltaDistance, float displacement) {
            onPull(deltaDistance);
        }

        @Override
        public void onPull(float deltaDistance) {
            mPullDistance = deltaDistance;
        }
    }
}
