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
internal fun Translations.bn() = mapOf(
    Strings.defaultPaneTitlePrimary to "প্রাথমিক প্যানেল",
    Strings.defaultPaneTitleSecondary to "সেকেন্ডারি প্যানেল",
    Strings.defaultPaneTitleTertiary to "টার্শিয়ারি প্যানেল",
    Strings.defaultPaneExpansionDragHandleContentDescription to "প্যানেল বড় করার টেনে আনার হ্যান্ডেল",
    Strings.defaultPaneExpansionDragHandleStateDescription to "প্যানেল স্প্লিটের বর্তমান স্ট্যাটাস %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "প্যানেল স্প্লিট %s-এ পরিবর্তন করুন",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d শতাংশ",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "শুরুর দিক থেকে %d DPs",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "শেষের দিক থেকে %d DPs",
    Strings.dragToResizeClickToExpandDescription to "বড় করুন",
    Strings.dragToResizeClickToCollapseDescription to "আড়াল করুন",
    Strings.dragToResizeClickToPartiallyExpandDescription to "আংশিক বড় করুন",
    Strings.dragToResizeExpandedStateDescription to "বড় করা হয়েছে",
    Strings.dragToResizeCollapsedStateDescription to "আড়াল করা হয়েছে",
    Strings.dragToResizePartiallyExpandedStateDescription to "আংশিক বড় করা হয়েছে",
)
