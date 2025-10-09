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
internal fun Translations.`is`() = mapOf(
    Strings.defaultPaneTitlePrimary to "Fyrsti gluggi",
    Strings.defaultPaneTitleSecondary to "Annar gluggi",
    Strings.defaultPaneTitleTertiary to "Þriðji gluggi",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Dragkló gluggastækkunar",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Núverandi skiptur gluggi, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Breyta gluggaskiptingu í %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d prósent",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d pixlar óháðir þéttleika (DP) frá upphafspunkti",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d pixlar óháðir þéttleika (DP) frá endapunkti",
    Strings.dragToResizeClickToExpandDescription to "stækka",
    Strings.dragToResizeClickToCollapseDescription to "draga saman",
    Strings.dragToResizeClickToPartiallyExpandDescription to "stækka að hluta",
    Strings.dragToResizeExpandedStateDescription to "stækkað",
    Strings.dragToResizeCollapsedStateDescription to "minnkað",
    Strings.dragToResizePartiallyExpandedStateDescription to "stækkað að hluta",
)
