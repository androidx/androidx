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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/*
* A composition local to customize the focus scrolling behavior used by some scrollable containers.
* [LocalBringIntoViewSpec] has a platform defined behavior. The scroll default behavior will move
* the least to bring the requested region into view.
*/
@Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
@get:ExperimentalFoundationApi
@ExperimentalFoundationApi
actual val LocalBringIntoViewSpec: ProvidableCompositionLocal<BringIntoViewSpec> =
    staticCompositionLocalOf {
        BringIntoViewSpec.DefaultBringIntoViewSpec
    }
