/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.browser.customtabs.testutil;

import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.content.Context;

import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * TestRule that helps establishing a connection to CustomTabsService.
 */
public class CustomTabConnectionRule extends TestWatcher {

    private final CountDownLatch mConnectionLatch = new CountDownLatch(1);

    private CustomTabsSession mSession;

    private Context mContext;

    private CustomTabsServiceConnection mConnection = new CustomTabsServiceConnection() {
        @Override
        public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
            mSession = client.newSession(null);
            mConnectionLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mSession = null;
        }
    };

    /**
     * Binds the CustomTabsService, creates a {@link CustomTabsSession} and returns it.
     */
    public CustomTabsSession establishSessionBlocking(Context context) {
        mContext = context;
        if (!CustomTabsClient.bindCustomTabsService(context, context.getPackageName(),
                mConnection)) {
            fail("Failed to bind the service");
            return null;
        }
        boolean success = false;
        try {
            success = mConnectionLatch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) { }
        if (!success) {
            fail("Failed to connect to service");
            return null;
        }
        return mSession;
    }

    @Override
    protected void finished(Description description) {
        if (mContext != null && mSession != null) {
            try {
                mContext.unbindService(mConnection);
            } catch (RuntimeException e) { } // Service might be disabled at this point
        }
    }
}
