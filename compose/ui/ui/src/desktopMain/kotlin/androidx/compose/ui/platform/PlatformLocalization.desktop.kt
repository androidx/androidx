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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

interface PlatformLocalization {
    val copy: String
    val cut: String
    val paste: String
    val selectAll: String
}

@Composable
internal fun defaultPlatformLocalization(): PlatformLocalization {
    val copy = getString(Strings.Copy)
    val cut = getString(Strings.Cut)
    val paste = getString(Strings.Paste)
    val selectAll = getString(Strings.SelectAll)
    return object : PlatformLocalization {
        override val copy = copy
        override val cut = cut
        override val paste = paste
        override val selectAll = selectAll
    }
}

val LocalLocalization: ProvidableCompositionLocal<PlatformLocalization> = staticCompositionLocalOf {
    error("CompositionLocal PlatformLocalization not present")
}