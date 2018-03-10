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

import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;

import java.util.ArrayList;

/**
 * A test class to check the incoming messages through {@link CustomTabsCallback}.
 */
public class TestCustomTabsCallback extends CustomTabsCallback {
    private boolean mOnMessageChannelReady;
    private ArrayList<String> mMessageList = new ArrayList<>();
    private ICustomTabsCallback.Stub mWrapper = new ICustomTabsCallback.Stub() {
        @Override
        public void onNavigationEvent(final int navigationEvent, final Bundle extras) {
            TestCustomTabsCallback.this.onNavigationEvent(navigationEvent, extras);
        }

        @Override
        public void extraCallback(final String callbackName, final Bundle args)
                throws RemoteException {
            TestCustomTabsCallback.this.extraCallback(callbackName, args);
        }

        @Override
        public void onMessageChannelReady(final Bundle extras)
                throws RemoteException {
            TestCustomTabsCallback.this.onMessageChannelReady(extras);
        }

        @Override
        public void onPostMessage(final String message, final Bundle extras)
                throws RemoteException {
            TestCustomTabsCallback.this.onPostMessage(message, extras);
        }

        @Override
        public void onRelationshipValidationResult(int relation, Uri origin, boolean result,
                                                   Bundle extras) throws RemoteException {
            TestCustomTabsCallback.this.onRelationshipValidationResult(
                    relation, origin, result, extras);
        }
    };

    /* package */ ICustomTabsCallback getStub() {
        return mWrapper;
    }

    @Override
    public void onMessageChannelReady(Bundle extras) {
        mOnMessageChannelReady = true;
    }

    /**
     * @return Whether the message channel is ready.
     */
    public boolean isMessageChannelReady() {
        return mOnMessageChannelReady;
    }

    @Override
    public void onPostMessage(String message, Bundle extras) {
        mMessageList.add(message);
    }

    /**
     * @return A list of messages that have been sent so far.
     */
    public ArrayList<String> getMessages() {
        return mMessageList;
    }
}
