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
internal fun Translations.uk() = mapOf(
    Strings.defaultPaneTitlePrimary to "Основна панель",
    Strings.defaultPaneTitleSecondary to "Друга панель",
    Strings.defaultPaneTitleTertiary to "Третя панель",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Маркер переміщення для розгортання панелі",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Поточне розділення панелі, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Змінити розділення панелі на %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d%%",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d пікс. від початку",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d пікс. від кінця",
    Strings.dragToResizeClickToExpandDescription to "розгорнути",
    Strings.dragToResizeClickToCollapseDescription to "згорнути",
    Strings.dragToResizeClickToPartiallyExpandDescription to "частково розгорнути",
    Strings.dragToResizeExpandedStateDescription to "розгорнуто",
    Strings.dragToResizeCollapsedStateDescription to "згорнуто",
    Strings.dragToResizePartiallyExpandedStateDescription to "частково розгорнуто",
)
