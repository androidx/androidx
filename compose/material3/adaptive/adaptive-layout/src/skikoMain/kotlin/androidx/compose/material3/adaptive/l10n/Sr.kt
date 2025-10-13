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
internal fun Translations.sr() = mapOf(
    Strings.defaultPaneTitlePrimary to "Примарно окно",
    Strings.defaultPaneTitleSecondary to "Секундарно окно",
    Strings.defaultPaneTitleTertiary to "Терцијарно окно",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Маркер за превлачење којим се проширује окно",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Тренутно подељено окно, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Промените подељено окно на: %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "Проценат: %d",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "Тачака података од почетка: %d",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "Тачака података од краја: %d",
    Strings.dragToResizeClickToExpandDescription to "прошири",
    Strings.dragToResizeClickToCollapseDescription to "скупи",
    Strings.dragToResizeClickToPartiallyExpandDescription to "делимично прошири",
    Strings.dragToResizeExpandedStateDescription to "проширено",
    Strings.dragToResizeCollapsedStateDescription to "скупљено",
    Strings.dragToResizePartiallyExpandedStateDescription to "делимично проширено",
)
