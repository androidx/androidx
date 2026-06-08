/*
 * Copyright 2026 The Android Open Source Project
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

import android.os.CancellationSignal;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.webkit.test.common.WebkitUtils;

import org.jspecify.annotations.NonNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.LinkedBlockingQueue;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PrefetchResultTest {
    private static final long TIMEOUT_SECONDS = 5;
    private static final String SERVER_PATH = "/test_path";

    private Profile mDefaultProfile;

    private static class TestOutcomeReceiver implements
            WebViewOutcomeReceiver<PrefetchResult, PrefetchException> {
        private final LinkedBlockingQueue<PrefetchResult> mQueue;

        TestOutcomeReceiver() {
            mQueue = new LinkedBlockingQueue<>();
        }

        @Override
        public void onResult(PrefetchResult result) {
            mQueue.add(result);
        }

        @Override
        public void onError(@NonNull PrefetchException error) {
            Assert.fail(error.getMessage());
        }

        public PrefetchResult waitForNextQueueElement() {
            return WebkitUtils.waitForNextQueueElement(mQueue);
        }
    }

    @Before
    public void setUp() throws Exception {
        WebkitUtils.checkFeature(WebViewFeature.PROFILE_URL_PREFETCH);
        WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE);
        WebkitUtils.checkFeature(WebViewFeature.PREFETCH_CACHE_V1);
        WebkitUtils.onMainThreadSync(() -> {
            mDefaultProfile = ProfileStore.getInstance().getProfile(Profile.DEFAULT_PROFILE_NAME);
        });
    }

    @After
    public void tearDown() throws Exception {
        // Clean up if necessary. Prefetch cache cleanup might be needed if it persists.
        // But usually profiles are isolated or reset.
    }

    @Test
    public void testPrefetchResultSuccess() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            MockWebServerHttpsUtil.enableHttps(server);
            server.start();

            HttpUrl url = server.url(SERVER_PATH);
            server.enqueue(new MockResponse().setBody("hello"));

            TestOutcomeReceiver callback = new TestOutcomeReceiver();

            mDefaultProfile.getPrefetchCache().prefetchUrlAsync(url.toString(),
                    new CancellationSignal(),
                    Runnable::run,
                    callback
            );

            PrefetchResult result = callback.waitForNextQueueElement();
            Assert.assertNotNull("Timed out waiting for prefetch result", result);
            Assert.assertFalse("Expected wasDuplicate to be false", result.wasDuplicate());
        }
    }

    @Test
    public void testPrefetchResultDuplicate() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            MockWebServerHttpsUtil.enableHttps(server);
            server.start();

            HttpUrl url = server.url(SERVER_PATH);
            server.enqueue(new MockResponse().setBody("hello"));

            TestOutcomeReceiver callback1 = new TestOutcomeReceiver();
            TestOutcomeReceiver callback2 = new TestOutcomeReceiver();

            // First prefetch
            mDefaultProfile.getPrefetchCache()
                    .prefetchUrlAsync(
                            url.toString(),
                            new CancellationSignal(),
                            Runnable::run,
                            callback1
                    );

            // Second prefetch (duplicate)
            mDefaultProfile.getPrefetchCache()
                    .prefetchUrlAsync(
                            url.toString(),
                            new CancellationSignal(),
                            Runnable::run,
                            callback2
                    );

            PrefetchResult result1 = callback1.waitForNextQueueElement();
            Assert.assertNotNull("Timed out waiting for first prefetch result", result1);
            Assert.assertFalse("Expected first prefetch wasDuplicate to be false",
                    result1.wasDuplicate());

            PrefetchResult result2 = callback2.waitForNextQueueElement();

            Assert.assertNotNull("Timed out waiting for second prefetch result", result2);
            Assert.assertTrue("Expected second prefetch wasDuplicate to be true",
                    result2.wasDuplicate());
        }
    }
}
