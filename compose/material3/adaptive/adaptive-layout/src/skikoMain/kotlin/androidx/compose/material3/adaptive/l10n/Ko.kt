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
internal fun Translations.ko() = mapOf(
    Strings.defaultPaneTitlePrimary to "기본 창",
    Strings.defaultPaneTitleSecondary to "보조 창",
    Strings.defaultPaneTitleTertiary to "세 번째 창",
    Strings.defaultPaneExpansionDragHandleContentDescription to "창 확장 드래그 핸들",
    Strings.defaultPaneExpansionDragHandleStateDescription to "현재 창 분할, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "창 분할을 %s(으)로 변경",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d퍼센트",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "시작 지점에서 %d DP",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "끝에서 %d DP",
    Strings.dragToResizeClickToExpandDescription to "펼치기",
    Strings.dragToResizeClickToCollapseDescription to "접기",
    Strings.dragToResizeClickToPartiallyExpandDescription to "부분적으로 펼치기",
    Strings.dragToResizeExpandedStateDescription to "펼침",
    Strings.dragToResizeCollapsedStateDescription to "접음",
    Strings.dragToResizePartiallyExpandedStateDescription to "부분적으로 펼침",
)
