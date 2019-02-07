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

package androidx.browser.customtabs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.ServiceTestRule;
import androidx.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for a complete loop between a browser side {@link CustomTabsService}
 * and a client side {@link PostMessageService}. Both services are bound to through
 * {@link ServiceTestRule}, but {@link CustomTabsCallback#extraCallback} is used to link browser
 * side actions.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class PostMessageTest {
    @Rule
    public final ServiceTestRule mServiceRule;
    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule;
    private TestCustomTabsCallback mCallback;
    private Context mContext;
    private CustomTabsServiceConnection mCustomTabsServiceConnection;
    private PostMessageServiceConnection mPostMessageServiceConnection;
    private AtomicBoolean mCustomTabsServiceConnected;
    private boolean mPostMessageServiceConnected;
    private CustomTabsSession mSession;

    public PostMessageTest() {
        mActivityTestRule = new ActivityTestRule<TestActivity>(TestActivity.class);
        mServiceRule = new ServiceTestRule();
        mCustomTabsServiceConnected = new AtomicBoolean(false);
    }


    @Before
    public void setup() {
        // Bind to PostMessageService only after CustomTabsService sends the callback to do so. This
        // callback is sent after requestPostMessageChannel is called.
        mCallback = new TestCustomTabsCallback() {
            @Override
            public void extraCallback(String callbackName, Bundle args) {
                if (TestCustomTabsService.CALLBACK_BIND_TO_POST_MESSAGE.equals(callbackName)) {
                    // This gets run on the UI thread, where mServiceRule.bindService will not work.
                    AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Intent postMessageServiceIntent = new Intent();
                                postMessageServiceIntent.setClassName(mContext.getPackageName(),
                                        PostMessageService.class.getName());
                                mServiceRule.bindService(postMessageServiceIntent,
                                        mPostMessageServiceConnection, Context.BIND_AUTO_CREATE);
                            } catch (TimeoutException e) {
                                fail();
                            }
                        }
                    });
                }
            }
        };
        mContext = mActivityTestRule.getActivity();
        mCustomTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                mCustomTabsServiceConnected.set(true);
                mSession = client.newSession(mCallback);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mCustomTabsServiceConnected.set(false);
            }
        };
        mPostMessageServiceConnection = new PostMessageServiceConnection(
                new CustomTabsSessionToken(mCallback.getStub())) {
            @Override
            public void onPostMessageServiceConnected() {
                mPostMessageServiceConnected = true;
            }

            @Override
            public void onPostMessageServiceDisconnected() {
                mPostMessageServiceConnected = false;
            }
        };
        Intent customTabsServiceIntent = new Intent();
        customTabsServiceIntent.setClassName(
                mContext.getPackageName(), TestCustomTabsService.class.getName());
        try {
            mServiceRule.bindService(customTabsServiceIntent,
                    mCustomTabsServiceConnection, Context.BIND_AUTO_CREATE);
        } catch (TimeoutException e) {
            fail();
        }
    }

    @Test
    public void testCustomTabsConnection() {
        PollingCheck.waitFor(500, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mCustomTabsServiceConnected.get();
            }
        });
        assertTrue(mCustomTabsServiceConnected.get());
        assertTrue(mSession.requestPostMessageChannel(Uri.EMPTY));
        assertEquals(CustomTabsService.RESULT_SUCCESS, mSession.postMessage("", null));
        PollingCheck.waitFor(500, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mPostMessageServiceConnected;
            }
        });
        assertTrue(mPostMessageServiceConnected);
    }
}
