/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi

/**
 * An item representing a navigation destination in a [ThreePaneScaffold].
 *
 * @param pane the pane destination of the navigation.
 * @param contentKey the optional key or id representing the content of the destination. The type
 *   [T] must be storable in a Bundle.
 */
@ExperimentalMaterial3AdaptiveApi
class ThreePaneScaffoldDestinationItem<out T>(
    val pane: ThreePaneScaffoldRole,
    val contentKey: T? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ThreePaneScaffoldDestinationItem<*>) return false

        if (pane != other.pane) return false
        if (contentKey != other.contentKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = pane.hashCode()
        result = 31 * result + (contentKey?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ThreePaneScaffoldDestinationItem(pane=$pane, contentKey=$contentKey)"
    }
}
