/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.v7.widget;

import org.junit.Test;
import org.junit.runner.RunWith;

import android.support.test.runner.AndroidJUnit4;
import android.support.v4.view.ViewCompat;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(AndroidJUnit4.class)
public class RecyclerViewAccessibilityLifecycleTest extends BaseRecyclerViewInstrumentationTest {
    @Test
    public void dontDispatchChangeDuringLayout() throws Throwable {
        LayoutAllLayoutManager lm = new LayoutAllLayoutManager();
        final AtomicBoolean calledA11DuringLayout = new AtomicBoolean(false);
        final List<Integer> invocations = new ArrayList<>();

        final WrappedRecyclerView recyclerView = new WrappedRecyclerView(getActivity()) {
            @Override
            boolean isAccessibilityEnabled() {
                return true;
            }

            @Override
            public boolean setChildImportantForAccessibilityInternal(ViewHolder viewHolder,
                    int importantForAccessibilityBeforeHidden) {
                invocations.add(importantForAccessibilityBeforeHidden);
                boolean notified = super.setChildImportantForAccessibilityInternal(viewHolder,
                        importantForAccessibilityBeforeHidden);
                if (notified && mRecyclerView.isComputingLayout()) {
                    calledA11DuringLayout.set(true);
                }
                return notified;
            }
        };
        TestAdapter adapter = new TestAdapter(10) {
            @Override
            public TestViewHolder onCreateViewHolder(ViewGroup parent,
                    int viewType) {
                TestViewHolder vh = super.onCreateViewHolder(parent, viewType);
                ViewCompat.setImportantForAccessibility(vh.itemView,
                        ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
                return vh;
            }
        };
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(lm);
        lm.expectLayouts(1);
        setRecyclerView(recyclerView);
        lm.waitForLayout(1);
        assertThat(calledA11DuringLayout.get(), is(false));
        lm.expectLayouts(1);
        adapter.deleteAndNotify(2, 2);
        lm.waitForLayout(2);
        recyclerView.waitUntilAnimations();
        assertThat(invocations, is(Arrays.asList(
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES,
                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES)));

        assertThat(calledA11DuringLayout.get(), is(false));
    }

    public class LayoutAllLayoutManager extends TestLayoutManager {
        private final boolean mAllowNullLayoutLatch;

        public LayoutAllLayoutManager() {
            // by default, we don't allow unexpected layouts.
            this(false);
        }
        LayoutAllLayoutManager(boolean allowNullLayoutLatch) {
            mAllowNullLayoutLatch = allowNullLayoutLatch;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            detachAndScrapAttachedViews(recycler);
            layoutRange(recycler, 0, state.getItemCount());
            if (!mAllowNullLayoutLatch || layoutLatch != null) {
                layoutLatch.countDown();
            }
        }
    }
}
