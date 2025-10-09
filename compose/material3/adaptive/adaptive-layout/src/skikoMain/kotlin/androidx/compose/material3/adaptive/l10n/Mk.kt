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
internal fun Translations.mk() = mapOf(
    Strings.defaultPaneTitlePrimary to "Примарно окно",
    Strings.defaultPaneTitleSecondary to "Секундарно окно",
    Strings.defaultPaneTitleTertiary to "Терцијарно окно",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Рачка за влечење за проширување на окното",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Тековно поделено окно, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Променете го поделеното окно на %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d насто",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DP од почетокот",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DP од крајот",
    Strings.dragToResizeClickToExpandDescription to "прошири",
    Strings.dragToResizeClickToCollapseDescription to "собери",
    Strings.dragToResizeClickToPartiallyExpandDescription to "делумно прошири",
    Strings.dragToResizeExpandedStateDescription to "проширено",
    Strings.dragToResizeCollapsedStateDescription to "собрано",
    Strings.dragToResizePartiallyExpandedStateDescription to "делумно проширено",
)
