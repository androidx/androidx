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

package androidx.pdf.selection

import android.graphics.drawable.Drawable
import androidx.annotation.RestrictTo
import androidx.pdf.view.PdfView

/**
 * Defines unique keys for the default selection menu items.
 *
 * These keys are used to identify standard actions like Copy and Select All within the selection
 * context menu.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object PdfSelectionMenuKeys {
    public object CopyKey

    public object SelectAllKey
}

/**
 * An abstract base class for any component that can be displayed within a context menu.
 *
 * @property key A unique identifier for this component within its context menu.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class ContextMenuComponent internal constructor(public val key: Any)

/**
 * Represents a clickable item with a label in a selection menu.
 *
 * @param key A unique identifier for this component.
 * @property label The text to display for this menu item.
 * @property contentDescription An optional accessibility description for screen readers.
 * @property onClick A lambda function invoked when the item is clicked, providing access to the
 *   [SelectionMenuSession].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SelectionMenuComponent(
    key: Any,
    public val label: String,
    public val contentDescription: String? = null,
    public val onClick: SelectionMenuSession.() -> Unit,
) : ContextMenuComponent(key)

/**
 * Represents a default selection menu item, designed for the standard selection menu components
 * that might need access to the [PdfView] instance when clicked, such as "Copy" or "Select All".
 *
 * @param key A unique identifier for this component.
 * @property label The text to display for this menu item.
 * @property contentDescription An optional accessibility description for screen readers.
 * @property onClick A lambda function invoked when the item is clicked, providing access to the
 *   [SelectionMenuSession] and the [PdfView].
 */
internal class DefaultSelectionMenuComponent(
    key: Any,
    public val label: String,
    public val contentDescription: String? = null,
    public val onClick: SelectionMenuSession.(pdfView: PdfView) -> Unit,
) : ContextMenuComponent(key)

/**
 * Represents a smart selection menu item, which can include a leading icon in addition to a label
 * and action.
 *
 * @param key A unique identifier for this component.
 * @property label The text to display for this menu item.
 * @property contentDescription An optional accessibility description for screen readers.
 * @property leadingIcon An optional drawable to display as an icon before the label.
 * @property onClick A lambda function invoked when the item is clicked, providing access to the
 *   [SelectionMenuSession].
 */
internal class SmartSelectionMenuComponent(
    key: Any,
    public val label: String,
    public val contentDescription: String? = null,
    public val leadingIcon: Drawable? = null,
    public val onClick: SelectionMenuSession.() -> Unit,
) : ContextMenuComponent(key)

/** Represents an active session of an open selection menu. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface SelectionMenuSession {
    /** Closes the currently open text context menu. */
    public fun close()
}
