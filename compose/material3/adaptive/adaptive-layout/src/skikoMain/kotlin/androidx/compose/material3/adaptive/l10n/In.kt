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
internal fun Translations.`in`() = mapOf(
    Strings.defaultPaneTitlePrimary to "Panel utama",
    Strings.defaultPaneTitleSecondary to "Panel sekunder",
    Strings.defaultPaneTitleTertiary to "Panel tersier",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Handel geser perluasan panel",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Luas panel ganda saat ini, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Ubah luas panel ganda menjadi %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d persen",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DP dari awal",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DP dari akhir",
    Strings.dragToResizeClickToExpandDescription to "luaskan",
    Strings.dragToResizeClickToCollapseDescription to "ciutkan",
    Strings.dragToResizeClickToPartiallyExpandDescription to "luaskan sebagian",
    Strings.dragToResizeExpandedStateDescription to "diluaskan",
    Strings.dragToResizeCollapsedStateDescription to "diciutkan",
    Strings.dragToResizePartiallyExpandedStateDescription to "diluaskan sebagian",
)
