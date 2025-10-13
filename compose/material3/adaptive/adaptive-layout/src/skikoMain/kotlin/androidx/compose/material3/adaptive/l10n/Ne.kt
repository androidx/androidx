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
internal fun Translations.ne() = mapOf(
    Strings.defaultPaneTitlePrimary to "मुख्य सामग्री भएको पेन",
    Strings.defaultPaneTitleSecondary to "सहायक सामग्री भएको पेन",
    Strings.defaultPaneTitleTertiary to "अतिरिक्त सामग्री भएको पेन",
    Strings.defaultPaneExpansionDragHandleContentDescription to "पेन एक्स्पान्सनको ड्र्याग ह्यान्डल",
    Strings.defaultPaneExpansionDragHandleStateDescription to "पेन स्प्लिटको हालको स्थिति, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "पेन स्प्लिट परिवर्तन गरी %s बनाउनुहोस्",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d प्रतिशत",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "सुरुबाट %d DP",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "अन्त्यबाट %d DP",
    Strings.dragToResizeClickToExpandDescription to "एक्स्पान्ड गर्नुहोस्",
    Strings.dragToResizeClickToCollapseDescription to "कोल्याप्स गर्नुहोस्",
    Strings.dragToResizeClickToPartiallyExpandDescription to "आंशिक रूपमा एक्स्पान्ड गर्नुहोस्",
    Strings.dragToResizeExpandedStateDescription to "एक्स्पान्ड गरिएको छ",
    Strings.dragToResizeCollapsedStateDescription to "कोल्याप्स गरिएको छ",
    Strings.dragToResizePartiallyExpandedStateDescription to "आंशिक रूपमा एक्स्पान्ड गरिएको छ",
)
