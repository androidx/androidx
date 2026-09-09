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

package androidx.compose.remote.player.compose.embedded.demos.embedded

import androidx.compose.foundation.text.BasicText
import androidx.compose.remote.player.compose.embedded.CustomComposablePlugin
import androidx.compose.remote.player.compose.embedded.FloatProperty
import androidx.compose.remote.player.compose.embedded.IntProperty
import androidx.compose.remote.player.compose.embedded.RcCustomComponent
import androidx.compose.remote.player.compose.embedded.StringProperty
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Custom component plugin for rendering a custom AnnotatedString / SpannableString with pure
 * Compose [BasicText] and [LinkAnnotation.Url].
 */
public object SupportSpannableStringPlugin : CustomComposablePlugin<AnnotatedString> {
    override val name: String = CONFIG

    public const val CONFIG: String = "SupportSpannableString"
    public const val PROP_TEXT: Short = 1
    public const val PROP_TEXT_COLOR: Short = 2
    public const val PROP_TEXT_SIZE: Short = 3
    public const val PROP_LINK_COUNT: Short = 10
    public const val PROP_LINK_URL_BASE: Short = 1000
    public const val PROP_LINK_START_BASE: Short = 2000
    public const val PROP_LINK_END_BASE: Short = 3000

    @Composable
    override fun extract(component: RcCustomComponent): AnnotatedString? {
        if (!component.config.equals(name, ignoreCase = true)) return null

        val text = component.textState(StringProperty(PROP_TEXT)).value
        val linkCount = component.intState(IntProperty(PROP_LINK_COUNT)).value

        val rawTextColor = component.intState(IntProperty(PROP_TEXT_COLOR)).value
        val rawTextSize = component.floatState(FloatProperty(PROP_TEXT_SIZE)).value
        val color = if (rawTextColor != 0) Color(rawTextColor) else Color.Unspecified
        val size = if (rawTextSize > 0f) rawTextSize.sp else TextUnit.Unspecified

        return buildAnnotatedString {
            append(text)
            if (color != Color.Unspecified || size != TextUnit.Unspecified) {
                addStyle(SpanStyle(color = color, fontSize = size), 0, text.length)
            }
            for (i in 0 until linkCount) {
                val url =
                    component.textState(StringProperty((PROP_LINK_URL_BASE + i).toShort())).value
                val start =
                    component.intState(IntProperty((PROP_LINK_START_BASE + i).toShort())).value
                val end = component.intState(IntProperty((PROP_LINK_END_BASE + i).toShort())).value

                val s = start.coerceIn(0, text.length)
                val e = end.coerceIn(0, text.length)
                if (s < e && url.isNotEmpty()) {
                    addLink(url = LinkAnnotation.Url(url = url), start = s, end = e)
                }
            }
        }
    }

    @Composable
    override fun Content(data: AnnotatedString, component: RcCustomComponent, modifier: Modifier) {
        BasicText(text = data, modifier = modifier)
    }
}
