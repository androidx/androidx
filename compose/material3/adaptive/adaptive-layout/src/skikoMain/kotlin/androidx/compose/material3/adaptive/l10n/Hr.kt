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
internal fun Translations.hr() = mapOf(
    Strings.defaultPaneTitlePrimary to "Primarno okno",
    Strings.defaultPaneTitleSecondary to "Sekundarno okno",
    Strings.defaultPaneTitleTertiary to "Tercijarno okno",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Marker za povlačenje proširenja okna",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Trenutačna podjela okna, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Promijeni podjelu okna u: %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d posto",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DP-ova od početka",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DP-ova od kraja",
    Strings.dragToResizeClickToExpandDescription to "proširi",
    Strings.dragToResizeClickToCollapseDescription to "sažmi",
    Strings.dragToResizeClickToPartiallyExpandDescription to "djelomično proširi",
    Strings.dragToResizeExpandedStateDescription to "prošireno",
    Strings.dragToResizeCollapsedStateDescription to "sažeto",
    Strings.dragToResizePartiallyExpandedStateDescription to "djelomično prošireno",
)
