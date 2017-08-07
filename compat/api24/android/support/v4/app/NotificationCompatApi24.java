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
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

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

    private static NotificationCompatBase.Action getActionCompatFromAction(
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

    private static Notification.Action getActionFromActionCompat(
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

    /**
     * Get a list of notification compat actions by parsing actions stored within a list of
     * parcelables using the {@link Bundle#getParcelableArrayList} function in the same
     * manner that framework code would do so. In API20, Using Action parcelable directly
     * is correct.
     */
    public static NotificationCompatBase.Action[] getActionsFromParcelableArrayList(
            ArrayList<Parcelable> parcelables,
            NotificationCompatBase.Action.Factory actionFactory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory) {
        if (parcelables == null) {
            return null;
        }
        NotificationCompatBase.Action[] actions = actionFactory.newArray(parcelables.size());
        for (int i = 0; i < actions.length; i++) {
            Notification.Action action = (Notification.Action) parcelables.get(i);
            actions[i] = getActionCompatFromAction(action, actionFactory, remoteInputFactory);
        }
        return actions;
    }

    /**
     * Get an array list of parcelables, suitable for {@link Bundle#putParcelableArrayList},
     * that matches what framework code would do to store an actions list in this way. In API20,
     * action parcelables were directly placed as entries in the array list.
     */
    public static ArrayList<Parcelable> getParcelableArrayListForActions(
            NotificationCompatBase.Action[] actions) {
        if (actions == null) {
            return null;
        }
        ArrayList<Parcelable> parcelables = new ArrayList<Parcelable>(actions.length);
        for (NotificationCompatBase.Action action : actions) {
            parcelables.add(getActionFromActionCompat(action));
        }
        return parcelables;
    }
}
