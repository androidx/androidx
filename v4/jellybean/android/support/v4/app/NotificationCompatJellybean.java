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
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.RemoteViews;
import java.util.ArrayList;

class NotificationCompatJellybean {
    private Notification.Builder b;
    public NotificationCompatJellybean(Context context, Notification n,
            CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
            RemoteViews tickerView, int number,
            PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
            int mProgressMax, int mProgress, boolean mProgressIndeterminate,
            boolean useChronometer, int priority, CharSequence subText) {
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
            .setProgress(mProgressMax, mProgress, mProgressIndeterminate);
    }

    public void addAction(int icon, CharSequence title, PendingIntent intent) {
        b.addAction(icon, title, intent);
    }

    public void addBigTextStyle(CharSequence bigContentTitle, boolean useSummary,
            CharSequence summaryText, CharSequence bigText) {
        Notification.BigTextStyle style = new Notification.BigTextStyle(b)
            .setBigContentTitle(bigContentTitle)
            .bigText(bigText);
        if (useSummary) {
            style.setSummaryText(summaryText);
         }
    }

    public void addBigPictureStyle(CharSequence bigContentTitle, boolean useSummary,
            CharSequence summaryText, Bitmap bigPicture, Bitmap bigLargeIcon,
            boolean bigLargeIconSet) {
       Notification.BigPictureStyle style = new Notification.BigPictureStyle(b)
           .setBigContentTitle(bigContentTitle)
           .bigPicture(bigPicture);
       if (bigLargeIconSet) {
           style.bigLargeIcon(bigLargeIcon);
       }
        if (useSummary) {
            style.setSummaryText(summaryText);
         }
    }

    public void addInboxStyle(CharSequence bigContentTitle, boolean useSummary,
            CharSequence summaryText, ArrayList<CharSequence> texts) {
        Notification.InboxStyle style = new Notification.InboxStyle(b)
            .setBigContentTitle(bigContentTitle);
        if (useSummary) {
            style.setSummaryText(summaryText);
        }
        for (CharSequence text: texts) {
            style.addLine(text);
        }
    }

    public Notification build() {
        return b.build();
    }
}
