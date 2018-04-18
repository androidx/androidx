/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Calendar;

/**
 * A bunch of utilities for slice UI.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SliceViewUtil {

    /**
     */
    @ColorInt
    public static int getColorAccent(@NonNull Context context) {
        return getColorAttr(context, android.R.attr.colorAccent);
    }

    /**
     */
    @ColorInt
    public static int getColorError(@NonNull Context context) {
        return getColorAttr(context, android.R.attr.colorError);
    }

    /**
     */
    @ColorInt
    public static int getDefaultColor(@NonNull Context context, int resId) {
        final ColorStateList list = ContextCompat.getColorStateList(context, resId);
        return list.getDefaultColor();
    }

    /**
     */
    @ColorInt
    public static int getDisabled(@NonNull Context context, int inputColor) {
        return applyAlphaAttr(context, android.R.attr.disabledAlpha, inputColor);
    }

    /**
     */
    @ColorInt
    public static int applyAlphaAttr(@NonNull Context context, @AttrRes int attr, int inputColor) {
        TypedArray ta = context.obtainStyledAttributes(new int[] {
                attr
        });
        float alpha = ta.getFloat(0, 0);
        ta.recycle();
        return applyAlpha(alpha, inputColor);
    }

    /**
     */
    @ColorInt
    public static int applyAlpha(float alpha, int inputColor) {
        alpha *= Color.alpha(inputColor);
        return Color.argb((int) (alpha), Color.red(inputColor), Color.green(inputColor),
                Color.blue(inputColor));
    }

    /**
     */
    @ColorInt
    public static int getColorAttr(@NonNull Context context, @AttrRes int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[] {
                attr
        });
        @ColorInt int colorAccent = ta.getColor(0, 0);
        ta.recycle();
        return colorAccent;
    }

    /**
     */
    public static int getThemeAttr(@NonNull Context context, @AttrRes int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[] {
                attr
        });
        int theme = ta.getResourceId(0, 0);
        ta.recycle();
        return theme;
    }

    /**
     */
    public static Drawable getDrawable(@NonNull Context context, @AttrRes int attr) {
        TypedArray ta = context.obtainStyledAttributes(new int[] {
                attr
        });
        Drawable drawable = ta.getDrawable(0);
        ta.recycle();
        return drawable;
    }

    /**
     */
    public static IconCompat createIconFromDrawable(Drawable d) {
        if (d instanceof BitmapDrawable) {
            return IconCompat.createWithBitmap(((BitmapDrawable) d).getBitmap());
        }
        Bitmap b = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        d.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        d.draw(canvas);
        return IconCompat.createWithBitmap(b);
    }

    /**
     */
    public static void createCircledIcon(@NonNull Context context, int iconSizePx,
            IconCompat icon, boolean isLarge, ViewGroup parent) {
        ImageView v = new ImageView(context);
        v.setImageDrawable(icon.loadDrawable(context));
        v.setScaleType(isLarge ? ImageView.ScaleType.CENTER_CROP
                : ImageView.ScaleType.CENTER_INSIDE);
        parent.addView(v);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
        if (isLarge) {
            // XXX better way to convert from icon -> bitmap or crop an icon (?)
            Bitmap iconBm = Bitmap.createBitmap(iconSizePx, iconSizePx, Config.ARGB_8888);
            Canvas iconCanvas = new Canvas(iconBm);
            v.layout(0, 0, iconSizePx, iconSizePx);
            v.draw(iconCanvas);
            v.setImageBitmap(getCircularBitmap(iconBm));
        } else {
            v.setColorFilter(Color.WHITE);
        }
        lp.width = iconSizePx;
        lp.height = iconSizePx;
        lp.gravity = Gravity.CENTER;
    }

    /**
     */
    public static @NonNull Bitmap getCircularBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    /**
     */
    public static CharSequence getRelativeTimeString(long time) {
        return DateUtils.getRelativeTimeSpanString(time, Calendar.getInstance().getTimeInMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    private SliceViewUtil() {
    }
}
