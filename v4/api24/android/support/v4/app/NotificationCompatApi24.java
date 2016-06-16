/*
 * Copyright (C) 2012 The Android Open Source Project
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
import android.app.RemoteInput;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

class NotificationCompatApi24 {

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

    public static class Builder implements NotificationBuilderWithBuilderAccessor,
            NotificationBuilderWithActions {
        private Notification.Builder b;

        public Builder(Context context, Notification n,
                CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
                RemoteViews tickerView, int number,
                PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
                int progressMax, int progress, boolean progressIndeterminate, boolean showWhen,
                boolean useChronometer, int priority, CharSequence subText, boolean localOnly,
                String category, ArrayList<String> people, Bundle extras, int color,
                int visibility, Notification publicVersion, String groupKey, boolean groupSummary,
                String sortKey, CharSequence[] remoteInputHistory, RemoteViews contentView,
                RemoteViews bigContentView, RemoteViews headsUpContentView) {
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
                    .setExtras(extras)
                    .setGroup(groupKey)
                    .setGroupSummary(groupSummary)
                    .setSortKey(sortKey)
                    .setCategory(category)
                    .setColor(color)
                    .setVisibility(visibility)
                    .setPublicVersion(publicVersion)
                    .setRemoteInputHistory(remoteInputHistory);
            if (contentView != null) {
                b.setCustomContentView(contentView);
            }
            if (bigContentView != null) {
                b.setCustomBigContentView(bigContentView);
            }
            if (headsUpContentView != null) {
                b.setCustomHeadsUpContentView(headsUpContentView);
            }
            for (String person: people) {
                b.addPerson(person);
            }
        }

        @Override
        public void addAction(NotificationCompatBase.Action action) {
            Notification.Action.Builder actionBuilder = new Notification.Action.Builder(
                    action.getIcon(), action.getTitle(), action.getActionIntent());
            if (action.getRemoteInputs() != null) {
                for (RemoteInput remoteInput : RemoteInputCompatApi20.fromCompat(
                        action.getRemoteInputs())) {
                    actionBuilder.addRemoteInput(remoteInput);
                }
            }
            Bundle actionExtras;
            if (action.getExtras() != null) {
                actionExtras = new Bundle(action.getExtras());
            } else {
                actionExtras = new Bundle();
            }
            actionExtras.putBoolean(NotificationCompatJellybean.EXTRA_ALLOW_GENERATED_REPLIES,
                    action.getAllowGeneratedReplies());
            actionBuilder.addExtras(actionExtras);
            actionBuilder.setAllowGeneratedReplies(action.getAllowGeneratedReplies());
            b.addAction(actionBuilder.build());
        }

        @Override
        public Notification.Builder getBuilder() {
            return b;
        }

        @Override
        public Notification build() {
            return b.build();
        }
    }

    public static void addMessagingStyle(NotificationBuilderWithBuilderAccessor b,
            CharSequence userDisplayName, CharSequence conversationTitle, List<CharSequence> texts,
            List<Long> timestamps, List<CharSequence> senders, List<String> dataMimeTypes,
            List<Uri> dataUris) {
        Notification.MessagingStyle style = new Notification.MessagingStyle(userDisplayName)
                .setConversationTitle(conversationTitle);
        for (int i = 0; i < texts.size(); i++) {
            Notification.MessagingStyle.Message message = new Notification.MessagingStyle.Message(
                    texts.get(i), timestamps.get(i), senders.get(i));
            if (dataMimeTypes.get(i) != null) {
                message.setData(dataMimeTypes.get(i), dataUris.get(i));
            }
            style.addMessage(message);
        }
        style.setBuilder(b.getBuilder());
    }
}
