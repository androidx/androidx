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
package androidx.wear.watchface.complications.rendering

import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.Px
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.data.SmallImageType

/** Defines attributes to customize appearance of rendered [ ]. */
public class ComplicationStyle {
    /** Constants used to define border styles for complicationSlots. */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(BORDER_STYLE_NONE, BORDER_STYLE_SOLID, BORDER_STYLE_DASHED)
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public annotation class BorderStyle

    /** The background color to be used. */
    @ColorInt
    public var backgroundColor: Int = BACKGROUND_COLOR_DEFAULT
        @ColorInt get() = field
        set(@ColorInt backgroundColor: Int) {
            field = backgroundColor
        }

    /** The background drawable to be used, or null if there's no background drawable. */
    public var backgroundDrawable: Drawable? = null

    /**
     * The color to render the text with. Text color is used for rendering short text and long text
     * fields.
     */
    @ColorInt
    public var textColor: Int = PRIMARY_COLOR_DEFAULT
        @ColorInt get() = field
        set(@ColorInt textColor: Int) {
            field = textColor
        }

    /**
     * The color to render the title with. Title color is used for rendering short title and long
     * title fields.
     */
    @ColorInt
    public var titleColor: Int = SECONDARY_COLOR_DEFAULT
        @ColorInt get() = field
        set(@ColorInt titleColor: Int) {
            field = titleColor
        }

    /** The typeface to be used for short and long text. */
    public var textTypeface: Typeface = TYPEFACE_DEFAULT
        private set

    /** The typeface to be used for short and long title. */
    public var titleTypeface: Typeface = TYPEFACE_DEFAULT
        private set

    @Px private var mTextSize = TEXT_SIZE_DEFAULT

    @Px private var mTitleSize = TEXT_SIZE_DEFAULT

    private var mImageColorFilter: ColorFilter? = null

    @ColorInt private var mIconColor = PRIMARY_COLOR_DEFAULT

    @ColorInt private var mBorderColor = BORDER_COLOR_DEFAULT

    @BorderStyle private var mBorderStyle = BORDER_STYLE_SOLID

    @Px private var mBorderDashWidth = DASH_WIDTH_DEFAULT

    @Px private var mBorderDashGap = DASH_GAP_DEFAULT

    @Px private var mBorderRadius = BORDER_RADIUS_DEFAULT

    @Px private var mBorderWidth = BORDER_WIDTH_DEFAULT

    @Px private var mRangedValueRingWidth = RING_WIDTH_DEFAULT

    @ColorInt private var mRangedValuePrimaryColor = PRIMARY_COLOR_DEFAULT

    @ColorInt private var mRangedValueSecondaryColor = SECONDARY_COLOR_DEFAULT

    @ColorInt private var mHighlightColor = HIGHLIGHT_COLOR_DEFAULT

    @get:JvmName(name = "isDirty")
    internal var isDirty: Boolean = true
        private set

    public constructor()

    public constructor(style: ComplicationStyle) {
        backgroundColor = style.backgroundColor
        backgroundDrawable = style.backgroundDrawable
        textColor = style.textColor
        titleColor = style.titleColor
        textTypeface = style.textTypeface
        titleTypeface = style.titleTypeface
        mTextSize = style.textSize
        mTitleSize = style.titleSize
        mImageColorFilter = style.imageColorFilter
        mIconColor = style.iconColor
        mBorderColor = style.borderColor
        mBorderStyle = style.borderStyle
        mBorderDashWidth = style.borderDashWidth
        mBorderDashGap = style.borderDashGap
        mBorderRadius = style.borderRadius
        mBorderWidth = style.borderWidth
        mRangedValueRingWidth = style.rangedValueRingWidth
        mRangedValuePrimaryColor = style.rangedValuePrimaryColor
        mRangedValueSecondaryColor = style.rangedValueSecondaryColor
        mHighlightColor = style.highlightColor
    }

    @JvmName(name = "clearDirtyFlag")
    internal fun clearDirtyFlag() {
        isDirty = false
    }

    /**
     * The color filter used in active mode when rendering large images and small images with style
     * [SmallImageType.PHOTO].
     */
    public var imageColorFilter: ColorFilter?
        get() = mImageColorFilter
        set(colorFilter) {
            mImageColorFilter = colorFilter
            isDirty = true
        }

    /** The color for tinting icons. */
    public var iconColor: Int
        @ColorInt get() = mIconColor
        set(@ColorInt iconColor) {
            mIconColor = iconColor
            isDirty = true
        }

    /** Returns the text size to be used for short and long text fields. */
    public var textSize: Int
        @Px get() = mTextSize
        set(@Px textSize) {
            mTextSize = textSize
            isDirty = true
        }

    /** The text size to be used for short and long title fields. */
    public var titleSize: Int
        @Px get() = mTitleSize
        set(@Px titleSize) {
            mTitleSize = titleSize
            isDirty = true
        }

    /** The color to render the complication border with. */
    public var borderColor: Int
        @ColorInt get() = mBorderColor
        set(@ColorInt borderColor) {
            mBorderColor = borderColor
            isDirty = true
        }

    /** The style to render the complication border with. */
    public var borderStyle: Int
        @BorderStyle get() = mBorderStyle
        set(@BorderStyle borderStyle) {
            mBorderStyle =
                when (borderStyle) {
                    BORDER_STYLE_SOLID -> BORDER_STYLE_SOLID
                    BORDER_STYLE_DASHED -> BORDER_STYLE_DASHED
                    else -> BORDER_STYLE_NONE
                }
            isDirty = true
        }

    /** The dash width to be used when drawing borders of type [.BORDER_STYLE_DASHED]. */
    public var borderDashWidth: Int
        @Px get() = mBorderDashWidth
        set(@Px borderDashWidth) {
            mBorderDashWidth = borderDashWidth
            isDirty = true
        }

    /** The dash gap to be used when drawing borders of type [.BORDER_STYLE_DASHED]. */
    public var borderDashGap: Int
        @Px get() = mBorderDashGap
        set(@Px borderDashGap) {
            mBorderDashGap = borderDashGap
            isDirty = true
        }

    /**
     * The border radius to be applied to the corners of the bounds of the complication in active
     * mode. Border radius will be limited to the half of width or height, depending on which one is
     * smaller. If [ComplicationStyle.BORDER_RADIUS_DEFAULT] is returned, border radius should be
     * reduced to half of the minimum of width or height during the rendering.
     */
    public var borderRadius: Int
        @Px get() = mBorderRadius
        set(@Px borderRadius) {
            mBorderRadius = borderRadius
            isDirty = true
        }

    /** The width to render the complication border with. */
    public var borderWidth: Int
        @Px get() = mBorderWidth
        set(@Px borderWidth) {
            mBorderWidth = borderWidth
            isDirty = true
        }

    /** The ring width to be used when rendering ranged value indicator. */
    public var rangedValueRingWidth: Int
        @Px get() = mRangedValueRingWidth
        set(@Px rangedValueRingWidth) {
            mRangedValueRingWidth = rangedValueRingWidth
            isDirty = true
        }

    /** The color to be used when rendering first part of ranged value indicator. */
    public var rangedValuePrimaryColor: Int
        @ColorInt get() = mRangedValuePrimaryColor
        set(@ColorInt rangedValuePrimaryColor) {
            mRangedValuePrimaryColor = rangedValuePrimaryColor
            isDirty = true
        }

    /** The color to be used when rendering second part of ranged value indicator. */
    public var rangedValueSecondaryColor: Int
        @ColorInt get() = mRangedValueSecondaryColor
        set(@ColorInt rangedValueSecondaryColor) {
            mRangedValueSecondaryColor = rangedValueSecondaryColor
            isDirty = true
        }

    /** The highlight color to be used when the complication is highlighted. */
    public var highlightColor: Int
        @ColorInt get() = mHighlightColor
        set(@ColorInt highlightColor) {
            mHighlightColor = highlightColor
            isDirty = true
        }

    /**
     * Sets [Typeface] to use when rendering short text and long text fields.
     *
     * @param textTypeface The [Typeface] to render the text with
     */
    public fun setTextTypeface(textTypeface: Typeface) {
        this.textTypeface = textTypeface
        isDirty = true
    }

    /**
     * Sets the [Typeface] to render the title for short and long text with.
     *
     * @param titleTypeface The [Typeface] to render the title with
     */
    public fun setTitleTypeface(titleTypeface: Typeface) {
        this.titleTypeface = titleTypeface
        isDirty = true
    }

    /** Returns a copy of the ComplicationStyle [tint]ed by [tintColor]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun asTinted(tintColor: Int): ComplicationStyle =
        ComplicationStyle(this).apply {
            backgroundColor = tint(backgroundColor, tintColor)
            borderColor = tint(borderColor, tintColor)
            highlightColor = tint(highlightColor, tintColor)
            iconColor = tint(iconColor, tintColor)
            rangedValuePrimaryColor = tint(rangedValuePrimaryColor, tintColor)
            rangedValueSecondaryColor = tint(rangedValueSecondaryColor, tintColor)
            textColor = tint(textColor, tintColor)
            titleColor = tint(titleColor, tintColor)
        }

    public companion object {
        /** Style where the borders are not drawn. */
        public const val BORDER_STYLE_NONE: Int = 0

        /** Style where the borders are drawn without any gap. */
        public const val BORDER_STYLE_SOLID: Int = 1

        /**
         * Style where the borders are drawn as dashed lines. If this is set as current border
         * style, dash width and dash gap should also be set via [.setBorderDashWidth],
         * [.setBorderDashGap] or XML attributes, or default values will be used.
         */
        public const val BORDER_STYLE_DASHED: Int = 2

        /** Default primary color. */
        private const val PRIMARY_COLOR_DEFAULT = Color.WHITE

        /** Default secondary color. */
        private const val SECONDARY_COLOR_DEFAULT = Color.LTGRAY

        /** Default background color. */
        private const val BACKGROUND_COLOR_DEFAULT = Color.BLACK

        /** Default background color. */
        private const val HIGHLIGHT_COLOR_DEFAULT = Color.LTGRAY

        /** Default border color. */
        private const val BORDER_COLOR_DEFAULT = Color.WHITE

        /** Default text size. */
        @Px private const val TEXT_SIZE_DEFAULT = Int.MAX_VALUE

        /** Default typeface. */
        private val TYPEFACE_DEFAULT = Typeface.create("sans-serif-condensed", Typeface.NORMAL)

        /** Default dash width. */
        @Px private const val DASH_WIDTH_DEFAULT = 3

        /** Default dash gap. */
        @Px private const val DASH_GAP_DEFAULT = 3

        /** Default border width. */
        @Px private const val BORDER_WIDTH_DEFAULT = 1

        /** Default ring width. */
        @Px private const val RING_WIDTH_DEFAULT = 2

        /** Default border radius. */
        @Px public const val BORDER_RADIUS_DEFAULT: Int = Int.MAX_VALUE

        /** Computes the luminance of [color] and applies that to [tint]. */
        internal fun tint(color: Int, tint: Int): Int {
            // See https://en.wikipedia.org/wiki/Relative_luminance
            val luminance =
                (Color.red(color).toFloat() * (0.2126f / 255.0f)) +
                    (Color.green(color).toFloat() * (0.7152f / 255.0f)) +
                    (Color.blue(color).toFloat() * (0.0722f / 255.0f))

            return Color.argb(
                Color.alpha(color).toFloat() / 255.0f,
                Color.red(tint) * luminance / 255.0f,
                Color.green(tint) * luminance / 255.0f,
                Color.blue(tint) * luminance / 255.0f
            )
        }
    }
}
