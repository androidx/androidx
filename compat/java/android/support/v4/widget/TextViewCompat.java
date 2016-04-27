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

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.widget.TextView;

/**
 * Helper for accessing features in {@link TextView} introduced after API level
 * 4 in a backwards compatible fashion.
 */
public final class TextViewCompat {

    // Hide constructor
    private TextViewCompat() {}

    interface TextViewCompatImpl {
        void setCompoundDrawablesRelative(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom);
        void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom);
        void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @DrawableRes int start, @DrawableRes int top, @DrawableRes int end,
                @DrawableRes int bottom);
        int getMaxLines(TextView textView);
        int getMinLines(TextView textView);
        void setTextAppearance(@NonNull TextView textView, @StyleRes int resId);
    }

    static class BaseTextViewCompatImpl implements TextViewCompatImpl {
        @Override
        public void setCompoundDrawablesRelative(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            textView.setCompoundDrawables(start, top, end, bottom);
        }

        @Override
        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            textView.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }

        @Override
        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @DrawableRes int start, @DrawableRes int top, @DrawableRes int end,
                @DrawableRes int bottom) {
            textView.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }

        @Override
        public int getMaxLines(TextView textView) {
            return TextViewCompatDonut.getMaxLines(textView);
        }

        @Override
        public int getMinLines(TextView textView) {
            return TextViewCompatDonut.getMinLines(textView);
        }

        @Override
        public void setTextAppearance(TextView textView, @StyleRes int resId) {
            TextViewCompatDonut.setTextAppearance(textView, resId);
        }
    }

    static class JbTextViewCompatImpl extends BaseTextViewCompatImpl {
        @Override
        public int getMaxLines(TextView textView) {
            return TextViewCompatJb.getMaxLines(textView);
        }

        @Override
        public int getMinLines(TextView textView) {
            return TextViewCompatJb.getMinLines(textView);
        }
    }

    static class JbMr1TextViewCompatImpl extends JbTextViewCompatImpl {
        @Override
        public void setCompoundDrawablesRelative(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            TextViewCompatJbMr1.setCompoundDrawablesRelative(textView, start, top, end, bottom);
        }

        @Override
        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            TextViewCompatJbMr1.setCompoundDrawablesRelativeWithIntrinsicBounds(textView,
                    start, top, end, bottom);
        }

        @Override
        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @DrawableRes int start, @DrawableRes int top, @DrawableRes int end,
                @DrawableRes int bottom) {
            TextViewCompatJbMr1.setCompoundDrawablesRelativeWithIntrinsicBounds(textView,
                    start, top, end, bottom);
        }
    }

    static class JbMr2TextViewCompatImpl extends JbMr1TextViewCompatImpl {
        @Override
        public void setCompoundDrawablesRelative(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            TextViewCompatJbMr2.setCompoundDrawablesRelative(textView, start, top, end, bottom);
        }

        @Override
        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            TextViewCompatJbMr2
                    .setCompoundDrawablesRelativeWithIntrinsicBounds(textView, start, top, end,
                            bottom);
        }

        @Override
        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @DrawableRes int start, @DrawableRes int top, @DrawableRes int end,
                @DrawableRes int bottom) {
            TextViewCompatJbMr2.setCompoundDrawablesRelativeWithIntrinsicBounds(textView,
                    start, top, end, bottom);
        }
    }

    static class Api23TextViewCompatImpl extends JbMr2TextViewCompatImpl {
        @Override
        public void setTextAppearance(@NonNull TextView textView, @StyleRes int resId) {
            TextViewCompatApi23.setTextAppearance(textView, resId);
        }
    }

    static final TextViewCompatImpl IMPL;

    static {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 23) {
            IMPL = new Api23TextViewCompatImpl();
        } else if (version >= 18) {
            IMPL = new JbMr2TextViewCompatImpl();
        } else if (version >= 17) {
            IMPL = new JbMr1TextViewCompatImpl();
        } else if (version >= 16) {
            IMPL = new JbTextViewCompatImpl();
        } else {
            IMPL = new BaseTextViewCompatImpl();
        }
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use {@code null} if you do not want a Drawable
     * there. The Drawables must already have had {@link Drawable#setBounds}
     * called.
     * <p/>
     * Calling this method will overwrite any Drawables previously set using
     * {@link TextView#setCompoundDrawables} or related methods.
     *
     * @param textView The TextView against which to invoke the method.
     * @attr name android:drawableStart
     * @attr name android:drawableTop
     * @attr name android:drawableEnd
     * @attr name android:drawableBottom
     */
    public static void setCompoundDrawablesRelative(@NonNull TextView textView,
            @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
            @Nullable Drawable bottom) {
        IMPL.setCompoundDrawablesRelative(textView, start, top, end, bottom);
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use {@code null} if you do not want a Drawable
     * there. The Drawables' bounds will be set to their intrinsic bounds.
     * <p/>
     * Calling this method will overwrite any Drawables previously set using
     * {@link TextView#setCompoundDrawables} or related methods.
     *
     * @param textView The TextView against which to invoke the method.
     * @attr name android:drawableStart
     * @attr name android:drawableTop
     * @attr name android:drawableEnd
     * @attr name android:drawableBottom
     */
    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
            @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
            @Nullable Drawable bottom) {
        IMPL.setCompoundDrawablesRelativeWithIntrinsicBounds(textView, start, top, end, bottom);
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use 0 if you do not want a Drawable there. The
     * Drawables' bounds will be set to their intrinsic bounds.
     * <p/>
     * Calling this method will overwrite any Drawables previously set using
     * {@link TextView#setCompoundDrawables} or related methods.
     *
     * @param textView The TextView against which to invoke the method.
     * @param start    Resource identifier of the start Drawable.
     * @param top      Resource identifier of the top Drawable.
     * @param end      Resource identifier of the end Drawable.
     * @param bottom   Resource identifier of the bottom Drawable.
     * @attr name android:drawableStart
     * @attr name android:drawableTop
     * @attr name android:drawableEnd
     * @attr name android:drawableBottom
     */
    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
            @DrawableRes int start, @DrawableRes int top, @DrawableRes int end,
            @DrawableRes int bottom) {
        IMPL.setCompoundDrawablesRelativeWithIntrinsicBounds(textView, start, top, end, bottom);
    }

    /**
     * Returns the maximum number of lines displayed in the given TextView, or -1 if the maximum
     * height was set in pixels instead.
     */
    public static int getMaxLines(@NonNull TextView textView) {
        return IMPL.getMaxLines(textView);
    }

    /**
     * Returns the minimum number of lines displayed in the given TextView, or -1 if the minimum
     * height was set in pixels instead.
     */
    public static int getMinLines(@NonNull TextView textView) {
        return IMPL.getMinLines(textView);
    }

    /**
     * Sets the text appearance from the specified style resource.
     * <p>
     * Use a framework-defined {@code TextAppearance} style like
     * {@link android.R.style#TextAppearance_Material_Body1 @android:style/TextAppearance.Material.Body1}
     * or see {@link android.R.styleable#TextAppearance TextAppearance} for the
     * set of attributes that can be used in a custom style.
     *
     * @param textView The TextView against which to invoke the method.
     * @param resId    The resource identifier of the style to apply.
     */
    public static void setTextAppearance(@NonNull TextView textView, @StyleRes int resId) {
        IMPL.setTextAppearance(textView, resId);
    }
}
