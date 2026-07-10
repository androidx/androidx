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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.creation.Painter
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.compose.capture.LocalRemoteDensity
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloatExpression
import androidx.compose.remote.creation.compose.state.RemoteStateInstanceKey
import androidx.compose.remote.creation.compose.state.RemoteString
import androidx.compose.remote.creation.compose.state.RemoteTextUnit
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.text.RemoteTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

private val RemoteComposeWriter.painter: Painter
    get() {
        if (this !is RemoteComposeWriterAndroid) {
            throw Exception("Invalid Writer $this, painter inaccessible")
        }

        return this.painter
    }

@Composable
@Suppress("UnrememberedMutableState")
public fun measureTextWidth(
    text: RemoteString,
    style: RemoteTextStyle = RemoteTextStyle.Default,
    fontSize: RemoteTextUnit? = null,
): RemoteFloat {
    val resolvedFontSize = fontSize ?: style.fontSize ?: 12.rsp
    val textSize = resolvedFontSize.toPx(LocalRemoteDensity.current)

    return RemoteFloatExpression(
        constantValueOrNull = null,
        // May depend on composition locals so avoid caching
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
            .commit() // For text width measuring

        floatArrayOf(
            doc.textAttribute(
                text.getIdForCreationState(creationState),
                TextAttribute.MEASURE_WIDTH,
            )
        )
    }
}

@Composable
@Suppress("UnrememberedMutableState")
public fun measureTextHeight(
    text: RemoteString,
    style: RemoteTextStyle = RemoteTextStyle.Default,
    fontSize: RemoteTextUnit? = null,
): RemoteFloat {
    val resolvedFontSize = fontSize ?: style.fontSize ?: 12.rsp
    val textSize = resolvedFontSize.toPx(LocalRemoteDensity.current)

    return RemoteFloatExpression(
        constantValueOrNull = null,
        // May depend on composition locals so avoid caching
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
            .commit() // For text width measuring

        floatArrayOf(
            doc.textAttribute(
                text.getIdForCreationState(creationState),
                TextAttribute.MEASURE_HEIGHT,
            )
        )
    }
}
