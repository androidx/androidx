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
internal fun Translations.de() = mapOf(
    Strings.defaultPaneTitlePrimary to "Primärer Bereich",
    Strings.defaultPaneTitleSecondary to "Sekundärer Bereich",
    Strings.defaultPaneTitleTertiary to "Tertiärer Bereich",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Ziehpunkt zum Maximieren des Bereichs",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Aktueller Bereich geteilt, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "„Fenster teilen“ in %s ändern",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d Prozent",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DPs vom Anfang",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DPs vom Ende",
    Strings.dragToResizeClickToExpandDescription to "maximieren",
    Strings.dragToResizeClickToCollapseDescription to "minimieren",
    Strings.dragToResizeClickToPartiallyExpandDescription to "teilweise maximieren",
    Strings.dragToResizeExpandedStateDescription to "maximiert",
    Strings.dragToResizeCollapsedStateDescription to "minimiert",
    Strings.dragToResizePartiallyExpandedStateDescription to "teilweise maximiert",
)
