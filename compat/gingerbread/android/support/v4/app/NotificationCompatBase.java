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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@RequiresApi(9)
@TargetApi(9)
public class NotificationCompatBase {
    private static Method sSetLatestEventInfo;

    public static abstract class Action {
        public abstract int getIcon();
        public abstract CharSequence getTitle();
        public abstract PendingIntent getActionIntent();
        public abstract Bundle getExtras();
        public abstract RemoteInputCompatBase.RemoteInput[] getRemoteInputs();
        public abstract boolean getAllowGeneratedReplies();

        public interface Factory {
            Action build(int icon, CharSequence title, PendingIntent actionIntent,
                    Bundle extras, RemoteInputCompatBase.RemoteInput[] remoteInputs,
                    boolean allowGeneratedReplies);
            public Action[] newArray(int length);
        }
    }

    public static abstract class UnreadConversation {
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

    public static Notification add(Notification notification, Context context,
            CharSequence contentTitle, CharSequence contentText, PendingIntent contentIntent,
            PendingIntent fullScreenIntent) {
        if (sSetLatestEventInfo == null) {
            try {
                sSetLatestEventInfo = Notification.class.getMethod("setLatestEventInfo",
                        Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
            } catch (NoSuchMethodException e) {
                // This method was @removed, so it must exist on later
                // versions even if it's not in public API.
                throw new RuntimeException(e);
            }
        }

        try {
            sSetLatestEventInfo.invoke(notification, context,
                    contentTitle, contentText, contentIntent);
        } catch (IllegalAccessException | InvocationTargetException e) {
            // This method was @removed, so it must be invokable on later
            // versions even if it's not in public API.
            throw new RuntimeException(e);
        }

        notification.fullScreenIntent = fullScreenIntent;
        return notification;
    }
}
