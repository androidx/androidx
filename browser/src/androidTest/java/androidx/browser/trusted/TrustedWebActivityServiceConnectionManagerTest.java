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

import android.content.Context;
import android.net.Uri;

import androidx.browser.customtabs.EnableComponentsTestRule;
import androidx.browser.customtabs.TestActivity;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.testutils.PollingCheck;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TrustedWebActivityServiceConnectionManagerTest {
    private static final String ORIGIN = "https://localhost:3080";
    private static final Uri GOOD_SCOPE = Uri.parse("https://www.example.com/notifications");
    private static final Uri BAD_SCOPE = Uri.parse("https://www.notexample.com");

    private TrustedWebActivityServiceConnectionManager mManager;
    private Context mContext;

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
        mManager = new TrustedWebActivityServiceConnectionManager(mContext);

        TrustedWebActivityServiceConnectionManager
                .registerClient(mContext, ORIGIN, mContext.getPackageName());
    }

    @After
    public void tearDown() {
        mManager.unbindAllConnections();
    }

    @Test
    public void testConnection() {
        final AtomicBoolean connected = new AtomicBoolean();
        boolean delegated = mManager.execute(GOOD_SCOPE, ORIGIN,
                service -> {
                    assertEquals(TestTrustedWebActivityService.SMALL_ICON_ID,
                            service.getSmallIconId());
                    connected.set(true);
                });
        assertTrue(delegated);

        PollingCheck.waitFor(connected::get);
    }

    @Test
    public void testNoService() {
        boolean delegated = mManager.execute(BAD_SCOPE, ORIGIN, service -> { });
        assertFalse(delegated);
    }

    @Test
    public void testMultipleExecutions() {
        final AtomicInteger count = new AtomicInteger();

        mManager.execute(GOOD_SCOPE, ORIGIN, service -> count.incrementAndGet());
        mManager.execute(GOOD_SCOPE, ORIGIN, service -> count.incrementAndGet());

        PollingCheck.waitFor(() -> count.get() == 2);
    }
}
