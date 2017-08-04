/**
 * Copyright (C) 2017 The Android Open Source Project
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
import android.widget.RemoteViews;

import java.util.ArrayList;

@RequiresApi(26)
class NotificationCompatApi26 {
    public static class Builder extends NotificationCompatApi24.Builder {
        Builder(Context context, Notification n,
                CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
                RemoteViews tickerView, int number,
                PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
                int progressMax, int progress, boolean progressIndeterminate, boolean showWhen,
                boolean useChronometer, int priority, CharSequence subText, boolean localOnly,
                String category, ArrayList<String> people, Bundle extras, int color,
                int visibility, Notification publicVersion, String groupKey, boolean groupSummary,
                String sortKey, CharSequence[] remoteInputHistory, RemoteViews contentView,
                RemoteViews bigContentView, RemoteViews headsUpContentView,
                String channelId, int badgeIcon, String shortcutId, long timeoutMs,
                boolean colorized, boolean colorizedSet, int groupAlertBehavior) {
            super(context, n, contentTitle, contentText, contentInfo, tickerView, number,
                    contentIntent, fullScreenIntent, largeIcon, progressMax, progress,
                    progressIndeterminate, showWhen, useChronometer, priority, subText, localOnly,
                    category, people, extras, color, visibility, publicVersion, groupKey,
                    groupSummary, sortKey, remoteInputHistory, contentView, bigContentView,
                    headsUpContentView, groupAlertBehavior, channelId);
            mBuilder.setChannelId(channelId)
                    .setBadgeIconType(badgeIcon)
                    .setShortcutId(shortcutId)
                    .setTimeoutAfter(timeoutMs)
                    .setGroupAlertBehavior(groupAlertBehavior);
            if (colorizedSet) {
                mBuilder.setColorized(colorized);
            }
        }

        @Override
        public Notification build() {
            return mBuilder.build();
        }

        @Override
        protected Notification.Builder newBuilder(Context context, String channelId) {
            return new Notification.Builder(context, channelId);
        }
    }
}
