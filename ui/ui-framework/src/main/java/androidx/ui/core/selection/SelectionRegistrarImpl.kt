/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.selection

internal class SelectionRegistrarImpl : SelectionRegistrar {
    /**
     * This is essentially the list of registered components that want
     * to handle text selection that are below the SelectionContainer.
     */
    private val _selectables = mutableSetOf<Selectable>()

    /**
     * Getter for handlers that returns an immutable Set.
     */
    internal val selectables: Set<Selectable>
        get() = _selectables

    override fun subscribe(selectable: Selectable): Selectable {
        _selectables.add(selectable)
        return selectable
    }

    override fun unsubscribe(selectable: Selectable) {
        _selectables.remove(selectable)
    }
}