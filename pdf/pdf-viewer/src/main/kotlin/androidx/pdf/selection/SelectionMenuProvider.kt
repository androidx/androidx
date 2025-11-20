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

/**
 * An interface for providing context menu items based on a given [Selection].
 *
 * This allows different types of selections (e.g., text, image) to offer their own specific actions
 * within a context menu.
 *
 * @param T The type of [Selection] this provider handles, which must inherit from [Selection].
 */
internal interface SelectionMenuProvider<T : Selection> {
    /**
     * Retrieves a list of [ContextMenuComponent]s to be displayed for the given [selection].
     *
     * This is a suspend function, indicating that the retrieval of menu items might involve
     * asynchronous operations, such as fetching data or performing complex calculations.
     *
     * @param selection The [Selection] for which to provide context menu items.
     * @return A [List] of [ContextMenuComponent]s relevant to the provided [selection].
     */
    suspend fun getMenuItems(selection: T): List<ContextMenuComponent>
}
