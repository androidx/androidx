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

import android.content.Context;
import android.content.Intent;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

/**
 * Tests for {@link PostMessageServiceConnection} with no {@link CustomTabsService} component.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PostMessageServiceConnectionTest {
    @Rule
    public final ServiceTestRule mServiceRule;
    @Rule
    public final ActivityTestRule<TestActivity> mActivityTestRule;
    private TestCustomTabsCallback mCallback;
    private Context mContext;
    private PostMessageServiceConnection mConnection;
    private boolean mServiceConnected;


    public PostMessageServiceConnectionTest() {
        mActivityTestRule = new ActivityTestRule<TestActivity>(TestActivity.class);
        mServiceRule = new ServiceTestRule();
    }

    @Before
    public void setup() {
        mCallback = new TestCustomTabsCallback();
        mContext = mActivityTestRule.getActivity();
        mConnection = new PostMessageServiceConnection(
                new CustomTabsSessionToken(mCallback.getStub())) {
            @Override
            public void onPostMessageServiceConnected() {
                mServiceConnected = true;
            }

            @Override
            public void onPostMessageServiceDisconnected() {
                mServiceConnected = false;
            }
        };
        Intent intent = new Intent();
        intent.setClassName(mContext.getPackageName(), PostMessageService.class.getName());
        try {
            mServiceRule.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        } catch (TimeoutException e) {
            fail();
        }
    }

    @Test
    public void testNotifyChannelCreationAndSendMessages() {
        PollingCheck.waitFor(500, new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mServiceConnected;
            }
        });
        assertTrue(mServiceConnected);
        mConnection.notifyMessageChannelReady(null);
        assertTrue(mCallback.isMessageChannelReady());
        mConnection.postMessage("message1", null);
        assertEquals(mCallback.getMessages().size(), 1);
        mConnection.postMessage("message2", null);
        assertEquals(mCallback.getMessages().size(), 2);
    }
}
