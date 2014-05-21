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
import android.app.RemoteInput;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;

class NotificationCompatApi20 {
    public static class Builder implements NotificationBuilderWithBuilderAccessor,
            NotificationBuilderWithActions {
        private Notification.Builder b;

        public Builder(Context context, Notification n,
                CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
                RemoteViews tickerView, int number,
                PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
                int mProgressMax, int mProgress, boolean mProgressIndeterminate,
                boolean useChronometer, int priority, CharSequence subText, boolean localOnly,
                Bundle extras, String groupKey, boolean groupSummary, String sortKey) {
            b = new Notification.Builder(context)
                .setWhen(n.when)
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
                .setProgress(mProgressMax, mProgress, mProgressIndeterminate)
                .setLocalOnly(localOnly)
                .setExtras(extras)
                .setGroup(groupKey)
                .setGroupSummary(groupSummary)
                .setSortKey(sortKey);
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
            if (action.getExtras() != null) {
                actionBuilder.addExtras(action.getExtras());
            }
            b.addAction(actionBuilder.build());
        }

        @Override
        public Notification.Builder getBuilder() {
            return b;
        }

        public Notification build() {
            return b.build();
        }
    }

    public static NotificationCompatBase.Action getAction(Notification notif,
            int actionIndex, NotificationCompatBase.Action.Factory factory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory) {
        Notification.Action action = notif.actions[actionIndex];
        RemoteInputCompatBase.RemoteInput[] remoteInputs = RemoteInputCompatApi20.toCompat(
                action.getRemoteInputs(), remoteInputFactory);
        return factory.build(action.icon, action.title, action.actionIntent,
                action.getExtras(), remoteInputs);
    }

    public static boolean getLocalOnly(Notification notif) {
        return (notif.flags & Notification.FLAG_LOCAL_ONLY) != 0;
    }

    public static String getGroup(Notification notif) {
        return notif.getGroup();
    }

    public static boolean isGroupSummary(Notification notif) {
        return (notif.flags & Notification.FLAG_GROUP_SUMMARY) != 0;
    }

    public static String getSortKey(Notification notif) {
        return notif.getSortKey();
    }
}
