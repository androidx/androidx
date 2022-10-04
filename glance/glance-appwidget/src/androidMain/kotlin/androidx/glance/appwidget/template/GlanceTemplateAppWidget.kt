/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.template

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.color.dynamicThemeColorProviders
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.template.LocalTemplateColors
import androidx.glance.template.LocalTemplateMode
import androidx.glance.template.TemplateMode

/**
 * A [GlanceAppWidget] that provides template local values.
 */
abstract class GlanceTemplateAppWidget : GlanceAppWidget() {

    companion object {
        private val COLLAPSED = DpSize(30.dp, 30.dp)
        private val HORIZONTAL_S = DpSize(241.dp, 30.dp)
        private val HORIZONTAL_M = DpSize(241.dp, 200.dp)
        private val HORIZONTAL_L = DpSize(350.dp, 200.dp)
        private val VERTICAL_S = DpSize(30.dp, 241.dp)
        private val VERTICAL_M = DpSize(200.dp, 241.dp)
        private val VERTICAL_L = DpSize(200.dp, 350.dp)
    }

    /** Default widget size mode is [SizeMode.Responsive] */
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            COLLAPSED, VERTICAL_S, VERTICAL_M, VERTICAL_L, HORIZONTAL_S, HORIZONTAL_M, HORIZONTAL_L
        )
    )

    /** Default widget state definition is [PreferencesGlanceStateDefinition] */
    override val stateDefinition: GlanceStateDefinition<*>? = PreferencesGlanceStateDefinition

    @Composable
    final override fun Content() {
        // TODO: Add other local values
        val mode = mode()
        val colors = dynamicThemeColorProviders()
        CompositionLocalProvider(
            LocalTemplateMode provides mode,
            LocalTemplateColors provides colors
        ) {
            TemplateContent()
        }
    }

    @Composable
    @GlanceComposable
    abstract fun TemplateContent()

    /** Resolves the current display mode */
    @Composable
    private fun mode(): TemplateMode {
        val height = LocalSize.current.height
        val width = LocalSize.current.width
        return if (height <= Dp(240f) && width <= Dp(240f)) {
            TemplateMode.Collapsed
        } else if ((width / height) < (3.0 / 2.0)) {
            TemplateMode.Vertical
        } else {
            TemplateMode.Horizontal
        }
    }
}
