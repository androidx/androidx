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

import static androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL;
import static androidx.recyclerview.widget.LinearLayoutManager.VERTICAL;

import static org.junit.Assert.assertEquals;

import android.os.Build;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(Parameterized.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP)
public class LinearLayoutManagerCacheTest extends BaseLinearLayoutManagerTest {

    final Config mConfig;
    final int mDx;
    final int mDy;

    public LinearLayoutManagerCacheTest(Config config, int dx, int dy) {
        mConfig = config;
        mDx = dx;
        mDy = dy;
    }

    @Parameterized.Parameters(name = "config:{0},dx:{1},dy:{2}")
    public static List<Object[]> getParams() {
        List<Object[]> result = new ArrayList<>();
        List<Config> configs = createBaseVariations();
        for (Config config : configs) {
            for (int dx : new int[] {-1, 0, 1}) {
                for (int dy : new int[] {-1, 0, 1}) {
                    result.add(new Object[]{config, dx, dy});
                }
            }
        }
        return result;
    }

    private ArrayList<RecyclerView.ViewHolder> cachedViews() {
        return mRecyclerView.mRecycler.mCachedViews;
    }

    @MediumTest
    @Test
    public void cacheAndPrefetch() throws Throwable {
        final Config config = (Config) mConfig.clone();

        setupByConfig(config, true);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // pretend to have an extra 5s before next frame so prefetch won't abort early
                ((WrappedRecyclerView)mRecyclerView).setDrawingTimeOffset(5000);

                mRecyclerView.scrollToPosition(100);
            }
        });

        mRecyclerView.setItemViewCacheSize(0);
        {
            mLayoutManager.expectPrefetch(1);
            mActivityRule.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.mRecycler.recycleAndClearCachedViews();
                    mRecyclerView.mGapWorker.postFromTraversal(mRecyclerView, mDx, mDy);

                    // Lie about post time, so prefetch executes even if it is delayed
                    mRecyclerView.mGapWorker.mPostTimeNs += TimeUnit.SECONDS.toNanos(5);
                }
            });
            mLayoutManager.waitForPrefetch(1);
        }


        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // validate cache state on UI thread
                if ((config.mOrientation == HORIZONTAL && mDx == 0)
                        || (config.mOrientation == VERTICAL && mDy == 0)) {
                    assertEquals(0, cachedViews().size());
                } else {
                    boolean reverseScroll = config.mOrientation == HORIZONTAL ? mDx < 0 : mDy < 0;
                    int lastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();
                    int firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();
                    assertEquals(1, cachedViews().size());
                    int prefetchedPosition = cachedViews().get(0).getAdapterPosition();
                    if (mConfig.mReverseLayout == reverseScroll) {
                        // Pos scroll on pos layout, or reverse scroll on reverse layout = toward last
                        assertEquals(lastVisibleItemPosition + 1, prefetchedPosition);
                    } else {
                        // Pos scroll on reverse layout, or reverse scroll on pos layout = toward first
                        assertEquals(firstVisibleItemPosition - 1, prefetchedPosition);
                    }
                }
            }
        });
    }
}
