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
 * limitations under the License.
 */

package android.support.v4.widget;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.widget.CompoundButton;

import java.lang.reflect.Field;

/**
 * Helper for accessing {@link android.widget.CompoundButton}.
 */
public final class CompoundButtonCompat {

    private static final CompoundButtonCompatBaseImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 23) {
            IMPL = new CompoundButtonCompatApi23Impl();
        } else if (Build.VERSION.SDK_INT >= 21) {
            IMPL = new CompoundButtonCompatApi21Impl();
        } else {
            IMPL = new CompoundButtonCompatBaseImpl();
        }
    }

    static class CompoundButtonCompatBaseImpl {
        private static final String TAG = "CompoundButtonCompat";

        private static Field sButtonDrawableField;
        private static boolean sButtonDrawableFieldFetched;

        public void setButtonTintList(CompoundButton button, ColorStateList tint) {
            if (button instanceof TintableCompoundButton) {
                ((TintableCompoundButton) button).setSupportButtonTintList(tint);
            }
        }

        public ColorStateList getButtonTintList(CompoundButton button) {
            if (button instanceof TintableCompoundButton) {
                return ((TintableCompoundButton) button).getSupportButtonTintList();
            }
            return null;
        }

        public void setButtonTintMode(CompoundButton button, PorterDuff.Mode tintMode) {
            if (button instanceof TintableCompoundButton) {
                ((TintableCompoundButton) button).setSupportButtonTintMode(tintMode);
            }
        }

        public PorterDuff.Mode getButtonTintMode(CompoundButton button) {
            if (button instanceof TintableCompoundButton) {
                return ((TintableCompoundButton) button).getSupportButtonTintMode();
            }
            return null;
        }

        public Drawable getButtonDrawable(CompoundButton button) {
            if (!sButtonDrawableFieldFetched) {
                try {
                    sButtonDrawableField = CompoundButton.class.getDeclaredField("mButtonDrawable");
                    sButtonDrawableField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    Log.i(TAG, "Failed to retrieve mButtonDrawable field", e);
                }
                sButtonDrawableFieldFetched = true;
            }

            if (sButtonDrawableField != null) {
                try {
                    return (Drawable) sButtonDrawableField.get(button);
                } catch (IllegalAccessException e) {
                    Log.i(TAG, "Failed to get button drawable via reflection", e);
                    sButtonDrawableField = null;
                }
            }
            return null;
        }
    }

    @RequiresApi(21)
    static class CompoundButtonCompatApi21Impl extends CompoundButtonCompatBaseImpl {
        @Override
        public void setButtonTintList(CompoundButton button, ColorStateList tint) {
            button.setButtonTintList(tint);
        }

        @Override
        public ColorStateList getButtonTintList(CompoundButton button) {
            return button.getButtonTintList();
        }

        @Override
        public void setButtonTintMode(CompoundButton button, PorterDuff.Mode tintMode) {
            button.setButtonTintMode(tintMode);
        }

        @Override
        public PorterDuff.Mode getButtonTintMode(CompoundButton button) {
            return button.getButtonTintMode();
        }
    }

    @RequiresApi(23)
    static class CompoundButtonCompatApi23Impl extends CompoundButtonCompatApi21Impl {
        @Override
        public Drawable getButtonDrawable(CompoundButton button) {
            return button.getButtonDrawable();
        }
    }

    private CompoundButtonCompat() {}

    /**
     * Applies a tint to the button drawable. Does not modify the current tint
     * mode, which is {@link PorterDuff.Mode#SRC_IN} by default.
     * <p>
     * Subsequent calls to {@link CompoundButton#setButtonDrawable(Drawable)} should
     * automatically mutate the drawable and apply the specified tint and tint
     * mode using {@link DrawableCompat#setTintList(Drawable, ColorStateList)}.
     *
     * @param tint the tint to apply, may be {@code null} to clear tint
     *
     * @see #setButtonTintList(CompoundButton, ColorStateList)
     */
    public static void setButtonTintList(@NonNull CompoundButton button, @Nullable ColorStateList tint) {
        IMPL.setButtonTintList(button, tint);
    }

    /**
     * Returns the tint applied to the button drawable
     *
     * @see #setButtonTintList(CompoundButton, ColorStateList)
     */
    @Nullable
    public static ColorStateList getButtonTintList(@NonNull CompoundButton button) {
        return IMPL.getButtonTintList(button);
    }

    /**
     * Specifies the blending mode used to apply the tint specified by
     * {@link #setButtonTintList(CompoundButton, ColorStateList)}} to the button drawable. The
     * default mode is {@link PorterDuff.Mode#SRC_IN}.
     *
     * @param tintMode the blending mode used to apply the tint, may be
     *                 {@code null} to clear tint
     *
     * @see #getButtonTintMode(CompoundButton)
     * @see DrawableCompat#setTintMode(Drawable, PorterDuff.Mode)
     */
    public static void setButtonTintMode(@NonNull CompoundButton button,
            @Nullable PorterDuff.Mode tintMode) {
        IMPL.setButtonTintMode(button, tintMode);
    }

    /**
     * @return the blending mode used to apply the tint to the button drawable
     * @attr name android:buttonTintMode
     * @see #setButtonTintMode(CompoundButton, PorterDuff.Mode)
     */
    @Nullable
    public static PorterDuff.Mode getButtonTintMode(@NonNull CompoundButton button) {
        return IMPL.getButtonTintMode(button);
    }

    /**
     * Returns the drawable used as the compound button image
     *
     * @see CompoundButton#setButtonDrawable(Drawable)
     */
    @Nullable
    public static Drawable getButtonDrawable(@NonNull CompoundButton button) {
        return IMPL.getButtonDrawable(button);
    }
}
