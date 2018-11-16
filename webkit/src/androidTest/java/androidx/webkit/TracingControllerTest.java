/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.webkit;

import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class TracingControllerTest {
    private TracingController mTracingController;
    private WebViewOnUiThread mWebViewOnUiThread;
    private ExecutorService mSingleThreadExecutor;

    private static final String EXECUTOR_THREAD_PREFIX = "TracingExecutorThread";
    private static final int POLLING_TIMEOUT = 60 * 1000;
    private static final int EXECUTOR_TIMEOUT = 10; // timeout of executor shutdown in seconds

    @Before
    public void setUp() {
        AssumptionUtils.checkFeature(WebViewFeature.TRACING_CONTROLLER_BASIC_USAGE);

        mWebViewOnUiThread = new androidx.webkit.WebViewOnUiThread();
        mSingleThreadExecutor = Executors.newSingleThreadExecutor(getCustomThreadFactory());
        mTracingController = TracingController.getInstance();
        Assert.assertNotNull(mTracingController);
    }

    @After
    public void tearDown() throws Exception {
        ensureTracingStopped();
        if (mSingleThreadExecutor != null) {
            mSingleThreadExecutor.shutdown();
            if (!mSingleThreadExecutor.awaitTermination(EXECUTOR_TIMEOUT, TimeUnit.SECONDS)) {
                Assert.fail("Failed to shutdown executor");
            }
        }

        if (mWebViewOnUiThread != null) {
            mWebViewOnUiThread.cleanUp();
        }
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.TracingControllerTest#testTracingControllerCallbacksOnUI.
     * Modifications to this test should be reflected in that test as necessary.
     * See http://go/modifying-webview-cts.
     */
    @Test
    public void testTracingControllerCallbacksOnUI() throws Throwable {
        final TracingReceiver tracingReceiver = new TracingReceiver();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                runTracingTestWithCallbacks(tracingReceiver, mSingleThreadExecutor);
            }
        });
        PollingCheck.check("Tracing did not complete", POLLING_TIMEOUT,
                tracingReceiver.getCompleteCallable());
        Assert.assertTrue(tracingReceiver.getNbChunks() > 0);
        Assert.assertTrue(tracingReceiver.getOutputStream().size() > 0);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.TracingControllerTest#testTracingControllerCallbacks.
     * Modifications to this test should be reflected in that test as necessary.
     * See http://go/modifying-webview-cts.
     */
    @Test
    public void testTracingControllerCallbacks() throws Throwable {
        final TracingReceiver tracingReceiver = new TracingReceiver();
        runTracingTestWithCallbacks(tracingReceiver, mSingleThreadExecutor);
        PollingCheck.check("Tracing did not complete", POLLING_TIMEOUT,
                tracingReceiver.getCompleteCallable());
        Assert.assertTrue(tracingReceiver.getNbChunks() > 0);
        Assert.assertTrue(tracingReceiver.getOutputStream().size() > 0);
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.TracingControllerTest#testTracingStopFalseIfNotTracing.
     * Modifications to this test should be reflected in that test as necessary.
     * See http://go/modifying-webview-cts.
     */
    @Test
    public void testTracingStopFalseIfNotTracing() {
        Assert.assertFalse(mTracingController.stop(null, mSingleThreadExecutor));
        Assert.assertFalse(mTracingController.isTracing());
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.TracingControllerTest#testTracingCannotStartIfAlreadyTracing.
     * Modifications to this test should be reflected in that test as necessary.
     * See http://go/modifying-webview-cts.
     */
    @Test
    public void testTracingCannotStartIfAlreadyTracing() throws Exception {
        TracingConfig config = new TracingConfig.Builder().build();
        mTracingController.start(config);
        Assert.assertTrue(mTracingController.isTracing());
        try {
            mTracingController.start(config);
        } catch (IllegalStateException e) {
            // as expected
            return;
        }
        Assert.assertTrue(mTracingController.stop(null, mSingleThreadExecutor));
        Assert.fail("Tracing start should throw an exception "
                + "when attempting to start while already tracing");
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.TracingControllerTest#testTracingInvalidCategoriesPatternExclusion.
     * Modifications to this test should be reflected in that test as necessary.
     * See http://go/modifying-webview-cts.
     */
    @Test
    public void testTracingInvalidCategoriesPatternExclusion() {
        testInvalidCategoriesPattern(Arrays.asList("android_webview", "-blink"));
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.TracingControllerTest#testTracingInvalidCategoriesPatternComma.
     * Modifications to this test should be reflected in that test as necessary.
     * See http://go/modifying-webview-cts.
     */
    @Test
    public void testTracingInvalidCategoriesPatternComma() {
        testInvalidCategoriesPattern(Arrays.asList("android_webview, blink"));
    }

    @Test
    public void testIsSingleton() {
        Assert.assertSame(TracingController.getInstance(),
                TracingController.getInstance());
    }

    private void testInvalidCategoriesPattern(List<String> categories) {
        try {
            TracingConfig config = new TracingConfig.Builder()
                    .addCategories(categories)
                    .build();
            mTracingController.start(config);
        } catch (IllegalArgumentException e) {
            // as expected;
            Assert.assertFalse(mTracingController.isTracing());
            return;
        }

        Assert.fail("Tracing start should throw an exception due to invalid category pattern");
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.TracingControllerTest#testTracingWithNullConfig.
     * Modifications to this test should be reflected in that test as necessary.
     * See http://go/modifying-webview-cts.
     */
    @Test
    public void testTracingWithNullConfig() {
        try {
            mTracingController.start(null);
        } catch (IllegalArgumentException e) {
            // as expected
            Assert.assertFalse(mTracingController.isTracing());
            return;
        }
        Assert.fail("Tracing start should throw exception if TracingConfig is null");
    }

    private void runTracingTestWithCallbacks(TracingReceiver tracingReceiver, Executor executor) {
        Assert.assertNotNull(mTracingController);

        TracingConfig config = new TracingConfig.Builder()
                .addCategories(android.webkit.TracingConfig.CATEGORIES_WEB_DEVELOPER)
                .setTracingMode(android.webkit.TracingConfig.RECORD_CONTINUOUSLY)
                .build();
        mTracingController.start(config);
        Assert.assertTrue(mTracingController.isTracing());

        mWebViewOnUiThread.loadUrlAndWaitForCompletion("about:blank");
        Assert.assertTrue(mTracingController.stop(tracingReceiver, executor));
    }

    private void ensureTracingStopped() throws Exception {
        if (mTracingController == null) return;
        mTracingController.stop(null, mSingleThreadExecutor);
        Callable<Boolean> tracingStopped = new Callable<Boolean>() {
            @Override
            public Boolean call() {
                return !mTracingController.isTracing();
            }
        };
        PollingCheck.check("Tracing did not stop", POLLING_TIMEOUT, tracingStopped);
    }

    private ThreadFactory getCustomThreadFactory() {
        return new ThreadFactory() {
            private final AtomicInteger mThreadCount = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(EXECUTOR_THREAD_PREFIX + "_" + mThreadCount.incrementAndGet());
                return thread;
            }
        };
    }

    /**
     * This should remain functionally equivalent to
     * android.webkit.cts.TracingControllerTest.TracingReceiver.
     * Modifications to this test should be reflected in that test as necessary.
     * See http://go/modifying-webview-cts.
     */
    public static class TracingReceiver extends OutputStream {
        private int mChunkCount;
        private boolean mComplete;
        private ByteArrayOutputStream mOutputStream;

        public TracingReceiver() {
            mOutputStream = new ByteArrayOutputStream();
        }

        @Override
        public void write(byte[] chunk) {
            validateThread();
            mChunkCount++;
            try {
                mOutputStream.write(chunk);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            validateThread();
            mComplete = true;
        }

        @Override
        public void flush() {
            Assert.fail("flush should not be called");
        }

        @Override
        public void write(int b) {
            Assert.fail("write(int) should not be called");
        }

        @Override
        public void write(byte[] b, int off, int len) {
            Assert.fail("write(byte[], int, int) should not be called");
        }

        private void validateThread() {
            Assert.assertTrue(Thread.currentThread().getName().startsWith(EXECUTOR_THREAD_PREFIX));
        }

        int getNbChunks() {
            return mChunkCount;

        }
        boolean getComplete() {
            return mComplete;
        }

        Callable<Boolean> getCompleteCallable() {
            return new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    return getComplete();
                }
            };
        }

        ByteArrayOutputStream getOutputStream() {
            return mOutputStream;
        }
    }
}
