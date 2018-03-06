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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.os.Build;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SmallTest
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
@RunWith(AndroidJUnit4.class)
public class RecyclerViewPrefetchTest extends BaseRecyclerViewInstrumentationTest {
    private class PrefetchLayoutManager extends TestLayoutManager {
        CountDownLatch prefetchLatch = new CountDownLatch(1);

        @Override
        public boolean canScrollVertically() {
            return true;
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            super.onLayoutChildren(recycler, state);
            detachAndScrapAttachedViews(recycler);
            layoutRange(recycler, 0, 5);
        }

        @Override
        public void onLayoutCompleted(RecyclerView.State state) {
            super.onLayoutCompleted(state);
            layoutLatch.countDown();
        }

        @Override
        public void collectAdjacentPrefetchPositions(int dx, int dy, RecyclerView.State state,
                LayoutPrefetchRegistry layoutPrefetchRegistry) {
            prefetchLatch.countDown();
            layoutPrefetchRegistry.addPosition(6, 0);
        }

        void waitForPrefetch(int time) throws InterruptedException {
            assertThat(prefetchLatch.await(time, TimeUnit.SECONDS),
                    is(true));
            getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                }
            });
        }
    }

    private ArrayList<RecyclerView.ViewHolder> cachedViews() {
        return mRecyclerView.mRecycler.mCachedViews;
    }

    @Test
    public void prefetchTest() throws Throwable {
        RecyclerView recyclerView = new RecyclerView(getActivity());
        recyclerView.setAdapter(new TestAdapter(50));
        PrefetchLayoutManager layout = new PrefetchLayoutManager();
        recyclerView.setLayoutManager(layout);

        {
            layout.expectLayouts(1);
            setRecyclerView(recyclerView);
            layout.waitForLayout(10);
        }

        assertThat(layout.prefetchLatch.getCount(), is(1L)); // shouldn't have fired yet
        assertThat(cachedViews().size(), is(0));
        smoothScrollBy(50);

        layout.waitForPrefetch(10);
        assertThat(cachedViews().size(), is(1));
        assertThat(cachedViews().get(0).getAdapterPosition(), is(6));
    }
}