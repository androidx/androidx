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

import java.util.ArrayList;
import java.util.List;
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
            public void onConnected(@NonNull MessageBrowser browser) {
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

    @Test
    public void testMultiConnection() throws Exception {
        BrowserCallbackForTestMultiConnection callback =
                new BrowserCallbackForTestMultiConnection();
        mMsgBrowserBuilder.setBrowserCallback(sHandlerExecutor, callback);
        MessageBrowser browser1 = mMsgBrowserBuilder.build();
        MessageBrowser browser2 = mMsgBrowserBuilder.build();
        callback.setBrowsers(browser1, browser2);
        assertTrue(callback.connectedLatch1.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.connectedLatch2.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        browser1.close();
        assertTrue(callback.disconnectedLatch1.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        browser2.close();
        assertTrue(callback.disconnectedLatch2.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private class BrowserCallbackForTestMultiConnection extends MessageBrowser.BrowserCallback {
        public MessageBrowser browser1;
        public final CountDownLatch connectedLatch1 = new CountDownLatch(1);
        public final CountDownLatch disconnectedLatch1 = new CountDownLatch(1);

        public MessageBrowser browser2;
        public final CountDownLatch connectedLatch2 = new CountDownLatch(1);
        public final CountDownLatch disconnectedLatch2 = new CountDownLatch(1);

        private List<MessageBrowser> mConnectedBrowsers;

        public void setBrowsers(MessageBrowser browser1, MessageBrowser browser2) {
            this.browser1 = browser1;
            this.browser2 = browser2;

            if (mConnectedBrowsers != null) {
                for (MessageBrowser browser : mConnectedBrowsers) {
                    if (browser == this.browser1) {
                        connectedLatch1.countDown();
                    } else if (browser == this.browser2) {
                        connectedLatch2.countDown();
                    }
                }
            }
            mConnectedBrowsers = null;
        }

        public void onConnected(@NonNull MessageBrowser browser,
                @NonNull MessageCommandGroup allowedCommands) {
            if (browser == browser1) {
                connectedLatch1.countDown();
            } else if (browser == browser2) {
                connectedLatch2.countDown();
            } else {
                if (mConnectedBrowsers != null) {
                    mConnectedBrowsers = new ArrayList<>();
                }
                mConnectedBrowsers.add(browser);
            }
        }

        public void onDisconnected(@NonNull MessageBrowser browser) {
            if (browser == browser1) {
                disconnectedLatch1.countDown();
            } else if (browser == browser2) {
                disconnectedLatch2.countDown();
            }
        }
    }
}
