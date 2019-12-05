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

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * A class for a message from {@link MessageLibraryService}.
 * @hide
 */
@RestrictTo(LIBRARY)
public class MessageInfo {
    private static final String KEY_SENDER_CONTACT_ID =
            "androidx.message.browser.MessageInfo.SENDER_CONTACT_ID";
    private static final String KEY_CONVERSATION_ID =
            "androidx.message.browser.MessageInfo.CONVERSATION_ID";
    private static final String KEY_TIMESTAMP = "androidx.message.browser.MessageInfo.TIMESTAMP";
    private static final String KEY_MESSAGE = "androidx.message.browser.MessageInfo.MESSAGE";
    private static final String KEY_EXTRAS = "androidx.message.browser.MessageInfo.EXTRAS";

    @NonNull
    public String senderContactId;
    @NonNull
    public String conversId;
    public long timestamp = -1;

    @Nullable
    public String message;
    @Nullable
    public Bundle extras;

    static MessageInfo fromBundle(Bundle bundle) {
        MessageInfo msg = new MessageInfo();
        msg.senderContactId = bundle.getString(KEY_SENDER_CONTACT_ID);
        msg.conversId = bundle.getString(KEY_CONVERSATION_ID);
        msg.timestamp = bundle.getLong(KEY_TIMESTAMP);
        msg.message = bundle.getString(KEY_MESSAGE);
        msg.extras = bundle.getBundle(KEY_EXTRAS);
        return msg;
    }

    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_SENDER_CONTACT_ID, senderContactId);
        bundle.putString(KEY_CONVERSATION_ID, conversId);
        bundle.putLong(KEY_TIMESTAMP, timestamp);
        bundle.putString(KEY_MESSAGE, message);
        bundle.putBundle(KEY_EXTRAS, bundle);
        return bundle;
    }
}
