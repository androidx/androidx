/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.glance.template

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition

/** A [GlanceAppWidget] that provides template local values. */
abstract class GlanceTemplateAppWidget : GlanceAppWidget() {

    companion object {
        internal val sizeMin = 30.dp
        internal val sizeS = 200.dp
        internal val sizeM = 241.dp
        internal val sizeL = 350.dp
        internal val sizeXL = 600.dp
        private val COLLAPSED = DpSize(sizeMin, sizeMin)
        private val HORIZONTAL_S = DpSize(sizeM, sizeMin)
        private val HORIZONTAL_M = DpSize(sizeM, sizeS)
        private val HORIZONTAL_L = DpSize(sizeL, sizeMin)
        private val HORIZONTAL_XL = DpSize(sizeXL, sizeL)
        private val VERTICAL_S = DpSize(sizeMin, sizeM)
        private val VERTICAL_M = DpSize(sizeS, sizeM)
        private val VERTICAL_L = DpSize(sizeS, sizeL)
    }

    /** Default widget size mode is [SizeMode.Responsive] */
    override val sizeMode: SizeMode =
        SizeMode.Responsive(
            setOf(
                COLLAPSED,
                VERTICAL_S,
                VERTICAL_M,
                VERTICAL_L,
                HORIZONTAL_S,
                HORIZONTAL_M,
                HORIZONTAL_L,
                HORIZONTAL_XL
            )
        )

    /** Default widget state definition is [PreferencesGlanceStateDefinition] */
    override val stateDefinition: GlanceStateDefinition<*>? = PreferencesGlanceStateDefinition

    final override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        CompositionLocalProvider(
            LocalTemplateMode provides mode(),
        ) {
            TemplateContent()
        }
    }

    @Composable @GlanceComposable abstract fun TemplateContent()

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
