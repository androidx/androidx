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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import android.os.Looper;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class ThreadUtilTest {
    @Rule
    public ActivityTestRule<TestActivity> mActivityRule =
            new ActivityTestRule<>(TestActivity.class);

    Map<String, LockedObject> results = new HashMap<>();

    ThreadUtil.MainThreadCallback<Integer> mMainThreadProxy;
    ThreadUtil.BackgroundCallback<Integer> mBackgroundProxy;

    @Before
    public void setup() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ThreadUtil<Integer> threadUtil = new MessageThreadUtil<>();

                mMainThreadProxy = threadUtil.getMainThreadProxy(
                        new ThreadUtil.MainThreadCallback<Integer>() {
                            @Override
                            public void updateItemCount(int generation, int itemCount) {
                                assertMainThread();
                                setResultData("updateItemCount", generation, itemCount);
                            }

                            @Override
                            public void addTile(int generation, TileList.Tile<Integer> data) {
                                assertMainThread();
                                setResultData("addTile", generation, data);
                            }

                            @Override
                            public void removeTile(int generation, int position) {
                                assertMainThread();
                                setResultData("removeTile", generation, position);
                            }
                        });

                mBackgroundProxy = threadUtil.getBackgroundProxy(
                        new ThreadUtil.BackgroundCallback<Integer>() {
                            @Override
                            public void refresh(int generation) {
                                assertBackgroundThread();
                                setResultData("refresh", generation);
                            }

                            @Override
                            public void updateRange(int rangeStart, int rangeEnd, int extRangeStart,
                                    int extRangeEnd, int scrollHint) {
                                assertBackgroundThread();
                                setResultData("updateRange", rangeStart, rangeEnd,
                                        extRangeStart, extRangeEnd, scrollHint);
                            }

                            @Override
                            public void loadTile(int position, int scrollHint) {
                                assertBackgroundThread();
                                setResultData("loadTile", position, scrollHint);
                            }

                            @Override
                            public void recycleTile(TileList.Tile<Integer> data) {
                                assertBackgroundThread();
                                setResultData("recycleTile", data);
                            }
                        });
            }
        });
    }

    @Test
    public void updateItemCount() throws InterruptedException {
        initWait("updateItemCount");
        // In this test and below the calls to mMainThreadProxy are not really made from the UI
        // thread. That's OK since the message queue inside mMainThreadProxy is synchronized.
        mMainThreadProxy.updateItemCount(7, 9);
        Object[] data = waitFor("updateItemCount");
        assertThat(data, is(new Object[]{7, 9}));
    }

    @Test
    public void addTile() throws InterruptedException {
        initWait("addTile");
        TileList.Tile<Integer> tile = new TileList.Tile<Integer>(Integer.class, 10);
        mMainThreadProxy.addTile(3, tile);
        Object[] data = waitFor("addTile");
        assertThat(data, is(new Object[]{3, tile}));
    }

    @Test
    public void removeTile() throws InterruptedException {
        initWait("removeTile");
        mMainThreadProxy.removeTile(1, 2);
        Object[] data = waitFor("removeTile");
        assertThat(data, is(new Object[]{1, 2}));
    }

    @Test
    public void refresh() throws InterruptedException {
        initWait("refresh");
        // In this test and below the calls to mBackgroundProxy are not really made from the worker
        // thread. That's OK since the message queue inside mBackgroundProxy is synchronized.
        mBackgroundProxy.refresh(2);
        Object[] data = waitFor("refresh");
        assertThat(data, is(new Object[]{2}));
    }

    @Test
    public void rangeUpdate() throws InterruptedException {
        initWait("updateRange");
        mBackgroundProxy.updateRange(10, 20, 5, 25, 1);
        Object[] data = waitFor("updateRange");
        assertThat(data, is(new Object[] {10, 20, 5, 25, 1}));
    }

    @Test
    public void loadTile() throws InterruptedException {
        initWait("loadTile");
        mBackgroundProxy.loadTile(2, 1);
        Object[] data = waitFor("loadTile");
        assertThat(data, is(new Object[]{2, 1}));
    }

    @Test
    public void recycleTile() throws InterruptedException {
        initWait("recycleTile");
        TileList.Tile<Integer> tile = new TileList.Tile<Integer>(Integer.class, 10);
        mBackgroundProxy.recycleTile(tile);
        Object[] data = waitFor("recycleTile");
        assertThat(data, is(new Object[]{tile}));
    }

    private void assertMainThread() {
        assertThat(Looper.myLooper(), notNullValue());
        assertThat(Looper.myLooper(), sameInstance(Looper.getMainLooper()));
    }

    private void assertBackgroundThread() {
        assertThat(Looper.myLooper(), not(Looper.getMainLooper()));
    }

    private void initWait(String key) throws InterruptedException {
        results.put(key, new LockedObject());
    }

    private Object[] waitFor(String key) throws InterruptedException {
        return results.get(key).waitFor();
    }

    private void setResultData(String key, Object... args) {
        if (results.containsKey(key)) {
            results.get(key).set(args);
        }
    }

    private class LockedObject {
        private Semaphore mLock = new Semaphore(1);
        private volatile Object[] mArgs;

        public LockedObject() {
            mLock.drainPermits();
        }

        public void set(Object... args) {
            mArgs = args;
            mLock.release(1);
        }

        public Object[] waitFor() throws InterruptedException {
            mLock.tryAcquire(1, 2, TimeUnit.SECONDS);
            return mArgs;
        }
    }
}
