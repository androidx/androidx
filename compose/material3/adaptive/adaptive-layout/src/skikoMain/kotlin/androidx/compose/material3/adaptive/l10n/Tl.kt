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
internal fun Translations.tl() = mapOf(
    Strings.defaultPaneTitlePrimary to "Pangunahing pane",
    Strings.defaultPaneTitleSecondary to "Pangalawang pane",
    Strings.defaultPaneTitleTertiary to "Pangatlong pane",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Handle sa pag-drag sa pagpapalawak ng pane",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Kasalukuyang hati ng pane, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Gawing %s ang pane split",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d (na) porsyento",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d (na) DP mula sa simula",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d (na) DP mula sa dulo",
    Strings.dragToResizeClickToExpandDescription to "i-expand",
    Strings.dragToResizeClickToCollapseDescription to "i-collapse",
    Strings.dragToResizeClickToPartiallyExpandDescription to "bahagyang i-expand",
    Strings.dragToResizeExpandedStateDescription to "naka-expand",
    Strings.dragToResizeCollapsedStateDescription to "naka-collapse",
    Strings.dragToResizePartiallyExpandedStateDescription to "bahagyang naka-expand",
)
