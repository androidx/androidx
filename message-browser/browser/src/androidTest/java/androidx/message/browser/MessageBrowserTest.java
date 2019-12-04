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

package androidx.message.browser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MessageBrowser}.
 */

public class MessageBrowserTest extends MessageBrowserTestBase {
    static final int TIMEOUT_MS = 1000;

    private MessageBrowser.Builder mMsgBrowserBuilder;
    private MessageBrowser.BrowserCallback mMsgCallback;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        mMsgBrowserBuilder = new MessageBrowser.Builder(mContext,
                new ComponentName(mContext.getPackageName(),
                        MockMessageLibraryService.class.getName()));
    }

    @After
    public void cleanUp() throws Exception {

    }

    @Test
    public void testConnect() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        MessageBrowser.BrowserCallback callback = new MessageBrowser.BrowserCallback() {
            public void onConnected(@NonNull MessageBrowser browser,
                    @NonNull MessageCommandGroup allowedCommands) {
                latch.countDown();
            }
        };
        mMsgBrowserBuilder.setBrowserCallback(sHandlerExecutor, callback);
        MessageBrowser browser = mMsgBrowserBuilder.build();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        browser.close();
    }

    @Test
    public void testConnectionRefused() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        MessageBrowser.BrowserCallback callback = new MessageBrowser.BrowserCallback() {
            public void onDisconnected(@NonNull MessageBrowser browser) {
                latch.countDown();
            }
        };
        Bundle connectionHints = new Bundle();
        connectionHints.putBoolean(MockMessageLibraryService.KEY_REFUSE_CONNECTION, true);
        mMsgBrowserBuilder.setConnectionHints(connectionHints);
        mMsgBrowserBuilder.setBrowserCallback(sHandlerExecutor, callback);
        MessageBrowser browser = mMsgBrowserBuilder.build();
        assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        browser.close();
    }

    @Test
    public void testServiceCrashWhileConnection() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        MessageBrowser.BrowserCallback callback = new MessageBrowser.BrowserCallback() {
            // TODO(sungsoo): check onDisconnect instead.
            public void onConnected(@NonNull MessageBrowser browser,
                    @NonNull MessageCommandGroup allowedCommands) {
                latch.countDown();
            }
        };
        Bundle connectionHints = new Bundle();
        connectionHints.putBoolean(MockMessageLibraryService.KEY_CRASH_CONNECTION, true);
        mMsgBrowserBuilder.setConnectionHints(connectionHints);
        mMsgBrowserBuilder.setBrowserCallback(sHandlerExecutor, callback);
        MessageBrowser browser = mMsgBrowserBuilder.build();
        assertFalse(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        browser.close();
    }
}
