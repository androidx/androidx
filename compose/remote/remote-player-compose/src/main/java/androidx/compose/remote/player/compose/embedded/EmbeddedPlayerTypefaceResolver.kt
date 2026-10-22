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

@file:Suppress("RestrictedApiAndroidX")

package androidx.compose.remote.player.compose.embedded

import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.player.compose.embedded.resources.applyVariationSettings
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat

/**
 * Request parameters for resolving a [Typeface] in the embedded player via [RcTypefaceResolver].
 *
 * Carries all font attributes needed upfront — including [variationSettings] — so downloadable font
 * resolvers (such as [GmsFontTypefaceResolver]) can pass `variationSettings` directly to the
 * 5-argument [FontRequest] constructor when requesting variable fonts.
 *
 * @property fontFamily The font family name (e.g., `"google:Roboto Flex"`, `"device:roboto-flex"`,
 *   `"sans-serif"`), or `null` when resolved by built-in [fontType].
 * @property fontType The built-in font type constant (e.g., [PaintBundle.FONT_TYPE_DEFAULT],
 *   [PaintBundle.FONT_TYPE_SANS_SERIF], [PaintBundle.FONT_TYPE_SERIF],
 *   [PaintBundle.FONT_TYPE_MONOSPACE]).
 * @property weight The font weight in `[1, 1000]` (e.g., `400` for normal, `700` for bold).
 * @property italic Whether the font style is italic.
 * @property variationSettings Optional font variation settings string in the format accepted by
 *   [Paint.setFontVariationSettings] and [FontRequest] (e.g., `"'wght' 500.0, 'wdth' 110.0"`), or
 *   `null` if no variation axes are specified.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RcTypefaceRequest(
    public val fontFamily: String? = null,
    public val fontType: Int = PaintBundle.FONT_TYPE_DEFAULT,
    public val weight: Int = 400,
    public val italic: Boolean = false,
    public val variationSettings: String? = null,
) {
    /** Returns a copy of this [RcTypefaceRequest] with selected properties overridden. */
    public fun copy(
        fontFamily: String? = this.fontFamily,
        fontType: Int = this.fontType,
        weight: Int = this.weight,
        italic: Boolean = this.italic,
        variationSettings: String? = this.variationSettings,
    ): RcTypefaceRequest =
        RcTypefaceRequest(
            fontFamily = fontFamily,
            fontType = fontType,
            weight = weight,
            italic = italic,
            variationSettings = variationSettings,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RcTypefaceRequest) return false
        return fontFamily == other.fontFamily &&
            fontType == other.fontType &&
            weight == other.weight &&
            italic == other.italic &&
            variationSettings == other.variationSettings
    }

    override fun hashCode(): Int {
        var result = fontFamily?.hashCode() ?: 0
        result = 31 * result + fontType
        result = 31 * result + weight
        result = 31 * result + italic.hashCode()
        result = 31 * result + (variationSettings?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "RcTypefaceRequest(fontFamily=$fontFamily, fontType=$fontType, weight=$weight, " +
            "italic=$italic, variationSettings=$variationSettings)"
}

/**
 * Pluggable suspending typeface resolver for the embedded [RcPlayer].
 *
 * Implementations receive an [RcTypefaceRequest] containing the full font specification (including
 * [RcTypefaceRequest.variationSettings]) and may suspend while fetching downloadable fonts (for
 * example via [FontsContractCompat.requestFont]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public fun interface RcTypefaceResolver {
    /**
     * Resolves a [Typeface] for the given [request], or returns `null` if the typeface cannot be
     * resolved.
     */
    public suspend fun resolve(request: RcTypefaceRequest): Typeface?
}

/**
 * Default [RcTypefaceResolver] for the embedded player that handles built-in system fonts and
 * custom prefixes like `"device:"`. For `"google:"` fonts without a GMS font resolver, it strips
 * the prefix and falls back to standard system font creation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object EmbeddedPlayerTypefaceResolver : RcTypefaceResolver {

    override suspend fun resolve(request: RcTypefaceRequest): Typeface = resolveSync(request)

    internal fun resolveSync(request: RcTypefaceRequest): Typeface {
        val fontName = request.fontFamily
        val baseTypeface =
            if (
                fontName != null &&
                    request.fontType !in
                        PaintBundle.FONT_TYPE_DEFAULT..PaintBundle.FONT_TYPE_MONOSPACE &&
                    fontName != "default" &&
                    fontName != "sans-serif" &&
                    fontName != "serif" &&
                    fontName != "monospace"
            ) {
                val actualName =
                    when {
                        fontName.startsWith("device:") -> fontName.substring("device:".length)
                        fontName.startsWith("google:") -> fontName.substring("google:".length)
                        else -> fontName
                    }
                Typeface.create(actualName, Typeface.NORMAL)
            } else {
                when {
                    request.fontType == PaintBundle.FONT_TYPE_SANS_SERIF ||
                        fontName == "sans-serif" -> Typeface.SANS_SERIF
                    request.fontType == PaintBundle.FONT_TYPE_SERIF || fontName == "serif" ->
                        Typeface.SERIF
                    request.fontType == PaintBundle.FONT_TYPE_MONOSPACE ||
                        fontName == "monospace" -> Typeface.MONOSPACE
                    else -> Typeface.DEFAULT
                }
            }
        val weighted = Typeface.create(baseTypeface, request.weight, request.italic)
        return applyVariationSettings(weighted, request.variationSettings)
    }
}
