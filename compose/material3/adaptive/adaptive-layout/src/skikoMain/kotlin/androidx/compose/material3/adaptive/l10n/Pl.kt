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
internal fun Translations.pl() = mapOf(
    Strings.defaultPaneTitlePrimary to "Panel główny",
    Strings.defaultPaneTitleSecondary to "Panel dodatkowy",
    Strings.defaultPaneTitleTertiary to "Panel trzeciorzędny",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Uchwyt do przeciągania panelu",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Bieżący podział panelu: %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Zmień podział panelu na %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d procent",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DP od początku",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DP od końca",
    Strings.dragToResizeClickToExpandDescription to "rozwiń",
    Strings.dragToResizeClickToCollapseDescription to "zwiń",
    Strings.dragToResizeClickToPartiallyExpandDescription to "częściowo rozwiń",
    Strings.dragToResizeExpandedStateDescription to "rozwinięty",
    Strings.dragToResizeCollapsedStateDescription to "zwinięty",
    Strings.dragToResizePartiallyExpandedStateDescription to "częściowo rozwinięty",
)
