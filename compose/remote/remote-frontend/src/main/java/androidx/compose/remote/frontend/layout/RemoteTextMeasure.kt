/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.compose.remote.frontend.layout

import androidx.annotation.RestrictTo
import androidx.compose.material.LocalTextStyle
import androidx.compose.remote.core.operations.TextAttribute
import androidx.compose.remote.creation.Painter
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.frontend.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.remote.frontend.state.RemoteString
import androidx.compose.remote.frontend.state.rememberRemoteFloat
import androidx.compose.remote.frontend.state.rf
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.takeOrElse

private val RemoteComposeWriter.painter: Painter
    get() {
        if (this !is RemoteComposeWriterAndroid) {
            throw Exception("Invalid Writer $this, painter inaccessible")
        }

        return this.painter
    }

@Composable
public fun measureTextWidth(
    text: RemoteString,
    style: TextStyle = LocalTextStyle.current,
    fontSize: TextUnit = TextUnit.Unspecified,
): RemoteFloat {
    val textSize = with(LocalDensity.current) { fontSize.takeOrElse { style.fontSize }.toPx() }
    val creationState = LocalRemoteComposeCreationState.current
    val doc = creationState.document
    doc.painter
        .setTextSize(textSize)
        .setTypeface(
            0,
            (style.fontWeight ?: FontWeight.Normal).weight,
            style.fontStyle == FontStyle.Italic,
        )
        .commit() // For text width measuring

    return rememberRemoteFloat {
        doc.textAttribute(text.getIdForCreationState(creationState), TextAttribute.MEASURE_WIDTH).rf
    }
}

@Composable
public fun measureTextHeight(
    text: RemoteString,
    style: TextStyle = LocalTextStyle.current,
    fontSize: TextUnit = TextUnit.Unspecified,
): RemoteFloat {
    val textSize = with(LocalDensity.current) { fontSize.takeOrElse { style.fontSize }.toPx() }
    val creationState = LocalRemoteComposeCreationState.current
    val doc = creationState.document
    doc.painter
        .setTextSize(textSize)
        .setTypeface(
            0,
            (style.fontWeight ?: FontWeight.Normal).weight,
            style.fontStyle == FontStyle.Italic,
        )
        .commit() // For text width measuring

    return rememberRemoteFloat {
        doc.textAttribute(text.getIdForCreationState(creationState), TextAttribute.MEASURE_HEIGHT)
            .rf
    }
}

@Composable
public fun measureTextLength(text: RemoteString): RemoteFloat {
    val creationState = LocalRemoteComposeCreationState.current
    val doc = creationState.document
    return rememberRemoteFloat {
        doc.textAttribute(text.getIdForCreationState(creationState), TextAttribute.TEXT_LENGTH).rf
    }
}
