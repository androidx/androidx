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
internal fun Translations.el() = mapOf(
    Strings.defaultPaneTitlePrimary to "Κύριο πλαίσιο",
    Strings.defaultPaneTitleSecondary to "Δευτερεύον πλαίσιο",
    Strings.defaultPaneTitleTertiary to "Τριτεύον πλαίσιο",
    Strings.defaultPaneExpansionDragHandleContentDescription to "Λαβή μεταφοράς επέκτασης πλαισίου",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Τρέχων επιμερισμός πλαισίου, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Αλλαγή επιμερισμού πλαισίου σε %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d τοις εκατό",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DPs από την αρχή",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DPs από το τέλος",
    Strings.dragToResizeClickToExpandDescription to "ανάπτυξη",
    Strings.dragToResizeClickToCollapseDescription to "σύμπτυξη",
    Strings.dragToResizeClickToPartiallyExpandDescription to "μερική ανάπτυξη",
    Strings.dragToResizeExpandedStateDescription to "αναπτυγμένo",
    Strings.dragToResizeCollapsedStateDescription to "συμπτυγμένo",
    Strings.dragToResizePartiallyExpandedStateDescription to "μερικώς αναπτυγμένo",
)
