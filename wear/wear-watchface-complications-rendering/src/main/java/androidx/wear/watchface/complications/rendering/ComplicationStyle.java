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

package androidx.wear.watchface.complications.rendering;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.wearable.complications.ComplicationData;

import androidx.annotation.ColorInt;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Defines attributes to customize appearance of rendered {@link
 * android.support.wearable.complications.ComplicationData}.
 */
public class ComplicationStyle {

    /**
     * Constants used to define border styles for complications.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BORDER_STYLE_NONE, BORDER_STYLE_SOLID, BORDER_STYLE_DASHED})
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @interface BorderStyle {
    }

    /** Style where the borders are not drawn. */
    public static final int BORDER_STYLE_NONE = 0;

    /** Style where the borders are drawn without any gap. */
    public static final int BORDER_STYLE_SOLID = 1;

    /**
     * Style where the borders are drawn as dashed lines. If this is set as current border style,
     * dash width and dash gap should also be set via {@link #setBorderDashWidth(int)},
     * {@link #setBorderDashGap(int)}  or XML attributes, or default values will be used.
     */
    public static final int BORDER_STYLE_DASHED = 2;

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
    @Px
    private static final int TEXT_SIZE_DEFAULT = Integer.MAX_VALUE;

    /** Default typeface. */
    private static final Typeface TYPEFACE_DEFAULT =
            Typeface.create("sans-serif-condensed", Typeface.NORMAL);

    /** Default dash width. */
    @Px
    private static final int DASH_WIDTH_DEFAULT = 3;

    /** Default dash gap. */
    @Px
    private static final int DASH_GAP_DEFAULT = 3;

    /** Default border width. */
    @Px
    private static final int BORDER_WIDTH_DEFAULT = 1;

    /** Default ring width. */
    @Px
    private static final int RING_WIDTH_DEFAULT = 2;

    /** Default border radius. */
    @Px
    public static final int BORDER_RADIUS_DEFAULT = Integer.MAX_VALUE;

    @ColorInt
    private int mBackgroundColor = BACKGROUND_COLOR_DEFAULT;
    private Drawable mBackgroundDrawable = null;
    @ColorInt
    private int mTextColor = PRIMARY_COLOR_DEFAULT;
    @ColorInt
    private int mTitleColor = SECONDARY_COLOR_DEFAULT;
    private Typeface mTextTypeface = TYPEFACE_DEFAULT;
    private Typeface mTitleTypeface = TYPEFACE_DEFAULT;
    @Px
    private int mTextSize = TEXT_SIZE_DEFAULT;
    @Px
    private int mTitleSize = TEXT_SIZE_DEFAULT;
    private ColorFilter mImageColorFilter = null;
    @ColorInt
    private int mIconColor = PRIMARY_COLOR_DEFAULT;
    @ColorInt
    private int mBorderColor = BORDER_COLOR_DEFAULT;
    @BorderStyle
    private int mBorderStyle = BORDER_STYLE_SOLID;
    @Px
    private int mBorderDashWidth = DASH_WIDTH_DEFAULT;
    @Px
    private int mBorderDashGap = DASH_GAP_DEFAULT;
    @Px
    private int mBorderRadius = BORDER_RADIUS_DEFAULT;
    @Px
    private int mBorderWidth = BORDER_WIDTH_DEFAULT;
    @Px
    private int mRangedValueRingWidth = RING_WIDTH_DEFAULT;
    @ColorInt
    private int mRangedValuePrimaryColor = PRIMARY_COLOR_DEFAULT;
    @ColorInt
    private int mRangedValueSecondaryColor = SECONDARY_COLOR_DEFAULT;
    @ColorInt
    private int mHighlightColor = HIGHLIGHT_COLOR_DEFAULT;
    private boolean mDirty = true;

    public ComplicationStyle() {
    }

    public ComplicationStyle(@NonNull ComplicationStyle style) {
        mBackgroundColor = style.getBackgroundColor();
        mBackgroundDrawable = style.getBackgroundDrawable();
        mTextColor = style.getTextColor();
        mTitleColor = style.getTitleColor();
        mTextTypeface = style.getTextTypeface();
        mTitleTypeface = style.getTitleTypeface();
        mTextSize = style.getTextSize();
        mTitleSize = style.getTitleSize();
        mImageColorFilter = style.getImageColorFilter();
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

    boolean isDirty() {
        return mDirty;
    }

    void clearDirtyFlag() {
        mDirty = false;
    }

    /** Returns the background color to be used. */
    @ColorInt
    public int getBackgroundColor() {
        return mBackgroundColor;
    }

    /** Returns the background drawable to be used, or null if there's no background drawable. */
    @Nullable
    public Drawable getBackgroundDrawable() {
        return mBackgroundDrawable;
    }

    /** Returns the text color. Text color should be used for short and long text. */
    @ColorInt
    public int getTextColor() {
        return mTextColor;
    }

    /** Returns the title color. Title color should be used for short and long title. */
    @ColorInt
    public int getTitleColor() {
        return mTitleColor;
    }

    /**
     * Returns the color filter to be used when rendering small and large images, or null if there's
     * no color filter.
     */
    @Nullable
    public ColorFilter getImageColorFilter() {
        return mImageColorFilter;
    }

    /** Returns the color for tinting icons. */
    @ColorInt
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
    @Px
    public int getTextSize() {
        return mTextSize;
    }

    /** Returns the text size to be used for short and long title. */
    @Px
    public int getTitleSize() {
        return mTitleSize;
    }

    /** Returns the border color. */
    @ColorInt
    public int getBorderColor() {
        return mBorderColor;
    }

    @BorderStyle
    public int getBorderStyle() {
        return mBorderStyle;
    }

    /**
     * Returns the dash width to be used when drawing borders of type {@link #BORDER_STYLE_DASHED}.
     */
    @Px
    public int getBorderDashWidth() {
        return mBorderDashWidth;
    }

    /**
     * Returns the dash gap to be used when drawing borders of type {@link #BORDER_STYLE_DASHED}.
     */
    @Px
    public int getBorderDashGap() {
        return mBorderDashGap;
    }

    /**
     * Returns the border radius. If {@link ComplicationStyle#BORDER_RADIUS_DEFAULT} is returned,
     * border radius should be reduced to half of the minimum of width or height during the
     * rendering.
     */
    @Px
    public int getBorderRadius() {
        return mBorderRadius;
    }

    /** Returns the border width. */
    @Px
    public int getBorderWidth() {
        return mBorderWidth;
    }

    /** Returns the ring width to be used when rendering ranged value indicator. */
    @Px
    public int getRangedValueRingWidth() {
        return mRangedValueRingWidth;
    }

    /** Returns the color to be used when rendering first part of ranged value indicator. */
    @ColorInt
    public int getRangedValuePrimaryColor() {
        return mRangedValuePrimaryColor;
    }

    /** Returns the color to be used when rendering second part of ranged value indicator. */
    @ColorInt
    public int getRangedValueSecondaryColor() {
        return mRangedValueSecondaryColor;
    }

    /** Returns the highlight color to be used when the complication is highlighted. */
    @ColorInt
    public int getHighlightColor() {
        return mHighlightColor;
    }

    /**
     * Sets the background color.
     *
     * @param backgroundColor The color to set
     */
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        this.mBackgroundColor = backgroundColor;
    }

    /**
     * Sets the {@link Drawable} to render in the background.
     *
     * @param backgroundDrawable The {@link Drawable} to render in the background
     */
    public void setBackgroundDrawable(@Nullable Drawable backgroundDrawable) {
        this.mBackgroundDrawable = backgroundDrawable;
    }

    /**
     * Sets the color to render the text with. Text color is used for rendering short text
     * and long text fields.
     *
     * @param textColor The color to render the text with
     */
    public void setTextColor(@ColorInt int textColor) {
        this.mTextColor = textColor;
    }

    /**
     * Sets the color to render the title with.  Title color is used for rendering short
     * title and long title fields.
     *
     * @param titleColor The color to render the title with
     */
    public void setTitleColor(@ColorInt int titleColor) {
        this.mTitleColor = titleColor;
    }

    /**
     * Sets the color filter used in active mode when rendering large images and small images
     * with style {@link ComplicationData#IMAGE_STYLE_PHOTO}.
     *
     * @param colorFilter The {@link ColorFilter} to use
     */
    public void setImageColorFilter(@Nullable ColorFilter colorFilter) {
        mImageColorFilter = colorFilter;
        mDirty = true;
    }

    /**
     * Sets the color for tinting the icon with.
     *
     * @param iconColor The color to render the icon with
     */
    public void setIconColor(@ColorInt int iconColor) {
        mIconColor = iconColor;
        mDirty = true;
    }

    /**
     * Sets {@link Typeface} to use when rendering short text and long text fields.
     *
     * @param textTypeface The {@link Typeface} to render the text with
     */
    public void setTextTypeface(@NonNull Typeface textTypeface) {
        mTextTypeface = textTypeface;
        mDirty = true;
    }

    /**
     * Sets the {@link Typeface} to render the title for short and long text with.
     *
     * @param titleTypeface The {@link Typeface} to render the title with
     */
    public void setTitleTypeface(@NonNull Typeface titleTypeface) {
        mTitleTypeface = titleTypeface;
        mDirty = true;
    }

    /**
     * Sets the size of the text to use when rendering short text and long text fields.
     *
     * @param textSize The size of the text=
     */
    public void setTextSize(@Px int textSize) {
        mTextSize = textSize;
        mDirty = true;
    }

    /**
     * Sets the size of the title text to use when rendering short text and long text fields.
     *
     * @param titleSize The size of the title text=
     */
    public void setTitleSize(@Px int titleSize) {
        mTitleSize = titleSize;
        mDirty = true;
    }

    /**
     * Sets the color to render the complication border with.
     *
     * @param borderColor The color to render the complication border with
     */
    public void setBorderColor(@ColorInt int borderColor) {
        mBorderColor = borderColor;
        mDirty = true;
    }

    /**
     * Sets the style to render the complication border with.
     *
     * @param borderStyle The style to render the complication border with
     */
    public void setBorderStyle(@BorderStyle int borderStyle) {
        switch (borderStyle) {
            case BORDER_STYLE_SOLID:
                mBorderStyle = BORDER_STYLE_SOLID;
                break;
            case BORDER_STYLE_DASHED:
                mBorderStyle = BORDER_STYLE_DASHED;
                break;
            default:
                mBorderStyle = BORDER_STYLE_NONE;
        }
        mDirty = true;
    }

    /**
     * Sets dash widths to render the complication border with when drawing borders with style
     * {@link #BORDER_STYLE_DASHED}.
     *
     * @param borderDashWidth The dash widths to render the complication border with
     */
    public void setBorderDashWidth(@Px int borderDashWidth) {
        mBorderDashWidth = borderDashWidth;
        mDirty = true;
    }

    /**
     * Sets the dash gap render the complication border with when drawing borders with style
     * {@link #BORDER_STYLE_DASHED}.
     *
     * @param borderDashGap The dash gap render the complication border with
     */
    public void setBorderDashGap(@Px int borderDashGap) {
        mBorderDashGap = borderDashGap;
        mDirty = true;
    }

    /**
     * Sets the border radius to be applied to the corners of the bounds of the complication in
     * active mode. Border radius will be limited to the half of width or height, depending
     * on which one is smaller.
     *
     * @param borderRadius The radius to render the complication border with
     */
    public void setBorderRadius(@Px int borderRadius) {
        mBorderRadius = borderRadius;
        mDirty = true;
    }

    /**
     * Sets the width to render the complication border with.
     *
     * @param borderWidth The width to render the complication border with
     */
    public void setBorderWidth(@Px int borderWidth) {
        mBorderWidth = borderWidth;
        mDirty = true;
    }

    /**
     * Sets the stroke width used when rendering the ranged value indicator.
     *
     * @param rangedValueRingWidth The width to render the ranged value ring with
     */
    public void setRangedValueRingWidth(@Px int rangedValueRingWidth) {
        mRangedValueRingWidth = rangedValueRingWidth;
        mDirty = true;
    }

    /**
     * Sets the main color to render the ranged value text with.
     *
     * @param rangedValuePrimaryColor The main color to render the ranged value text with
     */
    public void setRangedValuePrimaryColor(@ColorInt int rangedValuePrimaryColor) {
        mRangedValuePrimaryColor = rangedValuePrimaryColor;
        mDirty = true;
    }

    /**
     * Sets the secondary color to render the ranged value text with.
     *
     * @param rangedValueSecondaryColor The secondary color to render the ranged value text with
     */
    public void setRangedValueSecondaryColor(@ColorInt int rangedValueSecondaryColor) {
        mRangedValueSecondaryColor = rangedValueSecondaryColor;
        mDirty = true;
    }

    /**
     * Sets the background color to use when the complication is highlighted.
     *
     * @param highlightColor The background color to use when the complication is highlighted
     */
    public void setHighlightColor(@ColorInt int highlightColor) {
        mHighlightColor = highlightColor;
        mDirty = true;
    }
}
