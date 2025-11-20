/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.asynclayoutinflater.view;

import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;

import androidx.asynclayoutinflater.test.R;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

@MediumTest
@RunWith(AndroidJUnit4.class)
@SuppressWarnings("deprecation")
public class AsyncLayoutInflaterTest {
    private static final String BG_THREAD_NAME = "bg-async-thread";
    @Rule
    public ActivityScenarioRule<TestActivity> testActivityRule = new ActivityScenarioRule<>(
            TestActivity.class);
    Executor mBackgroundExecutor;
    AsyncLayoutInflater mAsyncLayoutInflater;
    AsyncLayoutInflater mAsyncLayoutInflaterAppCompat;
    TestActivity mActivity;

    @Before
    public void setup() {
        testActivityRule.getScenario().onActivity(activity -> {
            mActivity = activity;
            mAsyncLayoutInflater = activity.getAsyncLayoutInflater();
            mAsyncLayoutInflaterAppCompat = activity.getAsyncLayoutInflaterAppCompat();
        });
        mBackgroundExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setNameFormat(BG_THREAD_NAME).build());
    }

    @Test
    public void incorrectAsyncInflaterView_forButton_withDeprecatedAPI() throws Exception {
        SettableFuture<View> asyncInflatedViewFuture = SettableFuture.create();
        mAsyncLayoutInflater.inflate(R.layout.test_button, null, (view, resId, parent) -> {
            asyncInflatedViewFuture.set(view);
        });
        View asyncInflatedView = asyncInflatedViewFuture.get();
        View inflatedView = LayoutInflater.from(mActivity).inflate(R.layout.test_button, null,
                false);
        Assert.assertNotSame(asyncInflatedView.getClass(), inflatedView.getClass());
    }

    @Test
    public void correctAsyncInflaterView_forButton() throws Exception {
        SettableFuture<View> asyncInflatedViewFuture = SettableFuture.create();
        mAsyncLayoutInflaterAppCompat.inflate(R.layout.test_button, null,
                /* callbackExecutor= */ null,
                (view, resId, parent) -> {
                    asyncInflatedViewFuture.set(view);
                });
        View asyncInflatedView = asyncInflatedViewFuture.get();
        View inflatedView = LayoutInflater.from(mActivity).inflate(R.layout.test_button, null,
                false);
        Assert.assertSame(asyncInflatedView.getClass(), inflatedView.getClass());
    }

    @Test
    public void inflateCallback_onMainThread() throws Exception {
        AtomicReference<Thread> callbackThread = new AtomicReference<>(null);
        SettableFuture<View> asyncInflatedViewFuture = SettableFuture.create();
        mAsyncLayoutInflaterAppCompat.inflate(R.layout.test_button, null,
                /* callbackExecutor= */ null,
                (view, resId, parent) -> {
                    callbackThread.set(Thread.currentThread());
                    asyncInflatedViewFuture.set(view);
                });
        asyncInflatedViewFuture.get();
        Assert.assertSame(callbackThread.get(), Looper.getMainLooper().getThread());
    }

    @Test
    public void inflateCallback_onBackgroundThread() throws Exception {
        AtomicReference<Thread> callbackThread = new AtomicReference<>(null);
        SettableFuture<View> asyncInflatedViewFuture = SettableFuture.create();
        mAsyncLayoutInflaterAppCompat.inflate(R.layout.test_button, null,
                mBackgroundExecutor,
                (view, resId, parent) -> {
                    callbackThread.set(Thread.currentThread());
                    asyncInflatedViewFuture.set(view);
                });
        asyncInflatedViewFuture.get();
        // Not called on main thread.
        Assert.assertNotSame(callbackThread.get(), Looper.getMainLooper().getThread());
        Assert.assertEquals(callbackThread.get().getName(), BG_THREAD_NAME);
    }

    @Test
    public void inflateCallback_onBackgroundThread_onAsyncInflationFailure() throws Exception {
        AtomicReference<Thread> callbackThread = new AtomicReference<>(null);
        SettableFuture<View> asyncInflatedViewFuture = SettableFuture.create();
        mAsyncLayoutInflaterAppCompat.inflate(R.layout.fail_async_text_view, null,
                mBackgroundExecutor,
                (view, resId, parent) -> {
                    callbackThread.set(Thread.currentThread());
                    asyncInflatedViewFuture.set(view);
                });
        asyncInflatedViewFuture.get();
        Assert.assertNotSame(callbackThread.get(), Looper.getMainLooper().getThread());
        Assert.assertEquals(callbackThread.get().getName(), BG_THREAD_NAME);
    }
}
