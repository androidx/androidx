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

package androidx.glance.adaptive.appwidget.components.text

import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.glance.adaptive.appwidget.components.RemoteComposeFonts
import androidx.glance.adaptive.appwidget.components.RemoteComposePalette
import androidx.glance.adaptive.appwidget.components.SlotText
import androidx.glance.adaptive.core.slots.TemplateText

/**
 * Draws this text as a supporting value, for an action slot — the RemoteCompose counterpart of
 * [TemplateText.buildSupportingRemoteViews]. Every supporting value has the same style: the primary
 * color blended at 70% over the card background, truncated to a single line.
 */
@RemoteComposable
@Composable
internal fun TemplateText.SupportingContent() {
    SlotText(
        text = value,
        fontSize = TemplateTextDefaults.SupportingTextSize,
        fontWeight = RemoteComposeFonts.SemiBold,
        color = RemoteComposePalette.supportingText,
    )
}
