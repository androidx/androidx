/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.creation.Painter
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.LocalRemoteDensity
import androidx.compose.remote.creation.compose.capture.RemoteDensity
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteStateInstanceKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

private val RemoteComposeWriter.painter: Painter
    get() {
        if (this !is RemoteComposeWriterAndroid) {
            throw Exception("Invalid Writer $this, painter inaccessible")
        }

        return this.painter
    }

private val DefaultFontSize: RemoteTextUnit = 12.rsp

/**
 * Creates and remembers a [RemoteTextMeasurer].
 *
 * Parameters are read from CompositionLocals (such as [LocalRemoteDensity]).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
public fun rememberRemoteTextMeasurer(): RemoteTextMeasurer {
    val density = LocalRemoteDensity.current
    return remember(density) {
        RemoteTextMeasurer(density = density)
    }
}

/**
 * Measures remote text dimensions (width, height, size).
 *
 * Follows the [TextMeasurer] pattern from Compose UI.
 *
 * @param density density of the measurement environment, used for scaling fonts.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteTextMeasurer(internal val density: RemoteDensity) {
    /**
     * Measures the dimensions of [text] and returns a [RemoteTextLayoutResult].
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
    @Stable
    public fun measure(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteTextLayoutResult {
        val width = measureWidth(text, style, fontSize)
        val height = measureHeight(text, style, fontSize)
        return RemoteTextLayoutResult(RemoteSize(width, height))
    }

    /**
     * Measures the width of [text].
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
    internal fun measureWidth(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat {
        val resolvedFontSize = fontSize ?: style.fontSize ?: DefaultFontSize
        val textSize = resolvedFontSize.toPx(density)

        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteStateInstanceKey(),
        ) { creationState ->
            val doc = creationState.document
            val textSizePxId = textSize.getFloatIdForCreationState(creationState)
            doc.painter
                .setTextSize(textSizePxId)
                .setTypeface(
                    0,
                    (style.fontWeight ?: FontWeight.Normal).weight,
                    style.fontStyle == FontStyle.Italic,
                )
                .commit()

            floatArrayOf(
                doc.textAttribute(
                    text.getIdForCreationState(creationState),
                    TextAttribute.MEASURE_WIDTH,
                )
            )
        }
    }

    /**
     * Measures the height of [text].
     *
     * If [fontSize] is not specified (null), the font size is taken from [RemoteTextStyle.fontSize]
     * on [style], or defaults to 12.rsp if not specified in [style].
     *
     * @param text the remote string to be measured.
     * @param style the [RemoteTextStyle] to apply. Defaults to [RemoteTextStyle.Default].
     * @param fontSize optional font size override. If null, the font size is taken from [style], or
     *   defaults to 12.rsp.
     */
    internal fun measureHeight(
        text: RemoteString,
        style: RemoteTextStyle = RemoteTextStyle.Default,
        fontSize: RemoteTextUnit? = null,
    ): RemoteFloat {
        val resolvedFontSize = fontSize ?: style.fontSize ?: DefaultFontSize
        val textSize = resolvedFontSize.toPx(density)

        return RemoteFloatExpression(
            constantValueOrNull = null,
            cacheKey = RemoteStateInstanceKey(),
        ) { creationState ->
            val doc = creationState.document
            val textSizePxId = textSize.getFloatIdForCreationState(creationState)
            doc.painter
                .setTextSize(textSizePxId)
                .setTypeface(
                    0,
                    (style.fontWeight ?: FontWeight.Normal).weight,
                    style.fontStyle == FontStyle.Italic,
                )
                .commit()

            floatArrayOf(
                doc.textAttribute(
                    text.getIdForCreationState(creationState),
                    TextAttribute.MEASURE_HEIGHT,
                )
            )
        }
    }
}

/**
 * Holds the result of a remote text layout measurement.
 *
 * @param size the dimensions ([RemoteSize]) of the measured text.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteTextLayoutResult(public val size: RemoteSize) {
    public constructor(width: RemoteFloat, height: RemoteFloat) : this(RemoteSize(width, height))

    /** The measured width as a [RemoteFloat]. */
    public val width: RemoteFloat
        get() = size.width

    /** The measured height as a [RemoteFloat]. */
    public val height: RemoteFloat
        get() = size.height

    override fun toString(): String = "RemoteTextLayoutResult(size=$size)"
}
