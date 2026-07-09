/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
@kotlin.jvm.JvmInline
internal value class Strings private constructor(@Suppress("unused") private val value: Int) {
    companion object {
        val NavigationMenu
            get() = Strings(0)

        val CloseDrawer
            get() = Strings(1)

        val CloseSheet
            get() = Strings(2)

        val DefaultErrorMessage
            get() = Strings(3)

        val ExposedDropdownMenu
            get() = Strings(4)

        val SliderRangeStart
            get() = Strings(5)

        val SliderRangeEnd
            get() = Strings(6)

        val SnackbarPaneTitle
            get() = Strings(7)
    }
}

@Composable internal expect fun getString(string: Strings): String
