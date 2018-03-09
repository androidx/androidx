/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.fragment.test.R;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.testutils.FragmentActivityUtils;
import android.support.testutils.RecreatedActivity;
import android.support.v4.app.test.LoaderActivity;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class LoaderTest {
    private static final int DELAY_LOADER = 10;

    @Rule
    public ActivityTestRule<LoaderActivity> mActivityRule =
            new ActivityTestRule<>(LoaderActivity.class);

    /**
     * Test to ensure that there is no Activity leak due to Loader
     */
    @Test
    public void testLeak() throws Throwable {
        // Restart the activity because mActivityRule keeps a strong reference to the
        // old activity.
        LoaderActivity activity = FragmentActivityUtils.recreateActivity(mActivityRule,
                mActivityRule.getActivity());

        LoaderFragment fragment = new LoaderFragment();
        FragmentManager fm = activity.getSupportFragmentManager();

        fm.beginTransaction()
                .add(fragment, "1")
                .commit();

        FragmentTestUtil.executePendingTransactions(mActivityRule, fm);

        fm.beginTransaction()
                .remove(fragment)
                .addToBackStack(null)
                .commit();

        FragmentTestUtil.executePendingTransactions(mActivityRule, fm);
        fm = null; // clear it so that it can be released

        WeakReference<RecreatedActivity> weakActivity =
                new WeakReference<>(LoaderActivity.sActivity);

        activity = FragmentActivityUtils.recreateActivity(mActivityRule, activity);

        // Wait for everything to settle. We have to make sure that the old Activity
        // is ready to be collected.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        FragmentTestUtil.waitForExecution(mActivityRule);

        // Force a garbage collection.
        FragmentTestUtil.forceGC();
        assertNull(weakActivity.get());
    }

    /**
     * When a LoaderManager is reused, it should notify in onResume
     */
    @Test
    public void startWhenReused() throws Throwable {
        LoaderActivity activity = mActivityRule.getActivity();

        assertEquals("Loaded!", activity.textView.getText().toString());

        activity = FragmentActivityUtils.recreateActivity(mActivityRule, activity);

        FragmentTestUtil.waitForExecution(mActivityRule);

        // After orientation change, the text should still be loaded properly
        assertEquals("Loaded!", activity.textView.getText().toString());
    }

    @Test
    public void testRedeliverWhenReattached() throws Throwable {
        LoaderActivity activity = mActivityRule.getActivity();

        FragmentManager fm = activity.getSupportFragmentManager();

        LoaderActivity.TextLoaderFragment fragment =
                (LoaderActivity.TextLoaderFragment) fm.findFragmentById(R.id.fragmentContainer);

        assertNotNull(fragment);
        assertEquals("Loaded!", fragment.textView.getText().toString());

        fm.beginTransaction()
                .detach(fragment)
                .commit();

        FragmentTestUtil.executePendingTransactions(mActivityRule, fm);

        fm.beginTransaction()
                .attach(fragment)
                .commit();

        FragmentTestUtil.executePendingTransactions(mActivityRule, fm);

        assertEquals("Loaded!", fragment.textView.getText().toString());
    }

    /**
     * Test to ensure that loader operations, such as destroyLoader, can safely be called
     * in onLoadFinished
     */
    @Test
    public void testDestroyFromOnLoadFinished() throws Throwable {
        final LoaderActivity activity = mActivityRule.getActivity();
        final CountDownLatch onLoadFinishedLatch = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final LoaderManager loaderManager = activity.getSupportLoaderManager();
                activity.getSupportLoaderManager().initLoader(43, null,
                        new DummyLoaderCallbacks(activity) {
                            @Override
                            public void onLoadFinished(@NonNull Loader<Boolean> loader,
                                    Boolean data) {
                                super.onLoadFinished(loader, data);
                                loaderManager.destroyLoader(43);
                            }
                        });
            }
        });
        onLoadFinishedLatch.await(1, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalStateException.class)
    public void enforceOnMainThread_initLoader() {
        LoaderActivity activity = mActivityRule.getActivity();
        activity.getSupportLoaderManager().initLoader(-1, null,
                new DummyLoaderCallbacks(activity));
    }

    @Test(expected = IllegalStateException.class)
    public void enforceOnMainThread_restartLoader() {
        LoaderActivity activity = mActivityRule.getActivity();
        activity.getSupportLoaderManager().restartLoader(-1, null,
                new DummyLoaderCallbacks(activity));
    }

    @Test(expected = IllegalStateException.class)
    public void enforceOnMainThread_destroyLoader() {
        LoaderActivity activity = mActivityRule.getActivity();
        activity.getSupportLoaderManager().destroyLoader(-1);
    }

    @Test
    public void testDestroyLoaderBeforeDeliverData() throws Throwable {
        final LoaderActivity activity = mActivityRule.getActivity();
        final LoaderManager loaderManager = activity.getSupportLoaderManager();
        final LoaderTest.DelayLoaderCallbacks callback =
                new LoaderTest.DelayLoaderCallbacks(mock(Context.class), new CountDownLatch(1));
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
        final LoaderActivity activity = mActivityRule.getActivity();
        final LoaderManager loaderManager = activity.getSupportLoaderManager();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final LoaderTest.DelayLoaderCallbacks callback =
                new LoaderTest.DelayLoaderCallbacks(mock(Context.class), countDownLatch);
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
        final LoaderActivity activity = mActivityRule.getActivity();
        final LoaderManager loaderManager = activity.getSupportLoaderManager();
        final LoaderTest.DelayLoaderCallbacks initialCallback =
                new LoaderTest.DelayLoaderCallbacks(mock(Context.class), new CountDownLatch(1));
        CountDownLatch restartCountDownLatch = new CountDownLatch(1);
        final LoaderTest.DelayLoaderCallbacks restartCallback =
                new LoaderTest.DelayLoaderCallbacks(mock(Context.class), restartCountDownLatch);
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
        final LoaderActivity activity = mActivityRule.getActivity();
        final LoaderManager loaderManager = activity.getSupportLoaderManager();
        CountDownLatch initialCountDownLatch = new CountDownLatch(1);
        final LoaderTest.DelayLoaderCallbacks initialCallback =
                new LoaderTest.DelayLoaderCallbacks(mock(Context.class), initialCountDownLatch);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderManager.initLoader(45, null, initialCallback);
            }
        });
        // Wait for the first Loader to return data
        initialCountDownLatch.await(1, TimeUnit.SECONDS);
        CountDownLatch restartCountDownLatch = new CountDownLatch(1);
        final LoaderTest.DelayLoaderCallbacks restartCallback =
                new LoaderTest.DelayLoaderCallbacks(mock(Context.class), restartCountDownLatch);
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
        final LoaderActivity activity = mActivityRule.getActivity();
        final LoaderManager loaderManager = activity.getSupportLoaderManager();
        CountDownLatch initialCountDownLatch = new CountDownLatch(1);
        final LoaderTest.DelayLoaderCallbacks initialCallback =
                new LoaderTest.DelayLoaderCallbacks(mock(Context.class), initialCountDownLatch);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderManager.initLoader(46, null, initialCallback);
            }
        });
        // Wait for the first Loader to return data
        initialCountDownLatch.await(1, TimeUnit.SECONDS);
        final LoaderTest.DelayLoaderCallbacks intermediateCallback =
                new LoaderTest.DelayLoaderCallbacks(mock(Context.class), new CountDownLatch(1));
        CountDownLatch restartCountDownLatch = new CountDownLatch(1);
        final LoaderTest.DelayLoaderCallbacks restartCallback =
                new LoaderTest.DelayLoaderCallbacks(mock(Context.class), restartCountDownLatch);
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

    /**
     * When a change is interrupted with stop, the data in the LoaderManager remains stale.
     */
    //@Test
    public void noStaleData() throws Throwable {
        final LoaderActivity activity = mActivityRule.getActivity();
        final String[] value = new String[] { "First Value" };

        final CountDownLatch[] loadedLatch = new CountDownLatch[] { new CountDownLatch(1) };
        final Loader<String>[] loaders = new Loader[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final Loader<String> loader =
                        activity.getSupportLoaderManager().initLoader(DELAY_LOADER, null,
                                new LoaderManager.LoaderCallbacks<String>() {
                                    @NonNull
                                    @Override
                                    public Loader<String> onCreateLoader(int id, Bundle args) {
                                        return new AsyncTaskLoader<String>(activity) {
                                            @Override
                                            protected void onStopLoading() {
                                                cancelLoad();
                                            }

                                            @Override
                                            public String loadInBackground() {
                                                SystemClock.sleep(50);
                                                return value[0];
                                            }

                                            @Override
                                            protected void onStartLoading() {
                                                if (takeContentChanged()) {
                                                    forceLoad();
                                                }
                                                super.onStartLoading();
                                            }
                                        };
                                    }

                                    @Override
                                    public void onLoadFinished(@NonNull Loader<String> loader,
                                            String data) {
                                        activity.textViewB.setText(data);
                                        loadedLatch[0].countDown();
                                    }

                                    @Override
                                    public void onLoaderReset(@NonNull Loader<String> loader) {
                                    }
                                });
                loader.forceLoad();
                loaders[0] = loader;
            }
        });

        assertTrue(loadedLatch[0].await(1, TimeUnit.SECONDS));
        assertEquals("First Value", activity.textViewB.getText().toString());

        loadedLatch[0] = new CountDownLatch(1);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                value[0] = "Second Value";
                loaders[0].onContentChanged();
                loaders[0].stopLoading();
            }
        });

        // Since the loader was stopped (and canceled), it shouldn't notify the change
        assertFalse(loadedLatch[0].await(300, TimeUnit.MILLISECONDS));

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaders[0].startLoading();
            }
        });

        // Since the loader was stopped (and canceled), it shouldn't notify the change
        assertTrue(loadedLatch[0].await(1, TimeUnit.SECONDS));
        assertEquals("Second Value", activity.textViewB.getText().toString());
    }


    public static class LoaderFragment extends Fragment {
        private static final int LOADER_ID = 1;

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            getLoaderManager().initLoader(LOADER_ID, null,
                    new DummyLoaderCallbacks(getContext()));
        }
    }

    static class DummyLoaderCallbacks implements LoaderManager.LoaderCallbacks<Boolean> {
        private final Context mContext;

        Loader<Boolean> mLoader;
        boolean mOnLoadFinished;
        boolean mOnLoaderReset;

        DummyLoaderCallbacks(Context context) {
            mContext = context;
        }

        @NonNull
        @Override
        public Loader<Boolean> onCreateLoader(int id, Bundle args) {
            mLoader = new DummyLoader(mContext);
            return mLoader;
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Boolean> loader, Boolean data) {
            mOnLoadFinished = true;
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Boolean> loader) {
            mOnLoaderReset = true;
        }
    }

    static class DummyLoader extends Loader<Boolean> {
        DummyLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            deliverResult(true);
        }
    }

    static class DelayLoaderCallbacks implements LoaderManager.LoaderCallbacks<Boolean> {
        private final Context mContext;
        private final CountDownLatch mDeliverResultLatch;

        Loader<Boolean> mLoader;
        boolean mOnLoadFinished;
        boolean mOnLoaderReset;

        DelayLoaderCallbacks(Context context, CountDownLatch deliverResultLatch) {
            mContext = context;
            mDeliverResultLatch = deliverResultLatch;
        }

        @NonNull
        @Override
        public Loader<Boolean> onCreateLoader(int id, Bundle args) {
            mLoader = new DelayLoader(mContext, mDeliverResultLatch);
            return mLoader;
        }

        @Override
        public void onLoadFinished(@NonNull Loader<Boolean> loader, Boolean data) {
            mOnLoadFinished = true;
        }

        @Override
        public void onLoaderReset(@NonNull Loader<Boolean> loader) {
            mOnLoaderReset = true;
        }
    }

    static class DelayLoader extends AsyncTaskLoader<Boolean> {
        private final CountDownLatch mDeliverResultLatch;

        DelayLoader(Context context, CountDownLatch deliverResultLatch) {
            super(context);
            mDeliverResultLatch = deliverResultLatch;
        }

        @Override
        public Boolean loadInBackground() {
            SystemClock.sleep(50);
            return true;
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public void deliverResult(@Nullable Boolean data) {
            super.deliverResult(data);
            mDeliverResultLatch.countDown();
        }
    }
}
