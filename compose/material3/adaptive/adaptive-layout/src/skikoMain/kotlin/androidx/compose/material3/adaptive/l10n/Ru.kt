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
internal fun Translations.ru() = mapOf(
    Strings.defaultPaneTitlePrimary to "Основная панель",
    Strings.defaultPaneTitleSecondary to "Вспомогательная панель",
    Strings.defaultPaneTitleTertiary to "Дополнительная панель",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Маркер перемещения для расширения панели",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Пропорция разделения панелей, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Изменить пропорцию разделения панелей на %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "Значение в процентах: %d",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DP с начала",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DP с конца",
    Strings.dragToResizeClickToExpandDescription to "развернуть",
    Strings.dragToResizeClickToCollapseDescription to "свернуть",
    Strings.dragToResizeClickToPartiallyExpandDescription to "частично развернуть",
    Strings.dragToResizeExpandedStateDescription to "развернуто",
    Strings.dragToResizeCollapsedStateDescription to "свернуто",
    Strings.dragToResizePartiallyExpandedStateDescription to "в частично развернутом виде",
)
