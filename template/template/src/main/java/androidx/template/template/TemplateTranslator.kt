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

package androidx.template.template

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.glance.LocalSize

/**
 * Fetches composable content from a template, appropriate to the given surface and display context.
 * The translator parses the display context to determine which template content to fetch.
 */
public class TemplateTranslator {

    @Composable
    public fun TemplateContent(template: GlanceTemplate<*>) {
        // TODO: pass in host context and get layout for display params, including surface type etc.
        val height = LocalSize.current.height
        val width = LocalSize.current.width
        if (height < Dp(240f) && width < Dp(240f)) {
            template.WidgetLayoutCollapsed()
        } else if ((width / height) < (3.0 / 2.0)) {
            template.WidgetLayoutVertical()
        } else {
            template.WidgetLayoutHorizontal()
        }
    }
}
