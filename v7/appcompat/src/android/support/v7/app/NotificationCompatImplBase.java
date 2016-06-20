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

package android.support.v7.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompatBase;
import android.support.v7.appcompat.R;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to generate MediaStyle notifications for pre-Lollipop platforms. Overrides
 * contentView and bigContentView of the notification.
 */
class NotificationCompatImplBase {

    static final int MAX_MEDIA_BUTTONS_IN_COMPACT = 3;
    static final int MAX_MEDIA_BUTTONS = 5;
    private static final int MAX_ACTION_BUTTONS = 3;

    public static <T extends NotificationCompatBase.Action> RemoteViews overrideContentViewMedia(
            NotificationBuilderWithBuilderAccessor builder,
            Context context, CharSequence contentTitle, CharSequence contentText,
            CharSequence contentInfo, int number, Bitmap largeIcon, CharSequence subText,
            boolean useChronometer, long when, int priority, List<T> actions,
            int[] actionsToShowInCompact, boolean showCancelButton,
            PendingIntent cancelButtonIntent, boolean isDecoratedCustomView) {
        RemoteViews views = generateContentViewMedia(context, contentTitle, contentText, contentInfo,
                number, largeIcon, subText, useChronometer, when, priority, actions,
                actionsToShowInCompact, showCancelButton, cancelButtonIntent,
                isDecoratedCustomView);
        builder.getBuilder().setContent(views);
        if (showCancelButton) {
            builder.getBuilder().setOngoing(true);
        }
        return views;
    }

    private static <T extends NotificationCompatBase.Action> RemoteViews generateContentViewMedia(
            Context context, CharSequence contentTitle, CharSequence contentText,
            CharSequence contentInfo, int number, Bitmap largeIcon, CharSequence subText,
            boolean useChronometer, long when, int priority, List<T> actions,
            int[] actionsToShowInCompact, boolean showCancelButton,
            PendingIntent cancelButtonIntent, boolean isDecoratedCustomView) {
        RemoteViews view = applyStandardTemplate(context, contentTitle, contentText, contentInfo,
                number, 0 /* smallIcon */, largeIcon, subText, useChronometer, when, priority,
                0 /* color is unused on media */,
                isDecoratedCustomView ? R.layout.notification_template_media_custom
                        : R.layout.notification_template_media,
                true /* fitIn1U */);

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

    public static <T extends NotificationCompatBase.Action> void overrideMediaBigContentView(
            Notification n, Context context, CharSequence contentTitle, CharSequence contentText,
            CharSequence contentInfo, int number, Bitmap largeIcon, CharSequence subText,
            boolean useChronometer, long when, int priority, int color, List<T> actions,
            boolean showCancelButton, PendingIntent cancelButtonIntent,
            boolean decoratedCustomView) {
        n.bigContentView = generateMediaBigView(context, contentTitle, contentText, contentInfo,
                number, largeIcon, subText, useChronometer, when, priority, color,
                actions, showCancelButton, cancelButtonIntent, decoratedCustomView);
        if (showCancelButton) {
            n.flags |= Notification.FLAG_ONGOING_EVENT;
        }
    }

    public static <T extends NotificationCompatBase.Action> RemoteViews generateMediaBigView(
            Context context, CharSequence contentTitle, CharSequence contentText,
            CharSequence contentInfo, int number, Bitmap largeIcon, CharSequence subText,
            boolean useChronometer, long when, int priority, int color, List<T> actions,
            boolean showCancelButton, PendingIntent cancelButtonIntent,
            boolean decoratedCustomView) {
        final int actionCount = Math.min(actions.size(), MAX_MEDIA_BUTTONS);
        RemoteViews big = applyStandardTemplate(context, contentTitle, contentText, contentInfo,
                number, 0 /* smallIcon */, largeIcon, subText, useChronometer, when, priority,
                color,  /* fitIn1U */getBigMediaLayoutResource(decoratedCustomView, actionCount),
                false);

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

    private static int getBigMediaLayoutResource(boolean decoratedCustomView, int actionCount) {
        if (actionCount <= 3) {
            return decoratedCustomView
                    ? R.layout.notification_template_big_media_narrow_custom
                    : R.layout.notification_template_big_media_narrow;
        } else {
            return decoratedCustomView
                    ? R.layout.notification_template_big_media_custom
                    : R.layout.notification_template_big_media;
        }
    }

    public static RemoteViews applyStandardTemplateWithActions(Context context,
            CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
            int number, int smallIcon, Bitmap largeIcon, CharSequence subText,
            boolean useChronometer, long when, int priority, int color, int resId, boolean fitIn1U,
            ArrayList<NotificationCompat.Action> actions) {
        RemoteViews remoteViews = applyStandardTemplate(context, contentTitle, contentText,
                contentInfo, number, smallIcon, largeIcon, subText, useChronometer, when, priority,
                color, resId, fitIn1U);
        remoteViews.removeAllViews(R.id.actions);
        boolean actionsVisible = false;
        if (actions != null) {
            int N = actions.size();
            if (N > 0) {
                actionsVisible = true;
                if (N > MAX_ACTION_BUTTONS) N = MAX_ACTION_BUTTONS;
                for (int i = 0; i < N; i++) {
                    final RemoteViews button = generateActionButton(context, actions.get(i));
                    remoteViews.addView(R.id.actions, button);
                }
            }
        }
        int actionVisibility = actionsVisible ? View.VISIBLE : View.GONE;
        remoteViews.setViewVisibility(R.id.actions, actionVisibility);
        remoteViews.setViewVisibility(R.id.action_divider, actionVisibility);
        return remoteViews;
    }

    private static RemoteViews generateActionButton(Context context,
            NotificationCompat.Action action) {
        final boolean tombstone = (action.actionIntent == null);
        RemoteViews button =  new RemoteViews(context.getPackageName(),
                tombstone ? getActionTombstoneLayoutResource()
                        : getActionLayoutResource());
        button.setImageViewBitmap(R.id.action_image,
                createColoredBitmap(context, action.getIcon(),
                        context.getResources().getColor(R.color.notification_action_color_filter)));
        button.setTextViewText(R.id.action_text, action.title);
        if (!tombstone) {
            button.setOnClickPendingIntent(R.id.action_container, action.actionIntent);
        }
        button.setContentDescription(R.id.action_container, action.title);
        return button;
    }

    private static Bitmap createColoredBitmap(Context context, int iconId, int color) {
        return createColoredBitmap(context, iconId, color, 0);
    }

    private static Bitmap createColoredBitmap(Context context, int iconId, int color, int size) {
        Drawable drawable = context.getResources().getDrawable(iconId);
        int width = size == 0 ? drawable.getIntrinsicWidth() : size;
        int height = size == 0 ? drawable.getIntrinsicHeight() : size;
        Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, width, height);
        if (color != 0) {
            drawable.mutate().setColorFilter(
                    new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        }
        Canvas canvas = new Canvas(resultBitmap);
        drawable.draw(canvas);
        return resultBitmap;
    }

    private static int getActionLayoutResource() {
        return R.layout.notification_action;
    }

    private static int getActionTombstoneLayoutResource() {
        return R.layout.notification_action_tombstone;
    }

    public static RemoteViews applyStandardTemplate(Context context,
            CharSequence contentTitle, CharSequence contentText, CharSequence contentInfo,
            int number, int smallIcon, Bitmap largeIcon, CharSequence subText,
            boolean useChronometer, long when, int priority, int color, int resId,
            boolean fitIn1U) {
        Resources res = context.getResources();
        RemoteViews contentView = new RemoteViews(context.getPackageName(), resId);
        boolean showLine3 = false;
        boolean showLine2 = false;

        boolean minPriority = priority < NotificationCompat.PRIORITY_LOW;
        boolean afterJellyBean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
        boolean afterLollipop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        if (afterJellyBean && !afterLollipop) {
            // lets color the backgrounds
            if (minPriority) {
                contentView.setInt(R.id.notification_background,
                        "setBackgroundResource", R.drawable.notification_bg_low);
                contentView.setInt(R.id.icon,
                        "setBackgroundResource", R.drawable.notification_template_icon_low_bg);
            } else {
                contentView.setInt(R.id.notification_background,
                        "setBackgroundResource", R.drawable.notification_bg);
                contentView.setInt(R.id.icon,
                        "setBackgroundResource", R.drawable.notification_template_icon_bg);
            }
        }

        if (largeIcon != null) {
            // On versions before Jellybean, the large icon was shown by SystemUI, so we need to hide
            // it here.
            if (afterJellyBean) {
                contentView.setViewVisibility(R.id.icon, View.VISIBLE);
                contentView.setImageViewBitmap(R.id.icon, largeIcon);
            } else {
                contentView.setViewVisibility(R.id.icon, View.GONE);
            }
            if (smallIcon != 0) {
                int backgroundSize = res.getDimensionPixelSize(
                        R.dimen.notification_right_icon_size);
                int iconSize = backgroundSize - res.getDimensionPixelSize(
                        R.dimen.notification_small_icon_background_padding) * 2;
                if (afterLollipop) {
                    Bitmap smallBit = createIconWithBackground(context,
                            smallIcon,
                            backgroundSize,
                            iconSize,
                            color);
                    contentView.setImageViewBitmap(R.id.right_icon, smallBit);
                } else {
                    contentView.setImageViewBitmap(R.id.right_icon,
                            createColoredBitmap(context, smallIcon, Color.WHITE));
                }
                contentView.setViewVisibility(R.id.right_icon, View.VISIBLE);
            }
        } else if (smallIcon != 0) { // small icon at left
            contentView.setViewVisibility(R.id.icon, View.VISIBLE);
            if (afterLollipop) {
                int backgroundSize = res.getDimensionPixelSize(
                        R.dimen.notification_large_icon_width)
                        - res.getDimensionPixelSize(R.dimen.notification_big_circle_margin);
                int iconSize = res.getDimensionPixelSize(
                        R.dimen.notification_small_icon_size_as_large);
                Bitmap smallBit = createIconWithBackground(context,
                        smallIcon,
                        backgroundSize,
                        iconSize,
                        color);
                contentView.setImageViewBitmap(R.id.icon, smallBit);
            } else {
                contentView.setImageViewBitmap(R.id.icon,
                        createColoredBitmap(context, smallIcon, Color.WHITE));
            }
        }
        if (contentTitle != null) {
            contentView.setTextViewText(R.id.title, contentTitle);
        }
        if (contentText != null) {
            contentView.setTextViewText(R.id.text, contentText);
            showLine3 = true;
        }
        // If there is a large icon we have a right side
        boolean hasRightSide = !afterLollipop && largeIcon != null;
        if (contentInfo != null) {
            contentView.setTextViewText(R.id.info, contentInfo);
            contentView.setViewVisibility(R.id.info, View.VISIBLE);
            showLine3 = true;
            hasRightSide = true;
        } else if (number > 0) {
            final int tooBig = res.getInteger(
                    R.integer.status_bar_notification_info_maxnum);
            if (number > tooBig) {
                contentView.setTextViewText(R.id.info, ((Resources) res).getString(
                        R.string.status_bar_notification_info_overflow));
            } else {
                NumberFormat f = NumberFormat.getIntegerInstance();
                contentView.setTextViewText(R.id.info, f.format(number));
            }
            contentView.setViewVisibility(R.id.info, View.VISIBLE);
            showLine3 = true;
            hasRightSide = true;
        } else {
            contentView.setViewVisibility(R.id.info, View.GONE);
        }

        // Need to show three lines? Only allow on Jellybean+
        if (subText != null && afterJellyBean) {
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
        if (showLine2 && afterJellyBean) {
            if (fitIn1U) {
                // need to shrink all the type to make sure everything fits
                final float subTextSize = res.getDimensionPixelSize(
                        R.dimen.notification_subtext_size);
                contentView.setTextViewTextSize(R.id.text, TypedValue.COMPLEX_UNIT_PX, subTextSize);
            }
            // vertical centering
            contentView.setViewPadding(R.id.line1, 0, 0, 0, 0);
        }

        if (when != 0) {
            if (useChronometer && afterJellyBean) {
                contentView.setViewVisibility(R.id.chronometer, View.VISIBLE);
                contentView.setLong(R.id.chronometer, "setBase",
                        when + (SystemClock.elapsedRealtime() - System.currentTimeMillis()));
                contentView.setBoolean(R.id.chronometer, "setStarted", true);
            } else {
                contentView.setViewVisibility(R.id.time, View.VISIBLE);
                contentView.setLong(R.id.time, "setTime", when);
            }
            hasRightSide = true;
        }
        contentView.setViewVisibility(R.id.right_side, hasRightSide ? View.VISIBLE : View.GONE);
        contentView.setViewVisibility(R.id.line3, showLine3 ? View.VISIBLE : View.GONE);
        return contentView;
    }

    public static Bitmap createIconWithBackground(Context ctx, int iconId, int size, int iconSize,
            int color) {
        Bitmap coloredBitmap = createColoredBitmap(ctx, R.drawable.notification_icon_background,
                        color == NotificationCompat.COLOR_DEFAULT ? 0 : color, size);
        Canvas canvas = new Canvas(coloredBitmap);
        Drawable icon = ctx.getResources().getDrawable(iconId).mutate();
        icon.setFilterBitmap(true);
        int inset = (size - iconSize) / 2;
        icon.setBounds(inset, inset, iconSize + inset, iconSize + inset);
        icon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP));
        icon.draw(canvas);
        return coloredBitmap;
    }

    public static void buildIntoRemoteViews(Context ctx, RemoteViews outerView,
            RemoteViews innerView) {
        // this needs to be done fore the other calls, since otherwise we might hide the wrong
        // things if our ids collide.
        hideNormalContent(outerView);
        outerView.removeAllViews(R.id.notification_main_column);
        outerView.addView(R.id.notification_main_column, innerView.clone());
        outerView.setViewVisibility(R.id.notification_main_column, View.VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Adjust padding depending on font size.
            outerView.setViewPadding(R.id.notification_main_column_container,
                    0, calculateTopPadding(ctx), 0, 0);
        }
    }

    private static void hideNormalContent(RemoteViews outerView) {
        outerView.setViewVisibility(R.id.title, View.GONE);
        outerView.setViewVisibility(R.id.text2, View.GONE);
        outerView.setViewVisibility(R.id.text, View.GONE);
    }

    public static int calculateTopPadding(Context ctx) {
        int padding = ctx.getResources().getDimensionPixelSize(R.dimen.notification_top_pad);
        int largePadding = ctx.getResources().getDimensionPixelSize(
                R.dimen.notification_top_pad_large_text);
        float fontScale = ctx.getResources().getConfiguration().fontScale;
        float largeFactor = (constrain(fontScale, 1.0f, 1.3f) - 1f) / (1.3f - 1f);

        // Linearly interpolate the padding between large and normal with the font scale ranging
        // from 1f to LARGE_TEXT_SCALE
        return Math.round((1 - largeFactor) * padding + largeFactor * largePadding);
    }

    public static float constrain(float amount, float low, float high) {
        return amount < low ? low : (amount > high ? high : amount);
    }
}
