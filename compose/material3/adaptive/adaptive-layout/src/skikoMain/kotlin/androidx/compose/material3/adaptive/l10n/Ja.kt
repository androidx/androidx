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
internal fun Translations.ja() = mapOf(
    Strings.defaultPaneTitlePrimary to "プライマリ ペイン",
    Strings.defaultPaneTitleSecondary to "セカンダリ ペイン",
    Strings.defaultPaneTitleTertiary to "ターシャリ ペイン",
    Strings.defaultPaneExpansionDragHandleContentDescription to "ペインの展開のドラッグ ハンドル",
    Strings.defaultPaneExpansionDragHandleStateDescription to "現在のペイン分割、%s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "ペインの分割を %s に変更",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d パーセント",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "開始地点から %d DP",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "終了地点から %d DP",
    Strings.dragToResizeClickToExpandDescription to "開く",
    Strings.dragToResizeClickToCollapseDescription to "閉じる",
    Strings.dragToResizeClickToPartiallyExpandDescription to "一部開く",
    Strings.dragToResizeExpandedStateDescription to "開いています",
    Strings.dragToResizeCollapsedStateDescription to "閉じています",
    Strings.dragToResizePartiallyExpandedStateDescription to "一部開いています",
)
