/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.ui.A2uiCatalog
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.catalog.basiccatalog.createBasicCatalogFunctions
import androidx.a2ui.model.catalog.functions.A2uiLocaleProvider
import androidx.a2ui.model.catalog.functions.A2uiMessageFormatter
import androidx.a2ui.model.catalog.functions.A2uiUrlOpener

/**
 * Creates an [A2uiCatalog] for the A2UI Basic Catalog V1 using Material Design 3.
 *
 * Provides default Material 3 implementations for all components in the A2UI Basic Catalog V1
 * specification, while allowing individual component implementations to be overridden.
 *
 * @param urlOpener [A2uiUrlOpener] used by catalog functions to open URLs
 * @param messageFormatter [A2uiMessageFormatter] used by catalog functions to format localized
 *   messages with arguments
 * @param localeProvider [A2uiLocaleProvider] used by catalog functions to determine the active
 *   locale
 * @param text [A2uiBasicCatalogV1.Text] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.text]
 * @param card [A2uiBasicCatalogV1.Card] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.card]
 * @return an [A2uiCatalog] configured with Material 3 basic components and functions
 */
public fun materialA2uiBasicCatalogV1(
    urlOpener: A2uiUrlOpener,
    messageFormatter: A2uiMessageFormatter,
    localeProvider: A2uiLocaleProvider,
    text: A2uiBasicCatalogV1.Text = MaterialA2uiBasicCatalogV1Defaults.text,
    card: A2uiBasicCatalogV1.Card = MaterialA2uiBasicCatalogV1Defaults.card,
    // TODO(b/547851648): Add the rest of the basic catalog component types.
): A2uiCatalog =
    A2uiCatalog(
        A2uiBasicCatalogV1(
            text = text,
            card = card,
            // TODO(b/547851648): Add the rest of the basic catalog component types.
            functions = createBasicCatalogFunctions(urlOpener, messageFormatter, localeProvider),
        )
    )

/** Default component implementations for [materialA2uiBasicCatalogV1]. */
public object MaterialA2uiBasicCatalogV1Defaults {
    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.Text] component. */
    public val text: A2uiBasicCatalogV1.Text = MaterialA2uiBasicCatalogV1Text

    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.Card] component. */
    public val card: A2uiBasicCatalogV1.Card = MaterialA2uiBasicCatalogV1Card

    // TODO(b/547851648): Add the rest of the basic catalog component types.
}
