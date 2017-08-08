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
import android.support.annotation.RequiresApi;
import android.util.SparseArray;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(19)
class NotificationCompatKitKat {
    public static class Builder extends NotificationCompatJellybean.Builder {
        private Bundle mExtras;
        private List<Bundle> mActionExtrasList = new ArrayList<Bundle>();

        public Builder(Context context, Notification n,
                CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
                RemoteViews tickerView, int number,
                PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
                int progressMax, int progress, boolean progressIndeterminate, boolean showWhen,
                boolean useChronometer, int priority, CharSequence subText, boolean localOnly,
                ArrayList<String> people, Bundle extras, String groupKey, boolean groupSummary,
                String sortKey, RemoteViews contentView, RemoteViews bigContentView,
                String channelId) {
            super(context, n, contentTitle, contentText, contentInfo, tickerView, number,
                    contentIntent, fullScreenIntent, largeIcon, progressMax, progress,
                    progressIndeterminate, useChronometer, priority, subText, localOnly,
                    extras, groupKey, groupSummary, sortKey, contentView, bigContentView,
                    channelId);
            mBuilder.setShowWhen(showWhen);

            mExtras = new Bundle();
            if (extras != null) {
                mExtras.putAll(extras);
            }
            if (people != null && !people.isEmpty()) {
                mExtras.putStringArray(Notification.EXTRA_PEOPLE,
                        people.toArray(new String[people.size()]));
            }
            if (localOnly) {
                mExtras.putBoolean(NotificationCompatExtras.EXTRA_LOCAL_ONLY, true);
            }
            if (groupKey != null) {
                mExtras.putString(NotificationCompatExtras.EXTRA_GROUP_KEY, groupKey);
                if (groupSummary) {
                    mExtras.putBoolean(NotificationCompatExtras.EXTRA_GROUP_SUMMARY, true);
                } else {
                    mExtras.putBoolean(NotificationManagerCompat.EXTRA_USE_SIDE_CHANNEL, true);
                }
            }
            if (sortKey != null) {
                mExtras.putString(NotificationCompatExtras.EXTRA_SORT_KEY, sortKey);
            }
            mContentView = contentView;
            mBigContentView = bigContentView;
        }

        @Override
        public Notification build() {
            SparseArray<Bundle> actionExtrasMap = NotificationCompatJellybean.buildActionExtrasMap(
                    mActionExtrasList);
            if (actionExtrasMap != null) {
                // Add the action extras sparse array if any action was added with extras.
                mExtras.putSparseParcelableArray(
                        NotificationCompatExtras.EXTRA_ACTION_EXTRAS, actionExtrasMap);
            }
            mBuilder.setExtras(mExtras);
            Notification notification = mBuilder.build();
            if (mContentView != null) {
                notification.contentView = mContentView;
            }
            if (mBigContentView != null) {
                notification.bigContentView = mBigContentView;
            }
            return notification;
        }
    }

    public static NotificationCompatBase.Action getAction(Notification notif,
            int actionIndex, NotificationCompatBase.Action.Factory factory,
            RemoteInputCompatBase.RemoteInput.Factory remoteInputFactory) {
        Notification.Action action = notif.actions[actionIndex];
        Bundle actionExtras = null;
        SparseArray<Bundle> actionExtrasMap = notif.extras.getSparseParcelableArray(
                NotificationCompatExtras.EXTRA_ACTION_EXTRAS);
        if (actionExtrasMap != null) {
            actionExtras = actionExtrasMap.get(actionIndex);
        }
        return NotificationCompatJellybean.readAction(factory, remoteInputFactory,
                action.icon, action.title, action.actionIntent, actionExtras);
    }
}
