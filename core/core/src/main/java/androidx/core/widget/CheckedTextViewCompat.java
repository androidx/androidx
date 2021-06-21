/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.core.widget;

import static android.os.Build.VERSION.SDK_INT;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.graphics.drawable.DrawableCompat;

import java.lang.reflect.Field;

/**
 * Helper for accessing {@link CheckedTextView}.
 */
public final class CheckedTextViewCompat {
    private static final String TAG = "CheckedTextViewCompat";

    private CheckedTextViewCompat() {
    }

    /**
     * Applies a tint to the check mark drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link CheckedTextView#setCheckMarkDrawable(Drawable)} should
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link DrawableCompat#setTintList(Drawable, ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     * @see #setCheckMarkTintList(CheckedTextView, ColorStateList)
     */
    public static void setCheckMarkTintList(@NonNull CheckedTextView textView,
            @Nullable ColorStateList tint) {
        if (SDK_INT >= 21) {
            Api21Impl.setCheckMarkTintList(textView, tint);
        } else if (textView instanceof TintableCheckedTextView) {
            ((TintableCheckedTextView) textView).setSupportCheckMarkTintList(tint);
        }
    }

    /**
     * Returns the tint applied to the check mark drawable
     *
     * @see #setCheckMarkTintList(CheckedTextView, ColorStateList)
     */
    @Nullable
    public static ColorStateList getCheckMarkTintList(@NonNull CheckedTextView textView) {
        if (SDK_INT >= 21) {
            return Api21Impl.getCheckMarkTintList(textView);
        }
        if (textView instanceof TintableCheckedTextView) {
            return ((TintableCheckedTextView) textView).getSupportCheckMarkTintList();
        }
        return null;
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setCheckMarkTintList(CheckedTextView, ColorStateList)}} to the check mark drawable.
     * The default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     * @see #getCheckMarkTintMode(CheckedTextView)
     * @see DrawableCompat#setTintMode(Drawable, PorterDuff.Mode)
     */
    public static void setCheckMarkTintMode(@NonNull CheckedTextView textView,
            @Nullable PorterDuff.Mode tintMode) {
        if (SDK_INT >= 21) {
            Api21Impl.setCheckMarkTintMode(textView, tintMode);
        } else if (textView instanceof TintableCheckedTextView) {
            ((TintableCheckedTextView) textView).setSupportCheckMarkTintMode(tintMode);
        }
    }

    /**
     * @return the blending mode used to apply the tint to the check mark drawable
     * @attr name android:checkMarkTintMode
     * @see #setCheckMarkTintMode(CheckedTextView, PorterDuff.Mode)
     */
    @Nullable
    public static PorterDuff.Mode getCheckMarkTintMode(@NonNull CheckedTextView textView) {
        if (SDK_INT >= 21) {
            return Api21Impl.getCheckMarkTintMode(textView);
        }
        if (textView instanceof TintableCheckedTextView) {
            return ((TintableCheckedTextView) textView).getSupportCheckMarkTintMode();
        }
        return null;
    }

    /**
     * Returns the drawable used as the check mark image
     *
     * @see CheckedTextView#setCheckMarkDrawable(Drawable)
     */
    @Nullable
    public static Drawable getCheckMarkDrawable(@NonNull CheckedTextView textView) {
        if (SDK_INT >= 16) {
            return Api16Impl.getCheckMarkDrawable(textView);
        } else {
            return Api14Impl.getCheckMarkDrawable(textView);
        }
    }

    @RequiresApi(21)
    private static class Api21Impl {

        private Api21Impl() {
        }

        static void setCheckMarkTintList(@NonNull CheckedTextView textView,
                @Nullable ColorStateList tint) {
            textView.setCheckMarkTintList(tint);
        }

        @Nullable
        static ColorStateList getCheckMarkTintList(@NonNull CheckedTextView textView) {
            return textView.getCheckMarkTintList();
        }

        static void setCheckMarkTintMode(@NonNull CheckedTextView textView,
                @Nullable PorterDuff.Mode tintMode) {
            textView.setCheckMarkTintMode(tintMode);
        }

        @Nullable
        static PorterDuff.Mode getCheckMarkTintMode(@NonNull CheckedTextView textView) {
            return textView.getCheckMarkTintMode();
        }
    }

    @RequiresApi(16)
    private static class Api16Impl {

        private Api16Impl() {
        }

        @Nullable
        static Drawable getCheckMarkDrawable(@NonNull CheckedTextView textView) {
            return textView.getCheckMarkDrawable();
        }
    }

    private static class Api14Impl {

        private static Field sCheckMarkDrawableField;
        private static boolean sResolved;

        private Api14Impl() {
        }

        @Nullable
        static Drawable getCheckMarkDrawable(@NonNull CheckedTextView textView) {
            if (!sResolved) {
                try {
                    sCheckMarkDrawableField =
                            CheckedTextView.class.getDeclaredField("mCheckMarkDrawable");
                    sCheckMarkDrawableField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    Log.i(TAG, "Failed to retrieve mCheckMarkDrawable field", e);
                }
                sResolved = true;
            }

            if (sCheckMarkDrawableField != null) {
                try {
                    return (Drawable) sCheckMarkDrawableField.get(textView);
                } catch (IllegalAccessException e) {
                    Log.i(TAG, "Failed to get check mark drawable via reflection", e);
                    sCheckMarkDrawableField = null;
                }
            }
            return null;
        }
    }
}
