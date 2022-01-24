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

package androidx.template.appwidget

import androidx.compose.runtime.Composable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.template.template.GlanceTemplate
import androidx.template.template.TemplateTranslator

/**
 * A [GlanceAppWidget] that uses a [GlanceTemplate] to define its layout.
 */
public abstract class GlanceTemplateAppWidget(
    private val template: GlanceTemplate<*>
) : GlanceAppWidget() {

    /** Default widget size mode is [SizeMode.Exact] */
    override val sizeMode = SizeMode.Exact

    /** Default widget state definition is [PreferencesGlanceStateDefinition] */
    override val stateDefinition: GlanceStateDefinition<*>? = PreferencesGlanceStateDefinition

    private val translator = TemplateTranslator()

    @Composable
    final override fun Content() {
        translator.TemplateContent(template)
    }
}
