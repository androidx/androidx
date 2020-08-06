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

package androidx.wear.complications.rendering;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

/**
 * Defines attributes to customize appearance of rendered {@link
 * android.support.wearable.complications.ComplicationData}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressLint("BanParcelableUsage")
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class ComplicationStyle {

    /** Default primary color. */
    private static final int PRIMARY_COLOR_DEFAULT = Color.WHITE;

    /** Default secondary color. */
    private static final int SECONDARY_COLOR_DEFAULT = Color.LTGRAY;

    /** Default background color. */
    private static final int BACKGROUND_COLOR_DEFAULT = Color.BLACK;

    /** Default background color. */
    private static final int HIGHLIGHT_COLOR_DEFAULT = Color.LTGRAY;

    /** Default border color. */
    private static final int BORDER_COLOR_DEFAULT = Color.WHITE;

    /** Default text size. */
    private static final int TEXT_SIZE_DEFAULT = Integer.MAX_VALUE;

    /** Default typeface. */
    private static final Typeface TYPEFACE_DEFAULT =
            Typeface.create("sans-serif-condensed", Typeface.NORMAL);

    /** Default dash width. */
    private static final int DASH_WIDTH_DEFAULT = 3;

    /** Default dash gap. */
    private static final int DASH_GAP_DEFAULT = 3;

    /** Default border width. */
    private static final int BORDER_WIDTH_DEFAULT = 1;

    /** Default ring width. */
    private static final int RING_WIDTH_DEFAULT = 2;

    /** Default border radius. */
    public static final int BORDER_RADIUS_DEFAULT = Integer.MAX_VALUE;

    private final int mBackgroundColor;
    private final Drawable mBackgroundDrawable;
    private final int mTextColor;
    private final int mTitleColor;
    private final Typeface mTextTypeface;
    private final Typeface mTitleTypeface;
    private final int mTextSize;
    private final int mTitleSize;
    private final ColorFilter mColorFilter;
    private final int mIconColor;
    private final int mBorderColor;

    @ComplicationDrawable.BorderStyle
    private final int mBorderStyle;
    private final int mBorderDashWidth;
    private final int mBorderDashGap;
    private final int mBorderRadius;
    private final int mBorderWidth;
    private final int mRangedValueRingWidth;
    private final int mRangedValuePrimaryColor;
    private final int mRangedValueSecondaryColor;
    private final int mHighlightColor;

    private ComplicationStyle(
            int backgroundColor,
            Drawable backgroundDrawable,
            int textColor,
            int titleColor,
            Typeface textTypeface,
            Typeface titleTypeface,
            int textSize,
            int titleSize,
            ColorFilter colorFilter,
            int iconColor,
            int borderColor,
            @ComplicationDrawable.BorderStyle int borderStyle,
            int borderRadius,
            int borderWidth,
            int dashWidth,
            int dashGap,
            int ringWidth,
            int rangedPrimaryColor,
            int rangedSecondaryColor,
            int highlightColor) {

        mBackgroundColor = backgroundColor;
        mBackgroundDrawable = backgroundDrawable;
        mTextColor = textColor;
        mTitleColor = titleColor;
        mTextTypeface = textTypeface;
        mTitleTypeface = titleTypeface;
        mTextSize = textSize;
        mTitleSize = titleSize;
        mColorFilter = colorFilter;
        mIconColor = iconColor;
        mBorderColor = borderColor;
        mBorderStyle = borderStyle;
        mBorderDashWidth = dashWidth;
        mBorderDashGap = dashGap;
        mBorderRadius = borderRadius;
        mBorderWidth = borderWidth;
        mRangedValueRingWidth = ringWidth;
        mRangedValuePrimaryColor = rangedPrimaryColor;
        mRangedValueSecondaryColor = rangedSecondaryColor;
        mHighlightColor = highlightColor;
    }

    /** Returns the background color to be used. */
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    /** Returns the background drawable to be used, or null if there's no background drawable. */
    @Nullable
    public Drawable getBackgroundDrawable() {
        return mBackgroundDrawable;
    }

    /** Returns the text color. Text color should be used for short and long text. */
    public int getTextColor() {
        return mTextColor;
    }

    /** Returns the title color. Title color should be used for short and long title. */
    public int getTitleColor() {
        return mTitleColor;
    }

    /**
     * Returns the color filter to be used when rendering small and large images, or null if there's
     * no color filter.
     */
    @Nullable
    public ColorFilter getColorFilter() {
        return mColorFilter;
    }

    /** Returns the color for tinting icons. */
    public int getIconColor() {
        return mIconColor;
    }

    /** Returns the typeface to be used for short and long text. */
    @Nullable
    public Typeface getTextTypeface() {
        return mTextTypeface;
    }

    /** Returns the typeface to be used for short and long title. */
    @Nullable
    public Typeface getTitleTypeface() {
        return mTitleTypeface;
    }

    /** Returns the text size to be used for short and long text. */
    public int getTextSize() {
        return mTextSize;
    }

    /** Returns the text size to be used for short and long title. */
    public int getTitleSize() {
        return mTitleSize;
    }

    /** Returns the border color. */
    public int getBorderColor() {
        return mBorderColor;
    }

    @ComplicationDrawable.BorderStyle
    public int getBorderStyle() {
        return mBorderStyle;
    }

    /**
     * Returns the dash width to be used when drawing borders of type {@link
     * ComplicationDrawable#BORDER_STYLE_DASHED}.
     */
    public int getBorderDashWidth() {
        return mBorderDashWidth;
    }

    /**
     * Returns the dash gap to be used when drawing borders of type {@link
     * ComplicationDrawable#BORDER_STYLE_DASHED}.
     */
    public int getBorderDashGap() {
        return mBorderDashGap;
    }

    /**
     * Returns the border radius. If {@link ComplicationStyle#BORDER_RADIUS_DEFAULT} is returned,
     * border radius should be reduced to half of the minimum of width or height during the
     * rendering.
     */
    public int getBorderRadius() {
        return mBorderRadius;
    }

    /** Returns the border width. */
    public int getBorderWidth() {
        return mBorderWidth;
    }

    /** Returns the ring width to be used when rendering ranged value indicator. */
    public int getRangedValueRingWidth() {
        return mRangedValueRingWidth;
    }

    /** Returns the color to be used when rendering first part of ranged value indicator. */
    public int getRangedValuePrimaryColor() {
        return mRangedValuePrimaryColor;
    }

    /** Returns the color to be used when rendering second part of ranged value indicator. */
    public int getRangedValueSecondaryColor() {
        return mRangedValueSecondaryColor;
    }

    /** Returns the highlight color to be used when the complication is highlighted. */
    public int getHighlightColor() {
        return mHighlightColor;
    }

    /** Used to build an instance of this class. */
    public static class Builder implements Parcelable {

        private static final String FIELD_BACKGROUND_COLOR = "background_color";
        private static final String FIELD_TEXT_COLOR = "text_color";
        private static final String FIELD_TITLE_COLOR = "title_color";
        private static final String FIELD_TEXT_STYLE = "text_style";
        private static final String FIELD_TITLE_STYLE = "title_style";
        private static final String FIELD_TEXT_SIZE = "text_size";
        private static final String FIELD_TITLE_SIZE = "title_size";
        private static final String FIELD_ICON_COLOR = "icon_color";
        private static final String FIELD_BORDER_COLOR = "border_color";
        private static final String FIELD_BORDER_STYLE = "border_style";
        private static final String FIELD_BORDER_DASH_WIDTH = "border_dash_width";
        private static final String FIELD_BORDER_DASH_GAP = "border_dash_gap";
        private static final String FIELD_BORDER_RADIUS = "border_radius";
        private static final String FIELD_BORDER_WIDTH = "border_width";
        private static final String FIELD_RANGED_VALUE_RING_WIDTH = "ranged_value_ring_width";
        private static final String FIELD_RANGED_VALUE_PRIMARY_COLOR = "ranged_value_primary_color";
        private static final String FIELD_RANGED_VALUE_SECONDARY_COLOR =
                "ranged_value_secondary_color";
        private static final String FIELD_HIGHLIGHT_COLOR = "highlight_color";

        public static final Creator<Builder> CREATOR =
                new Creator<Builder>() {
                    @Override
                    @SuppressLint("SyntheticAccessor")
                    public Builder createFromParcel(Parcel source) {
                        return new Builder(source);
                    }

                    @Override
                    public Builder[] newArray(int size) {
                        return new Builder[size];
                    }
                };

        private int mBackgroundColor = BACKGROUND_COLOR_DEFAULT;
        private Drawable mBackgroundDrawable = null;
        private int mTextColor = PRIMARY_COLOR_DEFAULT;
        private int mTitleColor = SECONDARY_COLOR_DEFAULT;

        @SuppressLint("SyntheticAccessor")
        private Typeface mTextTypeface = TYPEFACE_DEFAULT;

        @SuppressLint("SyntheticAccessor")
        private Typeface mTitleTypeface = TYPEFACE_DEFAULT;
        private int mTextSize = TEXT_SIZE_DEFAULT;
        private int mTitleSize = TEXT_SIZE_DEFAULT;
        private ColorFilter mColorFilter = null;
        private int mIconColor = PRIMARY_COLOR_DEFAULT;
        private int mBorderColor = BORDER_COLOR_DEFAULT;
        private int mBorderStyle = ComplicationDrawable.BORDER_STYLE_SOLID;
        private int mBorderDashWidth = DASH_WIDTH_DEFAULT;
        private int mBorderDashGap = DASH_GAP_DEFAULT;
        private int mBorderRadius = BORDER_RADIUS_DEFAULT;
        private int mBorderWidth = BORDER_WIDTH_DEFAULT;
        private int mRangedValueRingWidth = RING_WIDTH_DEFAULT;
        private int mRangedValuePrimaryColor = PRIMARY_COLOR_DEFAULT;
        private int mRangedValueSecondaryColor = SECONDARY_COLOR_DEFAULT;
        private int mHighlightColor = HIGHLIGHT_COLOR_DEFAULT;

        Builder() {
        }

        Builder(@NonNull Builder builder) {
            mBackgroundColor = builder.mBackgroundColor;
            mBackgroundDrawable = builder.mBackgroundDrawable;
            mTextColor = builder.mTextColor;
            mTitleColor = builder.mTitleColor;
            mTextTypeface = builder.mTextTypeface;
            mTitleTypeface = builder.mTitleTypeface;
            mTextSize = builder.mTextSize;
            mTitleSize = builder.mTitleSize;
            mColorFilter = builder.mColorFilter;
            mIconColor = builder.mIconColor;
            mBorderColor = builder.mBorderColor;
            mBorderStyle = builder.mBorderStyle;
            mBorderDashWidth = builder.mBorderDashWidth;
            mBorderDashGap = builder.mBorderDashGap;
            mBorderRadius = builder.mBorderRadius;
            mBorderWidth = builder.mBorderWidth;
            mRangedValueRingWidth = builder.mRangedValueRingWidth;
            mRangedValuePrimaryColor = builder.mRangedValuePrimaryColor;
            mRangedValueSecondaryColor = builder.mRangedValueSecondaryColor;
            mHighlightColor = builder.mHighlightColor;
        }

        Builder(@NonNull ComplicationStyle style) {
            mBackgroundColor = style.getBackgroundColor();
            mBackgroundDrawable = style.getBackgroundDrawable();
            mTextColor = style.getTextColor();
            mTitleColor = style.getTitleColor();
            mTextTypeface = style.getTextTypeface();
            mTitleTypeface = style.getTitleTypeface();
            mTextSize = style.getTextSize();
            mTitleSize = style.getTitleSize();
            mColorFilter = style.getColorFilter();
            mIconColor = style.getIconColor();
            mBorderColor = style.getBorderColor();
            mBorderStyle = style.getBorderStyle();
            mBorderDashWidth = style.getBorderDashWidth();
            mBorderDashGap = style.getBorderDashGap();
            mBorderRadius = style.getBorderRadius();
            mBorderWidth = style.getBorderWidth();
            mRangedValueRingWidth = style.getRangedValueRingWidth();
            mRangedValuePrimaryColor = style.getRangedValuePrimaryColor();
            mRangedValueSecondaryColor = style.getRangedValueSecondaryColor();
            mHighlightColor = style.getHighlightColor();
        }

        private Builder(@NonNull Parcel in) {
            Bundle bundle = in.readBundle(getClass().getClassLoader());

            mBackgroundColor = bundle.getInt(FIELD_BACKGROUND_COLOR);
            mTextColor = bundle.getInt(FIELD_TEXT_COLOR);
            mTitleColor = bundle.getInt(FIELD_TITLE_COLOR);

            // TODO(b/69249429): Find a way to support non-default typeface.
            mTextTypeface =
                    Typeface.defaultFromStyle(bundle.getInt(FIELD_TEXT_STYLE, Typeface.NORMAL));
            mTitleTypeface =
                    Typeface.defaultFromStyle(bundle.getInt(FIELD_TITLE_STYLE, Typeface.NORMAL));

            mTextSize = bundle.getInt(FIELD_TEXT_SIZE);
            mTitleSize = bundle.getInt(FIELD_TITLE_SIZE);
            mIconColor = bundle.getInt(FIELD_ICON_COLOR);
            mBorderColor = bundle.getInt(FIELD_BORDER_COLOR);
            mBorderStyle = bundle.getInt(FIELD_BORDER_STYLE);
            mBorderDashWidth = bundle.getInt(FIELD_BORDER_DASH_WIDTH);
            mBorderDashGap = bundle.getInt(FIELD_BORDER_DASH_GAP);
            mBorderRadius = bundle.getInt(FIELD_BORDER_RADIUS);
            mBorderWidth = bundle.getInt(FIELD_BORDER_WIDTH);
            mRangedValueRingWidth = bundle.getInt(FIELD_RANGED_VALUE_RING_WIDTH);
            mRangedValuePrimaryColor = bundle.getInt(FIELD_RANGED_VALUE_PRIMARY_COLOR);
            mRangedValueSecondaryColor = bundle.getInt(FIELD_RANGED_VALUE_SECONDARY_COLOR);
            mHighlightColor = bundle.getInt(FIELD_HIGHLIGHT_COLOR);
        }

        @Override
        public void writeToParcel(@NonNull Parcel dest, int flags) {
            Bundle bundle = new Bundle();
            bundle.putInt(FIELD_BACKGROUND_COLOR, mBackgroundColor);
            bundle.putInt(FIELD_TEXT_COLOR, mTextColor);
            bundle.putInt(FIELD_TITLE_COLOR, mTitleColor);
            bundle.putInt(FIELD_TEXT_STYLE, mTextTypeface.getStyle());
            bundle.putInt(FIELD_TITLE_STYLE, mTitleTypeface.getStyle());
            bundle.putInt(FIELD_TEXT_SIZE, mTextSize);
            bundle.putInt(FIELD_TITLE_SIZE, mTitleSize);
            bundle.putInt(FIELD_ICON_COLOR, mIconColor);
            bundle.putInt(FIELD_BORDER_COLOR, mBorderColor);
            bundle.putInt(FIELD_BORDER_STYLE, mBorderStyle);
            bundle.putInt(FIELD_BORDER_DASH_WIDTH, mBorderDashWidth);
            bundle.putInt(FIELD_BORDER_DASH_GAP, mBorderDashGap);
            bundle.putInt(FIELD_BORDER_RADIUS, mBorderRadius);
            bundle.putInt(FIELD_BORDER_WIDTH, mBorderWidth);
            bundle.putInt(FIELD_RANGED_VALUE_RING_WIDTH, mRangedValueRingWidth);
            bundle.putInt(FIELD_RANGED_VALUE_PRIMARY_COLOR, mRangedValuePrimaryColor);
            bundle.putInt(FIELD_RANGED_VALUE_SECONDARY_COLOR, mRangedValueSecondaryColor);
            bundle.putInt(FIELD_HIGHLIGHT_COLOR, mHighlightColor);
            dest.writeBundle(bundle);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        /**
         * @param backgroundColor The color to set
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setBackgroundColor(int backgroundColor) {
            this.mBackgroundColor = backgroundColor;
            return this;
        }

        /**
         * @param backgroundDrawable The {@link Drawable} to render in the background
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setBackgroundDrawable(@Nullable Drawable backgroundDrawable) {
            this.mBackgroundDrawable = backgroundDrawable;
            return this;
        }

        /**
         * @param textColor The color to render the text with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setTextColor(int textColor) {
            this.mTextColor = textColor;
            return this;
        }

        /**
         * @param titleColor The color to render the title with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setTitleColor(int titleColor) {
            this.mTitleColor = titleColor;
            return this;
        }

        /**
         * @param colorFilter The {@link ColorFilter} to use
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setColorFilter(@Nullable ColorFilter colorFilter) {
            this.mColorFilter = colorFilter;
            return this;
        }

        /**
         * @param iconColor The color to render the icon with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setIconColor(int iconColor) {
            this.mIconColor = iconColor;
            return this;
        }

        /**
         * @param textTypeface The {@link Typeface} to render the text with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setTextTypeface(@NonNull Typeface textTypeface) {
            this.mTextTypeface = textTypeface;
            return this;
        }

        /**
         * @param titleTypeface The {@link Typeface} to render the title with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setTitleTypeface(@NonNull Typeface titleTypeface) {
            this.mTitleTypeface = titleTypeface;
            return this;
        }

        /**
         * @param textSize The size of the text
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setTextSize(int textSize) {
            this.mTextSize = textSize;
            return this;
        }

        /**
         * @param titleSize The size of the title text
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setTitleSize(int titleSize) {
            this.mTitleSize = titleSize;
            return this;
        }

        /**
         * @param borderColor The color to render the complication border with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setBorderColor(int borderColor) {
            this.mBorderColor = borderColor;
            return this;
        }

        /**
         * @param borderStyle The style to render the complication border with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setBorderStyle(@ComplicationDrawable.BorderStyle int borderStyle) {
            switch (borderStyle) {
                case ComplicationDrawable.BORDER_STYLE_SOLID:
                    this.mBorderStyle = ComplicationDrawable.BORDER_STYLE_SOLID;
                    break;
                case ComplicationDrawable.BORDER_STYLE_DASHED:
                    this.mBorderStyle = ComplicationDrawable.BORDER_STYLE_DASHED;
                    break;
                default:
                    this.mBorderStyle = ComplicationDrawable.BORDER_STYLE_NONE;
            }
            return this;
        }

        /**
         * @param borderDashWidth The dash widths to render the complication border with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setBorderDashWidth(int borderDashWidth) {
            this.mBorderDashWidth = borderDashWidth;
            return this;
        }

        /**
         * @param borderDashGap The dash gap render the complication border with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setBorderDashGap(int borderDashGap) {
            this.mBorderDashGap = borderDashGap;
            return this;
        }

        /**
         * @param borderRadius The radius to render the complication border with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setBorderRadius(int borderRadius) {
            this.mBorderRadius = borderRadius;
            return this;
        }

        /**
         * @param borderWidth The width to render the complication border with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setBorderWidth(int borderWidth) {
            this.mBorderWidth = borderWidth;
            return this;
        }

        /**
         * @param rangedValueRingWidth The width to render the ranged value ring with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setRangedValueRingWidth(int rangedValueRingWidth) {
            this.mRangedValueRingWidth = rangedValueRingWidth;
            return this;
        }

        /**
         * @param rangedValuePrimaryColor The main color to render the ranged value text with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setRangedValuePrimaryColor(int rangedValuePrimaryColor) {
            this.mRangedValuePrimaryColor = rangedValuePrimaryColor;
            return this;
        }

        /**
         * @param rangedValueSecondaryColor The secondary color to render the ranged value text with
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setRangedValueSecondaryColor(int rangedValueSecondaryColor) {
            this.mRangedValueSecondaryColor = rangedValueSecondaryColor;
            return this;
        }

        /**
         * @param highlightColor The background color to use when the complication is highlighted
         * @return the {@link Builder}.
         */
        @NonNull
        public Builder setHighlightColor(int highlightColor) {
            this.mHighlightColor = highlightColor;
            return this;
        }

        /**
         * @return A {@link ComplicationStyle} constructed from the parameters passed to the builder
         */
        @NonNull
        @SuppressWarnings("SyntheticAccessor")
        public ComplicationStyle build() {
            return new ComplicationStyle(
                    mBackgroundColor,
                    mBackgroundDrawable,
                    mTextColor,
                    mTitleColor,
                    mTextTypeface,
                    mTitleTypeface,
                    mTextSize,
                    mTitleSize,
                    mColorFilter,
                    mIconColor,
                    mBorderColor,
                    mBorderStyle,
                    mBorderRadius,
                    mBorderWidth,
                    mBorderDashWidth,
                    mBorderDashGap,
                    mRangedValueRingWidth,
                    mRangedValuePrimaryColor,
                    mRangedValueSecondaryColor,
                    mHighlightColor);
        }
    }
}
