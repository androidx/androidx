/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.core.locationbutton

import android.app.permissionui.LocationButtonSession
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import kotlin.math.ceil
import kotlin.math.min

/**
 * This button acts as a fallback on older platform where the System (remotely) rendered button is
 * unavailable. To ensure a seamless user experience, this class replicates Jetpack Compose's
 * Material 3 Expressive Button layout behaviors, including:
 * - Token-based dynamic scaling
 * - Flexbox-style Row centering for icon + text clusters
 * - Shape morphing on press
 * - Precise M3 typography tracking and weight adjustments
 */
internal class LocalLocationButton
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.buttonStyle,
) : AppCompatButton(createAppCompatThemeContext(context), attrs, defStyleAttr) {
    // Style attributes from the host wrapper.
    private var textType = -1
    private var iconTint = 0
    private var backgroundColor = 0
    private var textColor = 0
    private var cornerRadius = 0f
    private var strokeColor = 0
    private var strokeWidth = 0
    private var pressedCornerRadius = 0f

    private var backgroundDrawable: StateListDrawable? = null
    private var iconDrawable: Drawable? = null
    private var isConfigDirty = true

    // Internal cache of the last applied hardware metrics to prevent redundant layout requests
    private var textSizeSp = -1f
    private var leadingSpace = -1
    private var trailingSpace = -1
    private var verticalSpace = -1
    private var iconSize = -1

    fun configure(
        textType: Int,
        backgroundColor: Int,
        textColor: Int,
        iconTint: Int,
        cornerRadius: Float,
        strokeColor: Int,
        strokeWidth: Int,
        pressedCornerRadius: Float,
        maxLines: Int,
        textAllCaps: Boolean,
        includeFontPadding: Boolean,
    ) {
        val textChanged = this.textType != textType
        // Check if background or shape metrics have changed to trigger a drawable rebuild
        val visualChanged =
            (this.backgroundColor != backgroundColor ||
                this.cornerRadius != cornerRadius ||
                this.strokeColor != strokeColor ||
                this.strokeWidth != strokeWidth ||
                this.pressedCornerRadius != pressedCornerRadius ||
                this.iconTint != iconTint)

        if (visualChanged) {
            isConfigDirty = true
            this.iconTint = iconTint
            this.backgroundColor = backgroundColor
            this.cornerRadius = cornerRadius
            this.strokeColor = strokeColor

            val maxStrokeWidth = (MAX_STROKE_WIDTH_DP * resources.displayMetrics.density).toInt()
            this.strokeWidth = if (strokeWidth > maxStrokeWidth) maxStrokeWidth else strokeWidth

            this.pressedCornerRadius = pressedCornerRadius
        }

        if (maxLines != -1) {
            this.maxLines = maxLines
        }
        this.isAllCaps = textAllCaps
        this.includeFontPadding = includeFontPadding

        if (textChanged) {
            text = getTextForType(textType)
            this.textType = textType
        }

        if (this.textColor != textColor) {
            setTextColor(textColor)
            this.textColor = textColor
        }

        if (iconDrawable == null) {
            val d = ContextCompat.getDrawable(context, R.drawable.ic_location_button_my_location)
            iconDrawable = d?.let { DrawableCompat.wrap(it).mutate() }
        }
        if (visualChanged) {
            iconDrawable?.let { DrawableCompat.setTint(it, iconTint) }
            updateBackgroundIfDirty()
        }

        if (textChanged || !isLaidOut) {
            requestLayout()
        } else if (visualChanged) {
            invalidate()
        }
    }

    private fun updateBackgroundIfDirty() {
        if (!isConfigDirty && backgroundDrawable != null) return

        val defaultDrawable =
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                this.cornerRadius = this@LocalLocationButton.cornerRadius
                setColor(backgroundColor)
                if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
            }

        backgroundDrawable =
            StateListDrawable().apply {
                // Apply M3 Expressive pressed-state shape morphing if requested
                // TODO: Implement actual shape morphing.
                if (pressedCornerRadius > 0f && pressedCornerRadius != cornerRadius) {
                    val pressedDrawable =
                        GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            this.cornerRadius = pressedCornerRadius
                            setColor(backgroundColor)
                            if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
                        }
                    addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)
                }
                addState(intArrayOf(), defaultDrawable)
            }

        // Wrap the StateListDrawable in an InsetDrawable to prevent the stroke from being clipped
        // by the view bounds. Inset by half the stroke width.
        val insetPadding = strokeWidth / 2
        val finalDrawable =
            InsetDrawable(
                backgroundDrawable,
                insetPadding,
                insetPadding,
                insetPadding,
                insetPadding,
            )

        background = finalDrawable
        isConfigDirty = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val density = resources.displayMetrics.density
        val minHeight = (MIN_HEIGHT_DP * density).toInt()
        // Calculate width and height for M3 button tokens and styles (Small, Medium, Large etc)
        val referenceHeight =
            when (heightMode) {
                MeasureSpec.EXACTLY -> heightSize
                MeasureSpec.AT_MOST -> min(heightSize, minHeight)
                else -> minHeight
            }
        val referenceWidth =
            when (widthMode) {
                MeasureSpec.UNSPECIFIED -> Int.MAX_VALUE
                else -> widthSize
            }

        applyM3ExpressiveStyle(referenceWidth, referenceHeight, widthMode, density)

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    /**
     * Translates exact pixel constraints into M3 Expressive design tokens.
     *
     * This method precisely mimics the Jetpack Compose rendering logic used by the remote SystemUI
     * button. By achieving 1:1 visual parity, we ensure a seamless user experience with no
     * flickering when the local fallback is replaced by the remotely rendered button.
     *
     * Uses a Guarded Mutation pattern to apply these properties only if they differ from the
     * currently applied state, preventing redundant layout requests and infinite loops.
     */
    private fun applyM3ExpressiveStyle(
        referenceWidth: Int,
        referenceHeight: Int,
        widthMode: Int,
        density: Float,
    ) {
        val referenceWidthDp = referenceWidth / density
        val referenceHeightDp = referenceHeight / density
        // The smallest dimension dictates the button's scale tier
        val tokens = ButtonDefaults.tokensFor(min(referenceWidthDp, referenceHeightDp))

        val typography = tokens.typography
        if (typeface != typography.typeface) {
            typeface = typography.typeface
        }
        val requiredTextSizeSp = typography.fontSizeSp
        // only set text size if it has changed to avoid redundant requestLayout()
        if (textSizeSp != requiredTextSizeSp) {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, requiredTextSizeSp)
            textSizeSp = requiredTextSizeSp
        }
        if (letterSpacing != typography.letterSpacingEm) {
            letterSpacing = typography.letterSpacingEm
        }

        // Use ceil() to prevent sub-pixel rounding jitter that can cause text wrapping
        val textWidth =
            if (text.isEmpty()) 0 else ceil(paint.measureText(text, 0, text.length)).toInt()
        val targetIconSize = (tokens.iconSizeDp * density).toInt()
        // Only add spacing if there is actually text to separate from the icon
        val targetIconSpace = if (text.isEmpty()) 0 else (tokens.iconLabelSpaceDp * density).toInt()
        val contentWidth = targetIconSize + targetIconSpace + textWidth

        val defaultLeadingSpace = (tokens.leadingSpaceDp * density).toInt()
        val defaultTrailingSpace = (tokens.trailingSpaceDp * density).toInt()
        val contentBlockWidth = defaultLeadingSpace + contentWidth + defaultTrailingSpace

        val m3InternalPadding = (tokens.leadingSpaceDp * density).toInt()
        var finalLeadingSpace: Int
        var finalTrailingSpace: Int
        val requiredGravity: Int

        if (referenceWidthDp <= MIN_HEIGHT_DP) {
            finalLeadingSpace = ((referenceWidth - targetIconSize) / 2f).toInt()
            finalTrailingSpace = 0
            requiredGravity = Gravity.START or Gravity.CENTER_VERTICAL
        } else {
            val defaultLeadingSpace = m3InternalPadding + strokeWidth
            val defaultTrailingSpace = (tokens.trailingSpaceDp * density).toInt() + strokeWidth
            val contentBlockWidth = defaultLeadingSpace + contentWidth + defaultTrailingSpace

            if (widthMode == MeasureSpec.EXACTLY) {
                finalLeadingSpace =
                    maxOf(defaultLeadingSpace, ((referenceWidth - contentWidth) / 2f).toInt())
                finalTrailingSpace = 0
                requiredGravity = Gravity.START or Gravity.CENTER_VERTICAL
            } else {
                finalLeadingSpace = defaultLeadingSpace
                finalTrailingSpace = defaultTrailingSpace
                requiredGravity =
                    if (contentBlockWidth > referenceWidth) {
                        Gravity.START or Gravity.CENTER_VERTICAL
                    } else {
                        Gravity.CENTER
                    }
            }
        }

        if (gravity != requiredGravity) {
            gravity = requiredGravity
        }

        if (this.iconSize != targetIconSize) {
            iconDrawable?.let {
                it.setBounds(0, 0, targetIconSize, targetIconSize)
                setCompoundDrawablesRelative(null, null, null, null)
                setCompoundDrawablesRelative(it, null, null, null)
            }
            this.iconSize = targetIconSize
        }
        if (compoundDrawablePadding != targetIconSpace) {
            compoundDrawablePadding = targetIconSpace
        }

        val targetVerticalSpace = (tokens.verticalPaddingDp * density).toInt()
        if (
            this.leadingSpace != finalLeadingSpace ||
                this.trailingSpace != finalTrailingSpace ||
                this.verticalSpace != targetVerticalSpace
        ) {
            setPaddingRelative(
                finalLeadingSpace,
                targetVerticalSpace,
                finalTrailingSpace,
                targetVerticalSpace,
            )
            this.leadingSpace = finalLeadingSpace
            this.trailingSpace = finalTrailingSpace
            this.verticalSpace = targetVerticalSpace
        }

        // Lock the minimum physical height to the exact M3 Container specifications
        val requiredMinHeight = (tokens.containerHeightDp * density).toInt()
        if (minHeight != requiredMinHeight) {
            minHeight = requiredMinHeight
        }
    }

    private fun getTextForType(textType: Int): String {
        if (textType == LocationButtonSession.TEXT_TYPE_NONE) {
            return ""
        }
        return context.getString(
            when (textType) {
                LocationButtonSession.TEXT_TYPE_PRECISE_LOCATION ->
                    R.string.location_button_precise_location
                LocationButtonSession.TEXT_TYPE_USE_PRECISE_LOCATION ->
                    R.string.location_button_use_precise_location
                LocationButtonSession.TEXT_TYPE_SHARE_PRECISE_LOCATION ->
                    R.string.location_button_share_precise_location
                LocationButtonSession.TEXT_TYPE_NEAR_MY_PRECISE_LOCATION ->
                    R.string.location_button_near_my_precise_location
                LocationButtonSession.TEXT_TYPE_NEAR_YOUR_PRECISE_LOCATION ->
                    R.string.location_button_near_your_precise_location
                else -> R.string.location_button_precise_location
            }
        )
    }

    companion object {
        private fun createAppCompatThemeContext(context: Context): Context {
            return ContextThemeWrapper(context, androidx.appcompat.R.style.Theme_AppCompat)
        }

        private const val MIN_HEIGHT_DP = 48f
        private const val MAX_STROKE_WIDTH_DP = 3f
    }
}

// ============================================================================
// MATERIAL 3 EXPRESSIVE TOKENS
// ============================================================================
// These tokens perfectly map to frameworks/support/compose/material3/.../tokens/

/** Standardized M3 Typography scale parameters. */
private data class TypographyTokens(
    val fontSizeSp: Float,
    val lineHeightSp: Float,
    val letterSpacingEm: Float,
    val typeface: Typeface,
)

private object TypographyScale {
    val LabelLarge =
        TypographyTokens(
            fontSizeSp = 14f,
            lineHeightSp = 20f,
            letterSpacingEm = 0.1f / 14f,
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )
    val TitleMedium =
        TypographyTokens(
            fontSizeSp = 16f,
            lineHeightSp = 24f,
            letterSpacingEm = 0.15f / 16f,
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL),
        )
    val HeadlineSmall =
        TypographyTokens(
            fontSizeSp = 24f,
            lineHeightSp = 32f,
            letterSpacingEm = 0f,
            typeface =
                Typeface.create("sans-serif", Typeface.NORMAL), // Headline uses Regular (400) weight
        )
}

/** Encapsulates layout dimensions required to draw an M3 button. */
private data class ButtonTokens(
    val containerHeightDp: Float,
    val typography: TypographyTokens,
    val iconSizeDp: Float,
    val iconLabelSpaceDp: Float,
    val leadingSpaceDp: Float,
    val trailingSpaceDp: Float,
    // Android View Specific: Required to center content within standard layout bounds
    val verticalPaddingDp: Float,
)

/** Threshold mapping to determine the correct M3 Expressive layout tier. */
private object ButtonDefaults {

    private val ExtraSmall =
        ButtonTokens(
            containerHeightDp = 32f,
            typography = TypographyScale.LabelLarge,
            iconSizeDp = 20f,
            iconLabelSpaceDp = 8f,
            leadingSpaceDp = 16f,
            trailingSpaceDp = 16f,
            verticalPaddingDp = 6f,
        )

    private val Small =
        ButtonTokens(
            containerHeightDp = 40f,
            typography = TypographyScale.LabelLarge,
            iconSizeDp = 20f,
            iconLabelSpaceDp = 8f,
            leadingSpaceDp = 16f,
            trailingSpaceDp = 16f,
            verticalPaddingDp = 10f,
        )

    private val Medium =
        ButtonTokens(
            containerHeightDp = 56f,
            typography = TypographyScale.TitleMedium,
            iconSizeDp = 24f,
            iconLabelSpaceDp = 8f,
            leadingSpaceDp = 24f,
            trailingSpaceDp = 24f,
            verticalPaddingDp = 16f,
        )

    private val Large =
        ButtonTokens(
            containerHeightDp = 96f,
            typography = TypographyScale.HeadlineSmall,
            iconSizeDp = 32f,
            iconLabelSpaceDp = 12f,
            leadingSpaceDp = 48f,
            trailingSpaceDp = 48f,
            verticalPaddingDp = 32f,
        )

    private val ExtraLarge =
        ButtonTokens(
            containerHeightDp = 136f,
            typography = TypographyScale.HeadlineSmall,
            iconSizeDp = 40f,
            iconLabelSpaceDp = 16f,
            leadingSpaceDp = 64f,
            trailingSpaceDp = 64f,
            verticalPaddingDp = 48f,
        )

    /** Translates the available reference height into the appropriate sizing tier. */
    fun tokensFor(referenceHeightDp: Float): ButtonTokens {
        return when {
            referenceHeightDp < 40f -> ExtraSmall
            referenceHeightDp < 56f -> Small
            referenceHeightDp < 96f -> Medium
            referenceHeightDp < 136f -> Large
            else -> ExtraLarge
        }
    }
}
