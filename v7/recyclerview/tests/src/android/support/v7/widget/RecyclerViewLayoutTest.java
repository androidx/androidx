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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class RecyclerViewLayoutTest extends BaseRecyclerViewInstrumentationTest {

    public void testState() throws Throwable {
        final TestAdapter adapter = new TestAdapter(10);
        final RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(adapter);
        recyclerView.setItemAnimator(null);
        final AtomicInteger itemCount = new AtomicInteger();
        final AtomicBoolean structureChanged = new AtomicBoolean();
        TestLayoutManager testLayoutManager = new TestLayoutManager() {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                layoutRange(recycler, 0, state.getItemCount());
                itemCount.set(state.getItemCount());
                structureChanged.set(state.didStructureChange());
                layoutLatch.countDown();
            }
        };
        recyclerView.setLayoutManager(testLayoutManager);
        testLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                getActivity().mContainer.addView(recyclerView);
            }
        });
        Thread.sleep(5000);
        testLayoutManager.waitForLayout(2, TimeUnit.SECONDS);

        assertEquals("item count in state should be correct", adapter.getItemCount()
                , itemCount.get());
        assertEquals("structure changed should be true for first layout", true,
                structureChanged.get());
        Thread.sleep(1000); //wait for other layouts.
        testLayoutManager.expectLayouts(1);
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.requestLayout();
            }
        });
        testLayoutManager.waitForLayout(1, TimeUnit.SECONDS);
        assertEquals("in second layout,structure changed should be false", false,
                structureChanged.get());
        testLayoutManager.expectLayouts(recyclerView.getItemAnimator() == null ? 1 : 2); //
        adapter.deleteAndNotify(3, 2);
        testLayoutManager.waitForLayout(3, TimeUnit.SECONDS);
        assertEquals("when items are removed, item count in state should be updated",
                adapter.getItemCount(),
                itemCount.get());
        assertEquals("structure changed should be true when items are removed", true,
                structureChanged.get());
        testLayoutManager.expectLayouts(recyclerView.getItemAnimator() == null ? 1 : 2);
        adapter.addAndNotify(2, 5);
        testLayoutManager.waitForLayout(3, TimeUnit.SECONDS);

        assertEquals("when items are added, item count in state should be updated",
                adapter.getItemCount(),
                itemCount.get());
        assertEquals("structure changed should be true when items are removed", true,
                structureChanged.get());

    }

}