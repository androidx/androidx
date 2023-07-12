/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.builtintypes.experimental.properties

import androidx.appactions.builtintypes.experimental.types.ListItem

class ItemListElement(asListItem: ListItem) {
    @get:JvmName("asListItem")
    val asListItem: ListItem = asListItem

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ItemListElement) return false
        if (asListItem != other.asListItem) return false
        return true
    }
}