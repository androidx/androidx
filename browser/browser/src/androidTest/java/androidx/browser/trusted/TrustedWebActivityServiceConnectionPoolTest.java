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
import android.os.RemoteException;

import androidx.browser.customtabs.EnableComponentsTestRule;
import androidx.browser.customtabs.TestActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.testutils.PollingCheck;

import com.google.common.util.concurrent.ListenableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(AndroidJUnit4.class)
@MediumTest
@SuppressWarnings("deprecation") /* AsyncTask */
public class TrustedWebActivityServiceConnectionPoolTest {
    private static final Uri GOOD_SCOPE = Uri.parse("https://www.example.com/notifications");
    private static final Uri BAD_SCOPE = Uri.parse("https://www.notexample.com");

    private final Set<Token> mTrustedPackages = new HashSet<>();

    private TrustedWebActivityServiceConnectionPool mManager;

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
        Context context = ApplicationProvider.getApplicationContext();
        mManager = TrustedWebActivityServiceConnectionPool.create(context);
        mTrustedPackages.add(Token.create(context.getPackageName(), context.getPackageManager()));
    }

    @After
    public void tearDown() {
        mManager.unbindAllConnections();
    }

    @Ignore("Test disabled due to flakiness, see b/182415874")
    @Test
    public void testConnection() {
        final AtomicBoolean connected = new AtomicBoolean();

        ListenableFuture<TrustedWebActivityServiceConnection> serviceFuture =
                mManager.connect(GOOD_SCOPE, mTrustedPackages,
                        android.os.AsyncTask.THREAD_POOL_EXECUTOR);

        serviceFuture.addListener(() -> {
            try {
                assertEquals(TestTrustedWebActivityService.SMALL_ICON_ID,
                        serviceFuture.get().getSmallIconId());
                connected.set(true);
            } catch (RemoteException | ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }, android.os.AsyncTask.THREAD_POOL_EXECUTOR);

        PollingCheck.waitFor(connected::get);
    }

    @Test
    public void testNoService() {
        assertFalse(mManager.serviceExistsForScope(BAD_SCOPE, mTrustedPackages));

        ListenableFuture<TrustedWebActivityServiceConnection> serviceFuture =
                mManager.connect(BAD_SCOPE, mTrustedPackages,
                        android.os.AsyncTask.THREAD_POOL_EXECUTOR);

        try {
            serviceFuture.get();
            fail();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof IllegalArgumentException);
        } catch (InterruptedException e) {
            fail();
        }
    }
}
