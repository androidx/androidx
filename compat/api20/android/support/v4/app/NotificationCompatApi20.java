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

import static android.support.v4.app.NotificationCompat.DEFAULT_SOUND;
import static android.support.v4.app.NotificationCompat.DEFAULT_VIBRATE;
import static android.support.v4.app.NotificationCompat.FLAG_GROUP_SUMMARY;
import static android.support.v4.app.NotificationCompat.GROUP_ALERT_ALL;
import static android.support.v4.app.NotificationCompat.GROUP_ALERT_CHILDREN;
import static android.support.v4.app.NotificationCompat.GROUP_ALERT_SUMMARY;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.widget.RemoteViews;

import java.util.ArrayList;

@RequiresApi(20)
class NotificationCompatApi20 {
    public static class Builder extends NotificationCompatKitKat.Builder {
        protected int mGroupAlertBehavior;

        private Bundle mExtras;

        public Builder(Context context, Notification n,
                CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
                RemoteViews tickerView, int number,
                PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
                int progressMax, int progress, boolean progressIndeterminate, boolean showWhen,
                boolean useChronometer, int priority, CharSequence subText, boolean localOnly,
                ArrayList<String> people, Bundle extras, String groupKey, boolean groupSummary,
                String sortKey, RemoteViews contentView, RemoteViews bigContentView,
                int groupAlertBehavior, String channelId) {
            super(context, n, contentTitle, contentText, contentInfo, tickerView, number,
                    contentIntent, fullScreenIntent, largeIcon, progressMax, progress,
                    progressIndeterminate, showWhen, useChronometer, priority, subText, localOnly,
                    people, extras, groupKey, groupSummary, sortKey, contentView, bigContentView,
                    channelId);
            mBuilder.setLocalOnly(localOnly)
                .setGroup(groupKey)
                .setGroupSummary(groupSummary)
                .setSortKey(sortKey);
            mExtras = new Bundle();
            if (extras != null) {
                mExtras.putAll(extras);
            }
            if (people != null && !people.isEmpty()) {
                mExtras.putStringArray(Notification.EXTRA_PEOPLE,
                        people.toArray(new String[people.size()]));
            }

            mGroupAlertBehavior = groupAlertBehavior;
        }

        @Override
        public void addAction(NotificationCompatBase.Action action) {
            NotificationCompatApi20.addAction(mBuilder, action);
        }

        @Override
        public Notification build() {
            mBuilder.setExtras(mExtras);
            Notification notification = mBuilder.build();
            if (mContentView != null) {
                notification.contentView = mContentView;
            }
            if (mBigContentView != null) {
                notification.bigContentView = mBigContentView;
            }

            if (mGroupAlertBehavior != GROUP_ALERT_ALL) {
                // if is summary and only children should alert
                if (notification.getGroup() != null
                        && (notification.flags & FLAG_GROUP_SUMMARY) != 0
                        && mGroupAlertBehavior == GROUP_ALERT_CHILDREN) {
                    removeSoundAndVibration(notification);
                }
                // if is group child and only summary should alert
                if (notification.getGroup() != null
                        && (notification.flags & FLAG_GROUP_SUMMARY) == 0
                        && mGroupAlertBehavior == GROUP_ALERT_SUMMARY) {
                    removeSoundAndVibration(notification);
                }
            }

            return notification;
        }

        protected void removeSoundAndVibration(Notification notification) {
            notification.sound = null;
            notification.vibrate = null;
            notification.defaults &= ~DEFAULT_SOUND;
            notification.defaults &= ~DEFAULT_VIBRATE;
        }
    }

    public static void addAction(Notification.Builder b, NotificationCompatBase.Action action) {
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
        b.addAction(actionBuilder.build());
    }

    static Notification.Action getActionFromActionCompat(
            NotificationCompatBase.Action actionCompat) {
        Notification.Action.Builder actionBuilder = new Notification.Action.Builder(
                actionCompat.getIcon(), actionCompat.getTitle(), actionCompat.getActionIntent());
        Bundle actionExtras;
        if (actionCompat.getExtras() != null) {
            actionExtras = new Bundle(actionCompat.getExtras());
        } else {
            actionExtras = new Bundle();
        }
        actionExtras.putBoolean(NotificationCompatJellybean.EXTRA_ALLOW_GENERATED_REPLIES,
                actionCompat.getAllowGeneratedReplies());
        actionBuilder.addExtras(actionExtras);
        RemoteInputCompatBase.RemoteInput[] remoteInputCompats = actionCompat.getRemoteInputs();
        if (remoteInputCompats != null) {
            RemoteInput[] remoteInputs = RemoteInputCompatApi20.fromCompat(remoteInputCompats);
            for (RemoteInput remoteInput : remoteInputs) {
                actionBuilder.addRemoteInput(remoteInput);
            }
        }
        return actionBuilder.build();
    }
}
