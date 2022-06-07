/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill.inline.common;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/**
 * Specifies the style for a {@link View} or a {@link android.view.ViewGroup}.
 */
@RequiresApi(api = Build.VERSION_CODES.R)
public class ViewStyle extends BundledStyle {

    private static final String KEY_VIEW_STYLE = "view_style";

    private static final String KEY_BACKGROUND = "background";
    private static final String KEY_BACKGROUND_COLOR = "background_color";
    private static final String KEY_PADDING = "padding";
    private static final String KEY_LAYOUT_MARGIN = "layout_margin";

    /**
     * This is made public so it can be used by the renderer to converted the received bundle to
     * a style. It does not validate the provided bundle. {@link #isValid()} or
     * {@link #assertIsValid()} can be used for validation.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ViewStyle(@NonNull Bundle bundle) {
        super(bundle);
    }

    /**
     * Applies the specified style on the {@code view}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @SuppressWarnings("deprecation")
    public void applyStyleOnViewIfValid(@NonNull View view) {
        if (!isValid()) {
            return;
        }
        if (mBundle.containsKey(KEY_BACKGROUND)) {
            Icon background = mBundle.getParcelable(KEY_BACKGROUND);
            if (background != null) {
                final Drawable drawable = background.loadDrawable(view.getContext());
                if (drawable != null) {
                    view.setBackground(drawable);
                }
            }
        }
        if (mBundle.containsKey(KEY_BACKGROUND_COLOR)) {
            int color = mBundle.getInt(KEY_BACKGROUND_COLOR);
            view.setBackgroundColor(color);
        }
        if (mBundle.containsKey(KEY_PADDING)) {
            int[] padding = mBundle.getIntArray(KEY_PADDING);
            if (padding != null && padding.length == 4) {
                if (view.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                    view.setPadding(padding[0], padding[1], padding[2], padding[3]);
                } else {
                    view.setPadding(padding[2], padding[1], padding[0], padding[3]);
                }
            }
        }
        if (mBundle.containsKey(KEY_LAYOUT_MARGIN)) {
            int[] layoutMargin = mBundle.getIntArray(KEY_LAYOUT_MARGIN);
            if (layoutMargin != null && layoutMargin.length == 4) {
                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                if (layoutParams == null) {
                    layoutParams = new ViewGroup.MarginLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
                } else if (!(layoutParams instanceof ViewGroup.MarginLayoutParams)) {
                    layoutParams = new ViewGroup.MarginLayoutParams(layoutParams);
                }
                ViewGroup.MarginLayoutParams marginLayoutParams =
                        (ViewGroup.MarginLayoutParams) layoutParams;
                if (view.getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
                    marginLayoutParams.setMargins(layoutMargin[0],
                            layoutMargin[1], layoutMargin[2], layoutMargin[3]);
                } else {
                    marginLayoutParams.setMargins(layoutMargin[2],
                            layoutMargin[1], layoutMargin[0], layoutMargin[3]);
                }
                view.setLayoutParams(marginLayoutParams);
            }
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @NonNull
    @Override
    protected String getStyleKey() {
        return KEY_VIEW_STYLE;
    }

    /**
     * An abstract builder class for any subclass of {@link ViewStyle}.
     *
     * @param <T> represents the type this builder can build.
     * @param <B> represents the subclass of {@link ViewStyle.BaseBuilder}.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public abstract static class BaseBuilder<T extends ViewStyle,
            B extends BaseBuilder<T, B>> extends BundledStyle.Builder<T> {

        protected BaseBuilder(@NonNull String style) {
            super(style);
        }

        /**
         * Returns {@code this} with the actual type of the subclass, so the setter methods can
         * be chained.
         */
        @NonNull
        protected abstract B getThis();

        /**
         * Sets the background.
         *
         * @param icon The icon to use as the background
         * @see android.graphics.drawable.Icon#loadDrawable(android.content.Context)
         * @see android.view.View#setBackground(android.graphics.drawable.Drawable)
         */
        @NonNull
        public B setBackground(@NonNull Icon icon) {
            Preconditions.checkNotNull(icon, "background icon should not be null");
            mBundle.putParcelable(KEY_BACKGROUND, icon);
            return getThis();
        }

        /**
         * Sets the background color, it will always override the {@link #setBackground(Icon)},
         * regardless of which set method is call first.
         *
         * @param color the color of the background
         * @see android.view.View#setBackgroundColor(int)
         */
        @NonNull
        public B setBackgroundColor(@ColorInt int color) {
            mBundle.putInt(KEY_BACKGROUND_COLOR, color);
            return getThis();
        }

        /**
         * Sets the padding.
         *
         * <p> Note that the method takes start/end rather than left/right, respecting the layout
         * direction.
         *
         * @param start   the start padding in pixels
         * @param top    the top padding in pixels
         * @param end  the end padding in pixels
         * @param bottom the bottom padding in pixels
         * @see android.view.View#setPadding(int, int, int, int)
         */
        @NonNull
        public B setPadding(int start, int top, int end, int bottom) {
            mBundle.putIntArray(KEY_PADDING, new int[]{start, top, end, bottom});
            return getThis();
        }

        /**
         * Sets the layout margin through the view's layout param.
         *
         * <p> Note that the method takes start/end rather than left/right, respecting the layout
         * direction.
         *
         * @param start   the start margin size
         * @param top    the top margin size
         * @param end  the end margin size
         * @param bottom the bottom margin size
         * @see android.view.ViewGroup.MarginLayoutParams#setMargins(int, int, int, int)
         * @see android.view.View#setLayoutParams(android.view.ViewGroup.LayoutParams)
         */
        @NonNull
        public B setLayoutMargin(int start, int top, int end,
                int bottom) {
            mBundle.putIntArray(KEY_LAYOUT_MARGIN, new int[]{start, top, end, bottom});
            return getThis();
        }
    }

    /**
     * Builder for the {@link ViewStyle}.
     */
    public static final class Builder extends BaseBuilder<ViewStyle, Builder> {

        public Builder() {
            super(KEY_VIEW_STYLE);
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @NonNull
        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        @NonNull
        public ViewStyle build() {
            return new ViewStyle(mBundle);
        }
    }
}
