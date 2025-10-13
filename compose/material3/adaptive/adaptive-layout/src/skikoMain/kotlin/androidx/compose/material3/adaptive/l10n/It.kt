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
internal fun Translations.it() = mapOf(
    Strings.defaultPaneTitlePrimary to "Riquadro principale",
    Strings.defaultPaneTitleSecondary to "Riquadro secondario",
    Strings.defaultPaneTitleTertiary to "Riquadro terziario",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Punto di trascinamento per l\'espansione del riquadro",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Divisione del riquadro attuale, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Modifica la divisione del riquadro in %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d percentuale",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "A %d DP dall\'inizio",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "A %d DP dalla fine",
    Strings.dragToResizeClickToExpandDescription to "espandi",
    Strings.dragToResizeClickToCollapseDescription to "comprimi",
    Strings.dragToResizeClickToPartiallyExpandDescription to "espandi parzialmente",
    Strings.dragToResizeExpandedStateDescription to "espanso",
    Strings.dragToResizeCollapsedStateDescription to "compresso",
    Strings.dragToResizePartiallyExpandedStateDescription to "parzialmente espanso",
)
