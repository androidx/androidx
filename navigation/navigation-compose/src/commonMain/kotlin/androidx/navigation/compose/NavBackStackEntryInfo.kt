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

package androidx.navigation.compose

import androidx.compose.runtime.Immutable
import androidx.navigation.NavBackStackEntry
import androidx.navigationevent.NavigationEventInfo

/**
 * Snapshot of a back stack entry in a [androidx.navigation.NavController].
 *
 * Provides context for navigation transition animations such as predictive back.
 *
 * @param visibleEntry back stack entry associated with this snapshot
 */
@Immutable
public class NavBackStackEntryInfo(public val visibleEntry: NavBackStackEntry? = null) :
    NavigationEventInfo() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NavBackStackEntryInfo

        return visibleEntry == other.visibleEntry
    }

    override fun hashCode(): Int = visibleEntry.hashCode()

    override fun toString(): String = "NavBackStackEntryInfo(visibleEntry=$visibleEntry)"
}
