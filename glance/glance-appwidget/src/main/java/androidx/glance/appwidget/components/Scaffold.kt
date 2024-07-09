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

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider

/**
 * A simple slot api component for displaying widget UI with a [TitleBar]. Sets the background color
 * to `GlanceTheme.colors.surface` and applies padding. This is intended to be used as a top level
 * component.
 *
 * @param modifier a modifier
 * @param titleBar A composable that creates the [TitleBar]. Optional parameter.
 * @param backgroundColor the background color for the layout.
 * @param horizontalPadding Scaffold provides a default padding which should work for most use
 *   cases. However, it can be overridden.
 * @param content The main content of the widget.
 */
@Composable
fun Scaffold(
    modifier: GlanceModifier = GlanceModifier,
    titleBar: @Composable (() -> Unit)? = null,
    backgroundColor: ColorProvider = GlanceTheme.colors.widgetBackground,
    horizontalPadding: Dp = 12.dp,
    content: @Composable () -> Unit,
) {

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(backgroundColor)
                .appWidgetBackground()
                .widgetCornerRadius()
    ) {
        titleBar?.invoke()
        Box(
            modifier = GlanceModifier.padding(horizontal = horizontalPadding).defaultWeight(),
            content = content
        )
    }
}

private fun GlanceModifier.widgetCornerRadius(): GlanceModifier {
    val cornerRadiusModifier =
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            GlanceModifier.cornerRadius(android.R.dimen.system_app_widget_background_radius)
        } else {
            GlanceModifier
        }

    return this.then(cornerRadiusModifier)
}
