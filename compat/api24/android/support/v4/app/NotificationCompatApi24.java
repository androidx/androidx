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

@RequiresApi(24)
class NotificationCompatApi24 {

    public static class Builder extends NotificationCompatApi21.Builder {
        public Builder(Context context, Notification n,
                CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
                RemoteViews tickerView, int number,
                PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
                int progressMax, int progress, boolean progressIndeterminate, boolean showWhen,
                boolean useChronometer, int priority, CharSequence subText, boolean localOnly,
                String category, ArrayList<String> people, Bundle extras, int color,
                int visibility, Notification publicVersion, String groupKey, boolean groupSummary,
                String sortKey, CharSequence[] remoteInputHistory, RemoteViews contentView,
                RemoteViews bigContentView, RemoteViews headsUpContentView,
                int groupAlertBehavior, String channelId) {
            super(context, n, contentTitle, contentText, contentInfo, tickerView, number,
                    contentIntent, fullScreenIntent, largeIcon, progressMax, progress,
                    progressIndeterminate, showWhen, useChronometer, priority, subText, localOnly,
                    category, people, extras, color, visibility, publicVersion, groupKey,
                    groupSummary, sortKey, contentView, bigContentView,
                    headsUpContentView, groupAlertBehavior, channelId);
            mBuilder.setExtras(extras)
                    .setRemoteInputHistory(remoteInputHistory);
            if (contentView != null) {
                mBuilder.setCustomContentView(contentView);
            }
            if (bigContentView != null) {
                mBuilder.setCustomBigContentView(bigContentView);
            }
            if (headsUpContentView != null) {
                mBuilder.setCustomHeadsUpContentView(headsUpContentView);
            }
        }

        @Override
        public void addAction(NotificationCompatBase.Action action) {
            NotificationCompatApi24.addAction(mBuilder, action);
        }

        @Override
        public Notification build() {
            Notification notification =  mBuilder.build();

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
        actionBuilder.setAllowGeneratedReplies(action.getAllowGeneratedReplies());
        actionBuilder.addExtras(actionExtras);
        b.addAction(actionBuilder.build());
    }

    public static NotificationCompatBase.Action getAction(Notification notif,
            int actionIndex, NotificationCompatBase.Action.Factory actionFactory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory) {
        return getActionCompatFromAction(notif.actions[actionIndex], actionFactory,
                remoteInputFactory);
    }

    static NotificationCompatBase.Action getActionCompatFromAction(
            Notification.Action action, NotificationCompatBase.Action.Factory actionFactory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory) {
        RemoteInputCompatBase.RemoteInput[] remoteInputs = RemoteInputCompatApi20.toCompat(
                action.getRemoteInputs(), remoteInputFactory);
        boolean allowGeneratedReplies = action.getExtras().getBoolean(
                NotificationCompatJellybean.EXTRA_ALLOW_GENERATED_REPLIES)
                || action.getAllowGeneratedReplies();
        return actionFactory.build(action.icon, action.title, action.actionIntent,
                action.getExtras(), remoteInputs, null, allowGeneratedReplies);
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
        actionBuilder.setAllowGeneratedReplies(actionCompat.getAllowGeneratedReplies());
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
