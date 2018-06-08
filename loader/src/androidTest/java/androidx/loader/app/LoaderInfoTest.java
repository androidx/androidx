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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.loader.app.test.DelayLoader;
import androidx.loader.app.test.DummyLoaderCallbacks;
import androidx.loader.content.Loader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LoaderInfoTest {

    private LifecycleOwner mOwner;
    private LifecycleRegistry mRegistry;

    @Before
    public void setup() {
        mOwner = mock(LifecycleOwner.class);
        mRegistry = new LifecycleRegistry(mOwner);
        when(mOwner.getLifecycle()).thenReturn(mRegistry);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
    }

    @Test
    public void testIsCallbackWaitingForData() throws Throwable {
        final DummyLoaderCallbacks loaderCallback = new DummyLoaderCallbacks(mock(Context.class));
        final CountDownLatch deliverResultLatch = new CountDownLatch(1);
        Loader<Boolean> delayLoader = new DelayLoader(mock(Context.class),
                deliverResultLatch);
        final LoaderManagerImpl.LoaderInfo<Boolean> loaderInfo = new LoaderManagerImpl.LoaderInfo<>(
                0, null, delayLoader, null);
        assertFalse("isCallbackWaitingForData should be false before setCallback",
                loaderInfo.isCallbackWaitingForData());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                loaderInfo.setCallback(mOwner, loaderCallback);
            }
        });
        assertTrue("isCallbackWaitingForData should be true immediately after setCallback",
                loaderInfo.isCallbackWaitingForData());

        assertTrue("Loader timed out delivering results",
                deliverResultLatch.await(1, TimeUnit.SECONDS));
        // Results are posted to the UI thread, so we wait for them there
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                assertTrue("onLoadFinished should be called after setCallback",
                        loaderCallback.mOnLoadFinished);
                assertFalse("isCallbackWaitingForData should be false after onLoadFinished",
                        loaderInfo.isCallbackWaitingForData());
            }
        });
    }

    @UiThreadTest
    @Test
    public void testSetCallback() throws Throwable {
        final DummyLoaderCallbacks loaderCallback = new DummyLoaderCallbacks(mock(Context.class));
        Loader<Boolean> loader = loaderCallback.onCreateLoader(0, null);
        final LoaderManagerImpl.LoaderInfo<Boolean> loaderInfo = new LoaderManagerImpl.LoaderInfo<>(
                0, null, loader, null);
        assertFalse("onLoadFinished shouldn't be called before setCallback",
                loaderCallback.mOnLoadFinished);

        loaderInfo.setCallback(mOwner, loaderCallback);
        assertTrue("onLoadFinished should be called after setCallback",
                loaderCallback.mOnLoadFinished);
    }

    @UiThreadTest
    @Test
    public void testSetCallback_replace() throws Throwable {
        final DummyLoaderCallbacks initialCallback = new DummyLoaderCallbacks(mock(Context.class));
        Loader<Boolean> loader = initialCallback.onCreateLoader(0, null);
        LoaderManagerImpl.LoaderInfo<Boolean> loaderInfo = new LoaderManagerImpl.LoaderInfo<>(
                0, null, loader, null);
        assertFalse("onLoadFinished for initial shouldn't be called before setCallback initial",
                initialCallback.mOnLoadFinished);

        loaderInfo.setCallback(mOwner, initialCallback);
        assertTrue("onLoadFinished for initial should be called after setCallback initial",
                initialCallback.mOnLoadFinished);

        final DummyLoaderCallbacks replacementCallback =
                new DummyLoaderCallbacks(mock(Context.class));
        initialCallback.mOnLoadFinished = false;

        loaderInfo.setCallback(mOwner, replacementCallback);
        assertFalse("onLoadFinished for initial should not be called "
                        + "after setCallback replacement",
                initialCallback.mOnLoadFinished);
        assertTrue("onLoadFinished for replacement should be called "
                        + " after setCallback replacement",
                replacementCallback.mOnLoadFinished);
    }

    @UiThreadTest
    @Test
    public void testMarkForRedelivery() throws Throwable {
        DummyLoaderCallbacks loaderCallback =
                new DummyLoaderCallbacks(mock(Context.class));
        Loader<Boolean> loader = loaderCallback.onCreateLoader(0, null);
        LoaderManagerImpl.LoaderInfo<Boolean> loaderInfo = new LoaderManagerImpl.LoaderInfo<>(
                0, null, loader, null);
        loaderInfo.setCallback(mOwner, loaderCallback);
        assertTrue("onLoadFinished should be called after setCallback",
                loaderCallback.mOnLoadFinished);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        loaderCallback.mOnLoadFinished = false;
        loaderInfo.markForRedelivery();
        assertFalse("onLoadFinished should not be called when stopped after markForRedelivery",
                loaderCallback.mOnLoadFinished);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertTrue("onLoadFinished should be called after markForRedelivery",
                loaderCallback.mOnLoadFinished);
    }

    @UiThreadTest
    @Test
    public void testMarkForRedelivery_replace() throws Throwable {
        DummyLoaderCallbacks initialCallback =
                new DummyLoaderCallbacks(mock(Context.class));
        Loader<Boolean> loader = initialCallback.onCreateLoader(0, null);
        LoaderManagerImpl.LoaderInfo<Boolean> loaderInfo = new LoaderManagerImpl.LoaderInfo<>(
                0, null, loader, null);
        loaderInfo.setCallback(mOwner, initialCallback);
        assertTrue("onLoadFinished for initial should be called after setCallback initial",
                initialCallback.mOnLoadFinished);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP);
        initialCallback.mOnLoadFinished = false;
        loaderInfo.markForRedelivery();
        assertFalse("onLoadFinished should not be called when stopped after markForRedelivery",
                initialCallback.mOnLoadFinished);

        // Replace the callback
        final DummyLoaderCallbacks replacementCallback =
                new DummyLoaderCallbacks(mock(Context.class));
        loaderInfo.setCallback(mOwner, replacementCallback);

        mRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        assertFalse("onLoadFinished for initial should not be called "
                        + "after setCallback replacement",
                initialCallback.mOnLoadFinished);
        assertTrue("onLoadFinished for replacement should be called "
                        + " after setCallback replacement",
                replacementCallback.mOnLoadFinished);
    }

    @UiThreadTest
    @Test
    public void testDestroy() throws Throwable {
        final DummyLoaderCallbacks loaderCallback = new DummyLoaderCallbacks(mock(Context.class));
        final Loader<Boolean> loader = loaderCallback.onCreateLoader(0, null);
        final LoaderManagerImpl.LoaderInfo<Boolean> loaderInfo = new LoaderManagerImpl.LoaderInfo<>(
                0, null, loader, null);

        loaderInfo.setCallback(mOwner, loaderCallback);
        assertTrue("Loader should be started after setCallback", loader.isStarted());
        loaderInfo.destroy(true);
        assertFalse("Loader should not be started after destroy", loader.isStarted());
    }
}
