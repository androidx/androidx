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

package androidx.loader.app;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ViewModelStore;
import android.arch.lifecycle.ViewModelStoreOwner;
import android.content.Context;
import android.os.Bundle;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.loader.app.test.DelayLoaderCallbacks;
import androidx.loader.app.test.DummyLoaderCallbacks;
import androidx.loader.app.test.EmptyActivity;
import androidx.loader.content.Loader;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LoaderManagerTest {

    @Rule
    public ActivityTestRule<EmptyActivity> mActivityRule =
            new ActivityTestRule<>(EmptyActivity.class);

    @Test
    public void testDestroyFromOnCreateLoader() throws Throwable {
        final LoaderOwner loaderOwner = new LoaderOwner();
        final CountDownLatch onCreateLoaderLatch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LoaderManager.getInstance(loaderOwner).initLoader(65, null,
                        new DummyLoaderCallbacks(mock(Context.class)) {
                            @NonNull
                            @Override
                            public Loader<Boolean> onCreateLoader(int id, Bundle args) {
                                try {
                                    LoaderManager.getInstance(loaderOwner).destroyLoader(65);
                                    fail("Calling destroyLoader in onCreateLoader should throw an "
                                            + "IllegalStateException");
                                } catch (IllegalStateException e) {
                                    // Expected
                                    onCreateLoaderLatch.countDown();
                                }
                                return super.onCreateLoader(id, args);
                            }
                        });
            }
        });
        onCreateLoaderLatch.await(1, TimeUnit.SECONDS);
    }

    /**
     * Test to ensure that loader operations, such as destroyLoader, can safely be called
     * in onLoadFinished
     */
    @Test
    public void testDestroyFromOnLoadFinished() throws Throwable {
        final LoaderOwner loaderOwner = new LoaderOwner();
        final CountDownLatch onLoadFinishedLatch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LoaderManager.getInstance(loaderOwner).initLoader(43, null,
                        new DummyLoaderCallbacks(mock(Context.class)) {
                            @Override
                            public void onLoadFinished(@NonNull Loader<Boolean> loader,
                                    Boolean data) {
                                super.onLoadFinished(loader, data);
                                LoaderManager.getInstance(loaderOwner).destroyLoader(43);
                            }
                        });
            }
        });
        onLoadFinishedLatch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testDestroyLoaderBeforeDeliverData() throws Throwable {
        final LoaderOwner loaderOwner = new LoaderOwner();
        final LoaderManager loaderManager = LoaderManager.getInstance(loaderOwner);
        final DelayLoaderCallbacks callback =
                new DelayLoaderCallbacks(mock(Context.class), new CountDownLatch(1));
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderManager.initLoader(37, null, callback);
                // Immediately destroy it before it has a chance to deliver data
                loaderManager.destroyLoader(37);
            }
        });
        assertFalse("LoaderCallbacks should not be reset if they never received data",
                callback.mOnLoaderReset);
        assertTrue("Loader should be reset after destroyLoader()",
                callback.mLoader.isReset());
    }

    @Test
    public void testDestroyLoaderAfterDeliverData() throws Throwable {
        final LoaderOwner loaderOwner = new LoaderOwner();
        final LoaderManager loaderManager = LoaderManager.getInstance(loaderOwner);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final DelayLoaderCallbacks callback =
                new DelayLoaderCallbacks(mock(Context.class), countDownLatch);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderManager.initLoader(38, null, callback);
            }
        });
        // Wait for the Loader to return data
        countDownLatch.await(1, TimeUnit.SECONDS);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderManager.destroyLoader(38);
            }
        });
        assertTrue("LoaderCallbacks should be reset after destroyLoader()",
                callback.mOnLoaderReset);
        assertTrue("Loader should be reset after destroyLoader()",
                callback.mLoader.isReset());
    }


    @Test
    public void testRestartLoaderBeforeDeliverData() throws Throwable {
        final LoaderOwner loaderOwner = new LoaderOwner();
        final LoaderManager loaderManager = LoaderManager.getInstance(loaderOwner);
        final DelayLoaderCallbacks initialCallback =
                new DelayLoaderCallbacks(mock(Context.class), new CountDownLatch(1));
        CountDownLatch restartCountDownLatch = new CountDownLatch(1);
        final DelayLoaderCallbacks restartCallback =
                new DelayLoaderCallbacks(mock(Context.class), restartCountDownLatch);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderManager.initLoader(44, null, initialCallback);
                // Immediately restart it before it has a chance to deliver data
                loaderManager.restartLoader(44, null, restartCallback);
            }
        });
        assertFalse("Initial LoaderCallbacks should not be reset after restartLoader()",
                initialCallback.mOnLoaderReset);
        assertTrue("Initial Loader should be reset if it is restarted before delivering data",
                initialCallback.mLoader.isReset());
        restartCountDownLatch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void testRestartLoaderAfterDeliverData() throws Throwable {
        final LoaderOwner loaderOwner = new LoaderOwner();
        final LoaderManager loaderManager = LoaderManager.getInstance(loaderOwner);
        CountDownLatch initialCountDownLatch = new CountDownLatch(1);
        final DelayLoaderCallbacks initialCallback =
                new DelayLoaderCallbacks(mock(Context.class), initialCountDownLatch);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderManager.initLoader(45, null, initialCallback);
            }
        });
        // Wait for the first Loader to return data
        initialCountDownLatch.await(1, TimeUnit.SECONDS);
        CountDownLatch restartCountDownLatch = new CountDownLatch(1);
        final DelayLoaderCallbacks restartCallback =
                new DelayLoaderCallbacks(mock(Context.class), restartCountDownLatch);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderManager.restartLoader(45, null, restartCallback);
            }
        });
        assertFalse("Initial LoaderCallbacks should not be reset after restartLoader()",
                initialCallback.mOnLoaderReset);
        assertFalse("Initial Loader should not be reset if it is restarted after delivering data",
                initialCallback.mLoader.isReset());
        restartCountDownLatch.await(1, TimeUnit.SECONDS);
        assertTrue("Initial Loader should be reset after its replacement Loader delivers data",
                initialCallback.mLoader.isReset());
    }

    @Test
    public void testRestartLoaderMultiple() throws Throwable {
        final LoaderOwner loaderOwner = new LoaderOwner();
        final LoaderManager loaderManager = LoaderManager.getInstance(loaderOwner);
        CountDownLatch initialCountDownLatch = new CountDownLatch(1);
        final DelayLoaderCallbacks initialCallback =
                new DelayLoaderCallbacks(mock(Context.class), initialCountDownLatch);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderManager.initLoader(46, null, initialCallback);
            }
        });
        // Wait for the first Loader to return data
        initialCountDownLatch.await(1, TimeUnit.SECONDS);
        final DelayLoaderCallbacks intermediateCallback =
                new DelayLoaderCallbacks(mock(Context.class), new CountDownLatch(1));
        CountDownLatch restartCountDownLatch = new CountDownLatch(1);
        final DelayLoaderCallbacks restartCallback =
                new DelayLoaderCallbacks(mock(Context.class), restartCountDownLatch);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderManager.restartLoader(46, null, intermediateCallback);
                // Immediately replace the restarted Loader with yet another Loader
                loaderManager.restartLoader(46, null, restartCallback);
            }
        });
        assertFalse("Initial LoaderCallbacks should not be reset after restartLoader()",
                initialCallback.mOnLoaderReset);
        assertFalse("Initial Loader should not be reset if it is restarted after delivering data",
                initialCallback.mLoader.isReset());
        assertTrue("Intermediate Loader should be reset if it is restarted before delivering data",
                intermediateCallback.mLoader.isReset());
        restartCountDownLatch.await(1, TimeUnit.SECONDS);
        assertTrue("Initial Loader should be reset after its replacement Loader delivers data",
                initialCallback.mLoader.isReset());
    }

    @Test(expected = IllegalStateException.class)
    public void enforceOnMainThread_initLoader() {
        LoaderOwner loaderOwner = new LoaderOwner();
        LoaderManager.getInstance(loaderOwner).initLoader(-1, null,
                new DummyLoaderCallbacks(mock(Context.class)));
    }

    @Test(expected = IllegalStateException.class)
    public void enforceOnMainThread_restartLoader() {
        LoaderOwner loaderOwner = new LoaderOwner();
        LoaderManager.getInstance(loaderOwner).restartLoader(-1, null,
                new DummyLoaderCallbacks(mock(Context.class)));
    }

    @Test(expected = IllegalStateException.class)
    public void enforceOnMainThread_destroyLoader() {
        LoaderOwner loaderOwner = new LoaderOwner();
        LoaderManager.getInstance(loaderOwner).destroyLoader(-1);
    }

    class LoaderOwner implements LifecycleOwner, ViewModelStoreOwner {

        private LifecycleRegistry mLifecycle = new LifecycleRegistry(this);
        private ViewModelStore mViewModelStore = new ViewModelStore();

        LoaderOwner() {
            mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START);
        }

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return mLifecycle;
        }

        @NonNull
        @Override
        public ViewModelStore getViewModelStore() {
            return mViewModelStore;
        }
    }
}
