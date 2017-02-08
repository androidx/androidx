/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.support.v4.app;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.app.PendingIntent;
import android.os.Bundle;
import android.support.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class NotificationCompatBase {
    public abstract static class Action {
        public abstract int getIcon();
        public abstract CharSequence getTitle();
        public abstract PendingIntent getActionIntent();
        public abstract Bundle getExtras();
        public abstract RemoteInputCompatBase.RemoteInput[] getRemoteInputs();
        /** Returns RemoteInputs that ONLY accept data results, not text. */
        public abstract RemoteInputCompatBase.RemoteInput[] getDataOnlyRemoteInputs();
        public abstract boolean getAllowGeneratedReplies();

        public interface Factory {
            Action build(int icon, CharSequence title, PendingIntent actionIntent,
                    Bundle extras, RemoteInputCompatBase.RemoteInput[] remoteInputs,
                    RemoteInputCompatBase.RemoteInput[] dataOnlyRemoteInputs,
                    boolean allowGeneratedReplies);
            Action[] newArray(int length);
        }
    }

    public abstract static class UnreadConversation {
        abstract String[] getParticipants();
        abstract String getParticipant();
        abstract String[] getMessages();
        abstract RemoteInputCompatBase.RemoteInput getRemoteInput();
        abstract PendingIntent getReplyPendingIntent();
        abstract PendingIntent getReadPendingIntent();
        abstract long getLatestTimestamp();

        public interface Factory {
            UnreadConversation build(String[] messages,
                    RemoteInputCompatBase.RemoteInput remoteInput,
                    PendingIntent replyPendingIntent, PendingIntent readPendingIntent,
                    String[] participants, long latestTimestamp);
        }
    }
}
