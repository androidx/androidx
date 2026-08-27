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
 * @param image [A2uiBasicCatalogV1.Image] component implementation. Use
 *   [MaterialA2uiBasicCatalogV1Defaults.image] to create a default Material 3 implementation with
 *   an [A2uiImageRenderer]
 * @param urlOpener [A2uiUrlOpener] used by catalog functions to open URLs
 * @param messageFormatter [A2uiMessageFormatter] used by catalog functions to format localized
 *   messages with arguments
 * @param localeProvider [A2uiLocaleProvider] used by catalog functions to determine the active
 *   locale
 * @param text [A2uiBasicCatalogV1.Text] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.text]
 * @param icon [A2uiBasicCatalogV1.Icon] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.icon]
 * @param card [A2uiBasicCatalogV1.Card] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.card]
 * @param row [A2uiBasicCatalogV1.Row] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.row]
 * @param column [A2uiBasicCatalogV1.Column] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.column]
 * @param list [A2uiBasicCatalogV1.List] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.list]
 * @param tabs [A2uiBasicCatalogV1.Tabs] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.tabs]
 * @param button [A2uiBasicCatalogV1.Button] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.button]
 * @param dateTimeInput [A2uiBasicCatalogV1.DateTimeInput] component implementation, defaults to
 *   [MaterialA2uiBasicCatalogV1Defaults.dateTimeInput]
 * @return an [A2uiCatalog] configured with Material 3 basic components and functions
 */
public fun materialA2uiBasicCatalogV1(
    image: A2uiBasicCatalogV1.Image,
    urlOpener: A2uiUrlOpener,
    messageFormatter: A2uiMessageFormatter,
    localeProvider: A2uiLocaleProvider,
    text: A2uiBasicCatalogV1.Text = MaterialA2uiBasicCatalogV1Defaults.text,
    icon: A2uiBasicCatalogV1.Icon = MaterialA2uiBasicCatalogV1Defaults.icon,
    card: A2uiBasicCatalogV1.Card = MaterialA2uiBasicCatalogV1Defaults.card,
    row: A2uiBasicCatalogV1.Row = MaterialA2uiBasicCatalogV1Defaults.row,
    column: A2uiBasicCatalogV1.Column = MaterialA2uiBasicCatalogV1Defaults.column,
    list: A2uiBasicCatalogV1.List = MaterialA2uiBasicCatalogV1Defaults.list,
    tabs: A2uiBasicCatalogV1.Tabs = MaterialA2uiBasicCatalogV1Defaults.tabs,
    button: A2uiBasicCatalogV1.Button = MaterialA2uiBasicCatalogV1Defaults.button,
    dateTimeInput: A2uiBasicCatalogV1.DateTimeInput =
        MaterialA2uiBasicCatalogV1Defaults.dateTimeInput,
    // TODO(b/547851648): Add the rest of the basic catalog component types.
): A2uiCatalog =
    A2uiCatalog(
        A2uiBasicCatalogV1(
            text = text,
            image = image,
            icon = icon,
            card = card,
            row = row,
            column = column,
            list = list,
            tabs = tabs,
            button = button,
            dateTimeInput = dateTimeInput,
            // TODO(b/547851648): Add the rest of the basic catalog component types.
            functions = createBasicCatalogFunctions(urlOpener, messageFormatter, localeProvider),
        )
    )

/** Default component implementations for [materialA2uiBasicCatalogV1]. */
public object MaterialA2uiBasicCatalogV1Defaults {
    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.Text] component. */
    public val text: A2uiBasicCatalogV1.Text = MaterialA2uiBasicCatalogV1Text

    /**
     * Creates a default Material 3 implementation of the [A2uiBasicCatalogV1.Image] component.
     *
     * @param imageRenderer [A2uiImageRenderer] used to render images
     * @return an [A2uiBasicCatalogV1.Image] instance
     */
    public fun image(imageRenderer: A2uiImageRenderer): A2uiBasicCatalogV1.Image =
        MaterialA2uiBasicCatalogV1Image(imageRenderer)

    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.Icon] component. */
    public val icon: A2uiBasicCatalogV1.Icon = MaterialA2uiBasicCatalogV1Icon

    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.Card] component. */
    public val card: A2uiBasicCatalogV1.Card = MaterialA2uiBasicCatalogV1Card

    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.Row] component. */
    public val row: A2uiBasicCatalogV1.Row = MaterialA2uiBasicCatalogV1Row

    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.Column] component. */
    public val column: A2uiBasicCatalogV1.Column = MaterialA2uiBasicCatalogV1Column

    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.List] component. */
    public val list: A2uiBasicCatalogV1.List = MaterialA2uiBasicCatalogV1List

    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.Tabs] component. */
    public val tabs: A2uiBasicCatalogV1.Tabs = MaterialA2uiBasicCatalogV1Tabs

    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.Button] component. */
    public val button: A2uiBasicCatalogV1.Button = MaterialA2uiBasicCatalogV1Button

    /** Default Material 3 implementation of the [A2uiBasicCatalogV1.DateTimeInput] component. */
    public val dateTimeInput: A2uiBasicCatalogV1.DateTimeInput =
        MaterialA2uiBasicCatalogV1DateTimeInput

    // TODO(b/547851648): Add the rest of the basic catalog component types.
}
