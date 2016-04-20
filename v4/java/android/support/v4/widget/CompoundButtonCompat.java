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

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.widget.CompoundButton;

/**
 * Helper for accessing {@link android.widget.CompoundButton} methods introduced after
 * API level 4 in a backwards compatible fashion.
 */
public final class CompoundButtonCompat {

    private static final CompoundButtonCompatImpl IMPL;

    static {
        final int sdk = Build.VERSION.SDK_INT;
        if (sdk >= 23) {
            IMPL = new Api23CompoundButtonImpl();
        } else if (sdk >= 21) {
            IMPL = new LollipopCompoundButtonImpl();
        } else {
            IMPL = new BaseCompoundButtonCompat();
        }
    }

    interface CompoundButtonCompatImpl {
        void setButtonTintList(CompoundButton button, ColorStateList tint);
        ColorStateList getButtonTintList(CompoundButton button);
        void setButtonTintMode(CompoundButton button, PorterDuff.Mode tintMode);
        PorterDuff.Mode getButtonTintMode(CompoundButton button);
        Drawable getButtonDrawable(CompoundButton button);
    }

    static class BaseCompoundButtonCompat implements CompoundButtonCompatImpl {
        @Override
        public void setButtonTintList(CompoundButton button, ColorStateList tint) {
            CompoundButtonCompatDonut.setButtonTintList(button, tint);
        }

        @Override
        public ColorStateList getButtonTintList(CompoundButton button) {
            return CompoundButtonCompatDonut.getButtonTintList(button);
        }

        @Override
        public void setButtonTintMode(CompoundButton button, PorterDuff.Mode tintMode) {
            CompoundButtonCompatDonut.setButtonTintMode(button, tintMode);
        }

        @Override
        public PorterDuff.Mode getButtonTintMode(CompoundButton button) {
            return CompoundButtonCompatDonut.getButtonTintMode(button);
        }

        @Override
        public Drawable getButtonDrawable(CompoundButton button) {
            return CompoundButtonCompatDonut.getButtonDrawable(button);
        }
    }

    static class LollipopCompoundButtonImpl extends BaseCompoundButtonCompat {
        @Override
        public void setButtonTintList(CompoundButton button, ColorStateList tint) {
            CompoundButtonCompatLollipop.setButtonTintList(button, tint);
        }

        @Override
        public ColorStateList getButtonTintList(CompoundButton button) {
            return CompoundButtonCompatLollipop.getButtonTintList(button);
        }

        @Override
        public void setButtonTintMode(CompoundButton button, PorterDuff.Mode tintMode) {
            CompoundButtonCompatLollipop.setButtonTintMode(button, tintMode);
        }

        @Override
        public PorterDuff.Mode getButtonTintMode(CompoundButton button) {
            return CompoundButtonCompatLollipop.getButtonTintMode(button);
        }
    }

    static class Api23CompoundButtonImpl extends LollipopCompoundButtonImpl {
        @Override
        public Drawable getButtonDrawable(CompoundButton button) {
            return CompoundButtonCompatApi23.getButtonDrawable(button);
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
     * @see #setButtonTintMode(PorterDuff.Mode)
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
