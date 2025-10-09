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
internal fun Translations.th() = mapOf(
    Strings.defaultPaneTitlePrimary to "แผงหลัก",
    Strings.defaultPaneTitleSecondary to "แผงรอง",
    Strings.defaultPaneTitleTertiary to "แผงลำดับที่ 3",
    Strings.defaultPaneExpansionDragHandleContentDescription to "แฮนเดิลการลากเพื่อขยายแผง",
    Strings.defaultPaneExpansionDragHandleStateDescription to "การแบ่งแผงปัจจุบัน %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "เปลี่ยนการแบ่งแผงเป็น %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d เปอร์เซ็นต์",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DP จากจุดเริ่มต้น",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DP จากปลายทาง",
    Strings.dragToResizeClickToExpandDescription to "ขยาย",
    Strings.dragToResizeClickToCollapseDescription to "ยุบ",
    Strings.dragToResizeClickToPartiallyExpandDescription to "ขยายบางส่วน",
    Strings.dragToResizeExpandedStateDescription to "ขยายแล้ว",
    Strings.dragToResizeCollapsedStateDescription to "ยุบแล้ว",
    Strings.dragToResizePartiallyExpandedStateDescription to "ขยายบางส่วนแล้ว",
)
