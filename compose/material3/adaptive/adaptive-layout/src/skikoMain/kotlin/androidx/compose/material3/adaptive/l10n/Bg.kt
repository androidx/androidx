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
internal fun Translations.bg() = mapOf(
    Strings.defaultPaneTitlePrimary to "Основен панел",
    Strings.defaultPaneTitleSecondary to "Вторичен панел",
    Strings.defaultPaneTitleTertiary to "Третичен панел",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Манипулатор за преместване с плъзгане за разширяване на панела",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Текущо разделяне на панела – %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Промяна на разделянето на панела на %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "Процент: %d",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d независещи от плътността пиксели от началото",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d независещи от плътността пиксели от края",
    Strings.dragToResizeClickToExpandDescription to "разгъване",
    Strings.dragToResizeClickToCollapseDescription to "свиване",
    Strings.dragToResizeClickToPartiallyExpandDescription to "частично разгъване",
    Strings.dragToResizeExpandedStateDescription to "разгънато",
    Strings.dragToResizeCollapsedStateDescription to "свито",
    Strings.dragToResizePartiallyExpandedStateDescription to "частично разгънато",
)
