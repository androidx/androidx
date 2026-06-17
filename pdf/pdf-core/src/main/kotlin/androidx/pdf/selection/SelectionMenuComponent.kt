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

package androidx.pdf.selection

import androidx.annotation.RestrictTo

/**
 * Defines unique keys for the default selection menu items.
 *
 * These keys are used to identify standard actions like Copy and Select All within the selection
 * context menu.
 */
public object PdfSelectionMenuKeys {
    /** Key for the context menu "Copy" item. */
    @JvmField public val CopyKey: Any = Any()

    /** Key for the context menu "Copy link" item. */
    @JvmField public val CopyLinkKey: Any = Any()

    /** Key for the context menu "Jump" item. */
    @JvmField public val GoToKey: Any = Any()

    /** Key for the context menu "Select all" item. */
    @JvmField public val SelectAllKey: Any = Any()

    /** Key for all "smart actions" added by classifier in context menu. */
    @JvmField public val SmartActionKey: Any = Any()
}

/**
 * An abstract base class for any component that can be displayed within a context menu.
 *
 * @param key A unique identifier for this component within its context menu.
 */
public abstract class ContextMenuComponent
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public constructor(public val key: Any)
