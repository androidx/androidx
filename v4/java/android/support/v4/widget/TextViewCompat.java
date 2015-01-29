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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.TextView;

/**
 * Helper for accessing features in {@link TextView} introduced after API level
 * 4 in a backwards compatible fashion.
 */
public class TextViewCompat {

    // Hide constructor
    private TextViewCompat() {
    }

    interface TextViewCompatImpl {

        public void setCompoundDrawablesRelative(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom);

        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom);

        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                int start, int top, int end, int bottom);

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
                int start, int top, int end, int bottom) {
            textView.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }

    }

    static class JbMr1TextViewCompatImpl extends BaseTextViewCompatImpl {

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
                int start, int top, int end, int bottom) {
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
                int start, int top, int end, int bottom) {
            TextViewCompatJbMr2.setCompoundDrawablesRelativeWithIntrinsicBounds(textView,
                    start, top, end, bottom);
        }
    }

    static final TextViewCompatImpl IMPL;

    static {
        final int version = Build.VERSION.SDK_INT;
        if (version >= 18) {
            IMPL = new JbMr2TextViewCompatImpl();
        } else if (version >= 17) {
            IMPL = new JbMr1TextViewCompatImpl();
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
     * @attr ref android.R.styleable#TextView_drawableStart
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableEnd
     * @attr ref android.R.styleable#TextView_drawableBottom
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
     * @attr ref android.R.styleable#TextView_drawableStart
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableEnd
     * @attr ref android.R.styleable#TextView_drawableBottom
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
     * @attr ref android.R.styleable#TextView_drawableStart
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableEnd
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
            int start, int top, int end, int bottom) {
        IMPL.setCompoundDrawablesRelativeWithIntrinsicBounds(textView, start, top, end, bottom);
    }

}
