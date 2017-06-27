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
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v7.appcompat.R;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.text.NumberFormat;

/**
 * Helper class to generate MediaStyle notifications for pre-Lollipop platforms. Overrides
 * contentView and bigContentView of the notification.
 */
@RequiresApi(9)
class NotificationCompatImplBase {

    static Bitmap createColoredBitmap(Context context, int iconId, int color) {
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

    public static RemoteViews applyStandardTemplate(NotificationCompat.Builder builder,
            boolean showSmallIcon, int resId, boolean fitIn1U) {
        Resources res = builder.mContext.getResources();
        RemoteViews contentView = new RemoteViews(builder.mContext.getPackageName(), resId);
        boolean showLine3 = false;
        boolean showLine2 = false;

        boolean minPriority = builder.getPriority() < NotificationCompat.PRIORITY_LOW;
        if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 21) {
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

        if (builder.mLargeIcon != null) {
            // On versions before Jellybean, the large icon was shown by SystemUI, so we need to hide
            // it here.
            if (Build.VERSION.SDK_INT >= 16) {
                contentView.setViewVisibility(R.id.icon, View.VISIBLE);
                contentView.setImageViewBitmap(R.id.icon, builder.mLargeIcon);
            } else {
                contentView.setViewVisibility(R.id.icon, View.GONE);
            }
            if (showSmallIcon && builder.mNotification.icon != 0) {
                int backgroundSize = res.getDimensionPixelSize(
                        R.dimen.notification_right_icon_size);
                int iconSize = backgroundSize - res.getDimensionPixelSize(
                        R.dimen.notification_small_icon_background_padding) * 2;
                if (Build.VERSION.SDK_INT >= 21) {
                    Bitmap smallBit = createIconWithBackground(builder.mContext,
                            builder.mNotification.icon,
                            backgroundSize,
                            iconSize,
                            builder.getColor());
                    contentView.setImageViewBitmap(R.id.right_icon, smallBit);
                } else {
                    contentView.setImageViewBitmap(R.id.right_icon, createColoredBitmap(
                            builder.mContext, builder.mNotification.icon, Color.WHITE));
                }
                contentView.setViewVisibility(R.id.right_icon, View.VISIBLE);
            }
        } else if (showSmallIcon && builder.mNotification.icon != 0) { // small icon at left
            contentView.setViewVisibility(R.id.icon, View.VISIBLE);
            if (Build.VERSION.SDK_INT >= 21) {
                int backgroundSize = res.getDimensionPixelSize(
                        R.dimen.notification_large_icon_width)
                        - res.getDimensionPixelSize(R.dimen.notification_big_circle_margin);
                int iconSize = res.getDimensionPixelSize(
                        R.dimen.notification_small_icon_size_as_large);
                Bitmap smallBit = createIconWithBackground(builder.mContext,
                        builder.mNotification.icon,
                        backgroundSize,
                        iconSize,
                        builder.getColor());
                contentView.setImageViewBitmap(R.id.icon, smallBit);
            } else {
                contentView.setImageViewBitmap(R.id.icon, createColoredBitmap(
                        builder.mContext, builder.mNotification.icon, Color.WHITE));
            }
        }
        if (builder.mContentTitle != null) {
            contentView.setTextViewText(R.id.title, builder.mContentTitle);
        }
        if (builder.mContentText != null) {
            contentView.setTextViewText(R.id.text, builder.mContentText);
            showLine3 = true;
        }
        // If there is a large icon we have a right side
        boolean hasRightSide = !(Build.VERSION.SDK_INT >= 21) && builder.mLargeIcon != null;
        if (builder.mContentInfo != null) {
            contentView.setTextViewText(R.id.info, builder.mContentInfo);
            contentView.setViewVisibility(R.id.info, View.VISIBLE);
            showLine3 = true;
            hasRightSide = true;
        } else if (builder.mNumber > 0) {
            final int tooBig = res.getInteger(
                    R.integer.status_bar_notification_info_maxnum);
            if (builder.mNumber > tooBig) {
                contentView.setTextViewText(R.id.info, ((Resources) res).getString(
                        R.string.status_bar_notification_info_overflow));
            } else {
                NumberFormat f = NumberFormat.getIntegerInstance();
                contentView.setTextViewText(R.id.info, f.format(builder.mNumber));
            }
            contentView.setViewVisibility(R.id.info, View.VISIBLE);
            showLine3 = true;
            hasRightSide = true;
        } else {
            contentView.setViewVisibility(R.id.info, View.GONE);
        }

        // Need to show three lines? Only allow on Jellybean+
        if (builder.mSubText != null && Build.VERSION.SDK_INT >= 16) {
            contentView.setTextViewText(R.id.text, builder.mSubText);
            if (builder.mContentText != null) {
                contentView.setTextViewText(R.id.text2, builder.mContentText);
                contentView.setViewVisibility(R.id.text2, View.VISIBLE);
                showLine2 = true;
            } else {
                contentView.setViewVisibility(R.id.text2, View.GONE);
            }
        }

        // RemoteViews.setViewPadding and RemoteViews.setTextViewTextSize is not available on ICS-
        if (showLine2 && Build.VERSION.SDK_INT >= 16) {
            if (fitIn1U) {
                // need to shrink all the type to make sure everything fits
                final float subTextSize = res.getDimensionPixelSize(
                        R.dimen.notification_subtext_size);
                contentView.setTextViewTextSize(R.id.text, TypedValue.COMPLEX_UNIT_PX, subTextSize);
            }
            // vertical centering
            contentView.setViewPadding(R.id.line1, 0, 0, 0, 0);
        }

        if (builder.getWhenIfShowing() != 0) {
            if (builder.mUseChronometer && Build.VERSION.SDK_INT >= 16) {
                contentView.setViewVisibility(R.id.chronometer, View.VISIBLE);
                contentView.setLong(R.id.chronometer, "setBase",
                        builder.getWhenIfShowing()
                                + (SystemClock.elapsedRealtime() - System.currentTimeMillis()));
                contentView.setBoolean(R.id.chronometer, "setStarted", true);
            } else {
                contentView.setViewVisibility(R.id.time, View.VISIBLE);
                contentView.setLong(R.id.time, "setTime", builder.getWhenIfShowing());
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
