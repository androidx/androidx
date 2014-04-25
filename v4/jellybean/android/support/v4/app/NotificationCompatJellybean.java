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
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

import java.lang.reflect.Field;
import java.util.ArrayList;

class NotificationCompatJellybean {
    public static final String TAG = "NotificationCompat";

    private static final Object sExtrasLock = new Object();
    private static Field sExtrasField;
    private static boolean sExtrasFieldAccessFailed;

    public static class Builder implements NotificationBuilderWithBuilderAccessor,
            NotificationBuilderWithActions {
        private Notification.Builder b;
        private final Bundle mExtras;

        public Builder(Context context, Notification n,
                CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
                RemoteViews tickerView, int number,
                PendingIntent contentIntent, PendingIntent fullScreenIntent, Bitmap largeIcon,
                int mProgressMax, int mProgress, boolean mProgressIndeterminate,
                boolean useChronometer, int priority, CharSequence subText, Bundle extras) {
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
            mExtras = extras;
        }

        @Override
        public void addAction(int icon, CharSequence title, PendingIntent intent) {
            b.addAction(icon, title, intent);
        }

        @Override
        public Notification.Builder getBuilder() {
            return b;
        }

        public Notification build() {
            Notification notif = b.build();
            if (mExtras != null) {
                // Merge in developer provided extras, but let the values already set
                // for keys take precedence.
                Bundle extras = getExtras(notif);
                Bundle mergeBundle = new Bundle(mExtras);
                for (String key : mExtras.keySet()) {
                    if (extras.containsKey(key)) {
                        mergeBundle.remove(key);
                    }
                }
                extras.putAll(mergeBundle);
            }
            return notif;
        }
    }

    public static void addBigTextStyle(NotificationBuilderWithBuilderAccessor b,
            CharSequence bigContentTitle, boolean useSummary,
            CharSequence summaryText, CharSequence bigText) {
        Notification.BigTextStyle style = new Notification.BigTextStyle(b.getBuilder())
            .setBigContentTitle(bigContentTitle)
            .bigText(bigText);
        if (useSummary) {
            style.setSummaryText(summaryText);
        }
    }

    public static void addBigPictureStyle(NotificationBuilderWithBuilderAccessor b,
            CharSequence bigContentTitle, boolean useSummary,
            CharSequence summaryText, Bitmap bigPicture, Bitmap bigLargeIcon,
            boolean bigLargeIconSet) {
        Notification.BigPictureStyle style = new Notification.BigPictureStyle(b.getBuilder())
            .setBigContentTitle(bigContentTitle)
            .bigPicture(bigPicture);
        if (bigLargeIconSet) {
            style.bigLargeIcon(bigLargeIcon);
        }
        if (useSummary) {
            style.setSummaryText(summaryText);
        }
    }

    public static void addInboxStyle(NotificationBuilderWithBuilderAccessor b,
            CharSequence bigContentTitle, boolean useSummary,
            CharSequence summaryText, ArrayList<CharSequence> texts) {
        Notification.InboxStyle style = new Notification.InboxStyle(b.getBuilder())
            .setBigContentTitle(bigContentTitle);
        if (useSummary) {
            style.setSummaryText(summaryText);
        }
        for (CharSequence text: texts) {
            style.addLine(text);
        }
    }

    /**
     * Get the extras Bundle from a notification using reflection. Extras were present in
     * Jellybean notifications, but the field was private until KitKat.
     */
    public static Bundle getExtras(Notification notif) {
        synchronized (sExtrasLock) {
            if (sExtrasFieldAccessFailed) {
                return null;
            }
            try {
                if (sExtrasField == null) {
                    Field extrasField = Notification.class.getDeclaredField("extras");
                    if (!Bundle.class.isAssignableFrom(extrasField.getType())) {
                        Log.e(TAG, "Notification.extras field is not of type Bundle");
                        sExtrasFieldAccessFailed = true;
                        return null;
                    }
                    extrasField.setAccessible(true);
                    sExtrasField = extrasField;
                }
                Bundle extras = (Bundle) sExtrasField.get(notif);
                if (extras == null) {
                    extras = new Bundle();
                    sExtrasField.set(notif, extras);
                }
                return extras;
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Unable to access notification extras", e);
            } catch (NoSuchFieldException e) {
                Log.e(TAG, "Unable to access notification extras", e);
            }
            sExtrasFieldAccessFailed = true;
            return null;
        }
    }
}
