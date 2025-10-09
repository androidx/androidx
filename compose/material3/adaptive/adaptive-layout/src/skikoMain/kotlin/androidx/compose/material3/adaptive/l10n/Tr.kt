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
internal fun Translations.tr() = mapOf(
    Strings.defaultPaneTitlePrimary to "Birincil bölme",
    Strings.defaultPaneTitleSecondary to "İkincil bölme",
    Strings.defaultPaneTitleTertiary to "Üçüncül bölme",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Bölmeyi genişletmek için sürükleme tutamacı",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Mevcut bölme oranı, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Bölme oranını %s olarak değiştirin",
    Strings.defaultPaneExpansionProportionAnchorDescription to "Yüzde %d",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "Başlangıçtan %d DP",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "Sondan %d DP",
    Strings.dragToResizeClickToExpandDescription to "genişlet",
    Strings.dragToResizeClickToCollapseDescription to "daralt",
    Strings.dragToResizeClickToPartiallyExpandDescription to "kısmen genişlet",
    Strings.dragToResizeExpandedStateDescription to "genişletildi",
    Strings.dragToResizeCollapsedStateDescription to "daraltıldı",
    Strings.dragToResizePartiallyExpandedStateDescription to "kısmen genişletildi",
)
