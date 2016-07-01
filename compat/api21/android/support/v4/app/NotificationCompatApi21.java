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

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.RemoteViews;

import java.util.ArrayList;

class NotificationCompatApi21 {

    public static final String CATEGORY_CALL = Notification.CATEGORY_CALL;
    public static final String CATEGORY_MESSAGE = Notification.CATEGORY_MESSAGE;
    public static final String CATEGORY_EMAIL = Notification.CATEGORY_EMAIL;
    public static final String CATEGORY_EVENT = Notification.CATEGORY_EVENT;
    public static final String CATEGORY_PROMO = Notification.CATEGORY_PROMO;
    public static final String CATEGORY_ALARM = Notification.CATEGORY_ALARM;
    public static final String CATEGORY_PROGRESS = Notification.CATEGORY_PROGRESS;
    public static final String CATEGORY_SOCIAL = Notification.CATEGORY_SOCIAL;
    public static final String CATEGORY_ERROR = Notification.CATEGORY_ERROR;
    public static final String CATEGORY_TRANSPORT = Notification.CATEGORY_TRANSPORT;
    public static final String CATEGORY_SYSTEM = Notification.CATEGORY_SYSTEM;
    public static final String CATEGORY_SERVICE = Notification.CATEGORY_SERVICE;
    public static final String CATEGORY_RECOMMENDATION = Notification.CATEGORY_RECOMMENDATION;
    public static final String CATEGORY_STATUS = Notification.CATEGORY_STATUS;

    private static final String KEY_AUTHOR = "author";
    private static final String KEY_TEXT = "text";
    private static final String KEY_MESSAGES = "messages";
    private static final String KEY_REMOTE_INPUT = "remote_input";
    private static final String KEY_ON_REPLY = "on_reply";
    private static final String KEY_ON_READ = "on_read";
    private static final String KEY_PARTICIPANTS = "participants";
    private static final String KEY_TIMESTAMP = "timestamp";

    public static class Builder implements NotificationBuilderWithBuilderAccessor,
            NotificationBuilderWithActions {
        private Notification.Builder b;
        private Bundle mExtras;
        private RemoteViews mContentView;
        private RemoteViews mBigContentView;
        private RemoteViews mHeadsUpContentView;

        public Builder(Context context, Notification n,
                CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
                RemoteViews tickerView, int number,
                PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
                int progressMax, int progress, boolean progressIndeterminate, boolean showWhen,
                boolean useChronometer, int priority, CharSequence subText, boolean localOnly,
                String category, ArrayList<String> people, Bundle extras, int color,
                int visibility, Notification publicVersion, String groupKey, boolean groupSummary,
                String sortKey, RemoteViews contentView, RemoteViews bigContentView,
                RemoteViews headsUpContentView) {
            b = new Notification.Builder(context)
                    .setWhen(n.when)
                    .setShowWhen(showWhen)
                    .setSmallIcon(n.icon, n.iconLevel)
                    .setContent(n.contentView)
                    .setTicker(n.tickerText, tickerView)
                    .setSound(n.sound, n.audioStreamType)
                    .setVibrate(n.vibrate)
                    .setLights(n.ledARGB, n.ledOnMS, n.ledOffMS)
                    .setOngoing((n.flags & Notification.FLAG_ONGOING_EVENT) != 0)
                    .setOnlyAlertOnce((n.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0)
                    .setAutoCancel((n.flags & Notification.FLAG_AUTO_CANCEL) != 0)
                    .setDefaults(n.defaults)
                    .setContentTitle(contentTitle)
                    .setContentText(contentText)
                    .setSubText(subText)
                    .setContentInfo(contentInfo)
                    .setContentIntent(contentIntent)
                    .setDeleteIntent(n.deleteIntent)
                    .setFullScreenIntent(fullScreenIntent,
                            (n.flags & Notification.FLAG_HIGH_PRIORITY) != 0)
                    .setLargeIcon(largeIcon)
                    .setNumber(number)
                    .setUsesChronometer(useChronometer)
                    .setPriority(priority)
                    .setProgress(progressMax, progress, progressIndeterminate)
                    .setLocalOnly(localOnly)
                    .setGroup(groupKey)
                    .setGroupSummary(groupSummary)
                    .setSortKey(sortKey)
                    .setCategory(category)
                    .setColor(color)
                    .setVisibility(visibility)
                    .setPublicVersion(publicVersion);
            mExtras = new Bundle();
            if (extras != null) {
                mExtras.putAll(extras);
            }
            for (String person: people) {
                b.addPerson(person);
            }
            mContentView = contentView;
            mBigContentView = bigContentView;
            mHeadsUpContentView = headsUpContentView;
        }

        @Override
        public void addAction(NotificationCompatBase.Action action) {
            NotificationCompatApi20.addAction(b, action);
        }

        @Override
        public Notification.Builder getBuilder() {
            return b;
        }

        @Override
        public Notification build() {
            b.setExtras(mExtras);
            Notification notification = b.build();
            if (mContentView != null) {
                notification.contentView = mContentView;
            }
            if (mBigContentView != null) {
                notification.bigContentView = mBigContentView;
            }
            if (mHeadsUpContentView != null) {
                notification.headsUpContentView = mHeadsUpContentView;
            }
            return notification;
        }
    }

    public static String getCategory(Notification notif) {
        return notif.category;
    }

    static Bundle getBundleForUnreadConversation(NotificationCompatBase.UnreadConversation uc) {
        if (uc == null) {
            return null;
        }
        Bundle b = new Bundle();
        String author = null;
        if (uc.getParticipants() != null && uc.getParticipants().length > 1) {
            author = uc.getParticipants()[0];
        }
        Parcelable[] messages = new Parcelable[uc.getMessages().length];
        for (int i = 0; i < messages.length; i++) {
            Bundle m = new Bundle();
            m.putString(KEY_TEXT, uc.getMessages()[i]);
            m.putString(KEY_AUTHOR, author);
            messages[i] = m;
        }
        b.putParcelableArray(KEY_MESSAGES, messages);
        RemoteInputCompatBase.RemoteInput remoteInput = uc.getRemoteInput();
        if (remoteInput != null) {
            b.putParcelable(KEY_REMOTE_INPUT, fromCompatRemoteInput(remoteInput));
        }
        b.putParcelable(KEY_ON_REPLY, uc.getReplyPendingIntent());
        b.putParcelable(KEY_ON_READ, uc.getReadPendingIntent());
        b.putStringArray(KEY_PARTICIPANTS, uc.getParticipants());
        b.putLong(KEY_TIMESTAMP, uc.getLatestTimestamp());
        return b;
    }

    static NotificationCompatBase.UnreadConversation getUnreadConversationFromBundle(
            Bundle b, NotificationCompatBase.UnreadConversation.Factory factory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory) {
        if (b == null) {
            return null;
        }
        Parcelable[] parcelableMessages = b.getParcelableArray(KEY_MESSAGES);
        String[] messages = null;
        if (parcelableMessages != null) {
            String[] tmp = new String[parcelableMessages.length];
            boolean success = true;
            for (int i = 0; i < tmp.length; i++) {
                if (!(parcelableMessages[i] instanceof Bundle)) {
                    success = false;
                    break;
                }
                tmp[i] = ((Bundle) parcelableMessages[i]).getString(KEY_TEXT);
                if (tmp[i] == null) {
                    success = false;
                    break;
                }
            }
            if (success) {
                messages = tmp;
            } else {
                return null;
            }
        }

        PendingIntent onRead = b.getParcelable(KEY_ON_READ);
        PendingIntent onReply = b.getParcelable(KEY_ON_REPLY);

        android.app.RemoteInput remoteInput = b.getParcelable(KEY_REMOTE_INPUT);

        String[] participants = b.getStringArray(KEY_PARTICIPANTS);
        if (participants == null || participants.length != 1) {
            return null;
        }


        return factory.build(
                messages,
                remoteInput != null ? toCompatRemoteInput(remoteInput, remoteInputFactory) : null,
                onReply,
                onRead,
                participants, b.getLong(KEY_TIMESTAMP));
    }

    private static android.app.RemoteInput fromCompatRemoteInput(
            RemoteInputCompatBase.RemoteInput src) {
        return new android.app.RemoteInput.Builder(src.getResultKey())
                .setLabel(src.getLabel())
                .setChoices(src.getChoices())
                .setAllowFreeFormInput(src.getAllowFreeFormInput())
                .addExtras(src.getExtras())
                .build();
    }

    private static RemoteInputCompatBase.RemoteInput toCompatRemoteInput(
            android.app.RemoteInput remoteInput,
            RemoteInputCompatBase.RemoteInput.Factory factory) {
        return factory.build(remoteInput.getResultKey(),
                remoteInput.getLabel(),
                remoteInput.getChoices(),
                remoteInput.getAllowFreeFormInput(),
                remoteInput.getExtras());
    }
}
