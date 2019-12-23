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

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.graphics.drawable.IconCompat;
import androidx.versionedparcelable.ParcelUtils;

import java.util.ArrayList;

/**
 * A class for a conversation from {@link MessageLibraryService}.
 * @hide
 */
@RestrictTo(LIBRARY)
public class ConversationInfo {
    private static final String KEY_ID = "androidx.message.browser.ConversationInfo.ID";
    private static final String KEY_PARENT_ID =
            "androidx.message.browser.ConversationInfo.PARENT_ID";
    private static final String KEY_DISPLAY_NAME =
            "androidx.message.browser.ConversationInfo.DISPLAY_NAME";
    private static final String KEY_DISPLAY_ICON =
            "androidx.message.browser.ConversationInfo.DISPLAY_ICON";
    private static final String KEY_EXTRAS = "androidx.message.browser.ConversationInfo.EXTRAS";
    private static final String KEY_ATTENDEES =
            "androidx.message.browser.ConversationInfo.ATTENDEES";
    private static final String KEY_SUB_CONVERSATION_IDS =
            "androidx.message.browser.ConversationInfo.SUB_CONVERSATION_IDS";
    private static final String KEY_LOADED_MESSAGE_SIZE =
            "androidx.message.browser.ConversationInfo.LOADED_MESSAGE_SIZE";
    private static final String KEY_NEWEST_MESSAGE_TIMESTAMP =
            "androidx.message.browser.ConversationInfo.NEWEST_MESSAGE_TIMESTAMP";
    private static final String KEY_OLDEST_MESSAGE_TIMESTAMP =
            "androidx.message.browser.ConversationInfo.OLDEST_MESSAGE_TIMESTAMP";

    @NonNull
    public String id;
    @Nullable
    public String parentId;
    @Nullable
    public String displayName;
    @Nullable
    public IconCompat displayIcon;
    @Nullable
    public Bundle extras;

    @Nullable
    public ArrayList<String> attendees;
    @Nullable
    public ArrayList<String> subConversationIds;

    public int loadedMessageSize = 0;
    public long newestMsgTimestmap = -1;
    public long oldestMsgTimestmap = -1;

    // TODO: Find a way to remove @SuppressLint
    @SuppressLint("RestrictedApi")
    static ConversationInfo fromBundle(Bundle bundle) {
        ConversationInfo conversInfo = new ConversationInfo();
        conversInfo.id = bundle.getString(KEY_ID);
        conversInfo.parentId = bundle.getString(KEY_PARENT_ID);
        conversInfo.displayName = bundle.getString(KEY_DISPLAY_NAME);
        conversInfo.displayIcon =
                ParcelUtils.fromParcelable(bundle.getParcelable(KEY_DISPLAY_ICON));
        conversInfo.extras = bundle.getBundle(KEY_EXTRAS);
        conversInfo.attendees = bundle.getStringArrayList(KEY_ATTENDEES);
        conversInfo.subConversationIds = bundle.getStringArrayList(KEY_SUB_CONVERSATION_IDS);
        conversInfo.loadedMessageSize = bundle.getInt(KEY_LOADED_MESSAGE_SIZE, 0);
        conversInfo.newestMsgTimestmap = bundle.getLong(KEY_NEWEST_MESSAGE_TIMESTAMP, -1);
        conversInfo.oldestMsgTimestmap = bundle.getLong(KEY_OLDEST_MESSAGE_TIMESTAMP, -1);
        return conversInfo;
    }

    // TODO: Find a way to remove @SuppressLint
    @SuppressLint("RestrictedApi")
    Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_ID, id);
        bundle.putString(KEY_PARENT_ID, parentId);
        bundle.putString(KEY_DISPLAY_NAME, displayName);
        bundle.putParcelable(KEY_DISPLAY_ICON, ParcelUtils.toParcelable(displayIcon));
        bundle.putBundle(KEY_EXTRAS, bundle);
        bundle.putStringArrayList(KEY_ATTENDEES, attendees);
        bundle.putStringArrayList(KEY_SUB_CONVERSATION_IDS, subConversationIds);
        bundle.putInt(KEY_LOADED_MESSAGE_SIZE, loadedMessageSize);
        bundle.putLong(KEY_NEWEST_MESSAGE_TIMESTAMP, newestMsgTimestmap);
        bundle.putLong(KEY_OLDEST_MESSAGE_TIMESTAMP, oldestMsgTimestmap);
        return bundle;
    }
}
