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
internal fun Translations.hu() = mapOf(
    Strings.defaultPaneTitlePrimary to "Elsődleges panel",
    Strings.defaultPaneTitleSecondary to "Másodlagos panel",
    Strings.defaultPaneTitleTertiary to "Harmadlagos panel",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Panel kibontásának fogópontja",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Jelenlegi panelfelosztás, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Panelfelosztás módosítása a következőre: %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d százalék",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d sűrűségfüggetlen képpont az elejétől",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d sűrűségfüggetlen képpont a végétől",
    Strings.dragToResizeClickToExpandDescription to "kibontás",
    Strings.dragToResizeClickToCollapseDescription to "összecsukás",
    Strings.dragToResizeClickToPartiallyExpandDescription to "részleges kibontás",
    Strings.dragToResizeExpandedStateDescription to "kibontva",
    Strings.dragToResizeCollapsedStateDescription to "összecsukva",
    Strings.dragToResizePartiallyExpandedStateDescription to "részben kibontva",
)
