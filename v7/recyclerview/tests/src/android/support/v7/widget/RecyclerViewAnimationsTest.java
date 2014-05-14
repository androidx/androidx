/*
 * Copyright (C) 2014 The Android Open Source Project
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

import java.util.concurrent.TimeUnit;

public class RecyclerViewAnimationsTest extends BaseRecyclerViewInstrumentationTest {

    public void testBasicLayout() throws Throwable {
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        final int itemCount = 10;
        final TestAdapter testAdapter = new TestAdapter(itemCount);
        recyclerView.setAdapter(testAdapter);
        recyclerView.setItemAnimator(null);
        TestLayoutManager layoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    detachAndScrapAttachedViews(recycler);

                    layoutRange(recycler, 0, state.getItemCount());
                    assertEquals("correct # of children should be rendered",
                            state.getItemCount(), getChildCount());
                    assertVisibleItemPositions();
                    if (getRecyclerView().getItemAnimator() == null) {
                        removeAndRecycleScrap(recycler);
                    }
                } finally {
                    layoutLatch.countDown();
                }

            }
        };
        layoutManager.expectLayouts(1);
        recyclerView.setLayoutManager(layoutManager);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().mContainer.addView(recyclerView);
            }
        });
        layoutManager.waitForLayout(1, TimeUnit.SECONDS);
        layoutManager.expectLayouts(1);
        testAdapter.deleteRangeAndNotify(0, 7);
        layoutManager.waitForLayout(1, TimeUnit.SECONDS);
    }

}
