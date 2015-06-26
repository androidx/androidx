/*
 * Copyright (C) 2015 The Android Open Source Project
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
 * limitations under the License
 */

package android.support.v7.internal.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.support.v7.appcompat.R;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.app.NotificationCompatBase;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.text.NumberFormat;
import java.util.List;

/**
 * Helper class to generate MediaStyle notifications for pre-Lollipop platforms. Overrides
 * contentView and bigContentView of the notification.
 * @hide
 */
public class NotificationCompatImplBase {

    static final int MAX_MEDIA_BUTTONS_IN_COMPACT = 3;
    static final int MAX_MEDIA_BUTTONS = 5;

    public static <T extends NotificationCompatBase.Action> void overrideContentView(
            NotificationBuilderWithBuilderAccessor builder,
            Context context, CharSequence contentTitle, CharSequence contentText,
            CharSequence contentInfo, int number, Bitmap largeIcon, CharSequence subText,
            boolean useChronometer, long when, List<T> actions, int[] actionsToShowInCompact,
            boolean showCancelButton, PendingIntent cancelButtonIntent) {
        RemoteViews views = generateContentView(context, contentTitle, contentText, contentInfo,
                number, largeIcon, subText, useChronometer, when, actions, actionsToShowInCompact,
                showCancelButton, cancelButtonIntent);
        builder.getBuilder().setContent(views);
        if (showCancelButton) {
            builder.getBuilder().setOngoing(true);
        }
    }

    private static <T extends NotificationCompatBase.Action> RemoteViews generateContentView(
            Context context, CharSequence contentTitle, CharSequence contentText,
            CharSequence contentInfo, int number, Bitmap largeIcon, CharSequence subText,
            boolean useChronometer, long when, List<T> actions, int[] actionsToShowInCompact,
            boolean showCancelButton, PendingIntent cancelButtonIntent) {
        RemoteViews view = applyStandardTemplate(context, contentTitle, contentText, contentInfo,
                number, largeIcon, subText, useChronometer, when,
                R.layout.notification_template_media, true /* fitIn1U */);

        final int numActions = actions.size();
        final int N = actionsToShowInCompact == null
                ? 0
                : Math.min(actionsToShowInCompact.length, MAX_MEDIA_BUTTONS_IN_COMPACT);
        view.removeAllViews(R.id.media_actions);
        if (N > 0) {
            for (int i = 0; i < N; i++) {
                if (i >= numActions) {
                    throw new IllegalArgumentException(String.format(
                            "setShowActionsInCompactView: action %d out of bounds (max %d)",
                            i, numActions - 1));
                }

                final NotificationCompatBase.Action action = actions.get(actionsToShowInCompact[i]);
                final RemoteViews button = generateMediaActionButton(context, action);
                view.addView(R.id.media_actions, button);
            }
        }
        if (showCancelButton) {
            view.setViewVisibility(R.id.end_padder, View.GONE);
            view.setViewVisibility(R.id.cancel_action, View.VISIBLE);
            view.setOnClickPendingIntent(R.id.cancel_action, cancelButtonIntent);
            view.setInt(R.id.cancel_action, "setAlpha",
                    context.getResources().getInteger(R.integer.cancel_button_image_alpha));
        } else {
            view.setViewVisibility(R.id.end_padder, View.VISIBLE);
            view.setViewVisibility(R.id.cancel_action, View.GONE);
        }
        return view;
    }

    public static <T extends NotificationCompatBase.Action> void overrideBigContentView(
            Notification n, Context context, CharSequence contentTitle, CharSequence contentText,
            CharSequence contentInfo, int number, Bitmap largeIcon, CharSequence subText,
            boolean useChronometer, long when, List<T> actions, boolean showCancelButton,
            PendingIntent cancelButtonIntent) {
        n.bigContentView = generateBigContentView(context, contentTitle, contentText, contentInfo,
                number, largeIcon, subText, useChronometer, when, actions, showCancelButton,
                cancelButtonIntent);
        if (showCancelButton) {
            n.flags |= Notification.FLAG_ONGOING_EVENT;
        }
    }

    private static <T extends NotificationCompatBase.Action> RemoteViews generateBigContentView(
            Context context, CharSequence contentTitle, CharSequence contentText,
            CharSequence contentInfo, int number, Bitmap largeIcon, CharSequence subText,
            boolean useChronometer, long when, List<T> actions, boolean showCancelButton,
            PendingIntent cancelButtonIntent) {
        final int actionCount = Math.min(actions.size(), MAX_MEDIA_BUTTONS);
        RemoteViews big = applyStandardTemplate(context, contentTitle, contentText, contentInfo,
                number, largeIcon, subText, useChronometer, when,
                getBigLayoutResource(actionCount), false /* fitIn1U */);

        big.removeAllViews(R.id.media_actions);
        if (actionCount > 0) {
            for (int i = 0; i < actionCount; i++) {
                final RemoteViews button = generateMediaActionButton(context, actions.get(i));
                big.addView(R.id.media_actions, button);
            }
        }
        if (showCancelButton) {
            big.setViewVisibility(R.id.cancel_action, View.VISIBLE);
            big.setInt(R.id.cancel_action, "setAlpha",
                    context.getResources().getInteger(R.integer.cancel_button_image_alpha));
            big.setOnClickPendingIntent(R.id.cancel_action, cancelButtonIntent);
        } else {
            big.setViewVisibility(R.id.cancel_action, View.GONE);
        }
        return big;
    }

    private static RemoteViews generateMediaActionButton(Context context,
            NotificationCompatBase.Action action) {
        final boolean tombstone = (action.getActionIntent() == null);
        RemoteViews button = new RemoteViews(context.getPackageName(),
                R.layout.notification_media_action);
        button.setImageViewResource(R.id.action0, action.getIcon());
        if (!tombstone) {
            button.setOnClickPendingIntent(R.id.action0, action.getActionIntent());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            button.setContentDescription(R.id.action0, action.getTitle());
        }
        return button;
    }

    private static int getBigLayoutResource(int actionCount) {
        if (actionCount <= 3) {
            return R.layout.notification_template_big_media_narrow;
        } else {
            return R.layout.notification_template_big_media;
        }
    }

    private static RemoteViews applyStandardTemplate(Context context,
            CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
            int number, Bitmap largeIcon, CharSequence subText, boolean useChronometer, long when,
            int resId, boolean fitIn1U) {
        RemoteViews contentView = new RemoteViews(context.getPackageName(), resId);
        boolean showLine3 = false;
        boolean showLine2 = false;

        // On versions before Jellybean, the large icon was shown by SystemUI, so we need to hide
        // it here.
        if (largeIcon != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            contentView.setImageViewBitmap(R.id.icon, largeIcon);
        } else {
            contentView.setViewVisibility(R.id.icon, View.GONE);
        }
        if (contentTitle != null) {
            contentView.setTextViewText(R.id.title, contentTitle);
        }
        if (contentText != null) {
            contentView.setTextViewText(R.id.text, contentText);
            showLine3 = true;
        }
        if (contentInfo != null) {
            contentView.setTextViewText(R.id.info, contentInfo);
            contentView.setViewVisibility(R.id.info, View.VISIBLE);
            showLine3 = true;
        } else if (number > 0) {
            final int tooBig = context.getResources().getInteger(
                    R.integer.status_bar_notification_info_maxnum);
            if (number > tooBig) {
                contentView.setTextViewText(R.id.info, context.getResources().getString(
                        R.string.status_bar_notification_info_overflow));
            } else {
                NumberFormat f = NumberFormat.getIntegerInstance();
                contentView.setTextViewText(R.id.info, f.format(number));
            }
            contentView.setViewVisibility(R.id.info, View.VISIBLE);
            showLine3 = true;
        } else {
            contentView.setViewVisibility(R.id.info, View.GONE);
        }

        // Need to show three lines? Only allow on Jellybean+
        if (subText != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            contentView.setTextViewText(R.id.text, subText);
            if (contentText != null) {
                contentView.setTextViewText(R.id.text2, contentText);
                contentView.setViewVisibility(R.id.text2, View.VISIBLE);
                showLine2 = true;
            } else {
                contentView.setViewVisibility(R.id.text2, View.GONE);
            }
        }

        // RemoteViews.setViewPadding and RemoteViews.setTextViewTextSize is not available on ICS-
        if (showLine2 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            if (fitIn1U) {
                // need to shrink all the type to make sure everything fits
                final Resources res = context.getResources();
                final float subTextSize = res.getDimensionPixelSize(
                        R.dimen.notification_subtext_size);
                contentView.setTextViewTextSize(R.id.text, TypedValue.COMPLEX_UNIT_PX, subTextSize);
            }
            // vertical centering
            contentView.setViewPadding(R.id.line1, 0, 0, 0, 0);
        }

        if (when != 0) {
            if (useChronometer) {
                contentView.setViewVisibility(R.id.chronometer, View.VISIBLE);
                contentView.setLong(R.id.chronometer, "setBase",
                        when + (SystemClock.elapsedRealtime() - System.currentTimeMillis()));
                contentView.setBoolean(R.id.chronometer, "setStarted", true);
            } else {
                contentView.setViewVisibility(R.id.time, View.VISIBLE);
                contentView.setLong(R.id.time, "setTime", when);
            }
        }
        contentView.setViewVisibility(R.id.line3, showLine3 ? View.VISIBLE : View.GONE);
        return contentView;
    }
}
