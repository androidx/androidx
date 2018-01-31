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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.test.LoaderActivity;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LoaderInfoTest {

    @Rule
    public ActivityTestRule<LoaderActivity> mActivityRule =
            new ActivityTestRule<>(LoaderActivity.class);

    @Test
    public void testIsCallbackWaitingForData() throws Throwable {
        final LoaderTest.DummyLoaderCallbacks loaderCallback =
                new LoaderTest.DummyLoaderCallbacks(mActivityRule.getActivity());
        final CountDownLatch deliverResultLatch = new CountDownLatch(1);
        Loader<Boolean> delayLoader = new AsyncTaskLoader<Boolean>(mActivityRule.getActivity()) {
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
                deliverResultLatch.countDown();
            }
        };
        final LoaderManagerImpl.LoaderInfo<Boolean> loaderInfo = new LoaderManagerImpl.LoaderInfo<>(
                0, null, delayLoader);
        assertFalse("isCallbackWaitingForData should be false before setCallback",
                loaderInfo.isCallbackWaitingForData());

        loaderInfo.setCallback(mActivityRule.getActivity(), loaderCallback);
        assertTrue("isCallbackWaitingForData should be true immediately after setCallback",
                loaderInfo.isCallbackWaitingForData());

        assertTrue("Loader timed out delivering results",
                deliverResultLatch.await(1, TimeUnit.SECONDS));
        // Results are posted to the UI thread, so we wait for them there
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue("onLoadFinished should be called after setCallback",
                        loaderCallback.mOnLoadFinished);
                assertFalse("isCallbackWaitingForData should be false after onLoadFinished",
                        loaderInfo.isCallbackWaitingForData());
            }
        });
    }

    @Test
    public void testSetCallback() throws Throwable {
        final LoaderTest.DummyLoaderCallbacks loaderCallback =
                new LoaderTest.DummyLoaderCallbacks(mActivityRule.getActivity());
        Loader<Boolean> loader = loaderCallback.onCreateLoader(0, null);
        LoaderManagerImpl.LoaderInfo<Boolean> loaderInfo = new LoaderManagerImpl.LoaderInfo<>(
                0, null, loader);
        assertFalse("onLoadFinished shouldn't be called before setCallback",
                loaderCallback.mOnLoadFinished);

        loaderInfo.setCallback(mActivityRule.getActivity(), loaderCallback);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue("onLoadFinished should be called after setCallback",
                        loaderCallback.mOnLoadFinished);
            }
        });
    }

    @Test
    public void testSetCallback_replace() throws Throwable {
        final LoaderTest.DummyLoaderCallbacks initialCallback =
                new LoaderTest.DummyLoaderCallbacks(mActivityRule.getActivity());
        Loader<Boolean> loader = initialCallback.onCreateLoader(0, null);
        final LoaderManagerImpl.LoaderInfo<Boolean> loaderInfo = new LoaderManagerImpl.LoaderInfo<>(
                0, null, loader);
        assertFalse("onLoadFinished for initial shouldn't be called before setCallback initial",
                initialCallback.mOnLoadFinished);

        loaderInfo.setCallback(mActivityRule.getActivity(), initialCallback);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                assertTrue("onLoadFinished for initial should be called after setCallback initial",
                        initialCallback.mOnLoadFinished);
            }
        });

        final LoaderTest.DummyLoaderCallbacks replacementCallback =
                new LoaderTest.DummyLoaderCallbacks(mActivityRule.getActivity());
        initialCallback.mOnLoadFinished = false;

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderInfo.setCallback(mActivityRule.getActivity(), replacementCallback);
                assertFalse("onLoadFinished for initial should not be called "
                                + "after setCallback replacement",
                        initialCallback.mOnLoadFinished);
                assertTrue("onLoadFinished for replacement should be called "
                                + " after setCallback replacement",
                        replacementCallback.mOnLoadFinished);
            }
        });
    }

    @Test
    public void testDestroy() throws Throwable {
        final LoaderTest.DummyLoaderCallbacks loaderCallback =
                new LoaderTest.DummyLoaderCallbacks(mActivityRule.getActivity());
        final Loader<Boolean> loader = loaderCallback.onCreateLoader(0, null);
        final LoaderManagerImpl.LoaderInfo<Boolean> loaderInfo = new LoaderManagerImpl.LoaderInfo<>(
                0, null, loader);

        loaderInfo.setCallback(mActivityRule.getActivity(), loaderCallback);
        assertTrue("Loader should be started after setCallback", loader.isStarted());

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                loaderInfo.destroy();
                assertFalse("Loader should not be started after destroy", loader.isStarted());
            }
        });
    }
}
