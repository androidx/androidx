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

import android.annotation.TargetApi;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Field;

/**
 * Helper for accessing features in {@link TextView} in a backwards compatible fashion.
 */
public final class TextViewCompat {

    // Hide constructor
    private TextViewCompat() {}

    static class TextViewCompatBaseImpl {
        private static final String LOG_TAG = "TextViewCompatBase";
        private static final int LINES = 1;

        private static Field sMaximumField;
        private static boolean sMaximumFieldFetched;
        private static Field sMaxModeField;
        private static boolean sMaxModeFieldFetched;

        private static Field sMinimumField;
        private static boolean sMinimumFieldFetched;
        private static Field sMinModeField;
        private static boolean sMinModeFieldFetched;

        public void setCompoundDrawablesRelative(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            textView.setCompoundDrawables(start, top, end, bottom);
        }

        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            textView.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }

        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @DrawableRes int start, @DrawableRes int top, @DrawableRes int end,
                @DrawableRes int bottom) {
            textView.setCompoundDrawablesWithIntrinsicBounds(start, top, end, bottom);
        }

        private static Field retrieveField(String fieldName) {
            Field field = null;
            try {
                field = TextView.class.getDeclaredField(fieldName);
                field.setAccessible(true);
            } catch (NoSuchFieldException e) {
                Log.e(LOG_TAG, "Could not retrieve " + fieldName + " field.");
            }
            return field;
        }

        private static int retrieveIntFromField(Field field, TextView textView) {
            try {
                return field.getInt(textView);
            } catch (IllegalAccessException e) {
                Log.d(LOG_TAG, "Could not retrieve value of " + field.getName() + " field.");
            }
            return -1;
        }

        public int getMaxLines(TextView textView) {
            if (!sMaxModeFieldFetched) {
                sMaxModeField = retrieveField("mMaxMode");
                sMaxModeFieldFetched = true;
            }
            if (sMaxModeField != null && retrieveIntFromField(sMaxModeField, textView) == LINES) {
                // If the max mode is using lines, we can grab the maximum value
                if (!sMaximumFieldFetched) {
                    sMaximumField = retrieveField("mMaximum");
                    sMaximumFieldFetched = true;
                }
                if (sMaximumField != null) {
                    return retrieveIntFromField(sMaximumField, textView);
                }
            }
            return -1;
        }

        public int getMinLines(TextView textView) {
            if (!sMinModeFieldFetched) {
                sMinModeField = retrieveField("mMinMode");
                sMinModeFieldFetched = true;
            }
            if (sMinModeField != null && retrieveIntFromField(sMinModeField, textView) == LINES) {
                // If the min mode is using lines, we can grab the maximum value
                if (!sMinimumFieldFetched) {
                    sMinimumField = retrieveField("mMinimum");
                    sMinimumFieldFetched = true;
                }
                if (sMinimumField != null) {
                    return retrieveIntFromField(sMinimumField, textView);
                }
            }
            return -1;
        }

        @SuppressWarnings("deprecation")
        public void setTextAppearance(TextView textView, @StyleRes int resId) {
            textView.setTextAppearance(textView.getContext(), resId);
        }

        public Drawable[] getCompoundDrawablesRelative(@NonNull TextView textView) {
            return textView.getCompoundDrawables();
        }
    }

    @TargetApi(16)
    static class TextViewCompatApi16Impl extends TextViewCompatBaseImpl {
        @Override
        public int getMaxLines(TextView textView) {
            return textView.getMaxLines();
        }

        @Override
        public int getMinLines(TextView textView) {
            return textView.getMinLines();
        }
    }

    @TargetApi(17)
    static class TextViewCompatApi17Impl extends TextViewCompatApi16Impl {
        @Override
        public void setCompoundDrawablesRelative(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            textView.setCompoundDrawables(rtl ? end : start, top, rtl ? start : end, bottom);
        }

        @Override
        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            textView.setCompoundDrawablesWithIntrinsicBounds(rtl ? end : start, top,
                    rtl ? start : end,  bottom);
        }

        @Override
        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @DrawableRes int start, @DrawableRes int top, @DrawableRes int end,
                @DrawableRes int bottom) {
            boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            textView.setCompoundDrawablesWithIntrinsicBounds(rtl ? end : start, top,
                    rtl ? start : end, bottom);
        }

        @Override
        public Drawable[] getCompoundDrawablesRelative(@NonNull TextView textView) {
            final boolean rtl = textView.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
            final Drawable[] compounds = textView.getCompoundDrawables();
            if (rtl) {
                // If we're on RTL, we need to invert the horizontal result like above
                final Drawable start = compounds[2];
                final Drawable end = compounds[0];
                compounds[0] = start;
                compounds[2] = end;
            }
            return compounds;
        }
    }

    @TargetApi(18)
    static class TextViewCompatApi18Impl extends TextViewCompatApi17Impl {
        @Override
        public void setCompoundDrawablesRelative(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            textView.setCompoundDrawablesRelative(start, top, end, bottom);
        }

        @Override
        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @Nullable Drawable start, @Nullable Drawable top, @Nullable Drawable end,
                @Nullable Drawable bottom) {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
        }

        @Override
        public void setCompoundDrawablesRelativeWithIntrinsicBounds(@NonNull TextView textView,
                @DrawableRes int start, @DrawableRes int top, @DrawableRes int end,
                @DrawableRes int bottom) {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom);
        }

        @Override
        public Drawable[] getCompoundDrawablesRelative(@NonNull TextView textView) {
            return textView.getCompoundDrawablesRelative();
        }
    }

    @TargetApi(23)
    static class TextViewCompatApi23Impl extends TextViewCompatApi18Impl {
        @Override
        public void setTextAppearance(@NonNull TextView textView, @StyleRes int resId) {
            textView.setTextAppearance(resId);
        }
    }

    static final TextViewCompatBaseImpl IMPL;

    static {
        if (Build.VERSION.SDK_INT >= 23) {
            IMPL = new TextViewCompatApi23Impl();
        } else if (Build.VERSION.SDK_INT >= 18) {
            IMPL = new TextViewCompatApi18Impl();
        } else if (Build.VERSION.SDK_INT >= 17) {
            IMPL = new TextViewCompatApi17Impl();
        } else if (Build.VERSION.SDK_INT >= 16) {
            IMPL = new TextViewCompatApi16Impl();
        } else {
            IMPL = new TextViewCompatBaseImpl();
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
     * {@link android.R.style#TextAppearance_Material_Body1 @android:style/TextAppearance.Material.Body1}.
     *
     * @param textView The TextView against which to invoke the method.
     * @param resId    The resource identifier of the style to apply.
     */
    public static void setTextAppearance(@NonNull TextView textView, @StyleRes int resId) {
        IMPL.setTextAppearance(textView, resId);
    }

    /**
     * Returns drawables for the start, top, end, and bottom borders from the given text view.
     */
    public static Drawable[] getCompoundDrawablesRelative(@NonNull TextView textView) {
        return IMPL.getCompoundDrawablesRelative(textView);
    }
}
