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

package androidx.browser.trusted;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.browser.customtabs.EnableComponentsTestRule;
import androidx.browser.customtabs.TestActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.testutils.PollingCheck;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class TrustedWebActivityServiceConnectionPoolTest {
    private static final String ORIGIN = "https://localhost:3080";
    private static final Uri GOOD_SCOPE = Uri.parse("https://www.example.com/notifications");
    private static final Uri BAD_SCOPE = Uri.parse("https://www.notexample.com");

    private TrustedWebActivityServiceConnectionPool mManager;
    private Context mContext;

    // TODO: Test security exception.

    @Rule
    public final VerifiedProviderTestRule mVerifiedProvider = new VerifiedProviderTestRule();
    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            TestTrustedWebActivityService.class,
            TestActivity.class
    );

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mManager = TrustedWebActivityServiceConnectionPool.create(mContext);

        TrustedWebActivityServiceConnectionPool
                .registerClient(mContext, ORIGIN, mContext.getPackageName());
    }

    @After
    public void tearDown() {
        mManager.unbindAllConnections();
    }

    @Test
    public void testConnection() {
        final AtomicBoolean connected = new AtomicBoolean();

        ListenableFuture<TrustedWebActivityServiceConnection> serviceFuture =
                mManager.connect(GOOD_SCOPE, ORIGIN, AsyncTask.THREAD_POOL_EXECUTOR);

        serviceFuture.addListener(() -> {
            try {
                assertEquals(TestTrustedWebActivityService.SMALL_ICON_ID,
                        serviceFuture.get().getSmallIconId());
                connected.set(true);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, AsyncTask.THREAD_POOL_EXECUTOR);

        PollingCheck.waitFor(connected::get);
    }

    @Test
    public void testNoService() {
        assertFalse(mManager.serviceExistsForScope(BAD_SCOPE, ORIGIN));

        ListenableFuture<TrustedWebActivityServiceConnection> serviceFuture =
                mManager.connect(BAD_SCOPE, ORIGIN, AsyncTask.THREAD_POOL_EXECUTOR);

        try {
            serviceFuture.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        } catch (InterruptedException e) {
            fail();
        }
    }

    @Test
    public void testMultipleExecutions() {
        final AtomicInteger count = new AtomicInteger();

        mManager.connect(GOOD_SCOPE, ORIGIN, AsyncTask.THREAD_POOL_EXECUTOR)
                .addListener(count::incrementAndGet, AsyncTask.THREAD_POOL_EXECUTOR);
        mManager.connect(GOOD_SCOPE, ORIGIN, AsyncTask.THREAD_POOL_EXECUTOR)
                .addListener(count::incrementAndGet, AsyncTask.THREAD_POOL_EXECUTOR);

        PollingCheck.waitFor(() -> count.get() == 2);
    }
}
