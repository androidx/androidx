/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.glance.appwidget.components

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * A Title Bar. Contains an Icon, Text, and actions. Intended to be placed at the top of a widget.
 *
 * @param startIcon A tintable icon representing your app or brand.
 * @param title Text to be displayed. Generally the name of your widget or app. Title
 * should be shortened or omitted when the widget's width is narrow. The width can be checked
 * using `LocalSize.current.width`
 * @param iconColor The color which [startIcon] will be tinted.
 * @param textColor The color which [title] will be tinted.
 * @param modifier GlanceModifier.
 * @param fontFamily Optional override for [title]'s font family. Leave null to use the default.
 * @param actions A slot api for buttons. Use [CircleIconButton] with backgroundColor = null.
 * Buttons will be placed in a [Row].
 */
@SuppressLint("ComposableLambdaParameterNaming") // lint thinks `actions` should be called `content`
@Composable
fun TitleBar(
    startIcon: ImageProvider,
    title: String,
    iconColor: ColorProvider? = GlanceTheme.colors.onSurface,
    textColor: ColorProvider = GlanceTheme.colors.onSurface,
    modifier: GlanceModifier = GlanceModifier,
    fontFamily: FontFamily? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    @Composable
    fun StartIcon() {
        Box(
            GlanceModifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                modifier = GlanceModifier.size(24.dp),
                provider = startIcon,
                contentDescription = "",
                colorFilter = iconColor?.let { ColorFilter.tint(iconColor) }
            )
        }
    }

    @Composable
    fun RowScope.Title() {
        Text(
            text = title,
            style = TextStyle(
                color = textColor,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                fontFamily = fontFamily
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
    }

    Row(
        modifier.fillMaxWidth().padding(4.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        StartIcon()
        Title()
        actions()
    }
}
