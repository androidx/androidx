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
internal fun Translations.cs() = mapOf(
    Strings.defaultPaneTitlePrimary to "Primární panel",
    Strings.defaultPaneTitleSecondary to "Sekundární panel",
    Strings.defaultPaneTitleTertiary to "Terciární panel",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Úchyt pro přetažení a rozbalení panelu",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Aktuální rozdělení panelu, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Změnit rozdělení panelu na %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d procent",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DP od začátku",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DP od konce",
    Strings.dragToResizeClickToExpandDescription to "rozbalit",
    Strings.dragToResizeClickToCollapseDescription to "sbalit",
    Strings.dragToResizeClickToPartiallyExpandDescription to "částečně rozbalit",
    Strings.dragToResizeExpandedStateDescription to "rozbaleno",
    Strings.dragToResizeCollapsedStateDescription to "sbaleno",
    Strings.dragToResizePartiallyExpandedStateDescription to "částečně rozbaleno",
)
