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

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import androidx.testutils.PollingCheck;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link PostMessageServiceConnection} with no {@link CustomTabsService} component.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PostMessageServiceConnectionTest {
    @Rule
    public final EnableComponentsTestRule mEnableComponents = new EnableComponentsTestRule(
            PostMessageService.class
    );

    private TestCustomTabsCallback mCallback;
    private Context mContext;
    private PostMessageServiceConnection mConnection;
    private boolean mServiceConnected;

    @Before
    public void setup() {
        mCallback = new TestCustomTabsCallback();
        mContext = ApplicationProvider.getApplicationContext();

        CustomTabsSessionToken token = new CustomTabsSessionToken(mCallback.getStub(), null);
        mConnection = new PostMessageServiceConnection(token) {
            @Override
            public void onPostMessageServiceConnected() {
                mServiceConnected = true;
            }

            @Override
            public void onPostMessageServiceDisconnected() {
                mServiceConnected = false;
            }
        };

        mConnection.bindSessionToPostMessageService(mContext, mContext.getPackageName());
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return mServiceConnected;
            }
        });
    }

    @Test
    public void testNotifyChannelCreationAndSendMessages() {
        mConnection.notifyMessageChannelReady(null);
        assertTrue(mCallback.isMessageChannelReady());

        mConnection.postMessage("message1", null);
        assertEquals(mCallback.getMessages().size(), 1);

        mConnection.postMessage("message2", null);
        assertEquals(mCallback.getMessages().size(), 2);
    }

    @Test
    public void dontUnbindTwice() throws Throwable {
        mConnection.cleanup(mContext);
        mConnection.cleanup(mContext);
    }
}
