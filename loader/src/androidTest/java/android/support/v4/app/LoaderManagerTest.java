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

package android.support.v4.app;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ViewModelStore;
import android.arch.lifecycle.ViewModelStoreOwner;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.test.DummyLoaderCallbacks;
import android.support.v4.app.test.EmptyActivity;
import android.support.v4.content.Loader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
