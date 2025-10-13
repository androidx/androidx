/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3.adaptive.l10n

import androidx.compose.material3.adaptive.layout.internal.Strings
import androidx.compose.material3.adaptive.layout.internal.Translations

@Suppress("UnusedReceiverParameter", "DuplicatedCode")
internal fun Translations.gl() = mapOf(
    Strings.defaultPaneTitlePrimary to "Panel principal",
    Strings.defaultPaneTitleSecondary to "Panel secundario",
    Strings.defaultPaneTitleTertiary to "Panel terciario",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Controlador de arrastre para despregar o panel",
    Strings.defaultPaneExpansionDragHandleStateDescription to "División actual do panel, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Cambia o panel dividido a %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d por cento",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DP desde o inicio",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DP desde o final",
    Strings.dragToResizeClickToExpandDescription to "despregar",
    Strings.dragToResizeClickToCollapseDescription to "contraer",
    Strings.dragToResizeClickToPartiallyExpandDescription to "despregar parcialmente",
    Strings.dragToResizeExpandedStateDescription to "despregado",
    Strings.dragToResizeCollapsedStateDescription to "contraído",
    Strings.dragToResizePartiallyExpandedStateDescription to "despregado parcialmente",
)
