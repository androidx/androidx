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

package androidx.compose.remote.player.compose.textinput

import androidx.annotation.RestrictTo
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.remote.core.TextInputSession
import androidx.compose.remote.core.TextInputSupport
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp

/**
 * [TextInputSupport] for the canvas-based Compose player: while a session is active, [Editor]
 * overlays a text field on the player at the bounds of the edited component, styled from the
 * component's text attributes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ComposeTextInputSupport : TextInputSupport {
    private var session by mutableStateOf<TextInputSession?>(null)
    // Snapshot copy of the session bounds, as the session mutates its array in place.
    private var bounds by mutableStateOf(floatArrayOf(0f, 0f, 0f, 0f))

    /** Returns true while an editor is shown. */
    public fun isEditing(): Boolean = session?.isActive == true

    override fun startTextInput(session: TextInputSession) {
        this.session?.end()
        this.session = session
        bounds = session.bounds.copyOf()
    }

    override fun updateTextInputBounds(session: TextInputSession) {
        if (this.session === session) {
            bounds = session.bounds.copyOf()
        }
    }

    override fun stopTextInput(session: TextInputSession) {
        if (this.session === session) {
            this.session = null
        }
    }

    /** Ends the active session, if any. */
    public fun stopTextInput() {
        session?.end()
        session = null
    }

    /**
     * Called by the player on touch down; ends the session when the touch lands outside the editor.
     */
    public fun onTouchDown(x: Float, y: Float) {
        if (!isEditing()) return
        val b = bounds
        val inside = x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3]
        if (!inside) stopTextInput()
    }

    /** Composes the editor over the player content; place it after the player's canvas. */
    @Composable
    public fun Editor() {
        val session = session ?: return
        if (!session.isActive) return
        val density = LocalDensity.current
        // The editor appears in response to a tap on the document, not on the field itself, so it
        // has to take focus explicitly (which also shows the keyboard). One session at a time.
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(session) { focusRequester.requestFocus() }
        val b = bounds
        val padding =
            session.textComponent?.let {
                with(density) {
                    androidx.compose.foundation.layout.PaddingValues(
                        start = it.paddingLeft.toDp(),
                        top = it.paddingTop.toDp(),
                        end = it.paddingRight.toDp(),
                        bottom = it.paddingBottom.toDp(),
                    )
                }
            }
        val style = textStyle(session, density)
        RcTextInputField(
            text = session.text,
            op = session.layout,
            textStyle = style,
            hint = session.hint,
            onValueChange = { session.setText(it) },
            onDone = {
                if (this.session === session) {
                    this.session = null
                }
                session.end()
            },
            modifier =
                Modifier.focusRequester(focusRequester)
                    .offset { IntOffset(b[0].toInt(), b[1].toInt()) }
                    .size(with(density) { b[2].toDp() }, with(density) { b[3].toDp() })
                    .then(if (padding != null) Modifier.padding(padding) else Modifier),
        )
    }

    private fun textStyle(
        session: TextInputSession,
        density: androidx.compose.ui.unit.Density,
    ): TextStyle {
        val text = session.textComponent ?: return TextStyle.Default
        val fontSizePx = text.fontSizeValue
        val color = text.colorValue
        val weight = text.fontWeightValue
        val align = text.textAlignValue
        return TextStyle(
            color = Color(color),
            fontSize = if (fontSizePx > 0f) with(density) { fontSizePx.toSp() } else 16.sp,
            fontWeight = FontWeight(weight.toInt().coerceIn(1, 1000)),
            fontStyle = if (text.fontStyle == 1) FontStyle.Italic else FontStyle.Normal,
            textAlign =
                when (align) {
                    CoreText.TEXT_ALIGN_CENTER -> TextAlign.Center
                    CoreText.TEXT_ALIGN_RIGHT,
                    CoreText.TEXT_ALIGN_END -> TextAlign.End
                    else -> TextAlign.Start
                },
        )
    }
}
